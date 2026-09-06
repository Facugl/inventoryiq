package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.InventorySnapshotResult;
import com.inventoryiq.application.port.in.RecordInventoryCountCommand;
import com.inventoryiq.application.port.out.InventoryIngestionRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.ProductNotFoundException;
import com.inventoryiq.domain.exception.StoreNotFoundException;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Store;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica RecordInventoryCountService: valida que producto y sucursal
 * existan (mismo criterio de integridad referencial que IngestCsvFileService
 * usa para ventas) antes de delegar en InventoryIngestionRepository.save(),
 * y que stockInTransit siempre se registre en 0 (un conteo físico no releva
 * "lo que está en camino" — ver Javadoc de RecordInventoryCountCommand).
 */
class RecordInventoryCountServiceTest {

	private static final LocalDate COUNT_DATE = LocalDate.parse("2026-09-05");

	@Test
	void savesTheCountAndReturnsTheResultingSnapshot() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(new Product(1008L, "QUE-1008", "Queso Cremoso 300g", 2L, 1L, "UN", BigDecimal.TEN, BigDecimal.valueOf(20), new LeadTime(3), true));
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(new Store(1L, "Sucursal Centro", "Av. Siempre Viva 742", true));
		FakeInventoryIngestionRepository inventory = new FakeInventoryIngestionRepository();

		RecordInventoryCountService service = new RecordInventoryCountService(products, stores, inventory);

		InventorySnapshotResult result = service.execute(new RecordInventoryCountCommand(1008L, 1L, COUNT_DATE, 35));

		assertEquals(new InventorySnapshotResult(1L, COUNT_DATE, 1008L, 1L, 35, 0), result);
	}

	@Test
	void rejectsAnUnknownProduct() {
		FakeProductRepository products = new FakeProductRepository();
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(new Store(1L, "Sucursal Centro", "Av. Siempre Viva 742", true));

		RecordInventoryCountService service = new RecordInventoryCountService(products, stores, new FakeInventoryIngestionRepository());

		assertThrows(ProductNotFoundException.class,
				() -> service.execute(new RecordInventoryCountCommand(9999L, 1L, COUNT_DATE, 10)));
	}

	@Test
	void rejectsAnUnknownStore() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(new Product(1008L, "QUE-1008", "Queso Cremoso 300g", 2L, 1L, "UN", BigDecimal.TEN, BigDecimal.valueOf(20), new LeadTime(3), true));

		RecordInventoryCountService service = new RecordInventoryCountService(products, new FakeStoreRepository(), new FakeInventoryIngestionRepository());

		assertThrows(StoreNotFoundException.class,
				() -> service.execute(new RecordInventoryCountCommand(1008L, 9999L, COUNT_DATE, 10)));
	}

	private static class FakeProductRepository implements ProductRepository {
		private final Map<Long, Product> products = new HashMap<>();

		void add(Product product) {
			products.put(product.productId(), product);
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

		void add(Store store) {
			stores.put(store.storeId(), store);
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
		private final AtomicLong nextId = new AtomicLong(1);

		@Override
		public Inventory save(LocalDate snapshotDate, Long productId, Long storeId, int currentStock, int stockInTransit) {
			Inventory snapshot = new Inventory(nextId.getAndIncrement(), snapshotDate, productId, storeId, currentStock, stockInTransit);
			saved.add(snapshot);
			return snapshot;
		}
	}
}
