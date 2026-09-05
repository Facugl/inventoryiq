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

/** Backend: application.port.in.OverstockSortBy. */
export type OverstockSortBy = 'IMMOBILIZED_VALUE' | 'DAYS_OF_COVERAGE'

/** Forma de OverstockProductResponse (GET /api/v1/products/overstock). Todo resultado está, por definición, en Sobrestock. */
export interface OverstockProduct {
  productId: number
  sku: string
  productName: string
  storeId: number
  categoryId: number
  currentStock: number
  currentDaysOfCoverage: number
  /** BigDecimal en el backend; llega como número JSON. */
  immobilizedValue: number
}

/**
 * Forma de InventoryKPIsResponse (GET /api/v1/kpis). Los campos nullable
 * (todos salvo immobilizedOverstockValue) son null cuando no hay datos
 * suficientes en el período — no significan cero, ver CalculateInventoryKPIsService.
 */
export interface InventoryKpis {
  stockoutRate: number | null
  averageDaysOfCoverage: number | null
  immobilizedOverstockValue: number
  recommendationsFollowedRate: number | null
  inventoryTurnover: number | null
}

/** Forma de DemandForecastPeriodResponse, anidado en ForecastDemandResponse. */
export interface DemandForecastPeriod {
  periodStart: string
  periodEnd: string
  seasonalIndex: number
  projectedDailyAds: number
  projectedTotalDemand: number
}

/**
 * Forma de ForecastDemandResponse (GET /api/v1/products/{productId}/forecast).
 * No es una "ficha de producto" completa (la Sección 8.2 de InventoryIQ_Documentacion.md
 * describe eso, pero no está implementado) — es solo la proyección de demanda.
 * baseAds es null y periods vacío cuando el producto existe pero no tiene
 * historial de ventas suficiente para proyectar (ver ForecastDemandService).
 */
export interface ProductForecast {
  productId: number
  sku: string
  productName: string
  storeId: number
  baseAds: number | null
  periods: DemandForecastPeriod[]
}
