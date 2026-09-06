package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.SupplierRepository;
import com.inventoryiq.domain.exception.SupplierNotFoundException;
import com.inventoryiq.domain.model.Supplier;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida — lee y escribe proveedores.csv (Sección 5.3). A
 * diferencia de CsvInventoryRepositoryAdapter (que solo agrega filas al
 * final), acá updateLeadTime() reescribe el archivo completo: un proveedor
 * es una entidad mutable, no un registro histórico, así que no tiene
 * sentido "agregar una versión nueva" — se corrige el valor existente,
 * igual que PostgresRecommendationRepositoryAdapter.save() hace un upsert.
 * El orden de inserción original se preserva con un LinkedHashMap para que
 * reescribir el archivo no reordene filas que el usuario no tocó.
 */
public class CsvSupplierRepositoryAdapter implements SupplierRepository {

	private static final String FILE_NAME = "proveedores.csv";
	private static final String HEADER = "proveedor_id,razon_social,lead_time_promedio_dias,condicion_pago,activo";

	private final Path filePath;
	private final Map<Long, Supplier> suppliersById;

	public CsvSupplierRepositoryAdapter(Path csvBasePath) {
		this.filePath = csvBasePath.resolve(FILE_NAME);
		List<CSVRecord> records = CsvFileReader.readRecords(filePath);
		this.suppliersById = records.stream()
				.map(CsvSupplierRepositoryAdapter::toSupplier)
				.collect(Collectors.toMap(
						Supplier::supplierId, supplier -> supplier, (a, b) -> b, LinkedHashMap::new));
	}

	@Override
	public Optional<Supplier> findById(Long supplierId) {
		return Optional.ofNullable(suppliersById.get(supplierId));
	}

	@Override
	public List<Supplier> findAllActive() {
		return suppliersById.values().stream().filter(Supplier::active).toList();
	}

	@Override
	public synchronized Supplier updateLeadTime(Long supplierId, LeadTime newLeadTime) {
		Supplier existing = suppliersById.get(supplierId);
		if (existing == null) {
			throw new SupplierNotFoundException(supplierId);
		}

		Supplier updated = new Supplier(
				existing.supplierId(), existing.businessName(), newLeadTime, existing.paymentTerms(), existing.active());
		suppliersById.put(supplierId, updated);
		rewriteFile();

		return updated;
	}

	private void rewriteFile() {
		StringBuilder content = new StringBuilder(HEADER).append(System.lineSeparator());
		for (Supplier supplier : suppliersById.values()) {
			content.append(toLine(supplier)).append(System.lineSeparator());
		}

		try {
			Files.writeString(filePath, content.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to rewrite " + filePath, e);
		}
	}

	private static String toLine(Supplier supplier) {
		return String.join(",",
				String.valueOf(supplier.supplierId()),
				supplier.businessName(),
				String.valueOf(supplier.averageLeadTime().days()),
				supplier.paymentTerms(),
				supplier.active() ? "True" : "False");
	}

	private static Supplier toSupplier(CSVRecord record) {
		return new Supplier(
				CsvFieldParsers.parseLong(record.get("proveedor_id")),
				record.get("razon_social"),
				new LeadTime(CsvFieldParsers.parseInt(record.get("lead_time_promedio_dias"))),
				record.get("condicion_pago"),
				CsvFieldParsers.parseBoolean(record.get("activo")));
	}
}
