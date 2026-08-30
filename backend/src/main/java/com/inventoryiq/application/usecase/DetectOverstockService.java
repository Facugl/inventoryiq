package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.in.OverstockSortBy;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.application.usecase.shared.ProductIndicatorsCalculator;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de DetectOverstockUseCase (Sección 9.3).
 *
 * Algoritmo, por sucursal (storeId, obligatorio en este slice):
 * 1. Resuelve el catálogo activo del scope (filtrado opcional por categoría).
 * 2. Para cada producto: ProductIndicatorsCalculator calcula ADS corregido,
 *    stock de seguridad, punto de pedido, cobertura actual y estado (misma
 *    lógica compartida con GetCriticalProductsUseCase). Si el estado no es
 *    OVERSTOCK, el producto se descarta.
 * 3. Para los que sí están en sobrestock, calcula el valor de inventario
 *    inmovilizado (stock actual × costo unitario del producto).
 * 4. Ordena según sortBy (valor inmovilizado o días de cobertura, siempre
 *    descendente: el mayor problema primero).
 *
 * A diferencia de GetCriticalProductsUseCase, no necesita clasificación ABC
 * ni score de criticidad — la Sección 8.4 no los pide para este endpoint.
 */
public class DetectOverstockService implements DetectOverstockUseCase {

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final ProductIndicatorsCalculator productIndicatorsCalculator;

	public DetectOverstockService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.productIndicatorsCalculator = new ProductIndicatorsCalculator(categoryRepository, inventoryRepository);
	}

	@Override
	public List<OverstockProductResult> execute(DetectOverstockQuery query) {
		List<Product> products = productRepository.findAllActive().stream()
				.filter(p -> query.categoryId() == null || query.categoryId().equals(p.categoryId()))
				.toList();

		LocalDate windowStart = query.referenceDate().minusDays(query.salesWindowDays() - 1);

		List<OverstockProductResult> results = new ArrayList<>();
		for (Product product : products) {
			evaluateProduct(product, query, windowStart).ifPresent(results::add);
		}

		results.sort(comparatorFor(query.sortBy()));
		return results;
	}

	private Optional<OverstockProductResult> evaluateProduct(Product product, DetectOverstockQuery query, LocalDate windowStart) {
		List<Sale> sales = saleRepository.findByProductAndStore(
				product.productId(), query.storeId(), windowStart, query.referenceDate());

		Optional<ProductIndicatorsCalculator.ProductIndicators> indicators = productIndicatorsCalculator.calculate(
				product, query.storeId(), windowStart, query.referenceDate(), sales);
		if (indicators.isEmpty()) {
			return Optional.empty();
		}
		ProductIndicatorsCalculator.ProductIndicators productIndicators = indicators.get();

		if (productIndicators.status() != ProductStatus.OVERSTOCK) {
			return Optional.empty();
		}

		BigDecimal immobilizedValue = BigDecimal.valueOf(productIndicators.currentStock()).multiply(product.costPrice());

		return Optional.of(new OverstockProductResult(
				product.productId(), product.sku(), product.name(), query.storeId(), product.categoryId(),
				productIndicators.currentStock(), productIndicators.currentDaysOfCoverage(), immobilizedValue));
	}

	private static Comparator<OverstockProductResult> comparatorFor(OverstockSortBy sortBy) {
		return switch (sortBy) {
			case IMMOBILIZED_VALUE -> Comparator.comparing(OverstockProductResult::immobilizedValue).reversed();
			case DAYS_OF_COVERAGE -> Comparator.comparingDouble(OverstockProductResult::currentDaysOfCoverage).reversed();
		};
	}
}
