package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CategoryResult;
import com.inventoryiq.application.port.in.UpdateCategoryParametersCommand;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.domain.exception.CategoryNotFoundException;
import com.inventoryiq.domain.model.Category;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica UpdateCategoryParametersService: valida que la categoría exista
 * antes de delegar en CategoryRepository.updateParameters() (mismo criterio
 * que UpdateSupplierLeadTimeService) y traduce el resultado a CategoryResult.
 */
class UpdateCategoryParametersServiceTest {

	@Test
	void updatesTheParametersAndReturnsTheResultingCategory() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		categories.add(new Category(2L, "Lácteos", 1L, 12, 3));

		UpdateCategoryParametersService service = new UpdateCategoryParametersService(categories);

		CategoryResult result = service.execute(new UpdateCategoryParametersCommand(2L, 15, 5));

		assertEquals(new CategoryResult(2L, "Lácteos", 1L, 15, 5), result);
	}

	@Test
	void rejectsAnUnknownCategory() {
		UpdateCategoryParametersService service = new UpdateCategoryParametersService(new FakeCategoryRepository());

		assertThrows(CategoryNotFoundException.class,
				() -> service.execute(new UpdateCategoryParametersCommand(9999L, 15, 5)));
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
			Category existing = categories.get(categoryId);
			Category updated = new Category(
					existing.categoryId(), existing.name(), existing.parentCategoryId(),
					maxCoverageDaysThreshold, defaultExtraCoverageDays);
			categories.put(categoryId, updated);
			return updated;
		}
	}
}
