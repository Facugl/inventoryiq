package com.inventoryiq.config;

import com.inventoryiq.adapters.out.csv.CsvCategoryRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvInventoryRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvProductRepositoryAdapter;
import com.inventoryiq.adapters.out.csv.CsvSaleRepositoryAdapter;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Wiring de los puertos de salida hacia sus adaptadores CSV. Es el único
 * lugar de la aplicación que conoce tanto los puertos (application/port/out)
 * como sus implementaciones concretas (adapters/out/csv) — el rol propio
 * de una capa de composición en arquitectura hexagonal, no de dominio ni
 * de aplicación. Separado de UseCaseConfig porque estos beans son
 * reutilizables por cualquier caso de uso futuro, no solo por
 * GetCriticalProductsUseCase.
 */
@Configuration
public class CsvAdaptersConfig {

	@Bean
	public ProductRepository productRepository(CsvDataProperties csvDataProperties) {
		return new CsvProductRepositoryAdapter(Path.of(csvDataProperties.getBasePath()));
	}

	@Bean
	public CategoryRepository categoryRepository(CsvDataProperties csvDataProperties) {
		return new CsvCategoryRepositoryAdapter(Path.of(csvDataProperties.getBasePath()));
	}

	@Bean
	public SaleRepository saleRepository(CsvDataProperties csvDataProperties) {
		return new CsvSaleRepositoryAdapter(Path.of(csvDataProperties.getBasePath()));
	}

	@Bean
	public InventoryRepository inventoryRepository(CsvDataProperties csvDataProperties) {
		return new CsvInventoryRepositoryAdapter(Path.of(csvDataProperties.getBasePath()));
	}
}
