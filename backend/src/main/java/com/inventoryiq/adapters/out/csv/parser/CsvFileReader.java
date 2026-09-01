package com.inventoryiq.adapters.out.csv.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lee un archivo CSV con header por nombre de columna (robusto a que las
 * columnas cambien de orden). Usa Apache Commons CSV en vez de un split(",")
 * manual porque algunos campos reales traen comas entre comillas
 * (por ejemplo, direcciones en sucursales.csv).
 */
public final class CsvFileReader {
	private CsvFileReader() {
	}

	public static List<CSVRecord> readRecords(Path filePath) {
		try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			return readContent(reader).records();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read CSV file: " + filePath, e);
		}
	}

	/** Header + filas de una fuente que no es un archivo en disco (por ejemplo, un CSV subido por HTTP). */
	public static CsvContent readContent(Reader reader) {
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.build();

		try (CSVParser parser = format.parse(reader)) {
			return new CsvContent(new LinkedHashSet<>(parser.getHeaderNames()), parser.getRecords());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to parse CSV content", e);
		}
	}

	public record CsvContent(Set<String> headerColumns, List<CSVRecord> records) {
	}
}
