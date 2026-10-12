# InventoryIQ — ETL del catálogo real

**Estado: IMPLEMENTADO.** Este documento registra el relevamiento de los
exports reales del POS y las decisiones de negocio tomadas antes de escribir
código. Las cinco entidades están implementadas (`etl/`): **categorías**,
**proveedores**, **productos**, **inventario** y **ventas**.

El catálogo todavía **no es utilizable**, pero ya no por falta de código: falta
**historia**. El stock y las ventas se acumulan día a día a partir de exports
periódicos, y hoy hay un día de cada uno. La Sección 5.3 explica por qué el
inventario no se puede recuperar hacia atrás.

**El proyecto está pausado en ese punto, a propósito** — construir ese
historial exige un export manual diario y sostenido. Ver "Dónde se detuvo" en
`InventoryIQ_Roadmap.md` para el estado exacto y los dos caminos para
retomarlo.

Relevamiento hecho el 2026-10-10 sobre los exports en `data/real-export/`
(carpeta ignorada por git, ver `.gitignore`), ampliado el 2026-10-11 con el
listado de ventas que destrabó la categoría de los productos (Sección 5.1).

---

## 1. Alcance

Un pipeline aparte, en Python, que convierte los exports crudos del POS al
mismo formato de CSV que **los adaptadores ya consumen**:

```
data/real-export/   →   [ etl/ ]   →   data/csv-real/
```

El destino es `data/csv-real/`, **no** `data/csv/`. Esta última está versionada
y el repositorio es público: volcar ahí el catálogo real publicaría costos,
márgenes y proveedores. `data/csv/` sigue siendo el dataset simulado con el que
cualquiera puede clonar y levantar el sistema, y el backend se apunta al real
con `INVENTORYIQ_CSV_BASE_PATH`.

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
| `Listado_ventas_*.xls` | **SpreadsheetML** (XML de Excel 2003) | 6.528 filas |
| `Ventas_de_Productos.xls` | **SpreadsheetML** (XML de Excel 2003) | 375 filas |

Los `.xls` no se abren con `xlrd` ni `openpyxl`: uno es HTML y el otro XML.
Cada uno necesita su propio lector (`fuentes.leer_tabla_html` y
`fuentes.leer_spreadsheetml`).

Los dos SpreadsheetML son el mismo reporte —*Ventas de Productos*— exportado
con distinto filtro: el POS lo deja filtrar por sucursal y por rango de fechas,
y cada corrida cubre los productos que se vendieron en ese recorte. El ETL lee
**todos** los que encuentre y los une, porque la categoría es un atributo del
producto y no de la sucursal: cuantos más listados haya, más catálogo queda
categorizado.

Ninguno de los dos usa `ss:Index` para omitir celdas vacías, que es lo que
haría peligroso el parseo posicional: una celda salteada correría todas las
columnas siguientes en silencio.

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

Trae además dos columnas que no van al destino pero son la clave del cruce con
productos: `depto` y `catego`. **`catego` es la ruta completa de la categoría**
(`ALMACEN, BEBIDAS`), con exactamente la misma forma que la columna `Categoría`
del listado de ventas — por eso el cruce es una igualdad directa y no hay que
partir ni normalizar nada.

`denom` **no** sirve para ese cruce: 38 nombres se repiten entre departamentos
(`BEBIDAS` existe en `ALMACEN` y en `LIMPIEZA`). `catego` es único en las 267.

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

Origen principal: `Cambio_de_precios_xrp.csv`. Es el único slice que cruza
varios exports, porque el maestro de precios no trae ni la categoría ni el id
del proveedor.

| Destino | Origen | Transformación |
|---|---|---|
| `producto_id` | `Cód.Barra` | ver 2.3 |
| `sku` | `Cód.Barra` | mismo valor que `producto_id` |
| `nombre` | `Denominación` | reparar `&amp;` |
| `categoria_id` | `Categoría` del listado → `catego` de categorías | ver 2.6; sin match, decisión 4.7 |
| `proveedor_id` | `Proveedor` | matcheo exacto por nombre, ver 2.4 |
| `unidad_medida` | `Presentación` | último token (`1x Kgr` → `KG`), ver 4.8 |
| `precio_costo` | `Costo c/IVA` | sacar la coma de miles, sin redondear |
| `precio_venta` | `Pr.Vta` | sacar la coma de miles, sin redondear |
| `lead_time_dias` | — | default, ver 4.3 |
| `activo` | `Habilitación` | ver 4.5 |

**Los precios no se redondean a centavos.** El POS los declara con cinco
decimales (`1,428.57400`) y el `BigDecimal` del backend parsea la cadena
completa: recortarla sería un ajuste silencioso sobre las 10.000 filas del
catálogo, que es justo lo que este pipeline evita.

**Se descarta la fila sin precio.** 40 productos no tienen `Pr.Vta` y 4 no
tienen `Costo c/IVA` (41 filas en total). El backend los parsea con
`BigDecimal`, que no tolera un vacío: con uno solo, no arranca. Un 0 inventado
tampoco sirve, porque distorsiona el capital inmovilizado y la clasificación
ABC.

**Se descarta el producto cuyo proveedor no resuelve.** No se cae al proveedor
de la decisión 4.2: eso le cambiaría el lead time en silencio, y de ahí sale el
punto de pedido. Sobre el export real no se descartó ninguno —los 10.325
resuelven, como anticipaba 2.4.

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

**Las sucursales se corresponden por volumen.** El export nombra dos unidades
de negocio, de 10.148 y 2.577 filas, pero `sucursales.csv` tiene los nombres
**simulados** (Centro, Norte, Sur), así que la correspondencia se infirió por
volumen: la unidad más grande se mapea a la sucursal 1. No se puede verificar contra los nombres, y por decisión del
usuario (2026-10-11) queda así — ver Sección 5.2, donde está el razonamiento
completo. La consecuencia si estuviera invertida sigue en pie: un stock cargado
en la sucursal equivocada no falla, simplemente recomienda mal.

### 3.5 `ventas.csv`

Origen: el reporte *Ventas de Productos* exportado con **`desde = hasta`**.

| Destino | Origen | Transformación |
|---|---|---|
| `venta_id` | — | correlativo del ETL |
| `fecha` | el `Período` del encabezado | tiene que ser de un solo día |
| `producto_id` | `Barra` | ver 2.3 |
| `sucursal_id` | `Unidad de Negocio` del encabezado | ver 5.2 |
| `unidades_vendidas` | `Cantidad` | redondeado a entero |
| `importe_total` | `Venta` | sin redondear |

**El archivo se describe a sí mismo**, y eso es lo que hace viable el proceso:
el encabezado declara la unidad de negocio y el período, así que no hace falta
ninguna convención de nombres de archivo ni un manifiesto aparte. Se exporta,
se deja en la carpeta, y el ETL sabe de qué día y de qué sucursal es.

**Un listado de más de un día se rechaza.** `Cantidad` viene agregada al
período, así que sólo es venta diaria si el período es de un día. Un listado de
tres meses anotado como un martes inflaría el ADS de todo el catálogo sin que
nada falle — es el error más caro del pipeline y por eso se verifica contra el
archivo en vez de confiar en quien lo exportó.

**Los días sin venta no se emiten**, igual que antes: un día ausente ya
significa que no se vendió. Tampoco los de menos de una unidad (productos por
peso) ni los de importe no numérico, que se descartan con su motivo.

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

### 4.7 Los productos sin categoría van a "No Categorizado"

El listado de ventas trae la categoría de los productos que se vendieron, no
la de todo el catálogo: quedan **3.861 productos sin categoría**, de los cuales
sólo **599 tienen stock**.

No se inventa una categoría. `categorias_xrp.csv` ya declara `idcate = 0,
No Categorizado`, y el slice de categorías la emite como una raíz más: el
centinela **existe en el origen**, igual que el proveedor de la decisión 4.2.

Consecuencia aceptada: esos productos comparten umbral de sobrestock y
cobertura extra como cualquier otra categoría (decisión 4.4), y se corrigen
desde Administración. Los 599 con stock entran al sistema y se pueden reponer;
el resto queda inerte, sin stock ni ventas que disparen nada.

Si el export de categorías no trajera la categoría 0, los productos sin
categoría **se descartan** en vez de emitirse: una referencia colgada no
rompería esa fila, rompería el arranque del backend.

### 4.8 La unidad de medida sale de `Presentación`

El POS no la declara como campo. Viene al final de `Presentación`, en su propia
nomenclatura: `1x1 Un`, `1x Kgr`, `1x700 ml`. Se traduce al vocabulario del
dataset simulado (`UN`, `KG`, `LT`, `ML`, `GR`).

7 filas traen `1x` a secas, sin unidad. No se descartan: se registran como `UN`
y el reporte lo dice. `Product.unitOfMeasure` hoy no alimenta ningún cálculo —
el dominio lo transporta y no lo lee nadie—, así que perder el producto costaría
más que arrastrar un default.

Se descartó usar `Empaque` (10.324 de 10.325 filas dicen `Und`, no distingue
nada) y `UxB` (es unidades por bulto, no unidad de medida).

---

## 5. Decisiones pendientes

### 5.1 La categoría de los productos — RESUELTA (2026-10-11)

Era el bloqueante del ETL de productos: `productos.csv` exige un `categoria_id`
que apunte a una categoría existente, y la carga es todo-o-nada. Ni
`Cambio_de_precios_xrp.csv` (24 columnas) ni `Consulta_de_stock_xrp.xls` (27
columnas) traen la categoría.

El único export que la trae es el reporte **Ventas de Productos**, y el
relevamiento original lo descartó por cobertura: la corrida disponible cubría
375 de 10.325 productos (3,6%). Lo que no se había probado es que el POS deja
**filtrar ese reporte por sucursal y por rango de fechas**. Una corrida con el
rango completo pasa de 375 filas a **6.528**.

Cobertura con esa corrida, cruzada por `Barra` contra el maestro de precios:

| | Productos | % |
|---|---|---|
| Catálogo completo | 10.325 | 100% |
| Con categoría | 6.464 | 62,6% |
| Sin categoría | 3.861 | 37,4% |
| **Con stock y con categoría** | **4.732** | **88,8% de los 5.331 con stock** |
| **Con stock y sin categoría** | **599** | 11,2% de los que tienen stock |

El 37,4% suena peor de lo que es: son productos que nunca se vendieron **y**
no tienen stock. Los que importan para reponer son los 5.331 con stock, y de
esos quedan categorizados casi 9 de cada 10. Los 599 restantes se resuelven con
la decisión 4.7.

La cobertura sube sola sacando el mismo reporte con el filtro de la otra
sucursal: el ETL lee todos los listados que encuentre y los une (Sección 2.1).

### 5.2 Los nombres de las sucursales

`data/csv/sucursales.csv` tiene tres sucursales con **nombres y direcciones
inventados**, heredados de los datos simulados:

```
1,Sucursal Centro,"Av. San Martín 1450, Santiago del Estero",True
2,Sucursal Norte,"Ruta 9 Km 3.5, Santiago del Estero",True
3,Sucursal Sur,"Av. Belgrano Sur 890, Santiago del Estero",False
```

El negocio tiene dos, y el export de stock las nombra con sus nombres reales
(10.148 filas una, 2.577 la otra).

El ETL de inventario necesita traducir esos nombres a un `sucursal_id`, y hoy
lo hace con una correspondencia **inferida por volumen** (la unidad más grande
se mapea a la 1), declarada en `decisiones.SUCURSAL_POR_UNIDAD_DE_NEGOCIO`.

No se puede confirmar contra los nombres actuales, porque son ficticios: la
salida natural es reemplazarlos por los reales, conservando los ids 1 y 2 para
no invalidar las recomendaciones ya guardadas en Postgres. El POS no exporta
direcciones de sucursal.

**Decisión del usuario (2026-10-11): se siguen usando los nombres simulados.**
La correspondencia por volumen queda firme, y `sucursales.csv` se arrastra tal
cual al catálogo real.

**Corrección (2026-10-11).** Se dijo entonces que esto mantenía los nombres
reales fuera de un repositorio público. Era falso: lo ficticio era sólo el
`sucursales.csv` de salida, mientras los nombres verdaderos estaban escritos en
`decisiones.py`, en los tests, en un fixture y en este mismo documento.

Ahora sí se cumple. La correspondencia no vive en el código: se arma en
`data/real-export/sucursales.json`, que está ignorado por git, y lo que queda
versionado es sólo la regla (`sucursales.derivar_por_volumen`). Si el archivo
no existe, el ETL lo deriva por volumen del propio export y lo deja escrito
para que se pueda revisar y corregir a mano — en vez de decidirlo en silencio
en cada corrida. Los fixtures usan nombres inventados.

### 5.3 El historial — ventas e inventario

Es lo único que separa al catálogo real de poder usarse, y son **dos** huecos.
El pipeline para los dos está implementado; lo que falta es que haya historia
que procesar, y eso sólo se consigue acumulando.

#### Por qué no se puede recuperar hacia atrás

| | ¿Se puede pedir un día pasado? |
|---|---|
| Ventas | **Sí.** El reporte acepta rango de fechas, así que un día viejo se exporta cuando se quiera |
| Stock | **No.** El export es "stock actual": un export, un día, el de hoy |

De ahí sale toda la estrategia. El stock es lo urgente: **cada día que pasa sin
exportarlo es un día que no existe nunca más**. Las ventas pueden esperar.

#### Por qué un snapshot solo no alcanza

| | Dataset simulado | Catálogo real (hoy) |
|---|---|---|
| Fechas de snapshot | 395 | 1 |

`ProductIndicatorsCalculator` pide la serie diaria a
`DailySalesRecordAssembler`, y esa serie **exige un snapshot del día anterior a
cada venta**: es lo que permite saber si el producto estuvo en quiebre, para
excluir ese día del ADS corregido (regla 4.9). Un producto sin ningún día
utilizable se descarta entero.

No alcanza con tener muchos días de cada cosa: tienen que **cruzarse**. El
comando `migrar` lo verifica y lo dice, porque la integridad referencial no lo
ve — un catálogo puede cerrar perfecto y dejar todas las pantallas vacías:

```
HISTORIAL INSUFICIENTE PARA CALCULAR:
  - ningún día de venta tiene un snapshot del día anterior (1 día(s) de
    venta, 1 de stock). El ADS corregido descarta TODOS los productos
CATÁLOGO CONSISTENTE, PERO SIN HISTORIAL SUFICIENTE
```

#### La rutina

Un solo archivo por día: se exporta la consulta de stock y se deja en
`data/real-export/diarios/`. Sin renombrar nada — el export trae su fecha en el
pie y su unidad de negocio en el encabezado.

Conviene **a la misma hora siempre**, y preferentemente al cierre: el
`DailySalesRecordAssembler` trata el snapshot como stock de cierre del día. Un
día a las 9 y otro a las 21 corren el significado de "día".

Las ventas se exportan cuando se quiera, con `desde = hasta`, una por sucursal
y por día, a la misma carpeta.

#### Una distorsión conocida

El snapshot es lo que decide si hubo quiebre. Si la mercadería entró pero la
factura todavía no se cargó, el POS muestra menos stock del real, el día
**parece** quiebre y se excluye del ADS aunque haya habido ventas. No se puede
eliminar desde el pipeline; cargar las facturas con un desfasaje chico y parejo
lo acota. Queda asentado acá porque es la primera explicación a mirar cuando un
número salga raro.

Es la otra cara de la limitación ya documentada en
`InventoryIQ_Arquitectura.md` (el stock del POS llega sin corregir por las
ventas posteriores al snapshot).

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
| Entidad `&#160;` (espacio duro numérico) | 2.1 |
| Fila del listado sin categoría | 5.1 |
| Ruta de categoría que el export de categorías no conoce | 2.6 |
| Producto sin `Pr.Vta` / sin `Costo c/IVA` | 3.3 |
| `Cód.Barra` vacío y duplicado | 2.3 |
| `Presentación` sin unidad reconocible (`1x`) | 4.8 |
| Proveedor del maestro de precios que no está registrado | 2.4 |

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
   Sobre el export real: 12.725 leídas, **7.173 emitidas**, 5.552 descartadas
   (5.515 sin dato de stock, 36 de productos que no quedaron en el catálogo y
   1 sin código de barras) y 5.152 ajustadas por stock negativo. En la salida
   no queda ningún negativo, y el `CsvInventoryRepositoryAdapter` real la carga
   — lo que también prueba el ajuste, porque el invariante `currentStock >= 0`
   de `Inventory` habría fallado con cualquier negativo.
4. **Productos** — IMPLEMENTADO. Es el único que cruza cuatro exports:
   precios (maestro), el listado de ventas (categoría), categorías (resolver la
   ruta a un id) y proveedores (resolver el nombre a un id). Los tres
   auxiliares los deriva él mismo de la misma carpeta, con las mismas
   transformaciones que los otros slices, así que los ids coinciden por
   construcción y no necesita el catálogo ya generado. Sobre los exports
   reales: 10.325 leídas, 10.283 emitidas, 42 descartadas (41 sin precio
   utilizable y 1 sin código de barras), **cero descartadas por proveedor sin
   correspondencia**, y 3.861 ubicadas en "No Categorizado" por la decisión
   4.7.

5. **Ventas** — IMPLEMENTADO. No sale de un export del catálogo sino del
   reporte de ventas exportado con `desde = hasta`, uno por día y por
   sucursal. Junto con inventario son las dos entidades que **acumulan**: cada
   corrida agrega los días nuevos y conserva los viejos, porque el stock no se
   puede pedir hacia atrás y porque las dos se escriben también desde la
   aplicación (conteo manual de stock, carga de ventas por CSV) y regenerar el
   archivo borraría eso.

El comando `migrar` arma las cinco entidades juntas en `data/csv-real/` y las
verifica antes de dar el catálogo por bueno. Existe porque las entidades **no
se pueden migrar de a una**: se referencian entre sí, y el backend las carga
todo-o-nada.

Verifica dos cosas distintas, y conviene no confundirlas:

- **Integridad referencial** entre los seis archivos. Si no cierra, falla con
  código distinto de cero: el problema aparece ahora y no al arrancar la
  aplicación.
- **Historial suficiente** (Sección 5.3). El catálogo puede cerrar perfecto y
  no servir para nada, así que además comprueba que haya días de venta con
  snapshot del día anterior. Hoy avisa que no los hay. No hace fallar al
  comando —el catálogo *es* consistente— pero lo dice en vez de cantar
  victoria.

> Nota del slice 1: el catálogo real **no contiene el `categoria_id` 14** que
> usan los productos simulados, así que `data/csv/categorias.csv` no se puede
> reemplazar de forma aislada — categorías y productos tienen que migrarse
> juntos, o el backend falla al arrancar por integridad referencial.
