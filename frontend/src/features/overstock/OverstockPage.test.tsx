import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { OverstockPage } from './OverstockPage'
import { getOverstockProducts } from '../../api/overstockProducts'
import { ApiError } from '../../api/http'
import type { Category, OverstockProduct, Store } from '../../api/types'

vi.mock('../../api/overstockProducts', () => ({ getOverstockProducts: vi.fn() }))

const getOverstockProductsMock = vi.mocked(getOverstockProducts)

const STORES: Store[] = [
  { id: 1, name: 'Sucursal Centro' },
  { id: 2, name: 'Sucursal Norte' },
]

const CATEGORIES: Category[] = [
  { categoryId: 2, name: 'Lácteos', parentCategoryId: 1, maxCoverageDaysThreshold: 12, defaultExtraCoverageDays: 3 },
]

const product = (overrides: Partial<OverstockProduct> = {}): OverstockProduct => ({
  productId: 1001,
  sku: 'LEC-1001',
  productName: 'Leche Entera 1L',
  storeId: 1,
  categoryId: 2,
  currentStock: 357,
  currentDaysOfCoverage: 23.456,
  immobilizedValue: 163640.09,
  ...overrides,
})

function renderPage(props: Partial<Parameters<typeof OverstockPage>[0]> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<OverstockPage stores={STORES} categories={CATEGORIES} {...props} />, { wrapper })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('OverstockPage', () => {
  it('consulta la primera sucursal al montar', async () => {
    getOverstockProductsMock.mockResolvedValue([product()])

    renderPage()

    expect(await screen.findByText('LEC-1001')).toBeInTheDocument()
    expect(getOverstockProductsMock).toHaveBeenCalledWith({
      storeId: 1,
      referenceDate: '2026-08-01',
      sortBy: 'IMMOBILIZED_VALUE',
    })
  })

  /**
   * Los filtros se aplican al enviar, no al tipear: la pantalla mantiene el
   * estado del formulario separado del que dispara la consulta. Sin esto,
   * cada tecla en la fecha generaría un request.
   */
  it('no vuelve a consultar hasta que se envía el formulario', async () => {
    getOverstockProductsMock.mockResolvedValue([])
    renderPage()
    await screen.findByText(/No hay productos en sobrestock/)
    expect(getOverstockProductsMock).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByRole('combobox', { name: /sucursal/i }))
    await userEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: 'Sucursal Norte' }))

    expect(getOverstockProductsMock).toHaveBeenCalledTimes(1)

    await userEvent.click(screen.getByRole('button', { name: /buscar/i }))

    expect(getOverstockProductsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({ storeId: 2, sortBy: 'IMMOBILIZED_VALUE' }),
    )
  })

  it('envía el criterio de orden elegido', async () => {
    getOverstockProductsMock.mockResolvedValue([])
    renderPage()
    await screen.findByText(/No hay productos en sobrestock/)

    await userEvent.click(screen.getByRole('combobox', { name: /ordenar por/i }))
    await userEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: 'Días de cobertura' }))
    await userEvent.click(screen.getByRole('button', { name: /buscar/i }))

    expect(getOverstockProductsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({ sortBy: 'DAYS_OF_COVERAGE' }),
    )
  })

  it('resuelve el nombre de la categoría y formatea cobertura e importe', async () => {
    getOverstockProductsMock.mockResolvedValue([product()])

    renderPage()

    const row = (await screen.findByText('LEC-1001')).closest('tr')!
    expect(within(row).getByText('Lácteos')).toBeInTheDocument()
    expect(within(row).getByText('23.5')).toBeInTheDocument() // un decimal
    expect(within(row).getByText(/163\.640,09/)).toBeInTheDocument() // moneda es-AR
  })

  it('muestra el vacío cuando no hay productos en sobrestock', async () => {
    getOverstockProductsMock.mockResolvedValue([])

    renderPage()

    expect(await screen.findByText(/No hay productos en sobrestock/)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  /** El error pasa por getErrorMessage, así que un código del backend llega traducido. */
  it('traduce al español el error que devuelve el backend', async () => {
    getOverstockProductsMock.mockRejectedValue(
      new ApiError('Store not found: 9999', 404, undefined, 'STORE_NOT_FOUND', 9999),
    )

    renderPage()

    expect(await screen.findByText('No se encontró la sucursal 9999.')).toBeInTheDocument()
    expect(screen.queryByText(/Store not found/)).not.toBeInTheDocument()
  })

  it('avisa qué producto y sucursal se eligió al hacer clic en el SKU', async () => {
    getOverstockProductsMock.mockResolvedValue([product({ productId: 1042, storeId: 2, sku: 'YOG-1042' })])
    const onSelectProduct = vi.fn()

    renderPage({ onSelectProduct })
    await userEvent.click(await screen.findByRole('button', { name: 'YOG-1042' }))

    expect(onSelectProduct).toHaveBeenCalledWith(1042, 2)
  })

  it('muestra el SKU como texto plano cuando no hay a dónde navegar', async () => {
    getOverstockProductsMock.mockResolvedValue([product({ sku: 'YOG-1042' })])

    renderPage({ onSelectProduct: undefined })

    expect(await screen.findByText('YOG-1042')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'YOG-1042' })).not.toBeInTheDocument()
  })
})
