import { useEffect, useState, type FormEvent } from 'react'
import { getOverstockProducts } from '../../api/overstockProducts'
import { ApiError } from '../../api/http'
import type { OverstockProduct, OverstockSortBy } from '../../api/types'
import { STORES } from '../../data/stores'
import '../../shared/list-page.css'

const SORT_OPTIONS: { value: OverstockSortBy; label: string }[] = [
  { value: 'IMMOBILIZED_VALUE', label: 'Valor inmovilizado' },
  { value: 'DAYS_OF_COVERAGE', label: 'Días de cobertura' },
]

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

interface Filters {
  storeId: number
  referenceDate: string
  sortBy: OverstockSortBy
}

interface OverstockPageProps {
  onSelectProduct?: (productId: number, storeId: number) => void
}

export function OverstockPage({ onSelectProduct }: OverstockPageProps) {
  const [storeId, setStoreId] = useState(STORES[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [sortBy, setSortBy] = useState<OverstockSortBy>('IMMOBILIZED_VALUE')
  const [products, setProducts] = useState<OverstockProduct[] | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: OverstockProduct[] | null, errorMessage: string | null) => {
    setProducts(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    referenceDate: filters.referenceDate,
    sortBy: filters.sortBy,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getOverstockProducts(toApiParams({ storeId, referenceDate, sortBy }))
      .then((result) => {
        if (!cancelled) applyResult(result, null)
      })
      .catch((err) => {
        if (!cancelled) {
          applyResult(null, err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
        }
      })

    return () => {
      cancelled = true
    }
    // Solo la carga inicial: búsquedas siguientes las dispara handleSubmit.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError(null)
    getOverstockProducts(toApiParams({ storeId, referenceDate, sortBy }))
      .then((result) => applyResult(result, null))
      .catch((err) => {
        applyResult(null, err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
      })
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Sobrestock</h1>
        <p>Productos con cobertura excesiva que inmovilizan capital, candidatos a liquidación o promoción.</p>
      </header>

      <form className="filters" onSubmit={handleSubmit}>
        <label>
          Sucursal
          <select value={storeId} onChange={(event) => setStoreId(Number(event.target.value))}>
            {STORES.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Fecha de referencia
          <input
            type="date"
            value={referenceDate}
            onChange={(event) => setReferenceDate(event.target.value)}
            required
          />
        </label>

        <label>
          Ordenar por
          <select value={sortBy} onChange={(event) => setSortBy(event.target.value as OverstockSortBy)}>
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Buscando…' : 'Buscar'}
        </button>
      </form>

      {error && (
        <p className="state state-error" role="alert">
          {error}
        </p>
      )}

      {!error && !loading && products && products.length === 0 && (
        <p className="state state-empty">No hay productos en sobrestock para esta sucursal en la fecha seleccionada.</p>
      )}

      {products && products.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Producto</th>
              <th>Categoría</th>
              <th>Stock actual</th>
              <th>Cobertura (días)</th>
              <th>Valor inmovilizado</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.productId}>
                <td>
                  {onSelectProduct ? (
                    <button
                      type="button"
                      className="link-button"
                      onClick={() => onSelectProduct(product.productId, product.storeId)}
                    >
                      {product.sku}
                    </button>
                  ) : (
                    product.sku
                  )}
                </td>
                <td>{product.productName}</td>
                <td>#{product.categoryId}</td>
                <td>{product.currentStock}</td>
                <td>{product.currentDaysOfCoverage.toFixed(1)}</td>
                <td>{currencyFormatter.format(product.immobilizedValue)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}
