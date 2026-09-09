import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
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
import { getSuppliers, updateSupplierLeadTime } from '../../api/suppliers'
import { getErrorMessage } from '../../shared/apiError'
import { showSuccess } from '../../shared/toast'
import type { Supplier } from '../../api/types'
import { leadTimeSchema, type LeadTimeFormValues } from './validation/leadTimeSchema'

interface LeadTimeEditRowProps {
  supplier: Supplier
  onCancel: () => void
  onSaved: () => void
}

function LeadTimeEditRow({ supplier, onCancel, onSaved }: LeadTimeEditRowProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LeadTimeFormValues>({
    resolver: yupResolver(leadTimeSchema),
    defaultValues: { leadTimeDays: supplier.leadTimeDays },
  })

  const mutation = useMutation({
    mutationFn: (values: LeadTimeFormValues) => updateSupplierLeadTime(supplier.supplierId, values.leadTimeDays),
    onSuccess: (updated) => {
      queryClient.setQueryData<Supplier[]>(['suppliers'], (current) =>
        current?.map((s) => (s.supplierId === updated.supplierId ? updated : s)),
      )
      showSuccess(`Lead time de "${updated.businessName}" actualizado.`)
      onSaved()
    },
  })

  const rowError = errors.leadTimeDays?.message ?? (mutation.error ? getErrorMessage(mutation.error) : null)

  return (
    <>
      <TableCell>
        <TextField
          type="number"
          size="small"
          autoFocus
          slotProps={{ htmlInput: { min: 1 } }}
          error={Boolean(rowError)}
          helperText={rowError}
          sx={{ width: 110 }}
          {...register('leadTimeDays')}
        />
      </TableCell>
      <TableCell>{supplier.paymentTerms}</TableCell>
      <TableCell>
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            variant="contained"
            onClick={handleSubmit((values) => mutation.mutate(values))}
            disabled={mutation.isPending}
          >
            {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </Button>
          <Button size="small" variant="outlined" onClick={onCancel} disabled={mutation.isPending}>
            Cancelar
          </Button>
        </Stack>
      </TableCell>
    </>
  )
}

export function SuppliersPage() {
  const [editingId, setEditingId] = useState<number | null>(null)

  const {
    data: suppliers,
    error: loadError,
  } = useQuery({ queryKey: ['suppliers'], queryFn: getSuppliers })

  return (
    <Box component="main" sx={{ p: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Proveedores
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 720 }}>
        Catálogo de proveedores activos, con su lead time y condición de pago. El lead time es editable, ya que
        se va afinando con la práctica.
      </Typography>

      {loadError && <Alert severity="error">{getErrorMessage(loadError)}</Alert>}

      {!loadError && !suppliers && <CircularProgress size={24} />}

      {suppliers && suppliers.length === 0 && <Alert severity="info">No hay proveedores activos cargados.</Alert>}

      {suppliers && suppliers.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Razón social</TableCell>
                <TableCell>Lead time (días)</TableCell>
                <TableCell>Condición de pago</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {suppliers.map((supplier) => (
                <TableRow key={supplier.supplierId}>
                  <TableCell>{supplier.businessName}</TableCell>
                  {editingId === supplier.supplierId ? (
                    <LeadTimeEditRow
                      supplier={supplier}
                      onCancel={() => setEditingId(null)}
                      onSaved={() => setEditingId(null)}
                    />
                  ) : (
                    <>
                      <TableCell>{supplier.leadTimeDays}</TableCell>
                      <TableCell>{supplier.paymentTerms}</TableCell>
                      <TableCell>
                        <Button size="small" onClick={() => setEditingId(supplier.supplierId)}>
                          Editar lead time
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
