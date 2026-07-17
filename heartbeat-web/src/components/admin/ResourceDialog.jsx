import {useEffect, useRef, useState} from 'react'
import {getResourceDefinition} from '../../domain/admin/resourceDefinitions'

export default function ResourceDialog({open, mode, resource, values, busy = false, onClose, onSubmit}) {
  const definition = getResourceDefinition(resource)
  const [formValues, setFormValues] = useState(values || definition.emptyValues)
    const [submitError, setSubmitError] = useState('')
    const dialogRef = useRef(null)
    const onCloseRef = useRef(onClose)
    const busyRef = useRef(busy)
    const submittingRef = useRef(false)

  useEffect(() => {
    setFormValues(values || definition.emptyValues)
      setSubmitError('')
  }, [definition.emptyValues, resource, values])

    useEffect(() => {
        onCloseRef.current = onClose
    }, [onClose])

    useEffect(() => {
        busyRef.current = busy
    }, [busy])

    useEffect(() => {
        if (!open) return undefined

        const previousActiveElement = document.activeElement
        const frame = window.requestAnimationFrame(() => {
            const field = dialogRef.current?.querySelector('input:not([disabled]), select:not([disabled]), textarea:not([disabled])')
            const fallback = dialogRef.current?.querySelector('button:not([disabled])')
            ;(field || fallback)?.focus()
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
            previousActiveElement?.focus?.()
        }
    }, [open])

  if (!open) return null

  function updateValue(name, value) {
      setFormValues((previous) => ({...previous, [name]: value}))
  }

    async function handleSubmit(event) {
    event.preventDefault()
        if (busy || submittingRef.current) return
        submittingRef.current = true
        try {
            await onSubmit(formValues)
        } catch (error) {
            setSubmitError(error?.message || '保存失败，请检查表单内容')
        } finally {
            submittingRef.current = false
        }
  }

  const title = `${mode === 'edit' ? '编辑' : '新增'}${definition.title}`
  const fields = definition.fields.filter((field) => !(mode === 'edit' && field.createOnly))
    const titleId = 'resource-dialog-title'
    const descriptionId = 'resource-dialog-description'

  return (
      <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
              if (!busy && event.target === event.currentTarget) onCloseRef.current?.()
          }}
      >
          <form
              ref={dialogRef}
              className="resource-dialog"
              role="dialog"
              aria-modal="true"
              aria-labelledby={titleId}
              aria-describedby={descriptionId}
              aria-busy={busy}
              onSubmit={handleSubmit}
          >
          <div className="dialog-heading">
            <div>
              <span className="step">{mode === 'edit' ? 'EDIT' : 'CREATE'}</span>
                <h2 id={titleId}>{title}</h2>
            </div>
              <button
                  type="button"
                  className="text-button dialog-close-button"
                  aria-label={`关闭${title}`}
                  title="关闭"
                  disabled={busy}
                  onClick={() => onCloseRef.current?.()}
              >
                  ×
              </button>
          </div>

              <p id={descriptionId} className="dialog-required-hint">
                  标有 <span aria-hidden="true">*</span> 的项目为必填项
              </p>

              {submitError && <div className="error-banner backend-dialog-error" role="alert">{submitError}</div>}

          <div className="resource-form-grid">
              {fields.map((field) => {
                  const fieldId = `resource-field-${field.name}`
                  return (
                      <label
                          key={field.name}
                          htmlFor={fieldId}
                          className={field.type === 'textarea' || field.fullWidth ? 'full-field' : ''}
                      >
                    <span className="dialog-field-label">
                      {field.label}
                        {field.required && <span className="required-mark" aria-hidden="true"> *</span>}
                    </span>
                          {field.type === 'select' ? (
                              <select
                                  id={fieldId}
                                  value={formValues[field.name] ?? ''}
                                  required={field.required}
                                  disabled={busy || field.disabled}
                                  aria-required={field.required || undefined}
                                  onChange={(event) => updateValue(field.name, event.target.value)}
                              >
                                  {(field.options || []).map((option) => {
                                      const value = typeof option === 'object' ? option.value : option
                                      const label = typeof option === 'object' ? option.label : option
                                      return <option key={value} value={value}
                                                     disabled={Boolean(option?.disabled)}>{label}</option>
                                  })}
                              </select>
                          ) : field.type === 'textarea' ? (
                              <textarea
                                  id={fieldId}
                                  className="dialog-textarea"
                                  value={formValues[field.name] ?? ''}
                                  placeholder={field.placeholder}
                                  required={field.required}
                                  disabled={busy || field.disabled}
                                  aria-required={field.required || undefined}
                                  onChange={(event) => updateValue(field.name, event.target.value)}
                              />
                          ) : (
                              <input
                                  id={fieldId}
                                  type={field.type || 'text'}
                                  value={formValues[field.name] ?? ''}
                                  placeholder={field.placeholder}
                                  required={field.required}
                                  disabled={busy || field.disabled}
                                  aria-required={field.required || undefined}
                                  autoComplete={field.type === 'password' ? 'new-password' : undefined}
                                  onChange={(event) => updateValue(field.name, event.target.value)}
                              />
                          )}
                          {field.hint && <small className="dialog-field-hint">{field.hint}</small>}
                      </label>
                  )
              })}
          </div>

          <div className="dialog-actions">
              <button type="button" className="button ghost" disabled={busy}
                      onClick={() => onCloseRef.current?.()}>取消
              </button>
              <button type="submit" className="button primary" disabled={busy}>
                  {busy ? '保存中...' : (mode === 'edit' ? '保存修改' : '确认新增')}
              </button>
          </div>
        </form>
      </div>
  )
}
