package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.CategoryResponse;
import com.inventoryiq.adapters.in.rest.mapper.CategoryResponseMapper;
import com.inventoryiq.application.port.in.ListCategoriesUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para ListCategoriesUseCase. Sin endpoint
 * documentado en la Sección 8 — se agrega porque ningún otro endpoint
 * permite descubrir qué categorías existen; hoy categoryId es un filtro
 * opaco (long) en el resto de la API, sin forma de resolverlo a un nombre.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoriesController {

	private final ListCategoriesUseCase listCategoriesUseCase;

	public CategoriesController(ListCategoriesUseCase listCategoriesUseCase) {
		this.listCategoriesUseCase = listCategoriesUseCase;
	}

	@GetMapping
	public List<CategoryResponse> getCategories() {
		return listCategoriesUseCase.execute().stream()
				.map(CategoryResponseMapper::toResponse)
				.toList();
	}
}
