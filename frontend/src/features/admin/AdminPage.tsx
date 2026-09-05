import { useState, type FormEvent } from 'react'
import { ingestCsvFile, CsvIngestionThresholdError } from '../../api/csvIngestion'
import { recalculateProductStatus } from '../../api/productStatus'
import { ApiError } from '../../api/http'
import type { IngestionSummary, RecalculateProductStatusResult, RowRejection } from '../../api/types'
import { STORES } from '../../data/stores'
import '../../shared/list-page.css'
import './AdminPage.css'

function genericErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}

function RejectionsTable({ rejections }: { rejections: RowRejection[] }) {
  if (rejections.length === 0) return null

  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Fila</th>
            <th>Motivo</th>
          </tr>
        </thead>
        <tbody>
          {rejections.map((rejection) => (
            <tr key={rejection.rowNumber}>
              <td>{rejection.rowNumber}</td>
              <td>{rejection.reason}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function CsvIngestionSection() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [thresholdError, setThresholdError] = useState<CsvIngestionThresholdError | null>(null)
  const [result, setResult] = useState<IngestionSummary | null>(null)

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!file) {
      setError('Elegí un archivo CSV.')
      return
    }

    setUploading(true)
    setError(null)
    setThresholdError(null)
    setResult(null)

    ingestCsvFile('SALES', file)
      .then((summary) => setResult(summary))
      .catch((err) => {
        if (err instanceof CsvIngestionThresholdError) {
          setThresholdError(err)
        } else {
          setError(genericErrorMessage(err))
        }
      })
      .finally(() => setUploading(false))
  }

  return (
    <section>
      <h2>Carga de ventas (CSV)</h2>
      <p>
        Único tipo de archivo implementado hoy: <strong>ventas</strong>. Ver docs/InventoryIQ_Arquitectura.md — compras,
        inventario, productos, proveedores, categorías y sucursales no tienen ingesta vía API todavía.
      </p>

      <form className="filters" onSubmit={handleSubmit}>
        <label>
          Archivo (ventas.csv)
          <input
            type="file"
            accept=".csv"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </label>

        <button type="submit" disabled={uploading}>
          {uploading ? 'Subiendo…' : 'Subir archivo'}
        </button>
      </form>

      {error && (
        <p className="state state-error" role="alert">
          {error}
        </p>
      )}

      {thresholdError && (
        <div className="state state-error" role="alert">
          <p>
            {thresholdError.message} ({thresholdError.rejectedCount} de {thresholdError.totalRowsRead} filas,{' '}
            {thresholdError.rejectionRatePercent.toFixed(1)}%). No se persistió ninguna fila de este lote.
          </p>
          <RejectionsTable rejections={thresholdError.rejections} />
        </div>
      )}

      {result && (
        <div className="state state-empty">
          <p>
            {result.acceptedCount} de {result.totalRowsRead} filas aceptadas ({result.rejectedCount} rechazadas).
          </p>
          <RejectionsTable rejections={result.rejections} />
        </div>
      )}
    </section>
  )
}

function RecalculateSection() {
  const [storeId, setStoreId] = useState('')
  const [recalculating, setRecalculating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<RecalculateProductStatusResult | null>(null)

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setRecalculating(true)
    setError(null)
    setResult(null)

    recalculateProductStatus(storeId ? Number(storeId) : undefined)
      .then((summary) => setResult(summary))
      .catch((err) => setError(genericErrorMessage(err)))
      .finally(() => setRecalculating(false))
  }

  const storeName = (id: number) => STORES.find((store) => store.id === id)?.name ?? `Sucursal #${id}`

  return (
    <section>
      <h2>Recalcular estado de productos</h2>
      <p>
        Dispara manualmente lo mismo que corre el job programado (diario a las 02:00): productos críticos, sobrestock,
        recomendaciones y alertas, sucursal por sucursal. No persiste un "estado de producto" nuevo — el único efecto
        persistido es la actualización de recomendaciones en PostgreSQL.
      </p>

      <form className="filters" onSubmit={handleSubmit}>
        <label>
          Sucursal
          <select value={storeId} onChange={(event) => setStoreId(event.target.value)}>
            <option value="">Todas las sucursales activas</option>
            {STORES.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={recalculating}>
          {recalculating ? 'Recalculando…' : 'Recalcular'}
        </button>
      </form>

      {error && (
        <p className="state state-error" role="alert">
          {error}
        </p>
      )}

      {result && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Sucursal</th>
                <th>Críticos</th>
                <th>Sobrestock</th>
                <th>Alertas</th>
                <th>Recomendaciones (nuevas / actualizadas / auto-descartadas)</th>
              </tr>
            </thead>
            <tbody>
              {result.perStore.map((store) => (
                <tr key={store.storeId}>
                  <td>{storeName(store.storeId)}</td>
                  <td>{store.criticalProductsFound}</td>
                  <td>{store.overstockProductsFound}</td>
                  <td>{store.alertsGenerated}</td>
                  <td>
                    {store.recommendations.totalGenerated} ({store.recommendations.newCount} /{' '}
                    {store.recommendations.updatedCount} / {store.recommendations.autoDiscardedCount})
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export function AdminPage() {
  return (
    <main className="page">
      <header className="page-header">
        <h1>Administración</h1>
        <p>Carga de datos y disparo manual de procesos de recálculo.</p>
      </header>

      <CsvIngestionSection />
      <RecalculateSection />
    </main>
  )
}
