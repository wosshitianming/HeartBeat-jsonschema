export default function ResourceTable({
  moduleName,
  columns,
  records,
  selectedRow,
  editable,
  onSelect,
  onEdit,
  onDelete
}) {
  return (
      <div className="resource-table-wrap">
        <table className="resource-table" aria-label={`${moduleName}列表`}>
          <thead>
            <tr>
              {columns.map((column) => <th key={column} scope="col">{column}</th>)}
              {editable && <th scope="col" className="table-action-column">操作</th>}
            </tr>
          </thead>
          <tbody>
            {records.map((record, index) => (
                <tr
                    className={selectedRow?.id === record.id ? 'selected-row' : ''}
                    key={record.id || index}
                    onClick={() => onSelect(record)}
                >
                  {columns.map((column) => <td key={column}>{record[column] || '—'}</td>)}
                  {editable && (
                      <td className="row-actions">
                        <button
                            className="table-link"
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation()
                              onEdit(record)
                            }}
                        >
                          编辑
                        </button>
                        <button
                            className="table-link danger-text"
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation()
                              onDelete(record)
                            }}
                        >
                          删除
                        </button>
                      </td>
                  )}
                </tr>
            ))}
          </tbody>
        </table>
        {records.length === 0 && <div className="table-empty">暂无数据</div>}
      </div>
  )
}
