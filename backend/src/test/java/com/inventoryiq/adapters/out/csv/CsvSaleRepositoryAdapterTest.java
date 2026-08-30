package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Sale;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvSaleRepositoryAdapterTest {

	private static final Path FIXTURES_PATH = Path.of("src/test/resources/csv-fixtures");

	@Test
	void findsSalesWithinRangeForTheGivenProductAndStoreOnly() {
		var adapter = new CsvSaleRepositoryAdapter(FIXTURES_PATH);

		List<Sale> sales = adapter.findByProductAndStore(
				1001L, 1L, LocalDate.parse("2025-07-03"), LocalDate.parse("2025-07-05"));

		assertEquals(3, sales.size());
		assertTrue(sales.stream().allMatch(s -> s.productId().equals(1001L) && s.storeId().equals(1L)));
	}

	@Test
	void dateRangeIsInclusiveOnBothEnds() {
		var adapter = new CsvSaleRepositoryAdapter(FIXTURES_PATH);

		List<Sale> sales = adapter.findByProductAndStore(
				1001L, 1L, LocalDate.parse("2025-07-04"), LocalDate.parse("2025-07-04"));

		assertEquals(1, sales.size());
		assertEquals(55, sales.get(0).unitsSold());
	}

	@Test
	void excludesSalesFromOtherProductsAndStores() {
		var adapter = new CsvSaleRepositoryAdapter(FIXTURES_PATH);

		List<Sale> sales = adapter.findByProductAndStore(
				1001L, 1L, LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31"));

		assertTrue(sales.stream().noneMatch(s -> s.productId().equals(1002L)));
		assertTrue(sales.stream().noneMatch(s -> s.storeId().equals(2L)));
	}

	@Test
	void returnsEmptyListForUnknownProductStorePair() {
		var adapter = new CsvSaleRepositoryAdapter(FIXTURES_PATH);

		List<Sale> sales = adapter.findByProductAndStore(
				9999L, 1L, LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31"));

		assertTrue(sales.isEmpty());
	}
}
