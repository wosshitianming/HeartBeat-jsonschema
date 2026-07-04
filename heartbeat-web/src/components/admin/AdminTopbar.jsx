export default function AdminTopbar({
  module,
  currentUser,
  onRefresh,
  onPrimaryAction,
  primaryLabel,
  onLogout,
  busy
}) {
  return (
      <header className="command-bar">
        <div className="command-context">
          <span className="command-kicker">{module?.category || '管理后台'}</span>
          <strong>{module?.name || '管理控制台'}</strong>
          <small>首页 / {module?.name || '控制台'}</small>
        </div>
        <div className="command-actions">
          <button className="toolbar-button" onClick={onRefresh} disabled={Boolean(busy)}>刷新</button>
          <button className="toolbar-button primary" onClick={onPrimaryAction} disabled={Boolean(busy)}>
            {primaryLabel}
          </button>
          <div className="user-chip" title={currentUser?.username}>
            <span>{(currentUser?.nickname || currentUser?.username || 'U').slice(0, 1)}</span>
            <strong>{currentUser?.nickname || currentUser?.username}</strong>
          </div>
          <button className="toolbar-button subtle" onClick={onLogout} disabled={Boolean(busy)}>退出</button>
        </div>
      </header>
  )
}
