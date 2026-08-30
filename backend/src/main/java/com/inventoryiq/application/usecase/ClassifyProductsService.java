package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ClassifyProductsQuery;
import com.inventoryiq.application.port.in.ClassifyProductsUseCase;
import com.inventoryiq.application.port.in.ProductClassificationResult;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.XyzClassification;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import com.inventoryiq.domain.service.AbcClassifier;
import com.inventoryiq.domain.service.DailySalesRecordAssembler;
import com.inventoryiq.domain.service.DemandStatistics;
import com.inventoryiq.domain.service.XyzClassifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de ClassifyProductsUseCase (Sección 9.5).
 *
 * Algoritmo, por sucursal (storeId, obligatorio en este slice):
 * 1. Resuelve el catálogo activo del scope (filtrado opcional por categoría).
 * 2. Clasifica ABC el catálogo del scope UNA sola vez (igual que en
 *    GetCriticalProductsUseCase: AbcClassifier necesita el valor de venta
 *    de todo el conjunto, no se puede calcular producto por producto).
 * 3. Para cada producto: arma la serie de ventas diarias, descarta los
 *    días con quiebre de stock (censura de demanda, Sección 4.9) y
 *    calcula el coeficiente de variación (Sección 4.7) sobre los días
 *    restantes para obtener la clase XYZ. Si no queda historial válido
 *    suficiente (todos los días con quiebre, o ADS resultante 0), el
 *    producto se descarta de forma aislada — mismo criterio de
 *    resiliencia que los otros dos casos de uso de este slice.
 */
public class ClassifyProductsService implements ClassifyProductsUseCase {

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final InventoryRepository inventoryRepository;

	public ClassifyProductsService(
			ProductRepository productRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.inventoryRepository = inventoryRepository;
	}

	@Override
	public List<ProductClassificationResult> execute(ClassifyProductsQuery query) {
		List<Product> products = productRepository.findAllActive().stream()
				.filter(p -> query.categoryId() == null || query.categoryId().equals(p.categoryId()))
				.toList();

		LocalDate windowStart = query.referenceDate().minusDays(query.analysisWindowDays() - 1);

		Map<Long, List<Sale>> salesByProduct = products.stream()
				.collect(Collectors.toMap(Product::productId, p -> saleRepository.findByProductAndStore(
						p.productId(), query.storeId(), windowStart, query.referenceDate())));

		Map<Long, AbcClassification> abcByProduct = classifyAbc(products, salesByProduct);

		List<ProductClassificationResult> results = new ArrayList<>();
		for (Product product : products) {
			classifyProduct(product, query, windowStart, salesByProduct.get(product.productId()), abcByProduct)
					.ifPresent(results::add);
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

	private Optional<ProductClassificationResult> classifyProduct(
			Product product, ClassifyProductsQuery query, LocalDate windowStart, List<Sale> sales,
			Map<Long, AbcClassification> abcByProduct) {

		List<Inventory> snapshots = inventoryRepository.findSnapshotsInRange(
				product.productId(), query.storeId(), windowStart.minusDays(1), query.referenceDate());
		List<DailySalesRecord> dailyRecords = DailySalesRecordAssembler.assemble(sales, snapshots);

		List<Integer> unitsWithoutStockout = dailyRecords.stream()
				.filter(r -> !r.hadStockout())
				.map(DailySalesRecord::unitsSold)
				.toList();

		XyzClassification xyzClass;
		try {
			double ads = DemandStatistics.mean(unitsWithoutStockout);
			double standardDeviation = DemandStatistics.standardDeviation(unitsWithoutStockout);
			xyzClass = XyzClassifier.classify(ads, standardDeviation);
		} catch (InvalidDomainDataException e) {
			return Optional.empty(); // sin historial suficiente para clasificar este producto
		}

		AbcClassification abcClass = abcByProduct.getOrDefault(product.productId(), AbcClassification.C);

		return Optional.of(new ProductClassificationResult(
				product.productId(), product.sku(), product.name(), query.storeId(), product.categoryId(),
				abcClass, xyzClass));
	}
}
