import { useState } from 'react'
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

  const selectProduct = (productId: number, storeId: number) => {
    setSelection({ productId, storeId })
    setScreen('detail')
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

      {screen === 'home' && <HomePage />}
      {screen === 'alerts' && <AlertsPage />}
      {screen === 'critical' && <CriticalProductsPage onSelectProduct={selectProduct} />}
      {screen === 'overstock' && <OverstockPage onSelectProduct={selectProduct} />}
      {screen === 'recommendations' && <RecommendationsPage />}
      {screen === 'detail' && <ProductDetailPage initialSelection={selection} />}
      {screen === 'classification' && <ClassificationPage />}
      {screen === 'admin' && <AdminPage />}
    </>
  )
}

export default App
