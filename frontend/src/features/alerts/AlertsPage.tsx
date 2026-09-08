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
import { getAlerts } from '../../api/alerts'
import { getErrorMessage } from '../../shared/apiError'
import { StatusChip } from '../../shared/StatusChip'
import type { Alert as AlertItem, AlertSeverity, AlertType, Category, Store } from '../../api/types'
import { categoryLabel } from '../../shared/categoryLookup'

const TYPE_LABELS: Record<AlertType, string> = {
  STOCKOUT: 'Quiebre de stock',
  OVERSTOCK: 'Sobrestock',
}

const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
}

const TYPE_OPTIONS: { value: AlertType | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'STOCKOUT', label: 'Quiebre de stock' },
  { value: 'OVERSTOCK', label: 'Sobrestock' },
]

const SEVERITY_OPTIONS: { value: AlertSeverity | ''; label: string }[] = [
  { value: '', label: 'Todas' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'LOW', label: 'Baja' },
]

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'

interface Filters {
  storeId: number
  referenceDate: string
  type: AlertType | ''
  severity: AlertSeverity | ''
}

interface AlertsPageProps {
  stores: Store[]
  categories: Category[]
}

export function AlertsPage({ stores, categories }: AlertsPageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [type, setType] = useState<AlertType | ''>('')
  const [severity, setSeverity] = useState<AlertSeverity | ''>('')
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, referenceDate, type, severity })

  const {
    data: alerts,
    error,
    isFetching,
  } = useQuery<AlertItem[]>({
    queryKey: ['alerts', appliedFilters],
    queryFn: () =>
      getAlerts({
        storeId: appliedFilters.storeId,
        referenceDate: appliedFilters.referenceDate,
        type: appliedFilters.type || undefined,
        severity: appliedFilters.severity || undefined,
      }),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, referenceDate, type, severity })
  }

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Alertas
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Productos en quiebre de stock o sobrestock que necesitan atención, con su severidad.
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

        <TextField select size="small" label="Tipo" value={type} onChange={(event) => setType(event.target.value as AlertType | '')}>
          {TYPE_OPTIONS.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label="Severidad"
          value={severity}
          onChange={(event) => setSeverity(event.target.value as AlertSeverity | '')}
        >
          {SEVERITY_OPTIONS.map((option) => (
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

      {!error && !isFetching && alerts && alerts.length === 0 && (
        <Alert severity="info">No hay alertas para esta sucursal con estos filtros.</Alert>
      )}

      {alerts && alerts.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>SKU</TableCell>
                <TableCell>Producto</TableCell>
                <TableCell>Categoría</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Severidad</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {alerts.map((alert) => (
                <TableRow key={`${alert.productId}-${alert.type}`}>
                  <TableCell>{alert.sku}</TableCell>
                  <TableCell>{alert.productName}</TableCell>
                  <TableCell>{categoryLabel(categories, alert.categoryId)}</TableCell>
                  <TableCell>{TYPE_LABELS[alert.type]}</TableCell>
                  <TableCell>
                    <StatusChip statusKey={alert.severity.toLowerCase()} label={SEVERITY_LABELS[alert.severity]} />
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
