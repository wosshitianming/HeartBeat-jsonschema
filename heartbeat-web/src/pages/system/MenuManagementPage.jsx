import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {iamApi} from '../../api'
import {
    BackendDataTable,
    MetricStrip,
    RecordDialog,
    StatusBadge,
    WorkspaceHeader
} from '../../components/admin/BackendWorkspace'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import './MenuManagementPage.css'

const MENU_TYPE_OPTIONS = [
    {value: 'CATALOG', label: '目录'},
    {value: 'MENU', label: '菜单'},
    {value: 'BUTTON', label: '按钮'}
]

const BOOLEAN_OPTIONS = [
    {value: 'true', label: '是'},
    {value: 'false', label: '否'}
]

const STATUS_OPTIONS = [
    {value: 'ENABLED', label: '启用'},
    {value: 'DISABLED', label: '停用'}
]

const MENU_FIELDS = [
    {name: 'menuName', label: '菜单名称', required: true, placeholder: '例如：用户管理'},
    {name: 'menuCode', label: '菜单编码', required: true, placeholder: '例如：system:user'},
    {name: 'menuType', label: '节点类型', type: 'select', options: MENU_TYPE_OPTIONS},
    {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
    {name: 'routePath', label: '路由地址', placeholder: '例如：/system/user'},
    {name: 'componentPath', label: '组件路径', placeholder: '例如：system/user/index'},
    {name: 'redirectPath', label: '重定向地址', placeholder: '目录可配置默认跳转地址'},
    {name: 'icon', label: '菜单图标', placeholder: '图标名称或标识'},
    {name: 'externalLink', label: '外部链接', placeholder: '例如：https://example.com'},
    {
        name: 'permissionMode',
        label: '权限模式',
        type: 'select',
        options: [{value: 'RELATION', label: '关联权限'}],
        defaultValue: 'RELATION',
        disabled: true,
        hint: '当前后端通过菜单与权限关系表完成授权。'
    },
    {name: 'visible', label: '导航可见', type: 'select', options: BOOLEAN_OPTIONS, defaultValue: 'true'},
    {name: 'keepAlive', label: '页面缓存', type: 'select', options: BOOLEAN_OPTIONS, defaultValue: 'false'},
    {name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS, defaultValue: 'ENABLED'}
]

function stringId(value, fallback = '0') {
    if (value === undefined || value === null || value === '') return fallback
    return String(value)
}

function booleanValue(value, fallback = false) {
    if (value === undefined || value === null || value === '') return fallback
    if (typeof value === 'boolean') return value
    return String(value).toLowerCase() === 'true' || String(value) === '1'
}

function normalizeMenuNode(node, fallbackParentId = '0') {
    const id = stringId(node?.id, '')
    const children = Array.isArray(node?.children)
        ? node.children.map((child) => normalizeMenuNode(child, id || fallbackParentId))
        : []
    return {
        ...node,
        id,
        parentId: stringId(node?.parentId, fallbackParentId),
        menuCode: node?.menuCode || '',
        menuName: node?.menuName || node?.name || '',
        menuType: node?.menuType || node?.type || 'MENU',
        routePath: node?.routePath ?? node?.path ?? '',
        componentPath: node?.componentPath ?? node?.component ?? '',
        redirectPath: node?.redirectPath ?? '',
        icon: node?.icon ?? '',
        externalLink: node?.externalLink ?? '',
        permissionMode: node?.permissionMode || node?.permission || 'RELATION',
        visible: booleanValue(node?.visible, true),
        keepAlive: booleanValue(node?.keepAlive, false),
        sortNo: Number.isFinite(Number(node?.sortNo)) ? Number(node.sortNo) : 0,
        status: node?.status || 'ENABLED',
        children
    }
}

function flattenMenus(nodes, target = []) {
    nodes.forEach((node) => {
        target.push(node)
        flattenMenus(node.children, target)
    })
    return target
}

function collectExpandableIds(nodes, target = new Set()) {
    nodes.forEach((node) => {
        if (node.children.length > 0) target.add(node.id)
        collectExpandableIds(node.children, target)
    })
    return target
}

function menuTypeLabel(type) {
    if (type === 'CATALOG') return '目录'
    if (type === 'BUTTON') return '按钮'
    return '菜单'
}

function menuTypeMarker(type) {
    if (type === 'CATALOG') return 'D'
    if (type === 'BUTTON') return 'B'
    return 'M'
}

function booleanLabel(value) {
    return value ? '是' : '否'
}

function MenuTreeNode({node, depth, selectedId, expandedIds, onSelect, onToggle}) {
    const hasChildren = node.children.length > 0
    const expanded = expandedIds.has(node.id)
    const selected = selectedId === node.id

    return (
        <li
            className="menu-tree-node"
            role="treeitem"
            aria-level={depth + 1}
            aria-selected={selected}
            aria-expanded={hasChildren ? expanded : undefined}
        >
            <div
                className={selected ? 'menu-tree-row selected' : 'menu-tree-row'}
                style={{'--menu-indent': `${depth * 12}px`}}
            >
                {hasChildren ? (
                    <button
                        type="button"
                        className="menu-tree-toggle"
                        aria-label={expanded ? `收起${node.menuName}` : `展开${node.menuName}`}
                        title={expanded ? '收起' : '展开'}
                        onClick={() => onToggle(node.id)}
                    >
                        <span aria-hidden="true">{expanded ? '⌄' : '›'}</span>
                    </button>
                ) : <span className="menu-tree-toggle-placeholder"/>}
                <button type="button" className="menu-tree-select" onClick={() => onSelect(node)}>
            <span className="menu-tree-type" data-type={node.menuType} aria-hidden="true">
              {menuTypeMarker(node.menuType)}
            </span>
                    <span className="menu-tree-copy">
              <strong>{node.menuName || '未命名节点'}</strong>
              <small>{node.menuCode || `ID ${node.id}`}</small>
            </span>
                    {node.status === 'DISABLED' && <span className="menu-tree-disabled">停用</span>}
                </button>
            </div>
            {hasChildren && expanded && (
                <ul role="group" className="menu-tree-list">
                    {node.children.map((child) => (
                        <MenuTreeNode
                            key={child.id}
                            node={child}
                            depth={depth + 1}
                            selectedId={selectedId}
                            expandedIds={expandedIds}
                            onSelect={onSelect}
                            onToggle={onToggle}
                        />
                    ))}
                </ul>
            )}
        </li>
    )
}

function MenuTree({nodes, selectedId, expandedIds, onSelect, onToggle}) {
    if (nodes.length === 0) {
        return <div className="menu-management-empty">暂无菜单节点</div>
    }
    return (
        <ul className="menu-tree-list menu-tree-root" role="tree" aria-label="系统菜单树">
            {nodes.map((node) => (
                <MenuTreeNode
                    key={node.id}
                    node={node}
                    depth={0}
                    selectedId={selectedId}
                    expandedIds={expandedIds}
                    onSelect={onSelect}
                    onToggle={onToggle}
                />
            ))}
        </ul>
    )
}

function DetailGrid({items}) {
    return (
        <dl className="menu-detail-grid">
            {items.map(({label, value, code}) => (
                <div key={label}>
                    <dt>{label}</dt>
                    <dd title={value === undefined || value === null ? undefined : String(value)}>
                        {code ?
                            <code>{value || '—'}</code> : (value === undefined || value === null || value === '' ? '—' : value)}
                    </dd>
                </div>
            ))}
        </dl>
    )
}

export default function MenuManagementPage({permissions = [], onError}) {
    const [menuTree, setMenuTree] = useState([])
    const [selectedId, setSelectedId] = useState('')
    const [expandedIds, setExpandedIds] = useState(new Set())
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState('')
    const [error, setError] = useState('')
    const [recordDialog, setRecordDialog] = useState(null)
    const controllerRef = useRef(null)
    const mountedRef = useRef(true)
    const canAdd = hasPermission(permissions, 'system:menu:add')
    const canEdit = hasPermission(permissions, 'system:menu:edit')
    const canRemove = hasPermission(permissions, 'system:menu:remove')

    useEffect(() => {
        mountedRef.current = true
        return () => {
            mountedRef.current = false
            controllerRef.current?.abort()
        }
    }, [])

    const notifyError = useCallback((message) => {
        setError(message)
        onError?.(message)
    }, [onError])

    const loadMenus = useCallback(async ({preferredId, expandId} = {}) => {
        controllerRef.current?.abort()
        const controller = new AbortController()
        controllerRef.current = controller
        setLoading(true)
        setError('')
        try {
            const rows = await iamApi.menus({signal: controller.signal})
            if (controller.signal.aborted || !mountedRef.current) return
            const normalizedTree = (Array.isArray(rows) ? rows : []).map((row) => normalizeMenuNode(row))
            const flat = flattenMenus(normalizedTree)
            const ids = new Set(flat.map((item) => item.id))
            setMenuTree(normalizedTree)
            setSelectedId((current) => {
                const requested = preferredId ? String(preferredId) : current
                return ids.has(requested) ? requested : (flat[0]?.id || '')
            })
            setExpandedIds((current) => {
                const expandable = collectExpandableIds(normalizedTree)
                const next = current.size === 0
                    ? new Set(expandable)
                    : new Set([...current].filter((id) => expandable.has(id)))
                if (expandId && expandable.has(String(expandId))) next.add(String(expandId))
                return next
            })
        } catch (loadError) {
            if (controller.signal.aborted || loadError?.name === 'AbortError') return
            notifyError(loadError?.message || '菜单数据加载失败')
        } finally {
            if (controllerRef.current === controller) controllerRef.current = null
            if (!controller.signal.aborted && mountedRef.current) setLoading(false)
        }
    }, [notifyError])

    useEffect(() => {
        loadMenus()
    }, [loadMenus])

    const flatMenus = useMemo(() => flattenMenus(menuTree, []), [menuTree])
    const menuById = useMemo(
        () => new Map(flatMenus.map((menu) => [menu.id, menu])),
        [flatMenus]
    )
    const selectedMenu = menuById.get(selectedId) || null
    const parentMenu = selectedMenu && selectedMenu.parentId !== '0'
        ? menuById.get(selectedMenu.parentId) || null
        : null
    const siblingMenus = useMemo(() => {
        if (!selectedMenu) return []
        if (selectedMenu.parentId === '0') return menuTree
        return menuById.get(selectedMenu.parentId)?.children || []
    }, [menuById, menuTree, selectedMenu])

    const metrics = useMemo(() => [
        {label: '全部节点', value: flatMenus.length, hint: `${menuTree.length} 个根节点`},
        {
            label: '目录',
            value: flatMenus.filter((menu) => menu.menuType === 'CATALOG').length,
            hint: '用于组织导航层级'
        },
        {
            label: '菜单',
            value: flatMenus.filter((menu) => menu.menuType === 'MENU').length,
            hint: '可访问业务页面'
        },
        {
            label: '按钮',
            value: flatMenus.filter((menu) => menu.menuType === 'BUTTON').length,
            hint: `${flatMenus.filter((menu) => menu.status === 'DISABLED').length} 个停用节点`
        }
    ], [flatMenus, menuTree.length])

    const siblingColumns = useMemo(() => [
        {key: 'menuName', label: '菜单名称'},
        {
            key: 'menuType',
            label: '类型',
            render: (value) => <span className="menu-type-badge" data-type={value}>{menuTypeLabel(value)}</span>
        },
        {key: 'routePath', label: '路由', render: (value) => <code>{value || '—'}</code>},
        {key: 'sortNo', label: '排序'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>}
    ], [])

    const recordInitialValues = useMemo(() => {
        const row = recordDialog?.row
        const parent = recordDialog?.parent
        return {
            menuName: row?.menuName || '',
            menuCode: row?.menuCode || '',
            menuType: row?.menuType || (parent ? 'MENU' : 'CATALOG'),
            routePath: row?.routePath || '',
            componentPath: row?.componentPath || '',
            redirectPath: row?.redirectPath || '',
            icon: row?.icon || '',
            externalLink: row?.externalLink || '',
            permissionMode: 'RELATION',
            visible: String(row?.visible ?? true),
            keepAlive: String(row?.keepAlive ?? false),
            sortNo: row?.sortNo ?? ((parent?.children?.length ?? menuTree.length) + 1) * 10,
            status: row?.status || 'ENABLED'
        }
    }, [menuTree.length, recordDialog])

    function toggleExpanded(id) {
        setExpandedIds((current) => {
            const next = new Set(current)
            if (next.has(id)) next.delete(id)
            else next.add(id)
            return next
        })
    }

    function openCreateRoot() {
        setError('')
        setRecordDialog({mode: 'create', parent: null, row: null})
    }

    function openCreateChild(parent = selectedMenu) {
        if (!parent || parent.menuType === 'BUTTON') return
        setError('')
        setRecordDialog({mode: 'create', parent, row: null})
    }

    function openEdit(row = selectedMenu) {
        if (!row) return
        setError('')
        setRecordDialog({mode: 'edit', parent: menuById.get(row.parentId) || null, row})
    }

    async function submitMenu(values) {
        if (!recordDialog) return
        const editing = recordDialog.mode === 'edit'
        const row = recordDialog.row
        const parentId = editing
            ? stringId(row?.parentId)
            : stringId(recordDialog.parent?.id)
        const payload = {
            parentId,
            menuCode: values.menuCode?.trim(),
            menuName: values.menuName?.trim(),
            menuType: values.menuType || 'MENU',
            routePath: values.routePath?.trim() || '',
            componentPath: values.componentPath?.trim() || '',
            redirectPath: values.redirectPath?.trim() || '',
            icon: values.icon?.trim() || '',
            externalLink: values.externalLink?.trim() || '',
            permissionMode: 'RELATION',
            visible: booleanValue(values.visible, true),
            keepAlive: booleanValue(values.keepAlive, false),
            sortNo: Number.isFinite(Number(values.sortNo)) ? Number(values.sortNo) : 0,
            status: values.status || 'ENABLED'
        }

        setBusy('save-menu')
        setError('')
        try {
            const saved = editing
                ? await iamApi.updateMenu(row.id, payload)
                : await iamApi.createMenu(payload)
            setRecordDialog(null)
            await loadMenus({
                preferredId: saved?.id || row?.id,
                expandId: parentId !== '0' ? parentId : undefined
            })
        } catch (saveError) {
            notifyError(saveError?.message || '菜单保存失败')
            throw saveError
        } finally {
            if (mountedRef.current) setBusy('')
        }
    }

    async function deleteMenu(menu = selectedMenu) {
        if (!menu || menu.children.length > 0) return
        const confirmed = typeof window.confirm !== 'function'
            || window.confirm(`确认删除菜单“${menu.menuName || menu.menuCode}”吗？`)
        if (!confirmed) return
        const nextSelection = menu.parentId !== '0'
            ? menu.parentId
            : siblingMenus.find((item) => item.id !== menu.id)?.id
        setBusy('delete-menu')
        setError('')
        try {
            await iamApi.deleteMenu(menu.id)
            await loadMenus({preferredId: nextSelection})
        } catch (deleteError) {
            notifyError(deleteError?.message || '菜单删除失败')
        } finally {
            if (mountedRef.current) setBusy('')
        }
    }

    const allExpandableIds = useMemo(() => collectExpandableIds(menuTree), [menuTree])
    const allExpanded = allExpandableIds.size > 0
        && [...allExpandableIds].every((id) => expandedIds.has(id))

    return (
        <section className="menu-management-page" aria-labelledby="menu-management-title">
            <WorkspaceHeader
                breadcrumb="系统管理 / 权限导航"
                title="菜单管理"
                description="按目录、菜单和按钮维护后台导航结构，路由展示与权限关系保持清晰分离。"
                status={error ? 'ERROR' : loading ? 'RUNNING' : 'ACTIVE'}
                loading={loading}
                onRefresh={() => loadMenus()}
                actions={canAdd ? (
                    <button
                        type="button"
                        className="button primary"
                        disabled={Boolean(busy)}
                        onClick={openCreateRoot}
                    >
                        新增根菜单
                    </button>
                ) : null}
            />

            {error && (
                <div className="menu-management-error" role="alert">
                    <span>{error}</span>
                    <button type="button" className="text-button" onClick={() => setError('')}>关闭</button>
                </div>
            )}

            <MetricStrip items={metrics}/>

            <div className="menu-management-layout">
                <aside className="panel menu-tree-panel" aria-label="菜单树">
                    <div className="menu-tree-heading">
                        <div>
                            <span className="step">TREE</span>
                            <h2>导航结构</h2>
                        </div>
                        {allExpandableIds.size > 0 && (
                            <button
                                type="button"
                                className="text-button"
                                onClick={() => setExpandedIds(allExpanded ? new Set() : new Set(allExpandableIds))}
                            >
                                {allExpanded ? '全部收起' : '全部展开'}
                            </button>
                        )}
                    </div>
                    <div className="menu-tree-scroll" aria-busy={loading}>
                        {loading && menuTree.length === 0 ? (
                            <div className="menu-management-empty">正在加载菜单树...</div>
                        ) : (
                            <MenuTree
                                nodes={menuTree}
                                selectedId={selectedId}
                                expandedIds={expandedIds}
                                onSelect={(menu) => setSelectedId(menu.id)}
                                onToggle={toggleExpanded}
                            />
                        )}
                    </div>
                </aside>

                <main className="menu-management-main">
                    {!selectedMenu ? (
                        <section className="panel menu-selection-empty">
                            <strong>选择一个菜单节点</strong>
                            <p>从左侧菜单树中选择节点后，可查看详情、维护子项和浏览同级菜单。</p>
                        </section>
                    ) : (
                        <>
                            <section className="panel menu-detail-panel" aria-labelledby="selected-menu-title">
                                <header className="menu-detail-heading">
                                    <div>
                                        <div className="menu-detail-title-line">
                          <span className="menu-type-badge" data-type={selectedMenu.menuType}>
                            {menuTypeLabel(selectedMenu.menuType)}
                          </span>
                                            <StatusBadge value={selectedMenu.status}/>
                                        </div>
                                        <h2 id="selected-menu-title">{selectedMenu.menuName || '未命名节点'}</h2>
                                        <code>{selectedMenu.menuCode || `ID ${selectedMenu.id}`}</code>
                                    </div>
                                    <div className="menu-detail-actions">
                                        {canAdd && (
                                            <button
                                                type="button"
                                                className="button primary"
                                                disabled={Boolean(busy) || selectedMenu.menuType === 'BUTTON'}
                                                title={selectedMenu.menuType === 'BUTTON' ? '按钮节点不能添加子项' : '新增子项'}
                                                onClick={() => openCreateChild(selectedMenu)}
                                            >
                                                新增子项
                                            </button>
                                        )}
                                        {canEdit && (
                                            <button type="button" className="button ghost" disabled={Boolean(busy)}
                                                    onClick={() => openEdit(selectedMenu)}>
                                                编辑
                                            </button>
                                        )}
                                        {canRemove && (
                                            <button
                                                type="button"
                                                className="button ghost menu-delete-button"
                                                disabled={Boolean(busy) || selectedMenu.children.length > 0}
                                                title={selectedMenu.children.length > 0 ? '请先处理子节点' : '删除菜单'}
                                                onClick={() => deleteMenu(selectedMenu)}
                                            >
                                                删除
                                            </button>
                                        )}
                                    </div>
                                </header>

                                <DetailGrid items={[
                                    {label: '上级菜单', value: parentMenu?.menuName || '顶级菜单'},
                                    {label: '节点 ID', value: selectedMenu.id, code: true},
                                    {label: '菜单编码', value: selectedMenu.menuCode, code: true},
                                    {label: '路由地址', value: selectedMenu.routePath, code: true},
                                    {label: '组件路径', value: selectedMenu.componentPath, code: true},
                                    {label: '重定向地址', value: selectedMenu.redirectPath, code: true},
                                    {label: '菜单图标', value: selectedMenu.icon},
                                    {label: '外部链接', value: selectedMenu.externalLink, code: true},
                                    {label: '权限模式', value: selectedMenu.permissionMode || 'RELATION'},
                                    {label: '导航可见', value: booleanLabel(selectedMenu.visible)},
                                    {label: '页面缓存', value: booleanLabel(selectedMenu.keepAlive)},
                                    {label: '排序', value: selectedMenu.sortNo},
                                    {label: '直接子项', value: selectedMenu.children.length}
                                ]}/>
                            </section>

                            <section className="menu-sibling-section" aria-labelledby="sibling-menu-title">
                                <header>
                                    <div>
                                        <p className="page-breadcrumb">同级菜单</p>
                                        <h2 id="sibling-menu-title">{parentMenu ? parentMenu.menuName : '顶级菜单'}</h2>
                                    </div>
                                    <span>{siblingMenus.length} 个节点</span>
                                </header>
                                <BackendDataTable
                                    ariaLabel="同级菜单列表"
                                    columns={siblingColumns}
                                    rows={siblingMenus}
                                    loading={loading}
                                    emptyText="当前层级暂无菜单"
                                    searchPlaceholder="搜索同级菜单"
                                    selectedId={selectedMenu.id}
                                    onSelect={(menu) => setSelectedId(menu.id)}
                                    rowActions={(row) => (
                                        <>
                                            <button type="button" className="table-link"
                                                    onClick={() => setSelectedId(row.id)}>
                                                查看
                                            </button>
                                            {canEdit && (
                                                <button type="button" className="table-link"
                                                        onClick={() => openEdit(row)}>
                                                    编辑
                                                </button>
                                            )}
                                        </>
                                    )}
                                />
                            </section>
                        </>
                    )}
                </main>
            </div>

            <RecordDialog
                open={Boolean(recordDialog)}
                title={`${recordDialog?.mode === 'edit' ? '编辑' : '新增'}菜单节点`}
                description={recordDialog?.mode === 'edit'
                    ? `当前上级：${recordDialog.parent?.menuName || '顶级菜单'}。编辑不会改变节点层级。`
                    : `新节点将创建在“${recordDialog.parent?.menuName || '顶级菜单'}”下。`}
                fields={MENU_FIELDS}
                initialValues={recordInitialValues}
                busy={busy === 'save-menu'}
                onClose={() => setRecordDialog(null)}
                onSubmit={submitMenu}
            />
        </section>
    )
}
