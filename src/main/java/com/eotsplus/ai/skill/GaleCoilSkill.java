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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.entity.eots.EOTSSegment;
import teamrazor.deepaether.init.DASounds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaleCoilSkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int CIRCLE = 1;
    private static final int CONSTRICT = 2;
    private static final int RECOVER = 3;
    private static final int CONSTRICT_TICKS = 20;
    private static final double INNER_RADIUS = 2.5;
    private static final double BLAST_RADIUS = 4.5;

    private final Map<Integer, Integer> hitTimes = new HashMap<>();
    private Vec3 center = Vec3.ZERO;
    private double ringY;
    private double radius;
    private double angle;
    private double speed;
    private int circleTicks;

    public GaleCoilSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        LivingEntity target = this.g.target();
        if (target == null) {
            this.finish();
            return;
        }
        this.radius = EotsConfig.GALE_COIL_RADIUS.get();
        this.speed = 1.0;
        this.circleTicks = 56;
        if (this.g.stage() >= 3) {
            this.speed = 1.25;
            this.circleTicks = 44;
        }
        this.center = EotsCombat.onGround(this.g.level(), target.position());
        this.ringY = this.center.y + 2.5;
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 3.0F, 0.9F);
        this.setState(APPROACH);
    }

    @Override
    protected void tick() {
        if (this.state == APPROACH) {
            this.tickApproach();
        } else if (this.state == CIRCLE) {
            this.tickCircle();
        } else if (this.state == CONSTRICT) {
            this.tickConstrict();
        } else {
            this.tickRecover();
        }
    }

    private Vec3 ring(double a, double r) {
        return new Vec3(this.center.x + Math.cos(a) * r, this.ringY, this.center.z + Math.sin(a) * r);
    }

    private void tickApproach() {
        Vec3 rel = this.g.pos().subtract(this.center);
        Vec3 flat = new Vec3(rel.x, 0, rel.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1, 0, 0);
        }
        Vec3 entry = this.center.add(flat.normalize().scale(this.radius));
        entry = new Vec3(entry.x, this.ringY, entry.z);
        this.g.steerToward(entry, 1.3, 15.0F);
        if (this.g.pos().distanceTo(entry) < 2.5 || this.st >= 40) {
            this.angle = Math.atan2(this.g.pos().z - this.center.z, this.g.pos().x - this.center.x);
            ServerLevel level = this.g.level();
            int total = this.circleTicks + CONSTRICT_TICKS;
            EotsFx.circle(level, this.center.add(0, 0.05, 0), (float) this.radius, total, FxType.DEEP);
            EotsFx.circle(level, this.center.add(0, 0.06, 0), (float) BLAST_RADIUS, total, FxType.DANGER);
            this.g.anim(EotsAnim.COIL);
            this.g.seg.setMouthOpen(true);
            this.g.sound(DASounds.EOTS_BLOWING.get(), 3.0F, 0.7F);
            this.setState(CIRCLE);
        }
    }

    private void tickCircle() {
        this.angle += this.speed / this.radius;
        Vec3 ahead = this.ring(this.angle + 2.0 * this.speed / this.radius, this.radius);
        this.g.fly(ahead.subtract(this.g.pos()).normalize().scale(this.speed));
        ServerLevel level = this.g.level();
        double pull = EotsConfig.GALE_COIL_WALL_PULL.get();
        for (LivingEntity e : EotsCombat.enemies(level, this.center, this.radius + 4.0)) {
            double dx = e.getX() - this.center.x;
            double dz = e.getZ() - this.center.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > this.radius - 2.5 && d < this.radius + 2.0 && Math.abs(e.getY() - this.center.y) < 8.0) {
                EotsCombat.pull(e, this.center, pull, true);
            }
        }
        if (this.st % 3 == 0) {
            double a = this.angle + Math.PI * 0.5 + this.g.seg.getRandom().nextDouble() * Math.PI;
            Vec3 p = this.ring(a, this.radius);
            Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a));
            this.g.particle(ParticleTypes.CLOUD, new Vec3(p.x, this.center.y + 0.3, p.z), tangent.add(0, 0.2, 0), 0.3);
        }
        this.bodyHits();
        if (this.st >= this.circleTicks) {
            this.g.anim(EotsAnim.CONSTRICT);
            this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 0.5F);
            EotsFx.shake(level, this.center, 0.3F, 20.0F, 12);
            this.setState(CONSTRICT);
        }
    }

    private void tickConstrict() {
        float f = Mth.clamp((this.st + 1) / (float) CONSTRICT_TICKS, 0.0F, 1.0F);
        double r = Mth.lerp(f, this.radius, INNER_RADIUS);
        this.angle += Math.min(this.speed * 1.3 / r, 0.5);
        Vec3 ahead = this.ring(this.angle + 0.35, r);
        this.g.fly(ahead.subtract(this.g.pos()).normalize().scale(Math.min(this.speed * 1.3, ahead.distanceTo(this.g.pos()))));
        for (LivingEntity e : EotsCombat.enemies(this.g.level(), this.center, r + 3.0)) {
            EotsCombat.pull(e, this.center, 0.08, true);
        }
        this.bodyHits();
        if (this.st >= CONSTRICT_TICKS) {
            this.blast();
            this.g.anim(EotsAnim.IDLE);
            this.g.seg.setMouthOpen(false);
            this.setState(RECOVER);
        }
    }

    private void blast() {
        ServerLevel level = this.g.level();
        Vec3 c = this.center.add(0, 0.5, 0);
        for (LivingEntity e : EotsCombat.enemies(level, c, BLAST_RADIUS + 2.0)) {
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            if (dx * dx + dz * dz > BLAST_RADIUS * BLAST_RADIUS || Math.abs(e.getY() - this.center.y) > 5.0) {
                continue;
            }
            EotsCombat.hurt(this.g.seg, e, EotsConfig.GALE_COIL.damageMultiplier.get());
            EotsCombat.push(e, new Vec3(dx, 0, dz), 0.5, 0.75);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 1.0, 0.3, 1.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 40, 1.5, 0.4, 1.5, 0.25);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2.0 * i / 16.0;
            level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.2, c.z, 0, Math.cos(a), 0.6, Math.sin(a), 0.6);
        }
        EotsFx.shockwave(level, this.center.add(0, 0.1, 0), 7.0F, 14, FxType.WIND);
        EotsFx.column(level, this.center, 1.6F, 7.0F, 16, FxType.STORM);
        EotsFx.shake(level, c, 0.8F, 24.0F, 14);
        this.g.soundAt(c, SoundEvents.GENERIC_EXPLODE, 2.0F, 1.4F);
        this.g.soundAt(c, DASounds.EOTS_SHOOT.get(), 3.0F, 0.6F);
    }

    private void bodyHits() {
        if (this.st % 2 != 0) {
            return;
        }
        int now = this.t;
        double mult = EotsConfig.GALE_COIL_BODY_MULTIPLIER.get();
        List<EOTSSegment> chain = this.g.chain();
        for (EOTSSegment s : chain) {
            for (LivingEntity e : this.g.touching(s, 0.35)) {
                int last = this.hitTimes.getOrDefault(e.getId(), -100);
                if (now - last < 16) {
                    continue;
                }
                this.hitTimes.put(e.getId(), now);
                EotsCombat.hurt(this.g.seg, e, mult);
                EotsCombat.push(e, this.center.subtract(e.position()), 0.45, 0.2);
            }
        }
    }

    private void tickRecover() {
        Vec3 out = this.g.pos().subtract(this.center);
        Vec3 flat = new Vec3(out.x, 0, out.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = this.g.look();
        }
        this.g.fly(new Vec3(flat.x, 0, flat.z).normalize().scale(0.6).add(0, 0.8, 0));
        if (this.st >= 14) {
            this.finish();
        }
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
