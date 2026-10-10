import sys
from pathlib import Path

import pytest

# El paquete se importa desde el repo sin instalarlo: el ETL es una
# herramienta del repositorio, no una librería que se publique.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

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
