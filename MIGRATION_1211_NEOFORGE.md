# 1.21.1 NeoForge 迁移

创建日期：2026-10-16。

## 范围与原则

源版本为 Minecraft 1.20.1 Forge，目标为 Minecraft 1.21.1 NeoForge，使用官方映射及 Java 21。保留 modId、注册 ID、配置语义、11 项技能实现、阶段百分比、伤害倍率、实体行为、网络同步、粒子和渲染效果，不以空实现或删功能解决 API 差异。

源工作区：`1201-forge-EotsPlus`，分支 `1201forge`。

目标工作区：`1.21.1-neoforge-EotsPlus`，分支 `1211neoforge`。

## 迁移步骤

1. 保存并公开源版本代码，建立独立 Git 工作树。
2. 使用本地 1.21.1 NeoForge 模板替换构建与加载器配置，确认对应版本 Deep Aether、Aether 及前置依赖源码。
3. 迁移事件、注册、配置、网络 Payload、实体同步、Mixin、渲染和粒子 API。
4. 检查资源目录与 pack.mcmeta 格式，核对全部技能、参数与阶段行为。
5. 构建、服务端启动与技能测试，使用 Render Preview 工程核对渲染。分别记录静态、构建、服务端和可视化证据。

## 功能对等清单

- [x] 破空俯冲：预警直线、冲刺、风尾迹与乱流。
- [x] 连环噬咬：蓄力、多次咬击及阶段追加。
- [x] 风晶裂弹：蓄力粒子、弹体、分裂碎片。
- [x] 缠风绞环：盘绕、风墙、牵引、收紧和中心爆发。
- [x] 回旋风刃：扇形发射、返回路径及双程碰撞。
- [x] 蛇行雷痕：S 形移动、延迟节点和阶段变化。
- [x] 逆鳞风雨：多波落点预警与下落碎片。
- [x] 吞风吐息：扇形牵引、吐息伤害与击退。
- [x] 蜕鳞旋风：追踪旋风与阶段数量变化。
- [x] 风暴坍缩：风柱、牵引、俯冲、距离衰减爆发与震屏。
- [x] 怒吼：转阶段触发、声音、冲击波、击退与震屏。
- [x] 百分比阶段、生命和攻击配置、攻击力倍率、击退抗性。
- [x] 特效网络同步、实体生成/存档、全部渲染器和资源。

## 验证记录

清单勾选表示已完成源码功能对等检查，不代表运行验证全部通过。

- 公开仓库：https://github.com/hp3081925122/EotsPlus 。源分支提交 `1710e1c`。
- 源版 1.20.1 已构建成功并上传 CurseForge 文件 9204075。
- 目标 Java 21 / NeoForge 21.1.251，`build compileValidationJava` 成功。
- Deep Aether 目标类包改为 `io.github.razordevs.deep_aether`，客户端旋转注入补齐第四个 float 参数；8 个 Mixin 目标已经通过目标 JAR 签名核对。
- 网络迁移为 CustomPacketPayload；原字段 type 改为 kind，避免与 payload 类型方法重名，全部报文数据和范围广播保留。
- FX 顶点 API 更换但几何/颜色/混合模式保留，模型叠加颜色迁移为同数值的 ARGB。
- 独立机械比对未发现 11 技能、伤害/计时/碰撞/特效几何/配置默认值被简化。
- `src/validation` 是独立开发验证源码，正式 JAR 不包含测试或 Render Preview 适配器。
- 最终 ModDevGradle / NeoForge 21.1.251 服务端 38/38 GameTest 通过（7.812 秒）：11 技能的三个阶段生命周期共 33 项，以及伤害/击退抗性/报文、阶段阈值、真实 Boss 链启动、盾牌冷却、四实体 NBT/速度往返。测试使用独立服务端与测试玩家，不等同于多人实际战斗和平衡体验测试。
- Deep Aether 1.21.1-1.1.5.1 的 Minecraft `[1.21,1.21.1)` 范围由 FML 的 `VersionSupportMatrix`（1.21.1 兼容 1.21）正式接受，不修改第三方 JAR。
- Aether 1.5.10 内嵌 Accessories/Nitrogen/Cumulus，开发环境补充 Accessories 所需 owo-lib；Deep Aether 内嵌 AeroBlender/TerraBlender。未沿用旧版 Curios 运行依赖。
- Forge 原版通过 Render Preview 完成 28 项 × 3 时点 = 84 帧截图，零渲染错误、零空白帧、零缺失贴图色块。目录覆盖 19 个动作状态、4 种新增实体、5 种独立特效；这是组件预览，不是完整技能战斗录像。
- NeoForge 的 Render Preview 要求 21.1.251，因此最终环境提升到该版本。目标版也完成 84 帧，零错误、零空白、零缺失贴图色块；两版新增弹体和独立特效已完成截图对照。Boss 原始预览明暗差异已定位到 Minecraft 1.21 的 GUI 三维方向光变换：`setupGui3DDiffuseLighting` 新增 Y 轴翻转。仅在验证适配器中设置与 Forge 基线相同的光照矩阵，不修改生产渲染代码。进一步固定预览实体 ID，排除抖动相位不同的影响。最终 57 帧 Boss 动作的最大前景平均绝对色差为 0.596/255；15 帧独立特效中 11 帧逐像素相同，其余最大色差为 0.00113/255；12 帧新增实体中 7 帧逐像素相同，最大前景色差为 6.028/255（碎片第 20 tick，其余帧不超过 0.005/255）。几何、颜色和动作表现未发现效果删减，不声称所有帧逐像素完全一致。


## 构建流程修正

本地模板初始化使用 NeoGradle。运行客户端时发现 Aether 内嵌 Accessories 的 Mixin 依赖官方 `EntityRenderers.lambda$createEntityRenderers$26`，而 NeoGradle 重新编译 Minecraft 后生成了 `$2`，造成第三方 Mixin 注入失败。

因此最终构建使用 ModDevGradle 2.0.147，启用 `disableRecompilation = true`，保留官方二进制方法编号。没有修改或删减 Accessories，也没有屏蔽其 Mixin；EotsPlus 功能代码维持完整。相关官方构建模式说明：https://github.com/neoforged/ModDevGradle#disabling-decompilation-and-recompilation 。

可复现命令：

```powershell
$env:JAVA_HOME = 'C:\Users\30819\.jdks\ms-21.0.11'
.\gradlew.bat build
.\gradlew.bat runGameTestServer -PvalidateMigration
.\gradlew.bat runClient -PvalidateMigration '-PrenderPreviewJar=E:\模组\发cf的模组\Render Preview\1.21.1-neoforge-render-preview\build\libs\render-preview-1.21.1-neoforge-0.1.0.jar' '-PpreviewOutput=D:\AI临时文件\EotsPlus\render1211'
```

验证源码不进入正式 JAR。Render Preview 是开发验证工具，不是本模组运行前置。

## 产物检查

正式产物：`build/libs/eotsplus-1.21.1-neoforge-0.0.1.jar`。

- 元数据、资源和源码均使用 UTF-8，中文描述正确。
- Minecraft 资源格式为 34，全部 Mixin 配置均随包提供。
- 正式 JAR 不包含 `src/validation` 中的 GameTest 和 Render Preview 适配代码。
- SHA-256：`96fed147b0ec54674a80bab80caeca003e5b8b6d6afa2bc8084053e2673d0d3d`。

本机运行证据保存在 `D:\AI临时文件\EotsPlus`：`final-server-mdg.log`、`capture1201-deterministic/results.json`、`capture1211-deterministic/results.json`、`render-comparison-deterministic.json` 和 `render-pairs-deterministic.png`。截图比较包含 19 种动作状态、4 种实体、5 种独立特效，在第 5、20、45 tick 采样。
## 近战化改动迁移（来自 1201forge 6ac78ec、5e69c7d 及调试移除）

来源：Forge 1.20.1 分支的“低空起手、近战后游弋、五个近战技能”和“悬空不出招修复”。

| 文件 | 改动内容 | 1.21.1 适配点 |
| --- | --- | --- |
| `anim/EotsAnim.java` | 新增 STUCK、SWEEP、DOWNED、PROWL 四个动作 | 无 |
| `client/anim/EotsPose.java` | 四个新动作的姿态 | Deep Aether 包名 |
| `config/EotsConfig.java` | meleeLinger、lowMeleeStart、stuckTicks、impactMultiplier、sweepMultiplier、snapRange、downedTicks | `ForgeConfigSpec` 改为 `ModConfigSpec` |
| `ai/SkillType.java` | 新增 `melee` 标记 | 无 |
| `ai/EotsSkillGoal.java` | 低空近战起手、近战技能池回退全技能池、近战后低空游弋、残留占用清理、`floorBelow`/`holdHeight`/`headSweep` | Deep Aether 包名 |
| `ai/skill/SkyRendSkill.java` | 俯冲插地、卡地挣扎、二阶段甩头横扫、空目标保护 | 包名 |
| `ai/skill/ChainBiteSkill.java` | 贴地接近、低空咬击、收尾横扫 | 包名 |
| `ai/skill/GaleCoilSkill.java` | 低空盘绕，收尾砸地 | 包名、`GENERIC_EXPLODE.value()` |
| `ai/skill/InhaleSkill.java` | 低空吸气，近身扑咬或吐息 | 包名、`GENERIC_EXPLODE.value()` |
| `ai/skill/StormCollapseSkill.java` | 坠地后倒地硬直 | 包名、`GENERIC_EXPLODE.value()` |
| `ai/skill/SerpentTrailSkill.java` | 收尾不再爬升 | 包名 |

迁移方式：以 1.20.1 当前文件为准完整复制，只替换平台 API，不删减任何状态、特效、伤害或配置项。
