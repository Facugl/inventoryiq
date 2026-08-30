package com.inventoryiq.application.usecase.shared;

import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.Category;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;
import com.inventoryiq.domain.service.AdsCalculator;
import com.inventoryiq.domain.service.DailySalesRecordAssembler;
import com.inventoryiq.domain.service.OverstockDetector;
import com.inventoryiq.domain.service.ProductStatusEvaluator;
import com.inventoryiq.domain.service.ReorderPointCalculator;
import com.inventoryiq.domain.service.SafetyStockCalculator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Orquestación compartida por los casos de uso que necesitan evaluar el
 * estado de inventario de un producto en una sucursal (ADS corregido,
 * stock de seguridad, punto de pedido, cobertura actual y estado —
 * Secciones 4.3, 4.4, 4.8, 4.12). Extraído de GetCriticalProductsService
 * al escribir DetectOverstockService, que necesita exactamente el mismo
 * cálculo antes de aplicar su propio criterio de filtrado.
 *
 * Vive en application/, no en domain/, porque orquesta puertos de salida
 * (CategoryRepository, InventoryRepository) — el dominio no puede depender
 * de puertos, solo de sus propios servicios de cálculo puro.
 */
public class ProductIndicatorsCalculator {

	private final CategoryRepository categoryRepository;
	private final InventoryRepository inventoryRepository;

	public ProductIndicatorsCalculator(CategoryRepository categoryRepository, InventoryRepository inventoryRepository) {
		this.categoryRepository = categoryRepository;
		this.inventoryRepository = inventoryRepository;
	}

	public record ProductIndicators(
			int currentStock,
			double ads,
			SafetyStock safetyStock,
			ReorderPoint reorderPoint,
			double currentDaysOfCoverage,
			ProductStatus status) {
	}

	/**
	 * Vacío si no hay historial de ventas suficiente para calcular ADS, si no
	 * hay snapshot de inventario vigente, o si la categoría del producto no
	 * existe — un dato incompleto de un producto no debe tirar abajo el
	 * cálculo del resto del catálogo, así que cada caso de uso que llama a
	 * esto debe simplemente descartar ese producto.
	 */
	public Optional<ProductIndicators> calculate(
			Product product, Long storeId, LocalDate windowStart, LocalDate referenceDate, List<Sale> sales) {

		// findLatestSnapshotAsOf (más abajo) se mantiene como una consulta
		// independiente de este rango, a propósito: busca el snapshot vigente
		// más reciente en el historial completo, no solo dentro de la ventana
		// de ventas. Son alcances distintos (ventana acotada vs. búsqueda sin
		// límite hacia atrás), así que no es la misma consulta duplicada.
		List<Inventory> snapshots = inventoryRepository.findSnapshotsInRange(
				product.productId(), storeId, windowStart.minusDays(1), referenceDate);
		List<DailySalesRecord> dailyRecords = DailySalesRecordAssembler.assemble(sales, snapshots);

		double ads;
		try {
			ads = AdsCalculator.calculateCorrectedAds(dailyRecords);
		} catch (InvalidDomainDataException e) {
			return Optional.empty();
		}

		Optional<Inventory> latestSnapshot = inventoryRepository.findLatestSnapshotAsOf(
				product.productId(), storeId, referenceDate);
		if (latestSnapshot.isEmpty()) {
			return Optional.empty();
		}

		Optional<Category> category = categoryRepository.findById(product.categoryId());
		if (category.isEmpty()) {
			return Optional.empty();
		}

		int currentStock = latestSnapshot.get().currentStock();

		SafetyStock safetyStock = SafetyStockCalculator.calculateSimplifiedMethod(ads, category.get().defaultExtraCoverageDays());
		ReorderPoint reorderPoint = ReorderPointCalculator.calculate(ads, product.leadTime(), safetyStock);
		double currentDaysOfCoverage = OverstockDetector.calculateCurrentDaysOfCoverage(currentStock, ads);

		ProductStatus status = ProductStatusEvaluator.evaluate(new ProductStatusEvaluator.EvaluationContext(
				currentStock, reorderPoint, safetyStock, currentDaysOfCoverage,
				category.get().maxCoverageDaysThreshold(), false));

		return Optional.of(new ProductIndicators(currentStock, ads, safetyStock, reorderPoint, currentDaysOfCoverage, status));
	}
}
