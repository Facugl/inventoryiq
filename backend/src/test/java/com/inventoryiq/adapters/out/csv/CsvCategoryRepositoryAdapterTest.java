package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Category;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvCategoryRepositoryAdapterTest {

	private static final Path FIXTURES_PATH = Path.of("src/test/resources/csv-fixtures");

	@Test
	void parsesDecimalThresholdsAsExactInts() {
		var adapter = new CsvCategoryRepositoryAdapter(FIXTURES_PATH);

		Category category = adapter.findById(2L).orElseThrow();

		assertEquals("Lácteos", category.name());
		assertEquals(12, category.maxCoverageDaysThreshold());
		assertEquals(3, category.defaultExtraCoverageDays());
	}

	@Test
	void parsesEmptyParentCategoryIdAsNull() {
		var adapter = new CsvCategoryRepositoryAdapter(FIXTURES_PATH);

		Category rootCategory = adapter.findById(1L).orElseThrow();

		assertNull(rootCategory.parentCategoryId());
	}

	@Test
	void parsesNonEmptyParentCategoryId() {
		var adapter = new CsvCategoryRepositoryAdapter(FIXTURES_PATH);

		Category leafCategory = adapter.findById(2L).orElseThrow();

		assertEquals(1L, leafCategory.parentCategoryId());
	}

	@Test
	void findByIdReturnsEmptyForUnknownCategory() {
		var adapter = new CsvCategoryRepositoryAdapter(FIXTURES_PATH);

		assertTrue(adapter.findById(9999L).isEmpty());
	}
}
