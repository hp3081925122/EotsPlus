package com.eotsplus.mixin;

import com.eotsplus.ai.EotsSharedInvulnerability;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.duck.EotsControllerDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import teamrazor.deepaether.entity.eots.EOTSController;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = EOTSController.class, remap = false)
public abstract class EOTSControllerMixin implements EotsControllerDuck {
    @Unique
    private final Map<Object, EotsSharedInvulnerability.Hit> eotsplus$sharedHits = new HashMap<>();
    @Unique
    private final Map<Object, Long> eotsplus$guardHits = new HashMap<>();

    @ModifyConstant(method = "getBonusHealth", constant = @Constant(floatValue = 10.0F))
    private static float eotsplus$bonusHealthPerSegment(float original) {
        if (EotsConfig.ENABLED.get()) {
            return EotsConfig.SEGMENT_HEALTH.get().floatValue();
        }
        return original;
    }

    @Override
    public Map<Object, EotsSharedInvulnerability.Hit> eotsplus$getSharedHits() {
        return this.eotsplus$sharedHits;
    }

    @Override
    public Map<Object, Long> eotsplus$getGuardHits() {
        return this.eotsplus$guardHits;
    }
}
