package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleOrb;
import com.eotsplus.fx.EotsFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.init.DASounds;

public class ShatterCrystalSkill extends EotsSkill {
    private int charge;
    private int orbs;
    private int fired;

    public ShatterCrystalSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.charge = 30;
        this.orbs = this.g.stage();
        if (this.g.stage() >= 3) {
            this.charge = 22;
        }
        this.g.anim(EotsAnim.CHARGE);
        this.g.seg.setMouthOpen(true);
        this.g.sound(DASounds.EOTS_BLOWING.get(), 2.5F, 1.3F);
    }

    @Override
    protected void tick() {
        this.g.hover();
        this.g.lookAtTarget(10.0F);
        if (this.t < this.charge) {
            Vec3 mouth = this.g.mouth();
            if (this.t % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    Vec3 off = this.g.randomUnit().scale(3.0);
                    this.g.particle(ParticleTypes.CLOUD, mouth.add(off), off.scale(-1.0).normalize(), 0.35);
                }
                this.g.particle(ParticleTypes.SNOWFLAKE, mouth.add(this.g.randomUnit().scale(2.0)), Vec3.ZERO, 0.0);
            }
            return;
        }
        int sinceCharge = this.t - this.charge;
        if (this.fired < this.orbs && sinceCharge == this.fired * 10) {
            this.fire();
        }
        if (sinceCharge >= this.orbs * 10 + 12) {
            this.finish();
        }
    }

    private void fire() {
        LivingEntity target = this.g.target();
        Vec3 mouth = this.g.mouth();
        Vec3 aim;
        if (target != null) {
            aim = target.position().add(0, target.getBbHeight() * 0.5, 0).add(target.getDeltaMovement().multiply(8.0, 0.0, 8.0));
        } else {
            aim = mouth.add(this.g.look().scale(10.0));
        }
        Vec3 dir = aim.subtract(mouth).normalize();
        if (this.fired > 0) {
            double yaw = Math.toRadians(14.0);
            if (this.fired % 2 == 0) {
                yaw = -yaw;
            }
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);
            dir = new Vec3(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos);
        }
        double atk = EotsCombat.attack(this.g.seg);
        int shards = EotsConfig.SHATTER_SHARD_COUNT.get() + (this.g.stage() - 1) * 2;
        GaleOrb.spawn(this.g.level(), this.g.seg, mouth, dir.scale(0.85), (float) (atk * EotsConfig.SHATTER_CRYSTAL.damageMultiplier.get()), (float) (atk * EotsConfig.SHATTER_SHARD_MULTIPLIER.get()), shards);
        this.g.anim(EotsAnim.FIRE);
        this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 0.8F);
        this.g.burst(ParticleTypes.CLOUD, mouth, 10, 0.3, 0.15);
        EotsFx.shake(this.g.level(), mouth, 0.2F, 16.0F, 6);
        this.fired++;
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
