# InventoryIQ — Documentación Técnica y Funcional

**Versión:** 1.0
**Tipo de documento:** Especificación funcional y arquitectónica completa
**Audiencia:** Equipo de desarrollo, arquitectura y producto
**Propósito:** Permitir la implementación completa del sistema sin necesidad de redefinir requisitos

---

# 1. Visión del Producto

## 1.1 Problema que resuelve

En un supermercado —ya sea de barrio, de cadena regional o de gran superficie— la gestión de compras suele apoyarse en la intuición del responsable de turno, en hojas de cálculo dispersas o en reportes estáticos del ERP que muestran el pasado pero no orientan sobre el futuro. Esto genera tres síntomas recurrentes:

- **Quiebres de stock (stockouts):** productos que se agotan antes de que llegue la próxima reposición, generando pérdida de ventas y de confianza del cliente.
- **Sobrestock (overstock):** productos comprados en exceso que inmovilizan capital, ocupan espacio en depósito y, en el caso de perecederos, generan mermas.
- **Decisiones de compra reactivas:** el pedido se genera cuando el problema ya es visible (góndola vacía), no cuando el dato lo anticipa.

InventoryIQ nace para cerrar esa brecha: convertir datos históricos de ventas, compras e inventario en **decisiones de compra anticipadas, cuantificadas y justificadas**.

InventoryIQ **no es un ERP** ni pretende serlo. No gestiona ventas, cajas, clientes, facturación ni logística. Es una **capa de inteligencia** que se alimenta de los datos que el ERP ya genera (o que, en su ausencia, se simulan mediante archivos CSV) y devuelve una respuesta a tres preguntas:

1. ¿Qué debería comprar el supermercado?
2. ¿Cuándo debería comprarlo?
3. ¿En qué cantidad?

## 1.2 Usuarios del sistema

| Usuario | Rol | Necesidad principal |
|---|---|---|
| Responsable de Compras | Usuario primario | Recibir una lista priorizada de qué reponer, con cantidad y urgencia |
| Encargado de Categoría / Sector | Usuario secundario | Visualizar el comportamiento de su categoría (rotación, stock, alertas) |
| Gerente de Sucursal | Usuario consumidor de reportes | Visualizar KPIs agregados de su sucursal (capital inmovilizado, quiebres evitados) |
| Administrador del Sistema | Usuario técnico | Cargar/administrar archivos fuente (CSV), monitorear procesos ETL, configurar parámetros de negocio |

InventoryIQ es una herramienta de **soporte a la decisión**, no de ejecución automática: el sistema recomienda, la persona decide y ejecuta la orden de compra en el ERP.

## 1.3 Objetivos del producto

**Objetivo general**

Proveer al responsable de compras de un supermercado una herramienta que transforme datos históricos de ventas, inventario y compras en recomendaciones de reposición accionables, reduciendo quiebres de stock y capital inmovilizado.

**Objetivos específicos**

- Detectar productos en riesgo de quiebre de stock antes de que ocurra.
- Detectar productos con sobrestock o baja rotación.
- Calcular puntos de pedido, stock de seguridad y cantidades óptimas de reposición.
- Priorizar las recomendaciones según criticidad del producto.
- Presentar la información en un dashboard claro, orientado a la acción.
- Diseñar una arquitectura desacoplada del origen de datos, de forma que el paso de CSV simulados a un pipeline ETL real no requiera modificar la lógica de negocio.

## 1.4 Casos de uso principales (nivel producto)

- Como responsable de compras, quiero ver un listado de productos críticos a reponer hoy, ordenados por urgencia, para decidir qué pedir primero.
- Como responsable de compras, quiero saber cuánto pedir de cada producto, para no sobre-comprar ni sub-comprar.
- Como encargado de categoría, quiero ver la rotación de mis productos, para detectar cuáles dejar de reponer.
- Como gerente de sucursal, quiero ver el capital inmovilizado en sobrestock, para tomar decisiones de liquidación o promoción.
- Como administrador, quiero cargar nuevos archivos CSV de ventas/compras/inventario y que el sistema recalcule automáticamente las recomendaciones.
- Como responsable de compras, quiero recibir alertas de productos que están por quebrar stock antes del lead time del proveedor, para pedir a tiempo.

## 1.5 Alcance del MVP

**Incluido:**

- Ingesta de datos desde archivos CSV simulados (productos, ventas, compras, inventario, proveedores, categorías, sucursales).
- Cálculo de indicadores de inventario: stock actual, rotación, ventas promedio, punto de pedido, stock de seguridad.
- Motor de recomendación de compra (qué, cuándo, cuánto).
- Detección de productos críticos y de sobrestock.
- API REST para exponer productos, alertas y recomendaciones.
- Dashboard web (React) con listados, KPIs y gráficos básicos.
- Arquitectura hexagonal que desacople el dominio del origen de datos.

## 1.6 Fuera de alcance (MVP)

- Gestión de ventas, cajas o clientes.
- Emisión de órdenes de compra reales hacia proveedores (integración EDI, envío de mails, etc.).
- Autenticación multi-rol avanzada (SSO, permisos granulares) — se contempla un login básico o incluso ausencia de autenticación en el MVP.
- Forecasting con modelos de Machine Learning (se documenta como estrategia futura, no se implementa en el MVP).
- Multi-tenant real (varios supermercados distintos con aislamiento total de datos) — se diseña pensando en ello pero no se implementa en el MVP.
- Proceso ETL real conectado a un ERP productivo (se diseña el proceso, pero el MVP consume CSV simulados).

## 1.7 Roadmap de alto nivel

| Versión | Foco |
|---|---|
| MVP | CSV simulados, motor de reglas, API, dashboard básico |
| v1.1 | Alertas configurables, exportación de reportes, mejoras de UX |
| v1.2 | Multi-sucursal, comparativas entre sucursales |
| v2.0 | Reemplazo de CSV por ETL real (Python + Pandas) sobre Data Warehouse en modelo estrella |
| v3.0 | Forecasting estadístico/ML de demanda, estacionalidad avanzada, optimización de compras multi-proveedor |

(El detalle completo de cada versión se desarrolla en la Sección 12 — Roadmap Evolutivo.)


---

# 2. Arquitectura Completa

## 2.1 Principio rector

El principio central de InventoryIQ es que **el backend no debe conocer ni depender del origen de los datos**. Da igual si la información proviene de un CSV cargado manualmente o de un Data Warehouse alimentado por un ETL: el dominio de negocio (reglas de reposición, cálculo de puntos de pedido, detección de criticidad) debe permanecer intacto.

Esto se logra mediante **Arquitectura Hexagonal (Ports & Adapters)** combinada con **Domain-Driven Design (DDD)** y los principios de **Clean Architecture**.

## 2.2 Arquitectura general del sistema (vista macro)

```
[ Fuente de Datos ]
        |
        v
[ Proceso ETL ]  (Python + Pandas)  — en el MVP: sustituido por CSV simulados ya "limpios"
        |
        v
[ Data Warehouse ]  (PostgreSQL, Modelo Estrella)
        |
        v
[ Backend Spring Boot ]  (Arquitectura Hexagonal + DDD)
        |
        v
[ Frontend React + TypeScript ]
        |
        v
[ Usuario: Responsable de Compras ]
```

Cada capa tiene una única responsabilidad y se comunica con la siguiente mediante un contrato explícito (interfaz o esquema de datos), nunca mediante conocimiento implícito de la implementación interna de la capa vecina.

## 2.3 Arquitectura Hexagonal aplicada a InventoryIQ

La Arquitectura Hexagonal divide el sistema en tres grandes anillos:

### 2.3.1 Núcleo de Dominio (Domain Layer)

Es el corazón del sistema. Contiene:

- **Entidades de dominio:** Producto, Inventario, Proveedor, Categoría, Sucursal, MovimientoDeStock, RecomendacionDeCompra.
- **Objetos de valor (Value Objects):** PuntoDePedido, StockDeSeguridad, LeadTime, NivelDeCriticidad, CantidadRecomendada.
- **Servicios de dominio:** lógica de cálculo pura (por ejemplo, `CalculadorDePuntoDePedido`, `EvaluadorDeCriticidad`) que no depende de infraestructura.
- **Casos de uso (Application Services / Use Cases):** orquestan las entidades y servicios de dominio para resolver una necesidad concreta (ver Sección 9).

Esta capa **no importa nada** de Spring, de PostgreSQL, de React ni de CSV. Es Java puro (con anotaciones mínimas si acaso). Esto garantiza que las reglas de negocio se puedan testear de forma aislada y que sean 100% independientes de la tecnología.

### 2.3.2 Puertos (Ports)

Son interfaces que el dominio define para comunicarse hacia afuera. Se dividen en dos tipos:

- **Puertos de entrada (Input Ports / Driving Ports):** definen cómo el mundo exterior invoca al dominio. Ejemplo: `ObtenerProductosCriticosUseCase`, `GenerarRecomendacionesDeCompraUseCase`. Estos puertos son implementados por los casos de uso del dominio y consumidos por los adaptadores de entrada (por ejemplo, los controladores REST).
- **Puertos de salida (Output Ports / Driven Ports):** definen qué necesita el dominio del exterior, sin saber cómo se resuelve. Ejemplo: `ProductoRepository`, `VentaRepository`, `InventarioRepository`, `ProveedorRepository`. El dominio solo conoce estas interfaces; no sabe si detrás hay un CSV, una tabla PostgreSQL o un Data Warehouse en modelo estrella.

### 2.3.3 Adaptadores (Adapters)

Implementan los puertos y conectan el dominio con el mundo real. Se dividen en:

- **Adaptadores de entrada (Driving Adapters):** Controladores REST (Spring Web/MVC) que reciben peticiones HTTP y las traducen a llamadas a los casos de uso. También podrían existir adaptadores de entrada por batch/scheduler (por ejemplo, un job que dispara el recálculo diario de recomendaciones).
- **Adaptadores de salida (Driven Adapters):** Implementaciones concretas de los repositorios.
  - `CsvProductoRepositoryAdapter`, `CsvVentaRepositoryAdapter`, etc. — usados en el MVP, leen y parsean archivos CSV.
  - `PostgresProductoRepositoryAdapter`, etc. — usados en versiones posteriores, consultan el Data Warehouse.

El **cambio de CSV a Data Warehouse implica únicamente reemplazar el adaptador de salida** (o agregar uno nuevo y cambiar la configuración de inyección de dependencias de Spring), sin tocar una sola línea del dominio ni de los casos de uso.

## 2.4 Diagrama textual de capas

```
┌───────────────────────────────────────────────────────────────┐
│                      ADAPTADORES DE ENTRADA                     │
│   Controladores REST (Spring MVC)  |  Scheduler / Jobs batch    │
└───────────────────────────────┬─────────────────────────────────┘
                                  │  implementan / invocan
┌───────────────────────────────▼─────────────────────────────────┐
│                         PUERTOS DE ENTRADA                       │
│  ObtenerProductosCriticosUseCase, GenerarRecomendacionesUseCase  │
│  DetectarSobrestockUseCase, ObtenerDashboardUseCase, etc.        │
└───────────────────────────────┬─────────────────────────────────┘
                                  │
┌───────────────────────────────▼─────────────────────────────────┐
│                        NÚCLEO DE DOMINIO                         │
│  Entidades | Value Objects | Servicios de Dominio | Casos de Uso │
│  (Java puro, sin dependencias de framework ni de infraestructura)│
└───────────────────────────────┬─────────────────────────────────┘
                                  │  usa (a través de interfaces)
┌───────────────────────────────▼─────────────────────────────────┐
│                         PUERTOS DE SALIDA                        │
│   ProductoRepository, VentaRepository, InventarioRepository,     │
│   ProveedorRepository, CompraRepository, MovimientoRepository    │
└───────────────────────────────┬─────────────────────────────────┘
                                  │  implementan
┌───────────────────────────────▼─────────────────────────────────┐
│                     ADAPTADORES DE SALIDA                        │
│  CsvXxxRepositoryAdapter (MVP)  |  PostgresXxxRepositoryAdapter  │
│                     (versión con Data Warehouse)                 │
└───────────────────────────────────────────────────────────────────┘
```

## 2.5 Flujo completo de datos, extremo a extremo

1. **Origen:** el ERP del supermercado genera información transaccional (ventas, compras, movimientos de stock). En el MVP, esta realidad se simula con archivos CSV cargados manualmente o por un endpoint de carga.
2. **ETL (fase posterior):** un proceso en Python + Pandas extrae los datos crudos, los limpia, transforma y valida, y los carga en el Data Warehouse en PostgreSQL con modelo estrella. En el MVP este paso se omite: los CSV ya están "pre-limpios" y simulan la salida de ese ETL.
3. **Persistencia analítica:** el Data Warehouse almacena hechos (ventas, compras, movimientos de stock) y dimensiones (producto, tiempo, sucursal, proveedor, categoría) optimizados para consultas analíticas.
4. **Backend Spring Boot:** los adaptadores de salida leen del Data Warehouse (o de los CSV en el MVP) y los traducen a entidades de dominio. Los casos de uso aplican las reglas de negocio (Sección 4) y generan recomendaciones de compra.
5. **API REST:** expone los resultados —productos críticos, recomendaciones, KPIs, alertas— en formato JSON.
6. **Frontend React:** consume la API y presenta la información en un dashboard orientado a la toma de decisiones.
7. **Usuario:** el responsable de compras interpreta las recomendaciones y ejecuta la orden de compra fuera del sistema (en el ERP real).

## 2.6 Componentes del sistema y responsabilidades

| Componente | Responsabilidad | Tecnología |
|---|---|---|
| Módulo de Ingesta | Leer y parsear archivos CSV / consultar el DW | Java, Spring |
| Núcleo de Dominio | Reglas de negocio, cálculos, casos de uso | Java 21 (puro) |
| API REST | Exponer casos de uso al frontend | Spring Web |
| Persistencia operacional | Guardar snapshots, configuración de reglas, histórico de recomendaciones | PostgreSQL |
| Proceso ETL | Extraer, transformar, limpiar y cargar datos al DW | Python, Pandas |
| Data Warehouse | Almacenar hechos y dimensiones para análisis | PostgreSQL (modelo estrella) |
| Frontend | Visualización y experiencia de usuario | React + TypeScript |
| Orquestación | Levantar todos los servicios de forma reproducible | Docker, Docker Compose |

## 2.7 Justificación arquitectónica

- **Arquitectura Hexagonal + DDD:** permite testear el dominio sin base de datos ni HTTP, y sustituir el origen de datos (requisito explícito del proyecto) sin reescribir lógica de negocio.
- **Separación ETL / Data Warehouse / Backend:** refleja la separación real entre sistemas operacionales (OLTP, propiedad del ERP) y sistemas analíticos (OLAP, propiedad de InventoryIQ), evitando que el backend transaccional se sature con consultas analíticas pesadas.
- **Modelo Estrella en el DW:** optimiza las consultas de agregación (ventas por producto por período, rotación, etc.) que son el insumo principal del motor de recomendación.
- **Docker Compose:** garantiza que el entorno completo (backend, frontend, PostgreSQL, y eventualmente el ETL) sea reproducible en cualquier máquina, requisito clave para un proyecto de portfolio.


---

# 3. Modelo de Negocio

## 3.1 Usuario principal

El **Responsable de Compras** es el usuario para quien se diseña todo el sistema. Es quien decide qué, cuándo y cuánto reponer, y quien sufre directamente las consecuencias de un stockout (venta perdida, cliente insatisfecho) o de un sobrestock (capital inmovilizado, merma).

## 3.2 Stakeholders

| Stakeholder | Interés en el sistema |
|---|---|
| Responsable de Compras | Herramienta diaria de trabajo; reduce carga cognitiva y errores de criterio |
| Gerencia de Sucursal / Operaciones | Reducción de quiebres de stock y de capital inmovilizado; mejora de indicadores financieros |
| Proveedores (indirecto) | Pedidos más predecibles y mejor planificados |
| Equipo de IT / Datos | Punto de entrada limpio para futuras integraciones ETL sin reescribir el sistema |
| Cliente final (indirecto) | Mayor disponibilidad de producto en góndola |

## 3.3 Beneficios esperados

- **Reducción de quiebres de stock:** al anticipar la necesidad de reposición antes de que el stock llegue a cero.
- **Reducción de capital inmovilizado:** al detectar sobrestock y productos de baja rotación.
- **Estandarización del criterio de compra:** las decisiones dejan de depender exclusivamente de la experiencia individual del comprador y se apoyan en datos.
- **Priorización clara:** el comprador sabe qué atender primero (productos críticos) frente a decenas o cientos de SKUs.
- **Trazabilidad:** cada recomendación queda respaldada por una fórmula y unos datos de entrada auditables.

## 3.4 KPIs del producto

| KPI | Definición | Objetivo |
|---|---|---|
| Tasa de quiebre de stock | % de SKUs con stock = 0 sobre el total activo | Minimizar |
| Días de cobertura promedio | Stock actual / venta promedio diaria, promediado sobre el catálogo | Mantener dentro de un rango objetivo (ni muy bajo ni muy alto) |
| Capital inmovilizado en sobrestock | Suma del valor de inventario de productos clasificados como sobrestock | Minimizar |
| % de recomendaciones seguidas | De las recomendaciones generadas, cuántas fueron ejecutadas por el comprador | Maximizar (mide confianza en el sistema) |
| Rotación promedio del inventario | Costo de mercadería vendida / inventario promedio, en un período | Maximizar dentro de rangos saludables por categoría |
| Tiempo de detección de riesgo de quiebre | Días de anticipación entre la alerta y el quiebre real | Maximizar (cuanto antes se detecte, mejor) |

Estos KPIs, aunque el sistema no gestiona ventas ni finanzas, se calculan **a partir de los datos de inventario, ventas históricas y compras** que sí forman parte del alcance.


---

# 4. Reglas de Negocio

Esta sección define, con fórmula y justificación, todas las reglas de negocio que alimentan el motor de recomendación de InventoryIQ. Estas reglas son el contrato que el equipo de desarrollo debe implementar en el núcleo de dominio (capa de servicios de dominio), independientemente del origen de los datos.

## 4.1 Venta promedio (Average Daily Sales — ADS)

**Definición:** cantidad promedio de unidades vendidas por día de un producto, en un período de referencia (por ejemplo, los últimos 30, 60 o 90 días).

**Fórmula:**

```
ADS = Σ(unidades vendidas en el período) / número de días del período
```

**Justificación:** es la base de casi todos los demás cálculos (punto de pedido, stock de seguridad, cobertura). Debe excluir días en los que el producto estuvo en quiebre de stock (ver regla 4.9), ya que de lo contrario subestimaría la demanda real.

**Variantes:**
- ADS simple (todos los días pesan igual).
- ADS ponderado (los días más recientes pesan más, ej. media móvil exponencial) — se documenta como estrategia avanzada en la Sección 11.

## 4.2 Lead Time (Tiempo de reposición)

**Definición:** cantidad de días que transcurren desde que se genera una orden de compra a un proveedor hasta que la mercadería está disponible para la venta.

**Fórmula:** no es una fórmula calculada por el sistema en el MVP; es un dato maestro por producto/proveedor (`lead_time_dias`), aunque puede refinarse históricamente:

```
Lead Time Real = fecha de recepción - fecha de emisión de la orden de compra
Lead Time Promedio = promedio de Lead Time Real de las últimas N compras del par producto-proveedor
```

**Justificación:** determina con cuánta anticipación se debe emitir la orden de compra. Un lead time mal calibrado es la causa más común de quiebres de stock, incluso cuando el resto de las reglas están bien definidas.

## 4.3 Stock de Seguridad (Safety Stock)

**Definición:** cantidad mínima de stock que se mantiene como colchón para absorber variabilidad en la demanda o en el lead time.

**Fórmula (método estadístico simplificado):**

```
Stock de Seguridad = Z × σ_demanda × √(Lead Time)
```

Donde:
- `Z` = factor de servicio (nivel de confianza deseado, ej. 1.65 para 95%).
- `σ_demanda` = desvío estándar de la demanda diaria histórica.
- `Lead Time` = en días.

**Fórmula simplificada (para MVP, sin estadística avanzada):**

```
Stock de Seguridad = ADS × Días de Cobertura Extra
```

Donde "Días de Cobertura Extra" es un parámetro configurable por categoría o producto (ej. 3 a 7 días).

**Justificación:** ninguna demanda es perfectamente predecible; el stock de seguridad absorbe picos inesperados de venta o demoras del proveedor sin generar quiebre.

## 4.4 Punto de Pedido (Reorder Point — ROP)

**Definición:** nivel de stock en el cual se debe disparar una nueva orden de compra.

**Fórmula:**

```
Punto de Pedido = (ADS × Lead Time) + Stock de Seguridad
```

**Justificación:** representa el stock necesario para cubrir la demanda durante el tiempo que tarda en llegar la reposición, más el colchón de seguridad. Es el disparador central de toda recomendación de "cuándo comprar".

**Regla de disparo:** cuando `Stock Actual <= Punto de Pedido`, el producto pasa a estado "Requiere Reposición".

## 4.5 Cantidad de Reposición Sugerida (Order Quantity)

**Definición:** cantidad de unidades a pedir cuando se dispara el punto de pedido.

**Fórmula (método de cobertura objetivo):**

```
Cantidad Sugerida = (ADS × Días de Cobertura Objetivo) - Stock Actual - Stock en Tránsito
```

Donde "Días de Cobertura Objetivo" es un parámetro configurable (por ejemplo, cubrir 15 o 30 días de venta).

**Fórmula alternativa (Lote Económico de Pedido / EOQ)**, aplicable si se dispone de costo de pedido y costo de mantenimiento de inventario:

```
EOQ = √( (2 × Demanda Anual × Costo de Pedido) / Costo de Mantenimiento por Unidad )
```

**Justificación:** evita dos errores simétricos: pedir de menos (nuevo quiebre a corto plazo) o pedir de más (sobrestock). El método de cobertura objetivo es más simple e interpretable para un MVP; el EOQ es más preciso pero requiere datos de costos que pueden no estar disponibles inicialmente.

## 4.6 Rotación de Inventario (Inventory Turnover)

**Definición:** número de veces que el inventario se renueva en un período determinado.

**Fórmula:**

```
Rotación = Costo de Mercadería Vendida en el período / Inventario Promedio en el período
```

**Justificación:** una rotación baja indica que el producto permanece mucho tiempo en depósito/góndola sin venderse (candidato a sobrestock o descontinuación); una rotación muy alta puede indicar riesgo de quiebre si no se repone con suficiente frecuencia.

## 4.7 Clasificación de Productos por Criticidad (Curva ABC / XYZ)

**Definición ABC:** clasifica los productos según su contribución al valor de venta o margen.
- **A:** el 20% de los productos que generan aproximadamente el 80% del valor de venta.
- **B:** productos de contribución media.
- **C:** productos de baja contribución.

**Definición XYZ:** clasifica los productos según la variabilidad/previsibilidad de su demanda.
- **X:** demanda estable y predecible.
- **Y:** demanda con variabilidad moderada (estacional).
- **Z:** demanda errática, difícil de predecir.

**Fórmula de clasificación XYZ (coeficiente de variación):**

```
CV = σ_demanda / ADS
X: CV < 0.5   |   Y: 0.5 <= CV < 1.0   |   Z: CV >= 1.0
```

**Justificación:** cruzar ABC con XYZ (matriz ABC-XYZ) permite priorizar el esfuerzo de gestión: un producto "AX" (alto valor, demanda estable) merece control estricto de punto de pedido; un producto "CZ" (bajo valor, demanda errática) puede gestionarse con reglas simples o incluso descontinuarse.

## 4.8 Detección de Sobrestock

**Definición:** producto cuyo stock actual excede significativamente la demanda proyectada, inmovilizando capital innecesariamente.

**Fórmula (regla de cobertura excesiva):**

```
Días de Cobertura Actual = Stock Actual / ADS
Sobrestock si: Días de Cobertura Actual > Umbral Máximo de Cobertura (parámetro por categoría)
```

**Justificación:** complementa al punto de pedido: mientras el ROP protege contra el quiebre, esta regla protege contra el exceso, cerrando el ciclo completo de gestión de inventario.

## 4.9 Tratamiento de Días con Quiebre de Stock (Censura de Demanda)

**Definición:** si un producto estuvo en stock = 0 durante ciertos días, las "ventas" de esos días no reflejan la demanda real (no es que nadie quisiera comprarlo, es que no había para vender).

**Regla:**

```
ADS corregido = Σ(unidades vendidas en días con stock > 0) / (días del período - días con stock = 0)
```

**Justificación:** ignorar esta corrección subestima sistemáticamente la demanda de los productos que más se quiebran, generando un círculo vicioso de sub-reposición.

## 4.10 Estacionalidad

**Definición:** variación recurrente y predecible de la demanda asociada a una época del año, mes, o evento (ej. Navidad, Día de la Madre, vuelta a clases).

**Fórmula (índice estacional simplificado):**

```
Índice Estacional (mes) = ADS del mes (histórico) / ADS promedio anual
```

Este índice ajusta el ADS proyectado:

```
ADS proyectado = ADS base × Índice Estacional del período objetivo
```

**Justificación:** sin este ajuste, el sistema recomendaría comprar en base al promedio anual, ignorando picos previsibles (ej. sidra y pan dulce en diciembre) o valles (helados en invierno), generando quiebres o sobrestock estacional evitables.

## 4.11 Priorización de Alertas

**Definición:** no todas las alertas de reposición tienen la misma urgencia. Se define un **Nivel de Criticidad** compuesto.

**Fórmula (score de criticidad, 0 a 100):**

```
Score = (Peso_ABC × Valor_ABC) + (Peso_Cobertura × (1 - Días_Cobertura_Restante / Lead_Time)) + (Peso_Quiebre × Indicador_Ya_Quebrado)
```

Donde los pesos son parámetros configurables y `Indicador_Ya_Quebrado` es 1 si el stock actual ya es 0, 0 en caso contrario.

**Justificación:** con catálogos de cientos o miles de SKUs, el responsable de compras necesita un orden de atención, no una lista plana. Los productos clase A, con cobertura restante menor al lead time, o ya quebrados, deben aparecer primero.

## 4.12 Estados posibles de un Producto (máquina de estados de inventario)

| Estado | Condición |
|---|---|
| Normal | Stock Actual > Punto de Pedido y Días de Cobertura <= Umbral Máximo |
| Requiere Reposición | Stock Actual <= Punto de Pedido |
| Crítico | Stock Actual <= Stock de Seguridad, o Stock Actual = 0 |
| Sobrestock | Días de Cobertura Actual > Umbral Máximo de Cobertura |
| Baja Rotación / Descontinuable | Rotación por debajo de un umbral mínimo durante N períodos consecutivos |

Esta máquina de estados es la base de las alertas del dashboard (Sección 10) y del algoritmo de recomendación (Sección 11).


---

# 5. Modelo de Datos Operacional (CSV Simulados)

Esta sección define las tablas/archivos CSV que simulan la fuente de datos del ERP en el MVP. Cada una representa un archivo CSV independiente, con nombre de archivo sugerido entre paréntesis.

## 5.1 Productos (`productos.csv`)

**Descripción:** catálogo maestro de productos (SKUs) del supermercado.

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| producto_id | Entero (PK) | 1001 | Identificador único del producto |
| sku | Texto | "LAC-0234" | Código interno de producto |
| nombre | Texto | "Leche Entera 1L" | Nombre descriptivo |
| categoria_id | Entero (FK) | 12 | Referencia a Categorías |
| proveedor_id | Entero (FK) | 5 | Proveedor principal |
| unidad_medida | Texto | "UN" | Unidad de venta (UN, KG, LT) |
| precio_costo | Decimal | 450.00 | Costo unitario actual |
| precio_venta | Decimal | 690.00 | Precio de venta actual |
| lead_time_dias | Entero | 3 | Días de reposición del proveedor principal |
| activo | Booleano | true | Si el producto está activo en catálogo |

**Relaciones:** N:1 con Categorías, N:1 con Proveedores.

## 5.2 Categorías (`categorias.csv`)

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| categoria_id | Entero (PK) | 12 | Identificador único |
| nombre | Texto | "Lácteos" | Nombre de categoría |
| categoria_padre_id | Entero (FK, nullable) | 3 | Para jerarquía (ej. "Lácteos" dentro de "Almacén Frío") |
| umbral_max_cobertura_dias | Entero | 20 | Parámetro de sobrestock por categoría |
| dias_cobertura_extra_default | Entero | 4 | Parámetro por defecto de stock de seguridad |

**Relaciones:** auto-referencia (jerarquía), 1:N con Productos.

## 5.3 Proveedores (`proveedores.csv`)

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| proveedor_id | Entero (PK) | 5 | Identificador único |
| razon_social | Texto | "Lácteos del Sur S.A." | Nombre del proveedor |
| lead_time_promedio_dias | Entero | 3 | Lead time histórico promedio |
| condicion_pago | Texto | "30 días" | Información contextual |
| activo | Booleano | true | Estado del proveedor |

**Relaciones:** 1:N con Productos, 1:N con Compras.

## 5.4 Sucursales (`sucursales.csv`)

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| sucursal_id | Entero (PK) | 1 | Identificador único |
| nombre | Texto | "Sucursal Centro" | Nombre de la sucursal |
| direccion | Texto | "Av. Siempre Viva 742" | Dirección física |
| activa | Booleano | true | Estado operativo |

**Relaciones:** 1:N con Inventario, Ventas, Compras, Movimientos.

## 5.5 Ventas (`ventas.csv`)

**Descripción:** registro histórico de unidades vendidas por producto, por sucursal, por día (granularidad diaria agregada; no ticket por ticket, ya que InventoryIQ no gestiona el punto de venta).

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| venta_id | Entero (PK) | 500123 | Identificador único del registro |
| fecha | Fecha | 2026-06-15 | Día de la venta |
| producto_id | Entero (FK) | 1001 | Producto vendido |
| sucursal_id | Entero (FK) | 1 | Sucursal donde se vendió |
| unidades_vendidas | Entero | 42 | Cantidad vendida ese día |
| importe_total | Decimal | 28980.00 | Monto total vendido |

**Relaciones:** N:1 con Productos, N:1 con Sucursales.

## 5.6 Compras (`compras.csv`)

**Descripción:** órdenes de compra emitidas a proveedores y su recepción efectiva.

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| compra_id | Entero (PK) | 8001 | Identificador único |
| producto_id | Entero (FK) | 1001 | Producto comprado |
| proveedor_id | Entero (FK) | 5 | Proveedor |
| sucursal_id | Entero (FK) | 1 | Sucursal destino |
| fecha_emision | Fecha | 2026-06-10 | Fecha en que se emitió la orden |
| fecha_recepcion | Fecha (nullable) | 2026-06-13 | Fecha en que se recibió la mercadería |
| unidades_pedidas | Entero | 200 | Cantidad solicitada |
| unidades_recibidas | Entero (nullable) | 198 | Cantidad efectivamente recibida |
| costo_unitario | Decimal | 450.00 | Costo al momento de la compra |
| estado | Texto | "Recibida" | Pendiente / Recibida / Parcial / Cancelada |

**Relaciones:** N:1 con Productos, N:1 con Proveedores, N:1 con Sucursales.

## 5.7 Inventario (`inventario.csv`)

**Descripción:** foto (snapshot) del stock actual por producto y sucursal. En producción esta tabla se actualizaría por eventos de movimiento; en el MVP se simula como snapshot diario.

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| inventario_id | Entero (PK) | 900045 | Identificador único |
| fecha_snapshot | Fecha | 2026-07-29 | Fecha del corte de stock |
| producto_id | Entero (FK) | 1001 | Producto |
| sucursal_id | Entero (FK) | 1 | Sucursal |
| stock_actual | Entero | 35 | Unidades disponibles |
| stock_en_transito | Entero | 0 | Unidades ya compradas, no recibidas |

**Relaciones:** N:1 con Productos, N:1 con Sucursales.

## 5.8 Movimientos de Stock (`movimientos.csv`)

**Descripción:** histórico de entradas y salidas de stock, útil para auditoría y para reconstruir la evolución del inventario (por ejemplo, para detectar días con stock = 0, insumo clave de la regla 4.9).

| Columna | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| movimiento_id | Entero (PK) | 700321 | Identificador único |
| fecha | Fecha | 2026-06-15 | Fecha del movimiento |
| producto_id | Entero (FK) | 1001 | Producto afectado |
| sucursal_id | Entero (FK) | 1 | Sucursal |
| tipo_movimiento | Texto | "Salida por Venta" | Entrada por Compra / Salida por Venta / Ajuste / Merma |
| cantidad | Entero | -42 | Positivo (entrada) o negativo (salida) |
| stock_resultante | Entero | 35 | Stock luego del movimiento |

**Relaciones:** N:1 con Productos, N:1 con Sucursales.

## 5.9 Diagrama relacional textual (modelo operacional)

```
Categorías (1) ──< (N) Productos (N) >── (1) Proveedores
                          │
                          │ (1)
                          │
        ┌─────────────────┼─────────────────┬───────────────────┐
        │ (N)              │ (N)             │ (N)                │ (N)
     Ventas          Inventario          Compras           Movimientos
        │                  │                  │                    │
        └──────────────────┴──────────────────┴────────────────────┘
                                    │ (N)
                                    │
                              Sucursales (1)
```

---

# 6. Diseño del Data Warehouse (Modelo Estrella)

## 6.1 Objetivo del Data Warehouse

El Data Warehouse (DW) es la capa analítica que alimentará al backend en las versiones posteriores al MVP (a partir de v2.0, cuando el ETL real reemplace a los CSV). Está optimizado para consultas agregadas (ventas por producto por mes, rotación por categoría, etc.), no para transacciones.

Se adopta un **modelo estrella (Star Schema)**: una tabla de hechos central rodeada de tablas de dimensión desnormalizadas, priorizando la velocidad de lectura sobre la normalización estricta.

## 6.2 Granularidad

Se definen dos tablas de hechos con distinta granularidad, ya que mezclar granularidades distintas en un mismo hecho es un antipatrón de modelado dimensional:

- **Hecho de Ventas:** un registro por combinación de producto + sucursal + día.
- **Hecho de Inventario (snapshot):** un registro por combinación de producto + sucursal + día (foto de cierre de stock).
- **Hecho de Compras:** un registro por línea de orden de compra (producto + sucursal + proveedor + fecha de emisión).

## 6.3 Tablas de Dimensión

### 6.3.1 `dim_producto`

| Columna | Descripción |
|---|---|
| producto_key (PK sustituta) | Clave técnica autogenerada (surrogate key) |
| producto_id (clave de negocio) | Identificador original del sistema fuente |
| sku | Código interno |
| nombre | Nombre del producto |
| categoria_nombre | Nombre de categoría (desnormalizado) |
| categoria_padre_nombre | Categoría de nivel superior (desnormalizado) |
| unidad_medida | UN / KG / LT |
| clase_abc | A / B / C (calculado y actualizado periódicamente) |
| clase_xyz | X / Y / Z (calculado y actualizado periódicamente) |
| vigente_desde / vigente_hasta | Control de versiones (Slowly Changing Dimension tipo 2) |

**Nota sobre SCD:** si un producto cambia de categoría o de clasificación ABC/XYZ, se conserva el historial mediante Slowly Changing Dimension tipo 2 (nueva fila con rango de vigencia), para que los reportes históricos no se distorsionen retroactivamente.

### 6.3.2 `dim_proveedor`

| Columna | Descripción |
|---|---|
| proveedor_key (PK sustituta) | Clave técnica |
| proveedor_id (clave de negocio) | Identificador original |
| razon_social | Nombre del proveedor |
| lead_time_promedio_dias | Lead time histórico |
| condicion_pago | Información contextual |

### 6.3.3 `dim_sucursal`

| Columna | Descripción |
|---|---|
| sucursal_key (PK sustituta) | Clave técnica |
| sucursal_id (clave de negocio) | Identificador original |
| nombre | Nombre de la sucursal |
| direccion | Dirección física |

### 6.3.4 `dim_tiempo`

| Columna | Descripción |
|---|---|
| fecha_key (PK, formato AAAAMMDD) | Clave técnica basada en la fecha |
| fecha | Fecha calendario |
| año, mes, dia | Componentes de fecha |
| nombre_mes | "Junio" |
| dia_semana | "Lunes" |
| es_fin_de_semana | Booleano |
| temporada | "Navidad", "Vuelta a Clases", "Normal" (para reglas de estacionalidad) |

## 6.4 Tablas de Hechos

### 6.4.1 `fact_ventas`

**Granularidad:** una fila por producto, por sucursal, por día.

| Columna | Tipo | Descripción |
|---|---|---|
| producto_key (FK) | Entero | Referencia a dim_producto |
| sucursal_key (FK) | Entero | Referencia a dim_sucursal |
| fecha_key (FK) | Entero | Referencia a dim_tiempo |
| unidades_vendidas | Medida (Entero) | Cantidad vendida |
| importe_vendido | Medida (Decimal) | Monto vendido |
| tuvo_quiebre_stock | Medida (Booleano) | Si el producto estuvo en stock = 0 ese día (para la regla 4.9) |

### 6.4.2 `fact_inventario`

**Granularidad:** una fila por producto, por sucursal, por día (snapshot).

| Columna | Tipo | Descripción |
|---|---|---|
| producto_key (FK) | Entero | Referencia a dim_producto |
| sucursal_key (FK) | Entero | Referencia a dim_sucursal |
| fecha_key (FK) | Entero | Referencia a dim_tiempo |
| stock_actual | Medida (Entero) | Unidades disponibles al cierre del día |
| stock_en_transito | Medida (Entero) | Unidades compradas no recibidas |
| valor_inventario | Medida (Decimal) | stock_actual × costo unitario vigente |

### 6.4.3 `fact_compras`

**Granularidad:** una fila por línea de orden de compra.

| Columna | Tipo | Descripción |
|---|---|---|
| producto_key (FK) | Entero | Referencia a dim_producto |
| proveedor_key (FK) | Entero | Referencia a dim_proveedor |
| sucursal_key (FK) | Entero | Referencia a dim_sucursal |
| fecha_emision_key (FK) | Entero | Referencia a dim_tiempo (emisión) |
| fecha_recepcion_key (FK, nullable) | Entero | Referencia a dim_tiempo (recepción) |
| unidades_pedidas | Medida (Entero) | Cantidad solicitada |
| unidades_recibidas | Medida (Entero) | Cantidad recibida |
| lead_time_real_dias | Medida (Entero) | fecha_recepcion - fecha_emision |
| costo_total | Medida (Decimal) | unidades_recibidas × costo_unitario |

## 6.5 Diagrama textual del modelo estrella

```
                         dim_tiempo
                              │
     dim_producto ──┐         │         ┌── dim_sucursal
                     │         │         │
                     ▼         ▼         ▼
                 ┌───────────────────────────┐
                 │       fact_ventas          │
                 │  (producto, sucursal, día) │
                 └───────────────────────────┘

                     │         │         │
                     ▼         ▼         ▼
                 ┌───────────────────────────┐
                 │      fact_inventario       │
                 │  (producto, sucursal, día) │
                 └───────────────────────────┘

     dim_producto ──┐         │         ┌── dim_sucursal
                     │         │         │
     dim_proveedor ──┼─────────┼─────────┤
                     ▼         ▼         ▼
                 ┌───────────────────────────┐
                 │        fact_compras        │
                 │ (línea de orden de compra) │
                 └───────────────────────────┘
```

## 6.6 Justificación del modelo elegido

- **Modelo estrella vs. copo de nieve:** se prefiere estrella (dimensiones desnormalizadas) sobre copo de nieve para priorizar la simplicidad y velocidad de las consultas, ya que el volumen de datos de un supermercado individual no justifica la normalización adicional que el copo de nieve aportaría.
- **Separación de hechos por granularidad:** evitar mezclar "ventas diarias" con "líneas de compra" en un mismo hecho previene ambigüedades de agregación (sumar peras con manzanas) y refleja procesos de negocio distintos.
- **SCD tipo 2 en `dim_producto`:** necesario porque la clasificación ABC/XYZ y la categoría de un producto cambian con el tiempo, y los reportes históricos deben reflejar la clasificación vigente en cada momento, no la actual.
- **`dim_tiempo` explícita:** permite incorporar atributos de negocio (temporada, fin de semana) que no se derivan trivialmente de una fecha SQL, y que son clave para la regla de estacionalidad (4.10).


---

# 7. Diseño del Proceso ETL

Esta sección describe el proceso ETL (Extract, Transform, Load) que en versiones posteriores al MVP alimentará el Data Warehouse a partir de los datos crudos del ERP. Se describe a nivel de diseño y flujo, sin código.

## 7.1 Extracción

**Objetivo:** obtener los datos crudos desde el sistema fuente (ERP del supermercado, o los CSV simulados en la fase de transición).

**Fuentes contempladas:**
- Exportaciones periódicas del ERP (CSV, planillas, o consultas directas a su base de datos si el proveedor del ERP lo permite).
- Archivos CSV manuales (fase MVP / de transición).

**Estrategia de extracción:**
- **Incremental:** extraer solo los registros nuevos o modificados desde la última ejecución (por ejemplo, ventas del día anterior), usando una marca de fecha/hora o un número de secuencia como cursor.
- **Full (completa):** para dimensiones de bajo volumen (productos, proveedores, categorías, sucursales) puede optar por una extracción completa en cada corrida, dado su tamaño reducido.

**Consideraciones:** se debe registrar metadata de cada extracción (fecha de ejecución, cantidad de registros extraídos, origen) para trazabilidad.

## 7.2 Transformación

**Objetivo:** convertir los datos crudos al formato requerido por el modelo estrella del Data Warehouse.

**Tareas de transformación:**
- **Mapeo de columnas:** de los nombres/formatos del sistema origen a los nombres estándar del DW.
- **Conversión de tipos:** fechas a formato estándar, montos a decimal con precisión fija, códigos a los tipos esperados.
- **Cálculo de claves sustitutas (surrogate keys):** generación de `producto_key`, `sucursal_key`, etc., independientes de los identificadores del sistema origen.
- **Resolución de dimensión tiempo:** cada fecha se traduce a su `fecha_key` correspondiente en `dim_tiempo`.
- **Cálculo de indicadores derivados:** por ejemplo, `tuvo_quiebre_stock` (booleano derivado de cruzar ventas e inventario), `lead_time_real_dias` (derivado de fechas de emisión y recepción de compras).
- **Aplicación de SCD tipo 2:** al detectar cambios en atributos de `dim_producto` (categoría, clasificación ABC/XYZ), cerrar la vigencia de la fila anterior y crear una nueva.

## 7.3 Limpieza (Data Cleansing)

**Tareas:**
- Eliminación de registros duplicados (mismo producto + sucursal + fecha en ventas, por ejemplo).
- Normalización de textos (mayúsculas/minúsculas, espacios, tildes) para evitar que "Lácteos" y "lacteos" se traten como categorías distintas.
- Relleno o marcado explícito de valores faltantes (por ejemplo, `unidades_recibidas` nulo en una compra pendiente no debe interpretarse como cero).
- Detección de outliers evidentes (por ejemplo, una venta de 100.000 unidades de un producto que históricamente vende 10 unidades/día) para marcarlos como sospechosos, sin eliminarlos automáticamente.

## 7.4 Validaciones

**Validaciones estructurales:**
- Presencia de todas las columnas esperadas en cada archivo/fuente.
- Tipos de datos correctos (fechas parseables, montos numéricos).

**Validaciones de integridad referencial:**
- Todo `producto_id` referenciado en ventas/compras/inventario debe existir en el maestro de productos.
- Todo `proveedor_id` y `sucursal_id` referenciado debe existir en sus respectivos maestros.

**Validaciones de negocio:**
- `unidades_vendidas >= 0`.
- `fecha_recepcion >= fecha_emision` en compras.
- `stock_actual >= 0` (o marcado explícitamente como ajuste negativo si se permite backorder).

**Política ante fallos de validación:** los registros que no pasan validación se apartan a una zona de "cuarentena" (rechazados) con el motivo del rechazo, sin detener el proceso completo, salvo que el porcentaje de registros rechazados supere un umbral crítico configurable (por ejemplo, 5% del lote), en cuyo caso el proceso se detiene y notifica.

## 7.5 Normalización

- Unificación de unidades de medida (por ejemplo, si un producto se reporta a veces en KG y otras en gramos, normalizar a una única unidad base).
- Unificación de monedas (si aplica, en supermercados con productos importados).
- Unificación de zona horaria en las fechas/horas de eventos.

## 7.6 Carga (Load)

**Estrategia:** carga incremental en las tablas de hechos (`fact_ventas`, `fact_inventario`, `fact_compras`), agregando nuevas particiones por fecha sin reprocesar el histórico completo.

**Estrategia para dimensiones:** actualización tipo *upsert* (insertar si es nuevo, actualizar o versionar con SCD tipo 2 si cambió un atributo relevante).

**Particionamiento sugerido:** particionar las tablas de hechos por rango de fecha (por ejemplo, por mes) para mantener el rendimiento de las consultas a medida que el histórico crece.

**Idempotencia:** cada corrida del ETL debe poder re-ejecutarse sin duplicar datos (por ejemplo, mediante claves naturales únicas o borrado-e-inserción del período afectado antes de la carga).

## 7.7 Calidad de Datos

Se define un conjunto de métricas de calidad de datos monitoreadas en cada corrida:

| Métrica | Descripción |
|---|---|
| % de registros rechazados | Registros que no pasaron validación / total de registros del lote |
| % de completitud | Proporción de campos no nulos en campos obligatorios |
| % de duplicados detectados | Registros duplicados eliminados sobre el total |
| Desvío de volumen | Diferencia porcentual entre el volumen de registros de la corrida actual y el promedio histórico (para detectar cargas incompletas o duplicadas) |

## 7.8 Manejo de Errores

- **Errores recuperables** (ej. timeout de conexión a la fuente): reintento automático con backoff exponencial, hasta un máximo de intentos configurable.
- **Errores no recuperables** (ej. archivo fuente corrupto o ausente): el proceso se detiene, se registra el error y se notifica al administrador del sistema.
- **Errores de validación por registro:** no detienen el proceso; el registro se envía a cuarentena (ver 7.4).

## 7.9 Logs y Trazabilidad

Cada ejecución del ETL debe registrar, como mínimo:

- Identificador único de ejecución (run_id).
- Fecha y hora de inicio y fin.
- Fuente(s) procesadas y cantidad de registros leídos, transformados, rechazados y cargados.
- Duración total y por etapa (extracción, transformación, carga).
- Estado final (éxito, éxito con advertencias, fallo).

Esta información permite auditar el pipeline y diagnosticar rápidamente cualquier discrepancia entre el ERP fuente y el Data Warehouse.

## 7.10 Versionado del Proceso ETL

- El propio proceso ETL debe versionarse (control de versiones en Git), de forma que cada corrida quede asociada a una versión específica del código de transformación.
- Los cambios en las reglas de transformación (por ejemplo, un nuevo cálculo de `tuvo_quiebre_stock`) deben documentarse y, de ser necesario, dispararse un reprocesamiento del histórico afectado.
- Se recomienda mantener un registro de "esquema de destino" versionado (versión del modelo estrella), de forma que cambios estructurales en el DW queden desacoplados de cambios en la lógica de transformación.


---

# 8. API Funcional (Diseño de Endpoints REST)

Todos los endpoints se exponen bajo el prefijo `/api/v1`. El formato de intercambio es JSON. Se documenta el objetivo, request, response conceptual, casos de uso y errores de cada uno, sin definir código ni contratos formales (OpenAPI se generaría en una fase de implementación).

## 8.1 `GET /api/v1/productos`

**Objetivo:** listar el catálogo de productos, con filtros opcionales.

**Request (query params):** `categoria_id`, `proveedor_id`, `sucursal_id`, `estado` (Normal/Requiere Reposición/Crítico/Sobrestock), `pagina`, `tamano_pagina`.

**Response:** listado paginado de productos con su información maestra y estado actual de inventario resumido (stock actual, punto de pedido, estado).

**Casos de uso:** pantalla de catálogo/listado general; filtro por categoría en el dashboard.

**Errores:** 400 si los parámetros de filtro son inválidos (ej. `categoria_id` no numérico); 404 no aplica (lista vacía es una respuesta válida).

## 8.2 `GET /api/v1/productos/{productoId}`

**Objetivo:** obtener el detalle completo de un producto, incluyendo histórico reciente de ventas, stock y compras.

**Request:** `productoId` en el path; `sucursal_id` opcional en query.

**Response:** ficha del producto (datos maestros), indicadores calculados (ADS, punto de pedido, stock de seguridad, clasificación ABC/XYZ, estado), y series históricas resumidas (últimos 90 días de ventas y stock).

**Casos de uso:** pantalla de detalle de producto, drill-down desde una alerta.

**Errores:** 404 si el producto no existe.

## 8.3 `GET /api/v1/productos/criticos`

**Objetivo:** listar productos en estado "Crítico" o "Requiere Reposición", ordenados por score de criticidad (regla 4.11).

**Request (query params):** `sucursal_id`, `categoria_id`, `limite`.

**Response:** listado priorizado con producto, stock actual, punto de pedido, días de cobertura restante, score de criticidad.

**Casos de uso:** pantalla principal del dashboard ("qué comprar hoy").

**Errores:** 400 por parámetros inválidos.

## 8.4 `GET /api/v1/productos/sobrestock`

**Objetivo:** listar productos clasificados como sobrestock.

**Request (query params):** `sucursal_id`, `categoria_id`, `orden` (por valor inmovilizado, por días de cobertura).

**Response:** listado con producto, stock actual, días de cobertura actual, valor de inventario inmovilizado.

**Casos de uso:** pantalla de sobrestock del dashboard.

**Errores:** 400 por parámetros inválidos.

## 8.5 `GET /api/v1/recomendaciones`

**Objetivo:** obtener el listado de recomendaciones de compra vigentes (qué, cuándo, cuánto).

**Request (query params):** `sucursal_id`, `categoria_id`, `proveedor_id`, `estado` (pendiente/aplicada/descartada).

**Response:** listado de recomendaciones con producto, cantidad sugerida, fecha límite recomendada para emitir la orden, proveedor sugerido, justificación (fórmula/datos base resumidos).

**Casos de uso:** pantalla de recomendaciones, exportación a lista de compras.

**Errores:** 400 por parámetros inválidos.

## 8.6 `POST /api/v1/recomendaciones/generar`

**Objetivo:** disparar manualmente el recálculo de recomendaciones (además del recálculo automático programado).

**Request (body):** `sucursal_id` opcional (si se omite, recalcula para todas las sucursales).

**Response:** resumen de la ejecución (cantidad de recomendaciones generadas, nuevas, actualizadas, descartadas por resolución).

**Casos de uso:** botón "Recalcular ahora" en el panel de administración; ejecución tras cargar nuevos CSV.

**Errores:** 409 si ya hay un recálculo en curso; 500 si falla el proceso (con detalle de la etapa fallida).

## 8.7 `PATCH /api/v1/recomendaciones/{recomendacionId}`

**Objetivo:** marcar una recomendación como aplicada o descartada por el usuario, registrando feedback para el KPI "% de recomendaciones seguidas".

**Request (body):** `estado` (aplicada/descartada), `comentario` opcional.

**Response:** recomendación actualizada.

**Casos de uso:** interacción del comprador con la lista de recomendaciones.

**Errores:** 404 si la recomendación no existe; 400 si el estado enviado no es válido.

## 8.8 `GET /api/v1/kpis`

**Objetivo:** obtener los KPIs agregados definidos en la sección 3.4, para un período y alcance determinados.

**Request (query params):** `sucursal_id`, `fecha_desde`, `fecha_hasta`.

**Response:** objeto con tasa de quiebre de stock, días de cobertura promedio, capital inmovilizado en sobrestock, % de recomendaciones seguidas, rotación promedio.

**Casos de uso:** widgets de KPI del dashboard.

**Errores:** 400 si el rango de fechas es inválido (fecha_desde posterior a fecha_hasta).

## 8.9 `GET /api/v1/categorias`

**Objetivo:** listar categorías y sus parámetros de configuración (umbral de sobrestock, cobertura extra por defecto).

**Response:** listado de categorías con jerarquía.

**Casos de uso:** filtros del dashboard, pantalla de configuración de parámetros.

**Nota de implementación:** el endpoint real es `GET /api/v1/categories` (inglés, consistente con el resto de la API implementada) y hoy solo devuelve `categoryId`, `name` y `parentCategoryId` — sin los parámetros de cobertura que describe este punto, que siguen viviendo solo en `categorias.csv`/`CategoryRepository` sin exponerse vía API. Ver `docs/InventoryIQ_Arquitectura.md` Sección 1.3.

## 8.10 `PATCH /api/v1/categorias/{categoriaId}/parametros`

**Objetivo:** ajustar los parámetros de negocio configurables por categoría (umbral máximo de cobertura, días de cobertura extra, pesos del score de criticidad si se gestionan a este nivel).

**Request (body):** parámetros a modificar.

**Response:** categoría actualizada.

**Casos de uso:** pantalla de configuración/administración.

**Errores:** 404 si la categoría no existe; 400 si algún parámetro está fuera de rango válido (ej. umbral negativo).

**Estado: PLANIFICADO.** No implementado — las categorías son de solo lectura (ver 8.9).

## 8.11 `GET /api/v1/suppliers` y `PATCH /api/v1/suppliers/{supplierId}/lead-time`

**Objetivo:** listar proveedores activos y corregir a mano el lead time promedio de uno de ellos.

**Nota de implementación:** difiere del diseño original (que proponía `GET /api/v1/proveedores` de solo lectura, con lead time histórico y productos asociados). En el proceso real relevado con el usuario, la coordinación con proveedores se hace por WhatsApp, sin ningún sistema — no hay de dónde importar el catálogo de proveedores ni su lead time automáticamente, y tampoco existe un historial que listar. `Mantenimiento_de_Proveedores_.csv` (export real del sistema XRP) confirma esto: trae datos fiscales/administrativos pero ningún campo de lead time. Por eso el endpoint de solo lectura planificado se reemplazó por un par lectura+corrección manual — mismo criterio que el conteo físico de stock (Sección 8.16): cuando el dato no existe en ningún sistema, se carga y corrige a mano en vez de simular una ingesta automática inexistente.

**`GET /api/v1/suppliers` — Response:** listado de proveedores activos (`supplierId`, `businessName`, `leadTimeDays`, `paymentTerms`).

**`PATCH /api/v1/suppliers/{supplierId}/lead-time` — Request (body):** `leadTimeDays` (entero, mayor a 0).

**`PATCH /api/v1/suppliers/{supplierId}/lead-time` — Response:** el proveedor actualizado, misma forma que un elemento del listado.

**Casos de uso:** pantalla de Proveedores (Sección 10.6) — corregir el lead time de un proveedor antes de calcular recomendaciones de compra que dependan de él.

**Errores:** 404 si el proveedor no existe; 400 si `leadTimeDays` no es mayor a 0.

**Estado: IMPLEMENTADO.** `SupplierRepository` (lectura + actualización, sobre `proveedores.csv`), `ListSuppliersUseCase`/`UpdateSupplierLeadTimeUseCase`, `SuppliersController`. A diferencia de `SaleRepository`/`InventoryRepository` (registros históricos inmutables con un puerto de ingestión separado), un proveedor es una entidad mutable — el adaptador CSV reescribe el archivo completo al corregir un lead time, igual que `RecommendationRepository.save()` hace un upsert sobre Postgres.

## 8.12 `GET /api/v1/sucursales`

**Objetivo:** listar sucursales disponibles, usado para poblar filtros en todo el dashboard.

**Nota de implementación:** el endpoint real es `GET /api/v1/stores` (inglés). Devuelve solo las sucursales activas — de las 3 simuladas en `sucursales.csv`, 2 están activas hoy, reflejando que el despliegue real del usuario tiene 2 sucursales, no 3.

## 8.13 `POST /api/v1/ingesta/csv`

**Objetivo:** cargar un nuevo archivo CSV (ventas, compras, inventario, etc.) para su procesamiento por el adaptador de ingesta del MVP.

**Request:** archivo multipart + tipo de archivo (`ventas`, `compras`, `inventario`, `productos`, etc.).

**Response:** resumen de la ingesta (registros leídos, aceptados, rechazados), similar en espíritu al log del ETL (sección 7.9), aplicado a la escala del MVP.

**Casos de uso:** panel de administración para simular la llegada de nuevos datos sin depender aún del ETL real.

**Errores:** 400 si el archivo no respeta el esquema esperado; 422 si el contenido no pasa las validaciones de negocio mínimas.

## 8.14 `GET /api/v1/alertas`

**Objetivo:** listar alertas activas (derivadas de los estados "Crítico" y "Sobrestock"), pensadas para una futura integración con notificaciones (mail, etc.), aunque el envío efectivo esté fuera de alcance del MVP.

**Request (query params):** `sucursal_id`, `tipo` (quiebre/sobrestock), `severidad`.

**Response:** listado de alertas con producto, tipo, severidad, fecha de generación.

**Casos de uso:** widget de alertas del dashboard.

## 8.15 `GET /api/v1/products?q={término}` (no prevista en el diseño original)

**Objetivo:** buscar en el catálogo activo por código (`sku` — un mismo campo de texto sirve tanto para un código de barras real como para un código interno corto, por ejemplo el de un producto de fiambrería vendido por peso) o por nombre, sin distinguir mayúsculas.

**Origen:** no estaba prevista en esta sección; surgió de una necesidad real detectada durante el desarrollo — un selector de producto que no obligue a quien carga un dato a memorizar un `productId` numérico. La usa la pantalla de conteo de stock (ver 8.16).

**Request (query params):** `q` (obligatorio, no puede ser vacío ni estar en blanco).

**Response:** listado de productos coincidentes (`productId`, `sku`, `name`, `categoryId`).

**Errores:** 400 si `q` está vacío o en blanco.

## 8.16 `POST /api/v1/inventory-snapshots` (no prevista en el diseño original)

**Objetivo:** registrar un conteo físico manual de stock como un nuevo snapshot de inventario, fechado hoy.

**Origen:** el diseño original (Sección 7) asume que el inventario se actualiza vía un ETL automatizado desde el ERP/POS. En el proceso real relevado con el usuario, el export de stock del POS llega desactualizado — no se carga a tiempo cuando entra mercadería de un proveedor — así que el conteo físico manual, hecho antes de emitir un pedido, es la fuente confiable real. Este endpoint no reemplaza al ETL planificado (Sección 7); cubre la necesidad inmediata mientras ese proceso no exista.

**Request (body):** `productId`, `storeId`, `stockActual` (entero, 0 o más).

**Response:** el snapshot creado (`inventoryId`, `snapshotDate`, `productId`, `storeId`, `currentStock`, `stockInTransit`).

**Casos de uso:** pantalla de Administración → "Conteo de stock": buscar el producto (8.15), cargar la cantidad contada, guardar, repetir para el siguiente producto de la misma recorrida por el depósito.

**Errores:** 404 si el producto o la sucursal no existen; 400 si `stockActual` es negativo.

**Limitación conocida:** `stockInTransit` siempre se persiste en `0` — un conteo físico no puede relevar "lo que está en camino" de un proveedor.

---

# 9. Casos de Uso del Backend

Cada caso de uso se implementa como un servicio de aplicación en el núcleo de dominio, invocado por los adaptadores de entrada (controladores REST o jobs programados) a través de los puertos de entrada.

## 9.1 `ObtenerProductosCriticosUseCase`

- **Objetivo:** devolver el listado de productos en estado Crítico o Requiere Reposición, ordenado por score de criticidad.
- **Entradas:** filtros opcionales (sucursal, categoría), límite de resultados.
- **Salidas:** lista de productos con su estado, stock actual, punto de pedido, días de cobertura restante y score.
- **Algoritmo:** para cada producto activo en el alcance filtrado, calcular ADS corregido (regla 4.9), punto de pedido (4.4), comparar contra stock actual, determinar estado (4.12), calcular score de criticidad (4.11) si corresponde, ordenar descendente por score.
- **Dependencias:** `ProductoRepository`, `VentaRepository`, `InventarioRepository`, servicio de dominio `CalculadorDePuntoDePedido`, servicio de dominio `EvaluadorDeCriticidad`.

## 9.2 `CalculateReorderSuggestions` (Calcular Recomendaciones de Compra)

- **Objetivo:** generar, para cada producto que dispara el punto de pedido, una recomendación de compra con cantidad sugerida y fecha límite de emisión de orden.
- **Entradas:** alcance (sucursal opcional), parámetros de negocio vigentes por categoría/producto (días de cobertura objetivo, etc.).
- **Salidas:** lista de recomendaciones (producto, cantidad sugerida, proveedor, fecha límite, justificación).
- **Algoritmo:** para cada producto con `Stock Actual <= Punto de Pedido`, calcular cantidad sugerida según la regla 4.5 (método de cobertura objetivo, con EOQ como estrategia alternativa configurable), determinar proveedor sugerido (principal, o el de menor lead time si hay más de uno activo), calcular fecha límite de emisión = fecha en la que el stock proyectado alcanzaría el stock de seguridad.
- **Dependencias:** `ProductoRepository`, `InventarioRepository`, `CompraRepository` (para stock en tránsito), `ProveedorRepository`, servicio de dominio `CalculadorDeCantidadDeReposicion`.

## 9.3 `DetectOverstock` (Detectar Sobrestock)

- **Objetivo:** identificar productos con cobertura de stock excesiva.
- **Entradas:** alcance (sucursal, categoría opcionales).
- **Salidas:** lista de productos en estado Sobrestock, con días de cobertura actual y valor de inventario inmovilizado.
- **Algoritmo:** aplicar la regla 4.8 comparando días de cobertura actual contra el umbral configurado por categoría; calcular valor inmovilizado = stock actual × costo unitario.
- **Dependencias:** `ProductoRepository`, `InventarioRepository`, `VentaRepository` (para ADS), `CategoriaRepository` (para umbrales).

## 9.4 `ForecastDemand` (Proyectar Demanda)

- **Objetivo:** proyectar la demanda esperada de un producto para un horizonte futuro, incorporando estacionalidad.
- **Entradas:** producto, horizonte temporal (días), sucursal opcional.
- **Salidas:** demanda proyectada por período (por ejemplo, por semana dentro del horizonte).
- **Algoritmo:** calcular ADS base corregido (regla 4.9), aplicar índice estacional correspondiente al período proyectado (regla 4.10), devolver la serie proyectada. En el MVP puede limitarse a un promedio móvil simple; el detalle de estrategias alternativas se documenta en la Sección 11.
- **Dependencias:** `VentaRepository`, servicio de dominio `CalculadorDeEstacionalidad`.

## 9.5 `ClassifyProductsABCXYZ` (Clasificar Productos)

- **Objetivo:** calcular periódicamente la clasificación ABC (por contribución de venta) y XYZ (por variabilidad de demanda) de cada producto.
- **Entradas:** alcance (todos los productos activos), período de análisis (por ejemplo, últimos 12 meses).
- **Salidas:** clasificación ABC y XYZ por producto, persistida para uso en otros casos de uso (score de criticidad, priorización de alertas).
- **Algoritmo:** ordenar productos por valor de venta acumulado y aplicar el corte 80/20 (regla 4.7) para ABC; calcular coeficiente de variación de la demanda diaria para XYZ.
- **Dependencias:** `VentaRepository`, `ProductoRepository`.

## 9.6 `GenerateAlerts` (Generar Alertas)

- **Objetivo:** consolidar en un formato de alerta los productos en estado Crítico y Sobrestock, para su visualización o futura notificación.
- **Entradas:** alcance (sucursal opcional).
- **Salidas:** lista de alertas (producto, tipo, severidad, fecha de generación).
- **Algoritmo:** invocar `ObtenerProductosCriticosUseCase` y `DetectOverstock`, mapear sus resultados al formato de alerta, asignar severidad según el score de criticidad o la magnitud del sobrestock.
- **Dependencias:** `ObtenerProductosCriticosUseCase`, `DetectOverstock`.

## 9.7 `CalculateInventoryKPIs` (Calcular KPIs de Inventario)

- **Objetivo:** calcular los KPIs definidos en la sección 3.4 para un período y alcance determinados.
- **Entradas:** alcance (sucursal opcional), rango de fechas.
- **Salidas:** objeto con los valores de cada KPI.
- **Algoritmo:** aplicar las fórmulas correspondientes (tasa de quiebre, días de cobertura promedio, capital inmovilizado, % de recomendaciones seguidas, rotación promedio) sobre el alcance y período indicados.
- **Dependencias:** `VentaRepository`, `InventarioRepository`, `RecomendacionRepository`, `CompraRepository`.

## 9.8 `RegisterRecommendationFeedback` (Registrar Feedback de Recomendación)

- **Objetivo:** registrar si el usuario aplicó o descartó una recomendación de compra, insumo del KPI "% de recomendaciones seguidas".
- **Entradas:** identificador de recomendación, nuevo estado, comentario opcional.
- **Salidas:** recomendación actualizada.
- **Algoritmo:** validar que la recomendación exista y que la transición de estado sea válida; persistir el cambio.
- **Dependencias:** `RecomendacionRepository`.

## 9.9 `IngestCsvFile` (Ingerir Archivo CSV)

- **Objetivo:** procesar un archivo CSV cargado (ventas, compras, inventario, productos, etc.), validarlo y persistirlo, reemplazando en el MVP la función que en producción cumplirá el ETL.
- **Entradas:** archivo, tipo de archivo.
- **Salidas:** resumen de la ingesta (registros leídos, aceptados, rechazados con motivo).
- **Algoritmo:** aplicar el subconjunto de validaciones estructurales y de negocio descriptas en la Sección 7 (adaptadas a la escala del MVP), persistir los registros válidos mediante el adaptador de salida correspondiente, reportar los rechazados.
- **Dependencias:** el repositorio de salida correspondiente al tipo de archivo (`ProductoRepository`, `VentaRepository`, etc.).

## 9.10 `RecalculateProductStatus` (Recalcular Estado de Productos — job programado)

- **Objetivo:** recorrer el catálogo activo y actualizar el estado de inventario (Normal/Requiere Reposición/Crítico/Sobrestock) de cada producto, disparando en cadena la generación de recomendaciones y alertas.
- **Entradas:** ninguna (job programado, ej. diario), o alcance específico si se dispara manualmente.
- **Salidas:** ninguna directa; efectos: actualización de estados persistidos, nuevas recomendaciones y alertas.
- **Algoritmo:** orquesta, en secuencia, `ObtenerProductosCriticosUseCase`, `DetectOverstock`, `CalculateReorderSuggestions` y `GenerateAlerts` sobre el catálogo completo.
- **Dependencias:** todos los casos de uso anteriores.


---

# 10. Dashboard (Diseño Funcional de Pantallas)

## 10.1 Pantalla: Inicio / Resumen General

**Objetivo:** dar una fotografía instantánea del estado del inventario apenas el usuario ingresa.

**Widgets/KPIs:**
- Cantidad de productos en estado Crítico.
- Cantidad de productos con recomendación de compra pendiente.
- Capital inmovilizado en sobrestock (monto).
- Tasa de quiebre de stock del período.
- Días de cobertura promedio del catálogo.

**Gráficos:**
- Gráfico de barras: distribución de productos por estado (Normal / Requiere Reposición / Crítico / Sobrestock).
- Gráfico de línea: evolución de la tasa de quiebre de stock en las últimas semanas.

**Filtros:** sucursal, categoría, rango de fechas.

**Experiencia de usuario:** es la pantalla de aterrizaje; prioriza urgencia (lo crítico arriba) y da acceso directo (clic en un KPI) a la pantalla de detalle correspondiente.

## 10.2 Pantalla: Productos Críticos / A Reponer

**Objetivo:** el corazón operativo del sistema — la lista que el responsable de compras revisa todos los días.

**Tabla principal:** producto, categoría, stock actual, punto de pedido, días de cobertura restante, score de criticidad, estado, cantidad sugerida.

**Filtros:** sucursal, categoría, proveedor, clasificación ABC/XYZ, estado.

**Alertas visuales:** codificación por color según severidad (rojo = ya en quiebre, naranja = crítico, amarillo = requiere reposición próxima).

**Acciones:** marcar recomendación como "aplicada" o "descartada" (alimenta el KPI de seguimiento); exportar a lista de compras.

**Experiencia de usuario:** ordenado por defecto según score de criticidad descendente; permite ordenar por cualquier columna; búsqueda rápida por nombre/SKU.

## 10.3 Pantalla: Sobrestock

**Objetivo:** visualizar productos con exceso de inventario para decidir acciones (promoción, freno de compra, liquidación).

**Tabla principal:** producto, categoría, stock actual, días de cobertura actual, valor de inventario inmovilizado, rotación.

**Gráficos:** ranking de productos por valor inmovilizado (top 10/20).

**Filtros:** sucursal, categoría, rango de valor inmovilizado.

**Experiencia de usuario:** ordenado por defecto por valor inmovilizado descendente, para priorizar el impacto económico.

## 10.4 Pantalla: Detalle de Producto

**Objetivo:** ficha completa de un producto para análisis profundo.

**Contenido:**
- Datos maestros (nombre, categoría, proveedor, lead time).
- Indicadores calculados: ADS, punto de pedido, stock de seguridad, clasificación ABC/XYZ, estado actual.
- Gráfico de línea: evolución de ventas diarias (últimos 90 días), con marcas de días con quiebre de stock.
- Gráfico de línea: evolución de stock (últimos 90 días), con línea de referencia del punto de pedido.
- Histórico de compras (fecha de emisión, recepción, cantidad, proveedor).
- Recomendación vigente, si existe, con su justificación (fórmula y valores utilizados).

**Experiencia de usuario:** accesible por drill-down desde cualquier listado (críticos, sobrestock, catálogo general).

## 10.5 Pantalla: Categorías y Rotación

**Objetivo:** análisis agregado por categoría, útil para el encargado de sector.

**Widgets:** rotación promedio por categoría, distribución ABC/XYZ dentro de la categoría.

**Gráficos:** gráfico de barras comparando rotación entre categorías; matriz ABC-XYZ como mapa de calor (heatmap) con cantidad de productos en cada celda.

**Filtros:** sucursal, período.

## 10.6 Pantalla: Proveedores

**Objetivo:** visibilidad sobre el desempeño de los proveedores, en particular el cumplimiento de lead time.

**Tabla principal:** proveedor, lead time prometido/histórico promedio, cantidad de productos asociados, órdenes pendientes.

**Gráficos:** comparación de lead time prometido vs. lead time real (por proveedor), para detectar proveedores sistemáticamente incumplidores.

**Nota de implementación:** lo implementado hoy (ver 8.11) es un recorte de esta visión — listado de proveedores activos con edición manual del lead time promedio, sin lead time histórico, órdenes pendientes ni comparación prometido/real (no hay de dónde obtener esos datos, ver 8.11).

## 10.7 Pantalla: Comparativa entre Sucursales (post-MVP, contemplada en el diseño)

**Objetivo:** comparar KPIs de inventario entre distintas sucursales del mismo supermercado.

**Gráficos:** barras comparativas de tasa de quiebre, cobertura promedio y capital inmovilizado por sucursal.

**Nota:** esta pantalla depende del soporte multi-sucursal robusto, previsto para la versión v1.2 del roadmap (Sección 12), aunque el modelo de datos del MVP ya contempla `sucursal_id` en todas las entidades relevantes para no requerir rediseño.

## 10.8 Pantalla: Administración (Carga de Datos y Parámetros)

**Objetivo:** permitir al administrador cargar nuevos archivos CSV y ajustar los parámetros de negocio configurables (umbrales de sobrestock, días de cobertura extra, pesos del score de criticidad).

**Contenido:**
- Formulario de carga de archivos CSV por tipo, con resumen de resultado de la ingesta (aceptados/rechazados).
- Panel de parámetros de negocio por categoría.
- Botón de recálculo manual de recomendaciones.
- Log de las últimas ejecuciones de ingesta/recálculo.

**Experiencia de usuario:** pantalla técnica, separada del flujo diario del comprador, pensada para el rol de administrador del sistema.

## 10.9 Principios generales de UX del dashboard

- **Priorizar la acción sobre la exploración:** la primera pantalla que ve el usuario debe decirle qué hacer, no solo mostrarle datos.
- **Codificación de color consistente:** rojo/naranja/amarillo/verde deben significar siempre lo mismo en toda la aplicación (severidad de estado).
- **Justificación siempre visible:** ninguna recomendación se muestra como una "caja negra"; el usuario debe poder ver, con un clic, los datos y la fórmula que la originaron (transparencia del algoritmo, clave para generar confianza y adopción).
- **Filtros persistentes:** la selección de sucursal/categoría se mantiene al navegar entre pantallas dentro de una misma sesión.


---

# 11. Algoritmo de Recomendación de Compra

## 11.1 Visión general

El motor de recomendación responde a tres preguntas encadenadas para cada producto activo:

1. **¿Necesita reposición?** → se evalúa comparando el stock actual contra el punto de pedido (regla 4.4).
2. **¿Cuándo debe emitirse la orden?** → se calcula la fecha en la que el stock proyectado cruzará el stock de seguridad, dado el ritmo de venta actual.
3. **¿Cuánto debe pedirse?** → se calcula la cantidad sugerida (regla 4.5), considerando stock en tránsito para no sobre-pedir.

El sistema se diseña para soportar **múltiples estrategias de cálculo**, seleccionables por categoría o producto, ya que ningún método único es óptimo para todo el catálogo de un supermercado (los productos de alta rotación se comportan muy distinto a los de baja rotación o alta estacionalidad).

## 11.2 Estrategia 1: Basada en Reglas Fijas (Rule-Based)

**Descripción:** utiliza parámetros fijos configurados manualmente (ej. "siempre mantener 10 días de cobertura", "reponer cuando quedan 5 unidades"), sin cálculo estadístico de por medio.

**Ventajas:**
- Simple de implementar y de explicar al usuario.
- Predecible: el comprador entiende exactamente por qué se generó la recomendación.
- No requiere volumen de datos históricos significativo (útil para productos nuevos, sin historial).

**Desventajas:**
- No se adapta automáticamente a cambios de demanda.
- Requiere mantenimiento manual de los parámetros por parte de un administrador.
- Puede generar sobrestock o quiebres si los parámetros quedan desactualizados.

## 11.3 Estrategia 2: Basada en Ventas Históricas (ADS + Punto de Pedido)

**Descripción:** es la estrategia central del MVP (secciones 4.1 a 4.5): calcula ADS a partir del histórico de ventas, corregido por días de quiebre (regla 4.9), y deriva el punto de pedido y la cantidad sugerida.

**Ventajas:**
- Se adapta automáticamente a la evolución real de la demanda.
- Metodología estándar de la industria (fácil de validar contra literatura de gestión de inventarios).
- Balance razonable entre simplicidad y precisión.

**Desventajas:**
- Requiere un mínimo de historial de ventas confiable (productos nuevos carecen de este dato).
- Sensible a la calidad de los datos de ventas (si el ERP fuente tiene errores, se propagan directamente).
- No captura por sí sola tendencias de crecimiento/caída sostenida ni estacionalidad marcada (requiere combinarse con la Estrategia 4).

## 11.4 Estrategia 3: Basada en Lead Time (Ajuste por Confiabilidad del Proveedor)

**Descripción:** en lugar de usar el lead time nominal/prometido por el proveedor, usa el lead time real histórico (fecha de recepción menos fecha de emisión de órdenes pasadas), y ajusta el stock de seguridad en función de la variabilidad de ese lead time.

**Fórmula complementaria:**

```
Stock de Seguridad Ajustado = Z × ADS × σ_lead_time
```

Donde `σ_lead_time` es el desvío estándar del lead time real histórico del proveedor para ese producto.

**Ventajas:**
- Protege específicamente contra la causa de quiebre más subestimada: proveedores poco confiables (lead time variable), no solo demanda variable.
- Permite comparar objetivamente el desempeño de proveedores alternativos para un mismo producto.

**Desventajas:**
- Requiere un historial de compras suficiente por proveedor/producto para ser estadísticamente significativo.
- Añade complejidad de cálculo y de explicación al usuario final.

## 11.5 Estrategia 4: Basada en Stock de Seguridad Estadístico

**Descripción:** aplica la fórmula estadística completa de stock de seguridad (regla 4.3, fórmula con `Z` y `σ_demanda`), en lugar de la simplificación de "días de cobertura extra".

**Ventajas:**
- Más precisa: calibra el stock de seguridad al nivel de servicio deseado (ej. 95%, 98%) de forma cuantificada.
- Permite diferenciar el nivel de servicio objetivo por clase ABC (mayor nivel de servicio para productos A que para productos C).

**Desventajas:**
- Requiere una base de datos histórica más robusta y limpia.
- Menos intuitiva para el usuario de negocio si no se explica adecuadamente en el dashboard (mitigado por el principio de "justificación siempre visible", sección 10.9).

## 11.6 Estrategia 5: Basada en Estacionalidad

**Descripción:** ajusta el ADS proyectado con el índice estacional (regla 4.10) antes de calcular punto de pedido y cantidad sugerida, anticipando picos y valles previsibles de demanda.

**Ventajas:**
- Evita quiebres en fechas de alta demanda predecible (fiestas, eventos) que un promedio histórico plano no anticiparía.
- Evita sobrestock post-pico (por ejemplo, comprar de más después de Navidad usando el ADS de diciembre sin corregir).

**Desventajas:**
- Requiere al menos un ciclo estacional completo de historial (idealmente 2-3 años) para calibrar índices confiables.
- Productos nuevos o categorías nuevas no tienen historial estacional propio (se puede mitigar usando el índice estacional de la categoría como proxy).

## 11.7 Comparación de estrategias

| Estrategia | Precisión | Complejidad | Datos requeridos | Mejor uso |
|---|---|---|---|---|
| Reglas fijas | Baja | Muy baja | Ninguno / mínimo | Productos nuevos, catálogos pequeños, arranque del sistema |
| Ventas históricas (ADS + ROP) | Media-Alta | Media | 30-90 días de ventas | Estrategia por defecto para la mayoría del catálogo |
| Ajuste por lead time real | Media-Alta | Media-Alta | Historial de compras por proveedor | Productos con proveedores poco confiables o lead time largo |
| Stock de seguridad estadístico | Alta | Alta | Historial extenso y limpio | Productos clase A (alto valor, requieren precisión) |
| Estacionalidad | Alta (en su ventana) | Alta | 2-3 ciclos estacionales | Categorías con demanda marcadamente estacional |

## 11.8 Estrategia combinada recomendada (visión de diseño)

El diseño de InventoryIQ no obliga a elegir una única estrategia global. Se recomienda una **asignación de estrategia por clasificación ABC-XYZ** (cruce definido en la regla 4.7):

- **AX (alto valor, demanda estable):** ventas históricas + stock de seguridad estadístico.
- **AY / AZ (alto valor, demanda variable/errática):** ventas históricas + ajuste por lead time real + estacionalidad si aplica.
- **BX / BY:** ventas históricas + punto de pedido estándar (estrategia por defecto).
- **CZ (bajo valor, demanda errática):** reglas fijas simples, priorizando bajo esfuerzo de mantenimiento sobre precisión, dado su bajo impacto económico.

Esta asignación es **configurable**, no está *hardcodeada*: se define como parámetro en el modelo de datos (por ejemplo, a nivel de categoría o de clasificación ABC/XYZ), permitiendo su evolución sin cambios estructurales, en línea con el principio arquitectónico de desacoplar reglas de negocio de su implementación técnica.

## 11.9 Evolución futura (fuera del MVP, documentado como visión)

Como estrategia futura (v3.0 del roadmap), se contempla incorporar modelos de forecasting estadístico más sofisticados (por ejemplo, suavizado exponencial tipo Holt-Winters para capturar tendencia y estacionalidad simultáneamente, o modelos de Machine Learning supervisado entrenados sobre el histórico del Data Warehouse). Estas técnicas no reemplazan el marco conceptual de esta sección, sino que refinan específicamente el cálculo del ADS proyectado (Estrategia 2 y 5), manteniendo intactas las fórmulas de punto de pedido, stock de seguridad y cantidad sugerida.

---

# 12. Roadmap Evolutivo

## 12.1 MVP

**Objetivo:** validar el flujo completo qué-cuándo-cuánto con datos simulados, con arquitectura ya preparada para el reemplazo de origen de datos.

**Incluye:**
- Ingesta desde archivos CSV simulados (todas las entidades de la Sección 5).
- Núcleo de dominio con las reglas de negocio de la Sección 4 (ADS corregido, punto de pedido, stock de seguridad, cantidad sugerida, clasificación ABC/XYZ, detección de sobrestock, score de criticidad).
- Estrategia de recomendación única (Estrategia 2 — ventas históricas) aplicada a todo el catálogo.
- API REST completa según Sección 8.
- Dashboard con las pantallas: Inicio, Productos Críticos, Sobrestock, Detalle de Producto, Administración.
- Persistencia en PostgreSQL (para configuración, estados calculados e histórico de recomendaciones), aunque el dato transaccional de origen sea CSV.
- Contenerización completa vía Docker Compose (backend, frontend, PostgreSQL).

**Nota de implementación:** el dashboard implementado tiene 8 pantallas, no 5 — sumó Alertas, Recomendaciones y Clasificación ABC/XYZ, originalmente pensadas para v1.1/v1.2 (ver 12.2), porque los casos de uso de backend correspondientes ya estaban implementados y no tenían ninguna pantalla propia. Detalle de Producto además quedó acotado a la proyección de demanda (Sección 9.4), no a la ficha completa que describe 8.2 — ver `docs/InventoryIQ_Arquitectura.md`.

## 12.2 v1.1

**Objetivo:** pulir la experiencia de uso diario y agregar configurabilidad.

**Incorpora:**
- Alertas configurables (umbrales de severidad, filtros guardados).
- Exportación de listados (recomendaciones, sobrestock) a formatos descargables.
- Pantalla de Categorías y Rotación (matriz ABC-XYZ como heatmap).
- Registro de feedback de recomendaciones (aplicada/descartada) y su KPI asociado.
- Mejoras de UX: filtros persistentes entre pantallas, búsqueda rápida.

**Nota de implementación:** el registro de feedback de recomendaciones y una pantalla de Clasificación ABC/XYZ (sin heatmap todavía, tabla simple con badges) ya están implementados — se adelantaron al MVP en vez de esperar a v1.1, porque los casos de uso de backend ya existían sin ninguna pantalla propia. Las alertas también se muestran (pantalla propia), pero sin configurabilidad de umbrales todavía.

## 12.3 v1.2

**Objetivo:** robustecer el soporte multi-sucursal.

**Incorpora:**
- Pantalla de Comparativa entre Sucursales.
- Parámetros de negocio configurables por combinación categoría + sucursal (no solo por categoría global).
- Consolidación de KPIs a nivel cadena (todas las sucursales) además de por sucursal individual.

## 12.4 v2.0

**Objetivo:** reemplazar los CSV simulados por el pipeline real, sin modificar el dominio.

**Incorpora:**
- Implementación completa del proceso ETL (Python + Pandas) según el diseño de la Sección 7.
- Data Warehouse en PostgreSQL con el modelo estrella completo de la Sección 6.
- Nuevo adaptador de salida (`PostgresXxxRepositoryAdapter` sobre el DW) que reemplaza a los adaptadores CSV, validando en la práctica el principio arquitectónico central del proyecto.
- Monitoreo de calidad de datos del ETL (métricas de la sección 7.7) expuesto en la pantalla de Administración.
- Slowly Changing Dimensions (SCD tipo 2) operativas en `dim_producto`.

## 12.5 v3.0

**Objetivo:** incrementar la sofisticación analítica del motor de recomendación.

**Incorpora:**
- Asignación de estrategia de recomendación por clasificación ABC-XYZ (sección 11.8), en lugar de una estrategia única global.
- Forecasting estadístico avanzado (suavizado exponencial con tendencia y estacionalidad) para el cálculo de ADS proyectado.
- Exploración de modelos de Machine Learning supervisado para demanda, entrenados sobre el histórico consolidado en el Data Warehouse.
- Optimización de compras multi-proveedor (selección de proveedor no solo por lead time, sino por costo total, confiabilidad histórica y condiciones comerciales).
- Simulación "what-if" (ej. "¿qué pasaría con la cobertura si cambio el umbral de sobrestock de la categoría X?") como herramienta de apoyo a la configuración de parámetros.

---

*Fin del documento. Esta especificación cubre visión de producto, arquitectura, modelo de negocio, reglas de negocio, modelo de datos operacional, diseño del Data Warehouse, diseño del proceso ETL, API funcional, casos de uso del backend, diseño del dashboard, algoritmo de recomendación de compra y roadmap evolutivo, como base suficiente para iniciar la implementación de InventoryIQ sin necesidad de redefinir requisitos adicionales.*
