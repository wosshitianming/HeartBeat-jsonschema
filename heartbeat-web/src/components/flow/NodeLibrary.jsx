import {memo, useMemo, useState} from 'react'
import {
    Box,
    CaseUpper,
    ChevronDown,
    CircleStop,
    Database,
    GitBranch,
    Globe2,
    PanelLeftClose,
    Play,
    RadioTower,
    ScrollText,
    Search,
    Shuffle,
    Webhook,
    Zap
} from 'lucide-react'

const iconByName = {
    play: Play,
    webhook: Webhook,
    database: Database,
    redis: Database,
    mq: RadioTower,
    http: Globe2,
    condition: GitBranch,
    'case-upper': CaseUpper,
    mapper: Shuffle,
    log: ScrollText,
    end: CircleStop
}

const categoryOrder = ['触发器', '数据源', '消息', '动作', '逻辑', '转换', '输出', '系统']

function ComponentIcon({component}) {
    const Icon = iconByName[String(component?.icon || '').toLowerCase()]
        || (component?.category === '触发器' ? Zap : Box)
    return <Icon size={16} aria-hidden="true"/>
}

function normalizedText(value) {
    return String(value || '').trim().toLocaleLowerCase()
}

function NodeLibrary({components = [], disabled, onAddNode, onDragStart, onClose}) {
    const [query, setQuery] = useState('')
    const [collapsed, setCollapsed] = useState(() => new Set())
    const groups = useMemo(() => {
        const keyword = normalizedText(query)
        const filtered = components
            .filter((component) => {
                if (!keyword) return true
                return [component.name, component.type, component.category, component.description, component.version]
                    .some((value) => normalizedText(value).includes(keyword))
            })
            .slice()
            .sort((left, right) => (left.sortNo || 0) - (right.sortNo || 0)
                || String(left.name || left.type).localeCompare(String(right.name || right.type), 'zh-CN'))
        const result = new Map()
        for (const component of filtered) {
            const category = component.category || '其他'
            if (!result.has(category)) result.set(category, [])
            result.get(category).push(component)
        }
        return [...result.entries()].sort(([left], [right]) => {
            const leftIndex = categoryOrder.indexOf(left)
            const rightIndex = categoryOrder.indexOf(right)
            if (leftIndex >= 0 || rightIndex >= 0) {
                return (leftIndex < 0 ? categoryOrder.length : leftIndex)
                    - (rightIndex < 0 ? categoryOrder.length : rightIndex)
            }
            return left.localeCompare(right, 'zh-CN')
        })
    }, [components, query])

    const toggleCategory = (category) => {
        setCollapsed((current) => {
            const next = new Set(current)
            if (next.has(category)) next.delete(category)
            else next.add(category)
            return next
        })
    }

    return (
        <div className="flow-editor-library">
            <header className="flow-editor-library-header">
                <div>
                    <strong>节点</strong>
                    <span>{components.length} 个可用组件</span>
                </div>
                <button type="button" onClick={onClose} title="关闭节点库" aria-label="关闭节点库">
                    <PanelLeftClose size={17} aria-hidden="true"/>
                </button>
            </header>

            <label className="flow-editor-library-search">
                <Search size={15} aria-hidden="true"/>
                <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="搜索节点"
                    aria-label="搜索节点"
                />
                {query && <span>{groups.reduce((count, [, items]) => count + items.length, 0)}</span>}
            </label>

            <div className="flow-editor-library-groups">
                {groups.map(([category, items]) => {
                    const isCollapsed = collapsed.has(category) && !query
                    return (
                        <section className="flow-editor-library-group" key={category}>
                            <button
                                type="button"
                                className="flow-editor-library-group-heading"
                                onClick={() => toggleCategory(category)}
                                aria-expanded={!isCollapsed}
                            >
                                <ChevronDown size={13} aria-hidden="true"/>
                                <span>{category}</span>
                                <b>{items.length}</b>
                            </button>
                            {!isCollapsed && (
                                <div className="flow-editor-library-items">
                                    {items.map((component) => (
                                        <button
                                            type="button"
                                            className="flow-editor-library-item"
                                            key={`${component.type}@${component.version}`}
                                            disabled={disabled}
                                            draggable={!disabled}
                                            onDragStart={(event) => onDragStart?.(event, component)}
                                            onClick={() => onAddNode?.(component)}
                                            title={component.description || component.name}
                                        >
                              <span className="flow-editor-library-item-icon" aria-hidden="true">
                                <ComponentIcon component={component}/>
                              </span>
                                            <span className="flow-editor-library-item-copy">
                                <strong>{component.name || component.type}</strong>
                                <small>{component.description || component.type}</small>
                              </span>
                                            <i>v{component.version}</i>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </section>
                    )
                })}

                {groups.length === 0 && (
                    <div className="flow-editor-library-empty">
                        <Search size={21} aria-hidden="true"/>
                        <strong>没有匹配的节点</strong>
                        <span>0 个结果</span>
                    </div>
                )}
            </div>
        </div>
    )
}

export default memo(NodeLibrary)
