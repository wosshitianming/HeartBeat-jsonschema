import {useEffect, useState} from 'react'
import {authApi} from '../api'
import {
    applyAppearance,
    cacheAppearance,
    DEFAULT_APPEARANCE,
    normalizeAppearance,
    readCachedAppearance,
    readInitialAppearance,
    rememberUser,
    resolvedColorScheme,
    watchSystemColorScheme
} from './themeService'

export default function useAppearanceTheme(currentUser) {
  const [appearance, setAppearance] = useState(
      () => readInitialAppearance() || { ...DEFAULT_APPEARANCE }
  )
  const [syncState, setSyncState] = useState('idle')
  const userId = currentUser?.id

  useEffect(() => {
    applyAppearance(appearance)
    if (appearance.colorMode !== 'system') return undefined
    return watchSystemColorScheme(() => applyAppearance(appearance))
  }, [appearance])

  useEffect(() => {
    if (!userId) return undefined

    let active = true
    rememberUser(userId)
    const cached = readCachedAppearance(userId)
    setAppearance(cached)
    applyAppearance(cached)
    setSyncState('syncing')

    authApi.appearancePreference()
        .then((preference) => {
          if (!active) return
          const remoteAppearance = normalizeAppearance(preference)
          cacheAppearance(userId, remoteAppearance)
          applyAppearance(remoteAppearance)
          setAppearance(remoteAppearance)
          setSyncState('synced')
        })
        .catch(() => {
          if (active) setSyncState('pending')
        })

    return () => {
      active = false
    }
  }, [userId])

  async function changeAppearance(patch) {
    const nextAppearance = normalizeAppearance({ ...appearance, ...patch })
    setAppearance(nextAppearance)
    applyAppearance(nextAppearance)
    if (userId) cacheAppearance(userId, nextAppearance)
    setSyncState('syncing')

    try {
      const saved = await authApi.updateAppearancePreference(nextAppearance)
      const savedAppearance = normalizeAppearance(saved)
      if (userId) cacheAppearance(userId, savedAppearance)
      applyAppearance(savedAppearance)
      setAppearance(savedAppearance)
      setSyncState('synced')
    } catch {
      setSyncState('pending')
    }
  }

  return {
    appearance,
    colorMode: appearance.colorMode,
    fluidEnabled: appearance.fluidEnabled,
    accentColor: appearance.accentColor,
    visualStyle: appearance.visualStyle,
    colorScheme: resolvedColorScheme(appearance.colorMode),
    changeColorMode: (colorMode) => changeAppearance({ colorMode }),
    changeFluidEnabled: (fluidEnabled) => changeAppearance({ fluidEnabled }),
    changeAccentColor: (accentColor) => changeAppearance({ accentColor }),
    changeVisualStyle: (visualStyle) => changeAppearance({ visualStyle }),
    syncState
  }
}
