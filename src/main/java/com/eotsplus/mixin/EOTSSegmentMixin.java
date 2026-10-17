package com.eotsplus.mixin;

import com.eotsplus.EotsPlus;
import com.eotsplus.anim.EotsAnim;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.duck.EotsSegmentDuck;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamrazor.deepaether.entity.eots.EOTSController;
import teamrazor.deepaether.entity.eots.EOTSSegment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = EOTSSegment.class, remap = false)
public abstract class EOTSSegmentMixin extends FlyingMob implements EotsSegmentDuck {
    @Unique
    private static final EntityDataAccessor<Integer> EOTSPLUS$ANIM = SynchedEntityData.defineId(EOTSSegment.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Byte> EOTSPLUS$STAGE = SynchedEntityData.defineId(EOTSSegment.class, EntityDataSerializers.BYTE);
    @Unique
    private static final EntityDataAccessor<Integer> EOTSPLUS$CHAIN_INDEX = SynchedEntityData.defineId(EOTSSegment.class, EntityDataSerializers.INT);

    @Shadow
    private boolean isAttacking;
    @Shadow
    private boolean shouldMove;
    @Shadow
    protected boolean segmentDeathAnimation;
    @Shadow
    private int randomYOffset;

    @Unique
    private int eotsplus$animSeq;
    @Unique
    private int eotsplus$lastRaw;
    @Unique
    private int eotsplus$animStart;
    @Unique
    private boolean eotsplus$manual;
    @Unique
    private boolean eotsplus$chainDirty = true;
    @Unique
    private List<EOTSSegment> eotsplus$chain = new ArrayList<>();

    protected EOTSSegmentMixin(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
    }

    @Shadow
    private int getIdleYPos() {
        throw new AssertionError();
    }

    @Shadow
    private void goToIdlePos() {
        throw new AssertionError();
    }

    @Shadow
    public abstract EOTSController getController();

    @Shadow
    public abstract EOTSSegment getParent();

    @Shadow
    public abstract boolean isControllingSegment();

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = true)
    private void eotsplus$defineData(CallbackInfo ci) {
        this.entityData.define(EOTSPLUS$ANIM, 0);
        this.entityData.define(EOTSPLUS$STAGE, (byte) 1);
        this.entityData.define(EOTSPLUS$CHAIN_INDEX, 0);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = true)
    private void eotsplus$tick(CallbackInfo ci) {
        int raw = this.entityData.get(EOTSPLUS$ANIM);
        if (raw != this.eotsplus$lastRaw) {
            this.eotsplus$lastRaw = raw;
            this.eotsplus$animStart = this.tickCount;
        }
        if (this.level().isClientSide() || !this.isControllingSegment()) {
            return;
        }
        if (this.eotsplus$chainDirty || this.tickCount % 10 == 0) {
            this.eotsplus$chainDirty = false;
            this.eotsplus$rebuildChain();
        }
        int stage = this.entityData.get(EOTSPLUS$STAGE);
        for (int i = 1; i < this.eotsplus$chain.size(); i++) {
            EOTSSegment seg = this.eotsplus$chain.get(i);
            EotsSegmentDuck duck = EotsSegmentDuck.of(seg);
            if (duck.eotsplus$getAnimRaw() != raw) {
                duck.eotsplus$setAnimRaw(raw);
            }
            if (duck.eotsplus$getStage() != stage) {
                duck.eotsplus$setStage(stage);
            }
            if (duck.eotsplus$getChainIndex() != i) {
                duck.eotsplus$setChainIndex(i);
            }
        }
        if (this.entityData.get(EOTSPLUS$CHAIN_INDEX) != 0) {
            this.entityData.set(EOTSPLUS$CHAIN_INDEX, 0);
        }
    }

    @Unique
    private void eotsplus$rebuildChain() {
        List<EOTSSegment> nearby = this.level().getEntitiesOfClass(EOTSSegment.class, this.getBoundingBox().inflate(96.0));
        Map<EOTSSegment, EOTSSegment> childOf = new HashMap<>();
        for (EOTSSegment seg : nearby) {
            if (seg.isRemoved() || seg.isControllingSegment()) {
                continue;
            }
            EOTSSegment parent = seg.getParent();
            if (parent != null) {
                childOf.put(parent, seg);
            }
        }
        List<EOTSSegment> chain = new ArrayList<>();
        EOTSSegment cursor = (EOTSSegment) (Object) this;
        while (cursor != null && chain.size() < 256 && !chain.contains(cursor)) {
            chain.add(cursor);
            cursor = childOf.get(cursor);
        }
        this.eotsplus$chain = chain;
    }

    @Inject(method = "getIdleYPos", at = @At("HEAD"), cancellable = true)
    private void eotsplus$lowerIdle(CallbackInfoReturnable<Integer> cir) {
        if (!EotsPlus.isEnhanced((EOTSSegment) (Object) this)) {
            return;
        }
        Entity anchor = this.getController();
        if (anchor == null) {
            anchor = this.getTarget();
        }
        if (anchor == null) {
            return;
        }
        int spread = EotsConfig.IDLE_HEIGHT_RANDOM.get() + 1;
        cir.setReturnValue(anchor.blockPosition().getY() + EotsConfig.IDLE_HEIGHT.get() + this.randomYOffset % spread);
    }

    @Inject(method = "getGlobalSpeedModifier", at = @At("RETURN"), cancellable = true)
    private void eotsplus$speedBonus(CallbackInfoReturnable<Float> cir) {
        if (EotsConfig.ENABLED.get() && this.eotsplus$getStage() >= 3) {
            cir.setReturnValue(cir.getReturnValue() * EotsConfig.STAGE_THREE_SPEED_BONUS.get().floatValue());
        }
    }

    @ModifyConstant(method = "blockedByShield", constant = @Constant(intValue = 1000), remap = true)
    private int eotsplus$shieldCooldown(int original) {
        if (EotsConfig.ENABLED.get()) {
            return EotsConfig.SHIELD_COOLDOWN_TICKS.get();
        }
        return original;
    }

    @Override
    public int eotsplus$getAnimRaw() {
        return this.entityData.get(EOTSPLUS$ANIM);
    }

    @Override
    public void eotsplus$setAnimRaw(int raw) {
        this.entityData.set(EOTSPLUS$ANIM, raw);
    }

    @Override
    public void eotsplus$playAnim(int id) {
        this.eotsplus$animSeq = (this.eotsplus$animSeq + 1) & 0xFFFFFF;
        this.entityData.set(EOTSPLUS$ANIM, (this.eotsplus$animSeq << 8) | (id & 0xFF));
        this.eotsplus$chainDirty = true;
    }

    @Override
    public int eotsplus$getAnimId() {
        return EotsAnim.id(this.entityData.get(EOTSPLUS$ANIM));
    }

    @Override
    public float eotsplus$getAnimTicks(float partialTick) {
        return this.tickCount - this.eotsplus$animStart + partialTick;
    }

    @Override
    public int eotsplus$getStage() {
        return this.entityData.get(EOTSPLUS$STAGE);
    }

    @Override
    public void eotsplus$setStage(int stage) {
        this.entityData.set(EOTSPLUS$STAGE, (byte) stage);
    }

    @Override
    public int eotsplus$getChainIndex() {
        return this.entityData.get(EOTSPLUS$CHAIN_INDEX);
    }

    @Override
    public void eotsplus$setChainIndex(int index) {
        this.entityData.set(EOTSPLUS$CHAIN_INDEX, index);
    }

    @Override
    public boolean eotsplus$isManual() {
        return this.eotsplus$manual;
    }

    @Override
    public void eotsplus$setManual(boolean manual) {
        this.eotsplus$manual = manual;
    }

    @Override
    public List<EOTSSegment> eotsplus$getChain() {
        if (this.eotsplus$chain.isEmpty()) {
            this.eotsplus$rebuildChain();
        }
        return this.eotsplus$chain;
    }

    @Override
    public void eotsplus$setAttacking(boolean attacking) {
        this.isAttacking = attacking;
    }

    @Override
    public boolean eotsplus$isAttacking() {
        return this.isAttacking;
    }

    @Override
    public boolean eotsplus$isDying() {
        return this.segmentDeathAnimation;
    }

    @Override
    public void eotsplus$setShouldMove(boolean move) {
        this.shouldMove = move;
    }

    @Override
    public int eotsplus$idleY() {
        return this.getIdleYPos();
    }

    @Override
    public void eotsplus$returnToIdle() {
        this.goToIdlePos();
    }

    @Override
    public void eotsplus$randomizeIdleOffset() {
        this.randomYOffset = this.getRandom().nextInt(7);
    }

    @Override
    public Entity eotsplus$getControllerEntity() {
        return this.getController();
    }
}
