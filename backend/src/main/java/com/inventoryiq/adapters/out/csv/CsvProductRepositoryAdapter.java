package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida — lee productos.csv (Sección 5.1) y lo carga
 * indexado en memoria una sola vez, al construirse. El catálogo es chico
 * (decenas de SKUs para un supermercado), así que no hay costo real en
 * mantenerlo entero en memoria y evita releer el archivo en cada consulta.
 */
public class CsvProductRepositoryAdapter implements ProductRepository {

	private static final String FILE_NAME = "productos.csv";

	private final Map<Long, Product> productsById;

	public CsvProductRepositoryAdapter(Path csvBasePath) {
		List<CSVRecord> records = CsvFileReader.readRecords(csvBasePath.resolve(FILE_NAME));
		this.productsById = records.stream()
				.map(CsvProductRepositoryAdapter::toProduct)
				.collect(Collectors.toMap(Product::productId, product -> product));
	}

	@Override
	public List<Product> findAllActive() {
		return productsById.values().stream()
				.filter(Product::active)
				.toList();
	}

	@Override
	public Optional<Product> findById(Long productId) {
		return Optional.ofNullable(productsById.get(productId));
	}

	private static Product toProduct(CSVRecord record) {
		return new Product(
				CsvFieldParsers.parseLong(record.get("producto_id")),
				record.get("sku"),
				record.get("nombre"),
				CsvFieldParsers.parseLong(record.get("categoria_id")),
				CsvFieldParsers.parseLong(record.get("proveedor_id")),
				record.get("unidad_medida"),
				CsvFieldParsers.parseDecimal(record.get("precio_costo")),
				CsvFieldParsers.parseDecimal(record.get("precio_venta")),
				new LeadTime(CsvFieldParsers.parseInt(record.get("lead_time_dias"))),
				CsvFieldParsers.parseBoolean(record.get("activo")));
	}
}
