import {ACCENT_PRESETS, normalizeAccentColor} from '../../appearance/themeService'

export default function AccentColorPicker({ value, onChange, disabled = false }) {
  const current = normalizeAccentColor(value)

  return (
      <div className="accent-picker" role="group" aria-label="主题色">
        <div className="accent-swatches">
          {ACCENT_PRESETS.map((preset) => (
              <button
                  type="button"
                  key={preset}
                  className={`accent-swatch ${current === preset ? 'active' : ''}`}
                  style={{ '--swatch': preset }}
                  aria-label={`主题色 ${preset}`}
                  aria-pressed={current === preset}
                  disabled={disabled}
                  onClick={() => onChange(preset)}
              />
          ))}
        </div>
        <label className="accent-custom">
          <span className="accent-custom-chip" style={{ background: current }} aria-hidden="true" />
          <span className="accent-custom-label">自定义</span>
          <input
              type="color"
              aria-label="自定义主题色"
              value={current}
              disabled={disabled}
              onChange={(event) => onChange(event.target.value)}
          />
        </label>
      </div>
  )
}
