package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.ProductClassificationResponse;
import com.inventoryiq.adapters.in.rest.mapper.ProductClassificationResponseMapper;
import com.inventoryiq.application.port.in.ClassifyProductsQuery;
import com.inventoryiq.application.port.in.ClassifyProductsUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de entrada REST para ClassifyProductsUseCase (Sección 9.5).
 * A diferencia de los otros dos endpoints de este slice, esta URL no está
 * definida en la Sección 8 de la documentación (que no contempla un
 * endpoint propio para la clasificación ABC/XYZ) — se diseñó de cero como
 * una vista de catálogo útil por sí misma, no solo como paso intermedio
 * hacia una futura pantalla de categorías/heatmap.
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductClassificationController {

	private final ClassifyProductsUseCase classifyProductsUseCase;

	public ProductClassificationController(ClassifyProductsUseCase classifyProductsUseCase) {
		this.classifyProductsUseCase = classifyProductsUseCase;
	}

	@GetMapping("/classification")
	public List<ProductClassificationResponse> getProductClassification(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {

		ClassifyProductsQuery query = ClassifyProductsQuery.of(storeId, categoryId, referenceDate);

		return classifyProductsUseCase.execute(query).stream()
				.map(ProductClassificationResponseMapper::toResponse)
				.toList();
	}
}
