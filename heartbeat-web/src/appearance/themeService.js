import {safeStorageGet, safeStorageSet} from '../infrastructure/browser/safeStorage'

export const COLOR_MODES = ['light', 'dark', 'system']
export const VISUAL_STYLES = ['flat', 'glass']
export const DEFAULT_ACCENT_COLOR = '#1677ff'
export const ACCENT_PRESETS = Object.freeze([
  '#1677ff',
  '#2f6bff',
  '#5b8def',
  '#7c5cfc',
  '#9b59b6',
  '#00a6a6',
  '#13c2c2',
  '#10b981',
  '#f5a623',
  '#fa541c',
  '#eb2f96',
  '#475467'
])
export const DEFAULT_APPEARANCE = Object.freeze({
  colorMode: 'dark',
  fluidEnabled: true,
  accentColor: DEFAULT_ACCENT_COLOR,
  visualStyle: 'glass'
})
export const DEFAULT_INITIAL_APPEARANCE = Object.freeze({
    colorMode: 'light',
    fluidEnabled: false,
    accentColor: DEFAULT_ACCENT_COLOR,
    visualStyle: 'flat'
})
export const LAST_USER_KEY = 'heartbeat_last_user_id'

const HEX_COLOR = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/

export function normalizeAccentColor(value) {
  if (typeof value !== 'string') return DEFAULT_ACCENT_COLOR
  const trimmed = value.trim()
  return HEX_COLOR.test(trimmed) ? trimmed.toLowerCase() : DEFAULT_ACCENT_COLOR
}

export function normalizeAppearance(appearance) {
  const colorMode = COLOR_MODES.includes(appearance?.colorMode)
      ? appearance.colorMode
      : DEFAULT_APPEARANCE.colorMode
  const fluidEnabled = typeof appearance?.fluidEnabled === 'boolean'
      ? appearance.fluidEnabled
      : DEFAULT_APPEARANCE.fluidEnabled
  const accentColor = normalizeAccentColor(appearance?.accentColor)
  const visualStyle = VISUAL_STYLES.includes(appearance?.visualStyle)
      ? appearance.visualStyle
      : DEFAULT_APPEARANCE.visualStyle
  return { colorMode, fluidEnabled, accentColor, visualStyle }
}

export function appearanceStorageKey(userId) {
  return `heartbeat_appearance:${userId || 'anonymous'}`
}

function legacyAppearance(userId) {
    const legacyTheme = safeStorageGet(`heartbeat_appearance_theme:${userId || 'anonymous'}`)
  if (!legacyTheme) return null
  return {
    colorMode: 'dark',
    fluidEnabled: legacyTheme !== 'professional',
    accentColor: DEFAULT_ACCENT_COLOR,
    visualStyle: legacyTheme === 'professional' ? 'flat' : 'glass'
  }
}

export function readCachedAppearance(userId) {
    const cached = safeStorageGet(appearanceStorageKey(userId))
  if (cached) {
    try {
      return normalizeAppearance(JSON.parse(cached))
    } catch {
      // Fall through to legacy migration.
    }
  }
  const migrated = legacyAppearance(userId)
  if (migrated) {
    cacheAppearance(userId, migrated)
    return migrated
  }
    return {...DEFAULT_INITIAL_APPEARANCE}
}

export function readInitialAppearance() {
    const lastUserId = safeStorageGet(LAST_USER_KEY)
    return lastUserId ? readCachedAppearance(lastUserId) : {...DEFAULT_INITIAL_APPEARANCE}
}

export function cacheAppearance(userId, appearance) {
  const normalized = normalizeAppearance(appearance)
    safeStorageSet(appearanceStorageKey(userId), JSON.stringify(normalized))
  return normalized
}

export function rememberUser(userId) {
    if (userId) safeStorageSet(LAST_USER_KEY, String(userId))
}

export function resolvedColorScheme(colorMode, matchMedia = window.matchMedia?.bind(window)) {
  if (colorMode !== 'system') return colorMode
  return matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyAppearance(appearance, matchMedia = window.matchMedia?.bind(window)) {
  const normalized = normalizeAppearance(appearance)
  const colorScheme = resolvedColorScheme(normalized.colorMode, matchMedia)
  document.documentElement.dataset.colorMode = normalized.colorMode
  document.documentElement.dataset.colorScheme = colorScheme
  document.documentElement.dataset.theme = colorScheme
  document.documentElement.dataset.visualStyle = normalized.visualStyle
  document.documentElement.style.setProperty('--accent', normalized.accentColor)
  return { ...normalized, colorScheme }
}

export function watchSystemColorScheme(
    onChange,
    matchMedia = window.matchMedia?.bind(window)
) {
  if (!matchMedia) return () => {}
  const media = matchMedia('(prefers-color-scheme: dark)')
  const handler = (event) => onChange(event.matches)
  if (media.addEventListener) {
    media.addEventListener('change', handler)
    return () => media.removeEventListener('change', handler)
  }
  media.addListener?.(handler)
  return () => media.removeListener?.(handler)
}
