import { useEffect, useState, type FormEvent } from 'react'
import { ingestCsvFile, CsvIngestionThresholdError } from '../../api/csvIngestion'
import { recalculateProductStatus } from '../../api/productStatus'
import { recordInventoryCount } from '../../api/inventorySnapshots'
import { searchProducts } from '../../api/products'
import { ApiError } from '../../api/http'
import type {
  IngestionSummary,
  InventorySnapshot,
  ProductSummary,
  RecalculateProductStatusResult,
  RowRejection,
  Store,
} from '../../api/types'
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

function InventoryCountSection({ stores }: { stores: Store[] }) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [searchTerm, setSearchTerm] = useState('')
  const [results, setResults] = useState<ProductSummary[]>([])
  const [searchError, setSearchError] = useState<string | null>(null)
  const [selectedProduct, setSelectedProduct] = useState<ProductSummary | null>(null)
  const [quantity, setQuantity] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [sessionLog, setSessionLog] = useState<{ product: ProductSummary; snapshot: InventorySnapshot }[]>([])

  // Búsqueda con debounce. Todo setState ocurre dentro del callback del
  // timer (asíncrono), nunca en el cuerpo síncrono del efecto, para no
  // disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    const timeoutId = setTimeout(() => {
      const term = searchTerm.trim()
      if (!term) {
        if (!cancelled) {
          setResults([])
          setSearchError(null)
        }
        return
      }

      searchProducts(term)
        .then((found) => {
          if (!cancelled) {
            setResults(found)
            setSearchError(null)
          }
        })
        .catch((err) => {
          if (!cancelled) {
            setResults([])
            setSearchError(genericErrorMessage(err))
          }
        })
    }, 300)

    return () => {
      cancelled = true
      clearTimeout(timeoutId)
    }
  }, [searchTerm])

  const selectProduct = (product: ProductSummary) => {
    setSelectedProduct(product)
    setSearchTerm('')
    setResults([])
    setQuantity('')
    setSaveError(null)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!selectedProduct) return

    const parsedQuantity = Number(quantity)
    if (quantity === '' || !Number.isInteger(parsedQuantity) || parsedQuantity < 0) {
      setSaveError('Ingresá una cantidad válida (0 o más).')
      return
    }

    setSaving(true)
    setSaveError(null)
    recordInventoryCount({ productId: selectedProduct.productId, storeId, stockActual: parsedQuantity })
      .then((snapshot) => {
        setSessionLog((current) => [{ product: selectedProduct, snapshot }, ...current])
        setSelectedProduct(null)
        setQuantity('')
      })
      .catch((err) => setSaveError(genericErrorMessage(err)))
      .finally(() => setSaving(false))
  }

  return (
    <section>
      <h2>Conteo de stock</h2>
      <p>
        Registrá el conteo físico de un producto antes de pedirle a un proveedor — buscá por código (de barras o
        interno, por ejemplo "33" para un producto de fiambrería) o por nombre.
      </p>

      <div className="filters">
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

        {!selectedProduct && (
          <label>
            Buscar producto
            <input
              type="text"
              placeholder="Código o nombre…"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>
        )}
      </div>

      {searchError && (
        <p className="state state-error" role="alert">
          {searchError}
        </p>
      )}

      {!selectedProduct && results.length > 0 && (
        <ul className="search-results">
          {results.map((product) => (
            <li key={product.productId}>
              <button type="button" onClick={() => selectProduct(product)}>
                <strong>{product.sku}</strong> — {product.name}
              </button>
            </li>
          ))}
        </ul>
      )}

      {!selectedProduct && searchTerm.trim() !== '' && results.length === 0 && !searchError && (
        <p className="state state-empty">Sin resultados para "{searchTerm}".</p>
      )}

      {selectedProduct && (
        <form className="filters" onSubmit={handleSubmit}>
          <div className="selected-product">
            <strong>{selectedProduct.sku}</strong> — {selectedProduct.name}
          </div>

          <label>
            Cantidad contada
            <input
              type="number"
              min={0}
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              autoFocus
              required
            />
          </label>

          <button type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
          <button type="button" onClick={() => setSelectedProduct(null)}>
            Cancelar
          </button>
        </form>
      )}

      {saveError && (
        <p className="state state-error" role="alert">
          {saveError}
        </p>
      )}

      {sessionLog.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Producto</th>
                <th>Cantidad contada</th>
                <th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {sessionLog.map(({ product, snapshot }) => (
                <tr key={snapshot.inventoryId}>
                  <td>{product.sku}</td>
                  <td>{product.name}</td>
                  <td>{snapshot.currentStock}</td>
                  <td>{snapshot.snapshotDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
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

function RecalculateSection({ stores }: { stores: Store[] }) {
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

  const storeName = (id: number) => stores.find((store) => store.id === id)?.name ?? `Sucursal #${id}`

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
            {stores.map((store) => (
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

interface AdminPageProps {
  stores: Store[]
}

export function AdminPage({ stores }: AdminPageProps) {
  return (
    <main className="page">
      <header className="page-header">
        <h1>Administración</h1>
        <p>Carga de datos y disparo manual de procesos de recálculo.</p>
      </header>

      <InventoryCountSection stores={stores} />
      <CsvIngestionSection />
      <RecalculateSection stores={stores} />
    </main>
  )
}
