# BridgingSkin — 1.21.11 移植版

搭路方块皮肤选择。给 BridgingAnalyzer 提供 `BlockSkinProvider`,让玩家自选搭路用的方块。

## 出处与许可

上游 **[SakuraKoi/BridgingSkin](https://github.com/SakuraKoi/BridgingSkin)**,原作者 **SakuraKooi**。
本目录是 v3 的 1.21.11 移植版,源码取自对 `bridge/plugins/BlockSkin.disabled` 的
CFR 0.152 反编译 —— 那个文件其实就是 BridgingSkin v3 的 jar,被重命名成 `.disabled` 关掉了。

包名保留 `sakura.kooi` 以尊重出处。

> **注意:原服 1.8.8 的启动日志里没有 BridgingSkin,它当年就没在跑。**
> 移植它属于恢复一个被停用的功能,不是还原线上现状。

## 相对原版的改动

| 改动 | 原因 |
|---|---|
| **删掉 `SkinSet.data` 字节字段** | 1.13 扁平化后方块变体各自是独立 `Material`,data 不再承载信息。存量数据由 `tools/migrate_skins.py` 一次性转换 |
| 10 处物料改名 | `DIODE`→`REPEATER`、`PISTON_BASE`→`PISTON`、`WOOD_PLATE`→`OAK_PRESSURE_PLATE` 等,见 `IllegalMaterial.java` |
| 新增 `SkinSelectHolder`,不再比对菜单标题 | `Inventory#getTitle()` 1.14 起已移除(标题属于 `InventoryView`)。改用自定义 `InventoryHolder` 按类型识别,比字符串比对可靠,也不会被玩家用同名箱子骗过 |
| 剥离内嵌 gson | 原版把整个 gson shade 进 jar(`sakura.lib.com.google.gson`,69 个类)。服务端 `libraries/` 下已有,改成 `provided` |
| `onSetSkin` 加主手判定 | 1.9 起 `PlayerInteractEvent` 主手/副手各触发一次,不拦会重复添加皮肤 |
| 重写 `loadSkin()` | CFR 把原版的 try-with-resources 反编译成了 `Collections.singletonList(reader).get(0)` 判空加标签跳转的怪结构,且没接住受检异常,根本编译不过 |
| 移除 bStats 统计 | 依赖 BridgingAnalyzer 的 `Metrics` 类,后者因 `org.json.simple` 已在移植中删除 |
| `/bskin-edit clear` 去掉 `[data]` 参数 | 同上,data 已无意义 |

### 关于「平滑砂岩」

1.8 的默认皮肤是 `SANDSTONE:2`,当年菜单里叫**平滑砂岩**。扁平化后它叫 **`CUT_SANDSTONE`**。

1.21 里另有一个 `SMOOTH_SANDSTONE`,那是 1.13 新增的熔炼产物,材质不一样 —— **别搞混**。
这个映射不是猜的,是拿升级后的 `world/region/*.mca` 方块调色板核对出来的:
升级后的地图里只出现 `cut_sandstone`,没有 `smooth_sandstone`。

## 数据迁移

原服 3175 份皮肤文件存的是 1.8 的 `{"Material": "SANDSTONE", "Data": 2}`。
迁移脚本把 `(Material, Data)` 折叠成单个 1.21 的 Material 名并删掉 `Data` 键:

```bash
python tools/migrate_skins.py            # 只检查,打印转换前后统计
python tools/migrate_skins.py --apply    # 真正写出
```

脚本对**未登记的 `(Material, Data)` 组合直接报错退出**,不做猜测 ——
猜错会让老玩家的皮肤静默变样,而且没人会发现。

实际数据里只有两种组合:`SANDSTONE:2`(6349 处)和 `DIAMOND_BLOCK:0`(2 处)。

> 皮肤文件含玩家名与 UUID,已在 `.gitignore` 里排除,不入库。

## 构建

需要 JDK 21 与 Maven,且**先在 `../BridgingAnalyzer/` 执行 `mvn install`** ——
`plugin.yml` 里是 `depend` 而非 `softdepend`,是硬依赖。

```bash
mvn package
```

产物 `target/BridgingSkin-3-1.21.11.jar`。

## 命令与权限

| 命令 | 权限 | 作用 |
|---|---|---|
| `/bskin` | — | 打开皮肤库存,点选切换 |
| `/bskin-edit edit <player>` | `bridgingSkin.admin` | 编辑指定玩家的皮肤库存(关闭箱子时以箱内容为准覆盖) |
| `/bskin-edit clear <material>` | `bridgingSkin.admin` | 从所有玩家库存里清掉某个方块 |

玩家手持 lore 含 `§6皮肤方块` 的物品右键,即可把该方块收进自己的皮肤库存。

`IllegalMaterial` 里列了 24 种禁用方块(红石元件、活塞、铁砧、砂砾等),
选中会被自动剔除并回退到 `CUT_SANDSTONE`。
