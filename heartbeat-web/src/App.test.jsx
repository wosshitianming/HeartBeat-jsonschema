import {fireEvent, render, screen, waitFor, within} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {afterEach, expect, test, vi} from 'vitest'
import {MemoryRouter} from 'react-router-dom'
import App from './App'

afterEach(() => {
  vi.restoreAllMocks()
    vi.unstubAllGlobals()
  window.localStorage.clear()
})

function mockLoggedIn() {
  window.localStorage.setItem('heartbeat_admin_session', JSON.stringify({ accessToken: 'test' }))
}

function authMeResponse() {
  return {
    ok: true,
    json: async () => ({
      code: '0',
      msg: 'success',
        data: {id: '1', username: 'admin', nickname: '超级管理员', permissions: ['*']}
    })
  }
}

function routesResponse() {
  return {
    ok: true,
      json: async () => ({
          code: '0',
          msg: 'success',
          data: [
              {id: 'system:user', type: 'MENU', status: 'ENABLED'},
              {id: 'structure:definition', type: 'MENU', status: 'ENABLED'}
          ]
      })
  }
}

function adminModulesResponse() {
  return {
    ok: true,
    json: async () => ({ code: '0', msg: 'success', data: [] })
  }
}

function renderApp() {
    return render(
        <MemoryRouter
            initialEntries={['/admin/structure-definitions']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <App/>
        </MemoryRouter>
    )
}

test('previews parsed samples and renders generated JSON Schema', async () => {
  mockLoggedIn()
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') {
      return authMeResponse()
    }
    if (url === '/api/v1/iam/routes') {
      return routesResponse()
    }
    if (url === '/api/v1/admin/modules') {
      return adminModulesResponse()
    }
    return {
      ok: true,
      json: async () => ({
        code: '0',
        msg: 'success',
        data: {
          artifacts: {
            JSON_SCHEMA: {
              $schema: 'https://json-schema.org/draft/2020-12/schema',
              type: 'object',
              properties: { name: { type: 'string' } }
            },
              UI_SCHEMA: {fields: {name: {title: '姓名', widget: 'text'}}}
          },
          warnings: []
        }
      })
    }
  })

    renderApp()

  const editor = await screen.findByLabelText('JSON 样例数组')
  fireEvent.change(editor, { target: { value: '[{"name":"Alice"}]' } })
  await userEvent.click(screen.getByRole('button', { name: '预览生成' }))

  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/structure-definitions/preview',
      expect.any(Object)
  ))
  const [, request] = fetchMock.mock.calls.find(([url]) => url === '/api/v1/structure-definitions/preview')
  expect(JSON.parse(request.body).samples).toEqual([{ name: 'Alice' }])
  expect(await screen.findByText(/draft\/2020-12/)).toBeInTheDocument()

  await userEvent.click(screen.getByRole('button', { name: '表单预览' }))
    expect(await screen.findByRole('textbox', {name: '姓名'}, {timeout: 10000})).toBeInTheDocument()
})

test('generates editable Chinese title overrides from sample field names', async () => {
  mockLoggedIn()
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') {
      return authMeResponse()
    }
    if (url === '/api/v1/iam/routes') {
      return routesResponse()
    }
    return adminModulesResponse()
  })
    renderApp()

  fireEvent.change(await screen.findByLabelText('JSON 样例数组'), {
    target: { value: '[{"name1":"Alice","age1":20}]' }
  })
  await userEvent.click(screen.getByText('UI Schema 覆盖配置'))
  await userEvent.click(screen.getByRole('button', { name: '根据字段名生成中文标题' }))

  const overrides = JSON.parse(screen.getByLabelText('UI Schema 覆盖配置').value)
  expect(overrides['$.name1']).toEqual({ title: '姓名' })
  expect(overrides['$.age1']).toEqual({ title: '年龄' })
})

test('activates the version on the definition whose version button was clicked', async () => {
  mockLoggedIn()
  const definitions = [
    {
      id: 'definition-a',
      name: 'A',
      description: '',
      activeVersionNo: 1,
      versions: [{ versionNo: 1, validationMode: 'LENIENT' }]
    },
    {
      id: 'definition-b',
      name: 'B',
      description: '',
      activeVersionNo: 1,
      versions: [
        { versionNo: 1, validationMode: 'LENIENT' },
        { versionNo: 2, validationMode: 'STRICT' }
      ]
    }
  ]

  const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options = {}) => {
    if (url === '/api/v1/auth/me') {
      return authMeResponse()
    }
    if (url === '/api/v1/iam/routes') {
      return routesResponse()
    }
    if (url === '/api/v1/admin/modules') {
      return adminModulesResponse()
    }
    if (options.method === 'PUT') {
      // 激活成功后返回更新后的定义对象（包含 id）
      const updated = { ...definitions[1], activeVersionNo: 2 }
      return {
        ok: true,
        json: async () => ({ code: '0', msg: 'success', data: updated })
      }
    } else {
      // GET 请求返回定义列表
      return {
        ok: true,
        json: async () => ({ code: '0', msg: 'success', data: definitions })
      }
    }
  })

    renderApp()
  await userEvent.click(await screen.findByRole('button', { name: '刷新列表' }))
  await screen.findByText('B')
  // 关键：先点击定义卡片，选中 B
  await userEvent.click(screen.getByText('B'))
  // 再点击版本按钮 v2
  await userEvent.click(screen.getByRole('button', { name: /v2/i }))

  await waitFor(() => {
    expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/structure-definitions/definition-b/active-version',
        expect.objectContaining({ method: 'PUT' })
    )
  })
})

test('toggles the fluid background and synchronizes the user preference', async () => {
  mockLoggedIn()
    vi.stubGlobal('CSS', {supports: () => true})
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options = {}) => {
    if (url === '/api/v1/auth/me') {
      return authMeResponse()
    }
    if (url === '/api/v1/auth/preferences/appearance' && options.method === 'PUT') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: JSON.parse(options.body)
        })
      }
    }
    if (url === '/api/v1/auth/preferences/appearance') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
            data: {colorMode: 'dark', fluidEnabled: true, visualStyle: 'glass'}
        })
      }
    }
    if (url === '/api/v1/iam/routes') {
      return routesResponse()
    }
    return adminModulesResponse()
  })

    renderApp()

  expect(await screen.findByTestId('fluid-background')).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', {name: '外观设置'}))
  await userEvent.click(await screen.findByRole('switch', { name: '背景动效' }))

  expect(screen.queryByTestId('fluid-background')).not.toBeInTheDocument()
  expect(document.documentElement.dataset.colorScheme).toBe('dark')
  expect(JSON.parse(window.localStorage.getItem('heartbeat_appearance:1'))).toEqual({
    colorMode: 'dark',
    fluidEnabled: false,
    accentColor: '#1677ff',
    visualStyle: 'glass'
  })
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/auth/preferences/appearance',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({
          colorMode: 'dark',
          fluidEnabled: false,
          accentColor: '#1677ff',
          visualStyle: 'glass'
        }),
        headers: expect.objectContaining({ 'X-User-Id': '1' })
      })
  ))
})

test('renders grouped desktop navigation and a semantic resource table', async () => {
  mockLoggedIn()
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') return authMeResponse()
    if (url === '/api/v1/auth/preferences/appearance') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: { colorMode: 'dark', fluidEnabled: true }
        })
      }
    }
    if (url === '/api/v1/iam/routes') return routesResponse()
    if (url === '/api/v1/admin/resources/users') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: [{ id: '2', username: 'alice', nickname: 'Alice', deptId: '1', status: 'ACTIVE' }]
        })
      }
    }
    return adminModulesResponse()
  })

    renderApp()

  const desktopNav = await screen.findByLabelText('后台导航')
    const topNav = screen.getByRole('navigation', {name: '一级模块导航'})
    await userEvent.click(within(topNav).getByRole('button', {name: '平台治理'}))
    expect(within(desktopNav).getByText('账号与组织')).toBeInTheDocument()
  await userEvent.click(within(desktopNav).getByRole('button', { name: /用户管理/ }))

  expect(await screen.findByRole('table', { name: '用户管理列表' })).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: '用户管理', level: 1 })).toBeInTheDocument()
})

test('keeps authorized workspaces available through route-backed page tags', async () => {
  mockLoggedIn()
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') return authMeResponse()
    if (url === '/api/v1/auth/preferences/appearance') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: { colorMode: 'dark', fluidEnabled: true }
        })
      }
    }
    if (url === '/api/v1/iam/routes') return routesResponse()
    if (url === '/api/v1/admin/resources/users') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: [{ id: '2', username: 'alice', nickname: 'Alice', deptId: '1', status: 'ACTIVE' }]
        })
      }
    }
    return adminModulesResponse()
  })

    renderApp()

    const topNav = await screen.findByRole('navigation', {name: '一级模块导航'})
    await userEvent.click(within(topNav).getByRole('button', {name: '平台治理'}))
    const desktopNav = screen.getByLabelText('后台导航')
    await userEvent.click(within(desktopNav).getByRole('button', {name: /用户管理/}))

    expect(await screen.findByRole('table', {name: '用户管理列表'})).toBeInTheDocument()
    const tags = screen.getByRole('navigation', {name: '已打开页面'})
    expect(within(tags).getByRole('button', {name: '结构定义'})).toBeInTheDocument()
    expect(within(tags).getByRole('button', {name: '用户管理'})).toBeInTheDocument()

    await userEvent.click(within(tags).getByRole('button', {name: '结构定义'}))
    expect(await screen.findByLabelText('JSON 样例数组')).toBeInTheDocument()
})
