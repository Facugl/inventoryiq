package com.inventoryiq.domain.model;

import com.inventoryiq.domain.model.vo.RecommendedQuantity;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;

import java.time.LocalDate;

/**
 * Resultado final de aplicar todas las reglas de la Sección 4 a un producto
 * en una sucursal. Es lo que Fase 2 va a persistir/exponer, y lo que la
 * pantalla de "Detalle de Producto" del frontend usa para la justificación
 * siempre visible (por qué se recomienda esa cantidad).
 */
public record PurchaseRecommendation(
		Long productId,
		Long storeId,
		LocalDate generationDate,
		ReorderPoint reorderPoint,
		SafetyStock safetyStock,
		RecommendedQuantity suggestedQuantity,
		CriticalityLevel criticalityLevel,
		ProductStatus status,
		String justification) {
}
