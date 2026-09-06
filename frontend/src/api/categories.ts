import { fetchJson, patchJson } from './http'
import type { Category } from './types'

export function getCategories(): Promise<Category[]> {
  return fetchJson<Category[]>('/api/v1/categories')
}

export interface UpdateCategoryParametersParams {
  maxCoverageDaysThreshold: number
  defaultExtraCoverageDays: number
}

export function updateCategoryParameters(categoryId: number, params: UpdateCategoryParametersParams): Promise<Category> {
  return patchJson(`/api/v1/categories/${categoryId}/parameters`, params)
}
