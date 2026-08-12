# BridgingSkin v4

为 BridgingAnalyzer 提供方块皮肤，并加入 Vault 付费抽奖、FAWE/WorldEdit 抽奖箱注册、
SQLite/PostgreSQL 双后端和旧 JSON 安全迁移。默认皮肤仍是 `CUT_SANDSTONE`（1.8 的
`SANDSTONE:2`），`SEA_LANTERN` 因胜利破坏机制冲突而永久禁用。

## 身份与数据

- 当前数据的唯一主键是后端 `Player#getUniqueId()`。启用 UniversalAuth 后，这就是稳定的
  `profileUuid`；玩家名仅用于显示和首次迁移匹配。
- 默认后端是 `plugins/BridgingSkin/skins.db`（SQLite）；在 `config.yml` 把
  `database.type` 改为 `postgresql`（也接受 `pgsql`/`postgres`）并填写连接信息即可切换。
- 旧 `skins/*.json` 不会删除、改名或直接写进当前 UUID 表。插件会先严格验证整个目录，
  再在一个事务中导入 legacy staging；玩家认证完成后按唯一名字事务认领。
- 测试服冻结语料是 3176 个玩家文件、3177 条拥有记录。全量 SQLite 导入、幂等复跑和
  真实 PostgreSQL 的建表/认领/保存均有集成测试。
- 当前离线 UUID 已认领的数据，未来切换 UniversalAuth 随机 UUID 时，可在旧 UUID、名字
  均通过验证的前提下自动完成一次安全升级。冲突会拒绝加载，不会创建默认记录覆盖旧数据。
- PostgreSQL 模式按一个练习服实例写入设计；不要让多个 Paper 实例同时共享写入同一套皮肤表。

## 抽奖

抽奖只通过标准 Vault `Economy` 接口扣款，当前可使用 VaultUnlocked + EssentialsX Economy，
以后替换经济插件不需要改 BridgingSkin。

流程固定为：确定未拥有奖励 → Vault 扣款 → 整批奖励事务入库 → 播放展示动画。授权失败会
按实际扣款退款；动画中断、传送或掉线不会丢奖励。十连批内不重复，剩余不足 10 种时会拒绝
十连并提示改用单抽。动画中间一排方块向左滚动，上下两排彩色玻璃板分别向左、向右滚动；
结果物品名使用 Minecraft 客户端自带翻译，因此中文客户端会显示官方中文方块名。

奖池是 200 多种人工白名单中的完整 1×1×1 实体方块。半砖、楼梯、容器、重力方块、触发块、
西瓜和海晶灯都不能进入奖池。首次启动会把完整列表写入 `lottery.prize-pool`，管理员可删减或
调整顺序；非法条目启动时会被忽略。

费用位于：

```yaml
lottery:
  single-cost: 100.0
  ten-cost: 900.0
```

## 命令与权限

| 命令 | 权限 | 作用 |
|---|---|---|
| `/bskin` | 无 | 分页查看已拥有皮肤并切换 |
| `/bskin-edit edit <player>` | `bridgingSkin.admin`（OP） | 分页增删玩家皮肤，即时事务保存 |
| `/bskin-edit clear <material>` | `bridgingSkin.admin`（OP） | 从当前与未认领旧数据中全局清除皮肤；默认砂岩不可清除 |
| `/bskin-crate set` | `bridgingskin.admin.crate`（OP） | 追加注册 FAWE/WorldEdit 选中的单个末影箱；可重复注册多个位置 |
| `/bskin-crate remove` | 同上 | 移除当前单方块选区对应的一个抽奖箱 |
| `/bskin-crate clear` | 同上 | 清除全部已注册抽奖箱 |
| `/bskin-crate info` 或 `list` | 同上 | 列出全部已注册抽奖箱及坐标 |

注册时选区必须恰好一个方块，且当前方块必须是 `ENDER_CHEST`。已注册抽奖箱会阻止原版打开、
破坏、爆炸和活塞移动；即使 WorldGuard 区域禁止 `use/chest-access`，自定义菜单仍可打开。
旧版单一 `lottery.crate` 坐标会在启动时无损迁移到新的 `lottery.crates` 列表。

## 构建与测试

使用 JDK 21 和 Gradle Kotlin DSL：

```bash
gradle :BridgingSkin:build
```

产物为 `BridgingSkin/build/libs/BridgingSkin-4-1.21.11.jar`。普通测试使用临时 SQLite；可选
环境变量 `BRIDGING_SKIN_CORPUS` 验证真实旧 JSON 目录，`BRIDGING_SKIN_PG_URL` 验证真实
PostgreSQL。

上游为 [SakuraKoi/BridgingSkin](https://github.com/SakuraKoi/BridgingSkin)，包名保留
本目录是在原作者 v3 反编译移植基础上的 v4 模块化实现。
