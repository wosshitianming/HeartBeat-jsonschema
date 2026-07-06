import {useEffect, useState} from 'react'
import {authApi} from '../../api'

export function rememberSessionUser(user) {
    if (!user?.id) return
    try {
        const session = JSON.parse(localStorage.getItem('heartbeat_admin_session') || '{}')
        localStorage.setItem('heartbeat_admin_session', JSON.stringify({
            ...session,
            userId: user.id
        }))
    } catch {
        localStorage.setItem('heartbeat_admin_session', JSON.stringify({userId: user.id}))
    }
}

export function saveAuthSession(result) {
    localStorage.setItem('heartbeat_admin_session', JSON.stringify({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        userId: result.user?.id
    }))
}

export default function useAdminSession() {
    const [currentUser, setCurrentUser] = useState(null)
    const [authChecked, setAuthChecked] = useState(false)
    const [socialProviders, setSocialProviders] = useState([])

    useEffect(() => {
        let mounted = true
        const saved = localStorage.getItem('heartbeat_admin_session')
        if (!saved) {
            setAuthChecked(true)
            return () => {
                mounted = false
            }
        }
        authApi.me()
            .then((user) => {
                if (mounted) {
                    rememberSessionUser(user)
                    setCurrentUser(user)
                }
            })
            .catch(() => {
                localStorage.removeItem('heartbeat_admin_session')
            })
            .finally(() => {
                if (mounted) setAuthChecked(true)
            })
        return () => {
            mounted = false
        }
    }, [])

    useEffect(() => {
        if (!authChecked || currentUser) return undefined
        let mounted = true
        authApi.socialProviders()
            .then((items) => {
                if (mounted) setSocialProviders(Array.isArray(items) ? items : [])
            })
            .catch(() => {
                if (mounted) setSocialProviders([])
            })
        return () => {
            mounted = false
        }
    }, [authChecked, currentUser])

    return {
        authChecked,
        currentUser,
        setCurrentUser,
        socialProviders,
        setSocialProviders
    }
}
