package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsHazards;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import teamrazor.deepaether.entity.eots.EOTSSegment;
import teamrazor.deepaether.init.DASounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkyRendSkill extends EotsSkill {
    private static final int WINDUP = 0;
    private static final int DASH = 1;
    private static final int RECOVER = 2;

    private final Set<Integer> hit = new HashSet<>();
    private final List<Vec3> wake = new ArrayList<>();
    private int windup;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 lockPoint = Vec3.ZERO;
    private double floorY;
    private double speed;

    public SkyRendSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.windup = 24;
        this.speed = 1.55;
        if (this.g.stage() >= 3) {
            this.windup = 18;
            this.speed = 1.85;
        }
        this.g.anim(EotsAnim.REAR);
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 3.0F, 0.7F);
        this.setState(WINDUP);
    }

    @Override
    protected void tick() {
        if (this.state == WINDUP) {
            this.tickWindup();
        } else if (this.state == DASH) {
            this.tickDash();
        } else {
            this.tickRecover();
        }
    }

    private void tickWindup() {
        LivingEntity target = this.g.target();
        if (target == null && this.st < this.windup - 8) {
            this.finish();
            return;
        }
        this.g.seg.setDeltaMovement(this.g.seg.getDeltaMovement().scale(0.55).add(0, 0.03, 0));
        if (this.st < this.windup - 8) {
            this.g.lookAtTarget(14.0F);
        } else if (this.st == this.windup - 8) {
            Vec3 lead = target.getDeltaMovement().multiply(4.0, 0.0, 4.0);
            this.lockPoint = target.position().add(0, 0.6, 0).add(lead);
            this.dir = this.lockPoint.subtract(this.g.pos()).normalize();
            this.floorY = EotsCombat.groundY(this.g.level(), this.lockPoint.x, this.lockPoint.y + 1.0, this.lockPoint.z);
            Vec3 flat = new Vec3(this.dir.x, 0, this.dir.z).normalize();
            Vec3 ground = new Vec3(this.lockPoint.x, this.floorY + 0.05, this.lockPoint.z);
            EotsFx.line(this.g.level(), ground.subtract(flat.scale(6.0)), ground.add(flat.scale(14.0)), 2.6F, 34, FxType.DANGER);
            this.g.face(this.dir);
        } else {
            this.g.face(this.dir);
            this.g.burst(ParticleTypes.CLOUD, this.g.mouth(), 2, 0.3, 0.02);
        }
        if (this.st >= this.windup) {
            this.g.anim(EotsAnim.DASH);
            this.g.seg.setMouthOpen(true);
            this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 0.6F);
            EotsFx.shake(this.g.level(), this.g.pos(), 0.35F, 24.0F, 10);
            this.setState(DASH);
        }
    }

    private void tickDash() {
        Vec3 head = this.g.pos();
        if (this.dir.y < 0 && head.y < this.floorY + 1.4) {
            this.dir = new Vec3(this.dir.x, 0.04, this.dir.z).normalize();
        }
        this.g.fly(this.dir.scale(this.speed));
        this.g.particle(ParticleTypes.CLOUD, head, this.dir.scale(-1.0), 0.3);
        this.g.particle(ParticleTypes.CLOUD, head.add(this.g.randomUnit().scale(0.8)), this.dir.scale(-1.0), 0.2);
        for (LivingEntity e : this.g.touching(this.g.seg, 1.0)) {
            if (this.hit.add(e.getId())) {
                EotsCombat.hurt(this.g.seg, e, EotsConfig.SKY_REND.damageMultiplier.get());
                EotsCombat.push(e, this.dir, 1.0, 0.35);
            }
        }
        if (this.st % 2 == 0) {
            List<EOTSSegment> chain = this.g.chain();
            double bodyMult = EotsConfig.SKY_REND_BODY_MULTIPLIER.get();
            for (int i = 1; i < chain.size(); i++) {
                EOTSSegment s = chain.get(i);
                for (LivingEntity e : this.g.touching(s, 0.4)) {
                    if (this.hit.add(e.getId())) {
                        EotsCombat.hurt(this.g.seg, e, bodyMult);
                        Vec3 away = e.position().subtract(s.position());
                        EotsCombat.push(e, away, 0.6, 0.2);
                    }
                }
            }
        }
        if (this.g.stage() >= 2 && this.st % 2 == 0) {
            this.wake.add(head);
        }
        double passed = head.subtract(this.lockPoint).dot(this.dir);
        if (passed > 9.0 || this.st > 45) {
            this.g.anim(EotsAnim.IDLE);
            this.g.seg.setMouthOpen(false);
            this.setState(RECOVER);
        }
    }

    private void tickRecover() {
        Vec3 flat = new Vec3(this.dir.x, 0, this.dir.z).normalize();
        this.g.fly(flat.scale(0.9).add(0, 0.7, 0));
        if (this.st >= 14) {
            if (!this.wake.isEmpty() && EotsConfig.SKY_REND_WAKE_DURATION.get() > 0) {
                ServerLevel level = this.g.level();
                Vec3 first = this.wake.get(0);
                Vec3 last = this.wake.get(this.wake.size() - 1);
                EotsFx.line(level, EotsCombat.onGround(level, first).add(0, 0.05, 0), EotsCombat.onGround(level, last).add(0, 0.05, 0), 3.4F, EotsConfig.SKY_REND_WAKE_DURATION.get(), FxType.DEEP);
                EotsHazards.add(level, new WindWake(new ArrayList<>(this.wake), flat, EotsConfig.SKY_REND_WAKE_DURATION.get()));
            }
            this.finish();
        }
    }

    private static final class WindWake implements EotsHazards.Hazard {
        private final List<Vec3> points;
        private final Vec3 side;
        private int life;

        WindWake(List<Vec3> points, Vec3 flatDir, int life) {
            this.points = points;
            this.side = new Vec3(-flatDir.z, 0, flatDir.x);
            this.life = life;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (this.life-- <= 0) {
                return true;
            }
            Vec3 a = this.points.get(0);
            Vec3 b = this.points.get(this.points.size() - 1);
            double radius = Math.max(a.distanceTo(b) * 0.5 + 3.0, 4.0);
            Vec3 mid = a.add(b).scale(0.5);
            for (LivingEntity e : EotsCombat.enemies(level, mid, radius + 8.0)) {
                for (Vec3 p : this.points) {
                    Vec3 rel = e.position().subtract(p);
                    if (rel.x * rel.x + rel.z * rel.z < 3.2 && Math.abs(rel.y) < 6.0) {
                        double sign = 1.0;
                        if (rel.dot(this.side) < 0) {
                            sign = -1.0;
                        }
                        EotsCombat.push(e, this.side.scale(sign), 0.14, 0.03);
                        break;
                    }
                }
            }
            if (this.life % 3 == 0) {
                Vec3 p = this.points.get(level.random.nextInt(this.points.size()));
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, this.side.x, 0.05, this.side.z, 0.3);
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, -this.side.x, 0.05, -this.side.z, 0.3);
            }
            return false;
        }
    }
}
