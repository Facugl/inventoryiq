import type { ProductStatus } from '../api/types'

/** Sección 4.12 de la documentación — máquina de estados de un producto. */
export const STATUS_LABELS: Record<ProductStatus, string> = {
  NORMAL: 'Normal',
  REQUIRES_REPLENISHMENT: 'Requiere reposición',
  CRITICAL: 'Crítico',
  OVERSTOCK: 'Sobrestock',
  LOW_ROTATION: 'Baja rotación',
}

export function statusClassName(status: ProductStatus): string {
  return `status status-${status.toLowerCase().replaceAll('_', '-')}`
}
