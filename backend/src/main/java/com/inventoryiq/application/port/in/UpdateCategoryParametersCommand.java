package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Parámetros de entrada de UpdateCategoryParametersUseCase (Sección 8.10).
 * maxCoverageDaysThreshold usa el mismo umbral que DetectOverstockUseCase
 * (a partir de cuántos días de cobertura un producto de esta categoría se
 * marca Sobrestock); defaultExtraCoverageDays alimenta el Stock de
 * Seguridad (SafetyStockCalculator). Ambos se leen en caliente desde
 * ProductIndicatorsCalculator en cada request — corregirlos acá surte
 * efecto de inmediato, sin recalcular ni reiniciar nada.
 */
public record UpdateCategoryParametersCommand(Long categoryId, int maxCoverageDaysThreshold, int defaultExtraCoverageDays) {

	public UpdateCategoryParametersCommand {
		if (categoryId == null) {
			throw new InvalidDomainDataException("categoryId is required");
		}

		if (maxCoverageDaysThreshold <= 0) {
			throw new InvalidDomainDataException("maxCoverageDaysThreshold must be > 0, received: " + maxCoverageDaysThreshold);
		}

		if (defaultExtraCoverageDays < 0) {
			throw new InvalidDomainDataException("defaultExtraCoverageDays must be >= 0, received: " + defaultExtraCoverageDays);
		}
	}
}
