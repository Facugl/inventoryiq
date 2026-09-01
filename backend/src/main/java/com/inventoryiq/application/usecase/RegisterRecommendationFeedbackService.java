package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackCommand;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackUseCase;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.exception.ProductNotFoundException;
import com.inventoryiq.domain.exception.RecommendationNotFoundException;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Recommendation;

/**
 * Implementación de RegisterRecommendationFeedbackUseCase (Sección 8.7/9.8).
 *
 * La validación de la transición de estado (solo PENDING puede recibir
 * feedback, el nuevo estado debe ser APPLIED o DISCARDED) vive en
 * Recommendation.withFeedback — única fuente de verdad de esa regla,
 * mapeada a 400 vía InvalidDomainDataException por GlobalExceptionHandler.
 */
public class RegisterRecommendationFeedbackService implements RegisterRecommendationFeedbackUseCase {

	private final RecommendationRepository recommendationRepository;
	private final ProductRepository productRepository;

	public RegisterRecommendationFeedbackService(
			RecommendationRepository recommendationRepository, ProductRepository productRepository) {
		this.recommendationRepository = recommendationRepository;
		this.productRepository = productRepository;
	}

	@Override
	public RecommendationResult execute(RegisterRecommendationFeedbackCommand command) {
		Recommendation recommendation = recommendationRepository.findById(command.recommendationId())
				.orElseThrow(() -> new RecommendationNotFoundException(command.recommendationId()));

		Recommendation updated = recommendation.withFeedback(command.newStatus(), command.comment(), command.feedbackDate());
		Recommendation saved = recommendationRepository.save(updated);

		Product product = productRepository.findById(saved.productId())
				.orElseThrow(() -> new ProductNotFoundException(saved.productId()));

		return new RecommendationResult(
				saved.recommendationId(), saved.productId(), product.sku(), product.name(), saved.storeId(),
				product.categoryId(), saved.supplierId(), saved.suggestedQuantity(), saved.orderDeadlineDate(),
				saved.justification(), saved.status(), saved.generationDate(), saved.feedbackComment(), saved.feedbackDate());
	}
}
