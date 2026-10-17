package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.init.DASounds;

import java.util.HashSet;
import java.util.Set;

public class WindLanceSkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int AIM = 1;
    private static final int FIRE = 2;
    private static final int RECOVER = 3;
    private static final int FIRE_TICKS = 16;
    private static final double DISTANCE = 10.0;
    private static final double BEAM_RADIUS = 1.2;

    private final Set<Integer> hit = new HashSet<>();
    private Vec3 origin = Vec3.ZERO;
    private double baseAngle;
    private double sweep;
    private double range;
    private int aim;
    private int sign = 1;

    public WindLanceSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        if (this.g.target() == null) {
            this.finish();
            return;
        }
        this.range = EotsConfig.WIND_LANCE_RANGE.get();
        this.sweep = Math.toRadians(EotsConfig.WIND_LANCE_SWEEP.get());
        this.aim = EotsConfig.WIND_LANCE_AIM.get();
        if (this.g.stage() >= 3) {
            this.aim = Math.max(5, this.aim * 3 / 4);
        }
        if (this.g.seg.getRandom().nextBoolean()) {
            this.sign = -1;
        }
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.5F, 0.9F);
        this.setState(APPROACH);
    }

    @Override
    protected void tick() {
        LivingEntity target = this.g.target();
        if (this.state == APPROACH) {
            if (target == null) {
                this.finish();
                return;
            }
            Vec3 spot = this.spot(target);
            this.g.steerToward(spot, 1.0, 12.0F);
            if (this.g.pos().distanceTo(spot) < 3.0 || this.st >= 40) {
                this.lock(target);
                this.setState(AIM);
            }
            return;
        }
        this.g.holdHeight(Vec3.ZERO, EotsConfig.HOVER_HEIGHT.get());
        if (this.state == AIM) {
            this.g.face(this.beamEnd(this.angleAt(0.0)).subtract(this.g.pos()));
            Vec3 mouth = this.g.mouth();
            if (this.st % 2 == 0) {
                for (int i = 0; i < 3; i++) {
                    Vec3 off = this.g.randomUnit().scale(2.0);
                    this.g.particle(ParticleTypes.END_ROD, mouth.add(off), off.scale(-1.0).normalize(), 0.25);
                }
            }
            if (this.st % 6 == 0) {
                this.g.sound(DASounds.EOTS_BLOWING.get(), 1.5F, 1.2F + this.st / (float) this.aim);
            }
            if (this.st >= this.aim) {
                this.g.anim(EotsAnim.FIRE);
                this.g.sound(SoundEvents.WARDEN_SONIC_BOOM, 2.5F, 1.6F);
                this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 0.6F);
                EotsFx.shake(this.g.level(), this.g.pos(), 0.5F, 20.0F, FIRE_TICKS);
                this.setState(FIRE);
            }
            return;
        }
        if (this.state == FIRE) {
            double progress = Math.min(1.0, (this.st + 1) / (double) FIRE_TICKS);
            double angle = this.angleAt(progress);
            Vec3 end = this.beamEnd(angle);
            this.g.face(end.subtract(this.g.pos()));
            this.fireBeam(end);
            if (this.st + 1 >= FIRE_TICKS) {
                this.g.anim(EotsAnim.IDLE);
                this.g.seg.setMouthOpen(false);
                this.setState(RECOVER);
            }
            return;
        }
        if (this.st >= 12) {
            this.finish();
        }
    }

    private Vec3 spot(LivingEntity target) {
        Vec3 flat = new Vec3(this.g.pos().x - target.getX(), 0, this.g.pos().z - target.getZ());
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1, 0, 0);
        }
        Vec3 p = target.position().add(flat.normalize().scale(DISTANCE));
        double floor = EotsCombat.groundY(this.g.level(), p.x, Math.max(p.y, this.g.pos().y) + 1.0, p.z);
        return new Vec3(p.x, floor + EotsConfig.HOVER_HEIGHT.get(), p.z);
    }

    private void lock(LivingEntity target) {
        Vec3 head = this.g.pos();
        this.origin = new Vec3(head.x, this.g.floorBelow(head), head.z);
        this.baseAngle = Math.atan2(target.getZ() - head.z, target.getX() - head.x);
        this.g.anim(EotsAnim.CHARGE);
        this.g.seg.setMouthOpen(true);
        int lines = 5;
        if (this.sweep < 1.0E-3) {
            lines = 1;
        }
        for (int i = 0; i < lines; i++) {
            double progress = 0.5;
            if (lines > 1) {
                progress = i / (double) (lines - 1);
            }
            Vec3 end = this.beamEnd(this.angleAt(progress));
            EotsFx.line(this.g.level(), this.origin.add(0, 0.06, 0), end.add(0, -0.14, 0), (float) (BEAM_RADIUS * 2.0), this.aim + FIRE_TICKS, FxType.DANGER);
        }
        this.g.sound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.4F);
    }

    private double angleAt(double progress) {
        return this.baseAngle + this.sign * this.sweep * (progress * 2.0 - 1.0);
    }

    private Vec3 beamEnd(double angle) {
        double x = this.origin.x + Math.cos(angle) * this.range;
        double z = this.origin.z + Math.sin(angle) * this.range;
        double floor = EotsCombat.groundY(this.g.level(), x, this.origin.y + 4.0, z);
        return new Vec3(x, floor + 0.2, z);
    }

    private void fireBeam(Vec3 end) {
        ServerLevel level = this.g.level();
        Vec3 mouth = this.g.mouth();
        EotsFx.beam(level, mouth, end, 0.55F, 0.0F, 3, FxType.STORM);
        Vec3 ground = new Vec3(this.origin.x, this.origin.y, this.origin.z);
        Vec3 flat = new Vec3(end.x - ground.x, 0, end.z - ground.z);
        double len = flat.length();
        if (len < 1.0E-3) {
            return;
        }
        Vec3 dir = flat.scale(1.0 / len);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        for (double d = 2.0; d < len; d += 2.5) {
            Vec3 p = ground.add(dir.scale(d));
            double floor = EotsCombat.groundY(level, p.x, this.origin.y + 4.0, p.z);
            level.sendParticles(ParticleTypes.CLOUD, p.x, floor + 0.2, p.z, 1, 0.2, 0.05, 0.2, 0.08);
        }
        if (this.st % 2 == 0) {
            EotsFx.shockwave(level, end.add(0, -0.14, 0), 2.2F, 8, FxType.STORM);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y + 0.3, end.z, 6, 0.4, 0.3, 0.4, 0.2);
            level.sendParticles(ParticleTypes.SNOWFLAKE, end.x, end.y + 0.3, end.z, 6, 0.4, 0.3, 0.4, 0.15);
        }
        float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.WIND_LANCE.damageMultiplier.get());
        for (LivingEntity e : EotsCombat.enemies(level, ground.add(dir.scale(len * 0.5)), len * 0.5 + 3.0)) {
            double rx = e.getX() - ground.x;
            double rz = e.getZ() - ground.z;
            double along = rx * dir.x + rz * dir.z;
            if (along < 0.0 || along > len) {
                continue;
            }
            double across = Math.abs(rx * side.x + rz * side.z);
            if (across > BEAM_RADIUS + e.getBbWidth() * 0.5) {
                continue;
            }
            double beamY = mouth.y + (end.y - mouth.y) * (along / len);
            if (e.getY() > beamY + 1.0 || e.getY() + e.getBbHeight() < beamY - 3.0) {
                continue;
            }
            if (!this.hit.add(e.getId())) {
                continue;
            }
            EotsCombat.hurtIndirect(this.g.seg, this.g.seg, e, damage);
            EotsCombat.push(e, side.scale(this.sign).add(dir.scale(0.5)), 0.5, 0.3);
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1.0, e.getZ(), 10, 0.3, 0.4, 0.3, 0.15);
        }
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
