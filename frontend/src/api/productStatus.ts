import { postJson } from './http'
import type { RecalculateProductStatusResult } from './types'

/** storeId undefined → recalcula todas las sucursales activas (Sección 9.10). */
export function recalculateProductStatus(storeId?: number): Promise<RecalculateProductStatusResult> {
  return postJson('/api/v1/product-status/recalculate', storeId !== undefined ? { storeId } : undefined)
}
