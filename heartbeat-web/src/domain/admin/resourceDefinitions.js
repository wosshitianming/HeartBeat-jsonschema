const STATUS_OPTIONS = ['ACTIVE', 'DISABLED']
const DATA_SCOPE_OPTIONS = ['ALL', 'DEPT', 'DEPT_AND_CHILD', 'SELF', 'CUSTOM']

function payloadDisplay(payload) {
  if (!payload || payload === '{}') return '—'
  try {
    const parsed = JSON.parse(payload)
    if (parsed.value !== undefined) return String(parsed.value)
    if (parsed.cron !== undefined) return String(parsed.cron)
    return JSON.stringify(parsed)
  } catch {
    return payload
  }
}

export const READ_ONLY_RESOURCES = new Set(['oper-logs', 'login-logs', 'online-sessions', 'job-logs'])

export const RESOURCE_DEFINITIONS = {
  menus: {
    title: '菜单',
    readOnly: false,
    columns: ['名称', '类型', '路径', '权限标识', '状态'],
    fields: [
      { name: 'parentId', label: '上级菜单 ID', placeholder: '顶级菜单可留空' },
      { name: 'type', label: '菜单类型', type: 'select', options: ['DIR', 'MENU', 'BUTTON'] },
      { name: 'name', label: '菜单名称', required: true },
      { name: 'path', label: '路由地址' },
      { name: 'component', label: '组件路径' },
      { name: 'permission', label: '权限标识' },
      { name: 'icon', label: '图标' },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'visible', label: '是否显示', type: 'select', options: ['true', 'false'] },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      parentId: '',
      type: 'MENU',
      name: '',
      path: '',
      component: '',
      permission: '',
      icon: '',
      sortNo: 0,
      visible: 'true',
      status: 'ACTIVE'
    }
  },
  users: {
    title: '用户',
    readOnly: false,
    columns: ['用户名', '昵称', '部门 ID', '邮箱', '状态'],
    fields: [
      { name: 'username', label: '用户名', required: true },
      { name: 'nickname', label: '昵称', required: true },
      { name: 'password', label: '初始密码', type: 'password', createOnly: true },
      { name: 'email', label: '邮箱' },
      { name: 'phone', label: '手机号' },
      { name: 'deptId', label: '部门 ID' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      username: '',
      nickname: '',
      password: '123456',
      email: '',
      phone: '',
      deptId: '',
      status: 'ACTIVE'
    }
  },
  depts: {
    title: '部门',
    readOnly: false,
    columns: ['部门名称', '部门编码', '上级 ID', '状态'],
    fields: [
      { name: 'parentId', label: '上级部门 ID' },
      { name: 'name', label: '部门名称', required: true },
      { name: 'code', label: '部门编码' },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'description', label: '负责人/备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      parentId: '',
      name: '',
      code: '',
      sortNo: 0,
      description: '',
      status: 'ACTIVE'
    }
  },
  posts: {
    title: '岗位',
    readOnly: false,
    columns: ['岗位名称', '岗位编码', '排序', '状态'],
    fields: [
      { name: 'name', label: '岗位名称', required: true },
      { name: 'code', label: '岗位编码' },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: '',
      sortNo: 0,
      description: '',
      status: 'ACTIVE'
    }
  },
  roles: {
    title: '角色',
    readOnly: false,
    columns: ['角色名称', '权限字符', '数据范围', '状态'],
    fields: [
      { name: 'name', label: '角色名称', required: true },
      { name: 'code', label: '权限字符', required: true },
      { name: 'dataScope', label: '数据范围', type: 'select', options: DATA_SCOPE_OPTIONS },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: '',
      dataScope: 'ALL',
      sortNo: 0,
      description: '',
      status: 'ACTIVE'
    }
  },
  'dict-types': {
    title: '字典类型',
    readOnly: false,
    columns: ['字典名称', '字典类型', '备注', '状态'],
    fields: [
      { name: 'name', label: '字典名称', required: true },
      { name: 'code', label: '字典类型', required: true },
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: '',
      description: '',
      status: 'ACTIVE'
    }
  },
  configs: {
    title: '参数',
    readOnly: false,
    columns: ['参数名称', '参数键名', '参数键值', '状态'],
    fields: [
      { name: 'name', label: '参数名称', required: true },
      { name: 'code', label: '参数键名', required: true },
      { name: 'payload', label: '参数键值', required: true },
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: '',
      payload: '',
      description: '',
      status: 'ACTIVE'
    }
  },
  notices: {
    title: '公告',
    readOnly: false,
    columns: ['公告标题', '公告类型', '状态', '备注'],
    fields: [
      { name: 'name', label: '公告标题', required: true },
      { name: 'code', label: '公告类型' },
      { name: 'description', label: '公告内容', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: 'NOTICE',
      description: '',
      status: 'ACTIVE'
    }
  },
  jobs: {
    title: '定时任务',
    readOnly: false,
    columns: ['任务名称', '任务编码', 'Cron 表达式', '状态'],
    fields: [
      { name: 'name', label: '任务名称', required: true },
      { name: 'code', label: '任务编码/Bean', required: true },
      { name: 'payload', label: 'Cron 表达式', placeholder: '0 0 2 * * ?' },
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      name: '',
      code: '',
      payload: '0 0 2 * * ?',
      description: '',
      status: 'ACTIVE'
    }
  },
  'oper-logs': {
    title: '操作日志',
    readOnly: true,
    columns: ['操作人', '模块', '结果', '说明'],
    fields: [],
    emptyValues: {}
  },
  'login-logs': {
    title: '登录日志',
    readOnly: true,
    columns: ['用户名', '账号', '结果', '说明'],
    fields: [],
    emptyValues: {}
  },
  'online-sessions': {
    title: '在线用户',
    readOnly: true,
    columns: ['用户', '会话标识', '状态', '说明'],
    fields: [],
    emptyValues: {}
  },
  default: {
    title: '配置',
    readOnly: false,
    columns: ['名称', '编码', '状态', '说明'],
    fields: [
      { name: 'parentId', label: '上级 ID' },
      { name: 'name', label: '名称', required: true },
      { name: 'code', label: '编码' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'dataScope', label: '数据范围' },
      { name: 'description', label: '说明', type: 'textarea' }
    ],
    emptyValues: {
      parentId: '',
      name: '',
      code: '',
      status: 'ACTIVE',
      sortNo: 0,
      dataScope: '',
      description: ''
    }
  }
}

export function getResourceDefinition(resource) {
  return RESOURCE_DEFINITIONS[resource] || RESOURCE_DEFINITIONS.default
}

export function isResourceReadOnly(resource) {
  if (!resource) return true
  return READ_ONLY_RESOURCES.has(resource) || Boolean(getResourceDefinition(resource).readOnly)
}

export function actionsForResource(resource) {
  if (!resource) return ['刷新']
  if (resource === 'roles') return ['新增', '修改', '删除', '分配菜单', '刷新', '导出']
  if (isResourceReadOnly(resource)) return ['刷新', '导出']
  return ['新增', '修改', '删除', '刷新', '导出']
}

export { payloadDisplay }
