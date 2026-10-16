package com.eotsplus.ai;

import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.GaleOrb;
import com.eotsplus.entity.GaleShard;
import com.eotsplus.entity.GaleTornado;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSController;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

import java.util.List;

public final class EotsCombat {
    private EotsCombat() {
    }

    public static boolean isValidTarget(Entity e) {
        if (!(e instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (e instanceof EOTSSegment || e instanceof EOTSController || e instanceof GaleTornado || e instanceof GaleOrb || e instanceof GaleShard) {
            return false;
        }
        if (e instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    public static List<LivingEntity> enemies(Level level, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        double r2 = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> isValidTarget(e) && e.position().add(0, e.getBbHeight() * 0.5, 0).distanceToSqr(center) <= r2);
    }

    public static List<LivingEntity> enemiesIn(Level level, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, EotsCombat::isValidTarget);
    }

    public static double attack(LivingEntity attacker) {
        if (attacker.getAttribute(Attributes.ATTACK_DAMAGE) == null) {
            return EotsConfig.ATTACK_DAMAGE.get();
        }
        return attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public static boolean hurt(LivingEntity attacker, LivingEntity target, double multiplier) {
        float amount = (float) (attack(attacker) * multiplier);
        if (amount <= 0.0F) {
            return false;
        }
        return target.hurt(attacker.damageSources().mobAttack(attacker), amount);
    }

    public static boolean hurtIndirect(Entity direct, Entity owner, LivingEntity target, float amount) {
        if (amount <= 0.0F) {
            return false;
        }
        DamageSource source;
        if (owner instanceof LivingEntity livingOwner) {
            source = direct.damageSources().mobProjectile(direct, livingOwner);
        } else {
            source = direct.damageSources().indirectMagic(direct, owner);
        }
        return target.hurt(source, amount);
    }

    public static double resistFactor(LivingEntity target) {
        double res = 0.0;
        if (target.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            res = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        }
        return Mth.clamp(1.0 - res, 0.0, 1.0);
    }

    public static void push(LivingEntity target, Vec3 dir, double strength, double up) {
        double factor = resistFactor(target) * EotsConfig.KNOCKBACK_MULTIPLIER.get();
        if (factor <= 0.0) {
            return;
        }
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() > 1.0E-6) {
            flat = flat.normalize();
        }
        Vec3 add = flat.scale(strength * factor);
        Vec3 vel = target.getDeltaMovement().add(add.x, up * factor, add.z);
        double maxUp = EotsConfig.MAX_LAUNCH_VELOCITY.get();
        if (vel.y > maxUp) {
            vel = new Vec3(vel.x, maxUp, vel.z);
        }
        target.setDeltaMovement(vel);
        target.hasImpulse = true;
        if (target instanceof ServerPlayer) {
            target.hurtMarked = true;
        }
    }

    public static void pull(LivingEntity target, Vec3 point, double strength, boolean horizontalOnly) {
        double factor = resistFactor(target);
        if (factor <= 0.0) {
            return;
        }
        Vec3 diff = point.subtract(target.position());
        if (horizontalOnly) {
            diff = new Vec3(diff.x, 0, diff.z);
        }
        if (diff.lengthSqr() < 0.25) {
            return;
        }
        Vec3 add = diff.normalize().scale(strength * factor);
        target.setDeltaMovement(target.getDeltaMovement().add(add));
        target.hasImpulse = true;
        if (target instanceof ServerPlayer) {
            target.hurtMarked = true;
        }
    }

    public static double groundY(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        for (int i = 0; i < 64; i++) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1.0;
            }
            pos.move(0, -1, 0);
        }
        return y - 20.0;
    }

    public static Vec3 onGround(Level level, Vec3 p) {
        return new Vec3(p.x, groundY(level, p.x, p.y + 1.0, p.z), p.z);
    }
}
