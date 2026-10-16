package com.eotsplus.ai;

import com.eotsplus.EotsPlus;
import com.eotsplus.ai.skill.EotsSkill;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.duck.EotsSegmentDuck;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EotsSkillGoal extends Goal {
    public final EOTSSegment seg;
    public final EotsSegmentDuck duck;
    private EotsBrain brain;
    private EotsSkill skill;
    private SkillType skillType;
    private int idleTicks;
    private LivingEntity lockedTarget;

    public EotsSkillGoal(EOTSSegment seg) {
        this.seg = seg;
        this.duck = EotsSegmentDuck.of(seg);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (!EotsPlus.isEnhanced(this.seg) || !this.seg.isControllingSegment() || this.duck.eotsplus$isDying() || !this.seg.isAlive()) {
            return false;
        }
        this.brain = EotsBrain.of(this.seg);
        this.brain.update(this.seg);
        if (this.duck.eotsplus$getStage() != this.brain.stage) {
            this.duck.eotsplus$setStage(this.brain.stage);
        }
        LivingEntity target = this.seg.getTarget();
        if (target == null || !EotsCombat.isValidTarget(target) || target.distanceTo(this.seg) > EotsConfig.COMBAT_RANGE.get()) {
            this.idleTicks = 0;
            return false;
        }
        this.idleTicks++;
        if (!this.seg.isAroundIdlePos() && this.idleTicks < 80) {
            return false;
        }
        long now = this.level().getGameTime();
        if (this.brain.activeHeads.size() >= EotsConfig.MAX_SKILL_HEADS.get()) {
            return false;
        }
        if (this.brain.pendingRoar > 0 && this.brain.bigOwner == null) {
            this.skillType = SkillType.ROAR;
            return true;
        }
        if (now < this.brain.nextAction) {
            return false;
        }
        SkillType picked = this.pick(now);
        if (picked == null) {
            return false;
        }
        this.skillType = picked;
        return true;
    }

    private SkillType pick(long now) {
        List<SkillType> pool = new ArrayList<>();
        int total = 0;
        for (SkillType type : SkillType.values()) {
            if (type.config == null || !type.config.enabled.get()) {
                continue;
            }
            int weight = type.config.weight.get();
            if (weight <= 0 || this.brain.stage < type.config.minStage.get() || !this.brain.isReady(type, now)) {
                continue;
            }
            if (type.big && this.brain.bigOwner != null) {
                continue;
            }
            pool.add(type);
            total += weight;
        }
        if (total <= 0) {
            return null;
        }
        int roll = this.seg.getRandom().nextInt(total);
        for (SkillType type : pool) {
            roll -= type.config.weight.get();
            if (roll < 0) {
                return type;
            }
        }
        return null;
    }

    @Override
    public void start() {
        long now = this.level().getGameTime();
        this.lockedTarget = this.seg.getTarget();
        this.brain.activeHeads.add(this.seg.getUUID());
        if (this.skillType.big) {
            this.brain.bigOwner = this.seg.getUUID();
        }
        if (this.skillType == SkillType.ROAR) {
            this.brain.pendingRoar = 0;
        } else {
            this.brain.ready.put(this.skillType, now + (long) (this.skillType.config.cooldown.get() * this.brain.cooldownMultiplier()));
        }
        this.duck.eotsplus$setManual(true);
        this.duck.eotsplus$setAttacking(true);
        this.duck.eotsplus$setShouldMove(false);
        this.seg.setMouthOpen(false);
        this.skill = this.skillType.create(this);
        if (EotsConfig.DEBUG_LOGGING.get()) {
            EotsPlus.LOGGER.info("[EotsPlus] head {} starts {} (stage {}, active heads {})", this.seg.getId(), this.skillType, this.brain.stage, this.brain.activeHeads.size());
        }
        this.skill.start();
    }

    @Override
    public boolean canContinueToUse() {
        return this.skill != null && !this.skill.isFinished() && this.seg.isAlive() && this.seg.isControllingSegment() && !this.duck.eotsplus$isDying() && EotsPlus.isEnhanced(this.seg);
    }

    @Override
    public void tick() {
        if (this.skill != null && !this.skill.isFinished()) {
            this.skill.tickSkill();
        }
    }

    @Override
    public void stop() {
        if (this.skill != null) {
            this.skill.stop();
        }
        if (EotsConfig.DEBUG_LOGGING.get() && this.skillType != null) {
            EotsPlus.LOGGER.info("[EotsPlus] head {} ends {}", this.seg.getId(), this.skillType);
        }
        long now = this.level().getGameTime();
        if (this.brain != null) {
            this.brain.activeHeads.remove(this.seg.getUUID());
            if (this.seg.getUUID().equals(this.brain.bigOwner)) {
                this.brain.bigOwner = null;
            }
            this.brain.nextAction = now + (long) (EotsConfig.ACTION_INTERVAL.get() * this.brain.cooldownMultiplier());
        }
        this.skill = null;
        this.idleTicks = 0;
        this.duck.eotsplus$setManual(false);
        this.duck.eotsplus$setAttacking(false);
        this.duck.eotsplus$setShouldMove(true);
        this.duck.eotsplus$playAnim(com.eotsplus.anim.EotsAnim.IDLE);
        this.seg.setMouthOpen(false);
        this.duck.eotsplus$randomizeIdleOffset();
        this.duck.eotsplus$returnToIdle();
    }

    public ServerLevel level() {
        return (ServerLevel) this.seg.level();
    }

    public EotsBrain brain() {
        return this.brain;
    }

    public int stage() {
        if (this.brain == null) {
            return 1;
        }
        return this.brain.stage;
    }

    public LivingEntity target() {
        LivingEntity t = this.seg.getTarget();
        if (t != null && EotsCombat.isValidTarget(t)) {
            this.lockedTarget = t;
            return t;
        }
        if (this.lockedTarget != null && EotsCombat.isValidTarget(this.lockedTarget)) {
            return this.lockedTarget;
        }
        return null;
    }

    public void anim(int id) {
        this.duck.eotsplus$playAnim(id);
    }

    public Vec3 pos() {
        return this.seg.position();
    }

    public Vec3 look() {
        return this.seg.getLookAngle();
    }

    public Vec3 mouth() {
        return this.seg.position().add(this.seg.getLookAngle().scale(1.6)).add(0, 0.5, 0);
    }

    public void face(Vec3 dir) {
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yRot = Mth.wrapDegrees((float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F);
        float xRot = Mth.wrapDegrees((float) (-(Mth.atan2(dir.y, horiz) * Mth.RAD_TO_DEG)));
        this.seg.setYRot(yRot);
        this.seg.setXRot(xRot);
        this.seg.yBodyRot = yRot;
        this.seg.setYHeadRot(yRot);
    }

    public void turnToward(Vec3 dir, float maxDegrees) {
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float wantY = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float wantX = (float) (-(Mth.atan2(dir.y, horiz) * Mth.RAD_TO_DEG));
        float yRot = Mth.approachDegrees(this.seg.getYRot(), wantY, maxDegrees);
        float xRot = Mth.approachDegrees(this.seg.getXRot(), wantX, maxDegrees);
        this.seg.setYRot(yRot);
        this.seg.setXRot(xRot);
        this.seg.yBodyRot = yRot;
        this.seg.setYHeadRot(yRot);
    }

    public void fly(Vec3 velocity) {
        this.seg.setDeltaMovement(velocity);
        this.face(velocity);
    }

    public void flyToward(Vec3 point, double speed) {
        Vec3 diff = point.subtract(this.pos());
        double dist = diff.length();
        if (dist < 1.0E-4) {
            this.hover();
            return;
        }
        double s = Math.min(speed, dist);
        this.fly(diff.scale(s / dist));
    }

    public void steerToward(Vec3 point, double speed, float turnDegrees) {
        Vec3 diff = point.subtract(this.pos());
        double dist = diff.length();
        if (dist < 1.0E-4) {
            this.hover();
            return;
        }
        this.turnToward(diff, turnDegrees);
        this.seg.setDeltaMovement(this.look().scale(Math.min(speed, dist)));
    }

    public void hover() {
        this.seg.setDeltaMovement(this.seg.getDeltaMovement().scale(0.6));
    }

    public void lookAtTarget(float maxDegrees) {
        LivingEntity t = this.target();
        if (t != null) {
            this.turnToward(t.getEyePosition().subtract(this.seg.position()), maxDegrees);
        }
    }

    public List<LivingEntity> touching(Entity e, double inflate) {
        AABB box = e.getBoundingBox().inflate(inflate);
        return EotsCombat.enemiesIn(this.level(), box);
    }

    public List<EOTSSegment> chain() {
        return this.duck.eotsplus$getChain();
    }

    public Vec3 arenaCenter() {
        Entity controller = this.duck.eotsplus$getControllerEntity();
        if (controller != null) {
            return controller.position();
        }
        LivingEntity t = this.target();
        if (t != null) {
            return t.position();
        }
        return this.pos();
    }

    public int idleY() {
        return this.duck.eotsplus$idleY();
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.seg.getX(), this.seg.getY(), this.seg.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    public void soundAt(Vec3 p, SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, p.x, p.y, p.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public void particle(ParticleOptions type, Vec3 p, Vec3 velocity, double speed) {
        this.level().sendParticles(type, p.x, p.y, p.z, 0, velocity.x, velocity.y, velocity.z, speed);
    }

    public void burst(ParticleOptions type, Vec3 p, int count, double spread, double speed) {
        this.level().sendParticles(type, p.x, p.y, p.z, count, spread, spread, spread, speed);
    }

    public Vec3 randomUnit() {
        double a = this.seg.getRandom().nextDouble() * Math.PI * 2.0;
        double y = this.seg.getRandom().nextDouble() * 2.0 - 1.0;
        double r = Math.sqrt(1.0 - y * y);
        return new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
    }
}
