package com.inventoryiq.adapters.in.rest.dto;

import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;

import java.time.LocalDate;

/** Forma JSON pública de una alerta (GET /api/v1/alerts). */
public record AlertResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		AlertType type,
		AlertSeverity severity,
		LocalDate generatedAt) {
}
