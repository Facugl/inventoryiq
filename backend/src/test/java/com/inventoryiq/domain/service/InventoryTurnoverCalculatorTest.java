package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryTurnoverCalculatorTest {

	@Test
	void calculatesTheTurnoverAsCogsOverAverageInventory() {
		double turnover = InventoryTurnoverCalculator.calculate(new BigDecimal("120000"), new BigDecimal("20000"));
		assertEquals(6.0, turnover, 0.001);
	}

	@Test
	void rejectsZeroOrNegativeAverageInventory() {
		assertThrows(InvalidDomainDataException.class,
				() -> InventoryTurnoverCalculator.calculate(new BigDecimal("100"), BigDecimal.ZERO));
	}
}