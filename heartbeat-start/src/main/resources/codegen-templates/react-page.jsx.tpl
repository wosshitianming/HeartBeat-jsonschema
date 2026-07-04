import { useEffect, useState } from 'react'
import { adminApi } from '../../api'

export default function ${className}Page() {
  const [rows, setRows] = useState([])

  useEffect(() => {
    adminApi.resources('${resourceKey}').then(setRows)
  }, [])

  return (
    <div className="pig-page-card">
      <h1>${className} 管理</h1>
      <p>由 HeartBeat Flex 代码生成器生成的 PIG 风格列表页骨架。</p>
      <pre>{JSON.stringify(rows, null, 2)}</pre>
    </div>
  )
}
