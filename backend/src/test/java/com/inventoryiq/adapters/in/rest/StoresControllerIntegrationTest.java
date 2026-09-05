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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De punta a punta: HTTP -> StoresController -> ListStoresUseCase (wiring
 * real de config/) -> CsvStoreRepositoryAdapter real -> sucursales.csv real
 * (3 sucursales, todas activas — ver docs/README_datos_simulados.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class StoresControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsTheThreeActiveStoresFromTheRealCsv() throws Exception {
		mockMvc.perform(get("/api/v1/stores"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[?(@.storeId == 1)].name").value(org.hamcrest.Matchers.contains("Sucursal Centro")));
	}
}
