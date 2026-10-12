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
from datetime import date, timedelta
from pathlib import Path

from . import categorias, inventario, productos, proveedores, ventas
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
    # Estas dos no leen un archivo sino la carpeta entera: acumulan un día por
    # cada export que encuentren, y crecen con cada corrida (ver `acumular`).
    "inventario": (inventario.convertir, None, "inventario.csv"),
    "ventas": (ventas.convertir, None, "ventas.csv"),
}

# Lo que no sale del POS y se arrastra del dataset simulado.
#   - sucursales.csv: decisión del usuario, se siguen usando los nombres
#     simulados (Sección 5.2 del documento).
#   - ventas.csv NO se arrastra, aunque todavía no haya listados diarios de
#     los que sacarlo. Antes sí: era un stopgap mientras la entidad no tenía
#     fuente. Ahora sería peor que inútil, porque ventas.csv se ACUMULA: las
#     ventas simuladas quedarían en el archivo como si fueran reales y la
#     corrida siguiente las tomaría por datos ya cargados. Sin listados, el
#     archivo no existe y el comando lo reporta como faltante, que es la
#     verdad.
ARCHIVOS_ARRASTRADOS = ["sucursales.csv"]


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


# Días de historia a partir de los cuales el ADS deja de ser anecdótico. No es
# un umbral del dominio —que calcula con lo que haya— sino el punto en que vale
# la pena mirar los números.
DIAS_DE_HISTORIA_RAZONABLE = 30


def dias_utiles(carpeta: Path) -> tuple[set[str], set[str], set[str]]:
    """Días de venta, días de snapshot, y los días de venta que sirven.

    Un día de venta sólo entra al ADS corregido si existe un snapshot del día
    ANTERIOR: es lo que permite saber si el producto estuvo en quiebre
    (`DailySalesRecordAssembler`). Sin eso, el producto se descarta entero.

    Por eso no alcanza con contar días de cada cosa: hay que cruzarlos.
    """
    ventas_csv, inventario_csv = carpeta / "ventas.csv", carpeta / "inventario.csv"
    if not (ventas_csv.is_file() and inventario_csv.is_file()):
        return set(), set(), set()

    dias_venta = {d for d in _columna(ventas_csv, "fecha") if d}
    dias_stock = {d for d in _columna(inventario_csv, "fecha_snapshot") if d}

    siguiente = set()
    for dia in dias_stock:
        try:
            siguiente.add((date.fromisoformat(dia) + timedelta(days=1)).isoformat())
        except ValueError:
            continue

    return dias_venta, dias_stock, dias_venta & siguiente


@dataclass
class ResultadoMigracion:
    destino: Path
    reportes: list[Reporte] = field(default_factory=list)
    arrastrados: list[str] = field(default_factory=list)
    faltantes: list[str] = field(default_factory=list)
    violaciones: list[Violacion] = field(default_factory=list)
    advertencias: list[str] = field(default_factory=list)

    @property
    def sirve(self) -> bool:
        """¿El backend podría arrancar con este conjunto?"""
        return not self.faltantes and not self.violaciones

    @property
    def alcanza_para_calcular(self) -> bool:
        """Distinto de `sirve`: el catálogo puede cerrar y no servir para nada."""
        return not self.advertencias

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
        if self.advertencias:
            lineas.append("HISTORIAL INSUFICIENTE PARA CALCULAR:")
            lineas += [f"  - {a}" for a in self.advertencias]
        if not self.sirve:
            lineas.append("CATÁLOGO NO UTILIZABLE todavía (ver arriba)")
        elif self.advertencias:
            lineas.append("CATÁLOGO CONSISTENTE, PERO SIN HISTORIAL SUFICIENTE")
        else:
            lineas.append("CATÁLOGO COMPLETO Y CONSISTENTE")
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

    migrados: set[str] = set()

    for nombre, (convertir, origen, salida) in ENTIDADES_MIGRADAS.items():
        ruta_origen = desde / origen if origen else desde
        if not (ruta_origen.is_file() or ruta_origen.is_dir()):
            reporte = Reporte(entidad=nombre)
            reporte.descartar(0, "no está el export de origen", str(ruta_origen))
            resultado.reportes.append(reporte)
            continue

        extra: dict = {}
        if nombre in ("inventario", "ventas"):
            # Los exports de stock y de ventas tienen códigos de barra que no
            # están en el de precios, así que hay filas de productos que no van
            # a existir en el catálogo. Pasarle los ids válidos hace que las
            # descarte con su motivo, en vez de dejarlas como referencia
            # colgada para que las encuentre la verificación de más abajo.
            catalogo_productos = destino / "productos.csv"
            if catalogo_productos.is_file():
                extra["productos_validos"] = set(_columna(catalogo_productos, "producto_id"))
        if nombre == "inventario":
            extra["fecha_snapshot"] = fecha_snapshot

        reporte = convertir(ruta_origen, destino / salida, **extra)
        resultado.reportes.append(reporte)
        if reporte.emitidas:
            migrados.add(salida)

    for nombre in ARCHIVOS_ARRASTRADOS:
        if nombre in migrados:
            continue  # salió del POS, no hay nada que arrastrar
        origen = simulados / nombre
        if origen.is_file() and origen.resolve() != (destino / nombre).resolve():
            shutil.copyfile(origen, destino / nombre)
            resultado.arrastrados.append(nombre)

    resultado.faltantes, resultado.violaciones = verificar_integridad(destino)
    resultado.advertencias = _advertencias_de_historial(destino)
    return resultado


def _advertencias_de_historial(carpeta: Path) -> list[str]:
    """Lo que la integridad referencial no ve: que no haya con qué calcular.

    Un catálogo puede cerrar perfectamente y no servir para nada. Sin esto, el
    comando diría "COMPLETO Y CONSISTENTE" y las pantallas saldrían vacías sin
    que nada explique por qué.
    """
    dias_venta, dias_stock, utiles = dias_utiles(carpeta)
    if not dias_venta and not dias_stock:
        return []

    if not utiles:
        return [
            f"ningún día de venta tiene un snapshot del día anterior "
            f"({len(dias_venta)} día(s) de venta, {len(dias_stock)} de stock). "
            "El ADS corregido descarta TODOS los productos: el backend "
            "arrancaría y las pantallas saldrían vacías"
        ]
    if len(utiles) < DIAS_DE_HISTORIA_RAZONABLE:
        return [
            f"sólo {len(utiles)} día(s) de venta tienen snapshot del día "
            "anterior. El ADS se calcula, pero sobre muy poca historia"
        ]
    return []
