import {useEffect, useMemo, useRef, useState} from 'react'
import './BackendWorkspace.css'

function formatCellValue(value) {
    if (value === undefined || value === null || value === '') return '—'
    if (typeof value === 'boolean') return value ? '是' : '否'
    if (typeof value === 'object') {
        try {
            return JSON.stringify(value)
        } catch {
            return String(value)
        }
    }
    return String(value)
}

function normalizeInitialValues(fields, values = {}) {
    return fields.reduce((result, field) => {
        const value = values[field.name] ?? field.defaultValue ?? ''
        result[field.name] = field.type === 'json' && typeof value !== 'string'
            ? JSON.stringify(value ?? {}, null, 2)
            : value
        return result
    }, {})
}

export function WorkspaceHeader({breadcrumb, title, description, status, onRefresh, loading, actions}) {
    return (
        <header className="backend-workspace-header">
            <div>
                <p className="page-breadcrumb">{breadcrumb}</p>
                <h1>{title}</h1>
                <p>{description}</p>
            </div>
            <div className="backend-workspace-header-actions">
                {status && <StatusBadge value={status}/>}
                {actions}
                {onRefresh && (
                    <button className="button ghost" type="button" disabled={loading} onClick={onRefresh}>
                        {loading ? '刷新中...' : '刷新'}
                    </button>
                )}
            </div>
        </header>
    )
}

export function WorkspaceTabs({items, activeKey, onChange, label = '工作区视图'}) {
    return (
        <div className="backend-segmented-tabs" role="tablist" aria-label={label}>
            {items.map((item) => (
                <button
                    key={item.key}
                    type="button"
                    role="tab"
                    aria-selected={item.key === activeKey}
                    className={item.key === activeKey ? 'active' : ''}
                    onClick={() => onChange(item.key)}
                >
                    <span>{item.label}</span>
                    {item.count !== undefined && <small>{item.count}</small>}
                </button>
            ))}
        </div>
    )
}

export function MetricStrip({items}) {
    return (
        <div className="backend-metric-strip">
            {items.map((item) => (
                <div key={item.label} className="backend-metric-item">
                    <span>{item.label}</span>
                    <strong title={formatCellValue(item.value)}>{formatCellValue(item.value)}</strong>
                    {item.hint && <small title={item.hint}>{item.hint}</small>}
                </div>
            ))}
        </div>
    )
}

export function StatusBadge({value}) {
    const normalized = String(value || 'UNKNOWN').toUpperCase()
    const tone = ['ENABLED', 'ACTIVE', 'SUCCESS', 'PAID', 'COMPLETED', 'DEPLOYED', 'APPROVED', 'SYNCED', 'PUBLISHED']
        .includes(normalized)
        ? 'success'
        : ['FAILED', 'DISABLED', 'REJECTED', 'CANCELED', 'CANCELLED', 'ERROR', 'SIGN_FAIL'].includes(normalized)
            ? 'danger'
            : ['PENDING', 'RUNNING', 'PROCESSING', 'WAITING', 'DRAFT', 'PAYING', 'CREATED', 'TODO'].includes(normalized)
                ? 'warning'
                : 'neutral'
    return <span className="backend-status-badge" data-tone={tone}>{value || '—'}</span>
}

export function BackendDataTable({
                                     ariaLabel,
                                     columns,
                                     rows = [],
                                     loading = false,
                                     emptyText = '暂无数据',
                                     searchPlaceholder = '搜索当前列表',
                                     rowKey = 'id',
                                     selectedId,
                                     onSelect,
                                     rowActions,
                                     actionColumnWidth = 144
                                 }) {
    const [query, setQuery] = useState('')
    const normalizedQuery = query.trim().toLocaleLowerCase()
    const resolvedActionColumnWidth = typeof actionColumnWidth === 'number'
        ? `${actionColumnWidth}px`
        : actionColumnWidth
    const filteredRows = useMemo(() => {
        if (!normalizedQuery) return rows
        return rows.filter((row) => columns.some((column) => {
            const raw = typeof column.value === 'function' ? column.value(row) : row[column.key]
            return formatCellValue(raw).toLocaleLowerCase().includes(normalizedQuery)
        }))
    }, [columns, normalizedQuery, rows])

    return (
        <section className="backend-table-panel panel" aria-busy={loading}>
            <div className="backend-table-toolbar">
                <label>
                    <span>搜索</span>
                    <input
                        type="search"
                        value={query}
                        placeholder={searchPlaceholder}
                        onChange={(event) => setQuery(event.target.value)}
                    />
                </label>
                <span>{normalizedQuery ? `${filteredRows.length} / ${rows.length} 条` : `共 ${rows.length} 条`}</span>
            </div>
            <div className="backend-table-scroll">
                <table
                    className="resource-table backend-data-table"
                    aria-label={ariaLabel}
                    style={rowActions ? {'--backend-action-column-width': resolvedActionColumnWidth} : undefined}
                >
                    <thead>
                    <tr>
                        {columns.map((column) => <th key={column.key}>{column.label}</th>)}
                        {rowActions && <th className="backend-action-column">操作</th>}
                    </tr>
                    </thead>
                    <tbody>
                    {!loading && filteredRows.map((row, index) => {
                        const key = typeof rowKey === 'function' ? rowKey(row, index) : (row[rowKey] ?? index)
                        const selected = selectedId !== undefined && String(selectedId) === String(key)
                        const selectable = typeof onSelect === 'function'
                        return (
                            <tr
                                key={key}
                                className={[selected ? 'selected-row' : '', selectable ? 'selectable-row' : ''].filter(Boolean).join(' ')}
                                tabIndex={selectable ? 0 : undefined}
                                aria-selected={selectable ? selected : undefined}
                                onClick={() => onSelect?.(row)}
                                onKeyDown={(event) => {
                                    if (event.target !== event.currentTarget) return
                                    if (!selectable || (event.key !== 'Enter' && event.key !== ' ')) return
                                    event.preventDefault()
                                    onSelect(row)
                                }}
                            >
                                {columns.map((column) => {
                                    const raw = typeof column.value === 'function' ? column.value(row) : row[column.key]
                                    return (
                                        <td key={column.key} title={formatCellValue(raw)}>
                                            {column.render ? column.render(raw, row) : formatCellValue(raw)}
                                        </td>
                                    )
                                })}
                                {rowActions && (
                                    <td className="backend-action-cell" onClick={(event) => event.stopPropagation()}>
                                        {rowActions(row)}
                                    </td>
                                )}
                            </tr>
                        )
                    })}
                    </tbody>
                </table>
                {loading && <div className="table-empty">正在加载数据...</div>}
                {!loading && rows.length === 0 && <div className="table-empty">{emptyText}</div>}
                {!loading && rows.length > 0 && filteredRows.length === 0 && (
                    <div className="table-empty">
                        <p>未找到“{query.trim()}”</p>
                        <button className="text-button" type="button" onClick={() => setQuery('')}>清除搜索</button>
                    </div>
                )}
            </div>
        </section>
    )
}

export function RecordDialog({
                                 open,
                                 title,
                                 description,
                                 fields,
                                 initialValues,
                                 submitLabel = '保存',
                                 busy = false,
                                 onClose,
                                 onSubmit
                             }) {
    const [values, setValues] = useState(() => normalizeInitialValues(fields, initialValues))
    const [submitError, setSubmitError] = useState('')
    const dialogRef = useRef(null)
    const wasOpenRef = useRef(false)
    const onCloseRef = useRef(onClose)
    const busyRef = useRef(busy)

    useEffect(() => {
        if (open && !wasOpenRef.current) {
            setValues(normalizeInitialValues(fields, initialValues))
            setSubmitError('')
        }
        wasOpenRef.current = open
    }, [fields, initialValues, open])

    useEffect(() => {
        onCloseRef.current = onClose
    }, [onClose])

    useEffect(() => {
        busyRef.current = busy
    }, [busy])

    useEffect(() => {
        if (!open) return undefined
        const previous = document.activeElement
        const frame = window.requestAnimationFrame(() => {
            dialogRef.current?.querySelector('input, select, textarea, button')?.focus()
        })
        const handleKeyDown = (event) => {
            if (event.key === 'Escape' && !busyRef.current) {
                onCloseRef.current?.()
                return
            }
            if (event.key !== 'Tab') return
            const focusable = Array.from(dialogRef.current?.querySelectorAll(
                'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
            ) || [])
            if (focusable.length === 0) return
            const first = focusable[0]
            const last = focusable[focusable.length - 1]
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault()
                last.focus()
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault()
                first.focus()
            }
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => {
            window.cancelAnimationFrame(frame)
            document.removeEventListener('keydown', handleKeyDown)
            previous?.focus?.()
        }
    }, [open])

    if (!open) return null

    function updateField(field, value) {
        setValues((current) => ({
            ...current,
            [field.name]: field.type === 'number' && value !== '' ? Number(value) : value
        }))
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setSubmitError('')
        try {
            await onSubmit(values)
        } catch (error) {
            setSubmitError(error?.message || '提交失败，请检查表单内容')
        }
    }

    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
            if (!busy && event.target === event.currentTarget) onClose?.()
        }}>
            <form
                ref={dialogRef}
                className="resource-dialog backend-record-dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="backend-record-dialog-title"
                onSubmit={handleSubmit}
            >
                <div className="dialog-heading">
                    <div>
                        <span className="step">FORM</span>
                        <h2 id="backend-record-dialog-title">{title}</h2>
                    </div>
                    <button className="text-button dialog-close-button" type="button" disabled={busy} onClick={onClose}>
                        ×
                    </button>
                </div>
                {description && <p className="backend-dialog-description">{description}</p>}
                {submitError && <div className="error-banner backend-dialog-error" role="alert">{submitError}</div>}
                <div className="resource-form-grid">
                    {fields.map((field) => {
                        const inputId = `backend-field-${field.name}`
                        const wide = field.type === 'textarea' || field.type === 'json' || field.fullWidth
                        return (
                            <label key={field.name} htmlFor={inputId} className={wide ? 'full-field' : ''}>
                    <span className="dialog-field-label">
                      {field.label}{field.required && <span className="required-mark"> *</span>}
                    </span>
                                {field.type === 'select' ? (
                                    <select
                                        id={inputId}
                                        value={values[field.name] ?? ''}
                                        required={field.required}
                                        disabled={busy || field.disabled}
                                        onChange={(event) => updateField(field, event.target.value)}
                                    >
                                        {(field.options || []).map((option) => {
                                            const value = typeof option === 'object' ? option.value : option
                                            const label = typeof option === 'object' ? option.label : option
                                            return <option key={value} value={value}>{label}</option>
                                        })}
                                    </select>
                                ) : field.type === 'textarea' || field.type === 'json' ? (
                                    <textarea
                                        id={inputId}
                                        className={field.type === 'json' ? 'dialog-textarea backend-json-input' : 'dialog-textarea'}
                                        value={values[field.name] ?? ''}
                                        required={field.required}
                                        disabled={busy || field.disabled}
                                        placeholder={field.placeholder}
                                        onChange={(event) => updateField(field, event.target.value)}
                                    />
                                ) : (
                                    <input
                                        id={inputId}
                                        type={field.type || 'text'}
                                        value={values[field.name] ?? ''}
                                        required={field.required}
                                        disabled={busy || field.disabled}
                                        placeholder={field.placeholder}
                                        onChange={(event) => updateField(field, event.target.value)}
                                    />
                                )}
                                {field.hint && <small className="backend-field-hint">{field.hint}</small>}
                            </label>
                        )
                    })}
                </div>
                <div className="dialog-actions">
                    <button className="button ghost" type="button" disabled={busy} onClick={onClose}>取消</button>
                    <button className="button primary" type="submit" disabled={busy}>
                        {busy ? '处理中...' : submitLabel}
                    </button>
                </div>
            </form>
        </div>
    )
}

export function parseJsonField(value, fallback = {}) {
    if (value === undefined || value === null || value === '') return fallback
    if (typeof value !== 'string') return value
    try {
        return JSON.parse(value)
    } catch {
        throw new Error('JSON 配置格式不正确')
    }
}

export {formatCellValue}
