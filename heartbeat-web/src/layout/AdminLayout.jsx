import LayoutHeader from './LayoutHeader'
import LayoutSider from './LayoutSider'
import TagsView from './TagsView'

export default function AdminLayout({
  structureMode,
  topModules,
  sideMenus,
  tags,
  activeTopModuleId,
  activeModuleKey,
  liquidGlassEnabled,
  glassMode,
  currentUser,
  busy,
  fluidEnabled,
  onFluidChange,
  onSelectTopModule,
  onGlassModeChange,
  onSelectMenu,
  onSelectTag,
  onCloseTag,
  onRefresh,
  onLogout,
  children
}) {
  return (
      <div
          className={`hb-admin hb-admin-layout ${structureMode ? 'structure-mode' : ''} ${liquidGlassEnabled ? 'liquid-glass-enabled' : ''}`}
          data-glass-mode={glassMode || 'flat'}
      >
        <LayoutHeader
            topModules={topModules}
            activeTopModuleId={activeTopModuleId}
            currentUser={currentUser}
            busy={busy}
            fluidEnabled={fluidEnabled}
            onFluidChange={onFluidChange}
            onSelectTopModule={onSelectTopModule}
            glassMode={glassMode}
            onGlassModeChange={onGlassModeChange}
            onRefresh={onRefresh}
            onLogout={onLogout}
        />
        {!structureMode && (
            <TagsView
                tags={tags}
                activeTagId={activeModuleKey}
                onSelect={onSelectTag}
                onClose={onCloseTag}
            />
        )}
        {!structureMode && (
            <LayoutSider
                sideMenus={sideMenus}
                activeModuleKey={activeModuleKey}
                onSelectMenu={onSelectMenu}
            />
        )}
        <main className="hb-layout-main">
          {children}
        </main>
      </div>
  )
}
