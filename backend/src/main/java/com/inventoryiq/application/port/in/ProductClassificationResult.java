package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.XyzClassification;

/** Salida de ClassifyProductsUseCase — Sección 9.5: clasificación ABC (valor de venta) y XYZ (variabilidad de demanda). */
public record ProductClassificationResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		AbcClassification abcClass,
		XyzClassification xyzClass) {
}
