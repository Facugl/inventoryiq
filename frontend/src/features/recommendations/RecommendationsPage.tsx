import { useEffect, useState, type FormEvent } from 'react'
import { getRecommendations, recalculateRecommendations, registerRecommendationFeedback } from '../../api/recommendations'
import { ApiError } from '../../api/http'
import type { Recommendation, RecalculateRecommendationsSummary, RecommendationStatus } from '../../api/types'
import { STORES } from '../../data/stores'
import '../../shared/list-page.css'
import '../../shared/product-status.css'

const STATUS_LABELS: Record<RecommendationStatus, string> = {
  PENDING: 'Pendiente',
  APPLIED: 'Aplicada',
  DISCARDED: 'Descartada',
}

function statusClassName(status: RecommendationStatus): string {
  return `status status-${status.toLowerCase()}`
}

const STATUS_OPTIONS: { value: RecommendationStatus | ''; label: string }[] = [
  { value: '', label: 'Todas' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'APPLIED', label: 'Aplicada' },
  { value: 'DISCARDED', label: 'Descartada' },
]

function genericErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}

interface Filters {
  storeId: number
  status: RecommendationStatus | ''
}

export function RecommendationsPage() {
  const [storeId, setStoreId] = useState(STORES[0].id)
  const [status, setStatus] = useState<RecommendationStatus | ''>('')
  const [recommendations, setRecommendations] = useState<Recommendation[] | null>(null)
  // Arranca en true: la carga inicial se dispara apenas monta (ver efecto de abajo).
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [recalculating, setRecalculating] = useState(false)
  const [recalcResult, setRecalcResult] = useState<RecalculateRecommendationsSummary | null>(null)
  const [recalcError, setRecalcError] = useState<string | null>(null)

  const [updatingId, setUpdatingId] = useState<number | null>(null)

  const applyResult = (result: Recommendation[] | null, errorMessage: string | null) => {
    setRecommendations(result)
    setError(errorMessage)
    setLoading(false)
  }

  const toApiParams = (filters: Filters) => ({
    storeId: filters.storeId,
    status: filters.status || undefined,
  })

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getRecommendations(toApiParams({ storeId, status }))
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
    getRecommendations(toApiParams({ storeId, status }))
      .then((result) => applyResult(result, null))
      .catch((err) => applyResult(null, genericErrorMessage(err)))
  }

  const handleRecalculate = () => {
    setRecalculating(true)
    setRecalcError(null)
    setRecalcResult(null)

    recalculateRecommendations(storeId)
      .then((summary) => {
        setRecalcResult(summary)
        return getRecommendations(toApiParams({ storeId, status }))
      })
      .then((result) => applyResult(result, null))
      .catch((err) => setRecalcError(genericErrorMessage(err)))
      .finally(() => setRecalculating(false))
  }

  const handleFeedback = (recommendationId: number, newStatus: 'APPLIED' | 'DISCARDED') => {
    setUpdatingId(recommendationId)
    registerRecommendationFeedback(recommendationId, newStatus)
      .then((updated) => {
        setRecommendations((current) =>
          current ? current.map((rec) => (rec.recommendationId === recommendationId ? updated : rec)) : current,
        )
      })
      .catch((err) => setError(genericErrorMessage(err)))
      .finally(() => setUpdatingId(null))
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Recomendaciones</h1>
        <p>Recomendaciones de compra persistidas, con su justificación. Marcá cada una como aplicada o descartada.</p>
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
          Estado
          <select value={status} onChange={(event) => setStatus(event.target.value as RecommendationStatus | '')}>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Buscando…' : 'Buscar'}
        </button>

        <button type="button" onClick={handleRecalculate} disabled={recalculating}>
          {recalculating ? 'Recalculando…' : 'Recalcular recomendaciones'}
        </button>
      </form>

      {recalcError && (
        <p className="state state-error" role="alert">
          {recalcError}
        </p>
      )}

      {recalcResult && (
        <p className="state state-empty">
          {recalcResult.totalGenerated} recomendaciones generadas ({recalcResult.newCount} nuevas,{' '}
          {recalcResult.updatedCount} actualizadas, {recalcResult.autoDiscardedCount} auto-descartadas).
        </p>
      )}

      {error && (
        <p className="state state-error" role="alert">
          {error}
        </p>
      )}

      {!error && !loading && recommendations && recommendations.length === 0 && (
        <p className="state state-empty">No hay recomendaciones para esta sucursal con estos filtros.</p>
      )}

      {recommendations && recommendations.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Producto</th>
                <th>Cantidad sugerida</th>
                <th>Fecha límite</th>
                <th className="wrap-col">Justificación</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {recommendations.map((rec) => (
                <tr key={rec.recommendationId}>
                  <td>{rec.sku}</td>
                  <td>{rec.productName}</td>
                  <td>{rec.suggestedQuantity}</td>
                  <td>{rec.orderDeadlineDate}</td>
                  <td className="wrap-col">{rec.justification}</td>
                  <td>
                    <span className={statusClassName(rec.status)}>{STATUS_LABELS[rec.status]}</span>
                  </td>
                  <td>
                    {rec.status === 'PENDING' ? (
                      <div className="row-actions">
                        <button
                          type="button"
                          onClick={() => handleFeedback(rec.recommendationId, 'APPLIED')}
                          disabled={updatingId === rec.recommendationId}
                        >
                          Aplicar
                        </button>
                        <button
                          type="button"
                          onClick={() => handleFeedback(rec.recommendationId, 'DISCARDED')}
                          disabled={updatingId === rec.recommendationId}
                        >
                          Descartar
                        </button>
                      </div>
                    ) : (
                      rec.feedbackDate ?? '—'
                    )}
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
