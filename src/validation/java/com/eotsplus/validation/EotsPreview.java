package com.eotsplus.validation;

import com.eotsplus.EotsPlus;
import com.eotsplus.client.fx.FxManager;
import com.eotsplus.duck.EotsSegmentDuck;
import com.eotsplus.entity.EotsEntities;
import com.eotsplus.fx.FxType;
import com.eotsplus.network.FxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.hp.renderpreview.PreviewRegistry;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;
import io.github.razordevs.deep_aether.init.DAEntities;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EotsPlus.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EotsPreview {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (int animation = 0; animation <= 22; animation++) {
                int selected = animation;
                PreviewRegistry.register("eotsplus", "animation_" + animation, Component.literal("EOTS animation " + animation), 100,
                        (level, viewer) -> new EntityEffect(level, DAEntities.EOTS_SEGMENT.get(), selected));
            }
            registerEntity("gale_orb", EotsEntities.GALE_ORB.get());
            registerEntity("gale_shard", EotsEntities.GALE_SHARD.get());
            registerEntity("gale_crescent", EotsEntities.GALE_CRESCENT.get());
            registerEntity("gale_tornado", EotsEntities.GALE_TORNADO.get());
            for (int type = 0; type <= 4; type++) {
                int selected = type;
                PreviewRegistry.register("eotsplus", "fx_" + type, Component.literal("EOTS FX " + type), 80,
                        (level, viewer) -> new FxEffect(selected));
            }
        });
    }

    private static void registerEntity(String id, EntityType<?> type) {
        PreviewRegistry.register("eotsplus", id, Component.literal(id), 100, (level, viewer) -> new EntityEffect(level, type, 0));
    }

    private static final class EntityEffect implements PreviewRegistry.Effect {
        private final Entity entity;
        private final int animation;
        EntityEffect(ClientLevel level, EntityType<?> type, int animation) {
            this.entity = type.create(level);
            this.entity.setId(1000);
            this.animation = animation;
            entity.setPos(0, 1, 0);
            if (entity instanceof EOTSSegment segment) {
                segment.setControllingSegment(true);
                EotsSegmentDuck duck = EotsSegmentDuck.of(segment);
                duck.eotsplus$setAnimRaw(animation);
                duck.eotsplus$setStage(3);
            }
        }
        @Override public void seek(int tick) {
            entity.tickCount = tick;
            if (entity instanceof EOTSSegment segment) {
                EotsSegmentDuck.of(segment).eotsplus$setAnimRaw(animation);
            }
        }
        @Override public void submit(PoseStack pose, MultiBufferSource buffers, Camera camera) {
            com.mojang.blaze3d.platform.GlStateManager.setupLevelDiffuseLighting(
                    new org.joml.Vector3f(0.2F, 1.0F, -0.7F).normalize(),
                    new org.joml.Vector3f(-0.2F, 1.0F, 0.7F).normalize(),
                    new org.joml.Matrix4f().rotationYXZ(1.0821041F, 3.2375858F, 0.0F)
                            .rotateYXZ(-(float) Math.PI / 8F, 2.3561945F, 0.0F));
            Vec3 p = camera.getPosition();
            Minecraft.getInstance().getEntityRenderDispatcher().render(entity, entity.getX()-p.x, entity.getY()-p.y, entity.getZ()-p.z,
                    entity.getYRot(), 0, pose, buffers, LightTexture.FULL_BRIGHT);
        }
        @Override public double cameraDistance() { return entity instanceof EOTSSegment ? 10 : 16; }
    }

    private static final class FxEffect implements PreviewRegistry.Effect {
        private final int type;
        private int tick;
        FxEffect(int type) { this.type = type; }
        @Override public void seek(int tick) { this.tick = tick; }
        @Override public double cameraDistance() { return 24; }
        @Override public float cameraPitch() { return 30; }
        @Override public void submit(PoseStack pose, MultiBufferSource buffers, Camera camera) {
            try {
                Field field = FxManager.class.getDeclaredField("ACTIVE");
                field.setAccessible(true);
                List<Object> active = (List<Object>) field.get(null);
                List<Object> previous = new ArrayList<>(active);
                try {
                    active.clear();
                    float a = type == FxType.LINE ? 9 : 1;
                    float b = type == FxType.FAN ? 0 : 0.05F;
                    float c = type == FxType.LINE ? 0 : 0;
                    FxManager.accept(new FxPacket(type, type == FxType.LINE ? -9 : 0, 0.05, 0,
                            a, b, c, type == FxType.FAN ? 12 : 6, type == FxType.COLUMN ? 16 : 50,
                            80, FxType.WIND));
                    for (Object fx : active) {
                        Field age = fx.getClass().getDeclaredField("age");
                        age.setAccessible(true);
                        age.setInt(fx, tick);
                    }
                    FxManager.render(pose, camera.getPosition(), 0);
                } finally {
                    active.clear();
                    active.addAll(previous);
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Preview state failed", error);
            }
        }
    }
}
