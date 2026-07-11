function menuMarker(item) {
    const name = String(item?.name || '').trim()
    return name.slice(0, 1) || '·'
}

function SideMenuItems({items = [], activeModuleKey, onSelect, depth = 0}) {
  return items.map((item) => {
    if (item.type === 'BUTTON') return null
    const hasChildren = Array.isArray(item.children) && item.children.length > 0
    const isMenu = item.type === 'MENU'
    const isActive = item.id === activeModuleKey

    if (hasChildren && !isMenu) {
      return (
          <section className="hb-side-group" key={item.id}>
              <div className="hb-side-group-title" style={{paddingLeft: `${12 + depth * 8}px`}}>
                  <span className="hb-side-group-marker" aria-hidden="true"/>
                  <span>{item.name}</span>
            </div>
            <SideMenuItems
                items={item.children}
                activeModuleKey={activeModuleKey}
                onSelect={onSelect}
                depth={depth + 1}
            />
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
              />
          )}
        </div>
    )
  })
}

export default function LayoutSider({sideMenus = [], activeModuleKey, onSelectMenu}) {
  return (
      <aside className="hb-layout-sider" aria-label="后台导航">
          <nav className="hb-side-navigation" aria-label="功能菜单">
              <SideMenuItems
                  items={sideMenus}
                  activeModuleKey={activeModuleKey}
                  onSelect={onSelectMenu}
              />
          </nav>
      </aside>
  )
}
