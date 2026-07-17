import {render, screen, waitFor} from '@testing-library/react'
import {afterEach, expect, test, vi} from 'vitest'
import {MemoryRouter} from 'react-router-dom'
import DashboardPage from './DashboardPage'

afterEach(() => {
    vi.restoreAllMocks()
})

test('loads and presents the authorized flow operations summary', async () => {
    const summary = {
        activeFlows: 8,
        publishedFlows: 10,
        totalRuns: 1284,
        runningRuns: 4,
        waitingRuns: 6,
        successRuns: 1000,
        failedRuns: 15,
        canceledRuns: 3,
        averageDurationMs: 1850,
        successRate: 98.52
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
        ok: true,
        status: 200,
        text: async () => JSON.stringify({code: '0', msg: 'success', data: summary})
    })

    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <DashboardPage currentUser={{
                id: '1',
                username: 'admin',
                nickname: '超级管理员',
                tenantId: '1',
                permissions: ['flow:studio:list']
            }}/>
        </MemoryRouter>
    )

    expect(screen.getByRole('heading', {name: /欢迎回来，超级管理员/})).toBeInTheDocument()
    expect(await screen.findByText('1,284')).toBeInTheDocument()
    expect(screen.getAllByText('98.5%')).toHaveLength(2)
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/flows/runs/summary',
        expect.objectContaining({method: 'POST'})
    ))
})
