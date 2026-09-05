/**
 * Hoy no existe un endpoint que liste sucursales (ver docs/InventoryIQ_Arquitectura.md,
 * Sección 1.3: ningún controller expone StoreRepository). Hardcodeado a partir de
 * data/csv/sucursales.csv hasta que se implemente uno.
 */
export interface Store {
  id: number
  name: string
}

export const STORES: Store[] = [
  { id: 1, name: 'Sucursal Centro' },
  { id: 2, name: 'Sucursal Norte' },
  { id: 3, name: 'Sucursal Sur' },
]
