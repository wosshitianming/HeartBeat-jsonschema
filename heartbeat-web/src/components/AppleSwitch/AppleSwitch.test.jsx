import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {expect, test, vi} from 'vitest'
import AppleSwitch from './AppleSwitch'

test('reports the next checked state through an accessible switch control', async () => {
  const onChange = vi.fn()

  render(
      <AppleSwitch
          checked={false}
          label="流体特效"
          onChange={onChange}
      />
  )

  const control = screen.getByRole('switch', { name: '流体特效' })
  expect(control).not.toBeChecked()

  await userEvent.click(control)

  expect(onChange).toHaveBeenCalledWith(true)
})
