package com.eotsplus;

import com.eotsplus.ai.EotsBrain;
import com.eotsplus.ai.EotsHazards;
import com.eotsplus.ai.EotsSkillGoal;
import com.eotsplus.config.EotsClientConfig;
import com.eotsplus.config.EotsConfig;
import com.eotsplus.entity.EotsEntities;
import com.eotsplus.network.EotsNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import teamrazor.deepaether.entity.eots.EOTSController;
import teamrazor.deepaether.entity.eots.EOTSSegment;

@Mod(EotsPlus.MOD_ID)
public class EotsPlus {
    public static final String MOD_ID = "eotsplus";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final int BASE_SEGMENTS = 22;

    public EotsPlus() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EotsConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EotsClientConfig.SPEC);
        EotsEntities.ENTITIES.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(EotsNetwork::register);
    }

    public static boolean isEnhanced(EOTSSegment segment) {
        if (segment == null || segment.level().isClientSide() || !EotsConfig.ENABLED.get()) {
            return false;
        }
        return segment.getController() != null || EotsConfig.ENHANCE_SUMMONED.get();
    }

    private void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !EotsConfig.ENABLED.get()) {
            return;
        }
        if (event.getEntity() instanceof EOTSSegment segment) {
            boolean present = segment.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof EotsSkillGoal);
            if (!present) {
                segment.goalSelector.addGoal(0, new EotsSkillGoal(segment));
            }
            AttributeInstance health = segment.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                float oldMax = segment.getMaxHealth();
                health.setBaseValue(EotsConfig.SEGMENT_HEALTH.get());
                if (segment.getHealth() >= oldMax - 0.01F) {
                    segment.setHealth(segment.getMaxHealth());
                }
            }
            AttributeInstance attack = segment.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(EotsConfig.ATTACK_DAMAGE.get());
            }
        } else if (event.getEntity() instanceof EOTSController controller) {
            AttributeInstance health = controller.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(EotsConfig.SEGMENT_HEALTH.get() * BASE_SEGMENTS + 0.1);
                if (!controller.isBossFight()) {
                    controller.setHealth(controller.getMaxHealth());
                }
            }
        }
    }

    private void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        EotsHazards.tick(level);
        if (level.getGameTime() % 100L == 0L) {
            EotsBrain.prune(level);
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        EotsHazards.clear();
        EotsBrain.clear();
    }
}
