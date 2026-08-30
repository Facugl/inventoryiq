package com.inventoryiq.application.port.in;

import java.math.BigDecimal;

/** Salida de DetectOverstockUseCase — Sección 8.4: producto, cobertura actual y valor de inventario inmovilizado. */
public record OverstockProductResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		int currentStock,
		double currentDaysOfCoverage,
		BigDecimal immobilizedValue) {
}
