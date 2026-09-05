import { fetchJson } from './http'
import type { Store } from './types'

interface StoreResponse {
  storeId: number
  name: string
}

export function getStores(): Promise<Store[]> {
  return fetchJson<StoreResponse[]>('/api/v1/stores').then((rows) =>
    rows.map((row) => ({ id: row.storeId, name: row.name })),
  )
}
