package com.inventoryiq.application.port.in;

import java.util.List;

/** Puerto de entrada — Sección 8.5. */
public interface ListRecommendationsUseCase {

	List<RecommendationResult> execute(ListRecommendationsQuery query);
}
