# ETL del catálogo real

Convierte los exports crudos del POS XRP al mismo formato de CSV que ya
consumen los adaptadores del backend. **No toca el backend ni el dominio.**

Las decisiones de negocio, el relevamiento de los exports y el mapeo campo por
campo están en [`../docs/InventoryIQ_ETL_Catalogo.md`](../docs/InventoryIQ_ETL_Catalogo.md).

## Estado

| Entidad | Estado |
|---|---|
| `categorias.csv` | implementado |
| `proveedores.csv` | implementado |
| `inventario.csv` | implementado — la correspondencia de sucursales se infiere por volumen, decidido así en la Sección 5.2 del doc |
| `productos.csv` | implementado — 62,6% del catálogo queda con categoría real y el resto va a "No Categorizado" (decisión 4.7) |
| `ventas.csv` | implementado — del reporte de ventas exportado con `desde = hasta`, uno por día y sucursal |

Lo que falta no es código: es **historia**. El sistema calcula sobre series
diarias, y hoy hay un día de cada una. Ver "La rutina diaria".

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
carga todo-o-nada. Por eso `migrar` verifica dos cosas distintas:

- **Integridad referencial** entre los seis archivos. Si no cierra, falla con
  código distinto de cero: el problema aparece ahora y no al arrancar el
  backend.
- **Historial suficiente.** Un catálogo puede cerrar perfecto y no servir para
  nada, así que además comprueba que haya días de venta con snapshot del día
  anterior. Si no los hay, lo dice —hoy es el caso— sin hacer fallar el
  comando, porque el catálogo *es* consistente.

Siempre imprime un reporte de qué leyó, qué emitió, qué descartó y qué ajustó.
Eso no es decorativo: varias decisiones del pipeline modifican filas (por
ejemplo, el stock negativo pasa a 0), y un ajuste silencioso sobre miles de
filas sería inaceptable.

## La rutina diaria

El sistema necesita historia que el POS no guarda, y hay una asimetría que
define todo lo demás:

| | ¿Se puede pedir un día pasado? |
|---|---|
| Ventas | **Sí**, el reporte acepta rango de fechas |
| Stock | **No**, el export es "stock actual" |

Por eso lo único urgente es el stock: **cada día sin exportarlo es un día que
no existe nunca más**.

```
data/real-export/diarios/      ← dejar acá los exports, sin renombrar
```

- **Todos los días:** exportar la consulta de stock y dejarla en esa carpeta.
  Siempre a la misma hora, preferentemente al cierre: el backend trata el
  snapshot como stock de cierre del día.
- **Cuando quieras:** exportar el reporte de ventas con `desde = hasta`, uno
  por día y por sucursal, a la misma carpeta.
- **Después:** `python -m inventoryiq_etl migrar`.

No hace falta renombrar nada: cada export trae su fecha en el pie y su unidad
de negocio en el encabezado, y el ETL los lee de ahí — no del `mtime`, que se
cambia con sólo copiar el archivo.

Un listado de **más de un día se rechaza**. Su `Cantidad` viene agregada al
período, y anotarla como si fuera un día solo inflaría el ADS de todo el
catálogo sin que nada falle.

`inventario.csv` y `ventas.csv` **se acumulan y nunca se pisan**: cada corrida
agrega lo que falte y conserva lo demás. Eso incluye las filas que escribe la
aplicación —el conteo manual de stock y la carga de ventas por CSV agregan a
esos mismos archivos—, que de otro modo se borrarían en cada migración. Para
rehacer un archivo desde cero hay que borrarlo.

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
