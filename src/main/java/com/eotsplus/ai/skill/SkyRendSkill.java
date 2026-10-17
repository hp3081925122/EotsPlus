package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.ai.EotsHazards;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int STUCK = 2;
    private static final int SWEEP = 3;
    private static final int PULL = 4;
    private static final int SWEEP_TICKS = 14;
    private static final int PULL_TICKS = 6;
    private final Set<Integer> hit = new HashSet<>();
    private final Set<Integer> sweepHit = new HashSet<>();
    private final List<Vec3> wake = new ArrayList<>();
    private int windup;
    private int stuck;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 flat = Vec3.ZERO;
    private Vec3 lockPoint = Vec3.ZERO;
    private Vec3 stuckPos = Vec3.ZERO;
    private double speed;

    public SkyRendSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        this.windup = 24;
        this.speed = 1.55;
        this.stuck = EotsConfig.SKY_REND_STUCK.get();
        if (this.g.stage() >= 3) {
            this.windup = 18;
            this.speed = 1.85;
            this.stuck = this.stuck * 3 / 4;
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
        } else if (this.state == STUCK) {
            this.tickStuck();
        } else if (this.state == SWEEP) {
            this.tickSweep();
        } else {
            this.tickPull();
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
            this.lockPoint = target.position().add(lead);
            this.dir = this.lockPoint.subtract(this.g.pos()).normalize();
            double floorY = EotsCombat.groundY(this.g.level(), this.lockPoint.x, this.lockPoint.y + 1.0, this.lockPoint.z);
            this.flat = new Vec3(this.dir.x, 0, this.dir.z);
            if (this.flat.lengthSqr() < 1.0E-4) {
                this.flat = new Vec3(this.g.look().x, 0, this.g.look().z);
            }
            if (this.flat.lengthSqr() < 1.0E-4) {
                this.flat = new Vec3(1, 0, 0);
            }
            this.flat = this.flat.normalize();
            Vec3 ground = new Vec3(this.lockPoint.x, floorY + 0.05, this.lockPoint.z);
            EotsFx.line(this.g.level(), ground.subtract(this.flat.scale(6.0)), ground.add(this.flat.scale(4.0)), 2.6F, 34, FxType.DANGER);
            EotsFx.circle(this.g.level(), ground.add(this.flat.scale(1.5)), 3.0F, 34, FxType.DANGER);
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
        double floorY = this.g.floorBelow(head);
        double passed = head.subtract(this.lockPoint).dot(this.dir);
        if (passed > 1.0 && this.dir.y > -0.6) {
            this.dir = this.dir.add(0, -0.3, 0).normalize();
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
        boolean grounded = head.y <= floorY + 1.0 || this.g.seg.verticalCollision || (this.g.seg.horizontalCollision && this.st > 2);
        if (grounded) {
            this.impale(new Vec3(head.x, Math.max(head.y, floorY + 0.4), head.z));
            return;
        }
        if (this.st > 45) {
            if (head.y - floorY < 3.0) {
                this.impale(head);
            } else {
                this.g.anim(EotsAnim.IDLE);
                this.g.seg.setMouthOpen(false);
                this.leaveWake();
                this.finish();
            }
        }
    }

    private void impale(Vec3 at) {
        ServerLevel level = this.g.level();
        this.stuckPos = at;
        this.g.seg.setDeltaMovement(Vec3.ZERO);
        Vec3 ground = EotsCombat.onGround(level, at);
        double mult = EotsConfig.SKY_REND_IMPACT_MULTIPLIER.get();
        for (LivingEntity e : EotsCombat.enemies(level, ground, 4.0)) {
            Vec3 rel = e.position().subtract(ground);
            if (rel.x * rel.x + rel.z * rel.z > 9.0 || Math.abs(rel.y) > 3.0 || this.hit.contains(e.getId())) {
                continue;
            }
            EotsCombat.hurt(this.g.seg, e, mult);
            EotsCombat.push(e, rel, 0.6, 0.5);
        }
        BlockState state = level.getBlockState(BlockPos.containing(ground.x, ground.y - 0.5, ground.z));
        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), ground.x, ground.y + 0.2, ground.z, 40, 1.2, 0.3, 1.2, 0.2);
        }
        level.sendParticles(ParticleTypes.CLOUD, ground.x, ground.y + 0.3, ground.z, 24, 1.0, 0.2, 1.0, 0.15);
        EotsFx.shockwave(level, ground.add(0, 0.08, 0), 4.0F, 12, FxType.WIND);
        EotsFx.shake(level, ground, 0.6F, 24.0F, 12);
        this.g.soundAt(ground, SoundEvents.GENERIC_EXPLODE, 1.4F, 0.7F);
        this.g.soundAt(ground, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
        this.g.anim(EotsAnim.STUCK);
        this.setState(STUCK);
    }

    private void tickStuck() {
        this.g.seg.setDeltaMovement(this.stuckPos.subtract(this.g.pos()).scale(0.5));
        this.g.face(this.flat.add(0, -0.6, 0));
        if (this.st % 5 == 0) {
            Vec3 ground = EotsCombat.onGround(this.g.level(), this.stuckPos);
            this.g.burst(ParticleTypes.POOF, ground.add(0, 0.3, 0), 3, 0.5, 0.02);
        }
        if (this.st == this.stuck - 6) {
            this.g.sound(DASounds.EOTS_HURT.get(), 2.0F, 0.7F);
        }
        if (this.st >= this.stuck) {
            this.g.seg.setMouthOpen(true);
            if (this.g.stage() >= 2) {
                this.g.anim(EotsAnim.SWEEP);
                this.g.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 3.0F, 0.5F);
                this.setState(SWEEP);
            } else {
                this.g.anim(EotsAnim.BITE_SNAP);
                this.setState(PULL);
            }
            Vec3 ground = EotsCombat.onGround(this.g.level(), this.stuckPos);
            this.g.burst(ParticleTypes.CLOUD, ground.add(0, 0.4, 0), 16, 0.8, 0.12);
        }
    }

    private void tickSweep() {
        this.g.holdHeight(Vec3.ZERO, 1.4);
        float f = Math.min(1.0F, (this.st + 1) / (float) (SWEEP_TICKS - 4));
        this.g.headSweep(this.flat, f, 5.0, EotsConfig.SKY_REND_IMPACT_MULTIPLIER.get(), this.sweepHit);
        if (this.st >= SWEEP_TICKS) {
            this.g.seg.setMouthOpen(false);
            this.leaveWake();
            this.finish();
        }
    }

    private void tickPull() {
        this.g.holdHeight(this.flat.scale(-0.15), 1.6);
        this.g.face(this.flat);
        if (this.st >= PULL_TICKS) {
            this.g.seg.setMouthOpen(false);
            this.leaveWake();
            this.finish();
        }
    }

    private void leaveWake() {
        if (this.wake.size() < 2 || EotsConfig.SKY_REND_WAKE_DURATION.get() <= 0) {
            return;
        }
        ServerLevel level = this.g.level();
        Vec3 first = this.wake.get(0);
        Vec3 last = this.wake.get(this.wake.size() - 1);
        EotsFx.line(level, EotsCombat.onGround(level, first).add(0, 0.05, 0), EotsCombat.onGround(level, last).add(0, 0.05, 0), 3.4F, EotsConfig.SKY_REND_WAKE_DURATION.get(), FxType.DEEP);
        EotsHazards.add(level, new WindWake(new ArrayList<>(this.wake), this.flat, EotsConfig.SKY_REND_WAKE_DURATION.get()));
        this.wake.clear();
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
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
