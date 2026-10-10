import { describe, it, expect } from 'vitest'
import { ApiError } from '../api/http'
import { getErrorMessage } from './apiError'

/**
 * getErrorMessage es el embudo único por el que pasa todo error antes de
 * llegar a una pantalla (lo usan ~15 call sites). Su contrato tiene dos
 * mitades: traducir los códigos que el backend define, y no romperse con
 * los que no.
 */
describe('getErrorMessage', () => {
  it('traduce un código conocido usando el resourceId que vino con el error', () => {
    const err = new ApiError('Product not found: 9999', 404, undefined, 'PRODUCT_NOT_FOUND', 9999)

    expect(getErrorMessage(err)).toBe('No se encontró el producto 9999.')
  })

  it('traduce un código que no usa el resourceId', () => {
    const err = new ApiError('fromDate cannot be after toDate', 400, undefined, 'DATE_RANGE_INVALID')

    expect(getErrorMessage(err)).toBe('La fecha "Desde" no puede ser posterior a la fecha "Hasta".')
  })

  /**
   * El mensaje del backend está en inglés a propósito (es material de log),
   * así que el fallback es el último recurso y no el camino normal. Se
   * verifica igual porque es lo que sostiene a un código sin traducir.
   */
  it('cae al mensaje del backend cuando el código no está mapeado', () => {
    const err = new ApiError('Something went sideways', 400, undefined, 'CODIGO_QUE_NO_EXISTE')

    expect(getErrorMessage(err)).toBe('Something went sideways')
  })

  it('cae al mensaje del backend cuando la respuesta no trae código', () => {
    const err = new ApiError('El archivo no tiene las columnas esperadas para ventas', 400)

    expect(getErrorMessage(err)).toBe('El archivo no tiene las columnas esperadas para ventas')
  })

  it('devuelve un mensaje de conexión cuando el error no es un ApiError', () => {
    expect(getErrorMessage(new Error('socket hang up'))).toBe('No se pudo conectar con el servidor.')
    expect(getErrorMessage(undefined)).toBe('No se pudo conectar con el servidor.')
  })

  /** Ningún texto que pueda ver un usuario debería estar en inglés. */
  it('no deja mensajes traducidos en inglés', () => {
    const codes: [string, unknown][] = [
      ['PRODUCT_NOT_FOUND', 1],
      ['STORE_NOT_FOUND', 1],
      ['SUPPLIER_NOT_FOUND', 1],
      ['CATEGORY_NOT_FOUND', 1],
      ['RECOMMENDATION_NOT_FOUND', 1],
      ['DATE_RANGE_INVALID', undefined],
      ['VALIDATION_FAILED', undefined],
    ]

    for (const [code, resourceId] of codes) {
      const message = getErrorMessage(new ApiError('backend message in english', 400, undefined, code, resourceId))

      expect(message, `el código ${code} no está traducido`).not.toBe('backend message in english')
      expect(message).toMatch(/[áéíóúñ¿]|^(No|La|El|Hay)\b/)
    }
  })
})
