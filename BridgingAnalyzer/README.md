# BridgingAnalyzer — 1.21.11 移植版

搭路数据分析插件,本服的核心玩法。

## 出处与许可

本目录是 **[SakuraKoi/BridgingAnalyzer](https://github.com/SakuraKoi/BridgingAnalyzer)
v28** 的衍生作品,原作者 **SakuraKooi**,许可 **GNU GPL v2**(见 `LICENSE`)。

上游没有为 1.21 提供构建,本目录是把 v28 从 Spigot 1.8.8 移植到 Paper 1.21.11
的结果。源码来自对部署中 jar 的反编译(CFR 0.152)—— 上游 `plugin.yml` 的版本号
同为 28,与部署 jar 完全一致,故两者内容等同。

> **GPL v2 义务提醒:** 在自己的服务器上运行改后的版本没有任何义务。
> 但只要把编译出的 jar **分发**给他人(包括群组内其他管理员),就必须同时提供
> 本目录的完整源码,并保持 GPL v2 许可。

## 相对上游 v28 的改动

移植原则:**只改因 API 变更而无法编译的地方,不动任何业务逻辑。**
玩法行为必须与 2021 年的原版一致 —— 那是搭路服的立身之本。

移植中包含以下 API 兼容改动:

| 改动 | 数量 | 原因 |
|---|---|---|
| `PotionEffectType.SLOW` → `SLOWNESS` | 1 | 1.20.5 药水效果改用原版 id |
| `Material.GOLD_PICKAXE` → `GOLDEN_PICKAXE` | 3 | 1.13 扁平化 |
| `Material.SMOOTH_BRICK` → `STONE_BRICKS` | 4 | 1.13 扁平化 |
| `Material.GOLD_PLATE` → `LIGHT_WEIGHTED_PRESSURE_PLATE` | 1 | 1.13 压力板按材质拆分 |
| `Material.MELON_BLOCK` → `MELON` | 1 | 1.13 方块与切片同名合并 |
| `Sound.LEVEL_UP` → `ENTITY_PLAYER_LEVELUP` | 1 | 1.9 音效全面改名 |
| `EntityType.FIREWORK` → `FIREWORK_ROCKET` | 1 | 1.19 实体类型改用原版 id |
| `Material.STATIONARY_WATER` 判断 | 1 | 1.13 取消流动水/静止水区分 |
| 信标传送穿透判定 → `isPassThrough()` | 2 | 玻璃板拆成 16 色、告示牌拆成各木种,无法再用单常量比较 |
| `ItemStack` 的 data 字节 | 1 | 1.13 后方块状态由 `BlockData` 表达 |
| 删除 `utils/Metrics.java` | 1 个文件 | 依赖 `org.json.simple`(1.8 内置、现代已移除);仅为匿名用量统计,对玩法零贡献,Paper 自身已内置 bStats |
| 删除 `utils/SoundMachine.java`,6 处调用改直接常量 | 1 个文件 + 6 处 | 见下节 |

**未改动:** 全部 17 个事件监听在 1.21 中均存在。

### `SoundMachine` —— 一个编译期查不出来的坑

原版有个兼容垫片,先试 1.8 的音效名,`IllegalArgumentException` 就退回试 1.9 的名字:

```java
public static Sound get(String v18, String v19) {
    try { return Sound.valueOf(v18); }
    catch (IllegalArgumentException ex) { return Sound.valueOf(v19); }   // 第二次没人接
}
```

三对音效里有两对靠第二个参数还能命中,唯独末影人传送这对**两个名字在 1.21 都不存在**:
1.9 的 `ENTITY_ENDERMEN_TELEPORT` 是 Mojang 当年的拼写错误(ENDERMEN),后来修成了
`ENTITY_ENDERMAN_TELEPORT`。于是第二次 `valueOf` 裸抛,把整个 `PlayerMoveEvent` 打崩:

```
Could not pass event PlayerMoveEvent to BridgingAnalyzer
java.lang.IllegalArgumentException: No sound found with the name ENTITY_ENDERMEN_TELEPORT
	at TriggerBlockListener.triggerTeleportBlock(TriggerBlockListener.java:252)
```

踩中的是**踩红石块回传送点**这条核心路径。

因为音效名是字符串字面量而非枚举常量,**编译期完全查不出来**,第一轮移植时的常量改名清单自然漏掉了它。
垫片本身也已无意义(1.8 那半边永远失败),整个删掉,6 处调用改成直接引用 1.21 的常量。

同类隐患已全量扫过四个自研插件,再无第二处硬编码字符串查枚举的地方。

### NMS 反射层的重写(第二轮)

首轮移植时误判"NMS 依赖可借 Paper 重映射继续工作",保留了原样。实测证明是错的
—— 上线后服务端日志出现 **1435 次 `NullPointerException`**,四个反射类全数失效:

```
NoAIUtils.clsCraftEntity is null
  at BridgingAnalyzer.spawnVillager(BridgingAnalyzer.java:134)
  at BridgingAnalyzer.lambda$onEnable$0(BridgingAnalyzer.java:248)   ← 定时任务
```

根因:Paper 自 **1.20.5** 起去掉了 `org.bukkit.craftbukkit.v1_XX_RX` /
`net.minecraft.server.v1_XX_RX` 的版本号包名。这几个类都用
`Bukkit.getServer().getClass().getPackage().getName().split("\.")[3]` 取版本号再
拼包名,在 1.21 上必然拿到不存在的类。

`NoAIUtils` 还有个原版自带的 bug 放大了后果:静态块 `catch` 里置
`works = false` 之后,末尾又无条件 `works = true`,于是反射初始化失败也照样往下
执行,每次调用都 NPE。

四个类全部改用原生 API:

| 原实现 | 现实现 |
|---|---|
| `NoAIUtils` 反射写 NBT 的 `NoAI` 标志 | `LivingEntity#setAI`(1.9 起原生) |
| `TitleUtils` 反射拼 `PacketPlayOutTitle` | Adventure `Player#showTitle` |
| `ActionBarUtils` 反射拼 `PacketPlayOutChat` | Adventure `Player#sendActionBar` |
| `ParticleEffects`(551 行反射粒子库) | `World#spawnParticle` + `org.bukkit.Particle` |
| `ReflectionUtils`(209 行支撑类) | 一并删除 |

粒子类型映射:`SPELL_WITCH` → `Particle.WITCH`、`FIREWORKS_SPARK` →
`Particle.FIREWORK`、`TOWN_AURA` → `Particle.MYCELIUM`、`CLOUD` 不变。

标题与 ActionBar 改用 Adventure 时,文本经 `LegacyComponentSerializer` 解析 ——
插件的提示文本全是 `§` 颜色码格式,不这样处理会把颜色码当普通字符显示。
(原版是把文本直接拼进 JSON 字符串,本就无法处理 `§`。)

重写后服务端日志中 `sakura.kooi` 相关异常由 1435 次降为 **0**。

## 严重稳定性修复

### `Counter#setCheckPoint` 就地改坏了刚存进去的检查点

```java
this.checkPoint = loc;
Block target = loc.add(0.0, -1.0, 0.0).getBlock()...;   // Location#add 是就地修改
```

`Location#add` 改的是对象本身,而 `checkPoint` 存的正是同一个引用 ——
调用方传进来的是「绿宝石块坐标 + (0.5, 1, 0.5)」,被这一句 `add(0,-1,0)` 之后
**检查点降了一格,正好落在绿宝石块自己那一格里**。

后果是掉虚空后被传送进方块内部,原版碰撞把人挤出去、挤下平台,于是再次掉进虚空
—— 反复触发、反复回收,人却出不来。因为 `onDamage` 把玩家伤害全清零了,
连摔死解脱都做不到,表现就是「反复掉血且无法死亡」。

修法:`loc.clone()` 后再存,找箱子那步也用独立的副本。

诊断日志实测检查点为 `y=41.0`(绿宝石块在 40),修复前会是 `40.0`。

### 掉虚空时传送排在最后

`teleportCheckPoint()` 原顺序是「清背包 → 发方块 → 回血 → 传送」。
传送排最后意味着前面任何一步出问题,人就永远留在虚空里。

发方块这步现在走 BridgingSkin 的 `SkinProvider`(要读玩家皮肤 json),
比原来的固定砂岩多了几条出错路径,更不该挡在传送前面。已改成**传送优先**,
背包与状态挪到后面收尾。

### 重连后掉进深虚空且无法死亡

旧实现用 `HashMap<Player, Counter>` 保存状态,而 `Counter` 又永久持有创建时的
`Player`。Paper 按 UUID 判断 Player 相等,所以重连后的新实体会命中旧 Counter;
虚空恢复虽然执行了,实际传送的却是已经离线的旧实体。传送返回 `false` 又未检查。

真实虚空每次只造成 4 点伤害,达不到原来的「伤害大于 20 才恢复」条件,同时所有
伤害又会被锁血清零,最终玩家能一直掉到数千格以下且无法死亡。

现在改为:

- `PlayerSessionRegistry` 仅按 UUID 保存 Counter,Counter 不再持有 Player;
- `PlayerRecoveryService` 始终操作事件传入的当前在线 Player,并检查传送结果;
- `VoidSafetyListener` 同时覆盖 `y < 0`、真实 `VOID` 伤害、重连与逐 tick 看门狗;
- 同 tick 的移动、伤害和看门狗只会排一个恢复任务;
- 标题、统计、方块清理、皮肤/背包恢复全部是传送后的独立收尾,任何一项异常都不会挡住救援;
- 检查点与出生点传送若被第三方连续取消 3 次,会保留物品和经验并自动安全重生,
  避免再次出现「锁血但永远出不来」;
- OP 的 `bridginganalyzer.noclear` 只允许跳过正常加入重置,不能跳过深虚空救援。

### 西瓜偶发双重击退

旧逻辑把 `noDamageTicks` 当去重锁,但延迟 7 tick 后又主动清零并调用
`damage(0)`.第一次击退产生的新移动事件如果仍踩着西瓜,就会排入第二次击退。

现在由独立的 `MelonKnockbackController` 和 UUID 状态机管理:
连续接触只触发一次,离开西瓜且冷却结束后才重新激活;传送、死亡、退出、切世界或
切模式都会取消待执行任务。成功传送所在 tick 的旧移动事件也会被抑制,不会在传送点
延迟补一次击退。西瓜逻辑不再修改共享无敌帧来完成去重。

### 死亡后检查点箱子套装被默认方块覆盖

绿宝石检查点下方有箱子时,踩上去会把箱子内容作为该检查点套装。旧恢复路径查箱子
时与设置检查点相差一格,并且即使成功装入箱子物品,随后仍会无条件清包、发一个默认
练习方块。

现在设置与恢复共用同一个箱子定位器,物品策略固定为「检查点箱子优先;没有箱子或
箱子读取失败时才发默认方块」。普通死亡会保留 Key/等级并在重生后一 tick 重新装入
最后检查点的套装;连续传送失败触发的紧急安全重生则保持原背包,不执行清包。

## 代码模块

| 模块 | 职责 |
|---|---|
| `session` | UUID 玩家会话与 Counter 生命周期 |
| `recovery` | 检查点传送、虚空判定、伤害/移动/重连兜底 |
| `trigger` | 西瓜触发状态、延迟任务与清理 |
| `commands` | 管理命令 |
| `api` | BridgingSkin 等外部插件使用的兼容门面 |

## 本服自加的功能

### 死亡后回到检查点

上游没有实现 `PlayerRespawnEvent`,死亡走原版重生逻辑,回世界出生点。
现在改为回最后踩过的绿宝石块,与掉虚空的行为一致。

> 注意:`onDamage` 会把**所有玩家伤害清零**(上游设计,练习服不该被打死)。
> 唯一例外是两处安全传送连续 3 次都失败时,恢复模块会保物品强制重生,防止永久锁死。

### 终点烟花的「实弹」

**这不是上游的行为,是本服自己加的整蛊。**

踩红石块(终点)会放一发庆祝烟花。现在这发烟花有 **0.1%(千分之一)的概率是实弹**,
造成 500 点伤害,当场把人炸死。

实现在 `utils/FireworkUtils.java`,三个常量集中在类顶部:

| 常量 | 值 | 含义 |
|---|---|---|
| `RIGGED_ONE_IN` | `1000` | 概率分母,1/1000 = 0.1% |
| `RIGGED_DAMAGE` | `500.0` | 伤害值(玩家血量上限 20,必死) |
| `RIGGED_DELAY_TICKS` | `2` | 烟花发射后延迟多久结算 |

要关掉就把 `rig()` 的调用删掉;要调概率改 `RIGGED_ONE_IN` 即可。

### 实现上绕开的三个坑

**1. 无敌帧。** 踩红石块那一刻(tick 0)`TriggerBlockListener` 设了 40 tick 无敌。
烟花是在粒子环播完、也就是 **tick 20** 才发射,结算前显式
`setNoDamageTicks(0)` —— 否则这段时间里其他来源产生的无敌帧可能挡住实弹伤害。

**2. 伤害归因。** 必须用 `damage(double, DamageSource)` 配
`DamageType.FIREWORKS`,死亡信息才是原版的「被烟花火箭炸死了」。
用 `damage(double, Entity)` 那个重载会被映射成通用实体攻击,一看就不对劲 ——
整蛊要的就是看不出人为痕迹。

**3. 结算顺序。** 先打伤害,再 `detonate()`。后者会移除烟花实体,
而 `DamageSource` 里存着它的引用,顺序反了就是拿一个已消失的实体当伤害来源。

创造与旁观模式直接跳过(打不动,白费力气)。

## 构建

项目使用 Gradle Kotlin DSL 与 JDK 21。

```bash
gradle -p .. :BridgingAnalyzer:build
```

产物 `build/libs/BridgingAnalyzer-28-1.21.11.jar`,复制到服务端 `plugins/` 即可。

虚空边界与西瓜去重有 JUnit 回归测试,随 `build` 自动执行。

## 提供的命令

| 命令 | 作用 |
|---|---|
| `/bridge highlight` | 启用/禁用 侧搭辅助指示 |
| `/bridge pvp` | 启用/禁用 伤害屏蔽 |
| `/bridge speed` | 启用/禁用 搭路速度统计 |
| `/bridge stand` | 启用/禁用 走搭位置指示 |
| `/bridge reset` | 重置出生点 |
| `/bridge remove` | 删除最近的一个靶子 |
| `/clearblock` | 清理自己放置的方块 |
| `/imstuck` | 卡住时脱困 |
| `/genvillager` | 创建村民刷新点 |
| `/bsaveworld` | 保存世界 |

地图上绿宝石块 / 红石块 / 青金石块 / 信标四个功能方块的踩踏触发逻辑在
`TriggerBlockListener`。
