import {useCallback, useEffect, useRef, useState} from 'react'
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

const SAVE_DEBOUNCE_MS = 180

export default function useAppearanceTheme(currentUser) {
  const [appearance, setAppearance] = useState(
      () => readInitialAppearance() || { ...DEFAULT_APPEARANCE }
  )
  const [syncState, setSyncState] = useState('idle')
  const userId = currentUser?.id
    const appearanceRef = useRef(appearance)
    const requestIdRef = useRef(0)
    const preferenceControllerRef = useRef(null)
    // Debounce the first write, then serialize and coalesce changes while a PUT is in flight.
    const saveQueueRef = useRef({
        controller: null,
        generation: 0,
        inFlight: false,
        latestVersion: 0,
        pending: null,
        timer: null
    })

    const resetSaveQueue = useCallback(() => {
        const queue = saveQueueRef.current
        if (queue.timer) clearTimeout(queue.timer)
        queue.controller?.abort()
        queue.controller = null
        queue.generation += 1
        queue.inFlight = false
        queue.latestVersion = 0
        queue.pending = null
        queue.timer = null
    }, [])

    const flushPendingSave = useCallback(async function flushPendingSave() {
        const queue = saveQueueRef.current
        if (queue.inFlight || !queue.pending) return

        const job = queue.pending
        const generation = queue.generation
        const controller = new AbortController()
        queue.pending = null
        queue.inFlight = true
        queue.controller = controller

        try {
            const saved = await authApi.updateAppearancePreference(job.appearance, {signal: controller.signal})
            if (controller.signal.aborted || generation !== queue.generation) return

            const isLatest = job.version === queue.latestVersion && !queue.pending
            if (isLatest) {
                const savedAppearance = saved ? normalizeAppearance(saved) : job.appearance
                cacheAppearance(job.userId, savedAppearance)
                applyAppearance(savedAppearance)
                appearanceRef.current = savedAppearance
                setAppearance(savedAppearance)
                setSyncState('synced')
            }
        } catch (error) {
            if (error?.name === 'AbortError' || generation !== queue.generation) return
            if (!queue.pending && job.version === queue.latestVersion) {
                setSyncState('pending')
            }
        } finally {
            if (generation !== queue.generation) return
            queue.controller = null
            queue.inFlight = false
            if (queue.pending) {
                void flushPendingSave()
            }
        }
    }, [])

    const scheduleAppearanceSave = useCallback((targetUserId, nextAppearance) => {
        const queue = saveQueueRef.current
        const version = ++queue.latestVersion
        queue.pending = {
            appearance: nextAppearance,
            userId: targetUserId,
            version
        }
        setSyncState('syncing')

        if (queue.timer) clearTimeout(queue.timer)
        queue.timer = null
        if (!queue.inFlight) {
            queue.timer = setTimeout(() => {
                queue.timer = null
                void flushPendingSave()
            }, SAVE_DEBOUNCE_MS)
        }
    }, [flushPendingSave])

  useEffect(() => {
      appearanceRef.current = appearance
    applyAppearance(appearance)
    if (appearance.colorMode !== 'system') return undefined
    return watchSystemColorScheme(() => applyAppearance(appearance))
  }, [appearance])

  useEffect(() => {
      preferenceControllerRef.current?.abort()
      resetSaveQueue()
      if (!userId) {
          preferenceControllerRef.current = null
          return undefined
      }

      const controller = new AbortController()
      const requestId = ++requestIdRef.current
      preferenceControllerRef.current = controller
    rememberUser(userId)
    const cached = readCachedAppearance(userId)
      appearanceRef.current = cached
    setAppearance(cached)
    applyAppearance(cached)
    setSyncState('syncing')

      authApi.appearancePreference({signal: controller.signal})
        .then((preference) => {
            if (controller.signal.aborted || requestId !== requestIdRef.current) return
          const remoteAppearance = normalizeAppearance(preference)
          cacheAppearance(userId, remoteAppearance)
          applyAppearance(remoteAppearance)
            appearanceRef.current = remoteAppearance
          setAppearance(remoteAppearance)
          setSyncState('synced')
        })
          .catch((error) => {
              if (error?.name === 'AbortError' || requestId !== requestIdRef.current) return
              setSyncState('pending')
        })

    return () => {
        controller.abort()
        resetSaveQueue()
        if (preferenceControllerRef.current === controller) {
            preferenceControllerRef.current = null
        }
    }
  }, [resetSaveQueue, userId])

    const changeAppearance = useCallback(async (patch) => {
        const nextAppearance = normalizeAppearance({...appearanceRef.current, ...patch})
        appearanceRef.current = nextAppearance
    setAppearance(nextAppearance)
    applyAppearance(nextAppearance)
    if (userId) cacheAppearance(userId, nextAppearance)
        if (!userId) {
            setSyncState('idle')
            return nextAppearance
        }

        preferenceControllerRef.current?.abort()
        preferenceControllerRef.current = null
        requestIdRef.current += 1
        scheduleAppearanceSave(userId, nextAppearance)
        return nextAppearance
    }, [scheduleAppearanceSave, userId])

    const changeColorMode = useCallback(
        (colorMode) => changeAppearance({colorMode}),
        [changeAppearance]
    )
    const changeFluidEnabled = useCallback(
        (fluidEnabled) => changeAppearance({fluidEnabled}),
        [changeAppearance]
    )
    const changeAccentColor = useCallback(
        (accentColor) => changeAppearance({accentColor}),
        [changeAppearance]
    )
    const changeVisualStyle = useCallback(
        (visualStyle) => changeAppearance({visualStyle}),
        [changeAppearance]
    )

  return {
    appearance,
    colorMode: appearance.colorMode,
    fluidEnabled: appearance.fluidEnabled,
    accentColor: appearance.accentColor,
    visualStyle: appearance.visualStyle,
    colorScheme: resolvedColorScheme(appearance.colorMode),
      changeAppearance,
      changeColorMode,
      changeFluidEnabled,
      changeAccentColor,
      changeVisualStyle,
    syncState
  }
}
