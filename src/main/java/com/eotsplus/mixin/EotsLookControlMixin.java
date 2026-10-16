package com.eotsplus.mixin;

import com.eotsplus.duck.EotsSegmentDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamrazor.deepaether.entity.eots.EOTSSegment;

@Mixin(targets = "teamrazor.deepaether.entity.eots.EOTSSegment$EotsLookControl", remap = false)
public abstract class EotsLookControlMixin {
    @Shadow
    EOTSSegment segment;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void eotsplus$manualLook(CallbackInfo ci) {
        if (EotsSegmentDuck.of(this.segment).eotsplus$isManual()) {
            ci.cancel();
        }
    }
}
