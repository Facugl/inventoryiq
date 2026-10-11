# ETL del catálogo real

Convierte los exports crudos del POS XRP al formato de `data/csv/*.csv` que ya
consumen los adaptadores del backend. **No toca el backend ni el dominio.**

Las decisiones de negocio, el relevamiento de los exports y el mapeo campo por
campo están en [`../docs/InventoryIQ_ETL_Catalogo.md`](../docs/InventoryIQ_ETL_Catalogo.md).

## Estado

| Entidad | Estado |
|---|---|
| `categorias.csv` | implementado |
| `proveedores.csv` | implementado |
| `inventario.csv` | implementado — la correspondencia de sucursales es una suposición, ver Sección 5.2 del doc |
| `productos.csv` | bloqueado — ningún export trae la categoría del producto para el catálogo completo (Sección 5 del doc) |

## Uso

```bash
python -m inventoryiq_etl migrar            # arma el catálogo entero y lo verifica
python -m inventoryiq_etl entidad categorias   # una sola, a una carpeta temporal
```

`migrar` lee de `../data/real-export/` y escribe en `../data/csv-real/`, que
está fuera de git. **No escribe en `../data/csv/`**: esa carpeta está
versionada y el repositorio es público, así que el catálogo real —con costos,
márgenes y proveedores— no puede terminar ahí. El backend se apunta a la
carpeta real con `INVENTORYIQ_CSV_BASE_PATH`.

Las entidades no se migran de a una: se referencian entre sí y el backend las
carga todo-o-nada. Por eso `migrar` verifica la integridad referencial entre
los seis archivos y **falla** mientras el conjunto no cierre.

Siempre imprime un reporte de qué leyó, qué emitió, qué descartó y qué ajustó.
Eso no es decorativo: varias decisiones del pipeline modifican filas (por
ejemplo, el stock negativo pasa a 0), y un ajuste silencioso sobre miles de
filas sería inaceptable.

## Desarrollo

Sin dependencias de runtime: todo sale de la biblioteca estándar. Para los
tests hace falta pytest.

```bash
python -m venv .venv
.venv/Scripts/python -m pip install -e ".[dev]"   # Linux/Mac: .venv/bin/python
.venv/Scripts/python -m pytest
```

Los tests corren contra **fixtures sintéticos** en `tests/fixtures/`, no contra
los exports reales: esos traen CUIT, costos y márgenes de un negocio real y
están fuera de git. Cada fila de un fixture reproduce un defecto observado en
los exports verdaderos y está ahí por un motivo nombrado.
