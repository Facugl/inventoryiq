package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada — Sección 2.3.2 / 9.2. Genera, para cada producto que
 * dispara el punto de pedido, una recomendación de compra con cantidad
 * sugerida y fecha límite de emisión de orden.
 */
public interface GenerateReorderSuggestionsUseCase {

	List<ReorderSuggestionResult> execute(GenerateReorderSuggestionsQuery query);
}
