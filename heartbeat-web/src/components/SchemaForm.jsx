// src/components/SchemaForm.jsx
import Form from '@rjsf/core';
import validator from '@rjsf/validator-ajv8';

export default function SchemaForm({ schema, uiSchema, formData, onChange, onSubmit }) {
    const handleSubmit = ({ formData }) => {
        onSubmit?.(formData);
    };

    const handleChange = ({ formData }) => {
        onChange?.(formData);
    };

    // 如果 schema 无效，显示提示
    if (!schema || typeof schema !== 'object' || Object.keys(schema).length === 0) {
        return (
            <div className="empty-state">
                <span>📋</span>
                <p>当前结构定义中没有有效的 JSON Schema，无法生成表单。</p>
            </div>
        );
    }

    return (
        <div className="schema-form-container">
            <Form
                schema={schema}
                uiSchema={uiSchema || {}}
                formData={formData}
                validator={validator}
                onChange={handleChange}
                onSubmit={handleSubmit}
                liveValidate
                showErrorList={false}
            >
                <button type="submit" className="button primary" style={{ marginTop: 12 }}>
                    提交表单
                </button>
            </Form>
        </div>
    );
}