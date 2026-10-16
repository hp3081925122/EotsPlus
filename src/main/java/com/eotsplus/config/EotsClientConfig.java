package com.eotsplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EotsClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_FX;
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_WIND_OVERLAY;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("客户端特效").push("vfx");
        ENABLE_FX = b.comment("启用地面预警、风柱、冲击波等自定义渲染特效").define("enableFx", true);
        ENABLE_ANIMATIONS = b.comment("启用 Boss 技能动画").define("enableAnimations", true);
        ENABLE_WIND_OVERLAY = b.comment("启用 Boss 身上的风能覆盖层").define("enableWindOverlay", true);
        CAMERA_SHAKE = b.comment("镜头震动强度倍率").defineInRange("cameraShake", 1.0, 0.0, 5.0);
        b.pop();
        SPEC = b.build();
    }

    private EotsClientConfig() {
    }
}
