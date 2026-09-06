package com.inventoryiq.adapters.out.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.domain.exception.CategoryNotFoundException;
import com.inventoryiq.domain.model.Category;
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
 * Adaptador de salida — lee y escribe categorias.csv (Sección 5.2). Igual
 * que CsvSupplierRepositoryAdapter, updateParameters() reescribe el
 * archivo completo (una categoría es una entidad mutable, no un registro
 * histórico); el orden original se preserva con un LinkedHashMap.
 */
public class CsvCategoryRepositoryAdapter implements CategoryRepository {

	private static final String FILE_NAME = "categorias.csv";
	private static final String HEADER = "categoria_id,nombre,categoria_padre_id,umbral_max_cobertura_dias,dias_cobertura_extra_default";

	private final Path filePath;
	private final Map<Long, Category> categoriesById;

	public CsvCategoryRepositoryAdapter(Path csvBasePath) {
		this.filePath = csvBasePath.resolve(FILE_NAME);
		List<CSVRecord> records = CsvFileReader.readRecords(filePath);
		this.categoriesById = records.stream()
				.map(CsvCategoryRepositoryAdapter::toCategory)
				.collect(Collectors.toMap(
						Category::categoryId, category -> category, (a, b) -> b, LinkedHashMap::new));
	}

	@Override
	public Optional<Category> findById(Long categoryId) {
		return Optional.ofNullable(categoriesById.get(categoryId));
	}

	@Override
	public List<Category> findAll() {
		return List.copyOf(categoriesById.values());
	}

	@Override
	public synchronized Category updateParameters(Long categoryId, int maxCoverageDaysThreshold, int defaultExtraCoverageDays) {
		Category existing = categoriesById.get(categoryId);
		if (existing == null) {
			throw new CategoryNotFoundException(categoryId);
		}

		Category updated = new Category(
				existing.categoryId(), existing.name(), existing.parentCategoryId(),
				maxCoverageDaysThreshold, defaultExtraCoverageDays);
		categoriesById.put(categoryId, updated);
		rewriteFile();

		return updated;
	}

	private void rewriteFile() {
		StringBuilder content = new StringBuilder(HEADER).append(System.lineSeparator());
		for (Category category : categoriesById.values()) {
			content.append(toLine(category)).append(System.lineSeparator());
		}

		try {
			Files.writeString(filePath, content.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to rewrite " + filePath, e);
		}
	}

	private static String toLine(Category category) {
		return String.join(",",
				String.valueOf(category.categoryId()),
				category.name(),
				category.parentCategoryId() == null ? "" : String.valueOf(category.parentCategoryId()),
				String.valueOf(category.maxCoverageDaysThreshold()),
				String.valueOf(category.defaultExtraCoverageDays()));
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
