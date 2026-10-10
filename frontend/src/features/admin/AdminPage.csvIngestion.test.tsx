import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { CsvIngestionSection } from './AdminPage'
import { ingestCsvFile, CsvIngestionThresholdError } from '../../api/csvIngestion'

vi.mock('../../api/csvIngestion', async () => {
  const actual = await vi.importActual<typeof import('../../api/csvIngestion')>('../../api/csvIngestion')
  return { ...actual, ingestCsvFile: vi.fn() }
})
vi.mock('../../shared/toast', () => ({ showSuccess: vi.fn(), showError: vi.fn() }))

const ingestCsvFileMock = vi.mocked(ingestCsvFile)

function renderSection() {
  // retry en false: si no, un test de error espera los reintentos de React Query.
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<CsvIngestionSection />, { wrapper })
}

function fileInput(): HTMLInputElement {
  const input = document.querySelector('input[type="file"]')
  if (!input) throw new Error('No se encontró el input de archivo')
  return input as HTMLInputElement
}

async function selectFileType(label: 'Ventas' | 'Inventario') {
  await userEvent.click(screen.getByRole('combobox', { name: /tipo de archivo/i }))
  await userEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: label }))
}

const csv = (name: string) => new File(['contenido'], name, { type: 'text/csv' })

beforeEach(() => {
  vi.clearAllMocks()
})

describe('CsvIngestionSection', () => {
  it('arranca en ventas y anuncia las columnas de ventas', () => {
    renderSection()

    expect(screen.getByRole('combobox', { name: /tipo de archivo/i })).toHaveTextContent('Ventas')
    expect(screen.getByText(/venta_id, fecha, producto_id/)).toBeInTheDocument()
    expect(screen.getByText(/Elegir archivo \(ventas\.csv\)/)).toBeInTheDocument()
  })

  it('al elegir inventario cambia las columnas esperadas y el archivo sugerido', async () => {
    renderSection()

    await selectFileType('Inventario')

    expect(screen.getByText(/fecha_snapshot, producto_id, sucursal_id, stock_actual, stock_en_transito/)).toBeInTheDocument()
    expect(screen.getByText(/Elegir archivo \(inventario\.csv\)/)).toBeInTheDocument()
    // La asimetría con ventas que el backend decide: el id lo asigna el sistema.
    expect(screen.getByText(/el inventario_id lo asigna el sistema/)).toBeInTheDocument()
  })

  it('envía el fileType elegido junto con el archivo', async () => {
    ingestCsvFileMock.mockResolvedValue({ totalRowsRead: 2, acceptedCount: 2, rejectedCount: 0, rejections: [] })
    renderSection()

    await selectFileType('Inventario')
    await userEvent.upload(fileInput(), csv('inventario.csv'))
    await userEvent.click(screen.getByRole('button', { name: /subir archivo/i }))

    expect(ingestCsvFileMock).toHaveBeenCalledOnce()
    expect(ingestCsvFileMock.mock.calls[0][0]).toBe('INVENTORY')
    expect(ingestCsvFileMock.mock.calls[0][1].name).toBe('inventario.csv')
    expect(await screen.findByText(/2 de 2 filas aceptadas/)).toBeInTheDocument()
  })

  it('no llama al backend si no se eligió archivo', async () => {
    renderSection()

    await userEvent.click(screen.getByRole('button', { name: /subir archivo/i }))

    expect(ingestCsvFileMock).not.toHaveBeenCalled()
    expect(screen.getByText('Elegí un archivo CSV.')).toBeInTheDocument()
  })

  it('muestra el detalle de las filas rechazadas cuando se supera el umbral', async () => {
    ingestCsvFileMock.mockRejectedValue(
      new CsvIngestionThresholdError('backend message', 2, 1, 50, [
        { rowNumber: 1, reason: 'producto_id 9999 no existe en el catálogo' },
      ]),
    )
    renderSection()

    await userEvent.upload(fileInput(), csv('ventas.csv'))
    await userEvent.click(screen.getByRole('button', { name: /subir archivo/i }))

    // El mensaje se arma en español con los campos del error, no con el texto del backend.
    expect(await screen.findByText(/Se rechazaron 1 de 2 filas \(50\.0%\)/)).toBeInTheDocument()
    expect(screen.queryByText(/backend message/)).not.toBeInTheDocument()
    expect(screen.getByText('producto_id 9999 no existe en el catálogo')).toBeInTheDocument()
  })

  /**
   * El reset al cambiar de tipo: las columnas esperadas son otras, así que
   * dejar a la vista el archivo y el resumen del lote anterior confunde.
   */
  it('cambiar de tipo descarta el archivo elegido y el resultado anterior', async () => {
    ingestCsvFileMock.mockResolvedValue({ totalRowsRead: 2, acceptedCount: 2, rejectedCount: 0, rejections: [] })
    renderSection()

    await userEvent.upload(fileInput(), csv('ventas.csv'))
    await userEvent.click(screen.getByRole('button', { name: /subir archivo/i }))
    expect(await screen.findByText(/2 de 2 filas aceptadas/)).toBeInTheDocument()

    await selectFileType('Inventario')

    expect(screen.queryByText(/2 de 2 filas aceptadas/)).not.toBeInTheDocument()
    expect(screen.getByText(/Elegir archivo \(inventario\.csv\)/)).toBeInTheDocument()
    expect(screen.queryByText('ventas.csv')).not.toBeInTheDocument()
  })
})
