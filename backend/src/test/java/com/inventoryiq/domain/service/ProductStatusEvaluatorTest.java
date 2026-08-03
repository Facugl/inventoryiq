package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductStatusEvaluatorTest {

	private static final ReorderPoint ROP = new ReorderPoint(60);
	private static final SafetyStock SAFETY_STOCK = new SafetyStock(10);

	@Test
	void stockAtZeroIsAlwaysCritical() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(0, ROP, SAFETY_STOCK, 5, 30, false);
		assertEquals(ProductStatus.CRITICAL, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void stockBelowSafetyStockIsCritical() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(5, ROP, SAFETY_STOCK, 5, 30, false);
		assertEquals(ProductStatus.CRITICAL, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void coverageAboveThresholdIsOverstock() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(50, ROP, SAFETY_STOCK, 40, 30, false);
		assertEquals(ProductStatus.OVERSTOCK, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void stockBelowReorderPointRequiresReplenishment() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(55, ROP, SAFETY_STOCK, 20, 30, false);
		assertEquals(ProductStatus.REQUIRES_REPLENISHMENT, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void sustainedLowRotationIsDetectedWhenThereIsNoMoreUrgentCondition() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(70, ROP, SAFETY_STOCK, 20, 30, true);
		assertEquals(ProductStatus.LOW_ROTATION, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void withoutAnySpecialConditionTheStatusIsNormal() {
		var ctx = new ProductStatusEvaluator.EvaluationContext(70, ROP, SAFETY_STOCK, 20, 30, false);
		assertEquals(ProductStatus.NORMAL, ProductStatusEvaluator.evaluate(ctx));
	}

	@Test
	void criticalWinsOverOverstockIfBothConditionsAreMet() {
		// stock=0 (critico) pero con cobertura muy alta si el ADS fuera 0 -> igual gana
		// CRITICO por prioridad
		var lowRop = new ReorderPoint(1);
		var ctx = new ProductStatusEvaluator.EvaluationContext(0, lowRop, SAFETY_STOCK, 999, 30, false);
		assertEquals(ProductStatus.CRITICAL, ProductStatusEvaluator.evaluate(ctx));
	}
}