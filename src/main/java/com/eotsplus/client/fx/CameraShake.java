package com.eotsplus.client.fx;

import com.eotsplus.config.EotsClientConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CameraShake {
    private static final List<Shake> SHAKES = new ArrayList<>();
    private static int clock;

    private CameraShake() {
    }

    public static void add(Vec3 pos, float intensity, float radius, int duration) {
        SHAKES.add(new Shake(pos, intensity, radius, Math.max(1, duration)));
    }

    public static void tick() {
        clock++;
        Iterator<Shake> it = SHAKES.iterator();
        while (it.hasNext()) {
            Shake s = it.next();
            s.age++;
            if (s.age >= s.duration) {
                it.remove();
            }
        }
    }

    public static void clear() {
        SHAKES.clear();
    }

    public static float strength(Vec3 cam, float partial) {
        double mult = EotsClientConfig.CAMERA_SHAKE.get();
        if (mult <= 0.0 || SHAKES.isEmpty()) {
            return 0.0F;
        }
        float total = 0.0F;
        for (Shake s : SHAKES) {
            double d = s.pos.distanceTo(cam);
            if (d >= s.radius) {
                continue;
            }
            float life = 1.0F - (s.age + partial) / s.duration;
            if (life <= 0.0F) {
                continue;
            }
            total += (float) (s.intensity * life * (1.0 - d / s.radius));
        }
        return (float) (Math.min(total, 2.5F) * mult);
    }

    public static float time(float partial) {
        return clock + partial;
    }

    private static final class Shake {
        final Vec3 pos;
        final float intensity;
        final float radius;
        final int duration;
        int age;

        Shake(Vec3 pos, float intensity, float radius, int duration) {
            this.pos = pos;
            this.intensity = intensity;
            this.radius = radius;
            this.duration = duration;
        }
    }
}
