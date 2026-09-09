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
import { getProductForecast } from '../../api/forecast'
import { getErrorMessage } from '../../shared/apiError'
import type { Store } from '../../api/types'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md).
const DEFAULT_REFERENCE_DATE = '2026-08-01'
// 4 períodos semanales completos (PERIOD_LENGTH_DAYS en ForecastDemandService).
const DEFAULT_HORIZON_DAYS = '28'

const decimalFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

export interface ProductSelection {
  productId: number
  storeId: number
}

interface AppliedSelection {
  productId: number
  storeId: number
  referenceDate: string
  horizonDays: number
}

interface ProductDetailPageProps {
  stores: Store[]
  initialSelection: ProductSelection | null
}

export function ProductDetailPage({ stores, initialSelection }: ProductDetailPageProps) {
  const [productId, setProductId] = useState(initialSelection ? String(initialSelection.productId) : '')
  const [storeId, setStoreId] = useState(initialSelection?.storeId ?? stores[0].id)
  const [referenceDate, setReferenceDate] = useState(DEFAULT_REFERENCE_DATE)
  const [horizonDays, setHorizonDays] = useState(DEFAULT_HORIZON_DAYS)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [appliedSelection, setAppliedSelection] = useState<AppliedSelection | null>(
    initialSelection
      ? {
          productId: initialSelection.productId,
          storeId: initialSelection.storeId,
          referenceDate: DEFAULT_REFERENCE_DATE,
          horizonDays: Number(DEFAULT_HORIZON_DAYS),
        }
      : null,
  )

  const {
    data: forecast,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['forecast', appliedSelection],
    queryFn: () => getProductForecast(appliedSelection!),
    enabled: appliedSelection !== null,
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()

    const parsedProductId = Number(productId)
    if (!productId || !Number.isInteger(parsedProductId) || parsedProductId <= 0) {
      setValidationError('Ingresá un ID de producto válido.')
      setAppliedSelection(null)
      return
    }

    setValidationError(null)
    setAppliedSelection({ productId: parsedProductId, storeId, referenceDate, horizonDays: Number(horizonDays) })
  }

  const errorMessage = validationError ?? (error ? getErrorMessage(error) : null)

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Detalle de producto
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 720 }}>
        Proyección de venta diaria de un producto, período por período, ajustada por estacionalidad.
      </Typography>

      <Stack
        component="form"
        onSubmit={handleSubmit}
        direction="row"
        spacing={2}
        useFlexGap
        sx={{ flexWrap: 'wrap', alignItems: 'flex-end', mb: 3, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}
      >
        <TextField
          type="number"
          size="small"
          label="ID de producto"
          value={productId}
          onChange={(event) => setProductId(event.target.value)}
          required
          slotProps={{ htmlInput: { min: 1 } }}
        />

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
          label="Horizonte (días)"
          value={horizonDays}
          onChange={(event) => setHorizonDays(event.target.value)}
          required
          slotProps={{ htmlInput: { min: 1 } }}
        />

        <Button type="submit" variant="contained" disabled={isFetching}>
          {isFetching ? 'Buscando…' : 'Buscar'}
        </Button>
      </Stack>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {errorMessage}
        </Alert>
      )}

      {!errorMessage && !isFetching && !forecast && (
        <Alert severity="info">Ingresá un ID de producto para ver su proyección de demanda.</Alert>
      )}

      {forecast && (
        <>
          <Typography variant="h6" component="h2" gutterBottom>
            {forecast.sku} — {forecast.productName}
          </Typography>

          {forecast.baseAds === null ? (
            <Alert severity="info">
              El producto existe, pero no tiene historial de ventas suficiente en esta sucursal para proyectar demanda.
            </Alert>
          ) : (
            <>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Venta promedio diaria base: {decimalFormatter.format(forecast.baseAds)} unidades/día
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Desde</TableCell>
                      <TableCell>Hasta</TableCell>
                      <TableCell>Índice estacional</TableCell>
                      <TableCell>ADS proyectado</TableCell>
                      <TableCell>Demanda proyectada</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {forecast.periods.map((period) => (
                      <TableRow key={period.periodStart}>
                        <TableCell>{period.periodStart}</TableCell>
                        <TableCell>{period.periodEnd}</TableCell>
                        <TableCell>{decimalFormatter.format(period.seasonalIndex)}</TableCell>
                        <TableCell>{decimalFormatter.format(period.projectedDailyAds)}</TableCell>
                        <TableCell>{period.projectedTotalDemand}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </>
      )}
    </Box>
  )
}
