package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ProductSummaryResult;
import com.inventoryiq.application.port.in.SearchProductsQuery;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica que SearchProductsService filtre en memoria por código (sku) o
 * nombre, sin distinguir mayúsculas — el mismo criterio con el que el
 * resto del proyecto filtra en memoria sobre findAllActive() (por
 * ejemplo, GenerateReorderSuggestionsService filtrando por categoría).
 */
class SearchProductsServiceTest {

	private static final FakeProductRepository PRODUCTS = new FakeProductRepository();

	static {
		PRODUCTS.add(product(1008L, "QUE-1008", "Queso Cremoso 300g", 2L));
		PRODUCTS.add(product(1010L, "QUE-1010", "Queso Fresco 300g", 3L));
		PRODUCTS.add(product(33L, "33", "Castel Cremoso x kg", 3L));
		PRODUCTS.add(product(9999L, "DISC-9999", "Producto Descontinuado", 1L, false));
	}

	@Test
	void matchesByCodeCaseInsensitive() {
		SearchProductsService service = new SearchProductsService(PRODUCTS);

		List<ProductSummaryResult> result = service.execute(new SearchProductsQuery("que-1008"));

		assertEquals(List.of(new ProductSummaryResult(1008L, "QUE-1008", "Queso Cremoso 300g", 2L)), result);
	}

	@Test
	void matchesShortInternalCodeExactly() {
		SearchProductsService service = new SearchProductsService(PRODUCTS);

		List<ProductSummaryResult> result = service.execute(new SearchProductsQuery("33"));

		assertEquals(List.of(new ProductSummaryResult(33L, "33", "Castel Cremoso x kg", 3L)), result);
	}

	@Test
	void matchesByNameSubstringAndReturnsEveryMatchingPresentation() {
		SearchProductsService service = new SearchProductsService(PRODUCTS);

		List<ProductSummaryResult> result = service.execute(new SearchProductsQuery("queso"));

		assertEquals(
				Set.of(
						new ProductSummaryResult(1008L, "QUE-1008", "Queso Cremoso 300g", 2L),
						new ProductSummaryResult(1010L, "QUE-1010", "Queso Fresco 300g", 3L)),
				Set.copyOf(result));
	}

	@Test
	void excludesInactiveProducts() {
		SearchProductsService service = new SearchProductsService(PRODUCTS);

		List<ProductSummaryResult> result = service.execute(new SearchProductsQuery("descontinuado"));

		assertEquals(List.of(), result);
	}

	@Test
	void rejectsBlankSearchTerm() {
		assertThrows(RuntimeException.class, () -> new SearchProductsQuery("   "));
	}

	private static Product product(Long id, String sku, String name, Long categoryId) {
		return product(id, sku, name, categoryId, true);
	}

	private static Product product(Long id, String sku, String name, Long categoryId, boolean active) {
		return new Product(
				id, sku, name, categoryId, 1L, "UN",
				BigDecimal.TEN, BigDecimal.valueOf(20), new LeadTime(3), active);
	}

	private static class FakeProductRepository implements ProductRepository {
		private final Map<Long, Product> products = new HashMap<>();

		void add(Product product) {
			products.put(product.productId(), product);
		}

		@Override
		public List<Product> findAllActive() {
			return products.values().stream().filter(Product::active).toList();
		}

		@Override
		public Optional<Product> findById(Long productId) {
			return Optional.ofNullable(products.get(productId));
		}
	}
}
