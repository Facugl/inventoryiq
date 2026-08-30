package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.domain.model.Inventory;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador de salida — lee inventario.csv (Sección 5.7), indexado por producto+sucursal. */
public class CsvInventoryRepositoryAdapter implements InventoryRepository {

	private static final String FILE_NAME = "inventario.csv";

	private final Map<ProductStoreKey, List<Inventory>> snapshotsByProductAndStore;

	public CsvInventoryRepositoryAdapter(Path csvBasePath) {
		List<CSVRecord> records = CsvFileReader.readRecords(csvBasePath.resolve(FILE_NAME));
		Map<ProductStoreKey, List<Inventory>> grouped = records.stream()
				.map(CsvInventoryRepositoryAdapter::toInventory)
				.collect(Collectors.groupingBy(inv -> new ProductStoreKey(inv.productId(), inv.storeId())));
		grouped.replaceAll((key, snapshots) -> snapshots.stream().sorted(Comparator.comparing(Inventory::snapshotDate)).toList());
		this.snapshotsByProductAndStore = grouped;
	}

	@Override
	public Optional<Inventory> findLatestSnapshotAsOf(Long productId, Long storeId, LocalDate asOfDate) {
		List<Inventory> snapshots = snapshotsByProductAndStore.getOrDefault(new ProductStoreKey(productId, storeId), List.of());
		return snapshots.stream()
				.filter(snapshot -> !snapshot.snapshotDate().isAfter(asOfDate))
				.max(Comparator.comparing(Inventory::snapshotDate));
	}

	@Override
	public List<Inventory> findSnapshotsInRange(Long productId, Long storeId, LocalDate from, LocalDate to) {
		List<Inventory> snapshots = snapshotsByProductAndStore.getOrDefault(new ProductStoreKey(productId, storeId), List.of());
		return snapshots.stream()
				.filter(snapshot -> !snapshot.snapshotDate().isBefore(from) && !snapshot.snapshotDate().isAfter(to))
				.toList();
	}

	private static Inventory toInventory(CSVRecord record) {
		return new Inventory(
				CsvFieldParsers.parseLong(record.get("inventario_id")),
				CsvFieldParsers.parseDate(record.get("fecha_snapshot")),
				CsvFieldParsers.parseLong(record.get("producto_id")),
				CsvFieldParsers.parseLong(record.get("sucursal_id")),
				CsvFieldParsers.parseInt(record.get("stock_actual")),
				CsvFieldParsers.parseInt(record.get("stock_en_transito")));
	}
}
