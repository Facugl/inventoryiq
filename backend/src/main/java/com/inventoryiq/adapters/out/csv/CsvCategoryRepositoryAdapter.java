package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.domain.model.Category;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador de salida — lee categorias.csv (Sección 5.2). */
public class CsvCategoryRepositoryAdapter implements CategoryRepository {

	private static final String FILE_NAME = "categorias.csv";

	private final Map<Long, Category> categoriesById;

	public CsvCategoryRepositoryAdapter(Path csvBasePath) {
		List<CSVRecord> records = CsvFileReader.readRecords(csvBasePath.resolve(FILE_NAME));
		this.categoriesById = records.stream()
				.map(CsvCategoryRepositoryAdapter::toCategory)
				.collect(Collectors.toMap(Category::categoryId, category -> category));
	}

	@Override
	public Optional<Category> findById(Long categoryId) {
		return Optional.ofNullable(categoriesById.get(categoryId));
	}

	private static Category toCategory(CSVRecord record) {
		return new Category(
				CsvFieldParsers.parseLong(record.get("categoria_id")),
				record.get("nombre"),
				CsvFieldParsers.parseNullableLong(record.get("categoria_padre_id")),
				CsvFieldParsers.parseIntFromDecimal(record.get("umbral_max_cobertura_dias")),
				CsvFieldParsers.parseIntFromDecimal(record.get("dias_cobertura_extra_default")));
	}
}
