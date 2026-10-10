"""categorias_xrp.csv -> categorias.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.1 y 4.4.
"""

from pathlib import Path

from .fuentes import escribir_csv_canonico, leer_csv_pos
from .reporte import Reporte

COLUMNAS_DESTINO = [
    "categoria_id",
    "nombre",
    "categoria_padre_id",
    "umbral_max_cobertura_dias",
    "dias_cobertura_extra_default",
]

COLUMNAS_ORIGEN = {"idcate", "idparent", "denom"}

# Decisión 4.4: el POS no exporta estos parámetros. Se aplica a todas las
# categorías el valor más frecuente de los datos simulados, ajustable después
# desde la pantalla de Administración.
UMBRAL_SOBRESTOCK_DIAS = "20.0"
COBERTURA_EXTRA_DIAS = "4.0"

# El export usa idparent=0 para las raíces. No significa "hija de la categoría
# 0": la propia categoría 0 ("No Categorizado") se declara con idparent=0, o
# sea, sería su propia madre. El destino espera vacío para una raíz.
SIN_PADRE = "0"


def transformar(filas: list[dict[str, str]]) -> tuple[list[dict[str, str]], Reporte]:
    reporte = Reporte(entidad="categorias", leidas=len(filas))

    ids_vistos: set[str] = set()
    candidatas: list[tuple[int, dict[str, str]]] = []

    for numero, fila in enumerate(filas, start=1):
        faltantes = COLUMNAS_ORIGEN - fila.keys()
        if faltantes:
            reporte.descartar(numero, "faltan columnas", ", ".join(sorted(faltantes)))
            continue

        id_categoria = fila["idcate"].strip()
        if not id_categoria.isdigit():
            reporte.descartar(numero, "idcate no es un entero", repr(id_categoria))
            continue
        if id_categoria in ids_vistos:
            reporte.descartar(numero, "idcate duplicado", id_categoria)
            continue

        nombre = fila["denom"].strip()
        if not nombre:
            reporte.descartar(numero, "denom vacío", f"idcate={id_categoria}")
            continue

        ids_vistos.add(id_categoria)
        candidatas.append((numero, {**fila, "idcate": id_categoria, "denom": nombre}))

    # La integridad del padre se resuelve en una segunda pasada: una categoría
    # puede declarar un padre que aparece más abajo en el archivo.
    destino: list[dict[str, str]] = []
    for numero, fila in candidatas:
        padre = fila["idparent"].strip()

        if padre in ("", SIN_PADRE):
            padre_destino = ""
        elif padre not in ids_vistos:
            # No se descarta la categoría: se la promueve a raíz y se avisa.
            # Perderla arrastraría a sus hijas y a los productos que la usen.
            reporte.ajustar(numero, "idparent inexistente, se trata como raíz",
                            f"idcate={fila['idcate']}, idparent={padre}")
            padre_destino = ""
        elif padre == fila["idcate"]:
            reporte.ajustar(numero, "idparent apunta a sí misma, se trata como raíz",
                            f"idcate={fila['idcate']}")
            padre_destino = ""
        else:
            padre_destino = padre

        destino.append({
            "categoria_id": fila["idcate"],
            "nombre": fila["denom"],
            "categoria_padre_id": padre_destino,
            "umbral_max_cobertura_dias": UMBRAL_SOBRESTOCK_DIAS,
            "dias_cobertura_extra_default": COBERTURA_EXTRA_DIAS,
        })

    reporte.emitidas = len(destino)
    return destino, reporte


def convertir(origen: Path, destino: Path) -> Reporte:
    filas, entidades_desconocidas = leer_csv_pos(origen)
    destino_filas, reporte = transformar(filas)

    for entidad in sorted(entidades_desconocidas):
        reporte.ajustar(0, "entidad HTML desconocida, no se desescapó", entidad)

    escribir_csv_canonico(destino, COLUMNAS_DESTINO, destino_filas)
    return reporte
