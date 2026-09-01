package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Store;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvStoreRepositoryAdapterTest {

	private static final Path FIXTURES_PATH = Path.of("src/test/resources/csv-fixtures");

	@Test
	void findsAnExistingStoreById() {
		var adapter = new CsvStoreRepositoryAdapter(FIXTURES_PATH);

		Optional<Store> store = adapter.findById(1L);

		assertTrue(store.isPresent());
		assertEquals("Sucursal Centro", store.get().name());
		assertTrue(store.get().active());
	}

	@Test
	void parsesAnInactiveStore() {
		var adapter = new CsvStoreRepositoryAdapter(FIXTURES_PATH);

		Optional<Store> store = adapter.findById(3L);

		assertTrue(store.isPresent());
		assertEquals(false, store.get().active());
	}

	@Test
	void returnsEmptyForAnUnknownStoreId() {
		var adapter = new CsvStoreRepositoryAdapter(FIXTURES_PATH);

		assertTrue(adapter.findById(9999L).isEmpty());
	}
}
