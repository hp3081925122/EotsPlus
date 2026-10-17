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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;
import io.github.razordevs.deep_aether.init.DASounds;

import java.util.HashSet;
import java.util.Set;

public class SerpentTrailSkill extends EotsSkill {
    private static final int APPROACH = 0;
    private static final int PASS = 1;
    private static final int RECOVER = 2;
    private static final double HALF_LENGTH = 16.0;
    private static final double AMPLITUDE = 5.0;
    private static final double HEIGHT = 3.0;

    private final Set<Integer> hit = new HashSet<>();
    private Vec3 from = Vec3.ZERO;
    private Vec3 to = Vec3.ZERO;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 side = Vec3.ZERO;
    private double floorY;
    private int passTicks;
    private int interval;
    private int delay;

    public SerpentTrailSkill(EotsSkillGoal g) {
        super(g);
    }

    @Override
    public void start() {
        LivingEntity target = this.g.target();
        if (target == null) {
            this.finish();
            return;
        }
        Vec3 center = target.position();
        Vec3 flat = new Vec3(center.x - this.g.pos().x, 0, center.z - this.g.pos().z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(this.g.look().x, 0, this.g.look().z);
        }
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1, 0, 0);
        }
        this.dir = flat.normalize();
        this.side = new Vec3(-this.dir.z, 0, this.dir.x);
        if (this.g.seg.getRandom().nextBoolean()) {
            this.side = this.side.scale(-1.0);
        }
        this.floorY = EotsCombat.groundY(this.g.level(), center.x, center.y + 1.0, center.z);
        this.from = new Vec3(center.x, this.floorY + HEIGHT, center.z).subtract(this.dir.scale(HALF_LENGTH));
        this.to = new Vec3(center.x, this.floorY + HEIGHT, center.z).add(this.dir.scale(HALF_LENGTH));
        this.passTicks = 36;
        this.interval = 3;
        this.delay = EotsConfig.SERPENT_TRAIL_DELAY.get();
        if (this.g.stage() >= 3) {
            this.passTicks = 30;
            this.interval = 2;
            this.delay = Math.max(8, this.delay - 6);
        }
        this.g.sound(DASounds.EOTS_AMBIENT.get(), 3.0F, 1.1F);
        this.setState(APPROACH);
    }

    @Override
    protected void tick() {
        if (this.state == APPROACH) {
            this.tickApproach();
        } else if (this.state == PASS) {
            this.tickPass();
        } else {
            this.tickRecover();
        }
    }

    private Vec3 path(double s) {
        Vec3 base = this.from.add(this.to.subtract(this.from).scale(s));
        double wave = Math.sin(s * Math.PI * 3.0) * AMPLITUDE;
        return base.add(this.side.scale(wave));
    }

    private void tickApproach() {
        this.g.steerToward(this.from, 1.25, 14.0F);
        if (this.g.pos().distanceTo(this.from) < 3.0 || this.st >= 40) {
            this.g.anim(EotsAnim.SERPENT);
            this.g.seg.setMouthOpen(true);
            this.g.sound(DASounds.EOTS_SHOOT.get(), 3.0F, 0.9F);
            this.setState(PASS);
        }
    }

    private void tickPass() {
        double s = Math.min(1.0, (this.st + 1) / (double) this.passTicks);
        Vec3 vel = this.path(s).subtract(this.g.pos());
        if (vel.length() > 2.4) {
            vel = vel.normalize().scale(2.4);
        }
        this.g.fly(vel);
        Vec3 head = this.g.pos();
        this.g.particle(ParticleTypes.CLOUD, head, vel.scale(-1.0), 0.25);
        if (this.st % this.interval == 0) {
            ServerLevel level = this.g.level();
            Vec3 ground = EotsCombat.onGround(level, head);
            if (head.y - ground.y < 10.0) {
                double radius = EotsConfig.SERPENT_TRAIL_RADIUS.get();
                float damage = (float) (EotsCombat.attack(this.g.seg) * EotsConfig.SERPENT_TRAIL.damageMultiplier.get());
                EotsFx.circle(level, ground.add(0, 0.05, 0), (float) radius, this.delay, FxType.STORM);
                EotsHazards.add(level, new TrailNode(this.g.seg, ground, radius, damage, this.delay));
            }
        }
        double contact = EotsConfig.SERPENT_TRAIL_CONTACT_MULTIPLIER.get();
        for (LivingEntity e : this.g.touching(this.g.seg, 0.6)) {
            if (this.hit.add(e.getId())) {
                EotsCombat.hurt(this.g.seg, e, contact);
                EotsCombat.push(e, vel, 0.6, 0.25);
            }
        }
        if (this.st >= this.passTicks) {
            this.g.anim(EotsAnim.IDLE);
            this.g.seg.setMouthOpen(false);
            this.setState(RECOVER);
        }
    }

    private void tickRecover() {
        this.g.fly(this.dir.scale(0.8));
        if (this.st >= 14) {
            this.finish();
        }
    }

    @Override
    public void stop() {
        this.g.seg.setMouthOpen(false);
    }

    private static final class TrailNode implements EotsHazards.Hazard {
        private final EOTSSegment owner;
        private final Vec3 pos;
        private final double radius;
        private final float damage;
        private int life;

        TrailNode(EOTSSegment owner, Vec3 pos, double radius, float damage, int life) {
            this.owner = owner;
            this.pos = pos;
            this.radius = radius;
            this.damage = damage;
            this.life = life;
        }

        @Override
        public boolean tick(ServerLevel level) {
            this.life--;
            if (this.life > 0) {
                if (this.life % 4 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, this.pos.x, this.pos.y + 0.2, this.pos.z, 2, this.radius * 0.4, 0.05, this.radius * 0.4, 0.02);
                }
                return false;
            }
            for (LivingEntity e : EotsCombat.enemies(level, this.pos, this.radius + 2.0)) {
                double dx = e.getX() - this.pos.x;
                double dz = e.getZ() - this.pos.z;
                double dy = e.getY() - this.pos.y;
                if (dx * dx + dz * dz > this.radius * this.radius || dy < -1.5 || dy > 3.5) {
                    continue;
                }
                e.hurt(this.owner.damageSources().mobAttack(this.owner), this.damage);
                EotsCombat.push(e, new Vec3(dx, 0, dz), 0.25, 0.55);
            }
            level.sendParticles(ParticleTypes.CLOUD, this.pos.x, this.pos.y + 0.3, this.pos.z, 14, this.radius * 0.3, 0.2, this.radius * 0.3, 0.12);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.pos.x, this.pos.y + 1.0, this.pos.z, 12, 0.4, 1.0, 0.4, 0.3);
            for (int i = 0; i < 6; i++) {
                level.sendParticles(ParticleTypes.CLOUD, this.pos.x, this.pos.y + 0.2, this.pos.z, 0, 0, 1, 0, 0.5 + i * 0.12);
            }
            EotsFx.column(level, this.pos, (float) (this.radius * 0.5), 4.5F, 10, FxType.STORM);
            level.playSound(null, this.pos.x, this.pos.y, this.pos.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.4F, 0.6F);
            return true;
        }
    }
}
