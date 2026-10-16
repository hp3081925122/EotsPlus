package com.eotsplus.mixin;

import com.eotsplus.EotsPlus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamrazor.deepaether.entity.eots.EOTSSegment;

@Mixin(targets = "teamrazor.deepaether.entity.eots.EOTSSegment$EotsAttackGoal", remap = false)
public abstract class EotsAttackGoalMixin {
    @Shadow
    EOTSSegment segment;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = true)
    private void eotsplus$blockOriginal(CallbackInfoReturnable<Boolean> cir) {
        if (EotsPlus.isEnhanced(this.segment)) {
            cir.setReturnValue(false);
        }
    }
}
