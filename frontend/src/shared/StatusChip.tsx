import { Chip } from '@mui/material'

/**
 * Mismo lenguaje de color que usaban las clases .status-* (product-status.css):
 * ProductStatus, RecommendationStatus, AlertSeverity, AbcClassification y
 * XyzClassification comparten esta paleta por significado (rojo = crítico/alto,
 * ámbar = atención, verde = ok, azul = sobrestock/estable, gris = neutro/bajo).
 */
const STATUS_COLORS: Record<string, { bg: string; color: string }> = {
  critical: { bg: 'rgba(220, 38, 38, 0.15)', color: '#dc2626' },
  'requires-replenishment': { bg: 'rgba(217, 119, 6, 0.15)', color: '#d97706' },
  normal: { bg: 'rgba(22, 163, 74, 0.15)', color: '#16a34a' },
  overstock: { bg: 'rgba(37, 99, 235, 0.15)', color: '#2563eb' },
  'low-rotation': { bg: 'rgba(107, 114, 128, 0.15)', color: '#6b7280' },
  pending: { bg: 'rgba(217, 119, 6, 0.15)', color: '#d97706' },
  applied: { bg: 'rgba(22, 163, 74, 0.15)', color: '#16a34a' },
  discarded: { bg: 'rgba(107, 114, 128, 0.15)', color: '#6b7280' },
  high: { bg: 'rgba(220, 38, 38, 0.15)', color: '#dc2626' },
  medium: { bg: 'rgba(217, 119, 6, 0.15)', color: '#d97706' },
  low: { bg: 'rgba(107, 114, 128, 0.15)', color: '#6b7280' },
  a: { bg: 'rgba(22, 163, 74, 0.15)', color: '#16a34a' },
  b: { bg: 'rgba(217, 119, 6, 0.15)', color: '#d97706' },
  c: { bg: 'rgba(107, 114, 128, 0.15)', color: '#6b7280' },
  x: { bg: 'rgba(37, 99, 235, 0.15)', color: '#2563eb' },
  y: { bg: 'rgba(217, 119, 6, 0.15)', color: '#d97706' },
  z: { bg: 'rgba(220, 38, 38, 0.15)', color: '#dc2626' },
}

interface StatusChipProps {
  /** Clave en minúsculas con guiones, ej. "requires-replenishment" (antes: statusClassName). */
  statusKey: string
  label: string
}

export function StatusChip({ statusKey, label }: StatusChipProps) {
  const colors = STATUS_COLORS[statusKey] ?? { bg: 'action.selected', color: 'text.primary' }

  return (
    <Chip
      label={label}
      size="small"
      sx={{ bgcolor: colors.bg, color: colors.color, fontWeight: 500 }}
    />
  )
}
