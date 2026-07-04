import styles from './AppleSwitch.module.css'

export default function AppleSwitch({
  checked,
  onChange,
  label,
  disabled = false,
  description
}) {
  return (
      <label className={`${styles.switchWrapper} ${disabled ? styles.disabled : ''}`}>
        <input
            type="checkbox"
            role="switch"
            className={styles.hiddenCheckbox}
            checked={checked}
            disabled={disabled}
            aria-label={label}
            onChange={(event) => onChange(event.target.checked)}
        />
        <span className={styles.switchTrack} aria-hidden="true">
          <span className={styles.switchKnob} />
        </span>
        {(label || description) && (
            <span className={styles.switchCopy}>
              {label && <span className={styles.switchLabel}>{label}</span>}
              {description && <small>{description}</small>}
            </span>
        )}
      </label>
  )
}
