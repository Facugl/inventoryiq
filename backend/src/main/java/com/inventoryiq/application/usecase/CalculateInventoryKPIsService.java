package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CalculateInventoryKPIsQuery;
import com.inventoryiq.application.port.in.CalculateInventoryKPIsUseCase;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.InventoryKPIsResult;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.in.OverstockSortBy;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.application.usecase.shared.ProductIndicatorsCalculator;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.RecommendationStatus;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.service.InventoryTurnoverCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de CalculateInventoryKPIsUseCase (Sección 8.8/9.7).
 *
 * A diferencia de lo que lista la Sección 9.7, no depende de
 * CompraRepository: ninguno de los 5 KPIs implementados lo necesita
 * (stock en tránsito ya viene resuelto en Inventory, y el costo de
 * mercadería vendida sale de Producto.costPrice × unidades vendidas, no
 * de compras.csv) — mismo tipo de simplificación que ya hicimos con
 * GenerateReorderSuggestions en el Slice 6.
 *
 * Tasa de quiebre de stock y días de cobertura promedio se calculan en
 * un único recorrido del catálogo activo vía ProductIndicatorsCalculator
 * (mismo patrón que GetCriticalProducts/DetectOverstock), a toDate.
 * Capital inmovilizado en sobrestock reutiliza DetectOverstockUseCase
 * tal cual. % de recomendaciones seguidas se mide contra las
 * recomendaciones ya RESUELTAS en el período (APPLIED + DISCARDED,
 * ignorando las PENDING: el comprador todavía no se expidió sobre
 * ellas, así que no deberían empeorar el KPI). Rotación promedio del
 * inventario = InventoryTurnoverCalculator (Sección 4.6, hasta ahora sin
 * usar) sobre los totales del catálogo en el período.
 */
public class CalculateInventoryKPIsService implements CalculateInventoryKPIsUseCase {

	private static final int SALES_WINDOW_DAYS = 90;

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final InventoryRepository inventoryRepository;
	private final DetectOverstockUseCase detectOverstockUseCase;
	private final RecommendationRepository recommendationRepository;
	private final ProductIndicatorsCalculator productIndicatorsCalculator;

	public CalculateInventoryKPIsService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository,
			DetectOverstockUseCase detectOverstockUseCase,
			RecommendationRepository recommendationRepository) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.inventoryRepository = inventoryRepository;
		this.detectOverstockUseCase = detectOverstockUseCase;
		this.recommendationRepository = recommendationRepository;
		this.productIndicatorsCalculator = new ProductIndicatorsCalculator(categoryRepository, inventoryRepository);
	}

	@Override
	public InventoryKPIsResult execute(CalculateInventoryKPIsQuery query) {
		List<Product> products = productRepository.findAllActive();

		StockSnapshotKPIs stockSnapshotKPIs = calculateStockSnapshotKPIs(query, products);
		BigDecimal immobilizedOverstockValue = calculateImmobilizedOverstockValue(query);
		Double recommendationsFollowedRate = calculateRecommendationsFollowedRate(query);
		Double inventoryTurnover = calculateInventoryTurnover(query, products);

		return new InventoryKPIsResult(
				stockSnapshotKPIs.stockoutRate(), stockSnapshotKPIs.averageDaysOfCoverage(),
				immobilizedOverstockValue, recommendationsFollowedRate, inventoryTurnover);
	}

	private record StockSnapshotKPIs(Double stockoutRate, Double averageDaysOfCoverage) {
	}

	private StockSnapshotKPIs calculateStockSnapshotKPIs(CalculateInventoryKPIsQuery query, List<Product> products) {
		LocalDate windowStart = query.toDate().minusDays(SALES_WINDOW_DAYS - 1);

		int withData = 0;
		int outOfStock = 0;
		double coverageSum = 0;

		for (Product product : products) {
			List<Sale> sales = saleRepository.findByProductAndStore(
					product.productId(), query.storeId(), windowStart, query.toDate());
			Optional<ProductIndicatorsCalculator.ProductIndicators> indicators = productIndicatorsCalculator.calculate(
					product, query.storeId(), windowStart, query.toDate(), sales);
			if (indicators.isEmpty()) {
				continue;
			}

			withData++;
			if (indicators.get().currentStock() == 0) {
				outOfStock++;
			}
			coverageSum += indicators.get().currentDaysOfCoverage();
		}

		if (withData == 0) {
			return new StockSnapshotKPIs(null, null);
		}
		return new StockSnapshotKPIs((outOfStock * 100.0) / withData, coverageSum / withData);
	}

	private BigDecimal calculateImmobilizedOverstockValue(CalculateInventoryKPIsQuery query) {
		DetectOverstockQuery overstockQuery = DetectOverstockQuery.of(
				query.storeId(), null, query.toDate(), OverstockSortBy.IMMOBILIZED_VALUE);

		return detectOverstockUseCase.execute(overstockQuery).stream()
				.map(OverstockProductResult::immobilizedValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Double calculateRecommendationsFollowedRate(CalculateInventoryKPIsQuery query) {
		int applied = recommendationRepository.findByFilters(
				query.storeId(), null, RecommendationStatus.APPLIED, query.fromDate(), query.toDate()).size();
		int discarded = recommendationRepository.findByFilters(
				query.storeId(), null, RecommendationStatus.DISCARDED, query.fromDate(), query.toDate()).size();

		int resolved = applied + discarded;
		if (resolved == 0) {
			return null;
		}
		return (applied * 100.0) / resolved;
	}

	private Double calculateInventoryTurnover(CalculateInventoryKPIsQuery query, List<Product> products) {
		BigDecimal totalCogs = BigDecimal.ZERO;
		BigDecimal totalAverageInventory = BigDecimal.ZERO;

		for (Product product : products) {
			List<Sale> sales = saleRepository.findByProductAndStore(
					product.productId(), query.storeId(), query.fromDate(), query.toDate());
			int unitsSold = sales.stream().mapToInt(Sale::unitsSold).sum();
			totalCogs = totalCogs.add(product.costPrice().multiply(BigDecimal.valueOf(unitsSold)));

			List<Inventory> snapshots = inventoryRepository.findSnapshotsInRange(
					product.productId(), query.storeId(), query.fromDate(), query.toDate());
			if (snapshots.isEmpty()) {
				continue;
			}
			BigDecimal snapshotValueSum = snapshots.stream()
					.map(s -> product.costPrice().multiply(BigDecimal.valueOf(s.currentStock())))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal averageForProduct = snapshotValueSum.divide(BigDecimal.valueOf(snapshots.size()), 4, RoundingMode.HALF_UP);
			totalAverageInventory = totalAverageInventory.add(averageForProduct);
		}

		try {
			return InventoryTurnoverCalculator.calculate(totalCogs, totalAverageInventory);
		} catch (InvalidDomainDataException e) {
			return null; // inventario promedio 0 (o sin snapshots en el período): rotación indefinida
		}
	}
}
