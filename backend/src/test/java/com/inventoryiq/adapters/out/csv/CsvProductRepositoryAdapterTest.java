package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Product;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvProductRepositoryAdapterTest {

	private static final Path FIXTURES_PATH = Path.of("src/test/resources/csv-fixtures");

	@Test
	void findAllActiveExcludesInactiveProducts() {
		var adapter = new CsvProductRepositoryAdapter(FIXTURES_PATH);

		List<Product> active = adapter.findAllActive();

		assertEquals(2, active.size());
		assertTrue(active.stream().noneMatch(p -> p.productId().equals(1099L)));
	}

	@Test
	void findByIdParsesAllFieldsCorrectly() {
		var adapter = new CsvProductRepositoryAdapter(FIXTURES_PATH);

		Product product = adapter.findById(1001L).orElseThrow();

		assertEquals("LEC-1001", product.sku());
		assertEquals("Leche Entera 1L", product.name());
		assertEquals(2L, product.categoryId());
		assertEquals(1L, product.supplierId());
		assertEquals(4, product.leadTime().days());
		assertTrue(product.active());
	}

	@Test
	void findByIdReturnsEmptyForUnknownProduct() {
		var adapter = new CsvProductRepositoryAdapter(FIXTURES_PATH);

		assertTrue(adapter.findById(9999L).isEmpty());
	}
}
