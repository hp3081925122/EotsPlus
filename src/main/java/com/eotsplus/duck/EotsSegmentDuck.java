package com.eotsplus.duck;

import net.minecraft.world.entity.Entity;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

import java.util.List;

public interface EotsSegmentDuck {
    int eotsplus$getAnimRaw();

    void eotsplus$setAnimRaw(int raw);

    void eotsplus$playAnim(int id);

    int eotsplus$getAnimId();

    float eotsplus$getAnimTicks(float partialTick);

    int eotsplus$getStage();

    void eotsplus$setStage(int stage);

    int eotsplus$getChainIndex();

    void eotsplus$setChainIndex(int index);

    boolean eotsplus$isManual();

    void eotsplus$setManual(boolean manual);

    List<EOTSSegment> eotsplus$getChain();

    void eotsplus$setAttacking(boolean attacking);

    boolean eotsplus$isAttacking();

    boolean eotsplus$isDying();

    void eotsplus$setShouldMove(boolean move);

    int eotsplus$idleY();

    void eotsplus$returnToIdle();

    void eotsplus$randomizeIdleOffset();

    Entity eotsplus$getControllerEntity();

    static EotsSegmentDuck of(EOTSSegment segment) {
        return (EotsSegmentDuck) segment;
    }
}
