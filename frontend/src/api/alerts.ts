import { fetchJson } from './http'
import type { Alert, AlertSeverity, AlertType } from './types'

export interface GetAlertsParams {
  storeId: number
  /** ISO yyyy-MM-dd. Obligatorio, ver AlertsController. */
  referenceDate: string
  type?: AlertType
  severity?: AlertSeverity
}

export function getAlerts(params: GetAlertsParams): Promise<Alert[]> {
  const query = new URLSearchParams({
    storeId: String(params.storeId),
    referenceDate: params.referenceDate,
  })
  if (params.type !== undefined) query.set('type', params.type)
  if (params.severity !== undefined) query.set('severity', params.severity)

  return fetchJson<Alert[]>(`/api/v1/alerts?${query.toString()}`)
}
