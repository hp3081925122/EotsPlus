# Eye of the Storm Plus

Deep Aether 的风暴之眼 Boss 强化附属模组。加入三阶段战斗、10 个战斗技能、转阶段怒吼及额外风暴特效。

## 版本

- `1201forge`：Minecraft 1.20.1，Forge，Java 17。
- `1211neoforge`：Minecraft 1.21.1，NeoForge，Java 21，已完成迁移，记录见 [迁移与验证文档](MIGRATION_1211_NEOFORGE.md)。

## 功能

破空俯冲、风晶裂弹、缠风绞环、回旋风刃、蛇行雷痕、逆鳞风雨、风眼连珠、裂空风矛、风棱阵、蜕鳞旋风、风暴坍缩，以及阶段切换怒吼。待机悬浮高度默认降为控制器上方 8 格（可配置）。默认阶段阈值为 60% 和 25% 生命值；属性、阶段和技能参数支持配置。同一控制器下的节段按攻击者共用一份无敌帧（默认 10 tick，可配置），范围攻击同时打中多节只结算一次；另有独立无敌帧（默认关闭），可限制无视无敌帧的伤害。

需要 Deep Aether 与 The Aether，以及其对应版本的前置依赖。

## 构建

使用对应版本的 JDK 执行 `./gradlew build`。1.21.1 使用 NeoForge 21.1.251 / ModDevGradle 2.0.147，开发依赖坐标固定在构建配置中。`./gradlew runGameTestServer -PvalidateMigration` 可执行独立服务端迁移测试；验证源码不会进入正式 JAR。

CurseForge：https://www.curseforge.com/minecraft/mc-mods/eye-of-the-storm-plus

项目 ID：1747659。1.20.1 首次上传文件 ID：9204075。

## 许可

All Rights Reserved。
