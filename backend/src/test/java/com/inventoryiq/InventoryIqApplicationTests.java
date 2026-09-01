package com.inventoryiq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: el contexto de Spring debe levantar con lo que existe HOY
 * en el classpath.
 *
 * Desde que Postgres dejó de ser solo una dependencia sin usar (Sección
 * 8.5/8.6/8.7, RecommendationRepository), este contexto ya necesita un
 * DataSource de verdad para wirear PostgresAdaptersConfig — ya no alcanza
 * con excluir DataSourceAutoConfiguration como antes. En vez de exigir
 * Postgres real acá, se deja que Spring Boot arme automáticamente un
 * datasource embebido en H2 (dependencia de test): no hay ninguna
 * propiedad de datasource configurada en application.yml para el
 * perfil local, así que su autoconfiguración de "datasource embebido si
 * no hay otro configurado" aplica sola. Flyway sigue excluido (la
 * migración V1 usa sintaxis específica de Postgres) — este test no
 * necesita que la tabla recommendations exista, solo que el contexto
 * cargue.
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class InventoryIqApplicationTests {

	@Test
	void contextLoads() {
	}

}
