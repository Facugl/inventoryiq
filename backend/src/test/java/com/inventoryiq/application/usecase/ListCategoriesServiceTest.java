package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CategoryResult;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.domain.model.Category;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que ListCategoriesService delegue en CategoryRepository.findAll()
 * y traduzca cada Category a un CategoryResult, preservando parentCategoryId
 * (necesario para reconstruir la jerarquía documentada en la Sección 5.2) —
 * sin lógica de negocio propia que testear más allá de esa traducción.
 */
class ListCategoriesServiceTest {

	@Test
	void returnsAllCategoriesTranslatedToResultsPreservingHierarchy() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		categories.add(new Category(1L, "Almacén Frío", null, 20, 4));
		categories.add(new Category(2L, "Lácteos", 1L, 12, 3));

		ListCategoriesService service = new ListCategoriesService(categories);

		List<CategoryResult> result = service.execute();

		// findAll() no garantiza orden (respaldado por un Map, igual que el
		// adaptador CSV real): se compara como conjunto.
		assertEquals(
				Set.of(new CategoryResult(1L, "Almacén Frío", null, 20, 4), new CategoryResult(2L, "Lácteos", 1L, 12, 3)),
				Set.copyOf(result));
	}

	private static class FakeCategoryRepository implements CategoryRepository {
		private final Map<Long, Category> categories = new HashMap<>();

		void add(Category category) {
			categories.put(category.categoryId(), category);
		}

		@Override
		public Optional<Category> findById(Long categoryId) {
			return Optional.ofNullable(categories.get(categoryId));
		}

		@Override
		public List<Category> findAll() {
			return List.copyOf(categories.values());
		}

		@Override
		public Category updateParameters(Long categoryId, int maxCoverageDaysThreshold, int defaultExtraCoverageDays) {
			throw new UnsupportedOperationException("not exercised by this test");
		}
	}
}
