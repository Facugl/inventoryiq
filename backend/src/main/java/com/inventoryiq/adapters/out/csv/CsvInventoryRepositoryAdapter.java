package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.InventoryIngestionRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.domain.model.Inventory;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Adaptador de salida — lee inventario.csv (Sección 5.7), indexado por
 * producto+sucursal. Además implementa InventoryIngestionRepository:
 * save() agrega un nuevo snapshot al final del archivo en disco y al
 * índice en memoria de este mismo objeto, así un conteo manual queda
 * visible al resto de las consultas sin reiniciar la aplicación — mismo
 * criterio que CsvSaleRepositoryAdapter con SaleRepository/
 * SaleIngestionRepository. Ambos puertos deben resolver a esta misma
 * instancia (ver config/CsvAdaptersConfig).
 */
public class CsvInventoryRepositoryAdapter implements InventoryRepository, InventoryIngestionRepository {

	private static final String FILE_NAME = "inventario.csv";

	private final Path filePath;
	private final Map<ProductStoreKey, List<Inventory>> snapshotsByProductAndStore;
	private final AtomicLong nextInventoryId;

	public CsvInventoryRepositoryAdapter(Path csvBasePath) {
		this.filePath = csvBasePath.resolve(FILE_NAME);
		List<CSVRecord> records = CsvFileReader.readRecords(filePath);
		Map<ProductStoreKey, List<Inventory>> grouped = records.stream()
				.map(CsvInventoryRepositoryAdapter::toInventory)
				.collect(Collectors.groupingBy(inv -> new ProductStoreKey(inv.productId(), inv.storeId())));
		grouped.replaceAll((key, snapshots) -> new ArrayList<>(snapshots.stream().sorted(Comparator.comparing(Inventory::snapshotDate)).toList()));
		this.snapshotsByProductAndStore = grouped;

		long maxExistingId = records.stream().mapToLong(record -> CsvFieldParsers.parseLong(record.get("inventario_id"))).max().orElse(0);
		this.nextInventoryId = new AtomicLong(maxExistingId + 1);
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

	@Override
	public synchronized Inventory save(LocalDate snapshotDate, Long productId, Long storeId, int currentStock, int stockInTransit) {
		Inventory snapshot = new Inventory(nextInventoryId.getAndIncrement(), snapshotDate, productId, storeId, currentStock, stockInTransit);

		appendToFile(snapshot);
		ProductStoreKey key = new ProductStoreKey(productId, storeId);
		List<Inventory> snapshots = snapshotsByProductAndStore.computeIfAbsent(key, k -> new ArrayList<>());
		snapshots.add(snapshot);
		snapshots.sort(Comparator.comparing(Inventory::snapshotDate));

		return snapshot;
	}

	private void appendToFile(Inventory snapshot) {
		String line = String.join(",",
				String.valueOf(snapshot.inventoryId()),
				snapshot.snapshotDate().toString(),
				String.valueOf(snapshot.productId()),
				String.valueOf(snapshot.storeId()),
				String.valueOf(snapshot.currentStock()),
				String.valueOf(snapshot.stockInTransit()));
		try {
			Files.writeString(filePath, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to append inventory snapshot to " + filePath, e);
		}
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
