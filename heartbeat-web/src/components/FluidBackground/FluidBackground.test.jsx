import {render, screen} from '@testing-library/react'
import {expect, test, vi} from 'vitest'
import FluidBackground from './FluidBackground'

vi.mock('./GlassFluidScene', () => ({
  default: () => <div data-testid="glass-fluid-canvas-mock" />
}))

test('mounts the decorative fluid layer only while the effect is enabled', () => {
  const { rerender } = render(<FluidBackground enabled visualStyle="flat" />)

  expect(screen.getByTestId('fluid-background')).toHaveAttribute('aria-hidden', 'true')
  expect(screen.queryByTestId('glass-fluid-scene')).not.toBeInTheDocument()

  rerender(<FluidBackground enabled={false} visualStyle="glass" />)

  expect(screen.queryByTestId('fluid-background')).not.toBeInTheDocument()
})

test('uses the WebGL glass scene when the glass visual style is selected', async () => {
  render(<FluidBackground enabled visualStyle="glass" accentColor="#7c5cfc" colorScheme="dark" />)

  expect(await screen.findByTestId('glass-fluid-scene')).toBeInTheDocument()
  expect(await screen.findByTestId('glass-fluid-canvas-mock')).toBeInTheDocument()
})
