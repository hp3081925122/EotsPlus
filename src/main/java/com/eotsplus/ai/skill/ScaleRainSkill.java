package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsHazards;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleShard;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.entity.eots.EOTSSegment;
import teamrazor.deepaether.init.DASounds;

import java.util.List;

public class ScaleRainSkill extends EotsSkill {
    private static final int INTERVAL = 24;
    private int waves;
    private int fill;
    private Vec3 center = Vec3.ZERO;

    public ScaleRainSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.waves = EotsConfig.SCALE_RAIN_WAVES.get();
        this.fill = 30;
        if (this.g.stage() >= 3) {
            this.fill = 22;
        }
        this.center = this.g.arenaCenter();
        this.g.anim(EotsAnim.SCALE_RAISE);
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 3.0F, 0.8F);
    }

    @Override
    protected void tick() {
        double ang = this.t * 0.045;
        Vec3 drift = new Vec3(this.center.x + Math.cos(ang) * 6.0, this.g.idleY() + 3.0, this.center.z + Math.sin(ang) * 6.0);
        this.g.steerToward(drift, 0.35, 6.0F);
        int rel = this.t - 12;
        if (rel >= 0 && rel % INTERVAL == 0 && rel / INTERVAL < this.waves) {
            this.wave();
        }
        if (rel >= (this.waves - 1) * INTERVAL + this.fill + 12) {
            this.finish();
        }
    }

    private void wave() {
        ServerLevel level = this.g.level();
        RandomSource rand = this.g.seg.getRandom();
        double radius = EotsConfig.SCALE_RAIN_RADIUS.get();
        float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.SCALE_RAIN.damageMultiplier.get());
        List<EOTSSegment> chain = this.g.chain();
        for (int i = 1; i < chain.size(); i += 2) {
            Vec3 p = chain.get(i).position().add(0, 0.6, 0);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 1, 0, 0.6);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.05);
        }
        this.g.sound(DASounds.EOTS_SHOOT.get(), 2.5F, 1.5F);
        List<LivingEntity> targets = EotsCombat.enemies(level, this.g.pos(), EotsConfig.COMBAT_RANGE.get());
        int extra = EotsConfig.SCALE_RAIN_EXTRA_MARKS.get();
        for (LivingEntity e : targets) {
            Vec3 lead = e.getDeltaMovement().multiply(this.fill * 0.5, 0.0, this.fill * 0.5);
            if (lead.lengthSqr() > 16.0) {
                lead = lead.normalize().scale(4.0);
            }
            this.mark(level, e.position().add(lead), radius, damage);
            for (int k = 0; k < extra; k++) {
                double a = rand.nextDouble() * Math.PI * 2.0;
                double d = 2.5 + rand.nextDouble() * 4.5;
                this.mark(level, e.position().add(Math.cos(a) * d, 0, Math.sin(a) * d), radius, damage);
            }
        }
    }

    private void mark(ServerLevel level, Vec3 p, double radius, float damage) {
        Vec3 ground = EotsCombat.onGround(level, p);
        EotsFx.circle(level, ground.add(0, 0.04, 0), (float) radius, this.fill, FxType.DANGER);
        EotsHazards.add(level, new Impact(this.g.seg, ground, radius, damage, this.fill));
    }

    private static final class Impact implements EotsHazards.Hazard {
        private final EOTSSegment owner;
        private final Vec3 pos;
        private final double radius;
        private final float damage;
        private int delay;

        Impact(EOTSSegment owner, Vec3 pos, double radius, float damage, int delay) {
            this.owner = owner;
            this.pos = pos;
            this.radius = radius;
            this.damage = damage;
            this.delay = delay;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (this.delay == 10) {
                GaleShard.spawn(level, this.owner, this.pos.add(0, 12.0, 0), new Vec3(0, -1.2, 0), 0.0F, 12);
            }
            if (this.delay-- > 0) {
                return false;
            }
            for (LivingEntity e : EotsCombat.enemies(level, this.pos.add(0, 1.0, 0), this.radius + 1.0)) {
                Vec3 rel = e.position().subtract(this.pos);
                if (rel.x * rel.x + rel.z * rel.z <= this.radius * this.radius && rel.y > -1.0 && rel.y < 3.5) {
                    EotsCombat.hurtIndirect(this.owner, this.owner, e, this.damage);
                    EotsCombat.push(e, rel, 0.3, 0.4);
                }
            }
            level.sendParticles(ParticleTypes.CLOUD, this.pos.x, this.pos.y + 0.3, this.pos.z, 14, this.radius * 0.4, 0.2, this.radius * 0.4, 0.12);
            level.sendParticles(ParticleTypes.SNOWFLAKE, this.pos.x, this.pos.y + 0.5, this.pos.z, 20, this.radius * 0.3, 0.4, this.radius * 0.3, 0.2);
            level.playSound(null, this.pos.x, this.pos.y, this.pos.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
            EotsFx.shockwave(level, this.pos.add(0, 0.06, 0), (float) this.radius + 0.8F, 8, FxType.WIND);
            return true;
        }
    }
}
