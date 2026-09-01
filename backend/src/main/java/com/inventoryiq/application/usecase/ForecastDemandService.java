package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandQuery;
import com.inventoryiq.application.port.in.ForecastDemandResult;
import com.inventoryiq.application.port.in.ForecastDemandUseCase;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.exception.ProductNotFoundException;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import com.inventoryiq.domain.service.AdsCalculator;
import com.inventoryiq.domain.service.DailySalesRecordAssembler;
import com.inventoryiq.domain.service.SeasonalityCalculator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ForecastDemandUseCase (Sección 9.4).
 *
 * Algoritmo:
 * 1. Resuelve el producto por id (404 vía ProductNotFoundException si no
 *    existe en el catálogo — a diferencia de los otros cinco casos de
 *    uso, este sí filtra por un producto puntual, así que un id
 *    inexistente es un error de "recurso no encontrado", no una fila que
 *    simplemente se descarta de una lista).
 * 2. Arma el historial diario (ventas + inventario) de los últimos
 *    SALES_WINDOW_DAYS días y calcula el ADS base corregido (regla 4.9).
 *    Si no hay historial suficiente, el producto existe pero no se puede
 *    proyectar: se devuelve baseAds=null y periods=[] en vez de fallar
 *    toda la request.
 * 3. Parte el horizonte en períodos semanales (PERIOD_LENGTH_DAYS), y
 *    para cada uno calcula el índice estacional del mes de su fecha de
 *    inicio (regla 4.10, promedio móvil simple — Sección 9.4 permite esta
 *    simplificación en el MVP). Si ese mes no tiene historial propio
 *    dentro de la ventana, el índice cae a 1.0 (sin ajuste) en vez de
 *    fallar ese período puntual.
 * 4. ADS proyectado del período = ADS base × índice estacional; demanda
 *    total del período = ADS proyectado × días del período, redondeada.
 */
public class ForecastDemandService implements ForecastDemandUseCase {

	private static final int SALES_WINDOW_DAYS = 365;
	private static final int PERIOD_LENGTH_DAYS = 7;

	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final InventoryRepository inventoryRepository;

	public ForecastDemandService(
			ProductRepository productRepository, SaleRepository saleRepository, InventoryRepository inventoryRepository) {
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.inventoryRepository = inventoryRepository;
	}

	@Override
	public ForecastDemandResult execute(ForecastDemandQuery query) {
		Product product = productRepository.findById(query.productId())
				.orElseThrow(() -> new ProductNotFoundException(query.productId()));

		LocalDate windowStart = query.referenceDate().minusDays(SALES_WINDOW_DAYS - 1);
		List<Sale> sales = saleRepository.findByProductAndStore(
				product.productId(), query.storeId(), windowStart, query.referenceDate());
		List<Inventory> snapshots = inventoryRepository.findSnapshotsInRange(
				product.productId(), query.storeId(), windowStart.minusDays(1), query.referenceDate());
		List<DailySalesRecord> dailyRecords = DailySalesRecordAssembler.assemble(sales, snapshots);

		double baseAds;
		try {
			baseAds = AdsCalculator.calculateCorrectedAds(dailyRecords);
		} catch (InvalidDomainDataException e) {
			return new ForecastDemandResult(
					product.productId(), product.sku(), product.name(), query.storeId(), null, List.of());
		}

		List<DemandForecastPeriod> periods = buildPeriods(
				dailyRecords, baseAds, query.referenceDate(), query.horizonDays());

		return new ForecastDemandResult(
				product.productId(), product.sku(), product.name(), query.storeId(), baseAds, periods);
	}

	private static List<DemandForecastPeriod> buildPeriods(
			List<DailySalesRecord> dailyRecords, double baseAds, LocalDate referenceDate, int horizonDays) {

		List<DemandForecastPeriod> periods = new ArrayList<>();
		LocalDate periodStart = referenceDate.plusDays(1);
		int remainingDays = horizonDays;

		while (remainingDays > 0) {
			int periodLength = Math.min(PERIOD_LENGTH_DAYS, remainingDays);
			LocalDate periodEnd = periodStart.plusDays(periodLength - 1);

			double seasonalIndex = seasonalIndexFor(dailyRecords, periodStart);
			double projectedDailyAds = baseAds * seasonalIndex;
			int projectedTotalDemand = (int) Math.round(projectedDailyAds * periodLength);

			periods.add(new DemandForecastPeriod(periodStart, periodEnd, seasonalIndex, projectedDailyAds, projectedTotalDemand));

			periodStart = periodEnd.plusDays(1);
			remainingDays -= periodLength;
		}

		return periods;
	}

	private static double seasonalIndexFor(List<DailySalesRecord> dailyRecords, LocalDate periodStart) {
		try {
			return SeasonalityCalculator.calculateSeasonalIndex(dailyRecords, YearMonth.from(periodStart));
		} catch (InvalidDomainDataException e) {
			return 1.0; // sin historial propio de ese mes dentro de la ventana: sin ajuste estacional
		}
	}
}
