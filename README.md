# InfBridgePractice

Inf Network 搭路练习服的 Paper 插件项目，面向 Minecraft 1.21.11 与 Java 21。
仓库由四个相互配合的插件组成，提供搭路训练、等级排行、方块皮肤、付费抽奖、原生菜单和 CPS 统计。

## 项目模块

| 模块 | 版本 | 作用 |
|---|---|---|
| `BridgingAnalyzer` | `28-1.21.11` | 核心搭路玩法、检查点、死亡与虚空恢复、练习方块清理、训练靶、原生菜单和快捷传送 |
| `BlockLv` | `1.0-1.21.11` | 搭路经验与等级、Top 10 排行榜、PlaceholderAPI 变量 |
| `BridgingSkin` | `4-1.21.11` | 方块皮肤、皮肤选择菜单、Vault 抽奖、多个抽奖箱、SQLite/PostgreSQL 存储 |
| `CpsCounter` | `3-1.21.11` | CPS 统计、实时监控与高 CPS 告警 |

所有 Java 包均位于 `net.infnetwork.snowball` 命名空间下，并按模块继续分包。

## 主要功能

### 搭路练习

- 绿宝石块设置检查点，红石块返回检查点并完成练习，青金石块返回出生点。
- 正常死亡、虚空坠落和致命伤害共用安全恢复流程。
- 生存模式放置的练习方块会在死亡、离线或完成练习后清理；创造模式建筑保持不变。
- 检查点下方箱子可定义恢复套装；其中普通 `SANDSTONE` 会替换成玩家当前方块皮肤。
- 快捷栏第九格固定下界之星作为原生菜单入口。
- `/cd` 打开主菜单，`/warpbridge` 打开 54 格、配置驱动的快捷传送菜单。
- 菜单付费项通过 Vault 扣款，价格均可在 `plugins/BridgingAnalyzer/menus.yml` 修改。

### 等级系统

- 放置练习方块和击杀奖励搭路经验。
- 排行榜按等级、当前经验和名字排序，UUID 仅用于稳定识别玩家。
- 等级显示把数字与 `✫` 作为一个着色单元：
  - 0–99：`#A0A0A0`
  - 100–199：`#55D68A`
  - 200–299：`#4DA3FF` 到 `#8B5CF6`
  - 300 以上：`#FFD54A` 到 `#FF6B35`
- 经验与等级加减使用溢出安全的统一门槛算法，并立即保存管理员命令产生的变更。

### 方块皮肤与抽奖

- 默认皮肤是 `CUT_SANDSTONE`；`SEA_LANTERN` 因终点破坏机制冲突而禁止作为皮肤。
- 普通玩家可通过 `/bskin` 或主菜单打开 54 格皮肤选择器；材质名使用客户端原生翻译。
- 皮肤选择器支持分页，并可直接返回搭路主菜单。
- 抽奖通过 Vault Economy 扣款，支持单抽、十连、滚动动画和多个末影箱抽奖点。
- 抽奖奖励仅来自完整的 1×1×1 实体方块白名单，重复皮肤不会再次抽取。
- FAWE/WorldEdit 选区必须恰好包含一个末影箱，才能注册为抽奖箱。

### 数据与身份

`BlockLv` 与 `BridgingSkin` 均支持：

- SQLite：本地开发默认选项，数据保存在各插件目录。
- PostgreSQL：生产部署选项，连接信息写在对应插件的 `config.yml`。
- PostgreSQL JDBC 驱动直接打包在两个插件 JAR 内，不依赖服务器启动时联网下载。

玩家数据以 Bukkit `Player#getUniqueId()` 为主键。搭配 UniversalAuth 和 Velocity Modern Forwarding 时，
该 UUID 应为账号稳定的 `profileUuid`；玩家名只用于显示和受约束的旧数据迁移，不作为主键。

BridgingSkin 会以事务方式迁移旧 `skins/*.json`。迁移前会完整验证目录，遇到身份冲突或损坏文件时停止插件，
避免默认数据覆盖旧记录。生产切换数据库前应先备份插件目录和数据库。

## 依赖

运行环境：

- Java 21
- Paper 1.21.11
- Vault：`BridgingAnalyzer` 与 `BridgingSkin` 的必需依赖
- PlaceholderAPI：BlockLv 占位符，可选
- DecentHolograms：BlockLv 排行榜全息，可选
- WorldEdit 或 FastAsyncWorldEdit：注册抽奖箱时使用，可选
- 支持 Vault Economy 的经济插件

`BridgingSkin` 必须在 `BridgingAnalyzer` 之后加载，插件描述文件已声明该依赖。

## 构建

在仓库根目录执行：

```bash
gradle clean build --no-daemon --warning-mode fail
```

也可以只构建一个模块：

```bash
gradle :BridgingAnalyzer:build
gradle :BlockLv:build
gradle :BridgingSkin:build
gradle :CpsCounter:build
```

构建产物：

```text
BridgingAnalyzer/build/libs/BridgingAnalyzer-28-1.21.11.jar
BlockLv/build/libs/BlockLv-1.0-1.21.11.jar
BridgingSkin/build/libs/BridgingSkin-4-1.21.11.jar
CpsCounter/build/libs/CpsCounter-3-1.21.11.jar
```

部署时应停服后整体替换有关 JAR，再完整重启；不要使用插件热重载。

## 命令

### BridgingAnalyzer

| 命令 | 作用 |
|---|---|
| `/bridge` | 查看或切换练习功能 |
| `/clearblock [玩家]` | 清理练习方块 |
| `/imstuck` | 清除附近阻挡方块 |
| `/genvillager` | 创建训练靶刷新点 |
| `/bsaveworld` | 保存世界 |
| `/cd`、`/bridgemenu` | 打开搭路主菜单 |
| `/warpbridge`、`/bridgewarp` | 打开快捷传送菜单 |

普通玩家默认拥有 `bridginganalyzer.menu.main`、`bridginganalyzer.menu.warp` 和
`bridginganalyzer.menu.item`。管理功能默认仅 OP 可用。

### BlockLv

| 命令 | 权限 | 作用 |
|---|---|---|
| `/blocklv add <玩家> <经验>` | `blocklv.add` | 增加经验 |
| `/blocklv addlevel <玩家> <等级>` | `blocklv.addlevel` | 增加等级 |
| `/blocklv decrease <玩家> <经验>` | `blocklv.decrease` | 扣除完整进度，可跨级回退 |
| `/blocklv decreaselevel <玩家> <等级>` | `blocklv.decreaselevel` | 扣除等级并限制当前经验到合法范围 |
| `/blocklv clear <玩家>` | `blocklv.clear` | 清空等级和经验 |
| `/blocklv setrank` | `blocklv.setrank` | 设置排行榜位置 |
| `/blocklv refresh` | `blocklv.refresh` | 刷新排行榜 |

PlaceholderAPI 变量：

| 变量 | 含义 |
|---|---|
| `%blocklv_lv%` | 等级纯数字 |
| `%blocklv_level_display%` | 彩色 `数字✫` |
| `%blocklv_px%` | 当前经验 |
| `%blocklv_uppx%` | 距下一级所需经验 |
| `%blocklv_prefix%` | 带中性括号的完整等级徽章 |

### BridgingSkin

| 命令 | 权限 | 作用 |
|---|---|---|
| `/bskin` | `bridgingskin.skin.select` | 打开皮肤选择器，默认所有玩家可用 |
| `/bskin-edit edit <玩家>` | `bridgingSkin.admin` | 编辑玩家皮肤 |
| `/bskin-edit clear <材质>` | `bridgingSkin.admin` | 全局移除指定皮肤 |
| `/bskin-crate set` | `bridgingskin.admin.crate` | 追加注册当前选区中的末影箱 |
| `/bskin-crate remove` | `bridgingskin.admin.crate` | 移除当前选区对应的抽奖箱 |
| `/bskin-crate clear`、`clearall` | `bridgingskin.admin.crate` | 清除全部抽奖箱 |
| `/bskin-crate info`、`list` | `bridgingskin.admin.crate` | 列出全部抽奖箱 |

抽奖价格在 `plugins/BridgingSkin/config.yml` 中配置：

```yaml
lottery:
  single-cost: 100.0
  ten-cost: 900.0
```

### CpsCounter

| 命令 | 作用 |
|---|---|
| `/cps [玩家]` | 查看 CPS |
| `/cps #mon <玩家>` | 开始或停止实时监控 |
| `/cps #silent` | 切换自动告警 |

相关权限包括 `cpscounter.cps`、`cpscounter.bypass`、`cpscounter.bypasslimit` 和
`cpscounter.monitor`。

所有插件命令均提供上下文相关的 Tab 补全，并按权限和玩家可见性过滤候选项。

## 配置提示

- 数据库配置修改后必须完整重启服务器。
- PostgreSQL 的数据库、用户和密码应提前创建，并授予对应数据库所有权或建表权限。
- YAML 中含 `#`、`:` 等字符的密码应使用引号包裹。
- 主菜单清理价格位于 `main.entries.clearblock.cost`。
- 每个传送点使用自己的 `warp.entries.<id>.cost`，`0` 表示免费。
- 所有价格必须是非负、有限的 YAML 数值，不要给数字加引号。

## 来源与许可

- `BridgingAnalyzer` 基于 [SakuraKoi/BridgingAnalyzer](https://github.com/SakuraKoi/BridgingAnalyzer) v28，原作者 SakuraKooi，按 GNU GPL v2 发布；许可文本见 `BridgingAnalyzer/LICENSE`。
- `BridgingSkin` 基于 [SakuraKoi/BridgingSkin](https://github.com/SakuraKoi/BridgingSkin) v3 继续开发。
- `CpsCounter` 基于 [SakuraKoi/CPSCounter](https://github.com/SakuraKoi/CPSCounter) v3。
- `BlockLv` 原作者为 luanmenglei；本仓库维护其 Paper 1.21.11 版本。
