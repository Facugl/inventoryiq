package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CsvFileType;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.InventoryCandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.application.port.out.InventoryIngestionRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleIngestionRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import com.inventoryiq.domain.model.Inventory;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contraparte de IngestCsvFileServiceTest para CsvFileType.INVENTORY:
 * misma orquestación (integridad referencial, duplicados, umbral del 5%
 * todo-o-nada) con los puertos de salida FAKEADOS en memoria. No
 * ejercita el parseo de CSV (eso es InventoryCsvRowParserTest, en
 * adapters/) — acá los InventoryCandidateRow ya vienen armados a mano.
 *
 * Igual que en el test de ventas, las pruebas de rechazo por regla
 * puntual acompañan la fila problemática con 19 filas válidas: una sola
 * fila rechazada sobre un total de 1 sería 100% de rechazo y dispararía
 * el umbral crítico antes de poder observar el rechazo puntual en sí.
 */
class IngestCsvFileServiceInventoryTest {

	private static final Long PRODUCT_ID = 1001L;
	private static final Long OTHER_PRODUCT_ID = 1002L;
	private static final Long STORE_ID = 1L;

	@Test
	void acceptsValidCandidatesAndPersistsThem() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		IngestCsvFileCommand command = command(1,
				List.of(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03"))), List.of());

		IngestCsvFileResult result = service(products, stores, inventory).execute(command);

		assertEquals(1, result.totalRowsRead());
		assertEquals(1, result.acceptedCount());
		assertEquals(0, result.rejectedCount());
		assertEquals(1, inventory.saved.size());
	}

	/** El inventoryId lo asigna el adaptador de persistencia, no la fila del CSV (ver InventoryIngestionRepository). */
	@Test
	void persistsTheSnapshotFieldsThroughThePortAndLetsItAssignTheId() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		InventoryCandidateRow candidate = new InventoryCandidateRow(1,
				new Inventory(null, LocalDate.parse("2025-07-03"), PRODUCT_ID, STORE_ID, 314, 120));

		service(products, stores, inventory).execute(command(1, List.of(candidate), List.of()));

		Inventory persisted = inventory.saved.get(0);
		assertEquals(LocalDate.parse("2025-07-03"), persisted.snapshotDate());
		assertEquals(PRODUCT_ID, persisted.productId());
		assertEquals(STORE_ID, persisted.storeId());
		assertEquals(314, persisted.currentStock());
		assertEquals(120, persisted.stockInTransit());
		assertEquals(1L, persisted.inventoryId());
	}

	@Test
	void rejectsARowWhoseProductDoesNotExistAndDoesNotPersistIt() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(OTHER_PRODUCT_ID); // PRODUCT_ID no existe
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		List<InventoryCandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));

		IngestCsvFileResult result = service(products, stores, inventory)
				.execute(command(candidates.size(), candidates, List.of()));

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertTrue(inventory.saved.stream().noneMatch(s -> s.productId().equals(PRODUCT_ID)));
	}

	@Test
	void rejectsARowWhoseStoreDoesNotExist() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		products.add(OTHER_PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository(); // ninguna sucursal existe
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		List<InventoryCandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		IngestCsvFileCommand command = command(candidates.size(), candidates, List.of());

		var service = service(products, stores, inventory);
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
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();
		inventory.existing.add(PRODUCT_ID + "|" + STORE_ID + "|2025-07-03");

		List<InventoryCandidateRow> candidates = withValidPadding(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));

		IngestCsvFileResult result = service(products, stores, inventory)
				.execute(command(candidates.size(), candidates, List.of()));

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertTrue(inventory.saved.stream().noneMatch(s -> s.productId().equals(PRODUCT_ID)));
	}

	@Test
	void rejectsTheSecondOfTwoDuplicateRowsWithinTheSameBatch() {
		// nada persistido todavía: el duplicado es entre dos filas del propio archivo, no contra datos previos.
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		products.add(OTHER_PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		List<InventoryCandidateRow> candidates = new ArrayList<>();
		candidates.add(candidateRow(1, PRODUCT_ID, LocalDate.parse("2025-07-03")));
		candidates.add(candidateRow(2, PRODUCT_ID, LocalDate.parse("2025-07-03"))); // mismo producto+sucursal+fecha
		for (int i = 0; i < 18; i++) {
			candidates.add(candidateRow(i + 3, OTHER_PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}

		IngestCsvFileResult result = service(products, stores, inventory)
				.execute(command(candidates.size(), candidates, List.of()));

		assertEquals(candidates.size() - 1, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertEquals(2, result.rejections().get(0).rowNumber());
		assertEquals(1, inventory.saved.stream().filter(s -> s.productId().equals(PRODUCT_ID)).count());
	}

	@Test
	void aRejectionRateExactlyAtTheThresholdDoesNotAbortTheBatch() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		List<InventoryCandidateRow> candidates = new ArrayList<>();
		for (int i = 0; i < 19; i++) {
			candidates.add(candidateRow(i + 1, PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		// 1 rechazo de pre-validación sobre 20 filas leídas = 5% exacto (no supera el umbral).
		IngestCsvFileResult result = service(products, stores, inventory)
				.execute(command(20, candidates, List.of(new RowRejection(20, "stock_actual negativo"))));

		assertEquals(19, result.acceptedCount());
		assertEquals(1, result.rejectedCount());
		assertEquals(19, inventory.saved.size());
	}

	@Test
	void throwsAndPersistsNothingWhenTheRejectionRateExceedsTheThreshold() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(PRODUCT_ID);
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(STORE_ID);
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		List<InventoryCandidateRow> candidates = new ArrayList<>();
		for (int i = 0; i < 18; i++) {
			candidates.add(candidateRow(i + 1, PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		// 2 rechazos sobre 20 filas leídas = 10%, supera el umbral del 5%.
		IngestCsvFileCommand command = command(20, candidates,
				List.of(new RowRejection(19, "fecha inválida"), new RowRejection(20, "stock_actual negativo")));

		var service = service(products, stores, inventory);

		assertThrows(CsvIngestionThresholdExceededException.class, () -> service.execute(command));
		assertTrue(inventory.saved.isEmpty()); // nada se persistió, ni las 18 filas válidas
	}

	// ---- helpers ----

	private static IngestCsvFileService service(
			FakeProductRepository products, FakeStoreRepository stores, FakeInventoryIngestionRepository inventory) {
		return new IngestCsvFileService(products, stores, new UnusedSaleIngestionRepository(), inventory);
	}

	private static IngestCsvFileCommand command(
			int totalRowsRead, List<InventoryCandidateRow> candidates, List<RowRejection> preValidationRejections) {
		return new IngestCsvFileCommand(
				CsvFileType.INVENTORY, totalRowsRead, List.of(), candidates, preValidationRejections);
	}

	/** La fila problemática (row 1) más 19 filas válidas del OTHER_PRODUCT_ID, para mantener el rechazo bajo el umbral. */
	private static List<InventoryCandidateRow> withValidPadding(InventoryCandidateRow problemRow) {
		List<InventoryCandidateRow> candidates = new ArrayList<>();
		candidates.add(problemRow);
		for (int i = 0; i < 19; i++) {
			candidates.add(candidateRow(i + 2, OTHER_PRODUCT_ID, LocalDate.parse("2025-07-01").plusDays(i)));
		}
		return candidates;
	}

	private static InventoryCandidateRow candidateRow(int rowNumber, Long productId, LocalDate snapshotDate) {
		return new InventoryCandidateRow(rowNumber, new Inventory(null, snapshotDate, productId, STORE_ID, 100, 0));
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

	private static class FakeInventoryIngestionRepository implements InventoryIngestionRepository {
		private final List<Inventory> saved = new ArrayList<>();
		private final Set<String> existing = new HashSet<>();
		private final AtomicLong nextId = new AtomicLong(1);

		@Override
		public boolean existsByProductStoreAndSnapshotDate(Long productId, Long storeId, LocalDate snapshotDate) {
			return existing.contains(productId + "|" + storeId + "|" + snapshotDate);
		}

		@Override
		public Inventory save(LocalDate snapshotDate, Long productId, Long storeId, int currentStock, int stockInTransit) {
			Inventory snapshot = new Inventory(
					nextId.getAndIncrement(), snapshotDate, productId, storeId, currentStock, stockInTransit);
			saved.add(snapshot);
			return snapshot;
		}
	}

	/** El camino INVENTORY no debe tocar el puerto de ventas: cualquier uso rompe el test. */
	private static class UnusedSaleIngestionRepository implements SaleIngestionRepository {

		@Override
		public boolean existsByProductStoreAndDate(Long productId, Long storeId, LocalDate date) {
			throw new AssertionError("El camino INVENTORY no debería consultar SaleIngestionRepository");
		}

		@Override
		public void save(Sale sale) {
			throw new AssertionError("El camino INVENTORY no debería persistir ventas");
		}
	}
}
