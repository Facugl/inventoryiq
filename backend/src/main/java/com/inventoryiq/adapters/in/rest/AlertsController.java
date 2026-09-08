package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.AlertResponse;
import com.inventoryiq.adapters.in.rest.mapper.AlertResponseMapper;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de entrada REST para GenerateAlertsUseCase (Sección 8.14).
 * Depende únicamente del puerto de entrada (GenerateAlertsUseCase), nunca
 * de GenerateAlertsService ni de los otros dos casos de uso directamente
 * — el wiring concreto vive en config/UseCaseConfig.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Validated
@Tag(name = "Alertas", description = "Alertas de quiebre de stock y sobrestock (composición de Productos Críticos y Sobrestock)")
public class AlertsController {

	private final GenerateAlertsUseCase generateAlertsUseCase;

	public AlertsController(GenerateAlertsUseCase generateAlertsUseCase) {
		this.generateAlertsUseCase = generateAlertsUseCase;
	}

	@GetMapping
	@Operation(summary = "Listar alertas activas",
			description = "Deriva alertas de STOCKOUT (productos críticos) y OVERSTOCK para una sucursal, "
					+ "opcionalmente filtradas por tipo y severidad.")
	public List<AlertResponse> getAlerts(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
			@RequestParam(required = false) AlertType type,
			@RequestParam(required = false) AlertSeverity severity) {

		GenerateAlertsQuery query = new GenerateAlertsQuery(storeId, referenceDate, type, severity);

		return generateAlertsUseCase.execute(query).stream()
				.map(AlertResponseMapper.INSTANCE::toResponse)
				.toList();
	}
}
