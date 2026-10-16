package com.eotsplus.client;

import com.eotsplus.EotsPlus;
import com.eotsplus.client.fx.CameraShake;
import com.eotsplus.client.fx.FxManager;
import com.eotsplus.client.render.GaleCrescentRenderer;
import com.eotsplus.client.render.GaleOrbRenderer;
import com.eotsplus.client.render.GaleShardRenderer;
import com.eotsplus.client.render.GaleTornadoRenderer;
import com.eotsplus.client.render.WindOverlayLayer;
import com.eotsplus.entity.EotsEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;
import io.github.razordevs.deep_aether.init.DAEntities;

public final class EotsClient {
    private EotsClient() {
    }

    @EventBusSubscriber(modid = EotsPlus.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EotsEntities.GALE_ORB.get(), GaleOrbRenderer::new);
            event.registerEntityRenderer(EotsEntities.GALE_SHARD.get(), GaleShardRenderer::new);
            event.registerEntityRenderer(EotsEntities.GALE_TORNADO.get(), GaleTornadoRenderer::new);
            event.registerEntityRenderer(EotsEntities.GALE_CRESCENT.get(), GaleCrescentRenderer::new);
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            LivingEntityRenderer<EOTSSegment, EntityModel<EOTSSegment>> renderer = event.getRenderer(DAEntities.EOTS_SEGMENT.get());
            if (renderer != null) {
                renderer.addLayer(new WindOverlayLayer(renderer));
            }
        }
    }

    @EventBusSubscriber(modid = EotsPlus.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) {
                return;
            }
            FxManager.tick();
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                return;
            }
            FxManager.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }

        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            float partial = (float) event.getPartialTick();
            Vec3 cam = event.getCamera().getPosition();
            float s = CameraShake.strength(cam, partial);
            if (s <= 0.0F) {
                return;
            }
            float t = CameraShake.time(partial);
            float amp = s * 1.6F;
            event.setYaw(event.getYaw() + Mth.sin(t * 1.9F) * amp);
            event.setPitch(event.getPitch() + Mth.sin(t * 2.7F + 1.3F) * amp);
            event.setRoll(event.getRoll() + Mth.sin(t * 2.3F + 2.1F) * amp * 0.6F);
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            FxManager.clear();
        }
    }
}
