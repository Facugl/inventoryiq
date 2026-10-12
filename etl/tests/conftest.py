import shutil
import sys
from pathlib import Path

import pytest

# El paquete se importa desde el repo sin instalarlo: el ETL es una
# herramienta del repositorio, no una librería que se publique.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from helpers import escribir

FIXTURES = Path(__file__).resolve().parent / "fixtures"


@pytest.fixture
def fixture_categorias() -> Path:
    return FIXTURES / "categorias_xrp.csv"


@pytest.fixture
def fixture_proveedores() -> Path:
    return FIXTURES / "proveedores_xrp.csv"


@pytest.fixture
def fixture_proveedores_id_cero() -> Path:
    """El POS emite un proveedor real con el id reservado para "sin asignar"."""
    return FIXTURES / "proveedores_xrp_id_cero.csv"


@pytest.fixture
def fixture_proveedores_con_sin_proveedor() -> Path:
    """El POS ya registra "Sin Proveedor" como proveedor propio."""
    return FIXTURES / "proveedores_xrp_sin_proveedor_registrado.csv"


@pytest.fixture
def fixture_stock() -> Path:
    """HTML con extensión .xls, como el export de stock del POS."""
    return FIXTURES / "stock_xrp.xls"


@pytest.fixture
def fixture_listado() -> Path:
    """SpreadsheetML con extensión .xls: el listado de ventas, que es el único
    export que trae la categoría del producto."""
    return FIXTURES / "listado_ventas.xls"


@pytest.fixture
def fixture_precios() -> Path:
    return FIXTURES / "cambio_de_precios_xrp.csv"


@pytest.fixture
def fixture_listado_dia() -> Path:
    """Un listado de ventas exportado con `desde = hasta`: el único que sirve
    como venta diaria."""
    return FIXTURES / "listado_ventas_dia.xls"


@pytest.fixture
def carpeta_diarios(tmp_path) -> Path:
    """La carpeta donde se van dejando los exports del día.

    En tmp_path y no en fixtures/ porque el ETL escribe ahí al lado (la
    correspondencia de sucursales), y un test no debe tocar el repositorio.
    """
    carpeta = tmp_path / "diarios"
    carpeta.mkdir()
    shutil.copyfile(FIXTURES / "listado_ventas_dia.xls", carpeta / "ventas_dia.xls")
    return carpeta


@pytest.fixture
def export_pos(tmp_path) -> Path:
    """Una carpeta de exports con los nombres de archivo que usa el POS.

    Productos es el único slice que lee varios exports y los busca por nombre
    en su propia carpeta, así que el test necesita esa carpeta armada y no
    alcanza con pasarle un fixture suelto.
    """
    export = tmp_path / "real-export"
    export.mkdir()
    for origen, nombre in [
        (FIXTURES / "cambio_de_precios_xrp.csv", "Cambio_de_precios_xrp.csv"),
        (FIXTURES / "categorias_xrp.csv", "categorias_xrp.csv"),
        (FIXTURES / "proveedores_xrp.csv", "Mantenimiento_de_Proveedores_.csv"),
        (FIXTURES / "listado_ventas.xls", "Listado_ventas.xls"),
    ]:
        shutil.copyfile(origen, export / nombre)
    return export


@pytest.fixture
def catalogo_completo(tmp_path):
    """Un catálogo chico pero íntegro: los seis archivos y las FK cerradas."""
    c = tmp_path / "catalogo"
    escribir(c, "categorias.csv",
             ["categoria_id", "nombre", "categoria_padre_id", "umbral_max_cobertura_dias", "dias_cobertura_extra_default"],
             [{"categoria_id": "1", "nombre": "ALMACEN", "categoria_padre_id": "", "umbral_max_cobertura_dias": "20.0", "dias_cobertura_extra_default": "4.0"},
              {"categoria_id": "2", "nombre": "LACTEOS", "categoria_padre_id": "1", "umbral_max_cobertura_dias": "20.0", "dias_cobertura_extra_default": "4.0"}])
    escribir(c, "proveedores.csv",
             ["proveedor_id", "razon_social", "lead_time_promedio_dias", "condicion_pago", "activo"],
             [{"proveedor_id": "0", "razon_social": "Sin Proveedor", "lead_time_promedio_dias": "3", "condicion_pago": "", "activo": "True"}])
    escribir(c, "sucursales.csv", ["sucursal_id", "nombre", "direccion", "activa"],
             [{"sucursal_id": "1", "nombre": "Sucursal Centro", "direccion": "x", "activa": "True"}])
    escribir(c, "productos.csv",
             ["producto_id", "sku", "nombre", "categoria_id", "proveedor_id", "unidad_medida", "precio_costo", "precio_venta", "lead_time_dias", "activo"],
             [{"producto_id": "779", "sku": "779", "nombre": "Leche", "categoria_id": "2", "proveedor_id": "0", "unidad_medida": "UN", "precio_costo": "10", "precio_venta": "15", "lead_time_dias": "3", "activo": "True"}])
    escribir(c, "ventas.csv", ["venta_id", "fecha", "producto_id", "sucursal_id", "unidades_vendidas", "importe_total"],
             [{"venta_id": "1", "fecha": "2026-08-01", "producto_id": "779", "sucursal_id": "1", "unidades_vendidas": "3", "importe_total": "45"}])
    escribir(c, "inventario.csv", ["inventario_id", "fecha_snapshot", "producto_id", "sucursal_id", "stock_actual", "stock_en_transito"],
             [{"inventario_id": "1", "fecha_snapshot": "2026-09-06", "producto_id": "779", "sucursal_id": "1", "stock_actual": "5", "stock_en_transito": "0"}])
    return c
