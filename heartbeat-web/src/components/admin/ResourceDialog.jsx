import {useEffect, useState} from 'react'
import {getResourceDefinition} from '../../domain/admin/resourceDefinitions'

export default function ResourceDialog({ open, mode, resource, values, onClose, onSubmit }) {
  const definition = getResourceDefinition(resource)
  const [formValues, setFormValues] = useState(values || definition.emptyValues)

  useEffect(() => {
    setFormValues(values || definition.emptyValues)
  }, [values, resource])

  if (!open) return null

  function updateValue(name, value) {
    setFormValues((previous) => ({ ...previous, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit(formValues)
  }

  const title = `${mode === 'edit' ? '编辑' : '新增'}${definition.title}`
  const fields = definition.fields.filter((field) => !(mode === 'edit' && field.createOnly))

  return (
      <div className="modal-backdrop" role="presentation">
        <form className="resource-dialog" onSubmit={handleSubmit}>
          <div className="dialog-heading">
            <div>
              <span className="step">{mode === 'edit' ? 'EDIT' : 'CREATE'}</span>
              <h2>{title}</h2>
            </div>
            <button type="button" className="text-button" onClick={onClose}>关闭</button>
          </div>

          <div className="resource-form-grid">
            {fields.map((field) => (
                <label key={field.name} className={field.type === 'textarea' ? 'full-field' : ''}>
                  {field.label}
                  {field.type === 'select' ? (
                      <select
                          value={formValues[field.name] ?? ''}
                          onChange={(event) => updateValue(field.name, event.target.value)}
                      >
                        {(field.options || []).map((option) => (
                            <option key={option} value={option}>{option}</option>
                        ))}
                      </select>
                  ) : field.type === 'textarea' ? (
                      <textarea
                          className="dialog-textarea"
                          value={formValues[field.name] ?? ''}
                          onChange={(event) => updateValue(field.name, event.target.value)}
                          placeholder={field.placeholder}
                      />
                  ) : (
                      <input
                          type={field.type || 'text'}
                          value={formValues[field.name] ?? ''}
                          onChange={(event) => updateValue(field.name, event.target.value)}
                          placeholder={field.placeholder}
                          required={field.required}
                      />
                  )}
                </label>
            ))}
          </div>

          <div className="dialog-actions">
            <button type="button" className="button ghost" onClick={onClose}>取消</button>
            <button type="submit" className="button primary">{mode === 'edit' ? '保存修改' : '确认新增'}</button>
          </div>
        </form>
      </div>
  )
}
