import {useMemo, useState} from 'react'
import AppearanceSettingsPanel from '../AppearanceSettingsPanel/AppearanceSettingsPanel'

const NAV_ITEMS = [
  { key: 'workbench', label: '工作台', icon: '⌂' },
  { key: 'modules', label: '业务模块', icon: '▦' },
  { key: 'quick', label: '快捷操作', icon: '＋' },
  { key: 'profile', label: '我的', icon: '●' }
]

export default function MobileAdminShell({
  modules,
  activeModule,
  activeResource,
  resourceEditable = false,
  records,
  columns,
  selectedRecord,
  currentUser,
  colorMode,
  onColorModeChange,
  accentColor,
  onAccentColorChange,
  visualStyle,
  onVisualStyleChange,
  fluidEnabled,
  onFluidChange,
  syncState,
  onSelectModule,
  onSelectRecord,
  onCreate,
  onEdit,
  onDelete,
  onRefresh,
  onLogout
}) {
  const [section, setSection] = useState('workbench')
  const [moduleOpen, setModuleOpen] = useState(false)
  const [query, setQuery] = useState('')
  const filteredModules = useMemo(
      () => modules.filter((module) => module.name.toLowerCase().includes(query.trim().toLowerCase())),
      [modules, query]
  )

  function openModule(module) {
    onSelectRecord(null)
    onSelectModule(module.key)
    setSection('modules')
    setModuleOpen(true)
  }

  function navigate(key) {
    setSection(key)
    if (key === 'modules') {
      setModuleOpen(false)
      onSelectRecord(null)
    }
  }

  function moduleTitle() {
    return activeModule?.name?.replace(/管理$/, '') || '配置'
  }

  return (
      <section className="mobile-admin-shell" aria-label="移动管理台">
        <header className="mobile-app-bar">
          <div>
            <small>HeartBeat</small>
            <strong>{moduleOpen ? activeModule?.name : NAV_ITEMS.find((item) => item.key === section)?.label}</strong>
          </div>
          <button type="button" onClick={onRefresh} aria-label="刷新当前页面">↻</button>
        </header>

        <main className="mobile-screen">
          {section === 'workbench' && (
              <div className="mobile-workbench">
                <section className="mobile-welcome">
                  <span>{(currentUser?.nickname || currentUser?.username || 'U').slice(0, 1)}</span>
                  <div>
                    <small>欢迎回来</small>
                    <h1>{currentUser?.nickname || currentUser?.username}</h1>
                  </div>
                </section>
                <div className="mobile-stat-grid">
                  <article><strong>{modules.length}</strong><span>可用模块</span></article>
                  <article><strong>{records.length}</strong><span>当前数据</span></article>
                </div>
                <section className="mobile-section-card">
                  <h2>常用模块</h2>
                  {modules.slice(0, 4).map((module) => (
                      <button type="button" key={module.key} onClick={() => openModule(module)}>
                        <span>{module.name.slice(0, 1)}</span>
                        <strong>{module.name}</strong>
                        <small>{module.category}</small>
                      </button>
                  ))}
                </section>
              </div>
          )}

          {section === 'modules' && !moduleOpen && (
              <div className="mobile-module-browser">
                <label className="mobile-search">
                  <span>搜索</span>
                  <input
                      aria-label="搜索业务模块"
                      value={query}
                      onChange={(event) => setQuery(event.target.value)}
                      placeholder="搜索菜单、配置或工具"
                  />
                </label>
                <div className="mobile-module-grid">
                  {filteredModules.map((module) => (
                      <button type="button" key={module.key} onClick={() => openModule(module)}>
                        <span>{module.name.slice(0, 1)}</span>
                        <strong>{module.name}</strong>
                        <small>{module.category}</small>
                      </button>
                  ))}
                </div>
              </div>
          )}

          {section === 'modules' && moduleOpen && activeModule?.key === 'structure' && (
              <section className="mobile-special-workspace">
                <h1>结构配置工作台</h1>
                <p>复杂 Schema 配置已切换为下方全屏工作台，可继续推断、编辑、校验和发布版本。</p>
                <a href="#structure-workbench">进入配置工作台</a>
              </section>
          )}

          {section === 'modules' && moduleOpen && activeModule?.key !== 'structure' && !selectedRecord && (
              <div className="mobile-resource-list">
                <div className="mobile-page-heading">
                  <button type="button" onClick={() => setModuleOpen(false)}>‹ 模块</button>
                  <div>
                    <h1>{activeModule?.name}</h1>
                    <p>{activeModule?.description}</p>
                  </div>
                </div>
                {records.map((record, index) => (
                    <button
                        type="button"
                        className="mobile-record"
                        key={record.id || index}
                        onClick={() => onSelectRecord(record)}
                    >
                      <span className="mobile-record-icon">{String(index + 1).padStart(2, '0')}</span>
                      <span className="mobile-record-copy">
                        <strong>{record[columns[0]] || record.id || '未命名记录'}</strong>
                        <small>{columns.slice(1, 3).map((column) => record[column]).filter(Boolean).join(' · ') || '查看详细信息'}</small>
                      </span>
                      <span aria-hidden="true">›</span>
                    </button>
                ))}
                {records.length === 0 && <div className="mobile-empty">暂无数据{resourceEditable ? '，点击右下角新增。' : '。'}</div>}
                {resourceEditable && (
                    <button type="button" className="mobile-fab" aria-label={`新增${activeModule?.name}`} onClick={onCreate}>＋</button>
                )}
              </div>
          )}

          {section === 'modules' && moduleOpen && selectedRecord && (
              <div className="mobile-resource-detail">
                <div className="mobile-page-heading">
                  <button type="button" onClick={() => onSelectRecord(null)}>‹ 返回</button>
                  <div>
                    <h1>{moduleTitle()}详情</h1>
                    <p>查看记录信息与可用操作</p>
                  </div>
                </div>
                <dl>
                  {columns.map((column) => (
                      <div key={column}>
                        <dt>{column}</dt>
                        <dd>{selectedRecord[column] || '—'}</dd>
                      </div>
                  ))}
                </dl>
                {resourceEditable && (
                    <div className="mobile-detail-actions">
                      <button type="button" onClick={() => onEdit(selectedRecord)}>编辑</button>
                      <button type="button" className="danger" onClick={() => onDelete(selectedRecord)}>删除</button>
                    </div>
                )}
              </div>
          )}

          {section === 'quick' && (
              <div className="mobile-quick-actions">
                <h1>快捷操作</h1>
                <button type="button" onClick={onCreate} disabled={!resourceEditable}>新增当前配置</button>
                <button type="button" onClick={onRefresh}>刷新数据</button>
                <button type="button" onClick={() => openModule(modules[0])}>打开结构工作台</button>
              </div>
          )}

          {section === 'profile' && (
              <div className="mobile-profile">
                <div className="profile-avatar">{(currentUser?.nickname || currentUser?.username || 'U').slice(0, 1)}</div>
                <h1>{currentUser?.nickname || currentUser?.username}</h1>
                <p>{currentUser?.username}</p>
                <section>
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
                </section>
                <button type="button" className="mobile-logout" onClick={onLogout}>退出登录</button>
              </div>
          )}
        </main>

        <nav className="mobile-bottom-nav" aria-label="移动端主导航">
          {NAV_ITEMS.map((item) => (
              <button
                  type="button"
                  key={item.key}
                  className={section === item.key ? 'active' : ''}
                  onClick={() => navigate(item.key)}
              >
                <span aria-hidden="true">{item.icon}</span>
                {item.label}
              </button>
          ))}
        </nav>
      </section>
  )
}
