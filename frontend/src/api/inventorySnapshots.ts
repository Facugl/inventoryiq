import { postJson } from './http'
import type { InventorySnapshot } from './types'

export interface RecordInventoryCountParams {
  productId: number
  storeId: number
  stockActual: number
}

export function recordInventoryCount(params: RecordInventoryCountParams): Promise<InventorySnapshot> {
  return postJson('/api/v1/inventory-snapshots', params)
}
