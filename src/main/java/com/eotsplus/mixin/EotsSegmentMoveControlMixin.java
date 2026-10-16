package com.eotsplus.mixin;

import com.eotsplus.duck.EotsSegmentDuck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamrazor.deepaether.entity.eots.EOTSSegment;

@Mixin(targets = "teamrazor.deepaether.entity.eots.EOTSSegment$EotsSegmentMoveControl", remap = false)
public abstract class EotsSegmentMoveControlMixin {
    @Shadow
    @Final
    private EOTSSegment segment;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void eotsplus$manualFlight(CallbackInfo ci) {
        if (EotsSegmentDuck.of(this.segment).eotsplus$isManual()) {
            ci.cancel();
        }
    }
}
