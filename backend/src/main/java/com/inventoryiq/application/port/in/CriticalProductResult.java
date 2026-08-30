package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;

/** Salida de GetCriticalProductsUseCase — Sección 8.3: producto, stock actual, punto de pedido, días de cobertura y score. */
public record CriticalProductResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		int currentStock,
		ReorderPoint reorderPoint,
		double currentDaysOfCoverage,
		ProductStatus status,
		CriticalityLevel criticalityLevel) {
}
