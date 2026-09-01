package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsUseCase;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.application.usecase.shared.ProductIndicatorsCalculator;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.RecommendedQuantity;
import com.inventoryiq.domain.service.RecommendedQuantityCalculator;
import com.inventoryiq.domain.service.ReorderPointCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementación de GenerateReorderSuggestionsUseCase (Sección 9.2).
 *
 * Algoritmo, por sucursal (storeId, obligatorio en este slice):
 * 1. Resuelve el catálogo activo del scope (filtrado opcional por
 *    categoría y proveedor — este último ya disponible directo en
 *    Product.supplierId(), sin necesitar un puerto nuevo).
 * 2. Para cada producto: ProductIndicatorsCalculator calcula ADS
 *    corregido, stock de seguridad, punto de pedido, stock en tránsito y
 *    estado (misma lógica compartida con los otros dos casos de uso que
 *    la usan). Si no dispara el punto de pedido
 *    (ReorderPointCalculator.requiresReplenishment), se descarta.
 * 3. Cantidad sugerida: RecommendedQuantityCalculator.calculateByTargetCoverage
 *    (regla 4.5, método de cobertura objetivo — el único de los dos que
 *    describe la Sección 4.5 que se puede calcular con los datos
 *    disponibles: EOQ necesita costo de pedido y costo de mantenimiento,
 *    que no existen en ningún CSV). TARGET_COVERAGE_DAYS es una
 *    constante de MVP (15 días, dentro del rango "15 o 30" que sugiere
 *    la propia Sección 4.5), no viene de ningún CSV.
 * 4. Fecha límite de emisión: la fecha en la que el stock proyectado
 *    (decreciendo a razón de ADS por día) cruzaría el stock de
 *    seguridad.
 * 5. Proveedor sugerido: Product.supplierId() tal cual — el modelo de
 *    dominio no contempla varios proveedores por producto, así que no
 *    hay "el de menor lead time" que elegir.
 */
public class GenerateReorderSuggestionsService implements GenerateReorderSuggestionsUseCase {

	private static final int TARGET_COVERAGE_DAYS = 15;
	private static final int SALES_WINDOW_DAYS = 90;

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final ProductIndicatorsCalculator productIndicatorsCalculator;

	public GenerateReorderSuggestionsService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.productIndicatorsCalculator = new ProductIndicatorsCalculator(categoryRepository, inventoryRepository);
	}

	@Override
	public List<ReorderSuggestionResult> execute(GenerateReorderSuggestionsQuery query) {
		List<Product> products = productRepository.findAllActive().stream()
				.filter(p -> query.categoryId() == null || query.categoryId().equals(p.categoryId()))
				.filter(p -> query.supplierId() == null || query.supplierId().equals(p.supplierId()))
				.toList();

		LocalDate windowStart = query.referenceDate().minusDays(SALES_WINDOW_DAYS - 1);

		List<ReorderSuggestionResult> results = new ArrayList<>();
		for (Product product : products) {
			List<Sale> sales = saleRepository.findByProductAndStore(
					product.productId(), query.storeId(), windowStart, query.referenceDate());
			suggestFor(product, query, windowStart, sales).ifPresent(results::add);
		}
		return results;
	}

	private Optional<ReorderSuggestionResult> suggestFor(
			Product product, GenerateReorderSuggestionsQuery query, LocalDate windowStart, List<Sale> sales) {

		Optional<ProductIndicatorsCalculator.ProductIndicators> indicators = productIndicatorsCalculator.calculate(
				product, query.storeId(), windowStart, query.referenceDate(), sales);
		if (indicators.isEmpty()) {
			return Optional.empty(); // sin historial suficiente para evaluar este producto
		}
		ProductIndicatorsCalculator.ProductIndicators i = indicators.get();

		if (!ReorderPointCalculator.requiresReplenishment(i.currentStock(), i.reorderPoint())) {
			return Optional.empty();
		}

		RecommendedQuantity suggestedQuantity = RecommendedQuantityCalculator.calculateByTargetCoverage(
				i.ads(), TARGET_COVERAGE_DAYS, i.currentStock(), i.stockInTransit());

		LocalDate orderDeadlineDate = orderDeadlineDate(query.referenceDate(), i);

		String justification = justificationFor(i, suggestedQuantity, orderDeadlineDate);

		return Optional.of(new ReorderSuggestionResult(
				product.productId(), product.sku(), product.name(), query.storeId(), product.categoryId(),
				product.supplierId(), suggestedQuantity.units(), orderDeadlineDate, justification));
	}

	/** Fecha en la que el stock proyectado (decreciendo a razón de ADS por día) cruzaría el stock de seguridad. */
	private static LocalDate orderDeadlineDate(LocalDate referenceDate, ProductIndicatorsCalculator.ProductIndicators i) {
		if (i.ads() <= 0) {
			return referenceDate; // sin ventas registradas: no hay proyección posible, el límite es hoy
		}
		double daysUntilSafetyStock = (i.currentStock() - i.safetyStock().units()) / i.ads();
		long daysUntilDeadline = Math.max(0, (long) Math.floor(daysUntilSafetyStock));
		return referenceDate.plusDays(daysUntilDeadline);
	}

	private static String justificationFor(
			ProductIndicatorsCalculator.ProductIndicators i, RecommendedQuantity suggestedQuantity, LocalDate orderDeadlineDate) {
		return String.format(Locale.ROOT,
				"ADS corregido: %.2f u/día. Cobertura objetivo: %d días. Stock actual: %d, en tránsito: %d. "
						+ "Cantidad sugerida = (ADS × cobertura objetivo) − stock actual − stock en tránsito = %d unidades. "
						+ "Fecha límite de emisión: cuando el stock proyectado cruce el stock de seguridad (%.2f unidades), el %s.",
				i.ads(), TARGET_COVERAGE_DAYS, i.currentStock(), i.stockInTransit(),
				suggestedQuantity.units(), i.safetyStock().units(), orderDeadlineDate);
	}
}
