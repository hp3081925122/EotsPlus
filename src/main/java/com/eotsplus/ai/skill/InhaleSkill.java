package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.init.DASounds;

public class InhaleSkill extends EotsSkill {
    private static final int POSITION = 0;
    private static final int INHALE = 1;
    private static final int EXHALE = 2;
    private static final int RECOVER = 3;
    private static final double HALF_ANGLE = 35.0;

    private int inhale;
    private double range;
    private Vec3 exhaleDir = Vec3.ZERO;

    public InhaleSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.inhale = 45;
        if (this.g.stage() >= 3) {
            this.inhale = 35;
        }
        this.range = EotsConfig.INHALE_RANGE.get();
        this.setState(POSITION);
    }

    private boolean inCone(Vec3 origin, Vec3 dir, LivingEntity e, double maxRange, double halfAngle) {
        Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
        double d = rel.length();
        if (d > maxRange || d < 1.0E-3) {
            return false;
        }
        double cos = rel.scale(1.0 / d).dot(dir);
        return cos >= Math.cos(Math.toRadians(halfAngle));
    }

    @Override
    protected void tick() {
        LivingEntity target = this.g.target();
        if (this.state == POSITION) {
            if (target == null) {
                this.finish();
                return;
            }
            Vec3 away = this.g.pos().subtract(target.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            if (flat.lengthSqr() < 1.0E-3) {
                flat = new Vec3(1, 0, 0);
            }
            Vec3 spot = target.position().add(flat.normalize().scale(10.0)).add(0, 4.0, 0);
            this.g.steerToward(spot, 0.9, 14.0F);
            if (this.g.pos().distanceTo(spot) < 2.0 || this.st >= 30) {
                this.g.anim(EotsAnim.INHALE);
                this.g.seg.setMouthOpen(true);
                this.g.sound(DASounds.EOTS_BLOWING.get(), 3.0F, 0.6F);
                this.g.lookAtTarget(180.0F);
                EotsFx.fan(this.g.level(), this.g.mouth(), this.g.look(), (float) this.range, (float) HALF_ANGLE, this.inhale + 4, FxType.WIND);
                this.setState(INHALE);
            }
        } else if (this.state == INHALE) {
            this.g.hover();
            this.g.lookAtTarget(3.0F);
            Vec3 mouth = this.g.mouth();
            Vec3 dir = this.g.look();
            double pull = EotsConfig.INHALE_PULL.get();
            for (LivingEntity e : EotsCombat.enemies(this.g.level(), mouth, this.range)) {
                if (this.inCone(mouth, dir, e, this.range, HALF_ANGLE)) {
                    EotsCombat.pull(e, mouth, pull, false);
                }
            }
            for (int i = 0; i < 4; i++) {
                double dist = 4.0 + this.g.seg.getRandom().nextDouble() * (this.range - 4.0);
                Vec3 spread = this.g.randomUnit().scale(dist * 0.45);
                Vec3 p = mouth.add(dir.scale(dist)).add(spread);
                this.g.particle(ParticleTypes.CLOUD, p, mouth.subtract(p).normalize(), 0.7);
            }
            if (this.st % 10 == 0) {
                EotsFx.shake(this.g.level(), mouth, 0.12F, (float) this.range, 10);
            }
            if (this.st >= this.inhale) {
                this.exhaleDir = this.g.look();
                this.g.anim(EotsAnim.EXHALE);
                this.blast();
                this.setState(EXHALE);
            }
        } else if (this.state == EXHALE) {
            this.g.seg.setDeltaMovement(this.exhaleDir.scale(-0.08));
            this.g.face(this.exhaleDir);
            Vec3 mouth = this.g.mouth();
            for (int i = 0; i < 8; i++) {
                Vec3 v = this.exhaleDir.add(this.g.randomUnit().scale(0.35)).normalize();
                this.g.particle(ParticleTypes.CLOUD, mouth, v, 1.2);
            }
            this.g.particle(ParticleTypes.POOF, mouth, this.exhaleDir, 0.8);
            for (LivingEntity e : EotsCombat.enemies(this.g.level(), mouth, this.range * 0.8)) {
                if (this.inCone(mouth, this.exhaleDir, e, this.range * 0.8, HALF_ANGLE + 3.0)) {
                    EotsCombat.push(e, e.position().subtract(mouth), 0.1, 0.0);
                }
            }
            if (this.st >= 20) {
                this.g.seg.setMouthOpen(false);
                this.g.anim(EotsAnim.IDLE);
                this.setState(RECOVER);
            }
        } else {
            this.g.fly(this.exhaleDir.scale(-0.3).add(0, 0.6, 0));
            this.g.face(this.exhaleDir);
            if (this.st >= 10) {
                this.finish();
            }
        }
    }

    private void blast() {
        Vec3 mouth = this.g.mouth();
        for (LivingEntity e : EotsCombat.enemies(this.g.level(), mouth, this.range * 0.8)) {
            if (this.inCone(mouth, this.exhaleDir, e, this.range * 0.8, HALF_ANGLE + 3.0)) {
                EotsCombat.hurt(this.g.seg, e, EotsConfig.INHALE.damageMultiplier.get());
                EotsCombat.push(e, e.position().subtract(mouth), 1.3, 0.3);
            }
        }
        this.g.sound(DASounds.EOTS_SHOOT.get(), 4.0F, 0.5F);
        this.g.sound(SoundEvents.GENERIC_EXPLODE, 1.2F, 0.5F);
        EotsFx.fan(this.g.level(), mouth, this.exhaleDir, (float) (this.range * 0.8), (float) HALF_ANGLE + 3.0F, 12, FxType.DANGER);
        EotsFx.shake(this.g.level(), mouth, 0.7F, 28.0F, 12);
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }
}
