import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { HomePage } from './HomePage'
import { getInventoryKpis } from '../../api/kpis'
import { ApiError } from '../../api/http'
import type { InventoryKpis, Store } from '../../api/types'

vi.mock('../../api/kpis', () => ({ getInventoryKpis: vi.fn() }))

const getInventoryKpisMock = vi.mocked(getInventoryKpis)

const STORES: Store[] = [
  { id: 1, name: 'Sucursal Centro' },
  { id: 2, name: 'Sucursal Norte' },
]

const kpis = (overrides: Partial<InventoryKpis> = {}): InventoryKpis => ({
  stockoutRate: 6.9,
  averageDaysOfCoverage: 8.4,
  immobilizedOverstockValue: 3168274.08,
  recommendationsFollowedRate: 42.5,
  inventoryTurnover: 9.4,
  ...overrides,
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<HomePage stores={STORES} />, { wrapper })
}

/**
 * El valor de una tarjeta, buscado a partir de su título. Es el único <p>
 * de la tarjeta: el título es un heading y la ayuda un span (MUI mapea
 * subtitle2 a h6 y caption a span).
 */
function cardValue(title: string): string {
  const card = screen.getByText(title).closest('.MuiCard-root')
  if (!card) throw new Error(`No se encontró la tarjeta "${title}"`)
  const value = card.querySelector('p')
  if (!value) throw new Error(`La tarjeta "${title}" no tiene valor`)
  return value.textContent ?? ''
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('HomePage', () => {
  it('consulta la primera sucursal y la ventana por defecto al montar', async () => {
    getInventoryKpisMock.mockResolvedValue(kpis())

    renderPage()
    await screen.findByText('Tasa de quiebre de stock')

    expect(getInventoryKpisMock).toHaveBeenCalledWith({
      storeId: 1,
      fromDate: '2026-05-04',
      toDate: '2026-08-01',
    })
  })

  it('formatea cada indicador con su unidad', async () => {
    getInventoryKpisMock.mockResolvedValue(kpis())

    renderPage()
    await screen.findByText('Tasa de quiebre de stock')

    expect(cardValue('Tasa de quiebre de stock')).toBe('6,9%')
    expect(cardValue('Cobertura promedio')).toBe('8,4 días')
    expect(cardValue('Rotación de inventario')).toBe('9,4x')
    expect(cardValue('Capital inmovilizado en sobrestock')).toMatch(/3\.168\.274,08/)
  })

  /**
   * Un KPI en null significa "no hay datos suficientes en el período", que
   * no es lo mismo que cero: mostrar "0%" afirmaría que no hubo quiebres
   * de stock cuando en realidad no se pudo calcular.
   */
  it('distingue un indicador sin datos de uno en cero', async () => {
    getInventoryKpisMock.mockResolvedValue(
      kpis({ stockoutRate: 0, recommendationsFollowedRate: null, inventoryTurnover: null, averageDaysOfCoverage: null }),
    )

    renderPage()
    await screen.findByText('Tasa de quiebre de stock')

    expect(cardValue('Tasa de quiebre de stock')).toBe('0%')
    expect(cardValue('Recomendaciones seguidas')).toBe('Sin datos')
    expect(cardValue('Rotación de inventario')).toBe('Sin datos')
    expect(cardValue('Cobertura promedio')).toBe('Sin datos')
  })

  it('no vuelve a consultar hasta que se envía el formulario', async () => {
    getInventoryKpisMock.mockResolvedValue(kpis())
    renderPage()
    await screen.findByText('Tasa de quiebre de stock')
    expect(getInventoryKpisMock).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByRole('combobox', { name: /sucursal/i }))
    await userEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: 'Sucursal Norte' }))

    expect(getInventoryKpisMock).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByRole('button', { name: /calcular/i }))

    expect(getInventoryKpisMock).toHaveBeenLastCalledWith(expect.objectContaining({ storeId: 2 }))
  })

  /**
   * Esta pantalla es la única donde un usuario puede provocar un 400 del
   * backend: "Desde" y "Hasta" no tienen restricción cruzada, así que se
   * puede pedir un rango invertido. El mensaje tiene que llegar traducido.
   */
  it('traduce al español el rechazo de un rango de fechas invertido', async () => {
    getInventoryKpisMock.mockRejectedValue(
      new ApiError('fromDate cannot be after toDate', 400, undefined, 'DATE_RANGE_INVALID'),
    )

    renderPage()

    expect(
      await screen.findByText('La fecha "Desde" no puede ser posterior a la fecha "Hasta".'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/fromDate cannot be after/)).not.toBeInTheDocument()
  })

  it('no muestra tarjetas mientras el período no se pudo calcular', async () => {
    getInventoryKpisMock.mockRejectedValue(new ApiError('boom', 500))

    renderPage()
    await screen.findByText(/boom/)

    expect(screen.queryByText('Tasa de quiebre de stock')).not.toBeInTheDocument()
  })
})
