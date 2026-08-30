package com.inventoryiq.application.usecase;

import com.inventoryiq.adapters.out.csv.CsvCategoryRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvInventoryRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvProductRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvSaleRepositoryAdapter;
import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.service.CriticalityEvaluator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice completo, de punta a punta, contra los 8 CSV reales de data/csv
 * (no fixtures): CsvXxxRepositoryAdapter -> GetCriticalProductsService.
 * Los dos primeros casos se verificaron A MANO contra ventas.csv/inventario.csv
 * (ver comentarios), cumpliendo el Definition of Done de la Fase 2 del roadmap
 * ("podés cruzarla a mano contra 2-3 productos que ya viste").
 */
class GetCriticalProductsIntegrationTest {

	// Working directory de Maven Surefire = backend/, igual que el default
	// de application.yml (inventoryiq.csv.base-path=../data/csv).
	private static final Path CSV_BASE_PATH = Path.of("../data/csv");

	private static GetCriticalProductsService buildService() {
		var products = new CsvProductRepositoryAdapter(CSV_BASE_PATH);
		var categories = new CsvCategoryRepositoryAdapter(CSV_BASE_PATH);
		var sales = new CsvSaleRepositoryAdapter(CSV_BASE_PATH);
		var inventory = new CsvInventoryRepositoryAdapter(CSV_BASE_PATH);
		var weights = new CriticalityEvaluator.CriticalityWeights(1.0 / 3, 1.0 / 3, 1.0 / 3);
		return new GetCriticalProductsService(products, categories, sales, inventory, weights);
	}

	@Test
	void detectsAKnownStockoutAsCritical() {
		// Verificado a mano: inventario.csv fila 906534 -> producto 1006
		// (Crema de Leche 200ml), sucursal 2, fecha_snapshot=2026-02-01,
		// stock_actual=0. Stock=0 implica CRITICAL sin importar ADS/ROP
		// (regla 4.12), así que este caso no depende de ninguna fórmula,
		// solo de que el dato llegue correctamente desde el CSV.
		var service = buildService();
		var query = GetCriticalProductsQuery.of(2L, null, null, LocalDate.parse("2026-02-01"));

		List<CriticalProductResult> results = service.execute(query);

		Optional<CriticalProductResult> product1006 = results.stream()
				.filter(r -> r.productId().equals(1006L))
				.findFirst();

		assertTrue(product1006.isPresent(), "El producto 1006 (stock=0 en sucursal 2) debería aparecer como crítico");
		assertEquals(ProductStatus.CRITICAL, product1006.get().status());
		assertEquals(0, product1006.get().currentStock());
	}

	@Test
	void excludesAKnownOverstockedProductFromTheCriticalList() {
		// Verificado a mano contra producto 1001 (Leche Entera 1L, lead_time=4,
		// categoría 2 con umbral_max_cobertura_dias=12 y dias_cobertura_extra_default=3),
		// sucursal 1, ventana de 7 días 2026-01-09 a 2026-01-15 (ventas.csv):
		// unidades vendidas 77+43+25+63+35+92+26 = 361 -> ADS = 361/7 = 51.5714...
		// ninguno de esos 7 días tuvo quiebre (todos los cierres del día anterior > 0).
		// ROP = ADS*(leadTime+extraCoverage) = ADS*7 = 361 (exacto).
		// currentStock (inventario.csv, 2026-01-15) = 753 > ROP(361) -> no repone.
		// Cobertura actual = 753/51.5714 = 14.6 días > umbral(12) -> OVERSTOCK,
		// por lo tanto queda FUERA del alcance de este caso de uso.
		var service = buildService();
		var query = new GetCriticalProductsQuery(1L, null, null, LocalDate.parse("2026-01-15"), 7);

		List<CriticalProductResult> results = service.execute(query);

		assertTrue(results.stream().noneMatch(r -> r.productId().equals(1001L)),
				"El producto 1001 está en sobrestock en esta fecha/ventana, no debería listarse como crítico");
	}

	@Test
	void allResultsAreCriticalOrRequireReplenishmentSortedByScoreDescending() {
		var service = buildService();
		// Fecha de corte del dataset simulado (README_datos_simulados.md).
		var query = GetCriticalProductsQuery.of(1L, null, null, LocalDate.parse("2026-08-01"));

		List<CriticalProductResult> results = service.execute(query);

		assertFalse(results.isEmpty(), "Con 62 productos reales en sucursal 1 debería haber al menos un crítico");
		assertTrue(results.stream().allMatch(r ->
				r.status() == ProductStatus.CRITICAL || r.status() == ProductStatus.REQUIRES_REPLENISHMENT));
		assertTrue(results.stream().allMatch(r ->
				r.criticalityLevel().score() >= 0 && r.criticalityLevel().score() <= 100));

		for (int i = 1; i < results.size(); i++) {
			assertTrue(results.get(i - 1).criticalityLevel().score() >= results.get(i).criticalityLevel().score(),
					"El resultado debe estar ordenado por score descendente");
		}
	}
}
