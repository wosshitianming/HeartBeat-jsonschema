const STATUS_OPTIONS = ['ENABLED', 'DISABLED']
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

export const READ_ONLY_RESOURCES = new Set([
    'tenants',
    'posts',
    'dict-types',
    'dict-data',
    'notices',
    'oauth-clients',
    'oper-logs',
    'login-logs',
    'online-sessions',
    'jobs',
    'job-logs'
])

export const RESOURCE_DEFINITIONS = {
  menus: {
    title: '菜单',
    readOnly: false,
      columns: ['菜单编码', '名称', '类型', '路径', '权限模式', '状态'],
    fields: [
      { name: 'parentId', label: '上级菜单 ID', placeholder: '顶级菜单可留空' },
        {name: 'menuCode', label: '菜单编码', required: true, placeholder: '例如 system:user'},
        {name: 'menuType', label: '菜单类型', type: 'select', options: ['CATALOG', 'MENU', 'BUTTON']},
        {name: 'menuName', label: '菜单名称', required: true},
        {name: 'routePath', label: '路由地址'},
        {name: 'componentPath', label: '组件路径'},
        {name: 'redirectPath', label: '重定向地址'},
        {name: 'permissionMode', label: '权限模式', type: 'select', options: ['RELATION']},
      { name: 'icon', label: '图标' },
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'visible', label: '是否显示', type: 'select', options: ['true', 'false'] },
        {name: 'keepAlive', label: '页面缓存', type: 'select', options: ['true', 'false']},
        {name: 'externalLink', label: '外部链接'},
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      parentId: '',
        menuCode: '',
        menuType: 'MENU',
        menuName: '',
        routePath: '',
        componentPath: '',
        redirectPath: '',
        permissionMode: 'RELATION',
      icon: '',
      sortNo: 0,
      visible: 'true',
        keepAlive: 'false',
        externalLink: '',
        status: 'ENABLED'
    }
  },
  users: {
    title: '用户',
    readOnly: false,
    columns: ['用户名', '昵称', '部门 ID', '邮箱', '状态'],
    fields: [
      { name: 'username', label: '用户名', required: true },
      { name: 'nickname', label: '昵称', required: true },
        {name: 'password', label: '初始密码', type: 'password', createOnly: true, required: true},
      { name: 'email', label: '邮箱' },
      { name: 'phone', label: '手机号' },
      { name: 'deptId', label: '部门 ID' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      username: '',
      nickname: '',
        password: '',
      email: '',
      phone: '',
      deptId: '',
        status: 'ENABLED'
    }
  },
  depts: {
    title: '部门',
    readOnly: false,
      columns: ['部门名称', '部门编码', '上级 ID', '负责人 ID', '状态'],
    fields: [
      { name: 'parentId', label: '上级部门 ID' },
      { name: 'name', label: '部门名称', required: true },
      { name: 'code', label: '部门编码' },
        {name: 'leaderUserId', label: '负责人 ID'},
        {name: 'phone', label: '联系电话'},
        {name: 'email', label: '联系邮箱', type: 'email'},
      { name: 'sortNo', label: '排序', type: 'number' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
      parentId: '',
      name: '',
      code: '',
        leaderUserId: '',
        phone: '',
        email: '',
      sortNo: 0,
        status: 'ENABLED'
    }
  },
  posts: {
    title: '岗位',
      readOnly: true,
    columns: ['岗位名称', '岗位编码', '排序', '状态'],
      fields: [],
      emptyValues: {}
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
        status: 'ENABLED'
    }
  },
  'dict-types': {
    title: '字典类型',
      readOnly: true,
    columns: ['字典名称', '字典类型', '备注', '状态'],
      fields: [],
      emptyValues: {}
  },
    'dict-data': {
        title: '字典数据',
        readOnly: true,
        columns: ['字典类型 ID', '标签', '键值', '排序', '状态'],
        fields: [],
        emptyValues: {}
  },
  configs: {
    title: '参数',
    readOnly: false,
      columns: ['参数名称', '参数键名', '参数键值', '类型', '状态'],
    fields: [
        {name: 'configName', label: '参数名称', required: true},
        {name: 'configKey', label: '参数键名', required: true},
        {name: 'configValue', label: '参数键值', required: true, type: 'textarea'},
        {name: 'valueType', label: '值类型', type: 'select', options: ['STRING', 'NUMBER', 'BOOLEAN', 'JSON']},
        {name: 'encrypted', label: '加密存储', type: 'select', options: ['true', 'false']},
        {name: 'configGroup', label: '参数分组'},
      { name: 'description', label: '备注', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
        configName: '',
        configKey: '',
        configValue: '',
        valueType: 'STRING',
        encrypted: 'false',
        configGroup: 'system',
      description: '',
        status: 'ENABLED'
    }
  },
  notices: {
    title: '公告',
      readOnly: true,
      columns: ['公告标题', '公告类型', '发布状态', '发布时间', '公告内容'],
      fields: [],
      emptyValues: {}
  },
  jobs: {
    title: '定时任务',
      readOnly: true,
      columns: ['任务名称', '任务编码', 'Cron 表达式', '状态'],
      fields: [],
      emptyValues: {}
  },
    tenants: {
        title: '租户',
        readOnly: true,
        columns: ['租户编码', '租户名称', '租户类型', '域名', '联系人', '状态'],
        fields: [],
        emptyValues: {}
    },
    'oauth-clients': {
        title: 'OAuth 客户端',
        readOnly: true,
        columns: ['客户端 ID', '客户端名称', '客户端类型', '授权范围', '自动授权', '状态'],
        fields: [],
        emptyValues: {}
    },
    'social-providers': {
        title: '社交登录源',
    readOnly: false,
        columns: ['提供方编码', '提供方名称', '类型', '客户端 ID', '启用', '状态'],
    fields: [
        {name: 'providerCode', label: '提供方编码', required: true},
        {name: 'providerName', label: '提供方名称', required: true},
        {name: 'providerType', label: '提供方类型', type: 'select', options: ['OAUTH2', 'OIDC']},
        {name: 'clientId', label: '客户端 ID'},
        {name: 'appKey', label: 'App Key'},
        {name: 'appSecretCipher', label: '客户端密钥', type: 'password', hint: '编辑时留空将保留现有密钥'},
        {name: 'authorizeUrl', label: '授权地址'},
        {name: 'tokenUrl', label: '令牌地址'},
        {name: 'userInfoUrl', label: '用户信息地址'},
        {name: 'scopes', label: '授权范围'},
        {name: 'enabled', label: '是否启用', type: 'select', options: ['true', 'false']},
      { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS }
    ],
    emptyValues: {
        providerCode: '',
        providerName: '',
        providerType: 'OAUTH2',
        clientId: '',
        appKey: '',
        appSecretCipher: '',
        authorizeUrl: '',
        tokenUrl: '',
        userInfoUrl: '',
        scopes: '',
        enabled: 'true',
        status: 'ENABLED'
    }
  },
  'oper-logs': {
    title: '操作日志',
    readOnly: true,
      columns: ['操作人', '模块', '操作', '请求路径', '结果', '耗时'],
    fields: [],
    emptyValues: {}
  },
  'login-logs': {
    title: '登录日志',
    readOnly: true,
      columns: ['用户名', '登录方式', '登录 IP', '结果', '失败原因', '登录时间'],
    fields: [],
    emptyValues: {}
  },
  'online-sessions': {
    title: '在线用户',
    readOnly: true,
      columns: ['用户 ID', '会话标识', '设备', '登录 IP', '状态', '最后访问'],
    fields: [],
    emptyValues: {}
  },
  default: {
    title: '配置',
      readOnly: true,
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
        status: 'ENABLED',
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
