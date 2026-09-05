import { fetchJson } from './http'
import type { InventoryKpis } from './types'

export interface GetInventoryKpisParams {
  storeId: number
  /** ISO yyyy-MM-dd. Ambos obligatorios, ver KPIsController. */
  fromDate: string
  toDate: string
}

export function getInventoryKpis(params: GetInventoryKpisParams): Promise<InventoryKpis> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    fromDate: params.fromDate,
    toDate: params.toDate,
  })

  return fetchJson<InventoryKpis>(`/api/v1/kpis?${query.toString()}`)
}
