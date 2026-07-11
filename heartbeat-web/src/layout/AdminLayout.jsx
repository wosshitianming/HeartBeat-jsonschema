import LayoutHeader from './LayoutHeader'
import LayoutSider from './LayoutSider'
import TagsView from './TagsView'

export default function AdminLayout({
  structureMode,
  topModules,
  sideMenus,
  tags,
  activeTopModuleId,
                                        activeTagKey,
  activeModuleKey,
  liquidGlassEnabled,
  glassMode,
  currentUser,
  busy,
  fluidEnabled,
  onFluidChange,
                                        colorMode,
                                        onColorModeChange,
                                        accentColor,
                                        onAccentColorChange,
                                        visualStyle,
                                        onVisualStyleChange,
                                        syncState,
  onSelectTopModule,
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
            colorMode={colorMode}
            onColorModeChange={onColorModeChange}
            accentColor={accentColor}
            onAccentColorChange={onAccentColorChange}
            visualStyle={visualStyle}
            onVisualStyleChange={onVisualStyleChange}
            syncState={syncState}
            onSelectTopModule={onSelectTopModule}
            onRefresh={onRefresh}
            onLogout={onLogout}
        />
        {!structureMode && (
            <TagsView
                tags={tags}
                activeTagId={activeTagKey || activeModuleKey}
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
