#!/usr/bin/env python3
"""BridgingAnalyzer v28 从 Spigot 1.8.8 移植到 Paper 1.21.11 的 API 适配补丁。

只改因 API 变更而无法编译的地方,不动任何业务逻辑 —— 玩法行为必须与 2021 年
的原版逐帧一致,这是搭路服的立身之本。

每一处替换都在下面注明了变更来自哪个版本。
"""
import os
import re
import sys

SRC = sys.argv[1] if len(sys.argv) > 1 else "src/main/java/sakura/kooi/BridgingAnalyzer"

# ── 全局的一对一常量改名 ────────────────────────────────────────────────
# 正则用 \b 收尾,避免 SLOW 误伤 SLOW_DIGGING / SLOW_FALLING。
RENAMES = [
    # 1.20.5 药水效果统一改用原版 id 命名
    (r"\bPotionEffectType\.SLOW\b",        "PotionEffectType.SLOWNESS"),
    # 1.13 扁平化:金 -> golden
    (r"\bMaterial\.GOLD_PICKAXE\b",        "Material.GOLDEN_PICKAXE"),
    # 1.13 扁平化:石砖复数化
    (r"\bMaterial\.SMOOTH_BRICK\b",        "Material.STONE_BRICKS"),
    # 1.13 扁平化:压力板按材质拆分,金压力板 = 轻质
    (r"\bMaterial\.GOLD_PLATE\b",          "Material.LIGHT_WEIGHTED_PRESSURE_PLATE"),
    # 1.13 扁平化:方块与切片同名合并
    (r"\bMaterial\.MELON_BLOCK\b",         "Material.MELON"),
    # 1.9 音效全面改名为 <类别>_<主体>_<动作>
    (r"\bSound\.LEVEL_UP\b",               "Sound.ENTITY_PLAYER_LEVELUP"),
    # 1.19 实体类型改用原版 id
    (r"\bEntityType\.FIREWORK\b",          "EntityType.FIREWORK_ROCKET"),
]

# ── 需要结构性改写的位置 ────────────────────────────────────────────────
REWRITES = {
    # 1.13 取消了流动水/静止水的区分,STATIONARY_WATER 已不存在
    "utils/ParticleEffects.java": [
        ("material == Material.WATER || material == Material.STATIONARY_WATER",
         "material == Material.WATER"),
    ],

    # 1.13 之后 STAINED_GLASS_PANE 拆成 16 色、告示牌拆成各木种 + 墙上/立式,
    # 无法再用单个常量比较。抽成 isPassThrough 辅助方法,语义与原版一致:
    # 信标传送时向上/向下穿透"空气、染色玻璃板、告示牌"。
    "TriggerBlockListener.java": [
        ("(to.getType() == Material.AIR || to.getType() == Material.STAINED_GLASS_PANE "
         "|| to.getType() == Material.WALL_SIGN || to.getType() == Material.SIGN_POST) "
         "&& to.getY() < 255",
         "isPassThrough(to.getType()) && to.getY() < to.getWorld().getMaxHeight() - 1"),
        ("(to.getType() == Material.AIR || to.getType() == Material.STAINED_GLASS_PANE "
         "|| to.getType() == Material.WALL_SIGN || to.getType() == Material.SIGN_POST) "
         "&& to.getY() > 0",
         "isPassThrough(to.getType()) && to.getY() > to.getWorld().getMinHeight()"),
    ],

    # 1.13 之后 ItemStack 不再有 data 字节,方块状态由 BlockData 表达。
    # 原意是"补一个与手中同类型的方块",去掉 data 参数即可等价。
    # 同时 getItemInHand() 在 1.9 双持后已废弃,换成主手取物。
    "CounterListener.java": [
        ("new ItemStack(e.getPlayer().getItemInHand().getType(), 1, 0, "
         "Byte.valueOf(e.getPlayer().getItemInHand().getData().getData()))",
         "new ItemStack(e.getPlayer().getInventory().getItemInMainHand().getType(), 1)"),
    ],
}

# ── 需要新增的辅助方法 ──────────────────────────────────────────────────
PASS_THROUGH_HELPER = '''
    /**
     * 判断方块能否被信标传送穿透。
     *
     * 1.8 时代这里是三个常量的直接比较(AIR / STAINED_GLASS_PANE / WALL_SIGN /
     * SIGN_POST)。1.13 扁平化后玻璃板拆成 16 色、告示牌按木种与朝向拆成几十种,
     * 只能改成按类型族判断。
     *
     * @param type 待判断的方块类型
     * @return 可穿透返回 true
     */
    private static boolean isPassThrough(org.bukkit.Material type) {
        if (type == org.bukkit.Material.AIR) {
            return true;
        }
        // 覆盖 16 色染色玻璃板与无色玻璃板
        if (type.name().endsWith("GLASS_PANE")) {
            return true;
        }
        // Tag.ALL_SIGNS 同时涵盖立式、墙上与悬挂告示牌的全部木种
        return org.bukkit.Tag.ALL_SIGNS.isTagged(type);
    }
'''


def main():
    changed = {}

    for root, _, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(root, fn)
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            text = original = open(path, encoding="utf-8").read()

            for pattern, repl in RENAMES:
                text, n = re.subn(pattern, repl, text)
                if n:
                    changed.setdefault(rel, []).append(f"{pattern} -> {repl}  ({n} 处)")

            for old, new in REWRITES.get(rel, []):
                # 源码里这些表达式是单行的,先把补丁里的换行折叠掉再匹配
                needle = " ".join(old.split())
                flat = re.sub(r"\s+", " ", text)
                if needle not in flat:
                    changed.setdefault(rel, []).append(f"!! 未匹配: {needle[:60]}...")
                    continue
                # 在原文里按空白无关的方式定位并替换
                pat = re.escape(needle).replace(r"\ ", r"\s+")
                text, n = re.subn(pat, new.replace("\\", "\\\\"), text)
                changed.setdefault(rel, []).append(f"改写 {needle[:52]}...  ({n} 处)")

            if rel == "TriggerBlockListener.java" and "isPassThrough" not in original:
                # 插到类体的最后一个右花括号之前
                idx = text.rstrip().rfind("}")
                text = text[:idx] + PASS_THROUGH_HELPER + text[idx:]
                changed.setdefault(rel, []).append("新增 isPassThrough 辅助方法")

            if text != original:
                open(path, "w", encoding="utf-8", newline="\n").write(text)

    for rel in sorted(changed):
        print(f"  {rel}")
        for c in changed[rel]:
            print(f"      {c}")


if __name__ == "__main__":
    main()
