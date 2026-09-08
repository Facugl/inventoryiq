import { ApiError, postForm } from './http'
import type { CsvFileType, IngestionSummary, RowRejection } from './types'

interface ThresholdProblemDetail {
  detail?: string
  totalRowsRead?: number
  rejectedCount?: number
  rejectionRatePercent?: number
  rejections?: RowRejection[]
}

function isThresholdProblem(details: unknown): details is ThresholdProblemDetail {
  return typeof details === 'object' && details !== null
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

  try {
    return await postForm<IngestionSummary>('/api/v1/csv-ingestions', formData)
  } catch (err) {
    if (err instanceof ApiError && err.status === 422 && isThresholdProblem(err.details)) {
      const problem = err.details
      throw new CsvIngestionThresholdError(
        problem.detail ?? 'Se superó el umbral de filas rechazadas.',
        problem.totalRowsRead ?? 0,
        problem.rejectedCount ?? 0,
        problem.rejectionRatePercent ?? 0,
        problem.rejections ?? [],
      )
    }
    throw err
  }
}
