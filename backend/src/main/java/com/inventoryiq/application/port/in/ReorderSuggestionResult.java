package com.inventoryiq.application.port.in;

import java.time.LocalDate;

/**
 * Salida de GenerateReorderSuggestionsUseCase — Sección 8.5: producto,
 * cantidad sugerida, proveedor, fecha límite de emisión y justificación.
 * supplierId se expone como ID crudo, sin resolver el nombre del
 * proveedor (mismo criterio que categoryId en el resto de los DTOs de
 * este proyecto: no hay SupplierRepository todavía, y no hace falta
 * agregarlo solo para mostrar un nombre).
 */
public record ReorderSuggestionResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		Long supplierId,
		int suggestedQuantity,
		LocalDate orderDeadlineDate,
		String justification) {
}
