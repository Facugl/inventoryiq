import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Box, Button, Card, CardContent, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { getInventoryKpis } from '../../api/kpis'
import { getErrorMessage } from '../../shared/apiError'
import type { Store } from '../../api/types'

// Fecha de corte de los datos CSV simulados (docs/README_datos_simulados.md),
// con una ventana de 90 días hacia atrás (mismo criterio que el resto de las pantallas).
const DEFAULT_TO_DATE = '2026-08-01'
const DEFAULT_FROM_DATE = '2026-05-04'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const percentFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })
const decimalFormatter = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

function formatPercent(value: number | null): string {
  return value === null ? 'Sin datos' : `${percentFormatter.format(value)}%`
}

function formatDays(value: number | null): string {
  return value === null ? 'Sin datos' : `${decimalFormatter.format(value)} días`
}

function formatTurnover(value: number | null): string {
  return value === null ? 'Sin datos' : `${decimalFormatter.format(value)}x`
}

interface Filters {
  storeId: number
  fromDate: string
  toDate: string
}

interface HomePageProps {
  stores: Store[]
}

export function HomePage({ stores }: HomePageProps) {
  const [storeId, setStoreId] = useState(stores[0].id)
  const [fromDate, setFromDate] = useState(DEFAULT_FROM_DATE)
  const [toDate, setToDate] = useState(DEFAULT_TO_DATE)
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, fromDate, toDate })

  const {
    data: kpis,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['kpis', appliedFilters],
    queryFn: () => getInventoryKpis(appliedFilters),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, fromDate, toDate })
  }

  const kpiCards = kpis
    ? [
        { title: 'Tasa de quiebre de stock', value: formatPercent(kpis.stockoutRate), hint: `% de SKUs con stock en cero, al ${toDate}` },
        { title: 'Cobertura promedio', value: formatDays(kpis.averageDaysOfCoverage), hint: 'Stock actual / venta promedio diaria' },
        {
          title: 'Capital inmovilizado en sobrestock',
          value: currencyFormatter.format(kpis.immobilizedOverstockValue),
          hint: 'Valor de inventario en productos con sobrestock',
        },
        {
          title: 'Recomendaciones seguidas',
          value: formatPercent(kpis.recommendationsFollowedRate),
          hint: 'Aplicadas sobre el total resuelto en el período',
        },
        { title: 'Rotación de inventario', value: formatTurnover(kpis.inventoryTurnover), hint: 'Costo de mercadería vendida / inventario promedio' },
      ]
    : []

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Inicio
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Indicadores agregados de inventario para el período seleccionado.
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
          label="Desde"
          value={fromDate}
          onChange={(event) => setFromDate(event.target.value)}
          required
          slotProps={{ inputLabel: { shrink: true } }}
        />

        <TextField
          type="date"
          size="small"
          label="Hasta"
          value={toDate}
          onChange={(event) => setToDate(event.target.value)}
          required
          slotProps={{ inputLabel: { shrink: true } }}
        />

        <Button type="submit" variant="contained" disabled={isFetching}>
          {isFetching ? 'Calculando…' : 'Calcular'}
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {getErrorMessage(error)}
        </Alert>
      )}

      {kpis && (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 2 }}>
          {kpiCards.map((card) => (
            <Card key={card.title} variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  {card.title}
                </Typography>
                <Typography variant="h5" component="p" gutterBottom>
                  {card.value}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {card.hint}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Box>
      )}
    </Box>
  )
}
