package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.Sale;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Adaptador de salida — lee ventas.csv (Sección 5.5), indexado por producto+sucursal. */
public class CsvSaleRepositoryAdapter implements SaleRepository {

	private static final String FILE_NAME = "ventas.csv";

	private final Map<ProductStoreKey, List<Sale>> salesByProductAndStore;

	public CsvSaleRepositoryAdapter(Path csvBasePath) {
		List<CSVRecord> records = CsvFileReader.readRecords(csvBasePath.resolve(FILE_NAME));
		Map<ProductStoreKey, List<Sale>> grouped = records.stream()
				.map(CsvSaleRepositoryAdapter::toSale)
				.collect(Collectors.groupingBy(sale -> new ProductStoreKey(sale.productId(), sale.storeId())));
		grouped.replaceAll((key, sales) -> sales.stream().sorted(Comparator.comparing(Sale::date)).toList());
		this.salesByProductAndStore = grouped;
	}

	@Override
	public List<Sale> findByProductAndStore(Long productId, Long storeId, LocalDate from, LocalDate to) {
		List<Sale> sales = salesByProductAndStore.getOrDefault(new ProductStoreKey(productId, storeId), List.of());
		return sales.stream()
				.filter(sale -> !sale.date().isBefore(from) && !sale.date().isAfter(to))
				.toList();
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
