import { ApiError } from '../api/http'

/** El interceptor de apiClient normaliza todo error de red/HTTP a ApiError (ver api/http.ts). */
export function getErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}
