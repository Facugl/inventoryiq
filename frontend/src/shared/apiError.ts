import { ApiError } from '../api/http'

/**
 * Espejo de domain.exception.ErrorCode del backend. Las claves se
 * serializan literales en la propiedad `code` de la respuesta de error,
 * así que renombrar una del lado del backend obliga a tocar esto.
 */
const MESSAGES_BY_CODE: Record<string, (resourceId: unknown) => string> = {
  PRODUCT_NOT_FOUND: (id) => `No se encontró el producto ${id}.`,
  STORE_NOT_FOUND: (id) => `No se encontró la sucursal ${id}.`,
  SUPPLIER_NOT_FOUND: (id) => `No se encontró el proveedor ${id}.`,
  CATEGORY_NOT_FOUND: (id) => `No se encontró la categoría ${id}.`,
  RECOMMENDATION_NOT_FOUND: (id) => `No se encontró la recomendación ${id}.`,
}

/**
 * Único punto donde un error se convierte en texto para el usuario.
 *
 * Cuando el backend manda un `code`, el mensaje se arma acá en español.
 * El `message` del ApiError (que viene del `detail`) queda como fallback,
 * pero está en inglés —como todas las excepciones de dominio del
 * backend—, así que mostrarlo es el último recurso, no el camino normal:
 * un código sin entrada en este mapa es una traducción que falta.
 */
export function getErrorMessage(err: unknown): string {
  if (!(err instanceof ApiError)) return 'No se pudo conectar con el servidor.'

  const translate = err.code ? MESSAGES_BY_CODE[err.code] : undefined
  return translate ? translate(err.resourceId) : err.message
}
