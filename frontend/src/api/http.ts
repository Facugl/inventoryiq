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

async function sendJson<T>(method: 'POST' | 'PATCH', path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    const problem: ProblemDetail | null = await response.json().catch(() => null)
    throw new ApiError(problem?.detail ?? `Error ${response.status} al consultar ${path}`, response.status)
  }

  return response.json() as Promise<T>
}

export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return sendJson('POST', path, body)
}

export function patchJson<T>(path: string, body?: unknown): Promise<T> {
  return sendJson('PATCH', path, body)
}
