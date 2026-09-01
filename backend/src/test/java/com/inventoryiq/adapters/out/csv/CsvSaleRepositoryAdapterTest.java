package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Sale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

	@Test
	void existsByProductStoreAndDateReturnsTrueOnlyForAnAlreadyLoadedSale() {
		var adapter = new CsvSaleRepositoryAdapter(FIXTURES_PATH);

		assertTrue(adapter.existsByProductStoreAndDate(1001L, 1L, LocalDate.parse("2025-07-03")));
		assertFalse(adapter.existsByProductStoreAndDate(1001L, 1L, LocalDate.parse("2025-07-10")));
		assertFalse(adapter.existsByProductStoreAndDate(9999L, 1L, LocalDate.parse("2025-07-03")));
	}

	@Test
	void saveMakesTheNewSaleImmediatelyVisibleThroughFindByProductAndStore(@TempDir Path tempDir) throws IOException {
		copyFixturesTo(tempDir);
		var adapter = new CsvSaleRepositoryAdapter(tempDir);

		adapter.save(new Sale(999001L, LocalDate.parse("2025-07-10"), 1001L, 1L, 15, new BigDecimal("9000.00")));

		List<Sale> sales = adapter.findByProductAndStore(
				1001L, 1L, LocalDate.parse("2025-07-10"), LocalDate.parse("2025-07-10"));
		assertEquals(1, sales.size());
		assertEquals(15, sales.get(0).unitsSold());
		assertTrue(adapter.existsByProductStoreAndDate(1001L, 1L, LocalDate.parse("2025-07-10")));
	}

	@Test
	void saveAppendsTheNewRowToTheCsvFileOnDisk(@TempDir Path tempDir) throws IOException {
		copyFixturesTo(tempDir);
		var adapter = new CsvSaleRepositoryAdapter(tempDir);

		adapter.save(new Sale(999002L, LocalDate.parse("2025-07-11"), 1002L, 1L, 7, new BigDecimal("4200.00")));

		String content = Files.readString(tempDir.resolve("ventas.csv"));
		assertTrue(content.contains("999002,2025-07-11,1002,1,7,4200.00"));

		// una segunda instancia, leyendo el archivo desde cero, también ve la fila nueva.
		var reloaded = new CsvSaleRepositoryAdapter(tempDir);
		assertTrue(reloaded.existsByProductStoreAndDate(1002L, 1L, LocalDate.parse("2025-07-11")));
	}

	private static void copyFixturesTo(Path tempDir) throws IOException {
		try (Stream<Path> files = Files.list(FIXTURES_PATH)) {
			for (Path file : files.toList()) {
				Files.copy(file, tempDir.resolve(file.getFileName()));
			}
		}
	}
}
