import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Box, Button, CircularProgress, Stack } from '@mui/material'
import { getStores } from './api/stores'
import { getCategories } from './api/categories'
import { getErrorMessage } from './shared/apiError'
import { HomePage } from './features/home/HomePage'
import { CriticalProductsPage } from './features/critical-products/CriticalProductsPage'
import { OverstockPage } from './features/overstock/OverstockPage'
import { ProductDetailPage, type ProductSelection } from './features/product-detail/ProductDetailPage'
import { RecommendationsPage } from './features/recommendations/RecommendationsPage'
import { AlertsPage } from './features/alerts/AlertsPage'
import { ClassificationPage } from './features/classification/ClassificationPage'
import { SuppliersPage } from './features/suppliers/SuppliersPage'
import { AdminPage } from './features/admin/AdminPage'

type ScreenKey =
  | 'home'
  | 'alerts'
  | 'critical'
  | 'overstock'
  | 'recommendations'
  | 'detail'
  | 'classification'
  | 'suppliers'
  | 'admin'

const NAV_ITEMS: { key: ScreenKey; label: string }[] = [
  { key: 'home', label: 'Inicio' },
  { key: 'alerts', label: 'Alertas' },
  { key: 'critical', label: 'Productos críticos' },
  { key: 'overstock', label: 'Sobrestock' },
  { key: 'recommendations', label: 'Recomendaciones' },
  { key: 'detail', label: 'Detalle de producto' },
  { key: 'classification', label: 'Clasificación ABC/XYZ' },
  { key: 'suppliers', label: 'Proveedores' },
  { key: 'admin', label: 'Administración' },
]

function App() {
  const [screen, setScreen] = useState<ScreenKey>('home')
  const [selection, setSelection] = useState<ProductSelection | null>(null)

  const {
    data: stores,
    error: storesError,
  } = useQuery({ queryKey: ['stores'], queryFn: getStores })
  const {
    data: categories,
    error: categoriesError,
  } = useQuery({ queryKey: ['categories'], queryFn: getCategories })

  const catalogError = storesError ? getErrorMessage(storesError) : categoriesError ? getErrorMessage(categoriesError) : null

  const selectProduct = (productId: number, storeId: number) => {
    setSelection({ productId, storeId })
    setScreen('detail')
  }

  if (catalogError) {
    return (
      <Box sx={{ p: 4 }}>
        <Alert severity="error">No se pudo cargar el catálogo de sucursales/categorías: {catalogError}</Alert>
      </Box>
    )
  }

  if (!stores || !categories) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress size={24} />
      </Box>
    )
  }

  return (
    <>
      <Stack
        component="nav"
        direction="row"
        sx={{ flexWrap: 'wrap', gap: 0.5, px: { xs: 2, sm: 4 }, pt: 1.5, borderBottom: 1, borderColor: 'divider' }}
      >
        {NAV_ITEMS.map((item) => (
          <Button
            key={item.key}
            onClick={() => setScreen(item.key)}
            sx={{
              borderRadius: 0,
              borderBottom: 2,
              borderColor: item.key === screen ? 'primary.main' : 'transparent',
              color: item.key === screen ? 'text.primary' : 'text.secondary',
              py: 1.25,
            }}
          >
            {item.label}
          </Button>
        ))}
      </Stack>

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
      {screen === 'suppliers' && <SuppliersPage />}
      {screen === 'admin' && <AdminPage stores={stores} />}
    </>
  )
}

export default App
