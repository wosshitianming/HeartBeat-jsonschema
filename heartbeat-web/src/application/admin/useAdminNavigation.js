import {useEffect, useMemo, useState} from 'react'
import {adminApi, iamApi} from '../../api'
import {filterNavigableTree} from '../../domain/admin/navigationPolicy'
import {flattenRouteModules} from './adminModuleService'

export default function useAdminNavigation({fallbackModules = [], fallbackTree = []} = {}) {
    const [routeTree, setRouteTree] = useState([])
    const [modules, setModules] = useState(fallbackModules)
    const [status, setStatus] = useState('loading')
    const [error, setError] = useState(null)

    useEffect(() => {
        const controller = new AbortController()
        setStatus('loading')
        setError(null)

        iamApi.routes({signal: controller.signal})
            .then((routes) => {
                const safeRoutes = Array.isArray(routes) ? routes : []
                const nextModules = flattenRouteModules(safeRoutes)
                setRouteTree(safeRoutes)
                setModules(nextModules.length > 0 ? nextModules : fallbackModules)
                setStatus('ready')
            })
            .catch((err) => {
                if (err?.name === 'AbortError') return
                return adminApi.modules({signal: controller.signal})
                    .then((items) => {
                        setModules(Array.isArray(items) && items.length > 0 ? items : fallbackModules)
                        setStatus('fallback')
                    })
                    .catch((fallbackError) => {
                        if (fallbackError?.name === 'AbortError') return
                        setError(fallbackError)
                        setModules(fallbackModules)
                        setStatus('fallback')
                    })
            })

        return () => controller.abort()
    }, [fallbackModules])

    const navigationTree = useMemo(
        () => filterNavigableTree(routeTree.length > 0 ? routeTree : fallbackTree),
        [fallbackTree, routeTree]
    )

    return {
        routeTree,
        navigationTree,
        modules,
        status,
        error
    }
}
