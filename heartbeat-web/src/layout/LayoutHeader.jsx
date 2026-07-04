export default function LayoutHeader({
  brand = 'HeartBeat',
  topModules,
  activeTopModuleId,
  currentUser,
  busy,
  fluidEnabled,
  onFluidChange,
  onSelectTopModule,
  glassMode,
  onGlassModeChange,
  onRefresh,
  onLogout
}) {
  return (
      <header className="hb-layout-header">
        <div className="hb-layout-brand">
          <span />
          <div>{brand}</div>
        </div>
        <nav className="hb-top-nav" aria-label="顶栏导航">
          {topModules.map((module) => (
              <button
                  key={module.id}
                  type="button"
                  className={module.id === activeTopModuleId ? 'active' : ''}
                  onClick={() => onSelectTopModule(module.id)}
              >
                {module.name}
              </button>
          ))}
        </nav>
        <div className="hb-header-tools">
          <button type="button" disabled={Boolean(busy)}>
            主题与视觉效果
          </button>
          <label className="hb-header-switch">
            <input
                type="checkbox"
                role="switch"
                aria-label="背景动效"
                checked={Boolean(fluidEnabled)}
                onChange={(event) => onFluidChange?.(event.target.checked)}
                disabled={Boolean(busy)}
            />
            背景动效
          </label>
          <div className="hb-surface-switcher" aria-label="玻璃表面模式">
            {[
              ['immersive', '沉浸'],
              ['balanced', '均衡'],
              ['restrained', '克制'],
              ['flat', '简洁']
            ].map(([mode, label]) => (
                <button
                    key={mode}
                    type="button"
                    className={glassMode === mode ? 'active' : ''}
                    onClick={() => onGlassModeChange?.(mode)}
                    disabled={Boolean(busy)}
                >
                  {label}
                </button>
            ))}
          </div>
          <button type="button" onClick={onRefresh} disabled={Boolean(busy)}>刷新</button>
          <span>{currentUser?.nickname || currentUser?.username}</span>
          <button type="button" onClick={onLogout} disabled={Boolean(busy)}>退出</button>
        </div>
      </header>
  )
}
