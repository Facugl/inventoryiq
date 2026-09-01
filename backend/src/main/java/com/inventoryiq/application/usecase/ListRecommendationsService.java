package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ListRecommendationsQuery;
import com.inventoryiq.application.port.in.ListRecommendationsUseCase;
import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Recommendation;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de ListRecommendationsUseCase (Sección 8.5).
 *
 * Lectura simple sobre RecommendationRepository (Postgres), enriquecida
 * con datos de producto (CSV) para sku/nombre/categoría. El filtro por
 * categoryId se aplica acá, en memoria, porque la tabla recommendations
 * no tiene esa columna (ver Javadoc de ListRecommendationsQuery). Si el
 * producto de una recomendación ya no existe en el catálogo, esa
 * recomendación se descarta del listado en vez de fallar toda la request
 * — mismo criterio que el resto del proyecto ante un dato incompleto.
 */
public class ListRecommendationsService implements ListRecommendationsUseCase {

	private final RecommendationRepository recommendationRepository;
	private final ProductRepository productRepository;

	public ListRecommendationsService(RecommendationRepository recommendationRepository, ProductRepository productRepository) {
		this.recommendationRepository = recommendationRepository;
		this.productRepository = productRepository;
	}

	@Override
	public List<RecommendationResult> execute(ListRecommendationsQuery query) {
		return recommendationRepository.findByFilters(query.storeId(), query.supplierId(), query.status()).stream()
				.flatMap(recommendation -> toResult(recommendation).stream())
				.filter(result -> query.categoryId() == null || query.categoryId().equals(result.categoryId()))
				.toList();
	}

	private Optional<RecommendationResult> toResult(Recommendation recommendation) {
		return productRepository.findById(recommendation.productId()).map(product -> toResult(recommendation, product));
	}

	private static RecommendationResult toResult(Recommendation r, Product product) {
		return new RecommendationResult(
				r.recommendationId(), r.productId(), product.sku(), product.name(), r.storeId(), product.categoryId(),
				r.supplierId(), r.suggestedQuantity(), r.orderDeadlineDate(), r.justification(), r.status(),
				r.generationDate(), r.feedbackComment(), r.feedbackDate());
	}
}
