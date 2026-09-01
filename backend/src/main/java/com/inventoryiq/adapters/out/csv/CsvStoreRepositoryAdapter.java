package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.model.Store;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador de salida — lee sucursales.csv (Sección 5.4), indexado por id. */
public class CsvStoreRepositoryAdapter implements StoreRepository {

	private static final String FILE_NAME = "sucursales.csv";

	private final Map<Long, Store> storesById;

	public CsvStoreRepositoryAdapter(Path csvBasePath) {
		List<CSVRecord> records = CsvFileReader.readRecords(csvBasePath.resolve(FILE_NAME));
		this.storesById = records.stream()
				.map(CsvStoreRepositoryAdapter::toStore)
				.collect(Collectors.toMap(Store::storeId, store -> store));
	}

	@Override
	public Optional<Store> findById(Long storeId) {
		return Optional.ofNullable(storesById.get(storeId));
	}

	@Override
	public List<Store> findAllActive() {
		return storesById.values().stream().filter(Store::active).toList();
	}

	private static Store toStore(CSVRecord record) {
		return new Store(
				CsvFieldParsers.parseLong(record.get("sucursal_id")),
				record.get("nombre"),
				record.get("direccion"),
				CsvFieldParsers.parseBoolean(record.get("activa")));
	}
}
