#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量改造 HeartBeat-jsonschema 项目：
1. 删除 Entity 中的 mybatisflex 注解
2. 删除 Mapper 中 BaseMapper 继承
3. 删除 FlexRepository/FlexConfig 等文件
4. 将 HbFlowDefinition/HbFlowVersion 重命名为 *DO
"""
import os
import re
import sys
from pathlib import Path

ROOT = Path(r'd:\Desktop\自定义文件夹\HeartBeat-jsonschema')

# ============ 工具函数 ============
def read(p: Path) -> str:
    return p.read_text(encoding='utf-8')

def write(p: Path, content: str) -> None:
    p.write_text(content, encoding='utf-8')

# ============ 1. 改造 Entity ============
def transform_entity(content: str) -> str:
    # 去除 mybatisflex import
    content = re.sub(r'import\s+com\.mybatisflex\.annotation\.[A-Za-z]+;\s*\n', '', content)
    # 去除类上的 @Table
    content = re.sub(r'@Table\([^)]*\)\s*\n', '', content)
    # 去除字段上的 @Id / @Column
    content = re.sub(r'@Id\([^)]*\)\s*\n', '', content)
    content = re.sub(r'@Column\([^)]*\)\s*\n', '', content)
    # 多行 @Column
    content = re.sub(r'@Column\([^)]*\)\s*\n', '', content, flags=re.DOTALL)
    return content

entity_dir = ROOT / 'heartbeat-infrastructure' / 'src' / 'main' / 'java' / 'top' / 'kx' / 'heartbeat' / 'infrastructure' / 'persistence' / 'entity'
entity_files = [p for p in entity_dir.rglob('*.java') if 'example' not in str(p)]
# 跳过已经处理过的 DO 文件
entity_files = [p for p in entity_files if not p.name.endswith('DO.java')]

for f in entity_files:
    try:
        c = read(f)
        nc = transform_entity(c)
        if nc != c:
            write(f, nc)
            print(f'[Entity] {f.name} -> 处理完成')
    except Exception as e:
        print(f'[ERR Entity] {f}: {e}', file=sys.stderr)

print('=== Entity 阶段完成 ===\n')