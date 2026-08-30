package com.inventoryiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ruta base donde viven los CSV simulados (Sección 5 de la documentación).
 * Es el único punto de configuración externalizada de este slice: los
 * adaptadores CSV no son beans de Spring todavía (no hay nada que los
 * inyecte por DI hasta que exista un controller), así que esta propiedad
 * se resuelve acá y se pasa a mano al construirlos (ver tests de
 * integración). Cuando se agregue la capa REST, un @Configuration hará
 * `new CsvXxxRepositoryAdapter(csvDataProperties.getBasePath())`.
 */
@Component
@ConfigurationProperties(prefix = "inventoryiq.csv")
public class CsvDataProperties {

	private String basePath;

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
}
