import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {expect, test, vi} from 'vitest'
import AppearanceModeSelector from './AppearanceModeSelector'

test('selects an Apple-style color mode option', async () => {
  const onChange = vi.fn()

  render(<AppearanceModeSelector value="dark" onChange={onChange} />)

  expect(screen.getByRole('radio', { name: '深色' })).toBeChecked()
  await userEvent.click(screen.getByRole('radio', { name: '跟随系统' }))

  expect(onChange).toHaveBeenCalledWith('system')
})
