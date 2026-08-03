package com.inventoryiq.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverstockDetectorTest {

	@Test
	void calculatesTheDaysOfCoverageAsStockOverAds() {
		assertEquals(20.0, OverstockDetector.calculateCurrentDaysOfCoverage(100, 5), 0.001);
	}

	@Test
	void isOverstockWhenCoverageExceedsTheThreshold() {
		assertTrue(OverstockDetector.isOverstock(100, 5, 15)); // 20 días > 15
		assertFalse(OverstockDetector.isOverstock(100, 5, 25)); // 20 días <= 25
	}

	@Test
	void productWithStockAndNoSalesIsAlwaysOverstock() {
		assertTrue(OverstockDetector.isOverstock(50, 0, 15));
	}

	@Test
	void productWithNoStockAndNoSalesIsNotOverstock() {
		assertFalse(OverstockDetector.isOverstock(0, 0, 15));
	}
}