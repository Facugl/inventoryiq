"""Qué hizo el ETL con cada fila.

Existe porque las decisiones de `docs/InventoryIQ_ETL_Catalogo.md` incluyen
ajustar y descartar filas, y un ajuste silencioso sobre miles de filas es
inaceptable: el stock negativo, por ejemplo, afecta al 40% del export. Toda
transformación devuelve su reporte junto con las filas.
"""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Incidencia:
    """Una fila que no pasó tal cual. `fila` es 1-based sobre las filas de datos."""

    fila: int
    motivo: str
    detalle: str = ""

    def __str__(self) -> str:
        sufijo = f" ({self.detalle})" if self.detalle else ""
        return f"fila {self.fila}: {self.motivo}{sufijo}"


@dataclass
class Reporte:
    """Qué entró, qué salió, y qué pasó con la diferencia."""

    entidad: str
    leidas: int = 0
    emitidas: int = 0
    descartes: list[Incidencia] = field(default_factory=list)
    ajustes: list[Incidencia] = field(default_factory=list)

    def descartar(self, fila: int, motivo: str, detalle: str = "") -> None:
        self.descartes.append(Incidencia(fila, motivo, detalle))

    def ajustar(self, fila: int, motivo: str, detalle: str = "") -> None:
        self.ajustes.append(Incidencia(fila, motivo, detalle))

    def resumen(self, max_ejemplos: int = 5) -> str:
        lineas = [
            f"{self.entidad}: {self.leidas} leídas -> {self.emitidas} emitidas"
            f" | {len(self.descartes)} descartadas, {len(self.ajustes)} ajustadas"
        ]
        for titulo, items in (("descartes", self.descartes), ("ajustes", self.ajustes)):
            if not items:
                continue
            lineas.append(f"  {titulo}:")
            lineas += [f"    - {i}" for i in items[:max_ejemplos]]
            if len(items) > max_ejemplos:
                lineas.append(f"    ... y {len(items) - max_ejemplos} más")
        return "\n".join(lineas)
