package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.domain.model.Inventory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvInventoryRepositoryAdapterTest {

	private static final Path FIXTURES_PATH = Path.of("src/test/resources/csv-fixtures");

	@Test
	void findsLatestSnapshotOnExactDate() {
		var adapter = new CsvInventoryRepositoryAdapter(FIXTURES_PATH);

		Optional<Inventory> snapshot = adapter.findLatestSnapshotAsOf(1001L, 1L, LocalDate.parse("2025-07-04"));

		assertTrue(snapshot.isPresent());
		assertEquals(259, snapshot.get().currentStock());
		assertEquals(465, snapshot.get().stockInTransit());
	}

	@Test
	void findsMostRecentSnapshotOnOrBeforeAsOfDateWhenExactDateIsMissing() {
		var adapter = new CsvInventoryRepositoryAdapter(FIXTURES_PATH);

		Optional<Inventory> snapshot = adapter.findLatestSnapshotAsOf(1001L, 1L, LocalDate.parse("2025-07-10"));

		assertTrue(snapshot.isPresent());
		assertEquals(LocalDate.parse("2025-07-05"), snapshot.get().snapshotDate());
	}

	@Test
	void returnsEmptyWhenAsOfDateIsBeforeAllSnapshots() {
		var adapter = new CsvInventoryRepositoryAdapter(FIXTURES_PATH);

		Optional<Inventory> snapshot = adapter.findLatestSnapshotAsOf(1001L, 1L, LocalDate.parse("2025-07-01"));

		assertTrue(snapshot.isEmpty());
	}

	@Test
	void findSnapshotsInRangeIsInclusiveAndScopedToProductAndStore() {
		var adapter = new CsvInventoryRepositoryAdapter(FIXTURES_PATH);

		List<Inventory> snapshots = adapter.findSnapshotsInRange(
				1001L, 1L, LocalDate.parse("2025-07-02"), LocalDate.parse("2025-07-05"));

		assertEquals(4, snapshots.size());
		assertTrue(snapshots.stream().allMatch(i -> i.productId().equals(1001L) && i.storeId().equals(1L)));
	}
}
