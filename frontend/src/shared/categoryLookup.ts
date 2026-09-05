import type { Category } from '../api/types'

/** Nombre de la categoría, o "#<id>" como fallback si no está en la lista (no debería pasar). */
export function categoryLabel(categories: Category[], categoryId: number): string {
  return categories.find((category) => category.categoryId === categoryId)?.name ?? `#${categoryId}`
}
