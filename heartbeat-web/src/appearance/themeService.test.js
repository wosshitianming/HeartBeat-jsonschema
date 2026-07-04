import {afterEach, expect, test, vi} from 'vitest'
import {
    appearanceStorageKey,
    applyAppearance,
    cacheAppearance,
    DEFAULT_APPEARANCE,
    normalizeAppearance,
    readCachedAppearance,
    readInitialAppearance,
    watchSystemColorScheme
} from './themeService'

afterEach(() => {
  window.localStorage.clear()
  delete document.documentElement.dataset.theme
  delete document.documentElement.dataset.colorMode
  delete document.documentElement.dataset.colorScheme
  delete document.documentElement.dataset.visualStyle
})

test('stores appearance independently for each user', () => {
  cacheAppearance('user-a', { colorMode: 'light', fluidEnabled: true, accentColor: '#7c5cfc', visualStyle: 'glass' })
  cacheAppearance('user-b', { colorMode: 'system', fluidEnabled: false, visualStyle: 'flat' })

  expect(appearanceStorageKey('user-a')).toBe('heartbeat_appearance:user-a')
  expect(readCachedAppearance('user-a')).toEqual({
    colorMode: 'light',
    fluidEnabled: true,
    accentColor: '#7c5cfc',
    visualStyle: 'glass'
  })
  expect(readCachedAppearance('user-b')).toEqual({
    colorMode: 'system',
    fluidEnabled: false,
    accentColor: '#1677ff',
    visualStyle: 'flat'
  })
})

test('normalizes unsupported appearance values', () => {
  expect(DEFAULT_APPEARANCE).toEqual({
    colorMode: 'dark',
    fluidEnabled: true,
    accentColor: '#1677ff',
    visualStyle: 'glass'
  })
  expect(normalizeAppearance({ colorMode: 'neon', fluidEnabled: 'no', accentColor: 'nope', visualStyle: 'neon' }))
      .toEqual(DEFAULT_APPEARANCE)
})

test('accepts and lowercases a valid custom accent color', () => {
  expect(normalizeAppearance({
    colorMode: 'dark',
    fluidEnabled: true,
    accentColor: '#1ABC9C',
    visualStyle: 'flat'
  })).toEqual({
    colorMode: 'dark',
    fluidEnabled: true,
    accentColor: '#1abc9c',
    visualStyle: 'flat'
  })
})

test('applies the accent color and visual style to the document root', () => {
  applyAppearance({
    colorMode: 'dark',
    fluidEnabled: true,
    accentColor: '#10b981',
    visualStyle: 'glass'
  }, () => ({ matches: true }))
  expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#10b981')
  expect(document.documentElement.dataset.visualStyle).toBe('glass')
})

test('migrates the previous per-user theme cache', () => {
  window.localStorage.setItem('heartbeat_appearance_theme:user-a', 'professional')

  expect(readCachedAppearance('user-a')).toEqual({
    colorMode: 'dark',
    fluidEnabled: false,
    accentColor: '#1677ff',
    visualStyle: 'flat'
  })
})

test('applies system mode and reacts when the operating system color scheme changes', () => {
  let listener
  const media = {
    matches: false,
    addEventListener: (_event, callback) => { listener = callback },
    removeEventListener: vi.fn()
  }
  const matchMedia = vi.fn(() => media)
  const onChange = vi.fn()

  applyAppearance({ colorMode: 'system', fluidEnabled: true, visualStyle: 'glass' }, matchMedia)
  const stop = watchSystemColorScheme(onChange, matchMedia)
  listener({ matches: true })

  expect(document.documentElement.dataset.colorMode).toBe('system')
  expect(document.documentElement.dataset.colorScheme).toBe('light')
  expect(document.documentElement.dataset.visualStyle).toBe('glass')
  expect(onChange).toHaveBeenCalledWith(true)
  stop()
  expect(media.removeEventListener).toHaveBeenCalled()
})

test('restores the most recent user appearance before authentication finishes', () => {
  window.localStorage.setItem('heartbeat_last_user_id', 'user-a')
  cacheAppearance('user-a', { colorMode: 'light', fluidEnabled: true, visualStyle: 'glass' })

  expect(readInitialAppearance()).toEqual({
    colorMode: 'light',
    fluidEnabled: true,
    accentColor: '#1677ff',
    visualStyle: 'glass'
  })
})
