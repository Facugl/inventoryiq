package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.vo.RecommendedQuantity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendedQuantityCalculatorTest {

	@Test
	void calculatesTheQuantityByTargetCoverage() {
		// (10 * 20) - 50 - 10 = 140
		RecommendedQuantity qty = RecommendedQuantityCalculator.calculateByTargetCoverage(10, 20, 50, 10);
		assertEquals(140, qty.units());
	}

	@Test
	void neverSuggestsANegativeQuantityEvenIfStockAlreadyCoversTheTarget() {
		// (10 * 5) - 100 - 0 = -50 -> se recorta a 0
		RecommendedQuantity qty = RecommendedQuantityCalculator.calculateByTargetCoverage(10, 5, 100, 0);
		assertEquals(0, qty.units());
	}

	@Test
	void calculatesTheEoq() {
		// sqrt((2 * 3650 * 50) / 2) = sqrt(182500) ≈ 427.2
		RecommendedQuantity eoq = RecommendedQuantityCalculator.calculateEoq(3650, 50, 2);
		assertEquals(427, eoq.units());
	}
}