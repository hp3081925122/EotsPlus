package com.eotsplus.client.render;

import com.eotsplus.client.fx.FxDraw;
import com.eotsplus.client.fx.FxRenderTypes;
import com.eotsplus.entity.GaleTornado;
import com.eotsplus.fx.FxType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class GaleTornadoRenderer extends EntityRenderer<GaleTornado> {
    public GaleTornadoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GaleTornado tornado, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float age = tornado.tickCount + partial;
        float grow = Math.min(1.0F, age / 15.0F);
        double h = GaleTornado.HEIGHT * grow;
        VertexConsumer vc = buffers.getBuffer(FxRenderTypes.FX);
        Matrix4f m = ps.last().pose();
        FxDraw.spiral(vc, m, 0, 0, 0, 0.35, 2.3, h, 3, 0.9, age * 0.55, 0.9, 32, FxType.WIND, 0.6F * grow);
        FxDraw.spiral(vc, m, 0, 0, 0, 0.6, 2.7, h, 2, 0.7, -age * 0.4, 0.5, 32, FxType.DANGER, 0.35F * grow);
        FxDraw.ring(vc, m, 0, 0.05, 0, 0.3, GaleTornado.PULL_RADIUS, 40, FxType.WIND, 0.25F * grow, 0.0F);
        FxDraw.ring(vc, m, 0, 0.06, 0, GaleTornado.CORE_RADIUS - 0.12, GaleTornado.CORE_RADIUS, 32, FxType.DANGER, 0.6F * grow, 0.6F * grow);
        super.render(tornado, yaw, partial, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GaleTornado tornado) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
