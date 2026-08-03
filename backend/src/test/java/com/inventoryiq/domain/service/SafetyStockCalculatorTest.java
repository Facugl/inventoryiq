package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.SafetyStock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafetyStockCalculatorTest {

	@Test
	void statisticalMethodAppliesZTimesSigmaTimesSquareRootOfLeadTime() {
		SafetyStock ss = SafetyStockCalculator.calculateStatisticalMethod(1.65, 5, new LeadTime(4));
		// 1.65 * 5 * sqrt(4) = 1.65 * 5 * 2 = 16.5
		assertEquals(16.5, ss.units(), 0.001);
	}

	@Test
	void simplifiedMethodMultipliesAdsByExtraCoverageDays() {
		SafetyStock ss = SafetyStockCalculator.calculateSimplifiedMethod(10, 5);
		assertEquals(50.0, ss.units(), 0.001);
	}

	@Test
	void rejectsNegativeExtraCoverageDays() {
		assertThrows(InvalidDomainDataException.class,
				() -> SafetyStockCalculator.calculateSimplifiedMethod(10, -1));
	}
}