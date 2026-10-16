package com.eotsplus.mixin.client;

import com.eotsplus.client.anim.EotsPose;
import com.eotsplus.config.EotsClientConfig;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamrazor.deepaether.client.model.EOTSSegmentModel;
import teamrazor.deepaether.entity.eots.EOTSSegment;

@Mixin(value = EOTSSegmentModel.class, remap = false)
public abstract class EOTSSegmentModelMixin {
    @Shadow
    @Final
    private ModelPart bb_main;
    @Shadow
    @Final
    private ModelPart bb_segment;
    @Shadow
    @Final
    private ModelPart upperMouth;
    @Shadow
    @Final
    private ModelPart lowerMouth;

    @Unique
    private ModelPart eotsplus$frillLeft;
    @Unique
    private ModelPart eotsplus$frillRight;
    @Unique
    private ModelPart eotsplus$frillRightLow;
    @Unique
    private ModelPart eotsplus$fin;
    @Unique
    private ModelPart eotsplus$head;

    @Unique
    private void eotsplus$resolve() {
        if (this.eotsplus$head != null) {
            return;
        }
        this.eotsplus$head = this.bb_main.getChild("Head");
        this.eotsplus$frillLeft = this.bb_main.getChild("Head10");
        this.eotsplus$frillRight = this.bb_main.getChild("Head11");
        this.eotsplus$frillRightLow = this.bb_main.getChild("Head2");
        this.eotsplus$fin = this.bb_main.getChild("cube_r1");
    }

    @Inject(method = {"setupAnim(Lteamrazor/deepaether/entity/eots/EOTSSegment;FFFFF)V", "m_6973_(Lteamrazor/deepaether/entity/eots/EOTSSegment;FFFFF)V"}, at = @At("TAIL"), require = 1)
    private void eotsplus$animate(EOTSSegment entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        this.eotsplus$resolve();
        this.bb_main.resetPose();
        this.bb_segment.resetPose();
        this.eotsplus$head.resetPose();
        this.eotsplus$frillLeft.resetPose();
        this.eotsplus$frillRight.resetPose();
        this.eotsplus$frillRightLow.resetPose();
        this.eotsplus$fin.resetPose();
        if (!EotsClientConfig.ENABLE_ANIMATIONS.get()) {
            return;
        }
        float partial = ageInTicks - entity.tickCount;
        EotsPose pose = EotsPose.INSTANCE.compute(entity, partial);
        if (!pose.active) {
            return;
        }
        if (pose.head) {
            this.bb_main.xRot += pose.headPitch;
            this.bb_main.yRot += pose.headYaw;
            this.bb_main.z += pose.headPull;
            this.bb_main.xScale = pose.headScale;
            this.bb_main.yScale = pose.headScale;
            this.bb_main.zScale = pose.headScale;
            this.upperMouth.xRot = -pose.jaw;
            this.lowerMouth.xRot = pose.jaw * 1.2F;
            this.eotsplus$frillLeft.yRot -= pose.frill * 0.5F;
            this.eotsplus$frillLeft.zRot -= pose.frill * 0.35F;
            this.eotsplus$frillRight.yRot += pose.frill * 0.5F;
            this.eotsplus$frillRight.zRot += pose.frill * 0.35F;
            this.eotsplus$frillRightLow.yRot += pose.frill * 0.5F;
            this.eotsplus$frillRightLow.zRot += pose.frill * 0.35F;
            this.eotsplus$fin.xRot += pose.fin;
        } else {
            this.bb_segment.xScale = 1.0F + 0.45F * pose.finFlare;
            this.bb_segment.yScale = 1.0F + 0.25F * pose.finFlare;
        }
    }
}
