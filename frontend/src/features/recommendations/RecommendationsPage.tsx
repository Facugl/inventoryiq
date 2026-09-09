import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
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
  Tooltip,
  Typography,
} from '@mui/material'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { getRecommendations, recalculateRecommendations, registerRecommendationFeedback } from '../../api/recommendations'
import { getErrorMessage } from '../../shared/apiError'
import { showError, showSuccess } from '../../shared/toast'
import { StatusChip } from '../../shared/StatusChip'
import type { RecommendationStatus, Store } from '../../api/types'

interface JustificationDialogState {
  sku: string
  productName: string
  justification: string
}

const STATUS_LABELS: Record<RecommendationStatus, string> = {
  PENDING: 'Pendiente',
  APPLIED: 'Aplicada',
  DISCARDED: 'Descartada',
}

const STATUS_OPTIONS: { value: RecommendationStatus | ''; label: string }[] = [
  { value: '', label: 'Todas' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'APPLIED', label: 'Aplicada' },
  { value: 'DISCARDED', label: 'Descartada' },
]

interface Filters {
  storeId: number
  status: RecommendationStatus | ''
}

interface RecommendationsPageProps {
  stores: Store[]
}

export function RecommendationsPage({ stores }: RecommendationsPageProps) {
  const queryClient = useQueryClient()

  const [storeId, setStoreId] = useState(stores[0].id)
  const [status, setStatus] = useState<RecommendationStatus | ''>('')
  const [appliedFilters, setAppliedFilters] = useState<Filters>({ storeId, status })
  const [justificationDialog, setJustificationDialog] = useState<JustificationDialogState | null>(null)

  const recommendationsQueryKey = ['recommendations', appliedFilters]

  const {
    data: recommendations,
    error,
    isFetching,
  } = useQuery({
    queryKey: recommendationsQueryKey,
    queryFn: () =>
      getRecommendations({ storeId: appliedFilters.storeId, status: appliedFilters.status || undefined }),
  })

  const recalculateMutation = useMutation({
    mutationFn: () => recalculateRecommendations(appliedFilters.storeId),
    onSuccess: (summary) => {
      showSuccess(
        `${summary.totalGenerated} recomendaciones generadas (${summary.newCount} nuevas, ${summary.updatedCount} actualizadas, ${summary.autoDiscardedCount} auto-descartadas).`,
      )
      queryClient.invalidateQueries({ queryKey: recommendationsQueryKey })
    },
    onError: (err) => showError(getErrorMessage(err)),
  })

  const feedbackMutation = useMutation({
    mutationFn: ({ recommendationId, newStatus }: { recommendationId: number; newStatus: 'APPLIED' | 'DISCARDED' }) =>
      registerRecommendationFeedback(recommendationId, newStatus),
    onSuccess: (updated) => {
      queryClient.setQueryData(recommendationsQueryKey, (current: typeof recommendations) =>
        current?.map((rec) => (rec.recommendationId === updated.recommendationId ? updated : rec)),
      )
      showSuccess(`Recomendación marcada como ${STATUS_LABELS[updated.status].toLowerCase()}.`)
    },
    onError: (err) => showError(getErrorMessage(err)),
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setAppliedFilters({ storeId, status })
  }

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Recomendaciones
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Recomendaciones de compra persistidas, con su justificación. Marcá cada una como aplicada o descartada.
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

        <TextField select size="small" label="Estado" value={status} onChange={(event) => setStatus(event.target.value as RecommendationStatus | '')}>
          {STATUS_OPTIONS.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>

        <Button type="submit" variant="contained" disabled={isFetching}>
          {isFetching ? 'Buscando…' : 'Buscar'}
        </Button>

        <Button type="button" variant="outlined" onClick={() => recalculateMutation.mutate()} disabled={recalculateMutation.isPending}>
          {recalculateMutation.isPending ? 'Recalculando…' : 'Recalcular recomendaciones'}
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {getErrorMessage(error)}
        </Alert>
      )}

      {!error && !isFetching && recommendations && recommendations.length === 0 && (
        <Alert severity="info">No hay recomendaciones para esta sucursal con estos filtros.</Alert>
      )}

      {recommendations && recommendations.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          {/*
            Sin ancho por columna: el ancho fijo por px llevó a un
            tira-y-afloja (angostar una columna desbordaba otra, ensanchar
            la tabla entera la sacaba del viewport y obligaba a scrollear
            para ver "Descartar"). table-layout: auto con whiteSpace: normal
            en las columnas de texto largo alcanza para que el navegador
            reparta el ancho solo, sin desbordar ni exigir scroll.
          */}
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>SKU</TableCell>
                <TableCell>Producto</TableCell>
                <TableCell>Cantidad sugerida</TableCell>
                <TableCell>Fecha límite</TableCell>
                <TableCell align="center">Justificación</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Acciones</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {recommendations.map((rec) => (
                <TableRow key={rec.recommendationId}>
                  <TableCell>{rec.sku}</TableCell>
                  <TableCell sx={{ whiteSpace: 'normal' }}>{rec.productName}</TableCell>
                  <TableCell>{rec.suggestedQuantity}</TableCell>
                  <TableCell>{rec.orderDeadlineDate}</TableCell>
                  <TableCell align="center">
                    <Tooltip title="Ver justificación">
                      <IconButton
                        size="small"
                        aria-label={`Ver justificación de ${rec.productName}`}
                        onClick={() =>
                          setJustificationDialog({
                            sku: rec.sku,
                            productName: rec.productName,
                            justification: rec.justification,
                          })
                        }
                      >
                        <InfoOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                  <TableCell>
                    <StatusChip statusKey={rec.status.toLowerCase()} label={STATUS_LABELS[rec.status]} />
                  </TableCell>
                  <TableCell align="right">
                    {rec.status === 'PENDING' ? (
                      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                        <Button
                          size="small"
                          variant="contained"
                          onClick={() => feedbackMutation.mutate({ recommendationId: rec.recommendationId, newStatus: 'APPLIED' })}
                          disabled={feedbackMutation.isPending && feedbackMutation.variables?.recommendationId === rec.recommendationId}
                          sx={{ width: 100 }}
                        >
                          Aplicar
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => feedbackMutation.mutate({ recommendationId: rec.recommendationId, newStatus: 'DISCARDED' })}
                          disabled={feedbackMutation.isPending && feedbackMutation.variables?.recommendationId === rec.recommendationId}
                          sx={{ width: 100 }}
                        >
                          Descartar
                        </Button>
                      </Stack>
                    ) : (
                      rec.feedbackDate ?? '—'
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={justificationDialog !== null} onClose={() => setJustificationDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {justificationDialog?.sku} — {justificationDialog?.productName}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>{justificationDialog?.justification}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setJustificationDialog(null)}>Cerrar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
