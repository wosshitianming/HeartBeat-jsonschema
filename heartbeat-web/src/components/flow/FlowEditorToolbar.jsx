import {memo} from 'react'
import {FileCheck2, LoaderCircle, Maximize2, Play, Redo2, Rocket, Save, Undo2} from 'lucide-react'

const statusLabels = {
    DRAFT: '草稿',
    SAVED: '已保存',
    PUBLISHED: '已发布',
    ONLINE: '运行中',
    OFFLINE: '已停用',
    ARCHIVED: '已归档'
}

function IconButton({label, disabled, onClick, children}) {
    return (
        <button
            type="button"
            className="flow-editor-toolbar-icon-button"
            disabled={disabled}
            onClick={onClick}
            title={label}
            aria-label={label}
        >
            {children}
        </button>
    )
}

function FlowEditorToolbar({
                               flowName,
                               status,
                               dirty,
                               busy,
                               canEdit,
                               canRun,
                               canPublish,
                               canUndo,
                               canRedo,
                               onNameChange,
                               onUndo,
                               onRedo,
                               onSave,
                               onCompile,
                               onRun,
                               onPublish,
                               onFitView
                           }) {
    const isBusy = Boolean(busy)
    const statusKey = String(status || 'DRAFT').toUpperCase()

    return (
        <div className="flow-editor-toolbar">
            <div className="flow-editor-toolbar-history" role="group" aria-label="画布历史操作">
                <IconButton label="撤销" disabled={!canUndo || isBusy} onClick={onUndo}>
                    <Undo2 size={16} aria-hidden="true"/>
                </IconButton>
                <IconButton label="重做" disabled={!canRedo || isBusy} onClick={onRedo}>
                    <Redo2 size={16} aria-hidden="true"/>
                </IconButton>
                <IconButton label="适应画布" onClick={onFitView}>
                    <Maximize2 size={16} aria-hidden="true"/>
                </IconButton>
            </div>

            <div className="flow-editor-toolbar-document">
                <input
                    value={flowName}
                    onChange={(event) => onNameChange?.(event.target.value)}
                    disabled={!canEdit || isBusy}
                    aria-label="流程名称"
                    title={flowName || '流程名称'}
                />
                <span className="flow-editor-toolbar-status" data-status={statusKey}>
            <i aria-hidden="true"/>
                    {statusLabels[statusKey] || statusKey}
          </span>
                {dirty && <span className="flow-editor-toolbar-dirty">未保存</span>}
            </div>

            <div className="flow-editor-toolbar-actions">
                <button type="button" onClick={onCompile} disabled={isBusy} title="编译检查当前流程">
                    {busy === 'flow-compile'
                        ? <LoaderCircle className="flow-editor-spin" size={15} aria-hidden="true"/>
                        : <FileCheck2 size={15} aria-hidden="true"/>}
                    <span>检查</span>
                </button>
                <button type="button" onClick={onSave} disabled={!canEdit || isBusy} title="保存草稿 (Ctrl+S)">
                    {busy === 'flow-save'
                        ? <LoaderCircle className="flow-editor-spin" size={15} aria-hidden="true"/>
                        : <Save size={15} aria-hidden="true"/>}
                    <span>保存</span>
                </button>
                <button type="button" className="run" onClick={onRun} disabled={!canRun || isBusy}
                        title="使用测试数据运行">
                    {busy === 'flow-debug'
                        ? <LoaderCircle className="flow-editor-spin" size={15} aria-hidden="true"/>
                        : <Play size={15} fill="currentColor" aria-hidden="true"/>}
                    <span>测试</span>
                </button>
                <button type="button" className="publish" onClick={onPublish} disabled={!canPublish || isBusy}
                        title="发布新版本">
                    {busy === 'flow-publish'
                        ? <LoaderCircle className="flow-editor-spin" size={15} aria-hidden="true"/>
                        : <Rocket size={15} aria-hidden="true"/>}
                    <span>发布</span>
                </button>
            </div>
        </div>
    )
}

export default memo(FlowEditorToolbar)
