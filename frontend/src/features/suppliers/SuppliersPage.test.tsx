import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { SuppliersPage } from './SuppliersPage'
import { getSuppliers, updateSupplierLeadTime } from '../../api/suppliers'
import { showError, showSuccess } from '../../shared/toast'
import { ApiError } from '../../api/http'
import type { Supplier } from '../../api/types'

vi.mock('../../api/suppliers', () => ({ getSuppliers: vi.fn(), updateSupplierLeadTime: vi.fn() }))
vi.mock('../../shared/toast', () => ({ showSuccess: vi.fn(), showError: vi.fn() }))

const getSuppliersMock = vi.mocked(getSuppliers)
const updateLeadTimeMock = vi.mocked(updateSupplierLeadTime)

const supplier = (overrides: Partial<Supplier> = {}): Supplier => ({
  supplierId: 1,
  businessName: 'PIANCHU SRL',
  leadTimeDays: 4,
  paymentTerms: '30 días',
  ...overrides,
})

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return render(<SuppliersPage />, { wrapper })
}

async function startEditing(businessName: string) {
  const row = (await screen.findByText(businessName)).closest('tr')!
  await userEvent.click(within(row).getByRole('button', { name: /editar lead time/i }))
  return row
}

function leadTimeField(): HTMLInputElement {
  return screen.getByRole('spinbutton') as HTMLInputElement
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('SuppliersPage', () => {
  it('edita una fila por vez, partiendo del lead time actual', async () => {
    getSuppliersMock.mockResolvedValue([supplier(), supplier({ supplierId: 2, businessName: 'OTRO SA', leadTimeDays: 9 })])

    renderPage()
    await startEditing('PIANCHU SRL')

    expect(leadTimeField()).toHaveValue(4)
    // La otra fila sigue en modo lectura.
    const otherRow = screen.getByText('OTRO SA').closest('tr')!
    expect(within(otherRow).getByRole('button', { name: /editar lead time/i })).toBeInTheDocument()
  })

  it('guarda el nuevo lead time y actualiza la fila con lo que devolvió el backend', async () => {
    getSuppliersMock.mockResolvedValue([supplier()])
    updateLeadTimeMock.mockResolvedValue(supplier({ leadTimeDays: 7 }))

    renderPage()
    await startEditing('PIANCHU SRL')
    await userEvent.clear(leadTimeField())
    await userEvent.type(leadTimeField(), '7')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(updateLeadTimeMock).toHaveBeenCalledWith(1, 7)
    expect(await screen.findByRole('button', { name: /editar lead time/i })).toBeInTheDocument()
    expect(showSuccess).toHaveBeenCalledWith('Lead time de "PIANCHU SRL" actualizado.')
    // Sin refetch: la caché se parchea con la respuesta.
    expect(getSuppliersMock).toHaveBeenCalledTimes(1)
  })

  /**
   * La validación de yup corre antes de llamar al backend. Es la guarda que
   * hace que el "Lead time must be greater than 0 days" del dominio sea
   * inalcanzable desde la UI, así que conviene que quede fijada.
   */
  it('no llama al backend con un lead time inválido', async () => {
    getSuppliersMock.mockResolvedValue([supplier()])

    renderPage()
    await startEditing('PIANCHU SRL')
    await userEvent.clear(leadTimeField())
    await userEvent.type(leadTimeField(), '0')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Ingresá un número entero mayor a 0.')).toBeInTheDocument()
    expect(updateLeadTimeMock).not.toHaveBeenCalled()
  })

  it('tampoco llama al backend con el campo vacío', async () => {
    getSuppliersMock.mockResolvedValue([supplier()])

    renderPage()
    await startEditing('PIANCHU SRL')
    await userEvent.clear(leadTimeField())
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Ingresá un número entero mayor a 0.')).toBeInTheDocument()
    expect(updateLeadTimeMock).not.toHaveBeenCalled()
  })

  it('cancelar deja la fila como estaba', async () => {
    getSuppliersMock.mockResolvedValue([supplier()])

    renderPage()
    await startEditing('PIANCHU SRL')
    await userEvent.clear(leadTimeField())
    await userEvent.type(leadTimeField(), '99')
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(updateLeadTimeMock).not.toHaveBeenCalled()
    const row = screen.getByText('PIANCHU SRL').closest('tr')!
    expect(within(row).getByText('4')).toBeInTheDocument()
  })

  /**
   * El error de la mutación va al toast, no a la fila: el inline queda
   * reservado para la validación de cliente, que sí apunta a un campo.
   */
  it('avisa por toast con el mensaje traducido si el guardado falla', async () => {
    getSuppliersMock.mockResolvedValue([supplier()])
    updateLeadTimeMock.mockRejectedValue(
      new ApiError('Supplier not found: 1', 404, undefined, 'SUPPLIER_NOT_FOUND', 1),
    )

    renderPage()
    await startEditing('PIANCHU SRL')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    await vi.waitFor(() => expect(showError).toHaveBeenCalledWith('No se encontró el proveedor 1.'))
    expect(screen.queryByText('Supplier not found: 1')).not.toBeInTheDocument()
    // Sigue en edición, con lo tipeado, para poder reintentar.
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeInTheDocument()
  })
})
