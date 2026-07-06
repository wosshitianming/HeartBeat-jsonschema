import {useCallback} from 'react'
import {WorkspaceActivationProvider} from '../../application/admin/WorkspaceActivationContext'

export default function WorkspaceViewHost({views, activeKey, onTitleChange, renderView}) {
    const setTitle = useCallback((tagKey, title) => {
        onTitleChange?.(tagKey, title)
    }, [onTitleChange])

    return (
        <div className="workspace-view-host">
            {views.map((view) => {
                const key = view.key || view.id
                const active = key === activeKey
                return (
                    <section
                        key={key}
                        className={`workspace-view ${active ? 'active' : 'inactive'}`}
                        aria-hidden={!active}
                        style={{display: active ? 'block' : 'none'}}
                    >
                        <WorkspaceActivationProvider value={{active, tagKey: key, setTitle}}>
                            {renderView(view, active)}
                        </WorkspaceActivationProvider>
                    </section>
                )
            })}
        </div>
    )
}
