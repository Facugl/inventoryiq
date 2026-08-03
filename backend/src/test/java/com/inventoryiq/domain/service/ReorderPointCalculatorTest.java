package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReorderPointCalculatorTest {

	@Test
	void calculatesRopAsAdsTimesLeadTimePlusSafetyStock() {
		ReorderPoint rop = ReorderPointCalculator.calculate(10, new LeadTime(4), new SafetyStock(50));
		// (10 * 4) + 50 = 90
		assertEquals(90.0, rop.units(), 0.001);
	}

	@Test
	void requiresReplenishmentWhenStockIsExactlyAtTheReorderPoint() {
		ReorderPoint rop = new ReorderPoint(90);
		assertTrue(ReorderPointCalculator.requiresReplenishment(90, rop),
				"at the limit (<=) it must trigger replenishment");
		assertFalse(ReorderPointCalculator.requiresReplenishment(91, rop),
				"above the reorder point it must not trigger");
	}
}