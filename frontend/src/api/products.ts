import { fetchJson } from './http'
import type { ProductSummary } from './types'

export function searchProducts(query: string): Promise<ProductSummary[]> {
  return fetchJson<ProductSummary[]>(`/api/v1/products?q=${encodeURIComponent(query)}`)
}
