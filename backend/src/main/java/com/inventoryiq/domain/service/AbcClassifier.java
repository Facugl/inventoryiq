package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.AbcClassification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sección 4.7 — Clasificación ABC por curva de Pareto sobre el valor de venta.
 * A diferencia de los demás calculadores, esta clasificación no se puede
 * hacer producto por producto de forma aislada: necesita el catálogo
 * completo para calcular el porcentaje acumulado de valor.
 * Umbrales usados: A hasta 80% acumulado, B hasta 95% acumulado, C el resto
 * (el documento define A=80% pero no fija dónde termina B; 95% es una
 * convención estándar de curva ABC que se documenta acá para poder ajustarla).
 *
 * La clasificación de cada producto usa el porcentaje acumulado ANTES de
 * sumar su propio valor (no después): un producto pertenece a la clase A
 * si, sin contarlo a él, el acumulado todavía no llegó al 80% — es decir,
 * es uno de los productos que construyen ese 80%, incluyendo el que
 * termina de cruzarlo. Esto importa especialmente en el caso extremo de
 * un único producto que por sí solo ya representa más del 80% del valor
 * total: sigue siendo A (es, por definición, el de mayor contribución),
 * no C. Clasificarlo usando el acumulado DESPUÉS de sumarlo lo mandaría a
 * C ("baja contribución"), lo cual contradice directamente la Sección 4.7
 * ("C: productos de baja contribución") para el producto que más aporta.
 */
public final class AbcClassifier {
	private static final BigDecimal THRESHOLD_A = new BigDecimal("0.80");
	private static final BigDecimal THRESHOLD_B = new BigDecimal("0.95");

	private AbcClassifier() {
	}

	public record SalesValueByProduct(Long productId, BigDecimal salesValue) {
	}

	public static Map<Long, AbcClassification> classify(List<SalesValueByProduct> salesValues) {
		if (salesValues == null || salesValues.isEmpty()) {
			return Map.of();
		}

		List<SalesValueByProduct> sorted = salesValues.stream()
				.sorted(Comparator.comparing(SalesValueByProduct::salesValue).reversed())
				.toList();

		BigDecimal total = sorted.stream()
				.map(SalesValueByProduct::salesValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<Long, AbcClassification> result = new LinkedHashMap<>();

		if (total.compareTo(BigDecimal.ZERO) == 0) {
			// Ningún producto vendió nada: no hay Pareto que calcular, todos son C.
			sorted.forEach(v -> result.put(v.productId(), AbcClassification.C));
			return result;
		}

		BigDecimal accumulated = BigDecimal.ZERO;
		for (SalesValueByProduct v : sorted) {
			BigDecimal accumulatedPercentageBefore = accumulated.divide(total, 6, RoundingMode.HALF_UP);

			AbcClassification category;
			if (accumulatedPercentageBefore.compareTo(THRESHOLD_A) < 0) {
				category = AbcClassification.A;
			} else if (accumulatedPercentageBefore.compareTo(THRESHOLD_B) < 0) {
				category = AbcClassification.B;
			} else {
				category = AbcClassification.C;
			}
			result.put(v.productId(), category);

			accumulated = accumulated.add(v.salesValue());
		}
		return result;
	}
}