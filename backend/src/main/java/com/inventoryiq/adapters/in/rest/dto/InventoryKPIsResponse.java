package com.inventoryiq.adapters.in.rest.dto;

import java.math.BigDecimal;

/** Forma JSON pública de los KPIs de inventario (GET /api/v1/kpis). */
public record InventoryKPIsResponse(
		Double stockoutRate,
		Double averageDaysOfCoverage,
		BigDecimal immobilizedOverstockValue,
		Double recommendationsFollowedRate,
		Double inventoryTurnover) {
}
