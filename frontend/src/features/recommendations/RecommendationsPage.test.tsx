import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { RecommendationsPage } from './RecommendationsPage'
import { getRecommendations, registerRecommendationFeedback } from '../../api/recommendations'
import { showSuccess, showError } from '../../shared/toast'
import { ApiError } from '../../api/http'
import type { Recommendation, Store } from '../../api/types'

vi.mock('../../api/recommendations', () => ({
  getRecommendations: vi.fn(),
  recalculateRecommendations: vi.fn(),
  registerRecommendationFeedback: vi.fn(),
}))
vi.mock('../../shared/toast', () => ({ showSuccess: vi.fn(), showError: vi.fn() }))

const getRecommendationsMock = vi.mocked(getRecommendations)
const registerFeedbackMock = vi.mocked(registerRecommendationFeedback)

const STORES: Store[] = [{ id: 1, name: 'Sucursal Centro' }]

const recommendation = (overrides: Partial<Recommendation> = {}): Recommendation => ({
  recommendationId: 500,
  productId: 1001,
  sku: 'LEC-1001',
  productName: 'Leche Entera 1L',
  storeId: 1,
  categoryId: 2,
  supplierId: 1,
  suggestedQuantity: 120,
  orderDeadlineDate: '2026-08-05',
  justification: 'Stock por debajo del punto de pedido',
  status: 'PENDING',
  generationDate: '2026-08-01',
  feedbackComment: null,
  feedbackDate: null,
  ...overrides,
})

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<RecommendationsPage stores={STORES} />, { wrapper })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('RecommendationsPage', () => {
  it('ofrece aplicar o descartar solo mientras la recomendación está pendiente', async () => {
    getRecommendationsMock.mockResolvedValue([
      recommendation({ recommendationId: 500, sku: 'LEC-1001' }),
      recommendation({ recommendationId: 501, sku: 'YOG-1042', status: 'APPLIED', feedbackDate: '2026-08-02' }),
    ])

    renderPage()

    const pendingRow = (await screen.findByText('LEC-1001')).closest('tr')!
    expect(within(pendingRow).getByRole('button', { name: 'Aplicar' })).toBeInTheDocument()
    expect(within(pendingRow).getByRole('button', { name: 'Descartar' })).toBeInTheDocument()

    // Una ya resuelta muestra la fecha del feedback en lugar de los botones.
    const resolvedRow = screen.getByText('YOG-1042').closest('tr')!
    expect(within(resolvedRow).queryByRole('button', { name: 'Aplicar' })).not.toBeInTheDocument()
    expect(within(resolvedRow).getByText('2026-08-02')).toBeInTheDocument()
  })

  it('manda APPLIED con el id de la fila en la que se hizo clic', async () => {
    getRecommendationsMock.mockResolvedValue([
      recommendation({ recommendationId: 500, sku: 'LEC-1001' }),
      recommendation({ recommendationId: 777, sku: 'YOG-1042' }),
    ])
    registerFeedbackMock.mockResolvedValue(recommendation({ recommendationId: 777, status: 'APPLIED' }))

    renderPage()
    const targetRow = (await screen.findByText('YOG-1042')).closest('tr')!
    await userEvent.click(within(targetRow).getByRole('button', { name: 'Aplicar' }))

    expect(registerFeedbackMock).toHaveBeenCalledWith(777, 'APPLIED')
  })

  it('manda DISCARDED al descartar', async () => {
    getRecommendationsMock.mockResolvedValue([recommendation()])
    registerFeedbackMock.mockResolvedValue(recommendation({ status: 'DISCARDED' }))

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Descartar' }))

    expect(registerFeedbackMock).toHaveBeenCalledWith(500, 'DISCARDED')
  })

  /**
   * El feedback no refetchea: parchea la fila en la caché con lo que
   * devolvió el backend. Si eso se rompiera, la fila seguiría ofreciendo
   * "Aplicar" después de haberla aplicado.
   */
  it('actualiza la fila con la respuesta del backend sin volver a consultar', async () => {
    getRecommendationsMock.mockResolvedValue([recommendation()])
    registerFeedbackMock.mockResolvedValue(
      recommendation({ status: 'APPLIED', feedbackDate: '2026-08-03' }),
    )

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Aplicar' }))

    expect(await screen.findByText('2026-08-03')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aplicar' })).not.toBeInTheDocument()
    expect(getRecommendationsMock).toHaveBeenCalledTimes(1)
    expect(showSuccess).toHaveBeenCalledWith('Recomendación marcada como aplicada.')
  })

  it('avisa por toast y deja la fila como estaba si el feedback falla', async () => {
    getRecommendationsMock.mockResolvedValue([recommendation()])
    registerFeedbackMock.mockRejectedValue(
      new ApiError('Recommendation not found: 500', 404, undefined, 'RECOMMENDATION_NOT_FOUND', 500),
    )

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Aplicar' }))

    // El toast lleva el mensaje traducido, no el del backend en inglés.
    await vi.waitFor(() => expect(showError).toHaveBeenCalledWith('No se encontró la recomendación 500.'))
    expect(screen.getByRole('button', { name: 'Aplicar' })).toBeInTheDocument()
  })
})
