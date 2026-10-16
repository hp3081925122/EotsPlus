package com.eotsplus.fx;

import com.eotsplus.network.EotsNetwork;
import com.eotsplus.network.FxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class EotsFx {
    private static final double RANGE = 128.0;

    private EotsFx() {
    }

    public static void circle(ServerLevel level, Vec3 c, float radius, int duration, int color) {
        EotsNetwork.sendNear(level, c.x, c.y, c.z, RANGE, new FxPacket(FxType.CIRCLE, c.x, c.y, c.z, 0, 0, 0, radius, 0, duration, color));
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, float width, int duration, int color) {
        EotsNetwork.sendNear(level, from.x, from.y, from.z, RANGE, new FxPacket(FxType.LINE, from.x, from.y, from.z, (float) to.x, (float) to.y, (float) to.z, width, 0, duration, color));
    }

    public static void fan(ServerLevel level, Vec3 origin, Vec3 dir, float length, float halfAngleDeg, int duration, int color) {
        EotsNetwork.sendNear(level, origin.x, origin.y, origin.z, RANGE, new FxPacket(FxType.FAN, origin.x, origin.y, origin.z, (float) dir.x, (float) dir.y, (float) dir.z, length, halfAngleDeg, duration, color));
    }

    public static void shockwave(ServerLevel level, Vec3 c, float radius, int duration, int color) {
        EotsNetwork.sendNear(level, c.x, c.y, c.z, RANGE, new FxPacket(FxType.SHOCKWAVE, c.x, c.y, c.z, 0, 0, 0, radius, 0, duration, color));
    }

    public static void column(ServerLevel level, Vec3 base, float radius, float height, int duration, int color) {
        EotsNetwork.sendNear(level, base.x, base.y, base.z, RANGE, new FxPacket(FxType.COLUMN, base.x, base.y, base.z, 0, 0, 0, radius, height, duration, color));
    }

    public static void shake(ServerLevel level, Vec3 c, float intensity, float radius, int duration) {
        EotsNetwork.sendNear(level, c.x, c.y, c.z, RANGE, new FxPacket(FxType.SHAKE, c.x, c.y, c.z, intensity, 0, 0, radius, 0, duration, 0));
    }
}
