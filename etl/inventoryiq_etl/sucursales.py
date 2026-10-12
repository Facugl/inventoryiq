"""Correspondencia entre las unidades de negocio del POS y las sucursales.

El export de stock nombra las sucursales en `Unidad de Negocio`; el backend las
identifica por el id de `sucursales.csv`. Alguien tiene que traducir.

**Los nombres reales no se versionan.** Son los de un negocio concreto y este
repositorio es público, igual que los costos y los proveedores. Viven en
`data/real-export/sucursales.json`, que está ignorado por git, y el código
contiene sólo la regla con la que se arma, no los nombres.

La regla es la de la Sección 5.2 del documento: se ordenan las unidades por
volumen de filas y la más grande es la sucursal 1. No se puede confirmar contra
`sucursales.csv`, porque ahí los nombres son los simulados a propósito — así
que el archivo se genera para que se pueda revisar y corregir a mano, en vez de
decidirlo en silencio en cada corrida.
"""

import json
from collections import Counter
from pathlib import Path

ARCHIVO = "sucursales.json"

PLANTILLA = """{
  "<como lo nombra el POS en 'Unidad de Negocio'>": "<sucursal_id>"
}
"""


def derivar_por_volumen(unidades: list[str]) -> dict[str, str]:
    """Asigna los ids por volumen: la unidad con más filas es la sucursal 1."""
    conteo = Counter(u.strip() for u in unidades if u and u.strip())
    return {
        unidad: str(posicion)
        for posicion, (unidad, _) in enumerate(conteo.most_common(), start=1)
    }


def cargar(export: Path) -> dict[str, str] | None:
    """La correspondencia guardada, o None si todavía no se armó."""
    ruta = export / ARCHIVO
    if not ruta.is_file():
        return None
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    return {str(k): str(v) for k, v in datos.items()}


def guardar(export: Path, correspondencia: dict[str, str]) -> Path:
    ruta = export / ARCHIVO
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(
        json.dumps(correspondencia, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return ruta
