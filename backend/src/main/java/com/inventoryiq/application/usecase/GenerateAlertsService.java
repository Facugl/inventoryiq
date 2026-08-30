package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.AlertResult;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;
import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.in.OverstockSortBy;
import com.inventoryiq.domain.model.vo.CriticalityLevel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de GenerateAlertsUseCase (Sección 9.6). Es pura
 * composición: no agrega ninguna regla de negocio de dominio nueva, solo
 * invoca los dos casos de uso existentes (a través de sus PUERTOS, nunca
 * de sus implementaciones concretas) y traduce cada resultado a un
 * formato de alerta común.
 *
 * Severidad:
 * - Alertas de tipo STOCKOUT (productos Crítico/Requiere Reposición):
 *   se reutiliza directamente el criticalityLevel ya calculado por
 *   GetCriticalProductsUseCase (isCritical()/isHigh()) — sin cálculo
 *   nuevo. Cualquier producto con stock=0 cae inevitablemente en HIGH
 *   (el indicador de quiebre y el factor de cobertura de la fórmula 4.11
 *   quedan siempre al máximo cuando el stock es cero).
 * - Alertas de tipo OVERSTOCK: la Sección 9.6 pide basar la severidad en
 *   "la magnitud del sobrestock", sin dar fórmula. Se usan umbrales fijos
 *   en pesos sobre el valor inmovilizado (decisión de producto, no de la
 *   documentación): HIGH > $500.000, MEDIUM > $50.000, LOW en el resto.
 */
public class GenerateAlertsService implements GenerateAlertsUseCase {

	private static final BigDecimal OVERSTOCK_HIGH_THRESHOLD = new BigDecimal("500000");
	private static final BigDecimal OVERSTOCK_MEDIUM_THRESHOLD = new BigDecimal("50000");

	private final GetCriticalProductsUseCase getCriticalProductsUseCase;
	private final DetectOverstockUseCase detectOverstockUseCase;

	public GenerateAlertsService(
			GetCriticalProductsUseCase getCriticalProductsUseCase,
			DetectOverstockUseCase detectOverstockUseCase) {
		this.getCriticalProductsUseCase = getCriticalProductsUseCase;
		this.detectOverstockUseCase = detectOverstockUseCase;
	}

	@Override
	public List<AlertResult> execute(GenerateAlertsQuery query) {
		List<AlertResult> alerts = new ArrayList<>();

		if (query.type() == null || query.type() == AlertType.STOCKOUT) {
			alerts.addAll(stockoutAlerts(query));
		}

		if (query.type() == null || query.type() == AlertType.OVERSTOCK) {
			alerts.addAll(overstockAlerts(query));
		}

		return alerts;
	}

	private List<AlertResult> stockoutAlerts(GenerateAlertsQuery query) {
		GetCriticalProductsQuery criticalQuery = GetCriticalProductsQuery.of(
				query.storeId(), null, null, query.referenceDate());

		List<AlertResult> alerts = new ArrayList<>();
		for (CriticalProductResult product : getCriticalProductsUseCase.execute(criticalQuery)) {
			AlertSeverity severity = severityFor(product.criticalityLevel());
			if (query.severity() == null || query.severity() == severity) {
				alerts.add(new AlertResult(
						product.productId(), product.sku(), product.productName(), product.storeId(), product.categoryId(),
						AlertType.STOCKOUT, severity, query.referenceDate()));
			}
		}
		return alerts;
	}

	private List<AlertResult> overstockAlerts(GenerateAlertsQuery query) {
		DetectOverstockQuery overstockQuery = DetectOverstockQuery.of(
				query.storeId(), null, query.referenceDate(), OverstockSortBy.IMMOBILIZED_VALUE);

		List<AlertResult> alerts = new ArrayList<>();
		for (OverstockProductResult product : detectOverstockUseCase.execute(overstockQuery)) {
			AlertSeverity severity = severityFor(product.immobilizedValue());
			if (query.severity() == null || query.severity() == severity) {
				alerts.add(new AlertResult(
						product.productId(), product.sku(), product.productName(), product.storeId(), product.categoryId(),
						AlertType.OVERSTOCK, severity, query.referenceDate()));
			}
		}
		return alerts;
	}

	private static AlertSeverity severityFor(CriticalityLevel level) {
		if (level.isCritical()) {
			return AlertSeverity.HIGH;
		}
		if (level.isHigh()) {
			return AlertSeverity.MEDIUM;
		}
		return AlertSeverity.LOW;
	}

	private static AlertSeverity severityFor(BigDecimal immobilizedValue) {
		if (immobilizedValue.compareTo(OVERSTOCK_HIGH_THRESHOLD) > 0) {
			return AlertSeverity.HIGH;
		}
		if (immobilizedValue.compareTo(OVERSTOCK_MEDIUM_THRESHOLD) > 0) {
			return AlertSeverity.MEDIUM;
		}
		return AlertSeverity.LOW;
	}
}
