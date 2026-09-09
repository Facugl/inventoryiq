import { useEffect, useState, type FormEvent } from 'react'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  List,
  ListItemButton,
  ListItemText,
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
import { ingestCsvFile, CsvIngestionThresholdError } from '../../api/csvIngestion'
import { recalculateProductStatus } from '../../api/productStatus'
import { recordInventoryCount } from '../../api/inventorySnapshots'
import { searchProducts } from '../../api/products'
import { getCategories, updateCategoryParameters } from '../../api/categories'
import { getErrorMessage } from '../../shared/apiError'
import { showSuccess } from '../../shared/toast'
import type { Category, InventorySnapshot, ProductSummary, RowRejection, Store } from '../../api/types'
import { inventoryCountSchema, type InventoryCountFormValues } from './validation/inventoryCountSchema'
import { categoryParametersSchema, type CategoryParametersFormValues } from './validation/categoryParametersSchema'

function RejectionsTable({ rejections }: { rejections: RowRejection[] }) {
  if (rejections.length === 0) return null

  return (
    <TableContainer component={Paper} variant="outlined" sx={{ mt: 1 }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Fila</TableCell>
            <TableCell>Motivo</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rejections.map((rejection) => (
            <TableRow key={rejection.rowNumber}>
              <TableCell>{rejection.rowNumber}</TableCell>
              <TableCell>{rejection.reason}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function InventoryCountSection({ stores }: { stores: Store[] }) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [searchTerm, setSearchTerm] = useState('')
  const [debouncedTerm, setDebouncedTerm] = useState('')
  const [selectedProduct, setSelectedProduct] = useState<ProductSummary | null>(null)
  const [sessionLog, setSessionLog] = useState<{ product: ProductSummary; snapshot: InventorySnapshot }[]>([])

  // Debounce puro de UI: no orquesta datos, solo demora cuándo se dispara la búsqueda.
  useEffect(() => {
    const timeoutId = setTimeout(() => setDebouncedTerm(searchTerm.trim()), 300)
    return () => clearTimeout(timeoutId)
  }, [searchTerm])

  const {
    data: results,
    error: searchError,
  } = useQuery({
    queryKey: ['productSearch', debouncedTerm],
    queryFn: () => searchProducts(debouncedTerm),
    enabled: debouncedTerm.length > 0 && !selectedProduct,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InventoryCountFormValues>({ resolver: yupResolver(inventoryCountSchema) })

  const countMutation = useMutation({
    mutationFn: (values: InventoryCountFormValues) =>
      recordInventoryCount({ productId: selectedProduct!.productId, storeId, stockActual: values.quantity }),
    onSuccess: (snapshot) => {
      setSessionLog((current) => [{ product: selectedProduct!, snapshot }, ...current])
      showSuccess(`Conteo registrado: ${selectedProduct!.sku} — ${snapshot.currentStock} unidades.`)
      setSelectedProduct(null)
      reset()
    },
  })

  const selectProduct = (product: ProductSummary) => {
    setSelectedProduct(product)
    setSearchTerm('')
    setDebouncedTerm('')
    reset()
  }

  const cancelSelection = () => {
    setSelectedProduct(null)
    reset()
  }

  const saveError = errors.quantity?.message ?? (countMutation.error ? getErrorMessage(countMutation.error) : null)

  return (
    <Box component="section" sx={{ mb: 4 }}>
      <Typography variant="h5" component="h2" gutterBottom>
        Conteo de stock
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 720 }}>
        Registrá el conteo físico de un producto antes de pedirle a un proveedor — buscá por código de barras,
        código interno o nombre.
      </Typography>

      <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap', alignItems: 'flex-end', mb: 2 }}>
        <TextField select size="small" label="Sucursal" value={storeId} onChange={(event) => setStoreId(Number(event.target.value))}>
          {stores.map((store) => (
            <MenuItem key={store.id} value={store.id}>
              {store.name}
            </MenuItem>
          ))}
        </TextField>

        {!selectedProduct && (
          <TextField
            size="small"
            label="Buscar producto"
            placeholder="Código o nombre…"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            sx={{ minWidth: 260 }}
          />
        )}
      </Stack>

      {!selectedProduct && searchError && <Alert severity="error" sx={{ mb: 2 }}>{getErrorMessage(searchError)}</Alert>}

      {!selectedProduct && results && results.length > 0 && (
        <List component={Paper} variant="outlined" dense sx={{ mb: 2, maxWidth: 480 }}>
          {results.map((product) => (
            <ListItemButton key={product.productId} onClick={() => selectProduct(product)}>
              <ListItemText primary={`${product.sku} — ${product.name}`} />
            </ListItemButton>
          ))}
        </List>
      )}

      {!selectedProduct && debouncedTerm !== '' && results && results.length === 0 && !searchError && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Sin resultados para "{searchTerm}".
        </Alert>
      )}

      {selectedProduct && (
        <Stack
          component="form"
          onSubmit={handleSubmit((values) => countMutation.mutate(values))}
          direction="row"
          spacing={2}
          useFlexGap
          sx={{ flexWrap: 'wrap', alignItems: 'flex-end', mb: 2 }}
        >
          <Typography sx={{ alignSelf: 'center' }}>
            <strong>{selectedProduct.sku}</strong> — {selectedProduct.name}
          </Typography>

          <TextField
            type="number"
            size="small"
            label="Cantidad contada"
            autoFocus
            slotProps={{ htmlInput: { min: 0 } }}
            {...register('quantity')}
          />

          <Button type="submit" variant="contained" disabled={countMutation.isPending}>
            {countMutation.isPending ? 'Guardando…' : 'Guardar'}
          </Button>
          <Button type="button" variant="outlined" onClick={cancelSelection}>
            Cancelar
          </Button>
        </Stack>
      )}

      {saveError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {saveError}
        </Alert>
      )}

      {sessionLog.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>SKU</TableCell>
                <TableCell>Producto</TableCell>
                <TableCell>Cantidad contada</TableCell>
                <TableCell>Fecha</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sessionLog.map(({ product, snapshot }) => (
                <TableRow key={snapshot.inventoryId}>
                  <TableCell>{product.sku}</TableCell>
                  <TableCell>{product.name}</TableCell>
                  <TableCell>{snapshot.currentStock}</TableCell>
                  <TableCell>{snapshot.snapshotDate}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

function CsvIngestionSection() {
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (selectedFile: File) => ingestCsvFile('SALES', selectedFile),
    onSuccess: (summary) => showSuccess(`${summary.acceptedCount} de ${summary.totalRowsRead} filas aceptadas.`),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!file) {
      setFileError('Elegí un archivo CSV.')
      return
    }
    setFileError(null)
    mutation.mutate(file)
  }

  const thresholdError = mutation.error instanceof CsvIngestionThresholdError ? mutation.error : null
  const genericError = fileError ?? (mutation.error && !thresholdError ? getErrorMessage(mutation.error) : null)

  return (
    <Box component="section" sx={{ mb: 4 }}>
      <Typography variant="h5" component="h2" gutterBottom>
        Carga de ventas (CSV)
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 720 }}>
        Subí un archivo CSV de <strong>ventas</strong> para incorporarlo al sistema — por ahora es el único tipo
        de archivo soportado.
      </Typography>

      <Stack
        component="form"
        onSubmit={handleSubmit}
        direction="row"
        spacing={2}
        useFlexGap
        sx={{ flexWrap: 'wrap', alignItems: 'center', mb: 2 }}
      >
        <Button component="label" variant="outlined">
          {file ? file.name : 'Elegir archivo (ventas.csv)'}
          <input type="file" accept=".csv" hidden onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        </Button>

        <Button type="submit" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Subiendo…' : 'Subir archivo'}
        </Button>
      </Stack>

      {genericError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {genericError}
        </Alert>
      )}

      {thresholdError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          <Typography variant="body2">
            {thresholdError.message} ({thresholdError.rejectedCount} de {thresholdError.totalRowsRead} filas,{' '}
            {thresholdError.rejectionRatePercent.toFixed(1)}%). No se persistió ninguna fila de este lote.
          </Typography>
          <RejectionsTable rejections={thresholdError.rejections} />
        </Alert>
      )}

      {mutation.isSuccess && mutation.data && (
        <Alert severity="success">
          <Typography variant="body2">
            {mutation.data.acceptedCount} de {mutation.data.totalRowsRead} filas aceptadas ({mutation.data.rejectedCount}{' '}
            rechazadas).
          </Typography>
          <RejectionsTable rejections={mutation.data.rejections} />
        </Alert>
      )}
    </Box>
  )
}

function RecalculateSection({ stores }: { stores: Store[] }) {
  const [storeId, setStoreId] = useState('')

  const mutation = useMutation({
    mutationFn: () => recalculateProductStatus(storeId ? Number(storeId) : undefined),
    onSuccess: () => showSuccess('Recálculo completado.'),
  })

  const storeName = (id: number) => stores.find((store) => store.id === id)?.name ?? `Sucursal #${id}`

  return (
    <Box component="section" sx={{ mb: 4 }}>
      <Typography variant="h5" component="h2" gutterBottom>
        Recalcular estado de productos
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 720 }}>
        Recalcula productos críticos, sobrestock, alertas y recomendaciones para una sucursal (o todas). Se corre
        automáticamente todos los días a las 02:00 — usá esto si necesitás actualizarlo antes.
      </Typography>

      <Stack
        component="form"
        onSubmit={(event) => {
          event.preventDefault()
          mutation.mutate()
        }}
        direction="row"
        spacing={2}
        useFlexGap
        sx={{ flexWrap: 'wrap', alignItems: 'flex-end', mb: 2 }}
      >
        <TextField select size="small" label="Sucursal" value={storeId} onChange={(event) => setStoreId(event.target.value)} sx={{ minWidth: 220 }}>
          <MenuItem value="">Todas las sucursales activas</MenuItem>
          {stores.map((store) => (
            <MenuItem key={store.id} value={store.id}>
              {store.name}
            </MenuItem>
          ))}
        </TextField>

        <Button type="submit" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Recalculando…' : 'Recalcular'}
        </Button>
      </Stack>

      {mutation.error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {getErrorMessage(mutation.error)}
        </Alert>
      )}

      {mutation.data && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Sucursal</TableCell>
                <TableCell>Críticos</TableCell>
                <TableCell>Sobrestock</TableCell>
                <TableCell>Alertas</TableCell>
                <TableCell>Recomendaciones (nuevas / actualizadas / auto-descartadas)</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {mutation.data.perStore.map((store) => (
                <TableRow key={store.storeId}>
                  <TableCell>{storeName(store.storeId)}</TableCell>
                  <TableCell>{store.criticalProductsFound}</TableCell>
                  <TableCell>{store.overstockProductsFound}</TableCell>
                  <TableCell>{store.alertsGenerated}</TableCell>
                  <TableCell>
                    {store.recommendations.totalGenerated} ({store.recommendations.newCount} /{' '}
                    {store.recommendations.updatedCount} / {store.recommendations.autoDiscardedCount})
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

interface CategoryParametersEditRowProps {
  category: Category
  onCancel: () => void
  onSaved: () => void
}

function CategoryParametersEditRow({ category, onCancel, onSaved }: CategoryParametersEditRowProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CategoryParametersFormValues>({
    resolver: yupResolver(categoryParametersSchema),
    defaultValues: {
      maxCoverageDaysThreshold: category.maxCoverageDaysThreshold,
      defaultExtraCoverageDays: category.defaultExtraCoverageDays,
    },
  })

  const mutation = useMutation({
    mutationFn: (values: CategoryParametersFormValues) => updateCategoryParameters(category.categoryId, values),
    onSuccess: (updated) => {
      queryClient.setQueryData<Category[]>(['categories'], (current) =>
        current?.map((c) => (c.categoryId === updated.categoryId ? updated : c)),
      )
      showSuccess(`Parámetros de "${updated.name}" actualizados.`)
      onSaved()
    },
  })

  const rowError =
    errors.maxCoverageDaysThreshold?.message ??
    errors.defaultExtraCoverageDays?.message ??
    (mutation.error ? getErrorMessage(mutation.error) : null)

  return (
    <>
      <TableCell>
        <TextField type="number" size="small" autoFocus sx={{ width: 100 }} slotProps={{ htmlInput: { min: 1 } }} {...register('maxCoverageDaysThreshold')} />
      </TableCell>
      <TableCell>
        <TextField type="number" size="small" sx={{ width: 100 }} slotProps={{ htmlInput: { min: 0 } }} {...register('defaultExtraCoverageDays')} />
      </TableCell>
      <TableCell>
        <Stack direction="row" spacing={1}>
          <Button size="small" variant="contained" onClick={handleSubmit((values) => mutation.mutate(values))} disabled={mutation.isPending}>
            {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </Button>
          <Button size="small" variant="outlined" onClick={onCancel} disabled={mutation.isPending}>
            Cancelar
          </Button>
        </Stack>
        {rowError && (
          <Typography variant="caption" color="error" component="p" sx={{ mt: 0.5 }}>
            {rowError}
          </Typography>
        )}
      </TableCell>
    </>
  )
}

function CategoryParametersSection() {
  const [editingId, setEditingId] = useState<number | null>(null)

  const {
    data: categories,
    error: loadError,
  } = useQuery({ queryKey: ['categories'], queryFn: getCategories })

  const parentName = (category: Category) =>
    category.parentCategoryId === null
      ? '—'
      : (categories ?? []).find((c) => c.categoryId === category.parentCategoryId)?.name ?? `#${category.parentCategoryId}`

  return (
    <Box component="section">
      <Typography variant="h5" component="h2" gutterBottom>
        Parámetros de categorías
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 720 }}>
        Umbral de sobrestock: a partir de cuántos días de cobertura un producto de esa categoría se marca en
        Sobrestock. Stock de seguridad extra: colchón adicional para calcular el punto de pedido. Los cambios se
        aplican de inmediato.
      </Typography>

      {loadError && <Alert severity="error">{getErrorMessage(loadError)}</Alert>}

      {categories && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Categoría</TableCell>
                <TableCell>Categoría padre</TableCell>
                <TableCell>Umbral sobrestock (días)</TableCell>
                <TableCell>Stock de seguridad extra (días)</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {categories.map((category) => (
                <TableRow key={category.categoryId}>
                  <TableCell>{category.name}</TableCell>
                  <TableCell>{parentName(category)}</TableCell>
                  {editingId === category.categoryId ? (
                    <CategoryParametersEditRow
                      category={category}
                      onCancel={() => setEditingId(null)}
                      onSaved={() => setEditingId(null)}
                    />
                  ) : (
                    <>
                      <TableCell>{category.maxCoverageDaysThreshold}</TableCell>
                      <TableCell>{category.defaultExtraCoverageDays}</TableCell>
                      <TableCell>
                        <Button size="small" onClick={() => setEditingId(category.categoryId)}>
                          Editar
                        </Button>
                      </TableCell>
                    </>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

interface AdminPageProps {
  stores: Store[]
}

export function AdminPage({ stores }: AdminPageProps) {
  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Administración
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Carga de datos y disparo manual de procesos de recálculo.
      </Typography>

      <InventoryCountSection stores={stores} />
      <CsvIngestionSection />
      <RecalculateSection stores={stores} />
      <CategoryParametersSection />
    </Box>
  )
}
