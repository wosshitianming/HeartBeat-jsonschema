import styles from './AppearanceModeSelector.module.css'

const OPTIONS = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'system', label: '跟随系统' }
]

export default function AppearanceModeSelector({ value, onChange, disabled = false }) {
  return (
      <div className={styles.selector} role="radiogroup" aria-label="颜色模式">
        {OPTIONS.map((option) => (
            <label
                className={`${styles.option} ${value === option.value ? styles.active : ''}`}
                key={option.value}
            >
              <input
                  type="radio"
                  name="appearance-color-mode"
                  value={option.value}
                  checked={value === option.value}
                  disabled={disabled}
                  onChange={() => onChange(option.value)}
              />
              <span>{option.label}</span>
            </label>
        ))}
      </div>
  )
}
