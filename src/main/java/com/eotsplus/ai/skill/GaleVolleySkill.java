package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleOrb;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.init.DASounds;

public class GaleVolleySkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int CHARGE = 1;
    private static final int FIRE = 2;
    private static final int RECOVER = 3;
    private static final double DISTANCE = 11.0;

    private int count;
    private int fired;
    private int interval;
    private int side = 1;

    public GaleVolleySkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        if (this.g.target() == null) {
            this.finish();
            return;
        }
        this.count = EotsConfig.GALE_VOLLEY_COUNT.get();
        this.interval = 7;
        if (this.g.stage() >= 3) {
            this.count += 2;
            this.interval = 5;
        }
        if (this.g.seg.getRandom().nextBoolean()) {
            this.side = -1;
        }
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.5F, 1.2F);
        this.setState(APPROACH);
    }

    @Override
    protected void tick() {
        LivingEntity target = this.g.target();
        if (target == null) {
            this.finish();
            return;
        }
        if (this.state == APPROACH) {
            Vec3 spot = this.spot(target);
            this.g.steerToward(spot, 1.0, 12.0F);
            if (this.g.pos().distanceTo(spot) < 3.0 || this.st >= 40) {
                this.g.anim(EotsAnim.CHARGE);
                this.g.seg.setMouthOpen(true);
                this.g.sound(DASounds.EOTS_BLOWING.get(), 2.5F, 1.5F);
                this.setState(CHARGE);
            }
            return;
        }
        Vec3 flat = new Vec3(target.getX() - this.g.pos().x, 0, target.getZ() - this.g.pos().z);
        Vec3 strafe = Vec3.ZERO;
        if (flat.lengthSqr() > 1.0E-4) {
            strafe = new Vec3(-flat.z, 0, flat.x).normalize().scale(0.12 * this.side);
        }
        this.g.holdHeight(strafe, EotsConfig.HOVER_HEIGHT.get());
        this.g.lookAtTarget(14.0F);
        if (this.state == CHARGE) {
            Vec3 mouth = this.g.mouth();
            if (this.st % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    Vec3 off = this.g.randomUnit().scale(2.5);
                    this.g.particle(ParticleTypes.CLOUD, mouth.add(off), off.scale(-1.0).normalize(), 0.3);
                }
                this.g.particle(ParticleTypes.END_ROD, mouth.add(this.g.randomUnit().scale(1.5)), Vec3.ZERO, 0.0);
            }
            if (this.st >= 16) {
                this.setState(FIRE);
            }
            return;
        }
        if (this.state == FIRE) {
            if (this.st % this.interval == 0 && this.fired < this.count) {
                this.fire(target);
            }
            if (this.fired >= this.count && this.st >= this.count * this.interval + 6) {
                this.g.anim(EotsAnim.IDLE);
                this.g.seg.setMouthOpen(false);
                this.setState(RECOVER);
            }
            return;
        }
        if (this.st >= 10) {
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

    private void fire(LivingEntity target) {
        Vec3 mouth = this.g.mouth();
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
        if (aim.lengthSqr() < 1.0E-4) {
            aim = this.g.look();
        }
        Vec3 dir = aim.normalize();
        Vec3 sideDir = new Vec3(-dir.z, 0, dir.x);
        if (sideDir.lengthSqr() < 1.0E-4) {
            sideDir = new Vec3(1, 0, 0);
        }
        sideDir = sideDir.normalize();
        double spread = 0.55;
        if (this.fired % 2 == 1) {
            spread = -spread;
        }
        Vec3 launch = dir.add(sideDir.scale(spread)).add(0, 0.25, 0).normalize().scale(0.6);
        double homing = EotsConfig.GALE_VOLLEY_HOMING.get();
        float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.GALE_VOLLEY.damageMultiplier.get());
        GaleOrb.spawnHoming(this.g.level(), this.g.seg, mouth, launch, damage, target, homing);
        this.g.anim(EotsAnim.FIRE);
        this.g.sound(DASounds.EOTS_SHOOT.get(), 2.5F, 1.1F + this.fired * 0.05F);
        this.g.burst(ParticleTypes.CLOUD, mouth, 6, 0.2, 0.1);
        EotsFx.shockwave(this.g.level(), mouth, 1.6F, 6, FxType.WIND);
        this.fired++;
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
