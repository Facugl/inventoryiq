package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.AbcClassification;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbcClassifierTest {

	@Test
	void classifiesByParetoCurveWithThreeProducts() {
		var values = List.of(
				new AbcClassifier.SalesValueByProduct(1L, new BigDecimal("800")),
				new AbcClassifier.SalesValueByProduct(2L, new BigDecimal("150")),
				new AbcClassifier.SalesValueByProduct(3L, new BigDecimal("50")));
		Map<Long, AbcClassification> classes = AbcClassifier.classify(values);

		// Total = 1000. Producto 1: 80% acumulado -> A. Producto 2: 95% acumulado -> B.
		// Producto 3: 100% -> C.
		assertEquals(AbcClassification.A, classes.get(1L));
		assertEquals(AbcClassification.B, classes.get(2L));
		assertEquals(AbcClassification.C, classes.get(3L));
	}

	@Test
	void returnsEmptyMapIfNoProducts() {
		assertEquals(Map.of(), AbcClassifier.classify(List.of()));
	}

	@Test
	void classifiesADominantSingleProductAsANotC() {
		// Un único producto que por sí solo representa el 98% del valor total.
		// Es, por definición, el de mayor contribución (Sección 4.7): debe ser A,
		// no "C: productos de baja contribución".
		var values = List.of(
				new AbcClassifier.SalesValueByProduct(1L, new BigDecimal("980")),
				new AbcClassifier.SalesValueByProduct(2L, new BigDecimal("20")));
		Map<Long, AbcClassification> classes = AbcClassifier.classify(values);

		assertEquals(AbcClassification.A, classes.get(1L));
		assertEquals(AbcClassification.C, classes.get(2L));
	}

	@Test
	void theItemThatCrossesTheEightyPercentThresholdIsStillClassB() {
		// Producto 1 llega exactamente al 80% acumulado -> A.
		// Producto 2 arranca en 80% (ya no está estrictamente por debajo) -> B,
		// aunque sea el que "cruza" el umbral.
		var values = List.of(
				new AbcClassifier.SalesValueByProduct(1L, new BigDecimal("800")),
				new AbcClassifier.SalesValueByProduct(2L, new BigDecimal("200")));
		Map<Long, AbcClassification> classes = AbcClassifier.classify(values);

		assertEquals(AbcClassification.A, classes.get(1L));
		assertEquals(AbcClassification.B, classes.get(2L));
	}
}