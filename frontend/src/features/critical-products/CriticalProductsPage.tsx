import { useEffect, useState, type FormEvent } from 'react'
import { getCriticalProducts } from '../../api/criticalProducts'
import { ApiError } from '../../api/http'
import type { CriticalProduct } from '../../api/types'
import { STORES } from '../../data/stores'
import { STATUS_LABELS, statusClassName } from '../../shared/productStatus'
import '../../shared/list-page.css'
import '../../shared/product-status.css'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

interface Filters {
  storeId: number
  referenceDate: string
  limit: string
}

interface CriticalProductsPageProps {
  onSelectProduct?: (productId: number, storeId: number) => void
}

export function CriticalProductsPage({ onSelectProduct }: CriticalProductsPageProps) {
  const [storeId, setStoreId] = useState(STORES[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [limit, setLimit] = useState('')
  const [products, setProducts] = useState<CriticalProduct[] | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: CriticalProduct[] | null, errorMessage: string | null) => {
    setProducts(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    referenceDate: filters.referenceDate,
    limit: filters.limit ? Number(filters.limit) : undefined,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getCriticalProducts(toApiParams({ storeId, referenceDate, limit }))
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
    getCriticalProducts(toApiParams({ storeId, referenceDate, limit }))
      .then((result) => applyResult(result, null))
      .catch((err) => {
        applyResult(null, err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
      })
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Productos críticos</h1>
        <p>Productos que requieren reposición o ya están en quiebre de stock, ordenados por urgencia.</p>
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
          Límite de resultados
          <input
            type="number"
            min={1}
            placeholder="Sin límite"
            value={limit}
            onChange={(event) => setLimit(event.target.value)}
          />
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
        <p className="state state-empty">No hay productos críticos para esta sucursal en la fecha seleccionada.</p>
      )}

      {products && products.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Producto</th>
              <th>Categoría</th>
              <th>Stock actual</th>
              <th>Punto de pedido</th>
              <th>Cobertura (días)</th>
              <th>Estado</th>
              <th>Score</th>
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
                <td>{product.reorderPointUnits.toFixed(1)}</td>
                <td>{product.currentDaysOfCoverage.toFixed(1)}</td>
                <td>
                  <span className={statusClassName(product.status)}>{STATUS_LABELS[product.status]}</span>
                </td>
                <td>{product.criticalityScore.toFixed(0)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}
