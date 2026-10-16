package com.eotsplus.client.render;

import com.eotsplus.client.fx.FxDraw;
import com.eotsplus.client.fx.FxRenderTypes;
import com.eotsplus.entity.GaleShard;
import com.eotsplus.fx.FxType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GaleShardRenderer extends EntityRenderer<GaleShard> {
    public GaleShardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GaleShard shard, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.translate(0, shard.getBbHeight() * 0.5, 0);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, shard.yRotO, shard.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, shard.xRotO, shard.getXRot())));
        ps.mulPose(Axis.ZP.rotationDegrees((shard.tickCount + partial) * 20.0F));
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.GLOW);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(90.0F));
        FxDraw.octahedron(glow, ps.last().pose(), 0.12, 0.45, 0.12, FxType.WIND, 0.8F, 0xFFFFFF, 1.0F);
        ps.popPose();
        ps.popPose();
        super.render(shard, yaw, partial, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GaleShard shard) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
