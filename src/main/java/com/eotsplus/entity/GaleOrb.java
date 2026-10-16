package com.eotsplus.entity;

import com.eotsplus.ai.EotsCombat;
import com.eotsplus.fx.EotsFx;
import com.eotsplus.fx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class GaleOrb extends Projectile {
    private float damage;
    private float shardDamage;
    private int shardCount = 8;

    public GaleOrb(EntityType<? extends GaleOrb> type, Level level) {
        super(type, level);
    }

    public static GaleOrb spawn(Level level, Entity owner, Vec3 pos, Vec3 velocity, float damage, float shardDamage, int shardCount) {
        GaleOrb orb = new GaleOrb(EotsEntities.GALE_ORB.get(), level);
        orb.setOwner(owner);
        orb.setPos(pos);
        orb.setDeltaMovement(velocity);
        orb.damage = damage;
        orb.shardDamage = shardDamage;
        orb.shardCount = shardCount;
        level.addFreshEntity(orb);
        return orb;
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
                this.setPos(hit.getLocation());
                this.shatter();
                return;
            }
            if (this.tickCount >= 50) {
                this.shatter();
                return;
            }
        } else {
            this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), -vel.x * 0.2, -vel.y * 0.2, -vel.z * 0.2);
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX() + (this.random.nextDouble() - 0.5), this.getY() + (this.random.nextDouble() - 0.5), this.getZ() + (this.random.nextDouble() - 0.5), 0, 0, 0);
        }
        this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && EotsCombat.isValidTarget(e);
    }

    private void shatter() {
        ServerLevel level = (ServerLevel) this.level();
        Vec3 c = this.position();
        Entity owner = this.getOwner();
        for (LivingEntity e : EotsCombat.enemies(level, c, 2.8)) {
            EotsCombat.hurtIndirect(this, owner, e, this.damage);
            EotsCombat.push(e, e.position().subtract(c), 0.6, 0.3);
        }
        double offset = this.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < this.shardCount; i++) {
            double a = offset + Math.PI * 2.0 * i / this.shardCount;
            Vec3 v = new Vec3(Math.cos(a) * 0.55, 0.04, Math.sin(a) * 0.55);
            GaleShard.spawn(level, owner, c.add(v.scale(0.8)), v, this.shardDamage, 30);
        }
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 24, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y, c.z, 30, 0.5, 0.5, 0.5, 0.3);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        EotsFx.shockwave(level, c, 3.5F, 10, FxType.WIND);
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.putFloat("ShardDamage", this.shardDamage);
        tag.putInt("ShardCount", this.shardCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.shardDamage = tag.getFloat("ShardDamage");
        this.shardCount = Math.max(1, tag.getInt("ShardCount"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
