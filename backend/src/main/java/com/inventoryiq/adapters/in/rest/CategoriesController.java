package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.CategoryResponse;
import com.inventoryiq.adapters.in.rest.dto.UpdateCategoryParametersRequest;
import com.inventoryiq.adapters.in.rest.mapper.CategoryResponseMapper;
import com.inventoryiq.application.port.in.ListCategoriesUseCase;
import com.inventoryiq.application.port.in.UpdateCategoryParametersCommand;
import com.inventoryiq.application.port.in.UpdateCategoryParametersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para ListCategoriesUseCase (sin endpoint
 * documentado en la Sección 8 — se agrega porque ningún otro endpoint
 * permite descubrir qué categorías existen) y UpdateCategoryParametersUseCase
 * (Sección 8.10, antes PLANIFICADO).
 */
@RestController
@RequestMapping("/api/v1/categories")
@Validated
@Tag(name = "Categorías", description = "Catálogo de categorías de producto y sus parámetros de negocio (umbral de sobrestock, stock de seguridad)")
public class CategoriesController {

	private final ListCategoriesUseCase listCategoriesUseCase;
	private final UpdateCategoryParametersUseCase updateCategoryParametersUseCase;

	public CategoriesController(
			ListCategoriesUseCase listCategoriesUseCase, UpdateCategoryParametersUseCase updateCategoryParametersUseCase) {
		this.listCategoriesUseCase = listCategoriesUseCase;
		this.updateCategoryParametersUseCase = updateCategoryParametersUseCase;
	}

	@GetMapping
	@Operation(summary = "Listar categorías",
			description = "Catálogo completo de categorías, con su categoría padre si es una subcategoría, y sus parámetros de negocio vigentes.")
	public List<CategoryResponse> getCategories() {
		return listCategoriesUseCase.execute().stream()
				.map(CategoryResponseMapper::toResponse)
				.toList();
	}

	@PatchMapping("/{categoryId}/parameters")
	@Operation(summary = "Corregir los parámetros de negocio de una categoría",
			description = "maxCoverageDaysThreshold: días de cobertura a partir de los cuales un producto de esta "
					+ "categoría se marca Sobrestock. defaultExtraCoverageDays: colchón de stock de seguridad. Ambos "
					+ "se leen en caliente en cada request — surte efecto de inmediato, sin reiniciar el backend.")
	public CategoryResponse updateParameters(
			@PathVariable Long categoryId, @RequestBody @Valid UpdateCategoryParametersRequest request) {
		UpdateCategoryParametersCommand command = new UpdateCategoryParametersCommand(
				categoryId, request.maxCoverageDaysThreshold(), request.defaultExtraCoverageDays());
		return CategoryResponseMapper.toResponse(updateCategoryParametersUseCase.execute(command));
	}
}
