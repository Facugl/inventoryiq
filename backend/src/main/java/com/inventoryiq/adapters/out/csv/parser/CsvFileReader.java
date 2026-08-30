package com.inventoryiq.adapters.out.csv.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.build();

		try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
				CSVParser parser = format.parse(reader)) {
			return parser.getRecords();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read CSV file: " + filePath, e);
		}
	}
}
