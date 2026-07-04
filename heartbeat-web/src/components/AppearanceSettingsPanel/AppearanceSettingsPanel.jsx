import {useState} from 'react'
import AppleSwitch from '../AppleSwitch/AppleSwitch'
import AppearanceModeSelector from '../AppearanceModeSelector/AppearanceModeSelector'
import AccentColorPicker from '../AccentColorPicker/AccentColorPicker'
import VisualStyleSelector from '../VisualStyleSelector/VisualStyleSelector'
import styles from './AppearanceSettingsPanel.module.css'

export default function AppearanceSettingsPanel({
  colorMode,
  onColorModeChange,
  accentColor,
  onAccentColorChange,
  visualStyle,
  onVisualStyleChange,
  fluidEnabled,
  onFluidChange,
  syncState,
  className = ''
}) {
  const [open, setOpen] = useState(false)
  const syncHint = syncState === 'pending' ? '等待同步' : '跟随当前账号保存'
  const fluidDescription = visualStyle === 'glass'
      ? 'WebGL 液态玻璃背景折射'
      : '低功耗 CSS 动效'

  return (
      <section className={`${styles.panel} ${className}`.trim()}>
        <button
            type="button"
            className={styles.toggle}
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
        >
          <span className={styles.toggleLabel}>主题与视觉效果</span>
          <span className={styles.chevron} aria-hidden="true">{open ? '▾' : '▸'}</span>
        </button>

        {open && (
            <div className={styles.body}>
              <div className={styles.field}>
                <small>颜色模式</small>
                <AppearanceModeSelector value={colorMode} onChange={onColorModeChange} />
              </div>
              <div className={styles.field}>
                <small>主题色</small>
                <AccentColorPicker value={accentColor} onChange={onAccentColorChange} />
              </div>
              <div className={styles.field}>
                <small>界面风格</small>
                <VisualStyleSelector value={visualStyle} onChange={onVisualStyleChange} />
              </div>
              <AppleSwitch
                  label="背景动效"
                  description={`${fluidDescription} · ${syncHint}`}
                  checked={fluidEnabled}
                  onChange={onFluidChange}
              />
            </div>
        )}
      </section>
  )
}
