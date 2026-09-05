import { useEffect, useState } from 'react'
import { getStores } from './api/stores'
import { getCategories } from './api/categories'
import { ApiError } from './api/http'
import type { Category, Store } from './api/types'
import { HomePage } from './features/home/HomePage'
import { CriticalProductsPage } from './features/critical-products/CriticalProductsPage'
import { OverstockPage } from './features/overstock/OverstockPage'
import { ProductDetailPage, type ProductSelection } from './features/product-detail/ProductDetailPage'
import { RecommendationsPage } from './features/recommendations/RecommendationsPage'
import { AlertsPage } from './features/alerts/AlertsPage'
import { ClassificationPage } from './features/classification/ClassificationPage'
import { AdminPage } from './features/admin/AdminPage'
import './App.css'

type ScreenKey =
  | 'home'
  | 'alerts'
  | 'critical'
  | 'overstock'
  | 'recommendations'
  | 'detail'
  | 'classification'
  | 'admin'

const NAV_ITEMS: { key: ScreenKey; label: string }[] = [
  { key: 'home', label: 'Inicio' },
  { key: 'alerts', label: 'Alertas' },
  { key: 'critical', label: 'Productos críticos' },
  { key: 'overstock', label: 'Sobrestock' },
  { key: 'recommendations', label: 'Recomendaciones' },
  { key: 'detail', label: 'Detalle de producto' },
  { key: 'classification', label: 'Clasificación ABC/XYZ' },
  { key: 'admin', label: 'Administración' },
]

function App() {
  const [screen, setScreen] = useState<ScreenKey>('home')
  const [selection, setSelection] = useState<ProductSelection | null>(null)

  const [stores, setStores] = useState<Store[] | null>(null)
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [catalogError, setCatalogError] = useState<string | null>(null)

  // Se cargan una sola vez, apenas monta. Todo setState ocurre dentro de
  // then/catch (fuera del cuerpo síncrono del efecto) para no disparar el
  // warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    Promise.all([getStores(), getCategories()])
      .then(([storeList, categoryList]) => {
        if (!cancelled) {
          setStores(storeList)
          setCategories(categoryList)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setCatalogError(err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.')
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const selectProduct = (productId: number, storeId: number) => {
    setSelection({ productId, storeId })
    setScreen('detail')
  }

  if (catalogError) {
    return (
      <main style={{ padding: 32 }}>
        <p role="alert">No se pudo cargar el catálogo de sucursales/categorías: {catalogError}</p>
      </main>
    )
  }

  if (!stores || !categories) {
    return (
      <main style={{ padding: 32 }}>
        <p>Cargando…</p>
      </main>
    )
  }

  return (
    <>
      <nav className="app-nav">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={item.key === screen ? 'active' : ''}
            onClick={() => setScreen(item.key)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {screen === 'home' && <HomePage stores={stores} />}
      {screen === 'alerts' && <AlertsPage stores={stores} categories={categories} />}
      {screen === 'critical' && (
        <CriticalProductsPage stores={stores} categories={categories} onSelectProduct={selectProduct} />
      )}
      {screen === 'overstock' && (
        <OverstockPage stores={stores} categories={categories} onSelectProduct={selectProduct} />
      )}
      {screen === 'recommendations' && <RecommendationsPage stores={stores} />}
      {screen === 'detail' && <ProductDetailPage stores={stores} initialSelection={selection} />}
      {screen === 'classification' && <ClassificationPage stores={stores} categories={categories} />}
      {screen === 'admin' && <AdminPage stores={stores} />}
    </>
  )
}

export default App
