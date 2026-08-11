# CpsCounter — 1.21.11 移植版

点击速度(CPS)统计。

## 出处与许可

上游 **[SakuraKoi/CPSCounter](https://github.com/SakuraKoi/CPSCounter)**,原作者
**SakuraKooi**。本目录是 v3 的 1.21.11 移植版,源码取自对部署 jar 的
CFR 0.152 反编译。包名保留 `sakura.kooi` 以尊重出处。

## 相对原版的改动

移植面极小 —— 全插件未使用任何 `Material` / `Sound` 常量,监听的两个事件
(`PlayerInteractEvent` / `PlayerQuitEvent`)在 1.21 中均存在,也没有废弃 API 调用。

| 改动 | 原因 |
|---|---|
| 重写 `ActionBarUtils` | 原版用 NMS 反射拼 `PacketPlayOutChat` 发包,Paper 自 1.20.5 起去掉了包名里的版本号,反射初始化必然失败。改用 Adventure `Player#sendActionBar` |
| 删除 `Metrics.java` | 依赖 `org.json.simple`(1.8 内置、现代已移除),且**在本插件里完全没有被引用**,是死代码 |

ActionBar 文本经 `LegacyComponentSerializer` 解析 —— CPS 显示文本是 `§` 颜色码
格式,不这样处理会把颜色码当普通字符显示。

## 构建

需要 JDK 21 与 Maven。

```bash
mvn package
```

产物 `target/CpsCounter-3-1.21.11.jar`。

## 命令与权限

| 命令 | 权限 | 作用 |
|---|---|---|
| `/cps [玩家]` | `cpscounter.cps` | 查看自己或他人的 CPS |

| 权限 | 作用 |
|---|---|
| `cpscounter.bypass` | 免于被他人查询 CPS |
