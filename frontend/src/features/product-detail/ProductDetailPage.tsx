import { useEffect, useState, type FormEvent } from 'react'
import { getProductForecast } from '../../api/forecast'
import { ApiError } from '../../api/http'
import type { ProductForecast, Store } from '../../api/types'
import '../../shared/list-page.css'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'
// 4 períodos semanales completos (PERIOD_LENGTH_DAYS en ForecastDemandService).
const DEFAULT_HORIZON_DAYS = '28'

const decimalFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

export interface ProductSelection {
  productId: number
  storeId: number
}

interface ProductDetailPageProps {
  stores: Store[]
  initialSelection: ProductSelection | null
}

export function ProductDetailPage({ stores, initialSelection }: ProductDetailPageProps) {
  const [productId, setProductId] = useState(initialSelection ? String(initialSelection.productId) : '')
  const [storeId, setStoreId] = useState(initialSelection?.storeId ?? stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [horizonDays, setHorizonDays] = useState(DEFAULT_HORIZON_DAYS)
  const [forecast, setForecast] = useState<ProductForecast | null>(null)
  // Solo arranca en true si llegamos con un producto ya elegido (ver efecto de abajo).
  const [loading, setLoading] = useState(Boolean(initialSelection))
  const [error, setError] = useState<string | null>(null)

  const applyResult = (result: ProductForecast | null, errorMessage: string | null) => {
    setForecast(result)
    setError(errorMessage)
    setLoading(false)
  }

  // Carga inicial, solo si llegamos desde otra pantalla con un producto elegido.
  // Todo setState ocurre dentro de then/catch (fuera del cuerpo síncrono del
  // efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    if (!initialSelection) return
    let cancelled = false

    getProductForecast({
      productId: initialSelection.productId,
      storeId: initialSelection.storeId,
      referenceDate: DEFAULT_REFERENCE_DATE,
      horizonDays: Number(DEFAULT_HORIZON_DAYS),
    })
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

    const parsedProductId = Number(productId)
    if (!productId || !Number.isInteger(parsedProductId) || parsedProductId <= 0) {
      setForecast(null)
      setError('Ingresá un ID de producto válido.')
      return
    }

    setLoading(true)
    setError(null)
    getProductForecast({ productId: parsedProductId, storeId, referenceDate, horizonDays: Number(horizonDays) })
      .then((result) => applyResult(result, null))
      .catch((err) => {
        applyResult(null, err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
      })
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Detalle de producto</h1>
        <p>
          Proyección de demanda por producto. No incluye ficha completa ni histórico de ventas/stock: hoy el backend
          solo expone la proyección (ver docs/InventoryIQ_Arquitectura.md).
        </p>
      </header>

      <form className="filters" onSubmit={handleSubmit}>
        <label>
          ID de producto
          <input
            type="number"
            min={1}
            value={productId}
            onChange={(event) => setProductId(event.target.value)}
            required
          />
        </label>

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
          Horizonte (días)
          <input
            type="number"
            min={1}
            value={horizonDays}
            onChange={(event) => setHorizonDays(event.target.value)}
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

      {!error && !loading && !forecast && (
        <p className="state state-empty">Ingresá un ID de producto para ver su proyección de demanda.</p>
      )}

      {forecast && (
        <>
          <h2>
            {forecast.sku} — {forecast.productName}
          </h2>

          {forecast.baseAds === null ? (
            <p className="state state-empty">
              El producto existe, pero no tiene historial de ventas suficiente en esta sucursal para proyectar demanda.
            </p>
          ) : (
            <>
              <p>Venta promedio diaria base: {decimalFormatter.format(forecast.baseAds)} unidades/día</p>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Desde</th>
                      <th>Hasta</th>
                      <th>Índice estacional</th>
                      <th>ADS proyectado</th>
                      <th>Demanda proyectada</th>
                    </tr>
                  </thead>
                  <tbody>
                    {forecast.periods.map((period) => (
                      <tr key={period.periodStart}>
                        <td>{period.periodStart}</td>
                        <td>{period.periodEnd}</td>
                        <td>{decimalFormatter.format(period.seasonalIndex)}</td>
                        <td>{decimalFormatter.format(period.projectedDailyAds)}</td>
                        <td>{period.projectedTotalDemand}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </>
      )}
    </main>
  )
}
