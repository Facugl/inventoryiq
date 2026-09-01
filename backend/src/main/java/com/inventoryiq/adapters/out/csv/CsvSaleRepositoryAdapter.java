package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.SaleIngestionRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.Sale;
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
import java.util.stream.Collectors;

/**
 * Adaptador de salida — lee ventas.csv (Sección 5.5), indexado por producto+sucursal.
 * Además implementa SaleIngestionRepository (Sección 9.9): save() agrega la
 * fila al final del archivo en disco y actualiza el índice en memoria del
 * mismo objeto, así una venta ingerida queda visible al resto de las
 * consultas sin reiniciar la aplicación. Ambos puertos deben resolver a
 * esta misma instancia (ver config/CsvAdaptersConfig) — si se
 * construyeran dos adaptadores separados, las altas de uno no las vería
 * el otro.
 */
public class CsvSaleRepositoryAdapter implements SaleRepository, SaleIngestionRepository {

	private static final String FILE_NAME = "ventas.csv";

	private final Path filePath;
	private final Map<ProductStoreKey, List<Sale>> salesByProductAndStore;

	public CsvSaleRepositoryAdapter(Path csvBasePath) {
		this.filePath = csvBasePath.resolve(FILE_NAME);
		List<CSVRecord> records = CsvFileReader.readRecords(filePath);
		Map<ProductStoreKey, List<Sale>> grouped = records.stream()
				.map(CsvSaleRepositoryAdapter::toSale)
				.collect(Collectors.groupingBy(sale -> new ProductStoreKey(sale.productId(), sale.storeId())));
		grouped.replaceAll((key, sales) -> new ArrayList<>(sales.stream().sorted(Comparator.comparing(Sale::date)).toList()));
		this.salesByProductAndStore = grouped;
	}

	@Override
	public List<Sale> findByProductAndStore(Long productId, Long storeId, LocalDate from, LocalDate to) {
		List<Sale> sales = salesByProductAndStore.getOrDefault(new ProductStoreKey(productId, storeId), List.of());
		return sales.stream()
				.filter(sale -> !sale.date().isBefore(from) && !sale.date().isAfter(to))
				.toList();
	}

	@Override
	public synchronized boolean existsByProductStoreAndDate(Long productId, Long storeId, LocalDate date) {
		List<Sale> sales = salesByProductAndStore.getOrDefault(new ProductStoreKey(productId, storeId), List.of());
		return sales.stream().anyMatch(sale -> sale.date().equals(date));
	}

	@Override
	public synchronized void save(Sale sale) {
		appendToFile(sale);
		ProductStoreKey key = new ProductStoreKey(sale.productId(), sale.storeId());
		List<Sale> sales = salesByProductAndStore.computeIfAbsent(key, k -> new ArrayList<>());
		sales.add(sale);
		sales.sort(Comparator.comparing(Sale::date));
	}

	private void appendToFile(Sale sale) {
		String line = String.join(",",
				String.valueOf(sale.saleId()),
				sale.date().toString(),
				String.valueOf(sale.productId()),
				String.valueOf(sale.storeId()),
				String.valueOf(sale.unitsSold()),
				sale.totalAmount().toPlainString());
		try {
			Files.writeString(filePath, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to append sale to " + filePath, e);
		}
	}

	private static Sale toSale(CSVRecord record) {
		return new Sale(
				CsvFieldParsers.parseLong(record.get("venta_id")),
				CsvFieldParsers.parseDate(record.get("fecha")),
				CsvFieldParsers.parseLong(record.get("producto_id")),
				CsvFieldParsers.parseLong(record.get("sucursal_id")),
				CsvFieldParsers.parseInt(record.get("unidades_vendidas")),
				CsvFieldParsers.parseDecimal(record.get("importe_total")));
	}
}
