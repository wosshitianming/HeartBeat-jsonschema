import {useEffect, useRef} from 'react'

const MAX_PIXEL_RATIO = 1.25
const FRAME_INTERVAL = 1000 / 30
const TWO_PI = Math.PI * 2

const FALLBACK_ACCENT = {r: 22, g: 119, b: 255}
const CYAN = {r: 90, g: 200, b: 250}
const VIOLET = {r: 155, g: 89, b: 182}

function parseHexColor(value) {
    const match = /^#([\da-f]{3}|[\da-f]{6})$/i.exec(value || '')
    if (!match) return FALLBACK_ACCENT

    const normalized = match[1].length === 3
        ? match[1].split('').map((character) => character + character).join('')
        : match[1]

    return {
        r: Number.parseInt(normalized.slice(0, 2), 16),
        g: Number.parseInt(normalized.slice(2, 4), 16),
        b: Number.parseInt(normalized.slice(4, 6), 16)
    }
}

function rgba(color, alpha) {
    return `rgba(${color.r}, ${color.g}, ${color.b}, ${alpha})`
}

function createBlobPath(context, radius, phase) {
    const points = 12
    const coordinates = []

    for (let index = 0; index < points; index += 1) {
        const angle = index / points * TWO_PI
        const variance = 1
            + Math.sin(angle * 3 + phase) * 0.055
            + Math.cos(angle * 2 - phase * 0.7) * 0.035
        coordinates.push({
            x: Math.cos(angle) * radius * variance,
            y: Math.sin(angle) * radius * variance
        })
    }

    context.beginPath()
    coordinates.forEach((point, index) => {
        const next = coordinates[(index + 1) % points]
        const midpointX = (point.x + next.x) / 2
        const midpointY = (point.y + next.y) / 2
        if (index === 0) context.moveTo(midpointX, midpointY)
        context.quadraticCurveTo(next.x, next.y, midpointX, midpointY)
    })
    context.closePath()
}

function drawBackdrop(context, width, height, colorScheme) {
    const isLight = colorScheme === 'light'
    context.fillStyle = isLight ? '#e8edf5' : '#0c0e14'
    context.fillRect(0, 0, width, height)

    const lights = [
        {x: 0.16, y: 0.3, radius: 0.65, color: isLight ? '#6eb6ff' : '#2f6bff', alpha: isLight ? 0.26 : 0.22},
        {x: 0.82, y: 0.7, radius: 0.58, color: isLight ? '#b794ff' : '#7c5cfc', alpha: isLight ? 0.22 : 0.18},
        {x: 0.53, y: 0.08, radius: 0.44, color: isLight ? '#5eead4' : '#13c2c2', alpha: isLight ? 0.13 : 0.1}
    ]

    lights.forEach((light) => {
        const x = width * light.x
        const y = height * light.y
        const radius = Math.max(width, height) * light.radius
        const gradient = context.createRadialGradient(x, y, 0, x, y, radius)
        gradient.addColorStop(0, `${light.color}${Math.round(light.alpha * 255).toString(16).padStart(2, '0')}`)
        gradient.addColorStop(1, 'transparent')
        context.fillStyle = gradient
        context.fillRect(0, 0, width, height)
    })
}

function drawGlassOrb(context, orb, width, height, elapsed, colorScheme) {
    const unit = Math.min(width, height)
    const pulse = 1 + Math.sin(elapsed * orb.pulse + orb.phase) * 0.045
    const radius = unit * orb.radius * pulse
    const x = width * orb.x + Math.sin(elapsed * orb.driftX + orb.phase) * unit * orb.travel
    const y = height * orb.y + Math.cos(elapsed * orb.driftY + orb.phase) * unit * orb.travel
    const rotation = Math.sin(elapsed * 0.18 + orb.phase) * 0.14
    const isLight = colorScheme === 'light'

    context.save()
    context.translate(x, y)
    context.rotate(rotation)
    context.scale(1, orb.squash)
    createBlobPath(context, radius, elapsed * orb.morph + orb.phase)

    const fill = context.createRadialGradient(
        -radius * 0.34,
        -radius * 0.42,
        radius * 0.04,
        radius * 0.05,
        radius * 0.05,
        radius * 1.12
  )
    fill.addColorStop(0, 'rgba(255, 255, 255, 0.46)')
    fill.addColorStop(0.24, rgba(orb.color, isLight ? 0.17 : 0.23))
    fill.addColorStop(0.7, rgba(orb.color, isLight ? 0.09 : 0.14))
    fill.addColorStop(1, rgba(orb.color, isLight ? 0.3 : 0.38))

    context.globalCompositeOperation = isLight ? 'source-over' : 'screen'
    context.shadowColor = rgba(orb.color, isLight ? 0.22 : 0.36)
    context.shadowBlur = radius * 0.28
    context.fillStyle = fill
    context.fill()

    context.shadowBlur = 0
    const rim = context.createLinearGradient(-radius, -radius, radius, radius)
    rim.addColorStop(0, 'rgba(255, 255, 255, 0.74)')
    rim.addColorStop(0.34, rgba(orb.color, 0.18))
    rim.addColorStop(0.68, rgba(orb.color, 0.5))
    rim.addColorStop(1, 'rgba(255, 255, 255, 0.22)')
    context.lineWidth = Math.max(1.2, radius * 0.012)
    context.strokeStyle = rim
    context.stroke()

    context.beginPath()
    context.arc(-radius * 0.12, -radius * 0.16, radius * 0.68, Math.PI * 1.04, Math.PI * 1.53)
    context.lineCap = 'round'
    context.lineWidth = Math.max(1.5, radius * 0.025)
    context.strokeStyle = 'rgba(255, 255, 255, 0.34)'
    context.stroke()

    context.beginPath()
    context.ellipse(radius * 0.18, radius * 0.22, radius * 0.6, radius * 0.48, -0.25, 0.2, 1.34)
    context.lineWidth = Math.max(1, radius * 0.009)
    context.strokeStyle = rgba(orb.color, 0.32)
    context.stroke()
    context.restore()
}

function resizeCanvas(canvas, context) {
    const {width, height} = canvas.getBoundingClientRect()
    const pixelRatio = Math.min(window.devicePixelRatio || 1, MAX_PIXEL_RATIO)
    const renderWidth = Math.max(1, Math.round(width * pixelRatio))
    const renderHeight = Math.max(1, Math.round(height * pixelRatio))

    if (canvas.width !== renderWidth || canvas.height !== renderHeight) {
        canvas.width = renderWidth
        canvas.height = renderHeight
    }
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)

    return {width, height}
}

export default function GlassFluidScene({accentColor = '#1677ff', colorScheme = 'dark'}) {
    const canvasRef = useRef(null)

    useEffect(() => {
        const canvas = canvasRef.current
        const context = canvas?.getContext('2d', {alpha: false})
        if (!canvas || !context) return undefined

        const accent = parseHexColor(accentColor)
        const orbs = [
            {
                color: accent,
                x: 0.59,
                y: 0.5,
                radius: 0.26,
                squash: 1.03,
                phase: 0.4,
                pulse: 0.85,
                driftX: 0.22,
                driftY: 0.16,
                morph: 0.34,
                travel: 0.028
            },
            {
                color: CYAN,
                x: 0.38,
                y: 0.61,
                radius: 0.19,
                squash: 0.94,
                phase: 2.1,
                pulse: 0.72,
                driftX: 0.17,
                driftY: 0.24,
                morph: 0.28,
                travel: 0.024
            },
            {
                color: VIOLET,
                x: 0.51,
                y: 0.35,
                radius: 0.145,
                squash: 1.08,
                phase: 4.2,
                pulse: 1.02,
                driftX: 0.25,
                driftY: 0.19,
                morph: 0.4,
                travel: 0.02
            }
        ]
        let dimensions = resizeCanvas(canvas, context)
        let animationFrame = 0
        let previousFrame = 0
        let startedAt = performance.now()

        const draw = (timestamp) => {
            animationFrame = requestAnimationFrame(draw)
            if (document.hidden || timestamp - previousFrame < FRAME_INTERVAL) return
            previousFrame = timestamp

            const elapsed = (timestamp - startedAt) / 1000
            drawBackdrop(context, dimensions.width, dimensions.height, colorScheme)
            orbs.forEach((orb) => drawGlassOrb(context, orb, dimensions.width, dimensions.height, elapsed, colorScheme))
        }

        const handleResize = () => {
            dimensions = resizeCanvas(canvas, context)
        }
        const handleVisibilityChange = () => {
            if (!document.hidden) startedAt += performance.now() - previousFrame
        }
        const resizeObserver = typeof ResizeObserver === 'undefined'
            ? null
            : new ResizeObserver(handleResize)

        resizeObserver?.observe(canvas)
        window.addEventListener('resize', handleResize, {passive: true})
        document.addEventListener('visibilitychange', handleVisibilityChange)
        animationFrame = requestAnimationFrame(draw)

        return () => {
            cancelAnimationFrame(animationFrame)
            resizeObserver?.disconnect()
            window.removeEventListener('resize', handleResize)
            document.removeEventListener('visibilitychange', handleVisibilityChange)
        }
    }, [accentColor, colorScheme])

    return <canvas ref={canvasRef} className="glass-fluid-canvas"/>
}
