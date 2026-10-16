package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.init.DASounds;

public class RoarSkill extends EotsSkill {
    public RoarSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.g.anim(EotsAnim.ROAR);
        this.g.seg.setMouthOpen(true);
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 5.0F, 0.5F);
        this.g.sound(DASounds.EOTS_HURT.get(), 3.0F, 0.4F);
    }

    @Override
    protected void tick() {
        this.g.hover();
        this.g.lookAtTarget(4.0F);
        if (this.t == 12) {
            double radius = EotsConfig.ROAR_RADIUS.get();
            Vec3 c = this.g.pos();
            for (LivingEntity e : EotsCombat.enemies(this.g.level(), c, radius)) {
                EotsCombat.push(e, e.position().subtract(c), 0.9, 0.35);
            }
            Vec3 ground = EotsCombat.onGround(this.g.level(), c);
            EotsFx.shockwave(this.g.level(), ground.add(0, 0.08, 0), (float) radius, 14, FxType.STORM);
            EotsFx.shake(this.g.level(), c, 1.0F, 40.0F, 25);
            this.g.burst(ParticleTypes.CLOUD, c, 60, 1.5, 0.35);
        }
        if (this.t > 12 && this.t < 36 && this.t % 2 == 0) {
            Vec3 mouth = this.g.mouth();
            this.g.particle(ParticleTypes.CLOUD, mouth, this.g.look().add(this.g.randomUnit().scale(0.3)), 0.9);
        }
        if (this.t >= 40) {
            this.finish();
        }
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
