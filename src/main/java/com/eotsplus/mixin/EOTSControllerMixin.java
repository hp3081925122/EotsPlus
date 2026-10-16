package com.eotsplus.mixin;

import com.eotsplus.config.EotsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import teamrazor.deepaether.entity.eots.EOTSController;

@Mixin(value = EOTSController.class, remap = false)
public abstract class EOTSControllerMixin {
    @ModifyConstant(method = "getBonusHealth", constant = @Constant(floatValue = 10.0F))
    private static float eotsplus$bonusHealthPerSegment(float original) {
        if (EotsConfig.ENABLED.get()) {
            return EotsConfig.SEGMENT_HEALTH.get().floatValue();
        }
        return original;
    }
}
