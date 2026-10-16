package com.eotsplus.client.render;

import com.eotsplus.client.fx.FxDraw;
import com.eotsplus.client.fx.FxRenderTypes;
import com.eotsplus.entity.GaleCrescent;
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

public class GaleCrescentRenderer extends EntityRenderer<GaleCrescent> {
    private static final int STEPS = 20;
    private static final double OUTER = 1.25;
    private static final double SWEEP = Math.toRadians(220.0);

    public GaleCrescentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GaleCrescent blade, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float age = blade.tickCount + partial;
        float grow = Math.min(1.0F, age / 4.0F);
        ps.pushPose();
        ps.translate(0, blade.getBbHeight() * 0.5, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-age * 38.0F));
        ps.scale(grow, grow, grow);
        Matrix4f m = ps.last().pose();
        VertexConsumer fx = buffers.getBuffer(FxRenderTypes.FX);
        this.crescent(fx, m, 0.0, 0.5, FxType.WIND, 0.75F, 0.15F);
        this.crescent(fx, m, 0.12, 0.3, FxType.WIND, 0.35F, 0.0F);
        this.crescent(fx, m, -0.12, 0.3, FxType.WIND, 0.35F, 0.0F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.GLOW);
        this.crescent(glow, m, 0.0, 0.12, FxType.DANGER, 0.9F, 0.4F);
        ps.popPose();
        super.render(blade, yaw, partial, ps, buffers, light);
    }

    private void crescent(VertexConsumer vc, Matrix4f m, double y, double width, int color, float edgeAlpha, float innerAlpha) {
        for (int i = 0; i < STEPS; i++) {
            double f0 = i / (double) STEPS;
            double f1 = (i + 1) / (double) STEPS;
            double a0 = -SWEEP * 0.5 + SWEEP * f0;
            double a1 = -SWEEP * 0.5 + SWEEP * f1;
            double w0 = width * Math.sin(Math.PI * f0);
            double w1 = width * Math.sin(Math.PI * f1);
            float fade0 = (float) (0.35 + 0.65 * Math.sin(Math.PI * f0));
            float fade1 = (float) (0.35 + 0.65 * Math.sin(Math.PI * f1));
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            FxDraw.v(vc, m, c0 * OUTER, y, s0 * OUTER, color, edgeAlpha * fade0);
            FxDraw.v(vc, m, c1 * OUTER, y, s1 * OUTER, color, edgeAlpha * fade1);
            FxDraw.v(vc, m, c1 * (OUTER - w1), y, s1 * (OUTER - w1), color, innerAlpha * fade1);
            FxDraw.v(vc, m, c0 * (OUTER - w0), y, s0 * (OUTER - w0), color, innerAlpha * fade0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GaleCrescent blade) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
