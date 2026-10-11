# InventoryIQ — ETL del catálogo real

**Estado: PARCIALMENTE IMPLEMENTADO.** Este documento registra el relevamiento
de los exports reales del POS y las decisiones de negocio tomadas antes de
escribir código. De las cuatro entidades, **categorías**,
**proveedores** e **inventario** están implementadas (`etl/`); productos sigue
siendo diseño, bloqueado por la Sección 5.

Relevamiento hecho el 2026-10-10 sobre los exports en `data/real-export/`
(carpeta ignorada por git, ver `.gitignore`).

---

## 1. Alcance

Un pipeline aparte, en Python, que convierte los exports crudos del POS al
formato de `data/csv/*.csv` que **los adaptadores ya consumen**:

```
data/real-export/   →   [ etl/ ]   →   data/csv/
```

No toca el backend ni el dominio. Es deliberadamente distinto del ETL de la
fila v2.0 del roadmap, que sí implica reemplazar el adaptador CSV por uno
Postgres/DW.

Los exports reales **no se versionan**: traen razones sociales con CUIT,
precios de costo, márgenes y niveles de stock de un negocio real, y este
repositorio es público. Los tests corren contra fixtures sintéticos (Sección 6).

---

## 2. Relevamiento de los exports

### 2.1 Formatos

Los cuatro exports usan tres formatos distintos, y ninguno es el que sugiere su
extensión:

| Archivo | Formato real | Volumen |
|---|---|---|
| `categorias_xrp.csv` | CSV `;` + cp1252 | 267 filas |
| `Cambio_de_precios_xrp.csv` | CSV `;` + cp1252 | 10.325 filas |
| `Mantenimiento_de_Proveedores_.csv` | CSV `;` + cp1252 | 62 filas |
| `Consulta_de_stock_xrp.xls` | **HTML** (`<table>`), no Excel | 12.725 filas |
| `Ventas_de_Productos.xls` | **SpreadsheetML** (XML de Excel 2003) | 375 filas |

Los `.xls` no se abren con `xlrd` ni `openpyxl`: uno es HTML y el otro XML.
Cada uno necesita su propio lector.

### 2.2 Entidades HTML

Hay `&amp;` embebidos en los textos. Como el `;` de la entidad coincide con el
separador de campos, esas filas quedan con **un campo de más** y todas las
columnas corridas a partir de ahí. No falla ruidosamente: guarda valores en la
columna equivocada.

| Archivo | Filas afectadas |
|---|---|
| `Cambio_de_precios_xrp.csv` | 84 de 10.325 |
| `Mantenimiento_de_Proveedores_.csv` | 1 de 62 |

**Reemplazar `&amp;` por `&` antes de partir por `;` las normaliza por
completo**: las 10.325 filas quedan con 24 campos y las 62 con 12. No hace
falta nada más sofisticado.

### 2.3 Claves de producto

- `Cód.Barra`: 10.325 valores, **10.325 distintos** (único), 1 vacío.
- `Cód.Int.`: 10.325 valores, **10.227 distintos** → 98 duplicados.

Se usa `Cód.Barra` como `sku`. `Cód.Int.` no sirve como clave.

### 2.4 Proveedores

El roadmap anticipaba una reconciliación difusa de ~130 nombres contra 61
registrados. **El relevamiento la descarta:**

- 54 nombres distintos en el export de precios, 62 proveedores registrados.
- 52 matchean exacto contra razón social o nombre comercial.
- 1 fallaba sólo por la entidad HTML de la Sección 2.2, que partía su razón
  social en dos columnas. Reparada, matchea.
- Queda 1 sin match: el literal `Sin Proveedor`, que es un **centinela**, no un
  proveedor, y cubre **3.690 filas (36% del catálogo)**.

Con el reemplazo de `&amp;` previo, **matcheo exacto por nombre alcanza**.

`Ventas_de_Productos.xls` además trae `Id.Prov.` numérico, que permitiría
resolver el proveedor por id en vez de por nombre, pero sólo cubre 375
productos (ver Sección 5).

### 2.5 Stock

`Consulta_de_stock_xrp.xls`, columna `Stk.(U)`, sobre 12.725 filas:

| Valor | Filas |
|---|---|
| Negativo | 5.152 |
| Positivo | 2.057 |
| Exactamente 0 | 0 |
| Vacío o no numérico | 5.516 |

La columna `Unidad de Negocio` tiene exactamente dos valores, que mapean a las
dos sucursales activas de `sucursales.csv`.

El export trae también `Stk.Min.(U)` y `Stk.Máx.(U)` definidos en 12.268 filas.

### 2.6 Categorías

`categorias_xrp.csv` viene limpio: 267 filas, 14 raíces (`idparent = 0`), cero
jerarquías huérfanas, cero filas malformadas.

---

## 3. Mapeo por entidad

### 3.1 `categorias.csv`

| Destino | Origen | Transformación |
|---|---|---|
| `categoria_id` | `idcate` | directo |
| `nombre` | `denom` | directo |
| `categoria_padre_id` | `idparent` | `0` → vacío (raíz) |
| `umbral_max_cobertura_dias` | — | default, ver 4.4 |
| `dias_cobertura_extra_default` | — | default, ver 4.4 |

`idnivel`, `depto` y `catego` se descartan; `idnivel` sirve para validar que la
jerarquía reconstruida coincida.

### 3.2 `proveedores.csv`

| Destino | Origen | Transformación |
|---|---|---|
| `proveedor_id` | `Id` | directo |
| `razon_social` | `Razón Social` | reparar `&amp;` |
| `lead_time_promedio_dias` | — | default, ver 4.3 |
| `condicion_pago` | — | sin dato en el POS, queda vacío |
| `activo` | — | todos activos |

Más el proveedor sintético de la decisión 4.2.

### 3.3 `productos.csv`

| Destino | Origen | Transformación |
|---|---|---|
| `producto_id` | — | generado por el ETL, estable por `sku` |
| `sku` | `Cód.Barra` | ver 2.3 |
| `nombre` | `Denominación` | reparar `&amp;` |
| `categoria_id` | — | **sin resolver, ver Sección 5** |
| `proveedor_id` | `Proveedor` | matcheo exacto por nombre, ver 2.4 |
| `unidad_medida` | `Presentación` / `Empaque` | a definir al implementar |
| `precio_costo` | `Costo c/IVA` | normalizar separador decimal |
| `precio_venta` | `Pr.Vta` | normalizar separador decimal |
| `lead_time_dias` | — | default, ver 4.3 |
| `activo` | `Habilitación` | ver 4.5 |

### 3.4 `inventario.csv`

| Destino | Origen | Transformación |
|---|---|---|
| `inventario_id` | — | correlativo |
| `fecha_snapshot` | — | parámetro, ver más abajo |
| `producto_id` | `Código Barra` | el código de barras **es** el producto_id |
| `sucursal_id` | `Unidad de Negocio` | tabla de equivalencias, ver 2.5 |
| `stock_actual` | `Stk.(U)` | ver 4.1 |
| `stock_en_transito` | — | sin dato en el POS, `0` |

**`producto_id` es el código de barras**, no un contador del ETL ni un código
interno del POS:

- Tiene que ser **estable entre corridas**. Las recomendaciones viven en
  Postgres y guardan el `productId`: si una re-corrida reasignara los ids,
  cada recomendación pasaría a apuntar a otro producto, en silencio. Un
  contador depende del orden del archivo y del catálogo del día.
- Es la **única clave que comparten los dos exports**. `Cod.Int` coincide
  entre stock y precios (10.178 de 10.178 verificados), pero tiene 98
  duplicados en precios, así que no identifica; el `Id.` de precios es casi
  único pero no está en el export de stock.

Consecuencia aceptada: `producto_id` y `sku` terminan siendo el mismo número.

**`fecha_snapshot` no está en el export**: el documento no contiene ninguna
fecha. Se recibe por parámetro y, si no se indica, se cae a la fecha de
modificación del archivo. En ambos casos el reporte deja asentado qué fecha
usó y de dónde salió, en vez de elegirla en silencio.

**Las sucursales son una suposición pendiente de confirmar.** El export nombra
`Administración Central Alem` (10.148 filas) y `Sucursal Santa Cruz` (2.577),
pero `data/csv/sucursales.csv` todavía tiene los nombres **simulados** (Centro,
Norte, Sur). La correspondencia se infirió por volumen: la unidad más grande se
mapea a la sucursal 1. Un stock cargado en la sucursal equivocada no falla,
simplemente recomienda mal, así que hay que confirmarla antes de volcar el
inventario a `data/csv/` (ver Sección 5.2).

---

## 4. Decisiones tomadas

Decididas por el usuario el 2026-10-10.

### 4.1 Stock negativo a `0`, stock vacío se omite

Un stock negativo en el POS es casi siempre un desfasaje contable (ventas
registradas sin la compra correspondiente), no mercadería real: para decidir
una reposición, tratarlo como `0` es lo honesto. Las 5.516 filas sin valor **no
se inventan**: quedan fuera del snapshot, porque un `0` inventado afirmaría que
el producto está agotado.

Esto preserva el invariante `currentStock >= 0` que ya tiene `Inventory` en el
dominio, en vez de relajarlo.

**Implicancia:** el ETL tiene que reportar cuántas filas ajustó y cuántas
omitió. Un ajuste silencioso sobre el 40% de las filas es inaceptable.

### 4.2 Un proveedor sintético "Sin asignar"

Los 3.690 productos con `Sin Proveedor` **entran al catálogo**, apuntando a un
proveedor creado por el ETL con el lead time por defecto. La alternativa —
dejarlos afuera — haría que el sistema ignore más de un tercio de lo que el
negocio vende.

La pantalla de Proveedores permite corregir su lead time después.

**Id `0` y razón social `Sin Proveedor`**, y ninguna de las dos cosas es
arbitraria:

- El POS usa `Id.Prov. = 0` **exclusivamente** para `Sin Proveedor` (verificado
  sobre `Ventas_de_Productos.xls`), y el `0` no está tomado: los proveedores
  registrados van de 1 a 62.
- El export de precios **no trae id de proveedor**, sólo el nombre como texto
  (`Id.` y `Id` son del producto, y `Cód.Prov.` está vacío en el 99,5% de las
  filas). Productos va a resolver el proveedor por nombre, así que conservar el
  literal del POS evita un caso especial en ese cruce.

Al implementarlo se cambió el nombre previsto —"Sin asignar"— por el literal
del POS, por el segundo motivo.

El id `0` queda **reservado**: si el POS llegara a emitir un proveedor con ese
id, la fila se descarta y se avisa. Sin eso, los productos sin proveedor
terminarían apuntando en silencio a un proveedor real.

### 4.3 Lead time: un default global único

El POS no registra lead time, ni por producto ni por proveedor. Se usa un único
valor para todas las filas, corregible por proveedor desde la pantalla de
Proveedores (Sección 8.11 de la documentación funcional).

**Valor: 3 días** (decidido el 2026-10-10), a ajustar por proveedor desde la
pantalla una vez cargado el catálogo.

> El lead time alimenta directamente el punto de pedido: un valor demasiado
> bajo genera quiebres, uno demasiado alto infla las sugerencias de compra. Es
> el default con más impacto de los tres, y el primero que conviene corregir
> para los proveedores de mayor volumen.

### 4.4 Umbrales de categoría: los valores de los datos simulados

`umbral_max_cobertura_dias` y `dias_cobertura_extra_default` no existen en el
POS. Como las 267 categorías reales no tienen correspondencia con las 14
simuladas, no hay un "copiado" literal posible: se aplica a todas el valor más
frecuente de los datos simulados — **20 días** de umbral de sobrestock y **4
días** de cobertura extra — como punto de partida, ajustable por categoría
desde la pantalla de Administración.

### 4.5 Todos los productos quedan activos

`Habilitación` sólo toma dos valores: `Compra y Venta` (10.317 filas) y `Compra`
(8 filas). Se descartó tratar `Compra` como inactivo: son 8 productos y el
criterio no está claro.

### 4.6 No se usan `Stk.Min`/`Stk.Máx` para los umbrales

Se evaluó derivar los umbrales de categoría de las columnas `Stk.Min.(U)` y
`Stk.Máx.(U)` del export de stock. Se descartó: son por producto y por
sucursal, mientras que los parámetros de InventoryIQ son por categoría.

---

## 5. Decisiones pendientes

### 5.1 La categoría de los productos

**Es el bloqueante del ETL de productos.** `productos.csv` exige un
`categoria_id` que apunte a una categoría existente, y la carga es todo-o-nada.

Ni `Cambio_de_precios_xrp.csv` (24 columnas) ni `Consulta_de_stock_xrp.xls` (27
columnas) traen la categoría del producto.

El único export que la trae es **`Ventas_de_Productos.xls`**, que además es el
de mejor calidad para este fin:

- Columnas `Departamento` y `Categoría` (con ruta completa, p. ej. `ALMACEN,
  BEBIDAS`), más `Barra` para unir contra el catálogo de precios.
- 98 categorías distintas, y sólo 1 fila sin categoría.
- Trae además `Id.Prov.` numérico, que evitaría el matcheo por nombre.

**Su limitación:** cubre **375 de 10.325 productos (3,6%)**. Es un listado de
ventas de un período, no un maestro de productos.

Lo que demuestra es que **el POS puede exportar la categoría por producto**. El
camino natural es generar ese mismo reporte con un rango de fechas mucho más
amplio, o encontrar en XRP un listado de productos que la incluya.

Mientras tanto, las entidades `categorias`, `proveedores` e `inventario` se
pueden migrar sin depender de esto.

### 5.2 Los nombres de las sucursales

`data/csv/sucursales.csv` tiene tres sucursales con **nombres y direcciones
inventados**, heredados de los datos simulados:

```
1,Sucursal Centro,"Av. San Martín 1450, Santiago del Estero",True
2,Sucursal Norte,"Ruta 9 Km 3.5, Santiago del Estero",True
3,Sucursal Sur,"Av. Belgrano Sur 890, Santiago del Estero",False
```

El negocio tiene dos, y el export de stock las nombra `Administración Central
Alem` (10.148 filas) y `Sucursal Santa Cruz` (2.577).

El ETL de inventario necesita traducir esos nombres a un `sucursal_id`, y hoy
lo hace con una correspondencia **inferida por volumen** (la unidad más grande
se mapea a la 1), declarada en `decisiones.SUCURSAL_POR_UNIDAD_DE_NEGOCIO`.

No se puede confirmar contra los nombres actuales, porque son ficticios: la
salida natural es reemplazarlos por los reales, conservando los ids 1 y 2 para
no invalidar las recomendaciones ya guardadas en Postgres. El POS no exporta
direcciones de sucursal.

**Decisión del usuario (2026-10-10): dejarlo como está por ahora.** El riesgo
está contenido mientras el inventario no se vuelque a `data/csv/`; si se
volcara con la correspondencia al revés, el stock quedaría cargado en la
sucursal equivocada, y eso no falla: recomienda mal.

---

## 6. Estrategia de tests: fixtures sintéticos

Los tests **no** usan los exports reales. Se escriben archivos de entrada
chicos e inventados que reproducen los mismos defectos observados, y se
versionan en `etl/tests/fixtures/`.

Cada fila de un fixture está por un motivo nombrado. Los defectos a cubrir, con
su evidencia en la Sección 2:

| Caso | Dónde se observó |
|---|---|
| Entidad `&amp;` que corre las columnas | 2.2 |
| Encoding cp1252 con acentos y `ñ` | 2.1 |
| Separador `;` | 2.1 |
| `Cód.Int.` duplicado | 2.3 |
| `idparent = 0` como raíz | 2.6 |
| Proveedor centinela `Sin Proveedor` | 2.4 |
| Stock negativo, vacío y no numérico | 2.5 |
| `.xls` que es HTML, y `.xls` que es SpreadsheetML | 2.1 |

Esto permite que los tests corran en CI sin datos reales, que cada defecto
quede documentado como un caso con nombre, y que el repositorio siga siendo
publicable.

El pipeline se diseñó contra defectos **observados en exports reales**; los
exports no se publican.

---

## 7. Orden de implementación

Lo imponen las dependencias:

1. **Categorías** — IMPLEMENTADO. No depende de nada y es el archivo más
   limpio; fijó la forma del pipeline (lectores, reporte de incidencias,
   estructura de tests). Sobre el export real: 267 leídas, 267 emitidas, cero
   descartes y cero ajustes, y el `CsvCategoryRepositoryAdapter` real las carga
   como 14 raíces y 253 con padre.
2. **Proveedores** — IMPLEMENTADO. Sobre el export real: 62 leídas, 63
   emitidas (las 62 más el proveedor de la decisión 4.2), cero descartes. El
   `CsvSupplierRepositoryAdapter` real las carga, con la razón social que traía
   la entidad HTML ya reparada. Verificado además que **los 54 nombres de
   proveedor del export de precios resuelven** contra el archivo generado,
   cubriendo las 10.325 filas de producto: el cruce de productos no va a
   necesitar casos especiales.
3. **Inventario** — IMPLEMENTADO. Sumó el lector de HTML disfrazado de `.xls`.
   Sobre el export real: 12.725 leídas, 7.209 emitidas, 5.516 descartadas
   (5.515 sin dato de stock y 1 sin código de barras) y 5.152 ajustadas por
   stock negativo. En la salida no queda ningún negativo, y el
   `CsvInventoryRepositoryAdapter` real la carga — lo que también prueba el
   ajuste, porque el invariante `currentStock >= 0` de `Inventory` habría
   fallado con cualquier negativo.
4. **Productos** — bloqueado por la Sección 5; depende de 1 y 2.

> Nota del slice 1: el catálogo real **no contiene el `categoria_id` 14** que
> usan los productos simulados, así que `data/csv/categorias.csv` no se puede
> reemplazar de forma aislada — categorías y productos tienen que migrarse
> juntos, o el backend falla al arrancar por integridad referencial.
