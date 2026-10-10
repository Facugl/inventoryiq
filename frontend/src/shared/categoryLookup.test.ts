import { describe, it, expect } from 'vitest'
import type { Category } from '../api/types'
import { categoryLabel } from './categoryLookup'

const category = (categoryId: number, name: string): Category => ({
  categoryId,
  name,
  parentCategoryId: null,
  maxCoverageDaysThreshold: 20,
  defaultExtraCoverageDays: 3,
})

/** Lo usan Sobrestock, Críticos, Clasificación y Recomendaciones para resolver el nombre de la categoría de cada fila. */
describe('categoryLabel', () => {
  const categories = [category(1, 'Almacén Frío'), category(2, 'Lácteos')]

  it('devuelve el nombre de la categoría', () => {
    expect(categoryLabel(categories, 2)).toBe('Lácteos')
  })

  /**
   * El fallback existe para que una fila con una categoría que no vino en
   * el catálogo se siga pudiendo leer, en vez de mostrar "undefined".
   */
  it('cae a #id cuando la categoría no está en la lista', () => {
    expect(categoryLabel(categories, 99)).toBe('#99')
  })

  it('cae a #id con un catálogo vacío', () => {
    expect(categoryLabel([], 7)).toBe('#7')
  })
})
