#!/bin/sh
# ─────────────────────────────────────────────────────────────
# 构建全部四个自研插件,并把产物拷进 server-1.21.11/plugins/。
#
# 为什么需要这个脚本:
#   插件 jar 不入库(.gitignore 排除了 server-1.21.11/plugins/*.jar),
#   而 download-plugins.sh 只还原 11 个第三方插件 —— 这四个自研的必须自己编。
#   克隆仓库后不跑这一步,服务端会缺 BridgingAnalyzer 等核心插件。
#
#   顺序还有个坑:BridgingSkin 的 plugin.yml 里是 depend 而非 softdepend,
#   编译期要从本地 Maven 仓库拿 BridgingAnalyzer,所以后者必须先 mvn install。
#   手工按文档敲很容易漏掉这个先后。
#
# 前置:
#   1. JDK 21 与 Maven 在 PATH 上(Git Bash 下可用 MVN=... 指定 mvn.cmd 绝对路径)
#   2. 先跑过 server-1.21.11/download-plugins.sh(install-deps.sh 需要 DecentHolograms 的 jar)
#
# 用法:
#   plugins-src/build-all.sh
#   MVN='D:/04-Dev-Env/apache-maven-3.9.9/bin/mvn.cmd' plugins-src/build-all.sh
# ─────────────────────────────────────────────────────────────
set -e

cd "$(dirname "$0")/.."
ROOT="$(pwd)"
PLUGINS="$ROOT/server-1.21.11/plugins"
MVN="${MVN:-mvn}"

if [ ! -d "$PLUGINS" ]; then
    echo "找不到 $PLUGINS" >&2
    exit 1
fi

echo "==> 安装无公共坐标的编译期依赖"
MVN="$MVN" "$ROOT/plugins-src/install-deps.sh"

# BridgingAnalyzer 必须排第一并用 install(装进本地仓库),BridgingSkin 编译时要引用它
echo
echo "==> 构建 BridgingAnalyzer(install:BridgingSkin 依赖它)"
( cd "$ROOT/plugins-src/BridgingAnalyzer" && "$MVN" -q -B install )

for p in BlockLv CpsCounter BridgingSkin; do
    echo "==> 构建 $p"
    ( cd "$ROOT/plugins-src/$p" && "$MVN" -q -B package )
done

echo
echo "==> 部署到 $PLUGINS"
found=0
for p in BridgingAnalyzer BlockLv CpsCounter BridgingSkin; do
    jar=$(ls "$ROOT/plugins-src/$p/target/"*.jar 2>/dev/null | grep -v -- '-sources\|-javadoc' | head -1)
    if [ -z "$jar" ]; then
        echo "  !! $p 没有产物" >&2
        exit 1
    fi
    cp "$jar" "$PLUGINS/"
    echo "  $(basename "$jar")"
    found=$((found + 1))
done

echo
echo "完成,共 $found 个插件。"
echo "服务端若在运行,需重启才会加载新 jar。"
