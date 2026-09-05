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

/** Backend: application.port.in.CsvFileType. Único valor implementado: SALES. */
export type CsvFileType = 'SALES'

/** Forma de RowRejectionResponse (fila rechazada de una ingesta CSV). */
export interface RowRejection {
  rowNumber: number
  reason: string
}

/** Forma de IngestionSummaryResponse (POST /api/v1/csv-ingestions). */
export interface IngestionSummary {
  totalRowsRead: number
  acceptedCount: number
  rejectedCount: number
  rejections: RowRejection[]
}

/** Forma de RecalculateRecommendationsResponse, anidado en StoreRecalculationSummaryResponse. */
export interface RecalculateRecommendationsSummary {
  totalGenerated: number
  newCount: number
  updatedCount: number
  autoDiscardedCount: number
}

/** Forma de StoreRecalculationSummaryResponse, anidado en RecalculateProductStatusResponse. */
export interface StoreRecalculationSummary {
  storeId: number
  criticalProductsFound: number
  overstockProductsFound: number
  alertsGenerated: number
  recommendations: RecalculateRecommendationsSummary
}

/** Forma de RecalculateProductStatusResponse (POST /api/v1/product-status/recalculate). */
export interface RecalculateProductStatusResult {
  storesProcessed: number
  perStore: StoreRecalculationSummary[]
}

/** Sección 8.5/8.7 — estado de una recomendación de compra persistida (backend: domain.model.RecommendationStatus). */
export type RecommendationStatus = 'PENDING' | 'APPLIED' | 'DISCARDED'

/** Forma de RecommendationResponse (GET/PATCH /api/v1/recommendations). */
export interface Recommendation {
  recommendationId: number
  productId: number
  sku: string
  productName: string
  storeId: number
  categoryId: number
  supplierId: number | null
  suggestedQuantity: number
  orderDeadlineDate: string
  justification: string
  status: RecommendationStatus
  generationDate: string
  feedbackComment: string | null
  feedbackDate: string | null
}

/** Backend: application.port.in.AlertType. */
export type AlertType = 'STOCKOUT' | 'OVERSTOCK'

/** Backend: application.port.in.AlertSeverity. */
export type AlertSeverity = 'HIGH' | 'MEDIUM' | 'LOW'

/**
 * Forma de AlertResponse (GET /api/v1/alerts). Pura composición de
 * GetCriticalProductsUseCase (STOCKOUT) y DetectOverstockUseCase
 * (OVERSTOCK) — no hay ningún dato nuevo que este endpoint calcule.
 */
export interface Alert {
  productId: number
  sku: string
  productName: string
  storeId: number
  categoryId: number
  type: AlertType
  severity: AlertSeverity
  generatedAt: string
}
