package com.eotsplus.mixin;

import com.eotsplus.EotsPlus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

@Mixin(targets = "io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment$EotsAirChargeGoal", remap = false)
public abstract class EotsAirChargeGoalMixin {
    @Shadow
    EOTSSegment segment;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = true)
    private void eotsplus$blockOriginal(CallbackInfoReturnable<Boolean> cir) {
        if (EotsPlus.isEnhanced(this.segment)) {
            cir.setReturnValue(false);
        }
    }
}
