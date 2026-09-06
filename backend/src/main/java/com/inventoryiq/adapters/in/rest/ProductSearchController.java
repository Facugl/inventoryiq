package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.ProductSummaryResponse;
import com.inventoryiq.adapters.in.rest.mapper.ProductSummaryResponseMapper;
import com.inventoryiq.application.port.in.SearchProductsQuery;
import com.inventoryiq.application.port.in.SearchProductsUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para SearchProductsUseCase. Sin endpoint
 * documentado en la Sección 8 — ningún otro controller permite buscar en
 * el catálogo por código o nombre; hasta ahora todo cliente tenía que
 * conocer de antemano el productId numérico.
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductSearchController {

	private final SearchProductsUseCase searchProductsUseCase;

	public ProductSearchController(SearchProductsUseCase searchProductsUseCase) {
		this.searchProductsUseCase = searchProductsUseCase;
	}

	@GetMapping
	public List<ProductSummaryResponse> search(@RequestParam("q") @NotBlank String q) {
		return searchProductsUseCase.execute(new SearchProductsQuery(q)).stream()
				.map(ProductSummaryResponseMapper::toResponse)
				.toList();
	}
}
