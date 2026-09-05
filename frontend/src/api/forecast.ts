import { fetchJson } from './http'
import type { ProductForecast } from './types'

export interface GetProductForecastParams {
  productId: number
  storeId: number
  /** ISO yyyy-MM-dd. Obligatorio, ver ForecastController. */
  referenceDate: string
  horizonDays: number
}

export function getProductForecast(params: GetProductForecastParams): Promise<ProductForecast> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    referenceDate: params.referenceDate,
    horizonDays: String(params.horizonDays),
  })

  return fetchJson<ProductForecast>(`/api/v1/products/${params.productId}/forecast?${query.toString()}`)
}
