import { fetchJson } from './http'
import type { ProductClassification } from './types'

export interface GetProductClassificationParams {
  storeId: number
  /** ISO yyyy-MM-dd. Obligatorio, ver ProductClassificationController. */
  referenceDate: string
  categoryId?: number
}

export function getProductClassification(params: GetProductClassificationParams): Promise<ProductClassification[]> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    referenceDate: params.referenceDate,
  })
  if (params.categoryId !== undefined) query.set('categoryId', String(params.categoryId))

  return fetchJson<ProductClassification[]>(`/api/v1/products/classification?${query.toString()}`)
}
