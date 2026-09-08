import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
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
import { getProductClassification } from '../../api/classification'
import { getErrorMessage } from '../../shared/apiError'
import { StatusChip } from '../../shared/StatusChip'
import type { AbcClassification, Category, Store, XyzClassification } from '../../api/types'
import { categoryLabel } from '../../shared/categoryLookup'

const ABC_LABELS: Record<AbcClassification, string> = {
  A: 'A — Alto valor',
  B: 'B — Valor medio',
  C: 'C — Bajo valor',
}

const XYZ_LABELS: Record<XyzClassification, string> = {
  X: 'X — Demanda estable',
  Y: 'Y — Demanda variable',
  Z: 'Z — Demanda errática',
}

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

interface Filters {
  storeId: number
  referenceDate: string
}

interface ClassificationPageProps {
  stores: Store[]
  categories: Category[]
}

export function ClassificationPage({ stores, categories }: ClassificationPageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, referenceDate })

  const {
    data: products,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['classification', appliedFilters],
    queryFn: () => getProductClassification(appliedFilters),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, referenceDate })
  }

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Clasificación ABC/XYZ
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 2, maxWidth: 800 }}>
        ABC clasifica por contribución al valor de venta; XYZ, por variabilidad de la demanda. Cruzarlas prioriza el
        esfuerzo de gestión: un producto AX merece control estricto, un CZ puede gestionarse con reglas simples.
      </Typography>

      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap', mb: 3 }}>
        {Object.entries(ABC_LABELS).map(([letter, label]) => (
          <StatusChip key={letter} statusKey={letter.toLowerCase()} label={label} />
        ))}
        {Object.entries(XYZ_LABELS).map(([letter, label]) => (
          <StatusChip key={letter} statusKey={letter.toLowerCase()} label={label} />
        ))}
      </Stack>

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
        <Alert severity="info">No hay productos clasificados para esta sucursal en la fecha seleccionada.</Alert>
      )}

      {products && products.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>SKU</TableCell>
                <TableCell>Producto</TableCell>
                <TableCell>Categoría</TableCell>
                <TableCell>ABC</TableCell>
                <TableCell>XYZ</TableCell>
                <TableCell>Matriz</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {products.map((product) => (
                <TableRow key={product.productId}>
                  <TableCell>{product.sku}</TableCell>
                  <TableCell>{product.productName}</TableCell>
                  <TableCell>{categoryLabel(categories, product.categoryId)}</TableCell>
                  <TableCell>
                    <StatusChip statusKey={product.abcClass.toLowerCase()} label={product.abcClass} />
                  </TableCell>
                  <TableCell>
                    <StatusChip statusKey={product.xyzClass.toLowerCase()} label={product.xyzClass} />
                  </TableCell>
                  <TableCell>
                    {product.abcClass}
                    {product.xyzClass}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
