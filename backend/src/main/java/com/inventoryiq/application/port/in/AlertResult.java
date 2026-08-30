package com.inventoryiq.application.port.in;

import java.time.LocalDate;

/** Salida de GenerateAlertsUseCase — Sección 8.14: producto, tipo, severidad y fecha de generación. */
public record AlertResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		AlertType type,
		AlertSeverity severity,
		LocalDate generatedAt) {
}
