#!/bin/bash
set -e

BASE_DIR="d:/Desktop/自定义文件夹/HeartBeat-jsonschema"
ENTITY_GEN="$BASE_DIR/heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/gen"
ENTITY_PARENT="$BASE_DIR/heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity"
MAPPER_GEN="$BASE_DIR/heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/gen"
MAPPER_PARENT="$BASE_DIR/heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper"

# 领域分类函数
get_domain_for_do() {
    local name="$1"
    case "$name" in
        FlowWaitState*|SysInbox*|SysOutbox*) echo "event"; return ;;
        Auth*) echo "auth"; return ;;
        Hb*) echo "flow"; return ;;
        Wf*) echo "workflow"; return ;;
        Pay*) echo "pay"; return ;;
        Report*) echo "report"; return ;;
        Mobile*) echo "mobile"; return ;;
        Mp*) echo "mp"; return ;;
        Structure*) echo "structure"; return ;;
        Sys*) echo "sys"; return ;;
        *) echo "" ;;
    esac
}

echo "=== 创建子包目录 ==="
for d in auth flow workflow pay report mobile mp structure sys event tool common; do
    mkdir -p "$ENTITY_PARENT/$d"
    mkdir -p "$MAPPER_PARENT/$d"
done

echo "=== 移动并重写 DO 文件 ==="
for f in "$ENTITY_GEN"/*.java; do
    [ -f "$f" ] || continue
    base=$(basename "$f" .java)
    domain=$(get_domain_for_do "$base")
    if [ -z "$domain" ]; then
        echo "  未匹配: $base"
        continue
    fi
    target_dir="$ENTITY_PARENT/$domain"
    target_file="$target_dir/$(basename "$f")"
    # 替换 package
    sed "s|^package top\.kx\.heartbeat\.infrastructure\.persistence\.entity\.gen;|package top.kx.heartbeat.infrastructure.persistence.entity.$domain;|" "$f" > "$target_file"
    rm "$f"
    echo "  $base.java -> entity/$domain/"
done

echo "=== 移动并重写 Mapper 文件 ==="
for f in "$MAPPER_GEN"/*.java; do
    [ -f "$f" ] || continue
    base=$(basename "$f" .java)
    # FlowWaitState/SysInbox/SysOutbox 特殊处理
    case "$base" in
        FlowWaitState*|SysInbox*|SysOutbox*) domain="event" ;;
        *) domain=$(get_domain_for_do "$base") ;;
    esac
    if [ -z "$domain" ]; then
        echo "  未匹配: $base"
        continue
    fi
    target_dir="$MAPPER_PARENT/$domain"
    target_file="$target_dir/$(basename "$f")"
    # 替换 package + DO import 引用
    sed -e "s|^package top\.kx\.heartbeat\.infrastructure\.persistence\.mapper\.gen;|package top.kx.heartbeat.infrastructure.persistence.mapper.$domain;|" \
        -e "s|top\.kx\.heartbeat\.infrastructure\.persistence\.entity\.gen\.|top.kx.heartbeat.infrastructure.persistence.entity.$domain.|g" \
        "$f" > "$target_file"
    rm "$f"
    echo "  $base.java -> mapper/$domain/"
done

echo "=== 删除空的 gen 目录 ==="
[ -d "$ENTITY_GEN" ] && rmdir "$ENTITY_GEN" 2>/dev/null && echo "  删除 entity/gen" || true
[ -d "$MAPPER_GEN" ] && rmdir "$MAPPER_GEN" 2>/dev/null && echo "  删除 mapper/gen" || true

echo "=== 完成 ==="