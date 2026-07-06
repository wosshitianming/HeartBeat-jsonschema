import {useState} from 'react'
import AppearanceSettingsPanel from '../components/AppearanceSettingsPanel/AppearanceSettingsPanel'

export default function LayoutHeader({
  brand = 'HeartBeat',
  topModules,
  activeTopModuleId,
  currentUser,
  busy,
  fluidEnabled,
  onFluidChange,
                                         colorMode,
                                         onColorModeChange,
                                         accentColor,
                                         onAccentColorChange,
                                         visualStyle,
                                         onVisualStyleChange,
                                         syncState,
  onSelectTopModule,
  glassMode,
  onGlassModeChange,
  onRefresh,
  onLogout
}) {
    const [settingsOpen, setSettingsOpen] = useState(false)

  return (
      <header className="hb-layout-header">
        <div className="hb-layout-brand">
          <span />
          <div>{brand}</div>
        </div>
          <nav className="hb-top-nav" aria-label="top navigation">
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
            <div className="hb-header-settings">
                <button
                    type="button"
                    disabled={Boolean(busy)}
                    aria-expanded={settingsOpen}
                    onClick={() => setSettingsOpen((value) => !value)}
                >
                    Theme
                </button>
                {settingsOpen && (
                    <div className="hb-header-settings-popover">
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
                            defaultOpen
                        />
                    </div>
                )}
            </div>
          <label className="hb-header-switch">
            <input
                type="checkbox"
                role="switch"
                aria-label="background animation"
                checked={Boolean(fluidEnabled)}
                onChange={(event) => onFluidChange?.(event.target.checked)}
                disabled={Boolean(busy)}
            />
              Motion
          </label>
            <div className="hb-surface-switcher" aria-label="surface mode">
            {[
                ['immersive', 'Deep'],
                ['balanced', 'Glass'],
                ['restrained', 'Soft'],
                ['flat', 'Flat']
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
            <button type="button" onClick={onRefresh} disabled={Boolean(busy)}>Refresh</button>
          <span>{currentUser?.nickname || currentUser?.username}</span>
            <button type="button" onClick={onLogout} disabled={Boolean(busy)}>Logout</button>
        </div>
      </header>
  )
}
