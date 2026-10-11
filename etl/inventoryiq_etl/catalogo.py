"""Arma el catálogo completo y se niega a darlo por bueno si no cierra.

El backend carga los CSV de forma todo-o-nada: si un producto apunta a una
categoría que no existe, no falla esa fila, falla el arranque. Y las entidades
no se pueden migrar de a una, porque se referencian entre sí — migrar
`categorias.csv` solo, por ejemplo, deja a los productos simulados apuntando a
categorías que ya no están.

Por eso la migración no es "correr cada entidad y copiar": es generar el
conjunto entero en una carpeta aparte, verificar la integridad referencial
entre los archivos, y recién entonces decir si sirve. El día que se carguen
los datos reales, eso falla antes de romper nada en vez de después.
"""

import csv
import shutil
from dataclasses import dataclass, field
from datetime import date
from pathlib import Path

from . import categorias, inventario, productos, proveedores
from .reporte import Reporte

# Los seis que leen los adaptadores (ver adapters/out/csv/*.FILE_NAME).
# compras.csv y movimientos.csv están en data/csv pero no los lee nadie.
ARCHIVOS_REQUERIDOS = [
    "categorias.csv",
    "proveedores.csv",
    "productos.csv",
    "sucursales.csv",
    "ventas.csv",
    "inventario.csv",
]

# entidad -> (función, archivo de origen en el export, archivo de destino)
# El orden importa: inventario necesita el catálogo de productos ya generado
# para poder descartar el stock de productos que no existen, así que productos
# va antes. Productos, a su vez, lee más de un export, pero los auxiliares los
# resuelve él mismo a partir de la misma carpeta (ver `productos._auxiliares`).
ENTIDADES_MIGRADAS = {
    "categorias": (categorias.convertir, "categorias_xrp.csv", "categorias.csv"),
    "proveedores": (proveedores.convertir, "Mantenimiento_de_Proveedores_.csv", "proveedores.csv"),
    "productos": (productos.convertir, "Cambio_de_precios_xrp.csv", "productos.csv"),
    "inventario": (inventario.convertir, "Consulta_de_stock_xrp.xls", "inventario.csv"),
}

# Lo que todavía no sale del POS y se arrastra del dataset simulado.
#   - sucursales.csv: decisión del usuario, se siguen usando los nombres
#     simulados (Sección 5.2 del documento).
#   - ventas.csv: no hay export diario de ventas. Es el hueco más grande:
#     las ventas alimentan el ADS, y de ahí sale toda la recomendación.
ARCHIVOS_ARRASTRADOS = ["sucursales.csv", "ventas.csv"]


def _columna(ruta: Path, columna: str) -> list[str]:
    with ruta.open(encoding="utf-8", newline="") as fh:
        return [(f.get(columna) or "").strip() for f in csv.DictReader(fh)]


@dataclass(frozen=True)
class ClaveForanea:
    """`archivo.columna` tiene que existir en `referencia.columna_referencia`."""

    archivo: str
    columna: str
    referencia: str
    columna_referencia: str
    opcional: bool = False  # un valor vacío es válido (p. ej. categoría raíz)


CLAVES_FORANEAS = [
    ClaveForanea("categorias.csv", "categoria_padre_id", "categorias.csv", "categoria_id", opcional=True),
    ClaveForanea("productos.csv", "categoria_id", "categorias.csv", "categoria_id"),
    ClaveForanea("productos.csv", "proveedor_id", "proveedores.csv", "proveedor_id"),
    ClaveForanea("inventario.csv", "producto_id", "productos.csv", "producto_id"),
    ClaveForanea("inventario.csv", "sucursal_id", "sucursales.csv", "sucursal_id"),
    ClaveForanea("ventas.csv", "producto_id", "productos.csv", "producto_id"),
    ClaveForanea("ventas.csv", "sucursal_id", "sucursales.csv", "sucursal_id"),
]


@dataclass
class Violacion:
    clave: ClaveForanea
    faltantes: set[str]
    filas: int

    def __str__(self) -> str:
        ejemplos = ", ".join(sorted(self.faltantes)[:3])
        resto = f" y {len(self.faltantes) - 3} más" if len(self.faltantes) > 3 else ""
        return (
            f"{self.clave.archivo}.{self.clave.columna} -> "
            f"{self.clave.referencia}.{self.clave.columna_referencia}: "
            f"{self.filas} filas apuntan a {len(self.faltantes)} valores inexistentes "
            f"({ejemplos}{resto})"
        )


@dataclass
class ResultadoMigracion:
    destino: Path
    reportes: list[Reporte] = field(default_factory=list)
    arrastrados: list[str] = field(default_factory=list)
    faltantes: list[str] = field(default_factory=list)
    violaciones: list[Violacion] = field(default_factory=list)

    @property
    def sirve(self) -> bool:
        """¿El backend podría arrancar con este conjunto?"""
        return not self.faltantes and not self.violaciones

    def resumen(self) -> str:
        lineas = [r.resumen() for r in self.reportes]
        if self.arrastrados:
            lineas.append(
                "arrastrados del dataset simulado (sin fuente en el POS): "
                + ", ".join(self.arrastrados)
            )
        lineas.append("")
        if self.faltantes:
            lineas.append("FALTAN ARCHIVOS, el backend no arrancaría:")
            lineas += [f"  - {a}" for a in self.faltantes]
        if self.violaciones:
            lineas.append("INTEGRIDAD REFERENCIAL ROTA:")
            lineas += [f"  - {v}" for v in self.violaciones]
        lineas.append(
            "CATÁLOGO COMPLETO Y CONSISTENTE" if self.sirve
            else "CATÁLOGO NO UTILIZABLE todavía (ver arriba)"
        )
        return "\n".join(lineas)


def verificar_integridad(carpeta: Path) -> tuple[list[str], list[Violacion]]:
    """Qué archivos faltan y qué referencias quedan colgadas."""
    faltantes = [a for a in ARCHIVOS_REQUERIDOS if not (carpeta / a).is_file()]
    presentes = {a for a in ARCHIVOS_REQUERIDOS if a not in faltantes}

    violaciones = []
    for clave in CLAVES_FORANEAS:
        if clave.archivo not in presentes or clave.referencia not in presentes:
            continue  # ya está contado como archivo faltante

        validos = set(_columna(carpeta / clave.referencia, clave.columna_referencia))
        usados = _columna(carpeta / clave.archivo, clave.columna)

        colgados = [
            v for v in usados
            if not (clave.opcional and v == "") and v not in validos
        ]
        if colgados:
            violaciones.append(Violacion(clave, set(colgados), len(colgados)))

    return faltantes, violaciones


def migrar(
    desde: Path,
    destino: Path,
    simulados: Path,
    fecha_snapshot: date | None = None,
) -> ResultadoMigracion:
    """Genera el catálogo entero en `destino` y lo verifica.

    `simulados` es de donde salen los archivos que todavía no tienen fuente en
    el POS. No se escribe nada fuera de `destino`: el dataset simulado y el
    export crudo quedan intactos.
    """
    destino.mkdir(parents=True, exist_ok=True)
    resultado = ResultadoMigracion(destino=destino)

    for nombre, (convertir, origen, salida) in ENTIDADES_MIGRADAS.items():
        ruta_origen = desde / origen
        if not ruta_origen.is_file():
            reporte = Reporte(entidad=nombre)
            reporte.descartar(0, "no está el export de origen", str(ruta_origen))
            resultado.reportes.append(reporte)
            continue

        extra: dict = {}
        if nombre == "inventario":
            extra["fecha_snapshot"] = fecha_snapshot
            # El export de stock tiene códigos de barra que no están en el de
            # precios, así que hay stock de productos que no van a existir en
            # el catálogo. Pasarle los ids válidos hace que los descarte con
            # su motivo, en vez de dejarlos como referencia colgada para que
            # los encuentre la verificación de más abajo.
            catalogo_productos = destino / "productos.csv"
            if catalogo_productos.is_file():
                extra["productos_validos"] = set(_columna(catalogo_productos, "producto_id"))

        resultado.reportes.append(convertir(ruta_origen, destino / salida, **extra))

    for nombre in ARCHIVOS_ARRASTRADOS:
        origen = simulados / nombre
        if origen.is_file():
            shutil.copyfile(origen, destino / nombre)
            resultado.arrastrados.append(nombre)

    resultado.faltantes, resultado.violaciones = verificar_integridad(destino)
    return resultado
