import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { ApiError } from '../api/http'
import { getErrorMessage } from './apiError'

/**
 * El contrato de la propiedad `code` vive en el backend y el frontend lo
 * espeja a mano en MESSAGES_BY_CODE. Nada impedía que se desincronizaran:
 * agregar un código allá y olvidarse acá no rompe la compilación ni
 * ningún test — el usuario simplemente vuelve a ver el mensaje en inglés,
 * en silencio.
 *
 * Este test lee los archivos del backend y verifica que cada código que
 * puede emitir tenga traducción. No importa el módulo de traducciones: le
 * pregunta a getErrorMessage, que es la puerta real, usando un mensaje
 * centinela — si el código no está mapeado, lo devuelve tal cual.
 */

/**
 * Se resuelve desde cwd y no con `new URL(..., import.meta.url)` porque
 * Vite analiza esa forma en tiempo de build y trata el resultado como un
 * asset a importar: con una ruta armada en runtime termina intentando
 * transformar un .java. Se prueban las dos raíces posibles según desde
 * dónde se corra (frontend/ o la raíz del repo).
 */
function backendFile(relativePath: string): string {
  const candidates = [
    resolve(process.cwd(), '..', 'backend/src/main/java/com/inventoryiq', relativePath),
    resolve(process.cwd(), 'backend/src/main/java/com/inventoryiq', relativePath),
  ]

  for (const candidate of candidates) {
    try {
      return readFileSync(candidate, 'utf8')
    } catch {
      continue
    }
  }

  throw new Error(
    `No se pudo leer ${relativePath} del backend (probé: ${candidates.join(' | ')}). ` +
      'Si el backend se movió de lugar, actualizá esta ruta: el test existe para ' +
      'detectar que el contrato se desincronice, y sin los archivos no prueba nada.',
  )
}

/** Quita comentarios para no confundir con constantes las siglas en mayúscula del texto (HTTP, DTO, ADS). */
function stripComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '')
}

function domainErrorCodes(): string[] {
  const body = stripComments(backendFile('domain/exception/ErrorCode.java'))
  const enumBody = body.slice(body.indexOf('{') + 1, body.lastIndexOf('}'))
  return enumBody
    .split(/[,\n]/)
    .map((entry) => entry.trim())
    .filter((entry) => /^[A-Z][A-Z0-9_]*$/.test(entry))
}

function adapterErrorCodes(): string[] {
  const handler = stripComments(backendFile('adapters/in/rest/GlobalExceptionHandler.java'))
  return [...handler.matchAll(/String\s+[A-Z_]+\s*=\s*"([A-Z][A-Z0-9_]*)"/g)].map((match) => match[1])
}

describe('contrato de códigos de error con el backend', () => {
  const SENTINEL = '::mensaje del backend sin traducir::'

  it('encuentra los códigos del dominio en el enum del backend', () => {
    // Si el parseo deja de funcionar, el test de abajo pasaría vacío y no probaría nada.
    expect(domainErrorCodes()).toContain('PRODUCT_NOT_FOUND')
    expect(domainErrorCodes().length).toBeGreaterThanOrEqual(5)
  })

  it('encuentra el código que emite el adaptador', () => {
    expect(adapterErrorCodes()).toContain('VALIDATION_FAILED')
  })

  it('traduce todos los códigos que el backend puede emitir', () => {
    const untranslated = [...domainErrorCodes(), ...adapterErrorCodes()].filter((code) => {
      const message = getErrorMessage(new ApiError(SENTINEL, 400, undefined, code, 1))
      return message === SENTINEL
    })

    expect(
      untranslated,
      `estos códigos del backend no tienen traducción en shared/apiError.ts: ${untranslated.join(', ')}`,
    ).toEqual([])
  })
})
