package com.inventoryiq.application.port.in;

/**
 * Salida de ListCategoriesUseCase y UpdateCategoryParametersUseCase.
 * Incluye los parámetros de negocio (Sección 5.2) además del lookup básico
 * de id/nombre/padre, para que la pantalla de Administración pueda mostrar
 * el valor vigente de cada categoría antes de corregirlo.
 */
public record CategoryResult(
		Long categoryId,
		String name,
		Long parentCategoryId,
		int maxCoverageDaysThreshold,
		int defaultExtraCoverageDays) {
}
