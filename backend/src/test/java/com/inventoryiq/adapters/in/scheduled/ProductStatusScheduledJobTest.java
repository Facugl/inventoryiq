package com.inventoryiq.adapters.in.scheduled;

import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateProductStatusUseCase;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Prueba el método anotado @Scheduled invocándolo directamente (no se
 * espera a que dispare el cron real, solo se verifica que delegue al
 * caso de uso con los parámetros correctos) — el propio mecanismo de
 * @Scheduled ya está probado por el framework.
 */
class ProductStatusScheduledJobTest {

	@Test
	void delegatesToTheUseCaseWithAllStoresAndTheClockDate() {
		FakeRecalculateProductStatusUseCase useCase = new FakeRecalculateProductStatusUseCase();
		Clock fixedClock = Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC);

		var job = new ProductStatusScheduledJob(useCase, fixedClock);
		job.run();

		assertEquals(1, useCase.receivedCommands.size());
		RecalculateProductStatusCommand command = useCase.receivedCommands.get(0);
		assertNull(command.storeId()); // todas las sucursales activas
		assertEquals(java.time.LocalDate.parse("2026-02-01"), command.referenceDate());
	}

	private static class FakeRecalculateProductStatusUseCase implements RecalculateProductStatusUseCase {
		final List<RecalculateProductStatusCommand> receivedCommands = new java.util.ArrayList<>();

		@Override
		public RecalculateProductStatusResult execute(RecalculateProductStatusCommand command) {
			receivedCommands.add(command);
			return new RecalculateProductStatusResult(0, List.of());
		}
	}
}
