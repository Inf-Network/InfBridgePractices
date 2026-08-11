#!/bin/sh
# 使用 Gradle Kotlin DSL 构建全部四个插件。
#
# 用法:
#   ./build-all.sh
#   DEPLOY_DIR=/path/to/server/plugins ./build-all.sh
#
# 默认只构建；设置 DEPLOY_DIR 后会把四个 JAR 一并复制到服务端插件目录。
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE=${GRADLE:-gradle}

"$GRADLE" -p "$ROOT" --no-daemon clean build

echo
echo "构建完成:"
for project in BridgingAnalyzer BlockLv CpsCounter BridgingSkin; do
    for jar in "$ROOT/$project/build/libs/"*.jar; do
        [ -f "$jar" ] || continue
        echo "  $jar"
        if [ -n "${DEPLOY_DIR:-}" ]; then
            if [ ! -d "$DEPLOY_DIR" ]; then
                echo "部署目录不存在: $DEPLOY_DIR" >&2
                exit 1
            fi
            cp "$jar" "$DEPLOY_DIR/"
        fi
    done
done

if [ -n "${DEPLOY_DIR:-}" ]; then
    echo "已部署到: $DEPLOY_DIR"
fi
