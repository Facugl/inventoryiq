package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CandidateRow;
import com.inventoryiq.application.port.in.CsvFileType;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleIngestionRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.Store;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la orquestación de IngestCsvFileService con los 3 puertos de
 * salida FAKEADOS en memoria, mismo criterio que los otros casos de uso
 * de este proyecto. No ejercita el parseo de CSV (eso es
 * SalesCsvRowParserTest, en adapters/) — acá los CandidateRow ya vienen
 * armados a mano.
 *
 * Las pruebas de rechazo por regla puntual (producto/sucursal
 * inexistente, duplicado) acompañan la fila problemática con filas
 * válidas: una sola fila rechazada sobre un total de 1 sería 100% de
 * rechazo y dispararía el umbral crítico del 5% antes de poder observar
 * el rechazo puntual en sí.
 */
class IngestCsvFileServiceTest {

	private static final Long PRODUCT_ID = 1001L;
	private static final Long OTHER_PRODUCT_ID = 1002L;
	private static final Long STORE_ID = 1L;

	@Test
	void acceptsValidCandidatesAndPersistsThem() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, 1,
				List.of(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03"))), List.of());

		IngestCsvFileResult result = service(products, stores, sales).execute(command);

		assertEquals(1, result.totalRowsRead());
		assertEquals(1, result.acceptedCount());
		assertEquals(0, result.rejectedCount());
		assertEquals(1, sales.saved.size());
	}

	@Test
	void rejectsARowWhoseProductDoesNotExistAndDoesNotPersistIt() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(OTHER_PRODUCT_ID); // PRODUCT_ID no existe
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		List<CandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, candidates.size(), candidates, List.of());

		IngestCsvFileResult result = service(products, stores, sales).execute(command);

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertTrue(sales.saved.stream().noneMatch(s -> s.productId().equals(PRODUCT_ID)));
	}

	@Test
	void rejectsARowWhoseStoreDoesNotExist() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		products.add(OTHER_PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository(); // ninguna sucursal existe
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		List<CandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, candidates.size(), candidates, List.of());

		var service = service(products, stores, sales);
		assertThrows(CsvIngestionThresholdExceededException.class, () -> service.execute(command));
		// ninguna sucursal existe: TODAS las filas se rechazan, siempre supera el umbral -> el test valida eso.
	}

	@Test
	void rejectsADuplicateRow() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		products.add(OTHER_PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();
		sales.existing.add(PRODUCT_ID + "|" + STORE_ID + "|2025-07-03");

		List<CandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, candidates.size(), candidates, List.of());

		IngestCsvFileResult result = service(products, stores, sales).execute(command);

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertTrue(sales.saved.stream().noneMatch(s -> s.productId().equals(PRODUCT_ID)));
	}

	@Test
	void rejectsTheSecondOfTwoDuplicateRowsWithinTheSameBatch() {
		// nada persistido todavía: el duplicado es entre dos filas del propio archivo, no contra datos previos.
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		products.add(OTHER_PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		List<CandidateRow> candidates = new ArrayList<>();
		candidates.add(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		candidates.add(candidateRow(2, PRODUCT_ID, LocalDate.parse("2025-07-03"))); // mismo producto+sucursal+fecha
		for (int i = 0; i < 18; i++) {
			candidates.add(candidateRow(i + 3, OTHER_PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, candidates.size(), candidates, List.of());

		IngestCsvFileResult result = service(products, stores, sales).execute(command);

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertEquals(2, result.rejections().get(0).rowNumber());
		assertEquals(1, sales.saved.stream().filter(s -> s.productId().equals(PRODUCT_ID)).count());
	}

	@Test
	void aRejectionRateExactlyAtTheThresholdDoesNotAbortTheBatch() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		List<CandidateRow> candidates = new ArrayList<>();
		for (int i = 0; i < 19; i++) {
			candidates.add(candidateRow(i + 1, PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		// 1 rechazo de pre-validación sobre 20 filas leídas = 5% exacto (no supera el umbral).
		IngestCsvFileCommand command = new IngestCsvFileCommand(
				CsvFileType.SALES, 20, candidates, List.of(new RowRejection(20, "fecha inválida")));

		IngestCsvFileResult result = service(products, stores, sales).execute(command);

		assertEquals(19, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertEquals(19, sales.saved.size());
	}

	@Test
	void throwsAndPersistsNothingWhenTheRejectionRateExceedsTheThreshold() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeSaleIngestionRepository sales = new FakeSaleIngestionRepository();

		List<CandidateRow> candidates = new ArrayList<>();
		for (int i = 0; i < 18; i++) {
			candidates.add(candidateRow(i + 1, PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		// 2 rechazos sobre 20 filas leídas = 10%, supera el umbral del 5%.
		IngestCsvFileCommand command = new IngestCsvFileCommand(CsvFileType.SALES, 20, candidates,
				List.of(new RowRejection(19, "fecha inválida"), new RowRejection(20, "unidades negativas")));

		var service = service(products, stores, sales);

		assertThrows(CsvIngestionThresholdExceededException.class, () -> service.execute(command));
		assertTrue(sales.saved.isEmpty()); // nada se persistió, ni las 18 filas válidas
	}

	// ---- helpers ----

	private static IngestCsvFileService service(
			FakeProductRepository products, FakeStoreRepository stores, FakeSaleIngestionRepository sales) {
		return new IngestCsvFileService(products, stores, sales);
	}

	/** La fila problemática (row 1) más 19 filas válidas del OTHER_PRODUCT_ID, para mantener el rechazo bajo el umbral. */
	private static List<CandidateRow> withValidPadding(CandidateRow problemRow) {
		List<CandidateRow> candidates = new ArrayList<>();
		candidates.add(problemRow);
		for (int i = 0; i < 19; i++) {
			candidates.add(candidateRow(i + 2, OTHER_PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		return candidates;
	}

	private static CandidateRow candidateRow(int rowNumber, Long productId, LocalDate date) {
		return new CandidateRow(rowNumber, new Sale(
				(long) (900000 + rowNumber), date, productId, STORE_ID, 10, new BigDecimal("1000.00")));
	}

	private static class FakeProductRepository implements ProductRepository {
		private final Map<Long, Product> products = new HashMap<>();

		void add(Long productId) {
			products.put(productId, new Product(productId, "SKU-" + productId, "Producto " + productId, 1L, 1L, "UN",
					new BigDecimal("10.00"), new BigDecimal("15.00"), new LeadTime(3), true));
		}

		@Override
		public List<Product> findAllActive() {
			return products.values().stream().filter(Product::active).toList();
		}

		@Override
		public Optional<Product> findById(Long productId) {
			return Optional.ofNullable(products.get(productId));
		}
	}

	private static class FakeStoreRepository implements StoreRepository {
		private final Map<Long, Store> stores = new HashMap<>();

		void add(Long storeId) {
			stores.put(storeId, new Store(storeId, "Sucursal " + storeId, "dirección", true));
		}

		@Override
		public Optional<Store> findById(Long storeId) {
			return Optional.ofNullable(stores.get(storeId));
		}

		@Override
		public List<Store> findAllActive() {
			return stores.values().stream().filter(Store::active).toList();
		}
	}

	private static class FakeSaleIngestionRepository implements SaleIngestionRepository {
		private final List<Sale> saved = new ArrayList<>();
		private final Set<String> existing = new HashSet<>();

		@Override
		public boolean existsByProductStoreAndDate(Long productId, Long storeId, LocalDate date) {
			return existing.contains(productId + "|" + storeId + "|" + date);
		}

		@Override
		public void save(Sale sale) {
			saved.add(sale);
		}
	}
}
