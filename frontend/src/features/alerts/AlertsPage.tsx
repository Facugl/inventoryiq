import { useEffect, useState, type FormEvent } from 'react'
import { getAlerts } from '../../api/alerts'
import { ApiError } from '../../api/http'
import type { Alert, AlertSeverity, AlertType, Category, Store } from '../../api/types'
import { categoryLabel } from '../../shared/categoryLookup'
import '../../shared/list-page.css'
import '../../shared/product-status.css'

const TYPE_LABELS: Record<AlertType, string> = {
  STOCKOUT: 'Quiebre de stock',
  OVERSTOCK: 'Sobrestock',
}

const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
}

function severityClassName(severity: AlertSeverity): string {
  return `status status-${severity.toLowerCase()}`
}

const TYPE_OPTIONS: { value: AlertType | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'STOCKOUT', label: 'Quiebre de stock' },
  { value: 'OVERSTOCK', label: 'Sobrestock' },
]

const SEVERITY_OPTIONS: { value: AlertSeverity | ''; label: string }[] = [
  { value: '', label: 'Todas' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'LOW', label: 'Baja' },
]

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

function genericErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}

interface Filters {
  storeId: number
  referenceDate: string
  type: AlertType | ''
  severity: AlertSeverity | ''
}

interface AlertsPageProps {
  stores: Store[]
  categories: Category[]
}

export function AlertsPage({ stores, categories }: AlertsPageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [type, setType] = useState<AlertType | ''>('')
  const [severity, setSeverity] = useState<AlertSeverity | ''>('')
  const [alerts, setAlerts] = useState<Alert[] | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: Alert[] | null, errorMessage: string | null) => {
    setAlerts(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    referenceDate: filters.referenceDate,
    type: filters.type || undefined,
    severity: filters.severity || undefined,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getAlerts(toApiParams({ storeId, referenceDate, type, severity }))
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
    getAlerts(toApiParams({ storeId, referenceDate, type, severity }))
      .then((result) => applyResult(result, null))
      .catch((err) => applyResult(null, genericErrorMessage(err)))
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Alertas</h1>
        <p>Productos en quiebre de stock o sobrestock que necesitan atención, con su severidad.</p>
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
          Fecha de referencia
          <input
            type="date"
            value={referenceDate}
            onChange={(event) => setReferenceDate(event.target.value)}
            required
          />
        </label>

        <label>
          Tipo
          <select value={type} onChange={(event) => setType(event.target.value as AlertType | '')}>
            {TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Severidad
          <select value={severity} onChange={(event) => setSeverity(event.target.value as AlertSeverity | '')}>
            {SEVERITY_OPTIONS.map((option) => (
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

      {!error && !loading && alerts && alerts.length === 0 && (
        <p className="state state-empty">No hay alertas para esta sucursal con estos filtros.</p>
      )}

      {alerts && alerts.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Producto</th>
                <th>Categoría</th>
                <th>Tipo</th>
                <th>Severidad</th>
              </tr>
            </thead>
            <tbody>
              {alerts.map((alert) => (
                <tr key={`${alert.productId}-${alert.type}`}>
                  <td>{alert.sku}</td>
                  <td>{alert.productName}</td>
                  <td>{categoryLabel(categories, alert.categoryId)}</td>
                  <td>{TYPE_LABELS[alert.type]}</td>
                  <td>
                    <span className={severityClassName(alert.severity)}>{SEVERITY_LABELS[alert.severity]}</span>
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
