/** Los errores del backend llegan como ProblemDetail (RFC 7807, ver GlobalExceptionHandler). */
interface ProblemDetail {
  detail?: string
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function fetchJson<T>(path: string): Promise<T> {
  const response = await fetch(path)

  if (!response.ok) {
    const problem: ProblemDetail | null = await response.json().catch(() => null)
    throw new ApiError(problem?.detail ?? `Error ${response.status} al consultar ${path}`, response.status)
  }

  return response.json() as Promise<T>
}
