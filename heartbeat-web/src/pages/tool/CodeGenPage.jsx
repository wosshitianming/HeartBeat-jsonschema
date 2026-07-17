import {useEffect, useState} from 'react'
import {toolApi} from '../../api'
import {hasPermission} from '../../domain/admin/permissionPolicy'

export default function CodeGenPage({permissions = [], busy, onBusy, onError}) {
  const [dbTables, setDbTables] = useState([])
  const [importedTables, setImportedTables] = useState([])
  const [selectedImportId, setSelectedImportId] = useState('')
  const [previewFiles, setPreviewFiles] = useState(null)
    const canImport = hasPermission(permissions, 'tool:gen:import')
    const canDownload = hasPermission(permissions, 'tool:gen:download')

  async function loadTables() {
    onBusy('codegen-load')
    onError('')
    try {
      const [databaseTables, imported] = await Promise.all([
        toolApi.listDbTables(),
        toolApi.listImportedTables()
      ])
      setDbTables(Array.isArray(databaseTables) ? databaseTables : [])
      setImportedTables(Array.isArray(imported) ? imported : [])
      if (!selectedImportId && imported?.[0]?.id) {
        setSelectedImportId(imported[0].id)
      }
    } catch (err) {
      onError(err.message || '加载代码生成数据失败')
    } finally {
      onBusy('')
    }
  }

  useEffect(() => {
    loadTables()
  }, [])

  async function handleImport(tableName) {
    onBusy(`import-${tableName}`)
    onError('')
    try {
      await toolApi.importTable(tableName)
      await loadTables()
    } catch (err) {
      onError(err.message || '导入失败')
    } finally {
      onBusy('')
    }
  }

  async function handlePreview() {
    if (!selectedImportId) return
    onBusy('codegen-preview')
    onError('')
    try {
      const preview = await toolApi.previewCodegen(selectedImportId)
      setPreviewFiles(preview)
    } catch (err) {
      onError(err.message || '预览失败')
    } finally {
      onBusy('')
    }
  }

  async function handleDownload() {
    if (!selectedImportId) return
    onBusy('codegen-download')
    onError('')
    try {
      const blob = await toolApi.downloadCodegen(selectedImportId)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `heartbeat-codegen-${selectedImportId}.zip`
      link.click()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      onError(err.message || '下载失败')
    } finally {
      onBusy('')
    }
  }

  return (
      <div className="hb-page-card codegen-page">
        <h1>代码生成</h1>
        <p>基于 MyBatis Generator 导入表结构，预览并下载 DO/Example/DOMapper/Repository/Controller 与 React 页面骨架。</p>

        <section className="codegen-section">
          <h2>数据库表</h2>
          <div className="codegen-table-list">
            {dbTables.map((table) => (
                <div className="codegen-table-item" key={table.tableName}>
                  <strong>{table.tableName}</strong>
                    {canImport && (
                        <button
                            type="button"
                            className="button ghost"
                            disabled={Boolean(busy)}
                            onClick={() => handleImport(table.tableName)}
                        >
                            导入
                        </button>
                    )}
                </div>
            ))}
          </div>
        </section>

        <section className="codegen-section">
          <h2>已导入配置</h2>
          <div className="codegen-toolbar">
            <select
                value={selectedImportId}
                onChange={(event) => setSelectedImportId(event.target.value)}
                aria-label="已导入表"
            >
              {importedTables.map((item) => (
                  <option key={item.id} value={item.id}>{item.name || item.code}</option>
              ))}
            </select>
            <button type="button" className="button ghost" disabled={Boolean(busy) || !selectedImportId} onClick={handlePreview}>
              预览代码
            </button>
              {canDownload && (
                  <button type="button" className="button primary" disabled={Boolean(busy) || !selectedImportId}
                          onClick={handleDownload}>
                      下载 ZIP
                  </button>
              )}
            <button type="button" className="button ghost" disabled={Boolean(busy)} onClick={loadTables}>
              刷新
            </button>
          </div>
        </section>

        {previewFiles && (
            <section className="codegen-section">
              <h2>预览</h2>
              {Object.entries(previewFiles).map(([fileName, content]) => (
                  <details key={fileName} className="codegen-preview-item">
                    <summary>{fileName}</summary>
                    <pre>{content}</pre>
                  </details>
              ))}
            </section>
        )}
      </div>
  )
}
