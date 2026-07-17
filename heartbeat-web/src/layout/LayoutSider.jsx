import {useEffect, useMemo, useRef, useState} from 'react'
import {
    Activity,
    BarChart3,
    Braces,
    CreditCard,
    Database,
    Layers3,
    LayoutDashboard,
    Radio,
    Search,
    ShieldCheck,
    Smartphone,
    UsersRound,
    Workflow,
    Wrench
} from 'lucide-react'

function menuMarker(item) {
    const identity = `${item?.id || ''} ${item?.menuCode || ''}`.toLowerCase()
    let Icon = Layers3
    if (identity.includes('dashboard') || identity.includes('home')) Icon = LayoutDashboard
    else if (/tenant|user|dept|post/.test(identity)) Icon = UsersRound
    else if (/role|menu|oauth|social|permission/.test(identity)) Icon = ShieldCheck
    else if (/structure|dict|config|data/.test(identity)) Icon = Database
    else if (/workflow|flow/.test(identity)) Icon = Workflow
    else if (/pay|cashier/.test(identity)) Icon = CreditCard
    else if (/\bmp\b|official/.test(identity)) Icon = Radio
    else if (/report|analytics/.test(identity)) Icon = BarChart3
    else if (/mobile|app/.test(identity)) Icon = Smartphone
    else if (/monitor|server|cache|druid|log|session/.test(identity)) Icon = Activity
    else if (/gen|code/.test(identity)) Icon = Braces
    else if (/tool|job|schedule/.test(identity)) Icon = Wrench
    return <Icon size={16} strokeWidth={1.9}/>
}

function filterMenuTree(items, query) {
    const normalized = query.trim().toLocaleLowerCase('zh-CN')
    if (!normalized) return items

    return items.reduce((result, item) => {
        const name = String(item?.name || '').toLocaleLowerCase('zh-CN')
        if (name.includes(normalized)) {
            result.push(item)
            return result
        }
        const children = filterMenuTree(item.children || [], query)
        if (children.length > 0) result.push({...item, children})
        return result
    }, [])
}

function directoryIds(items, target = new Set()) {
    items.forEach((item) => {
        if (Array.isArray(item.children) && item.children.length > 0) {
            target.add(item.id)
            directoryIds(item.children, target)
        }
    })
    return target
}

function activeAncestorIds(items, activeId, ancestors = []) {
    for (const item of items) {
        const hasChildren = Array.isArray(item.children) && item.children.length > 0
        if (item.id === activeId) return ancestors
        if (hasChildren) {
            const found = activeAncestorIds(item.children, activeId, [...ancestors, item.id])
            if (found) return found
        }
    }
    return null
}

function firstDirectoryId(items) {
    return items.find((item) => Array.isArray(item.children) && item.children.length > 0)?.id
}

function SideMenuItems({items = [], activeModuleKey, onSelect, depth = 0, expandedIds, onToggle}) {
  return items.map((item) => {
    if (item.type === 'BUTTON') return null
    const hasChildren = Array.isArray(item.children) && item.children.length > 0
    const isMenu = item.type === 'MENU'
    const isActive = item.id === activeModuleKey

    if (hasChildren && !isMenu) {
        const expanded = expandedIds.has(item.id)
        const regionId = `side-group-${String(item.id).replace(/[^a-zA-Z0-9_-]/g, '-')}`
      return (
          <section className="hb-side-group" key={item.id}>
              <button
                  className="hb-side-group-title"
                  type="button"
                  style={{paddingLeft: `${10 + depth * 8}px`}}
                  aria-expanded={expanded}
                  aria-controls={regionId}
                  onClick={() => onToggle(item.id)}
              >
                  <span className="hb-side-group-marker" aria-hidden="true"/>
                  <span>{item.name}</span>
                  <span className="hb-side-group-chevron" aria-hidden="true">⌄</span>
              </button>
              <div id={regionId} className="hb-side-group-children" hidden={!expanded}>
                  <SideMenuItems
                      items={item.children}
                      activeModuleKey={activeModuleKey}
                      onSelect={onSelect}
                      depth={depth + 1}
                      expandedIds={expandedIds}
                      onToggle={onToggle}
                  />
              </div>
          </section>
      )
    }

    if (!isMenu && !hasChildren) return null

    return (
        <div key={item.id} className="hb-side-menu">
          <button
              type="button"
              className={isActive ? 'active' : ''}
              style={{paddingLeft: `${12 + depth * 8}px`}}
              aria-current={isActive ? 'page' : undefined}
              onClick={() => onSelect?.(item)}
          >
              <span className="hb-side-menu-icon" aria-hidden="true">{menuMarker(item)}</span>
              <span className="hb-side-menu-label">{item.name}</span>
          </button>
          {hasChildren && (
              <SideMenuItems
                  items={item.children}
                  activeModuleKey={activeModuleKey}
                  onSelect={onSelect}
                  depth={depth + 1}
                  expandedIds={expandedIds}
                  onToggle={onToggle}
              />
          )}
        </div>
    )
  })
}

export default function LayoutSider({sideMenus = [], activeModuleKey, onSelectMenu}) {
    const [query, setQuery] = useState('')
    const searchRef = useRef(null)
    const activeAncestors = useMemo(
        () => activeAncestorIds(sideMenus, activeModuleKey) || [],
        [activeModuleKey, sideMenus]
    )
    const [expandedIds, setExpandedIds] = useState(() => new Set(activeAncestors))
    const filteredMenus = useMemo(() => filterMenuTree(sideMenus, query), [query, sideMenus])
    const visibleExpandedIds = useMemo(
        () => query.trim() ? directoryIds(filteredMenus) : expandedIds,
        [expandedIds, filteredMenus, query]
    )

    useEffect(() => {
        setExpandedIds((current) => {
            const next = new Set(current)
            activeAncestors.forEach((id) => next.add(id))
            if (next.size === 0) {
                const first = firstDirectoryId(sideMenus)
                if (first) next.add(first)
            }
            return next
        })
    }, [activeAncestors, sideMenus])

    useEffect(() => {
        function focusMenuSearch(event) {
            if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
                event.preventDefault()
                searchRef.current?.focus()
            }
        }

        window.addEventListener('keydown', focusMenuSearch)
        return () => window.removeEventListener('keydown', focusMenuSearch)
    }, [])

    function toggleGroup(id) {
        setExpandedIds((current) => {
            const next = new Set(current)
            if (next.has(id)) next.delete(id)
            else next.add(id)
            return next
        })
    }

  return (
      <aside className="hb-layout-sider" aria-label="后台导航">
          <label className="hb-sidebar-search">
              <Search size={15} aria-hidden="true"/>
              <input
                  ref={searchRef}
                  type="search"
                  value={query}
                  placeholder="搜索菜单"
                  aria-label="搜索菜单"
                  onChange={(event) => setQuery(event.target.value)}
              />
              <kbd>⌘ K</kbd>
          </label>
          <nav className="hb-side-navigation" aria-label="功能菜单">
              <SideMenuItems
                  items={filteredMenus}
                  activeModuleKey={activeModuleKey}
                  onSelect={onSelectMenu}
                  expandedIds={visibleExpandedIds}
                  onToggle={toggleGroup}
              />
              {filteredMenus.length === 0 && (
                  <p className="hb-sidebar-empty">没有匹配的菜单</p>
              )}
          </nav>
          <a className="hb-sidebar-support" href="mailto:support@heartbeat.local">
              <span aria-hidden="true">?</span>
              <span>
            <strong>帮助与支持</strong>
            <small>联系我们</small>
          </span>
          </a>
      </aside>
  )
}
