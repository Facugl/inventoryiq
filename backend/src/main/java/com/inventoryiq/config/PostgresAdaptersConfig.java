package com.inventoryiq.config;

import com.inventoryiq.adapters.out.postgres.PostgresRecommendationRepositoryAdapter;
import com.inventoryiq.application.port.out.RecommendationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Wiring de los adaptadores de salida respaldados por Postgres. */
@Configuration
public class PostgresAdaptersConfig {

	@Bean
	public RecommendationRepository recommendationRepository(JdbcTemplate jdbcTemplate) {
		return new PostgresRecommendationRepositoryAdapter(jdbcTemplate);
	}
}
