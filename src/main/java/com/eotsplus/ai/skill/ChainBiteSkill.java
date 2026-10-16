package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.init.DASounds;

public class ChainBiteSkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int WINDUP = 1;
    private static final int LUNGE = 2;
    private static final int RECOIL = 3;

    private int bites;
    private int done;
    private int windup;
    private Vec3 dir = Vec3.ZERO;
    private boolean landed;

    public ChainBiteSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.bites = EotsConfig.CHAIN_BITE_COUNT.get();
        this.windup = 8;
        if (this.g.stage() >= 3) {
            this.bites++;
            this.windup = 6;
        }
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
            Vec3 aim = target.getEyePosition();
            this.g.steerToward(aim, 1.15, 16.0F);
            if (this.g.pos().distanceTo(aim) < 7.0 || this.st >= 30) {
                this.g.anim(EotsAnim.BITE_WINDUP);
                this.g.seg.setMouthOpen(true);
                this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.0F, 1.4F);
                this.setState(WINDUP);
            }
        } else if (this.state == WINDUP) {
            if (target != null) {
                this.g.lookAtTarget(20.0F);
            }
            this.g.seg.setDeltaMovement(this.g.look().scale(-0.12));
            this.g.burst(ParticleTypes.CLOUD, this.g.mouth(), 1, 0.2, 0.01);
            if (this.st >= this.windup) {
                if (target == null) {
                    this.dir = this.g.look();
                } else {
                    this.dir = target.getEyePosition().subtract(this.g.pos()).normalize();
                }
                this.landed = false;
                this.g.anim(EotsAnim.BITE_SNAP);
                this.g.sound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.6F);
                this.setState(LUNGE);
            }
        } else if (this.state == LUNGE) {
            this.g.fly(this.dir.scale(2.0));
            this.g.particle(ParticleTypes.CLOUD, this.g.pos(), this.dir.scale(-1.0), 0.25);
            for (LivingEntity e : this.g.touching(this.g.seg, 0.9)) {
                EotsCombat.hurt(this.g.seg, e, EotsConfig.CHAIN_BITE.damageMultiplier.get());
                EotsCombat.push(e, this.dir, 0.7, 0.25);
                this.landed = true;
            }
            if (this.landed || this.st >= 7) {
                this.g.seg.setMouthOpen(false);
                this.setState(RECOIL);
            }
        } else {
            this.g.fly(this.dir.scale(-0.45).add(0, 0.35, 0));
            this.g.face(this.dir);
            if (this.st >= 8) {
                this.done++;
                if (this.done >= this.bites || target == null) {
                    this.finish();
                } else {
                    this.g.anim(EotsAnim.IDLE);
                    this.setState(APPROACH);
                }
            }
        }
    }
}
