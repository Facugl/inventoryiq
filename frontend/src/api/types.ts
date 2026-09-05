/** Sección 4.12 de la documentación — máquina de estados de un producto (backend: domain.model.ProductStatus). */
export type ProductStatus = 'NORMAL' | 'REQUIRES_REPLENISHMENT' | 'CRITICAL' | 'OVERSTOCK' | 'LOW_ROTATION'

/** Forma de CriticalProductResponse (GET /api/v1/products/critical). */
export interface CriticalProduct {
  productId: number
  sku: string
  productName: string
  storeId: number
  categoryId: number
  currentStock: number
  reorderPointUnits: number
  currentDaysOfCoverage: number
  status: ProductStatus
  criticalityScore: number
}
