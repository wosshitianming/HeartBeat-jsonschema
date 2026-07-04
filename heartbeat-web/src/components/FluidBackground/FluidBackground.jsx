import {lazy, Suspense, useMemo} from 'react'
import styles from './FluidBackground.module.css'

const GlassFluidScene = lazy(() => import('./GlassFluidScene'))

function CssFluidLayer() {
  return (
      <>
        <span className={`${styles.blob} ${styles.blobPrimary}`} />
        <span className={`${styles.blob} ${styles.blobSecondary}`} />
      </>
  )
}

export default function FluidBackground({
  enabled = true,
  visualStyle = 'glass',
  accentColor = '#1677ff',
  colorScheme = 'dark'
}) {
  const useWebGL = visualStyle === 'glass'

  const containerClassName = useMemo(() => {
    const classes = [styles.backgroundContainer]
    if (useWebGL) classes.push(styles.webglMode)
    return classes.join(' ')
  }, [useWebGL])

  if (!enabled) return null

  return (
      <div
          className={containerClassName}
          data-testid="fluid-background"
          data-visual-style={visualStyle}
          aria-hidden="true"
      >
        {useWebGL ? (
            <Suspense fallback={<CssFluidLayer />}>
              <div className={styles.webglLayer} data-testid="glass-fluid-scene">
                <GlassFluidScene accentColor={accentColor} colorScheme={colorScheme} />
              </div>
            </Suspense>
        ) : (
            <CssFluidLayer />
        )}
      </div>
  )
}
