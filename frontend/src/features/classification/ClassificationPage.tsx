import { useEffect, useState, type FormEvent } from 'react'
import { getProductClassification } from '../../api/classification'
import { ApiError } from '../../api/http'
import type { AbcClassification, ProductClassification, XyzClassification } from '../../api/types'
import { STORES } from '../../data/stores'
import '../../shared/list-page.css'
import '../../shared/product-status.css'
import './ClassificationPage.css'

const ABC_LABELS: Record<AbcClassification, string> = {
  A: 'A — Alto valor',
  B: 'B — Valor medio',
  C: 'C — Bajo valor',
}

const XYZ_LABELS: Record<XyzClassification, string> = {
  X: 'X — Demanda estable',
  Y: 'Y — Demanda variable',
  Z: 'Z — Demanda errática',
}

function badgeClassName(letter: string): string {
  return `status status-${letter.toLowerCase()}`
}

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

function genericErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}

interface Filters {
  storeId: number
  referenceDate: string
}

export function ClassificationPage() {
  const [storeId, setStoreId] = useState(STORES[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [products, setProducts] = useState<ProductClassification[] | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: ProductClassification[] | null, errorMessage: string | null) => {
    setProducts(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    referenceDate: filters.referenceDate,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getProductClassification(toApiParams({ storeId, referenceDate }))
      .then((result) => {
        if (!cancelled) applyResult(result, null)
      })
      .catch((err) => {
        if (!cancelled) applyResult(null, genericErrorMessage(err))
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
    getProductClassification(toApiParams({ storeId, referenceDate }))
      .then((result) => applyResult(result, null))
      .catch((err) => applyResult(null, genericErrorMessage(err)))
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Clasificación ABC/XYZ</h1>
        <p>
          ABC clasifica por contribución al valor de venta; XYZ, por variabilidad de la demanda. Cruzarlas prioriza el
          esfuerzo de gestión: un producto AX merece control estricto, un CZ puede gestionarse con reglas simples.
        </p>
      </header>

      <div className="legend">
        {Object.entries(ABC_LABELS).map(([letter, label]) => (
          <span key={letter} className={badgeClassName(letter)}>
            {label}
          </span>
        ))}
        {Object.entries(XYZ_LABELS).map(([letter, label]) => (
          <span key={letter} className={badgeClassName(letter)}>
            {label}
          </span>
        ))}
      </div>

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
        <p className="state state-empty">No hay productos clasificados para esta sucursal en la fecha seleccionada.</p>
      )}

      {products && products.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Producto</th>
                <th>Categoría</th>
                <th>ABC</th>
                <th>XYZ</th>
                <th>Matriz</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.productId}>
                  <td>{product.sku}</td>
                  <td>{product.productName}</td>
                  <td>#{product.categoryId}</td>
                  <td>
                    <span className={badgeClassName(product.abcClass)}>{product.abcClass}</span>
                  </td>
                  <td>
                    <span className={badgeClassName(product.xyzClass)}>{product.xyzClass}</span>
                  </td>
                  <td>
                    {product.abcClass}
                    {product.xyzClass}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
