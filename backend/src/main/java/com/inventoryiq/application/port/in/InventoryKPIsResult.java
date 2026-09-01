package com.inventoryiq.application.port.in;

import java.math.BigDecimal;

/**
 * Salida de CalculateInventoryKPIsUseCase (Sección 3.4/8.8/9.7).
 *
 * Todos los campos son nullables salvo immobilizedOverstockValue: cada
 * uno vale null cuando no hay datos suficientes para calcularlo en el
 * alcance/período pedido (por ejemplo, 0 productos con historial de
 * ventas, o 0 recomendaciones generadas en el rango) — 0 sobrestock, en
 * cambio, es una respuesta válida, no "sin datos".
 */
public record InventoryKPIsResult(
		Double stockoutRate,
		Double averageDaysOfCoverage,
		BigDecimal immobilizedOverstockValue,
		Double recommendationsFollowedRate,
		Double inventoryTurnover) {
}
