package com.inventoryiq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata de la documentación OpenAPI generada por springdoc-openapi.
 * Sin configuración adicional, springdoc ya expone `/v3/api-docs` (JSON) y
 * `/swagger-ui/index.html` (UI interactiva) escaneando los `@RestController`
 * existentes — este bean solo reemplaza el título/descripción genéricos
 * por defecto.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI inventoryIqOpenApi() {
		return new OpenAPI().info(new Info()
				.title("InventoryIQ API")
				.description("API REST de InventoryIQ — ver docs/InventoryIQ_Documentacion.md (Sección 8) y "
						+ "docs/InventoryIQ_Arquitectura.md para el detalle auditado contra el código. "
						+ "Varios endpoints listados acá no estaban previstos en el diseño original; "
						+ "cada uno lo indica en su propia descripción.")
				.version("v0 (MVP)"));
	}
}
