import {useEffect, useMemo, useState} from 'react'

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

export default function ResourceTable({
  moduleName,
  columns,
  records,
  selectedRow,
  editable,
                                          canEdit = editable,
                                          canDelete = editable,
  onSelect,
  onEdit,
  onDelete
}) {
    const tableName = moduleName || '记录'
    const [query, setQuery] = useState('')
    const density = records.length > 50 ? 'dense' : 'normal'
    const normalizedQuery = query.trim().toLocaleLowerCase()
    const filteredRecords = useMemo(() => {
        if (!normalizedQuery) return records
        return records.filter((record) => {
            const searchableValues = [record.id, ...columns.map((column) => record[column])]
            return searchableValues.some((value) => (
                formatCellValue(value).toLocaleLowerCase().includes(normalizedQuery)
            ))
        })
    }, [columns, normalizedQuery, records])

    useEffect(() => {
        setQuery('')
    }, [moduleName])

    function selectRow(record) {
        onSelect?.(record)
    }

    function isSelected(record) {
        if (selectedRow === record) return true
        return selectedRow?.id !== undefined
            && selectedRow?.id !== null
            && selectedRow.id === record.id
    }

  return (
      <div className="resource-table-section">
          <div className="resource-table-toolbar">
              <label className="resource-table-search">
                  <span>搜索</span>
                  <input
                      type="search"
                      value={query}
                      placeholder={`搜索${tableName}`}
                      aria-label={`搜索${tableName}`}
                      onChange={(event) => setQuery(event.target.value)}
                  />
              </label>
              <span className="resource-table-count" role="status" aria-live="polite">
            {normalizedQuery
                ? `找到 ${filteredRecords.length} 条，共 ${records.length} 条`
                : `共 ${records.length} 条`}
          </span>
          </div>

          <div className="resource-table-wrap" data-density={density}>
              <table className="resource-table resource-table-sticky" aria-label={`${tableName}列表`}>
                  <thead className="sticky-table-header">
                  <tr>
                      {columns.map((column) => <th key={column} scope="col">{column}</th>)}
                      <th scope="col" className="table-action-column">操作</th>
                  </tr>
                  </thead>
                  <tbody>
                  {filteredRecords.map((record, index) => (
                      <tr
                          className={isSelected(record) ? 'selected-row' : ''}
                          key={record.id ?? index}
                          onClick={() => selectRow(record)}
                      >
                          {columns.map((column) => (
                              <td key={column} title={formatCellValue(record[column])}>
                                  {formatCellValue(record[column])}
                              </td>
                          ))}
                          <td className="table-action-cell">
                              <div className="row-actions">
                                  <button
                                      className="table-link"
                                      type="button"
                                      onClick={(event) => {
                                          event.stopPropagation()
                                          selectRow(record)
                                      }}
                                  >
                                      {isSelected(record) ? '已选择' : '选择'}
                                  </button>
                                  {(canEdit || canDelete) && (
                                      <>
                                          {canEdit && (
                                          <button
                                              className="table-link"
                                              type="button"
                                              onClick={(event) => {
                                                  event.stopPropagation()
                                                  onEdit?.(record)
                                              }}
                                          >
                                              编辑
                                          </button>
                                          )}
                                          {canDelete && (
                                          <button
                                              className="table-link danger-text"
                                              type="button"
                                              onClick={(event) => {
                                                  event.stopPropagation()
                                                  onDelete?.(record)
                                              }}
                                          >
                                              删除
                                          </button>
                                          )}
                                      </>
                                  )}
                              </div>
                          </td>
                      </tr>
                  ))}
                  </tbody>
              </table>
              {records.length === 0 && (
                  <div className="table-empty">暂无{tableName}数据</div>
              )}
              {records.length > 0 && filteredRecords.length === 0 && (
                  <div className="table-empty">
                      <p>未找到与“{query.trim()}”匹配的记录</p>
                      <button type="button" className="text-button" onClick={() => setQuery('')}>清除搜索</button>
                  </div>
              )}
          </div>
      </div>
  )
}
