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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GaleCrescent extends Entity {
    private static final double DECAY = 0.93;
    private static final double TURN_SPEED = 0.18;
    private static final double MAX_RETURN_SPEED = 1.1;

    private final Set<Integer> hitOut = new HashSet<>();
    private final Set<Integer> hitBack = new HashSet<>();
    private UUID ownerUUID;
    private Entity cachedOwner;
    private float damage;
    private boolean returning;
    private double cruiseY;
    private Vec3 origin = Vec3.ZERO;

    public GaleCrescent(EntityType<? extends GaleCrescent> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static double speedForRange(double range) {
        return range * (1.0 - DECAY) + TURN_SPEED;
    }

    public static GaleCrescent spawn(Level level, Entity owner, Vec3 pos, Vec3 velocity, double cruiseY, float damage) {
        GaleCrescent c = new GaleCrescent(EotsEntities.GALE_CRESCENT.get(), level);
        c.setPos(pos);
        c.setDeltaMovement(velocity);
        c.ownerUUID = owner.getUUID();
        c.cachedOwner = owner;
        c.cruiseY = cruiseY;
        c.damage = damage;
        c.origin = pos;
        level.addFreshEntity(c);
        return c;
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
            if (this.tickCount % 2 == 0) {
                double a = this.tickCount * 0.7;
                this.level().addParticle(ParticleTypes.CLOUD, this.getX() + Math.cos(a) * 1.0, this.getY() + 0.25, this.getZ() + Math.sin(a) * 1.0, 0, 0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Vec3 vel = this.getDeltaMovement();
        Entity owner = this.getOwner();
        if (!this.returning) {
            double vy = (this.cruiseY - this.getY()) * 0.25;
            vel = new Vec3(vel.x * DECAY, Math.max(-0.9, Math.min(0.5, vy)), vel.z * DECAY);
            if (Math.sqrt(vel.x * vel.x + vel.z * vel.z) < TURN_SPEED || this.tickCount > 45) {
                this.returning = true;
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        } else {
            Vec3 home = this.origin;
            if (owner != null && owner.isAlive()) {
                home = owner.position().add(0, 0.5, 0);
            }
            Vec3 to = home.subtract(this.position());
            if (to.length() < 1.8 || this.tickCount > 120) {
                level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 8, 0.4, 0.1, 0.4, 0.05);
                this.discard();
                return;
            }
            vel = vel.scale(0.88).add(to.normalize().scale(0.16));
            if (vel.length() > MAX_RETURN_SPEED) {
                vel = vel.normalize().scale(MAX_RETURN_SPEED);
            }
        }
        this.setDeltaMovement(vel);
        this.setPos(this.position().add(vel));
        Set<Integer> hits = this.hitOut;
        if (this.returning) {
            hits = this.hitBack;
        }
        for (LivingEntity e : EotsCombat.enemiesIn(level, this.getBoundingBox().inflate(0.3, 0.6, 0.3))) {
            if (hits.add(e.getId())) {
                EotsCombat.hurtIndirect(this, owner, e, this.damage);
                EotsCombat.push(e, vel, 0.4, 0.15);
            }
        }
        if (this.tickCount % 6 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5F, 1.5F);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.damage = tag.getFloat("Damage");
        this.returning = tag.getBoolean("Returning");
        this.cruiseY = tag.getDouble("CruiseY");
        this.origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putFloat("Damage", this.damage);
        tag.putBoolean("Returning", this.returning);
        tag.putDouble("CruiseY", this.cruiseY);
        tag.putDouble("OriginX", this.origin.x);
        tag.putDouble("OriginY", this.origin.y);
        tag.putDouble("OriginZ", this.origin.z);
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
