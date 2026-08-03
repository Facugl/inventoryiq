package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriticalityEvaluatorTest {

	private static final CriticalityEvaluator.CriticalityWeights WEIGHTS = new CriticalityEvaluator.CriticalityWeights(
			0.5,
			0.3, 0.2);

	@Test
	void calculatesTheScoreWithoutStockout() {
		// ValorABC(A)=1.0 -> 0.5*1.0*100=50 | factorCobertura=1-(2/10)=0.8 ->
		// 0.3*0.8*100=24 | quiebre=0
		CriticalityLevel nc = CriticalityEvaluator.calculate(AbcClassification.A, 2, new LeadTime(10), false, WEIGHTS);
		assertEquals(74.0, nc.score(), 0.001);
		assertFalse(nc.isCritical());
		assertTrue(nc.isHigh());
	}

	@Test
	void theStockoutIndicatorAddsAdditionalPointsToTheScore() {
		CriticalityLevel nc = CriticalityEvaluator.calculate(AbcClassification.A, 2, new LeadTime(10), true, WEIGHTS);
		assertEquals(94.0, nc.score(), 0.001);
		assertTrue(nc.isCritical());
	}

	@Test
	void aClassCProductWithGoodCoverageHasLowScore() {
		CriticalityLevel nc = CriticalityEvaluator.calculate(AbcClassification.C, 9, new LeadTime(10), false, WEIGHTS);
		// ValorABC(C)=0.3 -> 15 | factorCobertura=1-(9/10)=0.1 -> 3 | quiebre=0
		assertEquals(18.0, nc.score(), 0.001);
		assertFalse(nc.isHigh());
	}
}