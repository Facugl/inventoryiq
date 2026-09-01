package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.DailySalesRecord;

import java.time.YearMonth;
import java.util.List;

/**
 * Sección 4.10 — Estacionalidad.
 *
 * Índice Estacional (mes) = ADS del mes (histórico) / ADS promedio del
 * período completo recibido. Ambos promedios excluyen días con quiebre de
 * stock, por el mismo motivo que la regla 4.9 (calculateCorrectedAds): un
 * mes con muchos quiebres no es un mes de baja demanda, es un mes de
 * demanda no satisfecha.
 *
 * Con un solo año de datos disponibles (Sección 3, dataset simulado de
 * ~13 meses), "ADS del mes histórico" no puede promediar varias
 * ocurrencias del mismo mes calendario en distintos años — se reduce a la
 * única ocurrencia disponible dentro del período recibido. Es una
 * simplificación real de los datos, no del cálculo: si en el futuro hay
 * varios años de historial, basta con pasar un período más largo.
 */
public final class SeasonalityCalculator {
	private SeasonalityCalculator() {
	}

	public static double calculateSeasonalIndex(List<DailySalesRecord> records, YearMonth targetMonth) {
		if (records == null) {
			throw new InvalidDomainDataException("At least one sales record is needed to calculate a seasonal index");
		}

		List<DailySalesRecord> validRecords = records.stream()
				.filter(r -> !r.hadStockout())
				.toList();
		if (validRecords.isEmpty()) {
			throw new InvalidDomainDataException(
					"All days in the period had stockout; cannot estimate a seasonal index");
		}

		double periodAds = DemandStatistics.mean(validRecords.stream().map(DailySalesRecord::unitsSold).toList());
		if (periodAds == 0) {
			throw new InvalidDomainDataException("Period ADS is zero; cannot estimate a seasonal index");
		}

		List<Integer> monthUnits = validRecords.stream()
				.filter(r -> YearMonth.from(r.date()).equals(targetMonth))
				.map(DailySalesRecord::unitsSold)
				.toList();
		if (monthUnits.isEmpty()) {
			throw new InvalidDomainDataException("No sales history available for month " + targetMonth);
		}

		double monthAds = DemandStatistics.mean(monthUnits);
		return monthAds / periodAds;
	}
}
