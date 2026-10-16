package com.eotsplus.ai;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class EotsHazards {
    private static final Map<ServerLevel, List<Hazard>> ACTIVE = new WeakHashMap<>();

    private EotsHazards() {
    }

    public static void add(ServerLevel level, Hazard hazard) {
        ACTIVE.computeIfAbsent(level, k -> new ArrayList<>()).add(hazard);
    }

    public static void tick(ServerLevel level) {
        List<Hazard> list = ACTIVE.get(level);
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Hazard> snapshot = new ArrayList<>(list);
        list.clear();
        Iterator<Hazard> it = snapshot.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            if (h.tick(level)) {
                it.remove();
            }
        }
        list.addAll(0, snapshot);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public interface Hazard {
        boolean tick(ServerLevel level);
    }
}
