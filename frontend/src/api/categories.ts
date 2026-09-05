import { fetchJson } from './http'
import type { Category } from './types'

export function getCategories(): Promise<Category[]> {
  return fetchJson<Category[]>('/api/v1/categories')
}
