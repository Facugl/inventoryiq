import { fetchJson } from './http'
import type { OverstockProduct, OverstockSortBy } from './types'

export interface GetOverstockProductsParams {
  storeId: number
  /** ISO yyyy-MM-dd. Obligatorio, ver OverstockController. */
  referenceDate: string
  categoryId?: number
  sortBy?: OverstockSortBy
}

export function getOverstockProducts(params: GetOverstockProductsParams): Promise<OverstockProduct[]> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    referenceDate: params.referenceDate,
  })
  if (params.categoryId !== undefined) query.set('categoryId', String(params.categoryId))
  if (params.sortBy !== undefined) query.set('sortBy', params.sortBy)

  return fetchJson<OverstockProduct[]>(`/api/v1/products/overstock?${query.toString()}`)
}
