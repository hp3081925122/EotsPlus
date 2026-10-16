package com.eotsplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EotsConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue ENHANCE_SUMMONED;
    public static final ForgeConfigSpec.DoubleValue SEGMENT_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MAX_LAUNCH_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue STAGE_TWO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue STAGE_THREE_HEALTH;
    public static final ForgeConfigSpec.IntValue ACTION_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue STAGE_TWO_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STAGE_THREE_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STAGE_THREE_SPEED_BONUS;
    public static final ForgeConfigSpec.IntValue MAX_SKILL_HEADS;
    public static final ForgeConfigSpec.DoubleValue COMBAT_RANGE;
    public static final ForgeConfigSpec.IntValue SHIELD_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue STAGE_ROAR;
    public static final ForgeConfigSpec.DoubleValue ROAR_RADIUS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final SkillConfig SKY_REND;
    public static final ForgeConfigSpec.DoubleValue SKY_REND_BODY_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SKY_REND_WAKE_DURATION;

    public static final SkillConfig CHAIN_BITE;
    public static final ForgeConfigSpec.IntValue CHAIN_BITE_COUNT;

    public static final SkillConfig SHATTER_CRYSTAL;
    public static final ForgeConfigSpec.DoubleValue SHATTER_SHARD_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SHATTER_SHARD_COUNT;

    public static final SkillConfig GALE_COIL;
    public static final ForgeConfigSpec.DoubleValue GALE_COIL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue GALE_COIL_BODY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue GALE_COIL_WALL_PULL;

    public static final SkillConfig CRESCENT_BLADE;
    public static final ForgeConfigSpec.IntValue CRESCENT_COUNT;
    public static final ForgeConfigSpec.DoubleValue CRESCENT_RANGE;

    public static final SkillConfig SERPENT_TRAIL;
    public static final ForgeConfigSpec.IntValue SERPENT_TRAIL_DELAY;
    public static final ForgeConfigSpec.DoubleValue SERPENT_TRAIL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SERPENT_TRAIL_CONTACT_MULTIPLIER;

    public static final SkillConfig SCALE_RAIN;
    public static final ForgeConfigSpec.IntValue SCALE_RAIN_WAVES;
    public static final ForgeConfigSpec.IntValue SCALE_RAIN_EXTRA_MARKS;
    public static final ForgeConfigSpec.DoubleValue SCALE_RAIN_RADIUS;

    public static final SkillConfig INHALE;
    public static final ForgeConfigSpec.DoubleValue INHALE_PULL;
    public static final ForgeConfigSpec.DoubleValue INHALE_RANGE;

    public static final SkillConfig SHED_TORNADO;
    public static final ForgeConfigSpec.IntValue SHED_TORNADO_COUNT;
    public static final ForgeConfigSpec.IntValue SHED_TORNADO_LIFETIME;

    public static final SkillConfig STORM_COLLAPSE;
    public static final ForgeConfigSpec.DoubleValue STORM_COLLAPSE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue STORM_COLLAPSE_PULL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("风暴之眼基础设置").push("eots");
        ENABLED = b.comment("总开关，false 时完全保留深层天境原版行为").define("enabled", true);
        ENHANCE_SUMMONED = b.comment("是否增强用指令直接召唤、没有控制器的节段链").define("enhanceSummoned", true);
        SEGMENT_HEALTH = b.comment("每个节段的最大生命值（原版 10），地牢控制器总血量自动同步为 节段数 × 该值").defineInRange("segmentHealth", 16.0, 1.0, 100000.0);
        ATTACK_DAMAGE = b.comment("节段基础攻击力（原版 9），所有技能伤害 = 基础攻击力 × 技能倍率").defineInRange("attackDamage", 9.0, 0.0, 10000.0);
        KNOCKBACK_MULTIPLIER = b.comment("本模组击退整体倍率，实际击退还会乘以 (1 - 击退抗性)").defineInRange("knockbackMultiplier", 0.7, 0.0, 10.0);
        MAX_LAUNCH_VELOCITY = b.comment("本模组击退产生的最大向上速度").defineInRange("maxLaunchVelocity", 0.8, 0.0, 10.0);
        STAGE_TWO_HEALTH = b.comment("进入第二阶段的血量百分比").defineInRange("stageTwoHealth", 0.6, 0.0, 1.0);
        STAGE_THREE_HEALTH = b.comment("进入第三阶段的血量百分比").defineInRange("stageThreeHealth", 0.25, 0.0, 1.0);
        ACTION_INTERVAL = b.comment("同一 Boss 两次技能之间的最短间隔（tick）").defineInRange("actionInterval", 50, 0, 10000);
        STAGE_TWO_COOLDOWN_MULTIPLIER = b.comment("第二阶段冷却与间隔倍率").defineInRange("stageTwoCooldownMultiplier", 0.85, 0.05, 10.0);
        STAGE_THREE_COOLDOWN_MULTIPLIER = b.comment("第三阶段冷却与间隔倍率").defineInRange("stageThreeCooldownMultiplier", 0.65, 0.05, 10.0);
        STAGE_THREE_SPEED_BONUS = b.comment("第三阶段原版游动速度额外倍率").defineInRange("stageThreeSpeedBonus", 1.2, 1.0, 5.0);
        MAX_SKILL_HEADS = b.comment("蛇身被打断成多条后，同一时间最多几个蛇头在释放技能").defineInRange("maxSkillHeads", 2, 1, 64);
        COMBAT_RANGE = b.comment("技能索敌范围").defineInRange("combatRange", 48.0, 8.0, 512.0);
        SHIELD_COOLDOWN_TICKS = b.comment("盾牌格挡节段攻击后的冷却（tick，原版深层天境为 1000），0 为不冷却").defineInRange("shieldCooldownTicks", 100, 0, 6000);
        STAGE_ROAR = b.comment("转阶段时是否释放怒吼").define("stageRoar", true);
        ROAR_RADIUS = b.comment("怒吼风压半径").defineInRange("roarRadius", 12.0, 1.0, 64.0);
        DEBUG_LOGGING = b.comment("输出调试日志").define("debugLogging", false);
        b.pop();

        b.comment("破空俯冲：后仰锁定路线，沿直线贯穿地面预警区域；第二阶段起留下侧向乱流").push("skyRend");
        SKY_REND = new SkillConfig(b, 160, 1.4, 12, 1);
        SKY_REND_BODY_MULTIPLIER = b.comment("蛇身擦过时的伤害倍率").defineInRange("bodyDamageMultiplier", 0.6, 0.0, 1000.0);
        SKY_REND_WAKE_DURATION = b.comment("乱流尾迹持续时间（tick）").defineInRange("wakeDuration", 60, 0, 1200);
        b.pop();

        b.comment("连环噬咬：逼近后缩颈蓄力，再弹射咬击，可连续多次").push("chainBite");
        CHAIN_BITE = new SkillConfig(b, 140, 1.0, 12, 1);
        CHAIN_BITE_COUNT = b.comment("基础咬击次数，第三阶段 +1").defineInRange("count", 3, 1, 10);
        b.pop();

        b.comment("风晶裂弹：口中汇聚风晶，命中后向四周炸出碎晶").push("shatterCrystal");
        SHATTER_CRYSTAL = new SkillConfig(b, 180, 1.2, 10, 1);
        SHATTER_SHARD_MULTIPLIER = b.comment("碎晶伤害倍率").defineInRange("shardDamageMultiplier", 0.5, 0.0, 1000.0);
        SHATTER_SHARD_COUNT = b.comment("基础碎晶数量，阶段越高越多").defineInRange("shardCount", 8, 1, 32);
        b.pop();

        b.comment("缠风绞环：绕目标盘成风环困住敌人，随后收紧绞杀并在中心爆发").push("galeCoil");
        GALE_COIL = new SkillConfig(b, 260, 1.1, 9, 1);
        GALE_COIL_RADIUS = b.comment("风环半径").defineInRange("radius", 9.0, 4.0, 32.0);
        GALE_COIL_BODY_MULTIPLIER = b.comment("盘绕时蛇身碰撞伤害倍率").defineInRange("bodyDamageMultiplier", 0.5, 0.0, 1000.0);
        GALE_COIL_WALL_PULL = b.comment("风墙向内牵引强度").defineInRange("wallPull", 0.05, 0.0, 1.0);
        b.pop();

        b.comment("回旋风刃：扇形甩出贴地飞行的风刃，飞到尽头后折返，去程和回程各命中一次；第二阶段起两轮").push("crescentBlade");
        CRESCENT_BLADE = new SkillConfig(b, 200, 0.8, 10, 1);
        CRESCENT_COUNT = b.comment("每轮风刃数量，第三阶段 +2").defineInRange("count", 3, 1, 16);
        CRESCENT_RANGE = b.comment("风刃飞行距离").defineInRange("range", 14.0, 4.0, 48.0);
        b.pop();

        b.comment("蛇行雷痕：S 形低空掠过目标区域，沿途留下依次引爆的风雷节点").push("serpentTrail");
        SERPENT_TRAIL = new SkillConfig(b, 240, 0.9, 9, 1);
        SERPENT_TRAIL_DELAY = b.comment("节点落下到引爆的延迟（tick），第三阶段 -6").defineInRange("delay", 26, 8, 200);
        SERPENT_TRAIL_RADIUS = b.comment("节点爆炸半径").defineInRange("radius", 2.4, 0.5, 8.0);
        SERPENT_TRAIL_CONTACT_MULTIPLIER = b.comment("掠过时蛇头撞击伤害倍率").defineInRange("contactMultiplier", 0.6, 0.0, 1000.0);
        b.pop();

        b.comment("逆鳞风雨：抬身竖鳞，鳞片升空后砸向地面预警圈").push("scaleRain");
        SCALE_RAIN = new SkillConfig(b, 320, 0.9, 8, 1);
        SCALE_RAIN_WAVES = b.comment("波数").defineInRange("waves", 3, 1, 20);
        SCALE_RAIN_EXTRA_MARKS = b.comment("每名玩家附近额外的随机落点数").defineInRange("extraMarks", 2, 0, 20);
        SCALE_RAIN_RADIUS = b.comment("落点半径").defineInRange("radius", 2.5, 0.5, 16.0);
        b.pop();

        b.comment("吞风吐息：张口吸入扇形区域内的敌人，随后猛烈吐出狂风").push("inhale");
        INHALE = new SkillConfig(b, 360, 1.3, 9, 2);
        INHALE_PULL = b.comment("吸入牵引强度").defineInRange("pull", 0.07, 0.0, 1.0);
        INHALE_RANGE = b.comment("吸入与吐息距离").defineInRange("range", 20.0, 4.0, 64.0);
        b.pop();

        b.comment("蜕鳞旋风：蛇身震颤蜕下尾鳞，化为追踪玩家的旋风").push("shedTornado");
        SHED_TORNADO = new SkillConfig(b, 420, 0.35, 7, 2);
        SHED_TORNADO_COUNT = b.comment("基础旋风数量，第三阶段 +1").defineInRange("count", 2, 1, 10);
        SHED_TORNADO_LIFETIME = b.comment("旋风持续时间（tick）").defineInRange("lifetime", 220, 20, 2400);
        b.pop();

        b.comment("风暴坍缩（第三阶段大招）：盘旋成风柱牵引全场，随后从风眼正中俯冲砸地，越靠近中心伤害越高").push("stormCollapse");
        STORM_COLLAPSE = new SkillConfig(b, 700, 2.4, 6, 3);
        STORM_COLLAPSE_RADIUS = b.comment("冲击波半径").defineInRange("radius", 18.0, 4.0, 64.0);
        STORM_COLLAPSE_PULL = b.comment("风柱牵引强度").defineInRange("pull", 0.045, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private EotsConfig() {
    }

    public static final class SkillConfig {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.DoubleValue damageMultiplier;
        public final ForgeConfigSpec.IntValue weight;
        public final ForgeConfigSpec.IntValue minStage;

        SkillConfig(ForgeConfigSpec.Builder b, int cooldown, double damage, int weight, int minStage) {
            this.enabled = b.comment("是否启用").define("enabled", true);
            this.cooldown = b.comment("冷却（tick）").defineInRange("cooldown", cooldown, 0, 100000);
            this.damageMultiplier = b.comment("伤害倍率（伤害 = 基础攻击力 × 该值）").defineInRange("damageMultiplier", damage, 0.0, 1000.0);
            this.weight = b.comment("选择权重").defineInRange("weight", weight, 0, 1000);
            this.minStage = b.comment("最早可用阶段（1~3）").defineInRange("minStage", minStage, 1, 3);
        }
    }
}
