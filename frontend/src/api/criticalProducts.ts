import { fetchJson } from './http'
import type { CriticalProduct } from './types'

export interface GetCriticalProductsParams {
  storeId: number
  /** ISO yyyy-MM-dd. Obligatorio: el backend no asume "hoy", ver CriticalProductsController. */
  referenceDate: string
  categoryId?: number
  limit?: number
}

export function getCriticalProducts(params: GetCriticalProductsParams): Promise<CriticalProduct[]> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    referenceDate: params.referenceDate,
  })
  if (params.categoryId !== undefined) query.set('categoryId', String(params.categoryId))
  if (params.limit !== undefined) query.set('limit', String(params.limit))

  return fetchJson<CriticalProduct[]>(`/api/v1/products/critical?${query.toString()}`)
}
