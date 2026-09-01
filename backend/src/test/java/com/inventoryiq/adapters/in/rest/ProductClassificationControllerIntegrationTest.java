package com.inventoryiq.adapters.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> ProductClassificationController ->
 * ClassifyProductsUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> CSV reales de data/csv.
 *
 * El caso classifiesAKnownErraticLowValueProduct se verificó A MANO contra
 * ventas.csv/inventario.csv: producto 1012 (Mortadela 200g), sucursal 1,
 * ventana de 365 días (2025-08-02 a 2026-08-01):
 * - 365 días en la ventana, 21 censurados por quiebre de stock (regla 4.9).
 * - De esos 21 días censurados, solo se vendieron 4 unidades en total, que
 *   se excluyen del cálculo.
 * - ADS corregido = (72-4)/(365-21) = 68/344 = 0.197674...
 * - Desvío estándar poblacional de los 344 días válidos = 0.561762...
 * - CV = 0.561762/0.197674 = 2.84185... >= 1 -> clase Z.
 * (La clasificación ABC no se re-verificó a mano acá: es la misma función
 * AbcClassifier ya testeada exhaustivamente y reutilizada tal cual de
 * GetCriticalProductsService, sin cambios; lo genuinamente nuevo de este
 * caso de uso es el cálculo XYZ, que es lo que se verificó.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class ProductClassificationControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void classifiesAKnownErraticLowValueProductThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1012)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1012)].abcClass", contains("C")))
				.andExpect(jsonPath("$[?(@.productId == 1012)].xyzClass", contains("Z")));
	}

	@Test
	void allProductsHaveAValidAbcAndXyzClassification() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", not(empty())))
				.andExpect(jsonPath("$[*].abcClass", everyItem(is(in(List.of("A", "B", "C"))))))
				.andExpect(jsonPath("$[*].xyzClass", everyItem(is(in(List.of("X", "Y", "Z"))))));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}
