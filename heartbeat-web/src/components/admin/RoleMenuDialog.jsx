import {useEffect, useState} from 'react'

function collectDescendantIds(node, target = []) {
  target.push(node.id)
  ;(node.children || []).forEach((child) => collectDescendantIds(child, target))
  return target
}

function MenuTreeNode({ node, checkedIds, onToggle }) {
  const checked = checkedIds.includes(node.id)
  const children = node.children || []

  return (
      <li className="role-menu-node">
        <label className="role-menu-label">
          <input
              type="checkbox"
              checked={checked}
              onChange={() => onToggle(node)}
          />
          <span>{node.label}</span>
          {node.type && <small className="role-menu-type">{node.type}</small>}
        </label>
        {children.length > 0 && (
            <ul className="role-menu-children">
              {children.map((child) => (
                  <MenuTreeNode
                      key={child.id}
                      node={child}
                      checkedIds={checkedIds}
                      onToggle={onToggle}
                  />
              ))}
            </ul>
        )}
      </li>
  )
}

export default function RoleMenuDialog({ open, role, busy, onClose, onSubmit }) {
  const [menuTree, setMenuTree] = useState([])
  const [checkedIds, setCheckedIds] = useState([])

  useEffect(() => {
    if (!open || !role) return
    setMenuTree(role.menuTree || [])
    setCheckedIds(role.menuIds || [])
  }, [open, role])

  if (!open || !role) return null

  function toggleNode(node) {
    const descendants = collectDescendantIds(node, [])
    const allChecked = descendants.every((id) => checkedIds.includes(id))
    setCheckedIds((previous) => {
      if (allChecked) {
        const remove = new Set(descendants)
        return previous.filter((id) => !remove.has(id))
      }
      const merged = new Set(previous)
      descendants.forEach((id) => merged.add(id))
      return Array.from(merged)
    })
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit(checkedIds)
  }

  return (
      <div className="modal-backdrop" role="presentation">
        <form className="resource-dialog role-menu-dialog" onSubmit={handleSubmit}>
          <div className="dialog-heading">
            <div>
              <span className="step">GRANT</span>
              <h2>分配菜单 — {role.roleName}</h2>
            </div>
            <button type="button" className="text-button" onClick={onClose}>关闭</button>
          </div>

          <p className="role-menu-hint">勾选该角色可访问的菜单与按钮权限，保存后立即生效。</p>

          <div className="role-menu-tree-wrap">
            {menuTree.length === 0 ? (
                <div className="table-empty">暂无菜单数据</div>
            ) : (
                <ul className="role-menu-tree">
                  {menuTree.map((node) => (
                      <MenuTreeNode
                          key={node.id}
                          node={node}
                          checkedIds={checkedIds}
                          onToggle={toggleNode}
                      />
                  ))}
                </ul>
            )}
          </div>

          <div className="dialog-actions">
            <button type="button" className="button ghost" onClick={onClose}>取消</button>
            <button type="submit" className="button primary" disabled={busy}>
              {busy ? '保存中…' : '保存授权'}
            </button>
          </div>
        </form>
      </div>
  )
}
