package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.model.Sale;

/** Una fila de un CSV ingerido que ya pasó el parseo estructural y de negocio del dominio (Sección 9.9), pendiente de validación referencial. */
public record CandidateRow(int rowNumber, Sale sale) {
}
