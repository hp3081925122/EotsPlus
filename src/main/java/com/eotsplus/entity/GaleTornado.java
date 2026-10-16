package com.eotsplus.entity;

import com.eotsplus.ai.EotsCombat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GaleTornado extends Entity {
    public static final double PULL_RADIUS = 5.0;
    public static final double CORE_RADIUS = 1.6;
    public static final double HEIGHT = 6.0;

    private UUID ownerUUID;
    private Entity cachedOwner;
    private float damage;
    private int life = 220;
    private double speed = 0.09;
    private final Map<Integer, Integer> hitCooldown = new HashMap<>();

    public GaleTornado(EntityType<? extends GaleTornado> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static GaleTornado spawn(Level level, Entity owner, Vec3 pos, float damage, int life, double speed) {
        GaleTornado t = new GaleTornado(EotsEntities.GALE_TORNADO.get(), level);
        t.setPos(pos);
        t.ownerUUID = owner.getUUID();
        t.cachedOwner = owner;
        t.damage = damage;
        t.life = life;
        t.speed = speed;
        level.addFreshEntity(t);
        return t;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }
        if (this.ownerUUID != null && this.level() instanceof ServerLevel server) {
            this.cachedOwner = server.getEntity(this.ownerUUID);
            return this.cachedOwner;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientParticles();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount >= this.life) {
            level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.0, this.getZ(), 30, 0.8, 1.5, 0.8, 0.1);
            this.discard();
            return;
        }
        double floor = EotsCombat.groundY(level, this.getX(), this.getY() + 1.0, this.getZ());
        double vy = 0.0;
        if (this.getY() > floor + 0.1) {
            vy = Math.max(-0.6, (floor - this.getY()) * 0.3);
        } else if (this.getY() < floor - 0.1) {
            vy = Math.min(0.4, (floor - this.getY()) * 0.5);
        }
        Vec3 horiz = Vec3.ZERO;
        LivingEntity chase = this.nearestTarget(level);
        if (chase != null && this.tickCount > 15) {
            Vec3 diff = chase.position().subtract(this.position());
            Vec3 flat = new Vec3(diff.x, 0, diff.z);
            if (flat.lengthSqr() > 0.25) {
                horiz = flat.normalize().scale(this.speed);
            }
        }
        this.setDeltaMovement(horiz.x, vy, horiz.z);
        this.setPos(this.position().add(this.getDeltaMovement()));
        Vec3 center = this.position();
        for (LivingEntity e : EotsCombat.enemies(level, center.add(0, HEIGHT * 0.5, 0), PULL_RADIUS + HEIGHT * 0.5)) {
            Vec3 rel = e.position().subtract(center);
            double d2 = rel.x * rel.x + rel.z * rel.z;
            if (rel.y < -1.0 || rel.y > HEIGHT || d2 > PULL_RADIUS * PULL_RADIUS) {
                continue;
            }
            Vec3 inward = new Vec3(-rel.x, 0, -rel.z);
            Vec3 tangent = new Vec3(-rel.z, 0, rel.x);
            if (inward.lengthSqr() > 1.0E-4) {
                double factor = EotsCombat.resistFactor(e);
                Vec3 add = inward.normalize().scale(0.05 * factor).add(tangent.normalize().scale(0.04 * factor));
                e.setDeltaMovement(e.getDeltaMovement().add(add));
                e.hurtMarked = true;
            }
            if (d2 < CORE_RADIUS * CORE_RADIUS) {
                int last = this.hitCooldown.getOrDefault(e.getId(), -100);
                if (this.tickCount - last >= 10) {
                    this.hitCooldown.put(e.getId(), this.tickCount);
                    EotsCombat.hurtIndirect(this, this.getOwner(), e, this.damage);
                    EotsCombat.push(e, Vec3.ZERO, 0.0, 0.25);
                }
            }
        }
        if (this.tickCount % 20 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 0.25F, 1.6F);
        }
    }

    @Nullable
    private LivingEntity nearestTarget(ServerLevel level) {
        List<LivingEntity> list = EotsCombat.enemies(level, this.position(), 40.0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            if (!(e instanceof net.minecraft.world.entity.player.Player)) {
                continue;
            }
            double d = e.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private void clientParticles() {
        double a = this.tickCount * 0.6;
        for (int i = 0; i < 3; i++) {
            double h = this.random.nextDouble() * HEIGHT;
            double r = 0.4 + h * 0.3;
            double ang = a + i * 2.094 + h;
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() + Math.cos(ang) * r, this.getY() + h, this.getZ() + Math.sin(ang) * r, -Math.sin(ang) * 0.15, 0.05, Math.cos(ang) * 0.15);
        }
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.POOF, this.getX() + (this.random.nextDouble() - 0.5) * 2.0, this.getY() + 0.1, this.getZ() + (this.random.nextDouble() - 0.5) * 2.0, 0, 0.02, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.damage = tag.getFloat("Damage");
        this.life = tag.getInt("Life");
        this.speed = tag.getDouble("Speed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putFloat("Damage", this.damage);
        tag.putInt("Life", this.life);
        tag.putDouble("Speed", this.speed);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
