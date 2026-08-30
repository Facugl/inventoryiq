package com.inventoryiq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: el contexto de Spring debe levantar con lo que existe HOY
 * en el classpath (HealthController, CsvDataProperties).
 *
 * spring-boot-starter-data-jpa y spring-boot-starter-flyway están en el
 * pom.xml desde el día 1 del proyecto (Fase 0 del roadmap: "la
 * persistencia operacional ya vive en Postgres desde el día 1"), pero
 * todavía no existe ninguna entidad JPA ni migración Flyway — Postgres es
 * un slice futuro (Fase 3). En docker-compose.yml el backend SÍ recibe un
 * datasource real por variables de entorno, así que su autoconfiguración
 * debe seguir intacta ahí. Acá, en un `mvn test` local sin datasource, se
 * excluye explícitamente para este test (no globalmente en
 * application.yml) para no enmascarar ni desactivar nada: el test sigue
 * verificando de verdad que el contexto wire correctamente todo lo que
 * hoy existe.
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class InventoryIqApplicationTests {

	@Test
	void contextLoads() {
	}

}
