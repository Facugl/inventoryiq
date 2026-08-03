package com.inventoryiq.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandStatisticsTest {

	private static final List<Integer> VALUES = List.of(10, 12, 8, 10);

	@Test
	void calculatesTheMean() {
		assertEquals(10.0, DemandStatistics.mean(VALUES), 0.001);
	}

	@Test
	void calculatesThePopulationStandardDeviation() {
		assertEquals(Math.sqrt(2), DemandStatistics.standardDeviation(VALUES), 0.0001);
	}

	@Test
	void coefficientOfVariationIsZeroWhenTheMeanIsZero() {
		assertEquals(0.0, DemandStatistics.coefficientOfVariation(List.of(0, 0, 0)), 0.001);
	}
}