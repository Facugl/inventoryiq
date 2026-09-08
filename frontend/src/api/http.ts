import axios, { type AxiosError } from 'axios'

/** Los errores del backend llegan como ProblemDetail (RFC 7807, ver GlobalExceptionHandler). */
interface ProblemDetail {
  detail?: string
}

export class ApiError extends Error {
  readonly status: number
  /** Body crudo de la respuesta de error, para los pocos casos (ver csvIngestion.ts) que necesitan más que `detail`. */
  readonly details?: unknown

  constructor(message: string, status: number, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

/**
 * Instancia central de axios. Sin baseURL: las rutas relativas (`/api/v1/...`)
 * se resuelven contra el mismo origen, tal como las servía `fetch` antes —
 * en dev, el proxy de Vite (vite.config.ts) las reenvía al backend.
 *
 * El interceptor de request queda como punto de extensión: hoy no hace nada
 * porque el backend no tiene autenticación (ver docs/InventoryIQ_Arquitectura.md),
 * pero es donde se adjuntaría un `Authorization: Bearer` el día que la tenga.
 */
export const apiClient = axios.create()

apiClient.interceptors.request.use((config) => config)

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetail>) => {
    if (error.response) {
      const detail = error.response.data?.detail
      return Promise.reject(
        new ApiError(
          detail ?? `Error ${error.response.status} al consultar ${error.config?.url}`,
          error.response.status,
          error.response.data,
        ),
      )
    }
    return Promise.reject(new ApiError('No se pudo conectar con el servidor.', 0))
  },
)

export async function fetchJson<T>(path: string): Promise<T> {
  const response = await apiClient.get<T>(path)
  return response.data
}

async function sendJson<T>(method: 'post' | 'patch', path: string, body?: unknown): Promise<T> {
  const response = await apiClient.request<T>({ method, url: path, data: body })
  return response.data
}

export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return sendJson('post', path, body)
}

export function patchJson<T>(path: string, body?: unknown): Promise<T> {
  return sendJson('patch', path, body)
}

export async function postForm<T>(path: string, formData: FormData): Promise<T> {
  const response = await apiClient.post<T>(path, formData)
  return response.data
}
