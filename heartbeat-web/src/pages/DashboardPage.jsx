export default function DashboardPage({currentUser}) {
    const displayName = currentUser?.nickname || currentUser?.username || '管理员'
    const today = new Intl.DateTimeFormat('zh-CN', {
        month: 'long',
        day: 'numeric',
        weekday: 'long'
    }).format(new Date())

    const accountDetails = [
        ['登录账号', currentUser?.username],
        ['用户 ID', currentUser?.id],
        ['租户 ID', currentUser?.tenantId],
        ['邮箱', currentUser?.email],
        ['手机号', currentUser?.phone]
    ].filter(([, value]) => value !== undefined && value !== null && value !== '')

  return (
      <div className="hb-page-card">
          <header className="module-page-header">
              <div>
                  <p className="page-breadcrumb">首页 / 工作台</p>
                  <h1>工作台概览</h1>
                  <p>欢迎回来，{displayName}。今天是{today}。</p>
              </div>
              <div className="module-page-meta">
                  <span className="status-pill">会话正常</span>
                  <code>{currentUser?.username || '已认证'}</code>
              </div>
          </header>

        <div className="hb-dashboard-grid">
          <div className="hb-stat-card">
              <span>当前用户</span>
              <strong>{displayName}</strong>
              <small>已登录 HeartBeat 管理台</small>
          </div>
          <div className="hb-stat-card">
              <span>登录账号</span>
              <strong>{currentUser?.username || '—'}</strong>
              <small>当前会话身份</small>
          </div>
          <div className="hb-stat-card">
              <span>租户 ID</span>
              <strong>{currentUser?.tenantId ?? '—'}</strong>
              <small>数据访问范围</small>
          </div>
          <div className="hb-stat-card">
              <span>会话状态</span>
              <strong>在线</strong>
              <small>身份校验已通过</small>
          </div>
        </div>

          {accountDetails.length > 0 && (
              <section className="panel monitor-section" aria-labelledby="dashboard-account-heading">
                  <div className="panel-heading">
                      <div>
                          <span className="step">ACCOUNT</span>
                          <h2 id="dashboard-account-heading">账户信息</h2>
                      </div>
                      <span className="status-pill">当前会话</span>
                  </div>
                  <dl className="monitor-kv-grid">
                      {accountDetails.map(([label, value]) => (
                          <div key={label}>
                              <dt>{label}</dt>
                              <dd>{String(value)}</dd>
                          </div>
                      ))}
                  </dl>
              </section>
          )}
      </div>
  )
}
