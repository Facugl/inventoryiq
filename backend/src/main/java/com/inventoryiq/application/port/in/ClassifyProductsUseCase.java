package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada — Sección 2.3.2 / 9.5. Devuelve la clasificación
 * ABC/XYZ de cada producto del scope solicitado. No persiste el
 * resultado (eso es una preocupación de v2.0/Postgres): se recalcula al
 * vuelo en cada invocación, igual que ya hace CriticalityEvaluator
 * internamente para el score de criticidad.
 */
public interface ClassifyProductsUseCase {

	List<ProductClassificationResult> execute(ClassifyProductsQuery query);
}
