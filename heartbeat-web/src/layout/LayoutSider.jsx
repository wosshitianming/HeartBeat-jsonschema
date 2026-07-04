function SideMenuItems({ items, activeModuleKey, onSelect, depth = 0 }) {
  return items.map((item) => {
    if (item.type === 'BUTTON') return null
    const hasChildren = Array.isArray(item.children) && item.children.length > 0
    const isMenu = item.type === 'MENU'
    const isActive = item.id === activeModuleKey
    if (hasChildren && !isMenu) {
      return (
          <div key={item.id}>
            <div className="hb-side-group-title" style={{ paddingLeft: `${12 + depth * 8}px` }}>
              {item.name}
            </div>
            <SideMenuItems
                items={item.children}
                activeModuleKey={activeModuleKey}
                onSelect={onSelect}
                depth={depth + 1}
            />
          </div>
      )
    }
    if (!isMenu && !hasChildren) return null
    return (
        <div key={item.id} className="hb-side-menu">
          <button
              type="button"
              className={isActive ? 'active' : ''}
              style={{ paddingLeft: `${12 + depth * 8}px` }}
              onClick={() => onSelect(item)}
          >
            {item.name}
          </button>
          {hasChildren && (
              <SideMenuItems
                  items={item.children}
                  activeModuleKey={activeModuleKey}
                  onSelect={onSelect}
                  depth={depth + 1}
              />
          )}
        </div>
    )
  })
}

export default function LayoutSider({ sideMenus, activeModuleKey, onSelectMenu }) {
  return (
      <aside className="hb-layout-sider" aria-label="后台导航">
        <div className="hb-sider-logo" aria-hidden="true" />
        <SideMenuItems
            items={sideMenus}
            activeModuleKey={activeModuleKey}
            onSelect={onSelectMenu}
        />
        {sideMenus.length <= 1 && (
            <div className="hb-sider-ghosts" aria-hidden="true">
              <span />
              <span />
              <span />
              <span />
            </div>
        )}
      </aside>
  )
}
