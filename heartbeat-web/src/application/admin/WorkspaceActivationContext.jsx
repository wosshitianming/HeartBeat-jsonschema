import {createContext, useContext} from 'react'

export const WorkspaceActivationContext = createContext({
    active: true,
    tagKey: '',
    setTitle: () => {
    }
})

export function WorkspaceActivationProvider({value, children}) {
    return (
        <WorkspaceActivationContext.Provider value={value}>
            {children}
        </WorkspaceActivationContext.Provider>
    )
}

export function useWorkspaceActivation() {
    return useContext(WorkspaceActivationContext)
}
