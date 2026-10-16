package com.eotsplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EotsClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_FX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WIND_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
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
