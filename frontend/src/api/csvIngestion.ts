import { ApiError } from './http'
import type { CsvFileType, IngestionSummary, RowRejection } from './types'

interface ThresholdProblemDetail {
  detail?: string
  totalRowsRead?: number
  rejectedCount?: number
  rejectionRatePercent?: number
  rejections?: RowRejection[]
}

/**
 * Se lanza cuando el % de filas rechazadas supera el umbral crítico (Sección
 * 7.4, ver CsvIngestionThresholdExceededException) — a diferencia de un
 * ApiError genérico, trae el detalle completo del lote para mostrarlo.
 */
export class CsvIngestionThresholdError extends ApiError {
  readonly totalRowsRead: number
  readonly rejectedCount: number
  readonly rejectionRatePercent: number
  readonly rejections: RowRejection[]

  constructor(
    message: string,
    totalRowsRead: number,
    rejectedCount: number,
    rejectionRatePercent: number,
    rejections: RowRejection[],
  ) {
    super(message, 422)
    this.name = 'CsvIngestionThresholdError'
    this.totalRowsRead = totalRowsRead
    this.rejectedCount = rejectedCount
    this.rejectionRatePercent = rejectionRatePercent
    this.rejections = rejections
  }
}

export async function ingestCsvFile(fileType: CsvFileType, file: File): Promise<IngestionSummary> {
  const formData = new FormData()
  formData.append('fileType', fileType)
  formData.append('file', file)

  const response = await fetch('/api/v1/csv-ingestions', { method: 'POST', body: formData })

  if (!response.ok) {
    const problem: ThresholdProblemDetail | null = await response.json().catch(() => null)

    if (response.status === 422 && problem) {
      throw new CsvIngestionThresholdError(
        problem.detail ?? 'Se superó el umbral de filas rechazadas.',
        problem.totalRowsRead ?? 0,
        problem.rejectedCount ?? 0,
        problem.rejectionRatePercent ?? 0,
        problem.rejections ?? [],
      )
    }

    throw new ApiError(problem?.detail ?? `Error ${response.status} al subir el archivo`, response.status)
  }

  return response.json() as Promise<IngestionSummary>
}
