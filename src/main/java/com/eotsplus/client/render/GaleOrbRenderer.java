package com.eotsplus.client.render;

import com.eotsplus.client.fx.FxDraw;
import com.eotsplus.client.fx.FxRenderTypes;
import com.eotsplus.entity.GaleOrb;
import com.eotsplus.fx.FxType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class GaleOrbRenderer extends EntityRenderer<GaleOrb> {
    public GaleOrbRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GaleOrb orb, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float age = orb.tickCount + partial;
        float grow = Math.min(1.0F, age / 4.0F);
        ps.pushPose();
        ps.translate(0, orb.getBbHeight() * 0.5, 0);
        ps.scale(grow, grow, grow);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.GLOW);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(age * 9.0F));
        ps.mulPose(Axis.XP.rotationDegrees(age * 5.0F));
        Matrix4f m = ps.last().pose();
        FxDraw.octahedron(glow, m, 0.32, 0.5, 0.32, FxType.WIND, 0.85F, 0xFFFFFF, 1.0F);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-age * 6.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(age * 4.0F));
        m = ps.last().pose();
        FxDraw.octahedron(glow, m, 0.62, 0.8, 0.62, FxType.STORM, 0.25F, FxType.WIND, 0.45F);
        ps.popPose();
        VertexConsumer fx = buffers.getBuffer(FxRenderTypes.FX);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(age * 14.0F));
        ps.mulPose(Axis.XP.rotationDegrees(70.0F));
        m = ps.last().pose();
        FxDraw.ring(fx, m, 0, 0, 0, 0.75, 0.95, 24, FxType.WIND, 0.0F, 0.5F);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(age * 11.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(70.0F));
        m = ps.last().pose();
        FxDraw.ring(fx, m, 0, 0, 0, 0.7, 0.88, 24, FxType.DANGER, 0.0F, 0.4F);
        ps.popPose();
        ps.popPose();
        super.render(orb, yaw, partial, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GaleOrb orb) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
