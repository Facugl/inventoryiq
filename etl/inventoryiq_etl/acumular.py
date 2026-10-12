"""Sumar filas nuevas a un CSV sin perder las que ya estaban.

`inventario.csv` y `ventas.csv` no se regeneran: se acumulan. Dos motivos
distintos, y los dos importan.

**El historial.** Cada corrida aporta los días de los exports que haya en la
carpeta. Los días viejos tienen que sobrevivir aunque su export ya no esté:
el stock no se puede volver a pedir hacia atrás.

**Lo que escribe la aplicación.** Las dos entidades se escriben también desde
el backend — el conteo manual de stock agrega a `inventario.csv`
(`CsvInventoryRepositoryAdapter.appendToFile`) y la carga de ventas por CSV
agrega a `ventas.csv`. Si el ETL regenerara el archivo, borraría en silencio
las correcciones que alguien hizo a mano desde la pantalla de Administración.

De ahí la regla: **el ETL agrega, nunca pisa.** Si una clave ya existe, la fila
existente gana, venga de donde venga. Un export es un archivo inmutable: si ya
se procesó, volver a procesarlo no tiene nada nuevo que aportar. Para rehacer
el archivo desde cero, se lo borra y se corre `migrar` de nuevo.
"""

import csv
from pathlib import Path

from .reporte import Reporte


def leer_existentes(ruta: Path) -> list[dict[str, str]]:
    if not ruta.is_file():
        return []
    with ruta.open(encoding="utf-8", newline="") as fh:
        return [dict(fila) for fila in csv.DictReader(fh)]


def acumular(
    existentes: list[dict[str, str]],
    nuevas: list[dict[str, str]],
    clave: tuple[str, ...],
    columna_id: str,
    reporte: Reporte,
) -> list[dict[str, str]]:
    """Las existentes, más las nuevas cuya clave todavía no estaba.

    El id se renumera sobre el conjunto ya ordenado: no lo referencia nadie
    (no es clave foránea de ningún archivo) y así dos corridas con los mismos
    datos producen el mismo archivo, sin un diff espurio.
    """
    def clave_de(fila: dict[str, str]) -> tuple[str, ...]:
        return tuple((fila.get(c) or "").strip() for c in clave)

    vistas = {clave_de(f) for f in existentes}
    agregadas = []
    for fila in nuevas:
        if clave_de(fila) in vistas:
            continue
        vistas.add(clave_de(fila))
        agregadas.append(fila)

    conservadas = len(existentes)
    repetidas = len(nuevas) - len(agregadas)

    if conservadas:
        reporte.ajustar(0, f"{conservadas} filas ya estaban y se conservan",
                        "el ETL agrega, nunca pisa lo que ya hay")
    if repetidas:
        reporte.ajustar(0, f"{repetidas} filas nuevas ya existían, se omiten",
                        "export ya procesado en una corrida anterior")

    todas = existentes + agregadas
    todas.sort(key=clave_de)
    for numero, fila in enumerate(todas, start=1):
        fila[columna_id] = str(numero)
    return todas
