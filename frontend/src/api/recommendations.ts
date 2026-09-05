import { fetchJson, patchJson, postJson } from './http'
import type { Recommendation, RecalculateRecommendationsSummary, RecommendationStatus } from './types'

export interface GetRecommendationsParams {
  storeId: number
  status?: RecommendationStatus
}

export function getRecommendations(params: GetRecommendationsParams): Promise<Recommendation[]> {
  const query = new URLSearchParams({ storeId: String(params.storeId) })
  if (params.status !== undefined) query.set('status', params.status)

  return fetchJson<Recommendation[]>(`/api/v1/recommendations?${query.toString()}`)
}

export function recalculateRecommendations(storeId: number): Promise<RecalculateRecommendationsSummary> {
  return postJson('/api/v1/recommendations/recalculate', { storeId })
}

/** status debe ser APPLIED o DISCARDED — PENDING no es un feedback válido (Sección 8.7). */
export function registerRecommendationFeedback(
  recommendationId: number,
  status: Extract<RecommendationStatus, 'APPLIED' | 'DISCARDED'>,
): Promise<Recommendation> {
  return patchJson(`/api/v1/recommendations/${recommendationId}`, { status })
}
