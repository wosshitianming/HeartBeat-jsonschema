import {lazy, Suspense} from 'react'

const DashboardPage = lazy(() => import('../../pages/DashboardPage'))
const PayWorkspacePage = lazy(() => import('../../pages/pay/PayWorkspacePage'))
const PayCashierPage = lazy(() => import('../../pages/pay/PayCashierPage'))
const WorkflowWorkspacePage = lazy(() => import('../../pages/workflow/WorkflowWorkspacePage'))
const ReportWorkspacePage = lazy(() => import('../../pages/report/ReportWorkspacePage'))
const MpWorkspacePage = lazy(() => import('../../pages/mp/MpWorkspacePage'))
const MobileWorkspacePage = lazy(() => import('../../pages/mobile/MobileWorkspacePage'))
const FlowStudioPage = lazy(() => import('../../pages/flow/FlowStudioPage'))
const FlowOperationsPage = lazy(() => import('../../pages/flow/FlowOperationsPage'))
const CodeGenPage = lazy(() => import('../../pages/tool/CodeGenPage'))
const SchedulerControlPage = lazy(() => import('../../pages/tool/SchedulerControlPage'))
const SystemMonitorPage = lazy(() => import('../../pages/monitor/SystemMonitorPage'))
const MenuManagementPage = lazy(() => import('../../pages/system/MenuManagementPage'))

const MODULE_REGISTRY = {
    'home-dashboard': {component: DashboardPage},
    'system-menu': {component: MenuManagementPage},

    flow: {component: FlowStudioPage},
    'flow-definition': {component: FlowOperationsPage, initialView: 'definitions'},
    'flow-component': {component: FlowOperationsPage, initialView: 'components'},
    'flow-credential': {component: FlowOperationsPage, initialView: 'connections'},
    'flow-run': {component: FlowOperationsPage, initialView: 'runs'},

    'biz-workflow': {component: WorkflowWorkspacePage, initialView: 'definitions'},
    'biz-workflow-definition': {component: WorkflowWorkspacePage, initialView: 'definitions'},
    'biz-workflow-instance': {component: WorkflowWorkspacePage, initialView: 'instances'},
    'biz-workflow-todo': {component: WorkflowWorkspacePage, initialView: 'tasks'},

    'biz-pay': {component: PayWorkspacePage, initialView: 'orders'},
    'biz-pay-cashier': {component: PayCashierPage},
    'biz-pay-channel': {component: PayWorkspacePage, initialView: 'channels'},
    'biz-pay-order': {component: PayWorkspacePage, initialView: 'orders'},
    'biz-pay-notify': {component: PayWorkspacePage, initialView: 'notifications'},

    'biz-mp': {component: MpWorkspacePage, initialView: 'accounts'},
    'biz-mp-account': {component: MpWorkspacePage, initialView: 'accounts'},
    'biz-mp-menu': {component: MpWorkspacePage, initialView: 'menus'},
    'biz-mp-material': {component: MpWorkspacePage, initialView: 'materials'},
    'biz-mp-reply': {component: MpWorkspacePage, initialView: 'replies'},

    'biz-report': {component: ReportWorkspacePage, initialView: 'datasets'},
    'biz-report-dataset': {component: ReportWorkspacePage, initialView: 'datasets'},
    'biz-report-template': {component: ReportWorkspacePage, initialView: 'templates'},

    'biz-mobile': {component: MobileWorkspacePage, initialView: 'apps'},
    'biz-mobile-app': {component: MobileWorkspacePage, initialView: 'apps'},
    'biz-mobile-page': {component: MobileWorkspacePage, initialView: 'pages'},
    'biz-mobile-route': {component: MobileWorkspacePage, initialView: 'routes'},

    'monitor-server': {component: SystemMonitorPage, initialTab: 'server'},
    'monitor-cache': {component: SystemMonitorPage, initialTab: 'cache'},
    'monitor-druid': {component: SystemMonitorPage, initialTab: 'druid'},
    'tool-job': {component: SchedulerControlPage},
    'tool-gen': {component: CodeGenPage}
}

export function isDedicatedModule(moduleKey) {
    return Boolean(MODULE_REGISTRY[moduleKey])
}

export default function DedicatedModuleHost({
                                                moduleKey,
                                                refreshKey = 0,
                                                currentUser,
                                                busy,
                                                onBusy,
                                                onError
                                            }) {
    const registration = MODULE_REGISTRY[moduleKey]
    if (!registration) return null

    const Component = registration.component
    return (
        <Suspense fallback={<div className="table-empty lazy-module-fallback">正在加载工作区...</div>}>
            <Component
                key={`${moduleKey}-${refreshKey}`}
                initialView={registration.initialView}
                initialTab={registration.initialTab}
                currentUser={currentUser}
                permissions={currentUser?.permissions || []}
                busy={busy}
                onBusy={onBusy}
                onError={onError}
            />
        </Suspense>
    )
}
