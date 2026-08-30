package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.application.usecase.shared.ProductIndicatorsCalculator;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.service.AbcClassifier;
import com.inventoryiq.domain.service.CriticalityEvaluator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de GetCriticalProductsUseCase (Sección 9.1).
 *
 * Algoritmo, por sucursal (storeId, obligatorio en este slice):
 * 1. Resuelve el catálogo activo del scope (filtrado opcional por categoría).
 * 2. Clasifica ABC el catálogo del scope UNA sola vez (AbcClassifier necesita
 *    el valor de venta de todo el conjunto para calcular el acumulado Pareto,
 *    no se puede calcular producto por producto).
 * 3. Para cada producto: ProductIndicatorsCalculator calcula ADS corregido,
 *    stock de seguridad, punto de pedido, cobertura actual y estado (lógica
 *    compartida con DetectOverstockUseCase). Si el estado no es CRITICAL ni
 *    REQUIRES_REPLENISHMENT, el producto se descarta (fuera del alcance de
 *    este caso de uso). ProductIndicatorsCalculator ya descarta por su cuenta
 *    los productos con datos incompletos (sin historial de ventas, sin
 *    snapshot de inventario o con categoría inexistente).
 * 4. Ordena por score de criticidad descendente y trunca por límite si corresponde.
 */
public class GetCriticalProductsService implements GetCriticalProductsUseCase {

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final ProductIndicatorsCalculator productIndicatorsCalculator;
	private final CriticalityEvaluator.CriticalityWeights criticalityWeights;

	public GetCriticalProductsService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository,
			CriticalityEvaluator.CriticalityWeights criticalityWeights) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.productIndicatorsCalculator = new ProductIndicatorsCalculator(categoryRepository, inventoryRepository);
		this.criticalityWeights = criticalityWeights;
	}

	@Override
	public List<CriticalProductResult> execute(GetCriticalProductsQuery query) {
		List<Product> products = productRepository.findAllActive().stream()
				.filter(p -> query.categoryId() == null || query.categoryId().equals(p.categoryId()))
				.toList();

		LocalDate windowStart = query.referenceDate().minusDays(query.salesWindowDays() - 1);

		// Se busca una sola vez por producto: tanto la clasificación ABC como el
		// ADS corregido necesitan las ventas de la misma ventana [windowStart,
		// referenceDate], así que se reutiliza el mismo resultado para ambos en
		// vez de consultar el puerto dos veces por producto.
		Map<Long, List<Sale>> salesByProduct = products.stream()
				.collect(Collectors.toMap(Product::productId, p -> saleRepository.findByProductAndStore(
						p.productId(), query.storeId(), windowStart, query.referenceDate())));

		Map<Long, AbcClassification> abcByProduct = classifyAbc(products, salesByProduct);

		List<CriticalProductResult> results = new ArrayList<>();
		for (Product product : products) {
			evaluateProduct(product, query, windowStart, salesByProduct.get(product.productId()), abcByProduct)
					.ifPresent(results::add);
		}

		results.sort(Comparator.comparingDouble((CriticalProductResult r) -> r.criticalityLevel().score()).reversed());

		if (query.limit() != null && results.size() > query.limit()) {
			return results.subList(0, query.limit());
		}
		return results;
	}

	private Map<Long, AbcClassification> classifyAbc(List<Product> products, Map<Long, List<Sale>> salesByProduct) {
		List<AbcClassifier.SalesValueByProduct> salesValues = products.stream()
				.map(p -> new AbcClassifier.SalesValueByProduct(p.productId(), totalSalesValue(salesByProduct.get(p.productId()))))
				.toList();
		return AbcClassifier.classify(salesValues);
	}

	private BigDecimal totalSalesValue(List<Sale> sales) {
		return sales.stream()
				.map(Sale::totalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Optional<CriticalProductResult> evaluateProduct(
			Product product, GetCriticalProductsQuery query, LocalDate windowStart, List<Sale> sales,
			Map<Long, AbcClassification> abcByProduct) {

		Optional<ProductIndicatorsCalculator.ProductIndicators> indicators = productIndicatorsCalculator.calculate(
				product, query.storeId(), windowStart, query.referenceDate(), sales);
		if (indicators.isEmpty()) {
			return Optional.empty();
		}
		ProductIndicatorsCalculator.ProductIndicators productIndicators = indicators.get();

		if (productIndicators.status() != ProductStatus.CRITICAL
				&& productIndicators.status() != ProductStatus.REQUIRES_REPLENISHMENT) {
			return Optional.empty();
		}

		AbcClassification abcClass = abcByProduct.getOrDefault(product.productId(), AbcClassification.C);
		CriticalityLevel criticality = CriticalityEvaluator.calculate(
				abcClass, productIndicators.currentDaysOfCoverage(), product.leadTime(),
				productIndicators.currentStock() == 0, criticalityWeights);

		return Optional.of(new CriticalProductResult(
				product.productId(), product.sku(), product.name(), query.storeId(), product.categoryId(),
				productIndicators.currentStock(), productIndicators.reorderPoint(),
				productIndicators.currentDaysOfCoverage(), productIndicators.status(), criticality));
	}
}
