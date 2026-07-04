export default function DashboardPage({ currentUser }) {
  return (
      <div className="hb-page-card">
        <h1>工作台概览</h1>
        <p>欢迎回来，{currentUser?.nickname || currentUser?.username || '管理员'}。</p>
        <div className="hb-dashboard-grid">
          <div className="hb-stat-card">
            <span>系统模块</span>
            <strong>6</strong>
          </div>
          <div className="hb-stat-card">
            <span>结构定义</span>
            <strong>—</strong>
          </div>
          <div className="hb-stat-card">
            <span>在线用户</span>
            <strong>1</strong>
          </div>
          <div className="hb-stat-card">
            <span>待办任务</span>
            <strong>0</strong>
          </div>
        </div>
      </div>
  )
}
