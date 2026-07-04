import '@testing-library/jest-dom/vitest'
import {cleanup} from '@testing-library/react'
import {createElement} from 'react'
import {afterEach, vi} from 'vitest'

class ResizeObserverMock {
  observe() {}

  unobserve() {}

  disconnect() {}
}

globalThis.ResizeObserver = globalThis.ResizeObserver || ResizeObserverMock

vi.mock('../components/FluidBackground/GlassFluidScene.jsx', () => ({
  default: () => createElement('div', { 'data-testid': 'glass-fluid-scene-mock' })
}))

// 每个测试后自动清理 DOM 并重置所有 mock
afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
})