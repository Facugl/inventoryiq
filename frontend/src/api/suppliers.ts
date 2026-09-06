import { fetchJson, patchJson } from './http'
import type { Supplier } from './types'

export function getSuppliers(): Promise<Supplier[]> {
  return fetchJson<Supplier[]>('/api/v1/suppliers')
}

export function updateSupplierLeadTime(supplierId: number, leadTimeDays: number): Promise<Supplier> {
  return patchJson(`/api/v1/suppliers/${supplierId}/lead-time`, { leadTimeDays })
}
