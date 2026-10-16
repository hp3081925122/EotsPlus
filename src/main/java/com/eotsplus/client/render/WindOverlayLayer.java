package com.eotsplus.client.render;

import com.eotsplus.client.anim.EotsPose;
import com.eotsplus.config.EotsClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

public class WindOverlayLayer extends RenderLayer<EOTSSegment, EntityModel<EOTSSegment>> {
    private static final ResourceLocation SWIRL = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    public WindOverlayLayer(RenderLayerParent<EOTSSegment, EntityModel<EOTSSegment>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, EOTSSegment seg, float limbSwing, float limbSwingAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!EotsClientConfig.ENABLE_WIND_OVERLAY.get() || seg.isInvisible()) {
            return;
        }
        EotsPose pose = EotsPose.INSTANCE.compute(seg, partial);
        float strength = pose.overlay;
        if (strength <= 0.01F) {
            return;
        }
        float scroll = ageInTicks * 0.012F;
        VertexConsumer vc = buffers.getBuffer(RenderType.energySwirl(SWIRL, scroll, scroll * 0.6F));
        this.getParentModel().renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, net.minecraft.util.FastColor.ARGB32.color(255, (int) (255 * 0.35F * strength), (int) (255 * 0.75F * strength), (int) (255 * 0.9F * strength)));
    }
}
