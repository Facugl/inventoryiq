package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.in.OverstockSortBy;
import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateProductStatusUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsUseCase;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.model.Store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de RecalculateProductStatusUseCase (Sección 9.10, job
 * programado). Es el orquestador final del proyecto: por cada sucursal en
 * alcance, corre en secuencia GetCriticalProducts, DetectOverstock,
 * RecalculateRecommendations (que ya persiste, Sección 8.6) y
 * GenerateAlerts — el mismo orden que describe el algoritmo de la
 * Sección 9.10.
 *
 * No persiste un "estado de producto" nuevo en Postgres: el único efecto
 * persistido real que pide la Sección 9.10 ("actualización de estados
 * persistidos, nuevas recomendaciones") ya lo cubre
 * RecalculateRecommendationsUseCase. GetCriticalProducts/DetectOverstock/
 * GenerateAlerts se computan al vuelo como en el resto del proyecto —
 * corridos acá para completar el resumen del job, no porque tengan un
 * efecto secundario propio que persistir.
 */
public class RecalculateProductStatusService implements RecalculateProductStatusUseCase {

	private final StoreRepository storeRepository;
	private final GetCriticalProductsUseCase getCriticalProductsUseCase;
	private final DetectOverstockUseCase detectOverstockUseCase;
	private final RecalculateRecommendationsUseCase recalculateRecommendationsUseCase;
	private final GenerateAlertsUseCase generateAlertsUseCase;

	public RecalculateProductStatusService(
			StoreRepository storeRepository,
			GetCriticalProductsUseCase getCriticalProductsUseCase,
			DetectOverstockUseCase detectOverstockUseCase,
			RecalculateRecommendationsUseCase recalculateRecommendationsUseCase,
			GenerateAlertsUseCase generateAlertsUseCase) {
		this.storeRepository = storeRepository;
		this.getCriticalProductsUseCase = getCriticalProductsUseCase;
		this.detectOverstockUseCase = detectOverstockUseCase;
		this.recalculateRecommendationsUseCase = recalculateRecommendationsUseCase;
		this.generateAlertsUseCase = generateAlertsUseCase;
	}

	@Override
	public RecalculateProductStatusResult execute(RecalculateProductStatusCommand command) {
		List<Long> storeIds = command.storeId() != null
				? List.of(command.storeId())
				: storeRepository.findAllActive().stream().map(Store::storeId).toList();

		List<StoreRecalculationSummary> summaries = new ArrayList<>();
		for (Long storeId : storeIds) {
			summaries.add(recalculateForStore(storeId, command.referenceDate()));
		}

		return new RecalculateProductStatusResult(summaries.size(), summaries);
	}

	private StoreRecalculationSummary recalculateForStore(Long storeId, LocalDate referenceDate) {
		int criticalCount = getCriticalProductsUseCase.execute(
				GetCriticalProductsQuery.of(storeId, null, null, referenceDate)).size();

		int overstockCount = detectOverstockUseCase.execute(
				DetectOverstockQuery.of(storeId, null, referenceDate, OverstockSortBy.IMMOBILIZED_VALUE)).size();

		RecalculateRecommendationsResult recommendations = recalculateRecommendationsUseCase.execute(
				new RecalculateRecommendationsCommand(storeId, referenceDate));

		int alertsCount = generateAlertsUseCase.execute(
				new GenerateAlertsQuery(storeId, referenceDate, null, null)).size();

		return new StoreRecalculationSummary(storeId, criticalCount, overstockCount, alertsCount, recommendations);
	}
}
