import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { ProductDetailPage } from './ProductDetailPage'
import { getProductForecast } from '../../api/forecast'
import { ApiError } from '../../api/http'
import type { ProductForecast, Store } from '../../api/types'

vi.mock('../../api/forecast', () => ({ getProductForecast: vi.fn() }))

const getProductForecastMock = vi.mocked(getProductForecast)

const STORES: Store[] = [
  { id: 1, name: 'Sucursal Centro' },
  { id: 2, name: 'Sucursal Norte' },
]

const forecast = (overrides: Partial<ProductForecast> = {}): ProductForecast => ({
  productId: 1001,
  sku: 'LEC-1001',
  productName: 'Leche Entera 1L',
  storeId: 1,
  baseAds: 43.2,
  periods: [
    { periodStart: '2026-08-01', periodEnd: '2026-08-07', seasonalIndex: 1.2, projectedDailyAds: 49.7, projectedTotalDemand: 348 },
  ],
  ...overrides,
})

function renderPage(initialSelection: Parameters<typeof ProductDetailPage>[0]['initialSelection'] = null) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<ProductDetailPage stores={STORES} initialSelection={initialSelection} />, { wrapper })
}

const productIdField = () => screen.getByRole('spinbutton', { name: /id de producto/i })
const searchButton = () => screen.getByRole('button', { name: /buscar/i })

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProductDetailPage', () => {
  it('no consulta nada hasta que se pide un producto', async () => {
    renderPage()

    expect(getProductForecastMock).not.toHaveBeenCalled()
    expect(screen.getByText(/Ingresá un ID de producto/)).toBeInTheDocument()
  })

  /** Llega con producto y sucursal elegidos desde otra pantalla (por ejemplo, el SKU de Sobrestock). */
  it('consulta de entrada cuando llega con una selección previa', async () => {
    getProductForecastMock.mockResolvedValue(forecast())

    renderPage({ productId: 1001, storeId: 2 })

    expect(await screen.findByText(/LEC-1001/)).toBeInTheDocument()
    expect(getProductForecastMock).toHaveBeenCalledWith(
      expect.objectContaining({ productId: 1001, storeId: 2 }),
    )
  })

  it('proyecta los períodos con su índice estacional y demanda', async () => {
    getProductForecastMock.mockResolvedValue(forecast())

    renderPage({ productId: 1001, storeId: 1 })
    await screen.findByText(/LEC-1001/)

    expect(screen.getByText(/Venta promedio diaria base: 43,2 unidades\/día/)).toBeInTheDocument()
    const row = screen.getByText('2026-08-01').closest('tr')!
    expect(row).toHaveTextContent('1,2')
    expect(row).toHaveTextContent('49,7')
    expect(row).toHaveTextContent('348')
  })

  /**
   * Un producto que existe pero sin historial suficiente no es un error:
   * el backend responde 200 con baseAds en null, y hay que explicarlo en
   * vez de mostrar una tabla vacía.
   */
  it('explica que el producto existe pero no tiene historial para proyectar', async () => {
    getProductForecastMock.mockResolvedValue(forecast({ baseAds: null, periods: [] }))

    renderPage({ productId: 1001, storeId: 1 })

    expect(await screen.findByText(/no tiene historial de ventas suficiente/)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    // El producto se identifica igual, para que se vea cuál fue.
    expect(screen.getByText(/LEC-1001/)).toBeInTheDocument()
  })

  /**
   * El input declara required y min=1, y al no declarar step toma step=1,
   * así que el navegador ya rechaza vacío, 0, negativos y decimales: el
   * formulario no se envía y no se consulta nada.
   *
   * Consecuencia que conviene tener presente: la validación a mano del
   * componente (`Number.isInteger(...) || <= 0`, y la precedencia
   * `validationError ?? getErrorMessage(error)`) no llega a ejecutarse
   * nunca desde la UI, y su mensaje en español no se ve — lo que se ve es
   * el cartel nativo del navegador.
   */
  it.each([
    ['un decimal', '1.5'],
    ['cero', '0'],
    ['un negativo', '-3'],
  ])('no consulta al backend con %s como ID de producto', async (_caso, valor) => {
    renderPage()

    await userEvent.type(productIdField(), valor)
    await userEvent.click(searchButton())

    expect(getProductForecastMock).not.toHaveBeenCalled()
    expect(screen.getByText(/Ingresá un ID de producto/)).toBeInTheDocument()
  })

  it('traduce al español el error del backend cuando el producto no existe', async () => {
    getProductForecastMock.mockRejectedValue(
      new ApiError('Product not found: 9999', 404, undefined, 'PRODUCT_NOT_FOUND', 9999),
    )

    renderPage()
    await userEvent.type(productIdField(), '9999')
    await userEvent.click(searchButton())

    expect(await screen.findByText('No se encontró el producto 9999.')).toBeInTheDocument()
    expect(screen.queryByText(/Product not found/)).not.toBeInTheDocument()
  })
})
