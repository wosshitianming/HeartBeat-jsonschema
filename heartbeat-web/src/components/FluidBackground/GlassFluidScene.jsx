import {useRef} from 'react'
import {Canvas, useFrame} from '@react-three/fiber'
import {MeshTransmissionMaterial} from '@react-three/drei'

function LiquidGlassOrb({ color, position, scale = 1, speed = 1 }) {
  const meshRef = useRef(null)

  useFrame((state) => {
    const mesh = meshRef.current
    if (!mesh) return

    const t = state.clock.elapsedTime * speed
    mesh.rotation.x = Math.sin(t * 0.35) * 0.32 + 0.15
    mesh.rotation.y = t * 0.16
    mesh.rotation.z = Math.sin(t * 0.22) * 0.1
    const pulse = scale * (1 + Math.sin(t * 0.85) * 0.07 + Math.cos(t * 0.52) * 0.04)
    mesh.scale.setScalar(pulse)
  })

  return (
      <mesh ref={meshRef} position={position}>
        <icosahedronGeometry args={[1.2, 64]} />
        <MeshTransmissionMaterial
            backside
            backsideThickness={0.35}
            samples={10}
            resolution={768}
            transmission={1}
            roughness={0.04}
            thickness={1.1}
            ior={1.22}
            chromaticAberration={0.05}
            anisotropy={0.18}
            distortion={0.28}
            distortionScale={0.4}
            temporalDistortion={0.14}
            color={color}
            attenuationDistance={0.85}
            attenuationColor={color}
        />
      </mesh>
  )
}

function RefractionBackdrop({ colorScheme }) {
  const primary = colorScheme === 'light' ? '#6eb6ff' : '#2f6bff'
  const secondary = colorScheme === 'light' ? '#b794ff' : '#7c5cfc'
  const tertiary = colorScheme === 'light' ? '#5eead4' : '#13c2c2'

  return (
      <>
        <ambientLight intensity={colorScheme === 'light' ? 0.65 : 0.42} />
        <directionalLight position={[6, 8, 4]} intensity={1.1} color="#ffffff" />
        <pointLight position={[-5, 3, 2]} intensity={0.9} color={secondary} />
        <pointLight position={[4, -2, 3]} intensity={0.75} color={primary} />
        <mesh position={[-3.2, 0.8, -4.5]} scale={2.4}>
          <sphereGeometry args={[1, 48, 48]} />
          <meshBasicMaterial color={primary} />
        </mesh>
        <mesh position={[3.4, -1.1, -5.2]} scale={1.9}>
          <sphereGeometry args={[1, 48, 48]} />
          <meshBasicMaterial color={secondary} />
        </mesh>
        <mesh position={[0.2, 2.4, -6]} scale={1.5}>
          <sphereGeometry args={[1, 48, 48]} />
          <meshBasicMaterial color={tertiary} />
        </mesh>
      </>
  )
}

export default function GlassFluidScene({ accentColor = '#1677ff', colorScheme = 'dark' }) {
  const background = colorScheme === 'light' ? '#e8edf5' : '#0c0e14'

  return (
      <Canvas
          className="glass-fluid-canvas"
          camera={{ position: [0, 0, 6.2], fov: 40 }}
          dpr={[1, 1.75]}
          gl={{ alpha: true, antialias: true, powerPreference: 'high-performance' }}
      >
        <color attach="background" args={[background]} />
        <RefractionBackdrop colorScheme={colorScheme} />
        <LiquidGlassOrb color={accentColor} position={[1.1, 0.15, 0]} scale={1.35} speed={1} />
        <LiquidGlassOrb color="#5ac8fa" position={[-1.4, -0.55, -0.4]} scale={0.95} speed={0.82} />
        <LiquidGlassOrb color="#9b59b6" position={[0.2, 1.05, -0.8]} scale={0.72} speed={1.18} />
      </Canvas>
  )
}
