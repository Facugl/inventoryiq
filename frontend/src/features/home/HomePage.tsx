import { useEffect, useState, type FormEvent } from 'react'
import { getInventoryKpis } from '../../api/kpis'
import { ApiError } from '../../api/http'
import type { InventoryKpis, Store } from '../../api/types'
import '../../shared/list-page.css'
import './HomePage.css'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md),
// con una ventana de 90 días hacia atrás (mismo criterio que el resto de las pantallas).
const DEFAULT_TO_DATE = '2026-08-01'
const DEFAULT_FROM_DATE = '2026-05-04'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const percentFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })
const decimalFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

function formatPercent(value: number | null): string {
  return value === null ? 'Sin datos' : `${percentFormatter.format(value)}%`
}

function formatDays(value: number | null): string {
  return value === null ? 'Sin datos' : `${decimalFormatter.format(value)} días`
}

function formatTurnover(value: number | null): string {
  return value === null ? 'Sin datos' : `${decimalFormatter.format(value)}x`
}

interface Filters {
  storeId: number
  fromDate: string
  toDate: string
}

interface HomePageProps {
  stores: Store[]
}

export function HomePage({ stores }: HomePageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [fromDate, setFromDate] = useState(DEFAULT_FROM_DATE)
  const [toDate, setToDate] = useState(DEFAULT_TO_DATE)
  const [kpis, setKpis] = useState<InventoryKpis | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: InventoryKpis | null, errorMessage: string | null) => {
    setKpis(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    fromDate: filters.fromDate,
    toDate: filters.toDate,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getInventoryKpis(toApiParams({ storeId, fromDate, toDate }))
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
    getInventoryKpis(toApiParams({ storeId, fromDate, toDate }))
      .then((result) => applyResult(result, null))
      .catch((err) => {
        applyResult(null, err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
      })
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Inicio</h1>
        <p>Indicadores agregados de inventario para el período seleccionado.</p>
      </header>

      <form className="filters" onSubmit={handleSubmit}>
        <label>
          Sucursal
          <select value={storeId} onChange={(event) => setStoreId(Number(event.target.value))}>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Desde
          <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} required />
        </label>

        <label>
          Hasta
          <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} required />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Calculando…' : 'Calcular'}
        </button>
      </form>

      {error && (
        <p className="state state-error" role="alert">
          {error}
        </p>
      )}

      {kpis && (
        <div className="kpi-grid">
          <article className="kpi-card">
            <h2>Tasa de quiebre de stock</h2>
            <p className="kpi-value">{formatPercent(kpis.stockoutRate)}</p>
            <p className="kpi-hint">% de SKUs con stock en cero, al {toDate}</p>
          </article>

          <article className="kpi-card">
            <h2>Cobertura promedio</h2>
            <p className="kpi-value">{formatDays(kpis.averageDaysOfCoverage)}</p>
            <p className="kpi-hint">Stock actual / venta promedio diaria</p>
          </article>

          <article className="kpi-card">
            <h2>Capital inmovilizado en sobrestock</h2>
            <p className="kpi-value">{currencyFormatter.format(kpis.immobilizedOverstockValue)}</p>
            <p className="kpi-hint">Valor de inventario en productos con sobrestock</p>
          </article>

          <article className="kpi-card">
            <h2>Recomendaciones seguidas</h2>
            <p className="kpi-value">{formatPercent(kpis.recommendationsFollowedRate)}</p>
            <p className="kpi-hint">Aplicadas sobre el total resuelto en el período</p>
          </article>

          <article className="kpi-card">
            <h2>Rotación de inventario</h2>
            <p className="kpi-value">{formatTurnover(kpis.inventoryTurnover)}</p>
            <p className="kpi-hint">Costo de mercadería vendida / inventario promedio</p>
          </article>
        </div>
      )}
    </main>
  )
}
