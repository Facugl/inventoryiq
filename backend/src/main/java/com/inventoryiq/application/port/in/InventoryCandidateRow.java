package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.model.Inventory;

/**
 * Una fila de un CSV de inventario ingerido que ya pasó el parseo
 * estructural y de negocio del dominio, pendiente de validación
 * referencial. Análogo a CandidateRow, que cubre el caso de ventas.
 *
 * Es un tipo aparte y no un CandidateRow genérico para no tocar el
 * camino de ventas ya implementado: el snapshot todavía no tiene
 * inventoryId (lo asigna el adaptador de persistencia al guardarlo).
 */
public record InventoryCandidateRow(int rowNumber, Inventory inventory) {
}
