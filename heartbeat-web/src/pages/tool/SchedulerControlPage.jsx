import {useState} from 'react'
import {toolApi} from '../../api'
import {MetricStrip, StatusBadge, WorkspaceHeader} from '../../components/admin/BackendWorkspace'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import './SchedulerControlPage.css'

export default function SchedulerControlPage({permissions = [], onError}) {
    const [jobId, setJobId] = useState('')
    const [busy, setBusy] = useState('')
    const [feedback, setFeedback] = useState(null)
    const canRun = hasPermission(permissions, 'tool:job:run')
    const canEdit = hasPermission(permissions, 'tool:job:edit')

    async function execute(action, work) {
        if (action !== 'refresh' && !jobId.trim()) {
            const message = '请输入任务编码'
            setFeedback({status: 'FAILED', message})
            onError?.(message)
            return
        }
        setBusy(action)
        setFeedback(null)
        try {
            await work()
            const labels = {run: '任务已触发', pause: '任务已暂停', resume: '任务已恢复', refresh: '调度器已刷新'}
            setFeedback({status: 'SUCCESS', message: labels[action]})
        } catch (error) {
            const message = error?.message || '调度操作失败'
            setFeedback({status: 'FAILED', message})
            onError?.(message)
        } finally {
            setBusy('')
        }
    }

    return (
        <section className="scheduler-control-page">
            <WorkspaceHeader
                breadcrumb="开发工具 / 调度任务"
                title="调度控制台"
                description="按任务编码执行 Quartz 任务控制，并可刷新服务端调度器。"
                status={feedback?.status || 'ACTIVE'}
            />

            <MetricStrip items={[
                {label: '任务编码', value: jobId || '未输入', hint: 'Quartz Job Code'},
                {label: '执行权限', value: canRun ? '可执行' : '只读', hint: 'tool:job:run'},
                {label: '维护权限', value: canEdit ? '可维护' : '只读', hint: 'tool:job:edit'},
                {label: '最近结果', value: feedback?.status || '等待操作', hint: feedback?.message || '暂无操作记录'}
            ]}/>

            <section className="panel scheduler-command-panel" aria-labelledby="scheduler-command-title">
                <div className="panel-heading">
                    <div>
                        <span className="step">QUARTZ</span>
                        <h2 id="scheduler-command-title">任务控制</h2>
                    </div>
                    {feedback && <StatusBadge value={feedback.status}/>}
                </div>

                <label className="scheduler-job-input">
                    <span>任务编码</span>
                    <input
                        value={jobId}
                        placeholder="输入 sys_job.job_code"
                        disabled={Boolean(busy)}
                        onChange={(event) => setJobId(event.target.value)}
                    />
                </label>

                <div className="scheduler-command-actions">
                    {canRun && (
                        <button
                            className="button primary"
                            type="button"
                            disabled={Boolean(busy)}
                            onClick={() => execute('run', () => toolApi.runJob(jobId.trim()))}
                        >
                            {busy === 'run' ? '执行中...' : '立即执行'}
                        </button>
                    )}
                    {canEdit && (
                        <>
                            <button
                                className="button ghost"
                                type="button"
                                disabled={Boolean(busy)}
                                onClick={() => execute('pause', () => toolApi.pauseJob(jobId.trim()))}
                            >
                                {busy === 'pause' ? '暂停中...' : '暂停'}
                            </button>
                            <button
                                className="button ghost"
                                type="button"
                                disabled={Boolean(busy)}
                                onClick={() => execute('resume', () => toolApi.resumeJob(jobId.trim()))}
                            >
                                {busy === 'resume' ? '恢复中...' : '恢复'}
                            </button>
                            <button
                                className="button ghost"
                                type="button"
                                disabled={Boolean(busy)}
                                onClick={() => execute('refresh', () => toolApi.refreshJobs())}
                            >
                                {busy === 'refresh' ? '刷新中...' : '刷新调度器'}
                            </button>
                        </>
                    )}
                </div>

                {feedback && (
                    <div className="scheduler-feedback" data-status={feedback.status} role="status">
                        {feedback.message}
                    </div>
                )}
            </section>
        </section>
    )
}
