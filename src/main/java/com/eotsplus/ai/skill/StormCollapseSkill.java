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
import teamrazor.deepaether.init.DAParticles;
import teamrazor.deepaether.init.DASounds;

public class StormCollapseSkill extends EotsSkill {
    private static final int GATHER = 0;
    private static final int SPIRAL = 1;
    private static final int ALIGN = 2;
    private static final int DIVE = 3;
    private static final int DOWNED = 4;
    private static final int SPIRAL_TICKS = 70;
    private static final double SPIRAL_RADIUS = 5.5;

    private Vec3 center = Vec3.ZERO;
    private double floorY;
    private double lowY;
    private double topY;
    private double radius;
    private double angle;
    private Vec3 restPos = Vec3.ZERO;

    public StormCollapseSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        Vec3 c = this.g.arenaCenter();
        this.floorY = EotsCombat.groundY(this.g.level(), c.x, this.g.pos().y, c.z);
        this.center = new Vec3(c.x, this.floorY, c.z);
        this.topY = Math.max(this.g.idleY() + 4.0, this.floorY + 16.0);
        this.lowY = this.topY - 12.0;
        this.radius = EotsConfig.STORM_COLLAPSE_RADIUS.get();
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 4.0F, 0.5F);
        this.setState(GATHER);
    }

    @Override
    protected void tick() {
        ServerLevel level = this.g.level();
        if (this.state == GATHER) {
            Vec3 entry = new Vec3(this.center.x + SPIRAL_RADIUS, this.lowY, this.center.z);
            this.g.steerToward(entry, 1.4, 18.0F);
            if (this.g.pos().distanceTo(entry) < 2.5 || this.st >= 60) {
                this.g.anim(EotsAnim.ULT_SPIRAL);
                this.g.seg.setMouthOpen(true);
                float height = (float) (this.topY - this.floorY + 6.0);
                EotsFx.column(level, this.center, (float) SPIRAL_RADIUS + 0.5F, height, SPIRAL_TICKS + 30, FxType.STORM);
                EotsFx.circle(level, this.center.add(0, 0.05, 0), (float) this.radius, SPIRAL_TICKS + 30, FxType.DANGER);
                EotsFx.circle(level, this.center.add(0, 0.07, 0), (float) (this.radius * 0.35), SPIRAL_TICKS + 30, FxType.DANGER);
                this.angle = 0.0;
                this.setState(SPIRAL);
            }
        } else if (this.state == SPIRAL) {
            this.angle += 0.17;
            double progress = Math.min(1.0, this.st / (double) SPIRAL_TICKS);
            double y = this.lowY + (this.topY - this.lowY) * progress;
            Vec3 want = new Vec3(this.center.x + Math.cos(this.angle) * SPIRAL_RADIUS, y, this.center.z + Math.sin(this.angle) * SPIRAL_RADIUS);
            Vec3 diff = want.subtract(this.g.pos());
            double len = diff.length();
            if (len > 1.6) {
                diff = diff.scale(1.6 / len);
            }
            this.g.fly(diff);
            double pull = EotsConfig.STORM_COLLAPSE_PULL.get();
            for (LivingEntity e : EotsCombat.enemies(level, this.center.add(0, 4.0, 0), this.radius * 1.6)) {
                EotsCombat.pull(e, this.center, pull, true);
            }
            for (int i = 0; i < 6; i++) {
                double a = this.angle * 1.4 + i * 1.047;
                double h = this.g.seg.getRandom().nextDouble() * (this.topY - this.floorY);
                double r = SPIRAL_RADIUS + 1.0 + this.g.seg.getRandom().nextDouble() * 2.0;
                Vec3 p = this.center.add(Math.cos(a) * r, h, Math.sin(a) * r);
                Vec3 tangent = new Vec3(-Math.sin(a), 0.15, Math.cos(a));
                this.g.particle(ParticleTypes.CLOUD, p, tangent, 0.6);
            }
            if (this.st % 20 == 0) {
                this.g.soundAt(this.center, DASounds.EOTS_BLOWING.get(), 4.0F, 0.7F);
                EotsFx.shake(level, this.center, 0.25F, 40.0F, 20);
            }
            if (this.st >= SPIRAL_TICKS) {
                this.setState(ALIGN);
            }
        } else if (this.state == ALIGN) {
            Vec3 above = new Vec3(this.center.x, this.topY + 2.0, this.center.z);
            this.g.flyToward(above, 1.2);
            if (this.st >= 8) {
                this.g.anim(EotsAnim.ULT_DIVE);
                this.g.sound(DASounds.EOTS_AMBIENT.get(), 4.0F, 0.6F);
                this.setState(DIVE);
            }
        } else if (this.state == DIVE) {
            Vec3 pos = this.g.pos();
            Vec3 v = new Vec3((this.center.x - pos.x) * 0.25, -2.2, (this.center.z - pos.z) * 0.25);
            this.g.fly(v);
            this.g.particle(ParticleTypes.CLOUD, pos, new Vec3(0, 1, 0), 0.5);
            if (pos.y <= this.floorY + 1.5 || this.g.seg.verticalCollision || this.st >= 25) {
                this.impact();
                this.g.seg.setMouthOpen(false);
                this.g.seg.setDeltaMovement(Vec3.ZERO);
                this.restPos = new Vec3(pos.x, Math.max(pos.y, this.floorY + 0.5), pos.z);
                this.g.anim(EotsAnim.DOWNED);
                this.setState(DOWNED);
            }
        } else {
            this.g.seg.setDeltaMovement(this.restPos.subtract(this.g.pos()).scale(0.4));
            if (this.st % 8 == 0) {
                Vec3 p = this.restPos;
                level.sendParticles(ParticleTypes.CLOUD, p.x, this.floorY + 0.3, p.z, 6, 1.2, 0.1, 1.2, 0.02);
            }
            if (this.st == 0) {
                this.g.sound(DASounds.EOTS_HURT.get(), 3.0F, 0.5F);
            }
            if (this.st >= EotsConfig.STORM_COLLAPSE_DOWNED.get()) {
                this.g.sound(DASounds.EOTS_AMBIENT.get(), 2.5F, 0.8F);
                this.finish();
            }
        }
    }

    private void impact() {
        ServerLevel level = this.g.level();
        double mult = EotsConfig.STORM_COLLAPSE.damageMultiplier.get();
        for (LivingEntity e : EotsCombat.enemies(level, this.center.add(0, 2.0, 0), this.radius + 2.0)) {
            Vec3 rel = e.position().subtract(this.center);
            double d = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (d > this.radius || rel.y > 8.0) {
                continue;
            }
            double falloff = Math.max(0.15, 1.0 - d / this.radius);
            EotsCombat.hurt(this.g.seg, e, mult * falloff);
            EotsCombat.push(e, rel, 1.1 * falloff + 0.3, 0.45);
        }
        Vec3 c = this.center.add(0, 0.5, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(DAParticles.EOTS_EXPLOSION.get(), c.x, c.y + 1.0, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 80, this.radius * 0.3, 0.3, this.radius * 0.3, 0.4);
        this.g.soundAt(c, SoundEvents.GENERIC_EXPLODE, 3.0F, 0.6F);
        this.g.soundAt(c, DASounds.EOTS_AMBIENT.get(), 4.0F, 0.5F);
        EotsFx.shockwave(level, this.center.add(0, 0.1, 0), (float) this.radius + 2.0F, 16, FxType.STORM);
        EotsFx.shake(level, c, 1.6F, 48.0F, 20);
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
