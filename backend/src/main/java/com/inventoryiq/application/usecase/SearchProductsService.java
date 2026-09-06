package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ProductSummaryResult;
import com.inventoryiq.application.port.in.SearchProductsQuery;
import com.inventoryiq.application.port.in.SearchProductsUseCase;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.domain.model.Product;

import java.util.List;
import java.util.Locale;

/**
 * Implementación de SearchProductsUseCase. Filtra en memoria sobre
 * findAllActive() — mismo criterio que el resto del proyecto usa para
 * filtrar por categoría/proveedor (por ejemplo, GenerateReorderSuggestionsService):
 * el catálogo es chico, no justifica un método de búsqueda en el puerto.
 */
public class SearchProductsService implements SearchProductsUseCase {

	private final ProductRepository productRepository;

	public SearchProductsService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public List<ProductSummaryResult> execute(SearchProductsQuery query) {
		String term = query.searchTerm().toLowerCase(Locale.ROOT);

		return productRepository.findAllActive().stream()
				.filter(product -> matches(product, term))
				.map(product -> new ProductSummaryResult(product.productId(), product.sku(), product.name(), product.categoryId()))
				.toList();
	}

	private static boolean matches(Product product, String term) {
		return product.sku().toLowerCase(Locale.ROOT).contains(term)
				|| product.name().toLowerCase(Locale.ROOT).contains(term);
	}
}
