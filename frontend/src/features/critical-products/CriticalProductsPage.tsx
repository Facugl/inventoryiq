import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Link,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { getCriticalProducts } from '../../api/criticalProducts'
import { getErrorMessage } from '../../shared/apiError'
import { StatusChip } from '../../shared/StatusChip'
import type { Category, Store } from '../../api/types'
import { categoryLabel } from '../../shared/categoryLookup'
import { STATUS_LABELS } from '../../shared/productStatus'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

interface Filters {
  storeId: number
  referenceDate: string
  limit: string
}

interface CriticalProductsPageProps {
  stores: Store[]
  categories: Category[]
  onSelectProduct?: (productId: number, storeId: number) => void
}

export function CriticalProductsPage({ stores, categories, onSelectProduct }: CriticalProductsPageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [limit, setLimit] = useState('')
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, referenceDate, limit })

  const {
    data: products,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['criticalProducts', appliedFilters],
    queryFn: () =>
      getCriticalProducts({
        storeId: appliedFilters.storeId,
        referenceDate: appliedFilters.referenceDate,
        limit: appliedFilters.limit ? Number(appliedFilters.limit) : undefined,
      }),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, referenceDate, limit })
  }

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Productos críticos
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Productos que requieren reposición o ya están en quiebre de stock, ordenados por urgencia.
      </Typography>

      <Stack
        component="form"
        onSubmit={handleSubmit}
        direction="row"
        spacing={2}
        useFlexGap
        sx={{ flexWrap: 'wrap', alignItems: 'flex-end', mb: 3, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}
      >
        <TextField select size="small" label="Sucursal" value={storeId} onChange={(event) => setStoreId(Number(event.target.value))}>
          {stores.map((store) => (
            <MenuItem key={store.id} value={store.id}>
              {store.name}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          type="date"
          size="small"
          label="Fecha de referencia"
          value={referenceDate}
          onChange={(event) => setReferenceDate(event.target.value)}
          required
          slotProps={{ inputLabel: { shrink: true } }}
        />

        <TextField
          type="number"
          size="small"
          label="Límite de resultados"
          placeholder="Sin límite"
          value={limit}
          onChange={(event) => setLimit(event.target.value)}
          slotProps={{ htmlInput: { min: 1 } }}
        />

        <Button type="submit" variant="contained" disabled={isFetching}>
          {isFetching ? 'Buscando…' : 'Buscar'}
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {getErrorMessage(error)}
        </Alert>
      )}

      {!error && !isFetching && products && products.length === 0 && (
        <Alert severity="info">No hay productos críticos para esta sucursal en la fecha seleccionada.</Alert>
      )}

      {products && products.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>SKU</TableCell>
                <TableCell>Producto</TableCell>
                <TableCell>Categoría</TableCell>
                <TableCell>Stock actual</TableCell>
                <TableCell>Punto de pedido</TableCell>
                <TableCell>Cobertura (días)</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>Score</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {products.map((product) => (
                <TableRow key={product.productId}>
                  <TableCell>
                    {onSelectProduct ? (
                      <Link component="button" type="button" onClick={() => onSelectProduct(product.productId, product.storeId)}>
                        {product.sku}
                      </Link>
                    ) : (
                      product.sku
                    )}
                  </TableCell>
                  <TableCell>{product.productName}</TableCell>
                  <TableCell>{categoryLabel(categories, product.categoryId)}</TableCell>
                  <TableCell>{product.currentStock}</TableCell>
                  <TableCell>{product.reorderPointUnits.toFixed(1)}</TableCell>
                  <TableCell>{product.currentDaysOfCoverage.toFixed(1)}</TableCell>
                  <TableCell>
                    <StatusChip
                      statusKey={product.status.toLowerCase().replaceAll('_', '-')}
                      label={STATUS_LABELS[product.status]}
                    />
                  </TableCell>
                  <TableCell>{product.criticalityScore.toFixed(0)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
