package com.inventoryiq.config;

import com.inventoryiq.application.port.in.ClassifyProductsUseCase;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.application.usecase.ClassifyProductsService;
import com.inventoryiq.application.usecase.DetectOverstockService;
import com.inventoryiq.application.usecase.GenerateAlertsService;
import com.inventoryiq.application.usecase.GetCriticalProductsService;
import com.inventoryiq.domain.service.CriticalityEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring de los casos de uso de aplicación. */
@Configuration
public class UseCaseConfig {

	/**
	 * Default MVP acordado en el cierre del Vertical Slice 1: pesos iguales
	 * (1/3 cada uno). No se externaliza a application.yml todavía — sigue
	 * siendo una configuración deliberadamente simple, fuera de alcance de
	 * este slice.
	 */
	@Bean
	public CriticalityEvaluator.CriticalityWeights criticalityWeights() {
		return new CriticalityEvaluator.CriticalityWeights(1.0 / 3, 1.0 / 3, 1.0 / 3);
	}

	@Bean
	public GetCriticalProductsUseCase getCriticalProductsUseCase(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository,
			CriticalityEvaluator.CriticalityWeights criticalityWeights) {
		return new GetCriticalProductsService(
				productRepository, categoryRepository, saleRepository, inventoryRepository, criticalityWeights);
	}

	@Bean
	public DetectOverstockUseCase detectOverstockUseCase(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository) {
		return new DetectOverstockService(productRepository, categoryRepository, saleRepository, inventoryRepository);
	}

	@Bean
	public ClassifyProductsUseCase classifyProductsUseCase(
			ProductRepository productRepository,
			SaleRepository saleRepository,
			InventoryRepository inventoryRepository) {
		return new ClassifyProductsService(productRepository, saleRepository, inventoryRepository);
	}

	@Bean
	public GenerateAlertsUseCase generateAlertsUseCase(
			GetCriticalProductsUseCase getCriticalProductsUseCase,
			DetectOverstockUseCase detectOverstockUseCase) {
		return new GenerateAlertsService(getCriticalProductsUseCase, detectOverstockUseCase);
	}
}
