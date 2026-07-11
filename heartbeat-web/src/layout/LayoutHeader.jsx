import {useEffect, useMemo, useRef, useState} from 'react'
import AppearanceSettingsPanel from '../components/AppearanceSettingsPanel/AppearanceSettingsPanel'

export default function LayoutHeader({
  brand = 'HeartBeat',
                                         topModules = [],
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
  onRefresh,
  onLogout
}) {
    const [openPanel, setOpenPanel] = useState(null)
    const toolsRef = useRef(null)
    const displayName = String(currentUser?.nickname || currentUser?.username || '管理员')
    const accountName = currentUser?.username && String(currentUser.username) !== displayName
        ? String(currentUser.username)
        : ''
    const avatarText = useMemo(
        () => displayName.trim().slice(0, 1).toUpperCase() || 'H',
        [displayName]
    )

    useEffect(() => {
        if (!openPanel) return undefined

        function handlePointerDown(event) {
            if (!toolsRef.current?.contains(event.target)) {
                setOpenPanel(null)
            }
        }

        function handleKeyDown(event) {
            if (event.key === 'Escape') {
                setOpenPanel(null)
            }
        }

        document.addEventListener('pointerdown', handlePointerDown)
        document.addEventListener('keydown', handleKeyDown)
        return () => {
            document.removeEventListener('pointerdown', handlePointerDown)
            document.removeEventListener('keydown', handleKeyDown)
        }
    }, [openPanel])

  return (
      <header className="hb-layout-header">
        <div className="hb-layout-brand">
            <span aria-hidden="true"/>
            <strong>{brand}</strong>
        </div>
          <nav className="hb-top-nav" aria-label="一级模块导航">
              {topModules.map((module) => {
                  const active = module.id === activeTopModuleId
                  return (
                      <button
                          key={module.id}
                          type="button"
                          className={active ? 'active' : ''}
                          aria-current={active ? 'page' : undefined}
                          onClick={() => {
                              setOpenPanel(null)
                              onSelectTopModule?.(module.id)
                          }}
                      >
                          {module.name}
                      </button>
                  )
              })}
        </nav>
          <div className="hb-header-tools" ref={toolsRef}>
              <button
                  type="button"
                  className="hb-header-icon-button"
                  disabled={Boolean(busy)}
                  aria-label={busy ? '正在处理' : '刷新当前页面'}
                  title="刷新当前页面"
                  onClick={onRefresh}
              >
                  <span aria-hidden="true">↻</span>
              </button>

              <div className="hb-header-settings">
                  <button
                      type="button"
                      className="hb-header-icon-button"
                      disabled={Boolean(busy)}
                      aria-label="外观设置"
                      title="外观设置"
                      aria-expanded={openPanel === 'settings'}
                      onClick={() => setOpenPanel((value) => value === 'settings' ? null : 'settings')}
                  >
                      <span aria-hidden="true">⚙</span>
                  </button>
                  {openPanel === 'settings' && (
                      <div className="hb-header-settings-popover" role="dialog" aria-label="外观设置">
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

              <div className="hb-header-settings hb-user-menu">
                  <button
                      type="button"
                      className="hb-user-menu-trigger"
                disabled={Boolean(busy)}
                      aria-expanded={openPanel === 'user'}
                      aria-label={`${displayName}，打开账号菜单`}
                      onClick={() => setOpenPanel((value) => value === 'user' ? null : 'user')}
                  >
                      <span className="hb-user-avatar" aria-hidden="true">{avatarText}</span>
                      <span className="hb-user-name">{displayName}</span>
                  </button>
                  {openPanel === 'user' && (
                      <div className="hb-header-settings-popover hb-user-menu-popover" role="menu">
                          <div className="hb-user-menu-profile">
                              <strong>{displayName}</strong>
                              {accountName && <small>{accountName}</small>}
                          </div>
                          <button
                              type="button"
                              role="menuitem"
                              onClick={() => {
                                  setOpenPanel(null)
                                  onLogout?.()
                              }}
                          >
                              退出登录
                          </button>
                      </div>
                  )}
              </div>
        </div>
      </header>
  )
}
