package com.eotsplus.duck;

import com.eotsplus.ai.EotsSharedInvulnerability;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSController;

import java.util.Map;

public interface EotsControllerDuck {
    Map<Object, EotsSharedInvulnerability.Hit> eotsplus$getSharedHits();

    Map<Object, Long> eotsplus$getGuardHits();

    static EotsControllerDuck of(EOTSController controller) {
        return (EotsControllerDuck) controller;
    }
}
