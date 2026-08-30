package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada — Sección 2.3.2 / 9.6. Consolida en formato de alerta
 * los productos en estado Crítico/Requiere Reposición (GetCriticalProductsUseCase)
 * y Sobrestock (DetectOverstockUseCase).
 */
public interface GenerateAlertsUseCase {

	List<AlertResult> execute(GenerateAlertsQuery query);
}
