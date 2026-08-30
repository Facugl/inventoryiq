package com.inventoryiq.adapters.in.rest.dto;

import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.XyzClassification;

/**
 * Forma JSON pública de la clasificación de un producto (GET /api/v1/products/classification).
 * abcClass/xyzClass reutilizan los enums de dominio directamente (mismo
 * criterio que ProductStatus en CriticalProductResponse): un adaptador
 * puede depender de domain, la dirección permitida.
 */
public record ProductClassificationResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		AbcClassification abcClass,
		XyzClassification xyzClass) {
}
