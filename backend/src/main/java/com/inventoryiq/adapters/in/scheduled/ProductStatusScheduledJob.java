package com.inventoryiq.adapters.in.scheduled;

import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Trigger programado de RecalculateProductStatusUseCase (Sección 9.10,
 * "job programado, ej. diario"). storeId=null: procesa todas las
 * sucursales activas, tal como describe la sección. El cron es
 * configurable (inventoryiq.scheduling.recalculate-product-status-cron
 * en application.yml); por defecto corre todos los días a las 02:00.
 */
@Component
public class ProductStatusScheduledJob {

	private static final Logger log = LoggerFactory.getLogger(ProductStatusScheduledJob.class);

	private final RecalculateProductStatusUseCase recalculateProductStatusUseCase;
	private final Clock clock;

	public ProductStatusScheduledJob(RecalculateProductStatusUseCase recalculateProductStatusUseCase, Clock clock) {
		this.recalculateProductStatusUseCase = recalculateProductStatusUseCase;
		this.clock = clock;
	}

	@Scheduled(cron = "${inventoryiq.scheduling.recalculate-product-status-cron:0 0 2 * * *}")
	public void run() {
		LocalDate referenceDate = LocalDate.now(clock);
		log.info("Starting scheduled RecalculateProductStatus run for referenceDate={}", referenceDate);

		var result = recalculateProductStatusUseCase.execute(new RecalculateProductStatusCommand(null, referenceDate));

		log.info("Finished scheduled RecalculateProductStatus run: storesProcessed={}", result.storesProcessed());
	}
}
