import {useEffect} from 'react'
import {useWorkspaceActivation} from './WorkspaceActivationContext'

export default function useWorkspaceTitle(title) {
    const {active, tagKey, setTitle} = useWorkspaceActivation()

    useEffect(() => {
        if (!active || !tagKey || !title) return
        setTitle(tagKey, title)
    }, [active, tagKey, setTitle, title])
}
