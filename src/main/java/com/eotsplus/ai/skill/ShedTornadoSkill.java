package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleTornado;
import com.eotsplus.fx.EotsFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.entity.eots.EOTSSegment;
import teamrazor.deepaether.init.DASounds;

import java.util.List;

public class ShedTornadoSkill extends EotsSkill {
    private static final int SHED_TICKS = 30;
    private int count;
    private int spawned;

    public ShedTornadoSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.count = EotsConfig.SHED_TORNADO_COUNT.get();
        if (this.g.stage() >= 3) {
            this.count++;
        }
        this.g.anim(EotsAnim.SHED);
        this.g.sound(DASounds.EOTS_HURT.get(), 3.0F, 0.6F);
        EotsFx.shake(this.g.level(), this.g.pos(), 0.3F, 24.0F, SHED_TICKS);
    }

    @Override
    protected void tick() {
        this.g.hover();
        this.g.lookAtTarget(6.0F);
        List<EOTSSegment> chain = this.g.chain();
        if (this.t % 3 == 0 && chain.size() > 1) {
            EOTSSegment s = chain.get(1 + this.g.seg.getRandom().nextInt(chain.size() - 1));
            this.g.burst(ParticleTypes.SNOWFLAKE, s.position().add(0, 0.6, 0), 4, 0.4, 0.08);
        }
        if (this.spawned < this.count && this.t == 10 + this.spawned * 6) {
            int idx = chain.size() - 1 - this.spawned * 4;
            if (idx < 0) {
                idx = 0;
            }
            Vec3 from = chain.get(idx).position();
            float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.SHED_TORNADO.damageMultiplier.get());
            double speed = 0.09;
            if (this.g.stage() >= 3) {
                speed = 0.12;
            }
            GaleTornado.spawn(this.g.level(), this.g.seg, from, damage, EotsConfig.SHED_TORNADO_LIFETIME.get(), speed);
            this.g.burst(ParticleTypes.CLOUD, from, 20, 0.6, 0.15);
            this.g.soundAt(from, DASounds.EOTS_SHOOT.get(), 2.5F, 0.7F);
            this.spawned++;
        }
        if (this.t >= SHED_TICKS + 12) {
            this.finish();
        }
    }
}
