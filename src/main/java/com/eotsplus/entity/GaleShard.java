package com.eotsplus.entity;

import com.eotsplus.ai.EotsCombat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class GaleShard extends Projectile {
    private float damage;
    private int life = 30;

    public GaleShard(EntityType<? extends GaleShard> type, Level level) {
        super(type, level);
    }

    public static GaleShard spawn(Level level, Entity owner, Vec3 pos, Vec3 velocity, float damage, int life) {
        GaleShard shard = new GaleShard(EotsEntities.GALE_SHARD.get(), level);
        shard.setOwner(owner);
        shard.setPos(pos);
        shard.setDeltaMovement(velocity);
        shard.damage = damage;
        shard.life = life;
        ProjectileUtil.rotateTowardsMovement(shard, 1.0F);
        level.addFreshEntity(shard);
        return shard;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 vel = this.getDeltaMovement();
        if (!this.level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
                this.onHit(hit);
                if (this.isRemoved()) {
                    return;
                }
            }
            if (this.tickCount >= this.life) {
                this.discard();
                return;
            }
        } else if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
        this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return this.damage > 0.0F && super.canHitEntity(e) && EotsCombat.isValidTarget(e);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity living) {
            EotsCombat.hurtIndirect(this, this.getOwner(), living, this.damage);
        }
        this.burstAndDiscard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else {
            this.burstAndDiscard();
        }
    }

    private void burstAndDiscard() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.putInt("Life", this.life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.life = tag.getInt("Life");
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
