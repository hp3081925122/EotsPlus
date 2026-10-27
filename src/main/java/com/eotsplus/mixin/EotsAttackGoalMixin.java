package com.eotsplus.mixin;

import com.eotsplus.EotsPlus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

@Mixin(targets = "io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment$EotsAttackGoal", remap = false)
public abstract class EotsAttackGoalMixin {
    @Shadow
    EOTSSegment segment;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = true)
    private void eotsplus$blockOriginal(CallbackInfoReturnable<Boolean> cir) {
        if (EotsPlus.isEnhanced(this.segment)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"), remap = true)
    private void eotsplus$guardTargetKnockback(LivingEntity target, Vec3 movement) {
        if (target != null) {
            target.setDeltaMovement(movement);
        } else {
            EotsPlus.LOGGER.debug("[EotsPlus] Skipped original EOTS knockback after the target was cleared during attack: segment={}", this.segment.getId());
        }
    }
}
