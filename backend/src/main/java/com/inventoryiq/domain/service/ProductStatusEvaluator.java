package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;

/**
 * Sección 4.12 — Máquina de estados de un producto.
 * El documento define las 5 condiciones pero no un orden de prioridad
 * explícito entre ellas (más de una puede cumplirse a la vez). Se define
 * acá la prioridad CRITICO > SOBRESTOCK > REQUIERE_REPOSICION > BAJA_ROTACION
 * > NORMAL, porque un quiebre real (o inminente) siempre debe ganarle a
 * cualquier otra condición al momento de decidir qué mostrar primero en
 * una alerta.
 */
public final class ProductStatusEvaluator {
	private ProductStatusEvaluator() {
	}

	public record EvaluationContext(
			int currentStock,
			ReorderPoint reorderPoint,
			SafetyStock safetyStock,
			double currentDaysOfCoverage,
			int maximumCoverageThresholdDays,
			boolean sustainedLowRotation) {
	}
	public static ProductStatus evaluate(EvaluationContext ctx) {
		if (ctx.currentStock() == 0 || ctx.currentStock() <= ctx.safetyStock().units()) {
			return ProductStatus.CRITICAL;
		}

		if (ctx.currentDaysOfCoverage() > ctx.maximumCoverageThresholdDays()) {
			return ProductStatus.OVERSTOCK;
		}

		if (ctx.currentStock() <= ctx.reorderPoint().units()) {
			return ProductStatus.REQUIRES_REPLENISHMENT;
		}

		if (ctx.sustainedLowRotation()) {
			return ProductStatus.LOW_ROTATION;
		}
		
		return ProductStatus.NORMAL;
	}
}