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
