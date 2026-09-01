package com.inventoryiq.adapters.out.postgres;

import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica PostgresRecommendationRepositoryAdapter contra un Postgres real
 * (Testcontainers), aplicando la migración de Flyway real
 * (V1__create_recommendations_table.sql) — no un fake en memoria. Necesita
 * Docker Desktop corriendo; si no está disponible, esta clase (y solo
 * esta) falla al arrancar el contenedor, sin afectar al resto de la suite.
 */
@Testcontainers
class PostgresRecommendationRepositoryAdapterTest {

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	private static JdbcTemplate jdbcTemplate;
	private PostgresRecommendationRepositoryAdapter adapter;

	@BeforeAll
	static void migrate() {
		DataSource dataSource = new SimpleDriverDataSource(
				new org.postgresql.Driver(), POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
		Flyway.configure().dataSource(dataSource).load().migrate();
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM recommendations");
		adapter = new PostgresRecommendationRepositoryAdapter(jdbcTemplate);
	}

	@Test
	void insertingARecommendationWithoutIdAssignsOneAndPersistsEveryField() {
		Recommendation saved = adapter.save(pending(null, 1001L, 5L, 100));

		assertNotNull(saved.recommendationId());
		assertEquals(1001L, saved.productId());
		assertEquals(RecommendationStatus.PENDING, saved.status());

		Recommendation reloaded = adapter.findById(saved.recommendationId()).orElseThrow();
		assertEquals(saved, reloaded);
	}

	@Test
	void savingARecommendationWithAnExistingIdUpdatesItInPlace() {
		Recommendation saved = adapter.save(pending(null, 1001L, 5L, 100));

		Recommendation updated = saved.withFeedback(RecommendationStatus.APPLIED, "comprado", LocalDate.parse("2026-08-03"));
		adapter.save(updated);

		Recommendation reloaded = adapter.findById(saved.recommendationId()).orElseThrow();
		assertEquals(RecommendationStatus.APPLIED, reloaded.status());
		assertEquals("comprado", reloaded.feedbackComment());
		assertEquals(LocalDate.parse("2026-08-03"), reloaded.feedbackDate());
	}

	@Test
	void findByIdReturnsEmptyWhenNothingMatches() {
		assertTrue(adapter.findById(999L).isEmpty());
	}

	@Test
	void findByFiltersAppliesStoreSupplierAndStatusFilters() {
		adapter.save(pending(null, 1001L, 5L, 100)); // store 1, supplier 5, PENDING
		Recommendation other = adapter.save(pending(null, 1002L, 9L, 50)); // store 1, supplier 9, PENDING
		adapter.save(other.withFeedback(RecommendationStatus.DISCARDED, "no aplica", LocalDate.parse("2026-08-03")));

		List<Recommendation> allForStore = adapter.findByFilters(1L, null, null);
		assertEquals(2, allForStore.size()); // el discard actualizó 1002 in place, no insertó una fila nueva

		List<Recommendation> bySupplier = adapter.findByFilters(1L, 5L, null);
		assertEquals(1, bySupplier.size());
		assertEquals(1001L, bySupplier.get(0).productId());

		List<Recommendation> byStatus = adapter.findByFilters(1L, null, RecommendationStatus.DISCARDED);
		assertEquals(1, byStatus.size());
		assertEquals(1002L, byStatus.get(0).productId());
	}

	private static Recommendation pending(Long id, Long productId, Long supplierId, int suggestedQuantity) {
		return new Recommendation(id, productId, 1L, supplierId, suggestedQuantity, LocalDate.parse("2026-08-05"),
				"justificación", RecommendationStatus.PENDING, LocalDate.parse("2026-08-01"), null, null);
	}
}
