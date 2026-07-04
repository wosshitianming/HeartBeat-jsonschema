import AppearanceSettingsPanel from '../AppearanceSettingsPanel/AppearanceSettingsPanel'

function groupModules(modules) {
  return modules.reduce((groups, module) => {
    const category = module.category || '其他'
    if (!groups[category]) groups[category] = []
    groups[category].push(module)
    return groups
  }, {})
}

export default function AdminSidebar({
  modules,
  activeModuleKey,
  onSelect,
  selectedDefinition,
  colorMode,
  onColorModeChange,
  accentColor,
  onAccentColorChange,
  visualStyle,
  onVisualStyleChange,
  fluidEnabled,
  onFluidChange,
  syncState
}) {
  const groups = groupModules(modules)

  return (
      <aside className="glass-sidebar" aria-label="后台导航">
        <div className="sidebar-brand">
          <span className="brand-orb">H</span>
          <div>
            <strong>HeartBeat</strong>
            <span>管理控制台</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          {Object.entries(groups).map(([category, items]) => (
              <section className="sidebar-group" key={category}>
                <h2>{category}</h2>
                {items.map((module) => (
                    <button
                        type="button"
                        key={module.key}
                        className={activeModuleKey === module.key ? 'active' : ''}
                        onClick={() => onSelect(module.key)}
                    >
                      <span className="nav-icon" aria-hidden="true">{module.name.slice(0, 1)}</span>
                      <span className="nav-label">{module.name}</span>
                    </button>
                ))}
              </section>
          ))}
        </nav>

        <div className="sidebar-footer">
          <AppearanceSettingsPanel
              colorMode={colorMode}
              onColorModeChange={onColorModeChange}
              accentColor={accentColor}
              onAccentColorChange={onAccentColorChange}
              visualStyle={visualStyle}
              onVisualStyleChange={onVisualStyleChange}
              fluidEnabled={fluidEnabled}
              onFluidChange={onFluidChange}
              syncState={syncState}
          />
          <div className="sidebar-workspace">
            <small>当前工作区</small>
            <strong>{selectedDefinition?.name || '管理控制台'}</strong>
            <span>{selectedDefinition ? `上线版本 v${selectedDefinition.activeVersionNo || '—'}` : '业务数据与结构配置'}</span>
          </div>
        </div>
      </aside>
  )
}
