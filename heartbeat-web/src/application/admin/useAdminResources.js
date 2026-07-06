import {useEffect, useMemo, useState} from 'react'
import {adminApi, iamApi} from '../../api'
import {flattenMenuRows, recordFromResource, resourceFromModule} from './adminModuleService'

export default function useAdminResources({currentUser, activeModule}) {
    const activeResource = useMemo(() => resourceFromModule(activeModule), [activeModule])
    const [recordsByResource, setRecordsByResource] = useState({})
    const [status, setStatus] = useState('idle')
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!currentUser || !activeResource || activeModule?.key === 'structure') return undefined
        const controller = new AbortController()
        setStatus('loading')
        setError(null)

        const request = activeResource === 'menus'
            ? iamApi.menus({signal: controller.signal})
            : adminApi.resources(activeResource, {signal: controller.signal})

        request
            .then((items) => {
                const rows = activeResource === 'menus'
                    ? flattenMenuRows(items)
                    : items.map((item) => recordFromResource(activeResource, item))
                setRecordsByResource((previous) => ({...previous, [activeResource]: rows}))
                setStatus('ready')
            })
            .catch((err) => {
                if (err?.name === 'AbortError') return
                setError(err)
                setStatus('error')
            })

        return () => controller.abort()
    }, [activeModule?.key, activeResource, currentUser])

    return {
        activeResource,
        records: activeResource ? recordsByResource[activeResource] || [] : [],
        recordsByResource,
        status,
        error
    }
}
