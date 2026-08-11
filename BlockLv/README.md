# BlockLv — 1.21.11 移植版

搭路等级系统:放置方块累积经验、升级、Top 10 排行榜全息、`%blocklv_*%` 占位符。

## 出处

原作者 **luanmenglei**(原服管理员),**无公开源码**。本目录源自对部署 jar 的
CFR 0.152 反编译。原版号 `1.0`,移植版号 `1.0-1.21.11`。

与 BridgingAnalyzer 不同,这个插件没有上游可跟随,后续维护完全落在本仓库。

## 相对原版的改动

### 数据层:MySQL → SQLite / PostgreSQL 双后端

原版连 `jdbc:mysql://localhost:3306/blocklv`,硬编码,需要预先建库建账号。
现在由 `config.yml` 的 `database.type` 决定:

| 取值 | 用途 | 驱动来源 |
|---|---|---|
| `sqlite` | 本地开发。零配置,数据落在插件目录的 `blocklv.db` | Paper 自带 `org.xerial:sqlite-jdbc` |
| `postgresql` | 生产(Ubuntu 主服) | `plugin.yml` 的 `libraries:` 段启动时下载 —— **Paper 不自带 PG 驱动** |

**SQL 只有一套,没有方言分支:**

- 标识符一律不加引号的小写。原版用的反引号是 MySQL/SQLite 方言,PostgreSQL 不认。
- upsert 用 `INSERT ... ON CONFLICT (uuid) DO UPDATE`。
  PostgreSQL 9.5+ 与 SQLite 3.24+ 语法完全一致(Paper 自带的是 3.49)。
  原版是 delete + insert 两条语句,非原子,中途失败会留下空记录。
- `varchar` / `bigint` / `limit` 都是标准 SQL。

这几条语句已用 Paper 自带的 sqlite-jdbc 实测:DDL 幂等、upsert 插入后再覆盖值正确、
不产生重复行、排行榜查询正常。

> **PostgreSQL 后端尚未对真库验证** —— 本机没有可用凭据,也没有 Docker。
> 已验证的是:SQL 在 SQLite 上真实执行通过、PG 驱动能被 Paper 正确下载并加载、
> 占位符未替换时插件干净拒绝启动。部署步骤见 `docs/DEPLOY-UBUNTU.md`。

连接失败时插件**自我停用**,而不是带着 null 连接继续跑 —— 后者会让每次放方块、
每次登录都抛 NPE,而等级数据一条都存不下来。

### UniversalAuth 身份与旧数据迁移

排行榜以认证完成后的 Bukkit `Player#getUniqueId()` 作为唯一玩家主键。使用
UniversalAuth 时,这个值必须由 Velocity Modern Forwarding 转发为账号永久的
`profileUuid`;临时 `frontUuid`、Mojang `premiumUuid` 和玩家名都不会作为主键。

为了保留启用 UniversalAuth 前的等级数据:

- UUID 未命中时,只会收养“UUID 精确等于 Bukkit `OfflinePlayer:<name>` 算法结果”的
  同名旧记录,不会仅凭同名夺取另一个正式账号 UUID 的数据;
- 若新的 `profileUuid` 行已经存在,旧记录与新记录比较 `(等级,当前经验)`,保留较高者,
  不会把两边经验相加;
- 旧表若只有自增 `id` 而没有 UUID,启动时会原样保留为
  `blocklv_legacy_id`,创建 UUID 主键的新表,再在玩家认证登录时按唯一名字迁移;
- 混合旧表若带有空值或非法 UUID,迁移会整体回滚并停用插件,要求先人工修复,
  不会让无法映射的排行数据静默消失;
- 遇到多条无法唯一确认的旧记录时,该会话会禁止写回,避免用 0 级数据覆盖旧档;
- 玩家名只保存为排行榜显示名,UUID 命中后会自动刷新名字。

后端必须禁止绕过 Velocity 直连。否则攻击者仍可能用离线模式名字进入后端,这不是
业务数据库能够安全修复的身份边界。

排行榜顺序为 `等级 DESC,当前经验 DESC,名字 ASC`;UUID 只负责识别账号,不参与名次计算。

### 全息:HolographicDisplays → DecentHolograms

两者 API 完全不同,DecentHolograms 没有提供 HD 兼容层(只有一次性的数据转换
工具)。改用 `DHAPI`:全息以名字 `blocklv_rank` 注册,整行列表一次性
`setHologramLines` 替换,不再逐行 `insert`。显示文本逐字未改。

### 修正原版的四个 bug

| 位置 | 问题 |
|---|---|
| `MySQLUtil#get` | 连续两次 `rs.next()` 读 `lv` 和 `px`。按 uuid 查询只有一行,第二次 `next()` 必然返回 false,**`px` 永远读不到 —— 玩家每次登录经验都被清零**。改为一次 `next()` 读两列 |
| `PointManger#addPx` | 升级时**扣错了经验**,详见下节 |
| `PointManger#upLevel` | 构造了 `FireworkMeta` 却从未 `setFireworkMeta` 写回,升级烟花一直是默认样式。已补上 |
| `onDisable` | 数据库连接从不关闭。已补 `Database#close`,且顺序为"先落盘在线玩家数据,再关连接" |

#### 升级扣经验的顺序错误

`addPx` 的升级循环里,判定用的是**当前等级**的门槛,扣除用的却是**自增之后**那一级的门槛:

```java
while (px >= getNextLvPx(lv)) {
    upLevel(lv + 1, player);
    players.get(name).lv = ++lv;                       // 先自增
    players.get(name).px = px -= getNextLvPx(lv);      // 再按新等级的门槛扣
}
```

门槛随等级递增(`lv*20 + 2^(lv/1000) + lv/3*2`),所以每次升级都会多扣一截:
1 级攒满 21 点该升 2 级,却按 2 级的 41 点去扣,`px` 直接变成 **-20**。

于是 `refreshExp` 里算出的经验条进度是 `(-20+1)/41 = -0.463`。
**1.8 的 `CraftPlayer#setExp` 不校验取值范围,这个错误一直是静默的** ——
玩家只是看到经验条不对劲。1.21 会直接抛:

```
java.lang.IllegalArgumentException: Experience progress must be between 0.0 and 1.0 (-0.46341464)
	at PointManger.refreshExp(PointManger.java:69)
	at PointManger.addPx(PointManger.java:64)
	at BlockPlace.onPlace(BlockPlace.java:23)
```

而且异常发生在 `setExp`,把后面的 `setLevel` 也一起带崩,**等级数字同样不刷新**;
`BlockPlaceEvent` 每放一个方块就报一次。

修法是把扣除挪到自增之前 —— 一行位置的事。

### API 适配

| 改动 | 原因 |
|---|---|
| `EntityType.FIREWORK` → `FIREWORK_ROCKET` | 1.19 实体类型改用原版 id |

### 顺带优化

`refreshTop` 原本把全表读进内存再做 10 轮线性扫描找最大值(O(10n)),
改为 `order by lv desc limit 10` 交给 SQL,结果一致。

## 构建

项目使用 Gradle Kotlin DSL 与 JDK 21。Gradle 会自动下载编译依赖。

```bash
gradle -p .. :BlockLv:build
```

产物 `build/libs/BlockLv-1.0-1.21.11.jar`。

## 命令与占位符

| 命令 | 权限 | 作用 |
|---|---|---|
| `/blocklv add <玩家> <经验>` | `blocklv.add` | 增加经验 |
| `/blocklv clear <玩家>` | `blocklv.clear` | 清空等级与经验 |
| `/blocklv setrank` | — | 把排行榜全息设到当前位置 |
| `/blocklv refresh` | `blocklv.refresh` | 立即刷新排行榜 |

| 占位符 | 含义 |
|---|---|
| `%blocklv_lv%` | 当前等级 |
| `%blocklv_px%` | 当前经验 |
| `%blocklv_uppx%` | 距下一级还差多少经验 |
| `%blocklv_prefix%` | 带颜色的等级标签,如 `§6[123✫]`。菜单与 TAB 用的就是它 |

## 经验来源

- 放置方块:每块 1 点
- 把别人推下虚空:10 点(见 `PlayerMove` 与 `PlayerDeathByPlayer`,击杀记录保留 120 tick)
- 每日签到:500 点(由 DeluxeMenus 的 `reward` 菜单调 `/blocklv add`)

升级公式在 `PointManger#getNextLvPx`:`lv*20 + 2^(lv/1000) + (lv/3)*2`。
