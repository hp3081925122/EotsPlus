package com.eotsplus.ai;

import com.eotsplus.EotsPlus;
import com.eotsplus.config.EotsConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSController;
import io.github.razordevs.deep_aether.entity.living.boss.eots.EOTSSegment;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EotsBrain {
    private static final UUID WILD = new UUID(0L, 0L);
    private static final Map<ResourceKey<Level>, Map<UUID, EotsBrain>> BRAINS = new HashMap<>();

    public final UUID key;
    public int stage = 1;
    public int pendingRoar;
    public long nextAction;
    public UUID bigOwner;
    public final Set<UUID> activeHeads = new HashSet<>();
    public final Map<SkillType, Long> ready = new EnumMap<>(SkillType.class);
    private double peakMax;
    private long lastStageCheck = -100L;
    private long lastSeen;

    private EotsBrain(UUID key) {
        this.key = key;
    }

    public static EotsBrain of(EOTSSegment head) {
        ServerLevel level = (ServerLevel) head.level();
        Map<UUID, EotsBrain> map = BRAINS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        EOTSController controller = head.getController();
        UUID key = WILD;
        if (controller != null) {
            key = controller.getUUID();
        }
        EotsBrain brain = map.computeIfAbsent(key, EotsBrain::new);
        brain.lastSeen = level.getGameTime();
        return brain;
    }

    public static void prune(ServerLevel level) {
        Map<UUID, EotsBrain> map = BRAINS.get(level.dimension());
        if (map == null) {
            return;
        }
        long now = level.getGameTime();
        Iterator<EotsBrain> it = map.values().iterator();
        while (it.hasNext()) {
            EotsBrain b = it.next();
            if (now - b.lastSeen > 200L) {
                it.remove();
            }
        }
    }

    public static void clear() {
        BRAINS.clear();
    }

    public void update(EOTSSegment head) {
        ServerLevel level = (ServerLevel) head.level();
        long now = level.getGameTime();
        if (now - this.lastStageCheck < 10L) {
            return;
        }
        this.lastStageCheck = now;
        double fraction = this.healthFraction(head);
        int newStage = 1;
        if (fraction <= EotsConfig.STAGE_THREE_HEALTH.get()) {
            newStage = 3;
        } else if (fraction <= EotsConfig.STAGE_TWO_HEALTH.get()) {
            newStage = 2;
        }
        if (newStage > this.stage) {
            if (EotsConfig.DEBUG_LOGGING.get()) {
                EotsPlus.LOGGER.info("[EotsPlus] brain {} stage {} -> {} (health fraction {})", this.key, this.stage, newStage, String.format("%.3f", fraction));
            }
            this.stage = newStage;
            if (EotsConfig.STAGE_ROAR.get()) {
                this.pendingRoar = newStage;
            }
        }
    }

    private double healthFraction(EOTSSegment head) {
        EOTSController controller = head.getController();
        if (controller != null) {
            if (controller.getMaxHealth() <= 0.0F) {
                return 1.0;
            }
            return controller.getHealth() / controller.getMaxHealth();
        }
        ServerLevel level = (ServerLevel) head.level();
        List<? extends EOTSSegment> wild = level.getEntities(EntityTypeTest.forClass(EOTSSegment.class), s -> s.isAlive() && s.getController() == null);
        double sum = 0.0;
        double max = 0.0;
        for (EOTSSegment s : wild) {
            sum += s.getHealth();
            max += s.getMaxHealth();
        }
        if (max > this.peakMax) {
            this.peakMax = max;
        }
        if (this.peakMax <= 0.0) {
            return 1.0;
        }
        return sum / this.peakMax;
    }

    public double cooldownMultiplier() {
        if (this.stage >= 3) {
            return EotsConfig.STAGE_THREE_COOLDOWN_MULTIPLIER.get();
        }
        if (this.stage == 2) {
            return EotsConfig.STAGE_TWO_COOLDOWN_MULTIPLIER.get();
        }
        return 1.0;
    }

    public boolean isReady(SkillType type, long now) {
        Long t = this.ready.get(type);
        return t == null || now >= t;
    }
}
