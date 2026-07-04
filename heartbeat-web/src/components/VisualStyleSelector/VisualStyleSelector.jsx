import styles from './VisualStyleSelector.module.css'

const OPTIONS = [
  { value: 'glass', label: '液态玻璃', description: 'WebGL 折射 + 毛玻璃面板' },
  { value: 'flat', label: '扁平后台', description: '实色面板，低功耗' }
]

export default function VisualStyleSelector({ value, onChange, disabled = false }) {
  return (
      <div className={styles.selector} role="radiogroup" aria-label="界面风格">
        {OPTIONS.map((option) => (
            <label
                className={`${styles.option} ${value === option.value ? styles.active : ''}`}
                key={option.value}
            >
              <input
                  type="radio"
                  name="appearance-visual-style"
                  value={option.value}
                  checked={value === option.value}
                  disabled={disabled}
                  onChange={() => onChange(option.value)}
              />
              <span className={styles.label}>{option.label}</span>
              <span className={styles.description}>{option.description}</span>
            </label>
        ))}
      </div>
  )
}
