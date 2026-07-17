import {memo, useEffect, useMemo, useRef, useState} from 'react'
import Form from '@rjsf/core'
import validator from '@rjsf/validator-ajv8'
import {AlertCircle, Braces, Check, FileJson2, ListTree, PanelRightClose, SlidersHorizontal} from 'lucide-react'

const fieldTitles = {
    connectionId: '连接凭证',
    sql: 'SQL 语句',
    key: '键',
    value: '值',
    topic: '主题',
    url: '请求地址',
    method: '请求方法',
    expression: '条件表达式',
    mapping: '字段映射',
    message: '日志内容',
    body: '请求体',
    headers: '请求头',
    timeoutMs: '超时时间（毫秒）'
}

const tabOrder = ['fields', 'json', 'manifest']

function cloneValue(value) {
    return JSON.parse(JSON.stringify(value ?? {}))
}

function pretty(value) {
    try {
        return JSON.stringify(value ?? {}, null, 2)
    } catch {
        return '{}'
    }
}

function localizeSchema(value, fieldName = '') {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return value
    const schema = {...value}
    if (!schema.title && fieldTitles[fieldName]) schema.title = fieldTitles[fieldName]
    if (schema.properties) {
        schema.properties = Object.fromEntries(
            Object.entries(schema.properties).map(([key, child]) => [key, localizeSchema(child, key)])
        )
    }
    if (schema.items) schema.items = localizeSchema(schema.items, fieldName)
    for (const key of ['oneOf', 'anyOf', 'allOf']) {
        if (Array.isArray(schema[key])) schema[key] = schema[key].map((child) => localizeSchema(child, fieldName))
    }
    if (schema.$defs) {
        schema.$defs = Object.fromEntries(Object.entries(schema.$defs).map(([key, child]) => [key, localizeSchema(child, key)]))
    }
    if (schema.definitions) {
        schema.definitions = Object.fromEntries(
            Object.entries(schema.definitions).map(([key, child]) => [key, localizeSchema(child, key)])
        )
    }
    return schema
}

function buildUiSchema(schema) {
    if (!schema || typeof schema !== 'object') return {}
    const result = {'ui:submitButtonOptions': {norender: true}}
    for (const [key, child] of Object.entries(schema.properties || {})) {
        const nested = buildUiSchema(child)
        const multiline = child?.format === 'textarea'
            || ['sql', 'expression', 'mapping', 'message', 'body', 'template'].some((token) => key.toLowerCase().includes(token))
        result[key] = multiline
            ? {...nested, 'ui:widget': 'textarea', 'ui:options': {...(nested['ui:options'] || {}), rows: 5}}
            : nested
    }
    if (schema.items) result.items = buildUiSchema(schema.items)
    return result
}

function transformErrors(errors) {
    return errors.map((error) => {
        const messages = {
            required: '该字段为必填项',
            minLength: '内容长度不足',
            maxLength: '内容长度超出限制',
            minimum: '数值低于允许范围',
            maximum: '数值超出允许范围',
            pattern: '内容格式不正确',
            type: '数据类型不正确',
            enum: '请选择有效选项'
        }
        return {...error, message: messages[error.name] || error.message}
    })
}

function hasSchemaDrivenFields(schema) {
    if (!schema || typeof schema !== 'object') return false
    if (Object.keys(schema.properties || {}).length > 0
        || Object.prototype.hasOwnProperty.call(schema, '$ref')) return true
    if (['oneOf', 'anyOf', 'allOf'].some((key) => Array.isArray(schema[key]) && schema[key].length > 0)) return true
    return Object.prototype.hasOwnProperty.call(schema, 'additionalProperties')
}

function NodeInspector({
                           node,
                           manifest,
                           value = {},
                           onChange,
                           onApply,
                           onClose,
                           readOnly = false
                       }) {
    const [mode, setMode] = useState('fields')
    const [jsonText, setJsonText] = useState(() => pretty(value))
    const [jsonError, setJsonError] = useState('')
    const [validationErrors, setValidationErrors] = useState([])
    const jsonFocusedRef = useRef(false)
    const schema = useMemo(() => localizeSchema(manifest?.configSchema || {type: 'object'}), [manifest?.configSchema])
    const uiSchema = useMemo(() => buildUiSchema(schema), [schema])
    const hasFields = hasSchemaDrivenFields(schema)
    const baseId = `flow-node-inspector-${String(node?.id || 'node').replace(/[^a-zA-Z0-9_-]/g, '-')}`
    const valueSignature = pretty(value)

    useEffect(() => {
        if (jsonFocusedRef.current && mode === 'json') return
        setJsonText(valueSignature)
        setJsonError('')
    }, [mode, node?.id, valueSignature])

    useEffect(() => {
        setValidationErrors([])
    }, [node?.id])

    const selectMode = (nextMode) => {
        if (nextMode === 'json') {
            setJsonText(pretty(value))
            setJsonError('')
        }
        setMode(nextMode)
    }

    const handleTabKeyDown = (event) => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
        event.preventDefault()
        let index = tabOrder.indexOf(mode)
        if (event.key === 'Home') index = 0
        else if (event.key === 'End') index = tabOrder.length - 1
        else index = (index + (event.key === 'ArrowRight' ? 1 : -1) + tabOrder.length) % tabOrder.length
        selectMode(tabOrder[index])
        document.getElementById(`${baseId}-tab-${tabOrder[index]}`)?.focus()
    }

    const handleFormChange = ({formData, errors = []}) => {
        setValidationErrors(errors)
        onChange?.(formData || {})
    }

    const handleJsonChange = (event) => {
        const text = event.target.value
        setJsonText(text)
        try {
            const parsed = JSON.parse(text || '{}')
            if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
                throw new Error('节点配置必须是 JSON 对象')
            }
            setJsonError('')
            const result = validator.validateFormData(parsed, schema, undefined, transformErrors)
            setValidationErrors(result.errors || [])
            onChange?.(parsed)
        } catch (error) {
            setJsonError(error.message || 'JSON 格式不正确')
        }
    }

    const apply = () => {
        if (jsonError || readOnly) return
        const result = validator.validateFormData(value, schema, undefined, transformErrors, uiSchema)
        setValidationErrors(result.errors || [])
        if (result.errors?.length) return
        onApply?.(cloneValue(value))
    }

    const tabs = [
        {id: 'fields', label: '参数', icon: ListTree},
        {id: 'json', label: 'JSON', icon: Braces},
        {id: 'manifest', label: 'Manifest', icon: FileJson2}
    ]

    return (
        <div className="flow-editor-inspector">
            <header className="flow-editor-inspector-header">
                <div className="flow-editor-inspector-icon" aria-hidden="true">
                    <SlidersHorizontal size={17}/>
                </div>
                <div>
                    <strong>{manifest?.name || node?.type || '节点属性'}</strong>
                    <span>{node?.type}@{node?.version}</span>
                </div>
                <button type="button" onClick={onClose} title="关闭属性面板" aria-label="关闭属性面板">
                    <PanelRightClose size={17} aria-hidden="true"/>
                </button>
            </header>

            {manifest?.description && <p className="flow-editor-inspector-description">{manifest.description}</p>}

            <div
                className="flow-editor-inspector-tabs"
                role="tablist"
                aria-label="节点配置视图"
                onKeyDown={handleTabKeyDown}
            >
                {tabs.map(({id, label, icon: Icon}) => (
                    <button
                        id={`${baseId}-tab-${id}`}
                        key={id}
                        type="button"
                        role="tab"
                        tabIndex={mode === id ? 0 : -1}
                        aria-selected={mode === id}
                        aria-controls={`${baseId}-panel-${id}`}
                        className={mode === id ? 'active' : ''}
                        onClick={() => selectMode(id)}
                    >
                        <Icon size={14} aria-hidden="true"/>
                        {label}
                    </button>
                ))}
            </div>

            <div
                id={`${baseId}-panel-${mode}`}
                className="flow-editor-inspector-body"
                role="tabpanel"
                aria-labelledby={`${baseId}-tab-${mode}`}
            >
                {mode === 'fields' && (hasFields ? (
                    <Form
                        className="flow-editor-inspector-form"
                        schema={schema}
                        uiSchema={uiSchema}
                        formData={value}
                        validator={validator}
                        disabled={readOnly}
                        readonly={readOnly}
                        liveValidate
                        showErrorList={false}
                        noHtml5Validate
                        focusOnFirstError
                        transformErrors={transformErrors}
                        onChange={handleFormChange}
                    >
                        <></>
                    </Form>
                ) : (
                    <div className="flow-editor-inspector-empty">
                        <Check size={19} aria-hidden="true"/>
                        <strong>无需额外配置</strong>
                        <span>0 个配置项</span>
                    </div>
                ))}

                {mode === 'json' && (
                    <div className="flow-editor-inspector-json">
                <textarea
                    value={jsonText}
                    onChange={handleJsonChange}
                    onFocus={() => {
                        jsonFocusedRef.current = true
                    }}
                    onBlur={() => {
                        jsonFocusedRef.current = false
                    }}
                    readOnly={readOnly}
                    spellCheck="false"
                    aria-label="节点 JSON 配置"
                />
                        {jsonError && (
                            <div className="flow-editor-inspector-error">
                                <AlertCircle size={14} aria-hidden="true"/>
                                <span>{jsonError}</span>
                            </div>
                        )}
                    </div>
                )}

                {mode === 'manifest' && (
                    <pre className="flow-editor-inspector-manifest">{pretty(manifest || {})}</pre>
                )}

                {!jsonError && validationErrors.length > 0 && mode !== 'fields' && (
                    <div className="flow-editor-inspector-validation" role="alert">
                        <AlertCircle size={14} aria-hidden="true"/>
                        <div>
                            {validationErrors.slice(0, 4).map((error, index) => (
                                <span
                                    key={`${error.name}-${error.property}-${index}`}>{error.stack || error.message}</span>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            <footer className="flow-editor-inspector-footer">
                <span>{validationErrors.length ? `${validationErrors.length} 个配置问题` : node?.id}</span>
                <button type="button" onClick={apply}
                        disabled={readOnly || Boolean(jsonError) || validationErrors.length > 0}>
                    应用更改
                </button>
            </footer>
        </div>
    )
}

export default memo(NodeInspector)
