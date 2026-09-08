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
import { getOverstockProducts } from '../../api/overstockProducts'
import { getErrorMessage } from '../../shared/apiError'
import type { Category, OverstockSortBy, Store } from '../../api/types'
import { categoryLabel } from '../../shared/categoryLookup'

const SORT_OPTIONS: { value: OverstockSortBy; label: string }[] = [
  { value: 'IMMOBILIZED_VALUE', label: 'Valor inmovilizado' },
  { value: 'DAYS_OF_COVERAGE', label: 'Días de cobertura' },
]

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

interface Filters {
  storeId: number
  referenceDate: string
  sortBy: OverstockSortBy
}

interface OverstockPageProps {
  stores: Store[]
  categories: Category[]
  onSelectProduct?: (productId: number, storeId: number) => void
}

export function OverstockPage({ stores, categories, onSelectProduct }: OverstockPageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [sortBy, setSortBy] = useState<OverstockSortBy>('IMMOBILIZED_VALUE')
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, referenceDate, sortBy })

  const {
    data: products,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['overstockProducts', appliedFilters],
    queryFn: () => getOverstockProducts(appliedFilters),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, referenceDate, sortBy })
  }

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Sobrestock
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Productos con cobertura excesiva que inmovilizan capital, candidatos a liquidación o promoción.
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

        <TextField select size="small" label="Ordenar por" value={sortBy} onChange={(event) => setSortBy(event.target.value as OverstockSortBy)}>
          {SORT_OPTIONS.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>

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
        <Alert severity="info">No hay productos en sobrestock para esta sucursal en la fecha seleccionada.</Alert>
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
                <TableCell>Cobertura (días)</TableCell>
                <TableCell>Valor inmovilizado</TableCell>
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
                  <TableCell>{product.currentDaysOfCoverage.toFixed(1)}</TableCell>
                  <TableCell>{currencyFormatter.format(product.immobilizedValue)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
