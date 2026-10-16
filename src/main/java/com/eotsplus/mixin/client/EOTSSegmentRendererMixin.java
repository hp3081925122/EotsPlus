package com.eotsplus.mixin.client;

import com.eotsplus.client.anim.EotsPose;
import com.eotsplus.config.EotsClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.razordevs.deep_aether.client.renderer.entity.EOTSSegmentRenderer;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

@Mixin(value = EOTSSegmentRenderer.class, remap = false)
public abstract class EOTSSegmentRendererMixin {
    @Inject(method = "setupRotations(Lio/github/razordevs/deep_aether/entity/living/boss/eots/EOTSSegment;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"), require = 1)
    private void eotsplus$pose(EOTSSegment seg, PoseStack ps, float ageInTicks, float rotationYaw, float partialTicks, float scale, CallbackInfo ci) {
        if (!EotsClientConfig.ENABLE_ANIMATIONS.get()) {
            return;
        }
        EotsPose pose = EotsPose.INSTANCE.compute(seg, partialTicks);
        if (!pose.active) {
            return;
        }
        ps.translate(0.0F, 0.5F, 0.0F);
        if (pose.bodyPitch != 0.0F) {
            ps.mulPose(Axis.XP.rotationDegrees(pose.bodyPitch));
        }
        if (pose.bodyRoll != 0.0F) {
            ps.mulPose(Axis.ZP.rotationDegrees(pose.bodyRoll));
        }
        if (pose.bodyScale != 1.0F) {
            ps.scale(pose.bodyScale, pose.bodyScale, pose.bodyScale);
        }
        if (pose.shake > 0.0F) {
            float t = ageInTicks + seg.getId();
            ps.translate(Mth.sin(t * 2.9F) * pose.shake, Mth.sin(t * 3.7F) * pose.shake, Mth.sin(t * 3.3F) * pose.shake);
        }
        ps.translate(0.0F, -0.5F, 0.0F);
    }
}
