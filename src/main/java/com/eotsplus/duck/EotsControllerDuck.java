package com.eotsplus.duck;

import com.eotsplus.ai.EotsSharedInvulnerability;
import teamrazor.deepaether.entity.eots.EOTSController;

import java.util.Map;

public interface EotsControllerDuck {
    Map<Object, EotsSharedInvulnerability.Hit> eotsplus$getSharedHits();

    Map<Object, Long> eotsplus$getGuardHits();

    static EotsControllerDuck of(EOTSController controller) {
        return (EotsControllerDuck) controller;
    }
}
