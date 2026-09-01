package com.inventoryiq.application.port.in;

/**
 * Puerto de entrada — Sección 9.10 (job programado). Orquesta, por cada
 * sucursal en alcance, GetCriticalProductsUseCase, DetectOverstockUseCase,
 * RecalculateRecommendationsUseCase (que internamente corre
 * GenerateReorderSuggestionsUseCase y persiste) y GenerateAlertsUseCase,
 * en ese orden — el mismo que describe el algoritmo de la Sección 9.10.
 */
public interface RecalculateProductStatusUseCase {

	RecalculateProductStatusResult execute(RecalculateProductStatusCommand command);
}
