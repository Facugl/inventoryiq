# InventoryIQ — Roadmap de Implementación (MVP)

Este roadmap toma el alcance del MVP definido en la documentación (Sección 1.5 y 12.1)
y lo baja a fases ejecutables, en orden, para que puedas ir de cero a un sistema
funcionando de punta a punta. Cada fase tiene un objetivo claro, entregables
concretos y un criterio de "terminado" (Definition of Done) para que sepas cuándo
avanzar a la siguiente.

**Duración estimada total (part-time, proyecto de portfolio):** 8 a 12 semanas.

**Estado general (actualizado):** las Fases 0 a 6 están completas (con algunos entregables puntuales sin cerrar, marcados en cada una) y el proyecto ya avanzó bastante más allá del alcance original de este roadmap — ver `docs/InventoryIQ_Arquitectura.md` para el detalle auditado contra código. La sección "Próximo paso concreto" al final refleja el estado real, no el original.

---

## Fase 0 — Preparación del entorno (ya en curso)

**Objetivo:** tener los datos y el entorno de trabajo listos antes de escribir una línea de dominio.

- [x] Datos CSV simulados generados (`productos`, `categorías`, `proveedores`, `sucursales`, `ventas`, `compras`, `inventario`, `movimientos`). `sucursales.csv` tiene 1 de las 3 sucursales marcada inactiva, para reflejar que el despliegue real del usuario es de 2 sucursales.
- [x] Repositorio Git creado, con estructura de carpetas por capa hexagonal (`domain`, `application`, `adapters/in`, `adapters/out`).
- [x] Docker Compose base con PostgreSQL (aunque el MVP arranque con CSV, la persistencia operacional ya vive en Postgres desde el día 1, según 2.6 y 12.1).
- [x] Decidir versión de Java/Spring Boot y de Node/React, y dejarlas fijadas (`.tool-versions`, `pom.xml`, `package.json`).

**Definition of Done:** `docker compose up` levanta Postgres vacío, el proyecto Spring Boot compila con un endpoint `/health`, y el proyecto React arranca con una pantalla en blanco.

---

## Fase 1 — Núcleo de dominio (sin infraestructura)

**Objetivo:** implementar las reglas de negocio de la Sección 4 como Java puro, testeadas de forma aislada, sin tocar Spring, Postgres ni CSV todavía.

**Entregables:**
- Entidades de dominio: `Producto`, `Inventario`, `Proveedor`, `Categoria`, `Sucursal`, `MovimientoDeStock`, `RecomendacionDeCompra`.
- Value Objects: `PuntoDePedido`, `StockDeSeguridad`, `LeadTime`, `NivelDeCriticidad`, `CantidadRecomendada`.
- Servicios de dominio con las fórmulas 4.1 a 4.11: ADS corregido, stock de seguridad, punto de pedido, cantidad sugerida, rotación, clasificación ABC/XYZ, detección de sobrestock, score de criticidad.
- Máquina de estados de producto (4.12): Normal / Requiere Reposición / Crítico / Sobrestock / Baja Rotación.
- Tests unitarios de cada fórmula, usando casos armados a mano (no dependen de los CSV todavía).

**Por qué primero:** es la parte que realmente diferencia el proyecto y la más fácil de testear sin infraestructura. Si el dominio está mal, ningún adaptador lo va a arreglar después.

**Definition of Done:** suite de tests unitarios en verde, cobertura razonable sobre `domain/`, cero imports de Spring/JPA/CSV en ese paquete.

**Estado: COMPLETADA**, con una diferencia de alcance: `Proveedor` (`Supplier`) y `MovimientoDeStock` (`StockMovement`) sí existen como entidades de dominio, pero sin servicio de dominio ni caso de uso propio — quedaron sin conectar (ver Fase 2). `RecomendacionDeCompra` (`Recommendation`) sí está completa y en uso.

---

## Fase 2 — Puertos y adaptadores CSV (ingesta de datos)

**Objetivo:** conectar el dominio con los datos reales simulados, respetando la arquitectura hexagonal.

**Entregables:**
- Puertos de salida: `ProductoRepository`, `VentaRepository`, `InventarioRepository`, `ProveedorRepository`, `CompraRepository`, `MovimientoRepository`.
- Adaptadores `CsvXxxRepositoryAdapter` que leen y parsean los CSV generados en la Fase 0.
- Validación básica de datos al ingerir (tipos, FKs existentes, fechas válidas) — importante porque en v2.0 esto se reemplaza por el ETL, pero el MVP necesita defenderse de un CSV mal cargado.
- Casos de uso (Application Services) que orquestan dominio + repos: `ObtenerProductosCriticosUseCase`, `GenerarRecomendacionesDeCompraUseCase`, `DetectarSobrestockUseCase`.

**Definition of Done:** con los 8 CSV cargados, podés ejecutar `GenerarRecomendacionesDeCompraUseCase` desde un test de integración y obtener una lista de recomendaciones coherente (podés cruzarla a mano contra 2-3 productos que ya viste en el análisis exploratorio de los CSV).

**Estado: COMPLETADA para 5 de los 6 puertos, más un sexto agregado después de la Fase 0.** `ProductRepository`, `SaleRepository` (+ `SaleIngestionRepository`), `InventoryRepository` (+ `InventoryIngestionRepository`, agregado después para conteos manuales), `CategoryRepository` y `StoreRepository` existen con sus adaptadores CSV. `SupplierRepository` (`ProveedorRepository`) también existe ahora, con lectura y corrección manual de lead time (ver Sección 8.11 de `InventoryIQ_Documentacion.md` y Sección 6 de `InventoryIQ_Arquitectura.md`) — a diferencia de los demás, no es solo lectura: `CsvSupplierRepositoryAdapter` también escribe. `CompraRepository` **no existe** — no hay caso de uso implementado que lo necesite, y en el proceso real del usuario las compras se coordinan por WhatsApp, sin ningún sistema del que importarlas.

---

## Fase 3 — Persistencia operacional en PostgreSQL

**Objetivo:** persistir configuración, estados calculados e histórico de recomendaciones (12.1), aunque el dato transaccional siga viniendo de CSV.

**Entregables:**
- Esquema Postgres para: parámetros de negocio configurables (por categoría), snapshots de recomendaciones generadas, histórico de ejecución de recálculos.
- Migraciones versionadas (Flyway o Liquibase).
- Job/scheduler (o endpoint manual) que dispara el recálculo completo y persiste el resultado.

**Definition of Done:** correr el recálculo dos veces en días distintos deja dos snapshots distintos en Postgres, consultables por fecha.

**Estado: COMPLETADA**, acotada a recomendaciones: la tabla `recommendations` en Postgres (vía Flyway) persiste cada recálculo, con historial consultable por sucursal/estado. No hay una tabla separada de "parámetros de negocio configurables" ni de "histórico de ejecución de recálculos" como entidades propias — los parámetros de categoría siguen viviendo solo en `categorias.csv`.

---

## Fase 4 — API REST

**Objetivo:** exponer los casos de uso al frontend según la Sección 8 del documento.

**Entregables:**
- Controladores REST (adaptadores de entrada) para: listado de productos críticos, listado de sobrestock, detalle de producto, KPIs agregados, alertas.
- Manejo de errores y códigos HTTP consistentes.
- Documentación de la API (OpenAPI/Swagger).

**Definition of Done:** Postman/curl contra cada endpoint devuelve JSON válido y consistente con lo que viste en la Fase 2-3.

**Estado: COMPLETADA y ampliada.** 19 casos de uso expuestos vía REST (ver `docs/InventoryIQ_Arquitectura.md` Tabla 1.3), varios sin endpoint documentado en la Sección 8 original (búsqueda de productos, sucursales, categorías, conteo manual de inventario, clasificación ABC/XYZ, forecast, reorder suggestions, y proveedores). `PATCH /api/v1/categories/{id}/parameters` (Sección 8.10) sí estaba documentado desde el diseño original y ahora también está implementado. Documentación OpenAPI vía springdoc-openapi: Swagger UI interactiva en `/swagger-ui/index.html` y el JSON crudo en `/v3/api-docs`, con cada controller anotado (`@Tag`/`@Operation`) agrupado por área funcional.

---

## Fase 5 — Dashboard (Frontend React)

**Objetivo:** las 5 pantallas del MVP mencionadas en 12.1: Inicio, Productos Críticos, Sobrestock, Detalle de Producto, Administración.

**Entregables:**
- Pantalla de Inicio con KPIs agregados (Sección 3.4).
- Listado de Productos Críticos, ordenado por score de criticidad (4.11).
- Listado de Sobrestock con capital inmovilizado.
- Detalle de Producto con la "justificación siempre visible" (por qué se recomienda esa cantidad, con las fórmulas de fondo).
- Pantalla de Administración básica (carga de CSV, disparo manual de recálculo).

**Definition of Done:** un responsable de compras ficticio puede abrir el dashboard y, sin explicación adicional, entender qué comprar hoy y por qué.

**Estado: COMPLETADA y ampliada a 9 pantallas.** Además de las 5 originales, se sumaron Alertas, Recomendaciones (con feedback aplicada/descartada), Clasificación ABC/XYZ y Proveedores — adelantadas de v1.1/10.6 porque el backend ya las soportaba (o, en el caso de Proveedores, un recorte acotado a lo que hay dato real para soportar: ver Sección 8.11 de `InventoryIQ_Documentacion.md`). Detalle de Producto quedó acotado a la proyección de demanda, no a la ficha completa con histórico que describe la Sección 8.2. Administración ganó dos secciones no previstas en el diseño original de la pantalla (aunque "parámetros de negocio por categoría" sí figuraba como contenido esperado, Sección 10.8): "Conteo de stock" (Sección 8.16) y "Parámetros de categorías" (edición del umbral de sobrestock y stock de seguridad extra por categoría, Sección 8.10 — primer punto de la Sección 8 original que pasa de PLANIFICADO a IMPLEMENTADO sin haber sido primero un endpoint "no previsto").

---

## Fase 6 — Contenerización y cierre del MVP

**Objetivo:** que todo el sistema sea reproducible con un solo comando, como pide 2.7.

**Entregables:**
- Docker Compose completo (backend + frontend + Postgres) documentado en un `README.md` de arranque rápido.
- Carga inicial de los CSV simulados automatizada (script o endpoint de seed).
- Checklist final contra el alcance del MVP (Sección 1.5) para verificar que no falta nada.

**Definition of Done:** en una máquina limpia, `docker compose up` + un comando de seed dejan el sistema navegable end-to-end con los datos simulados.

**Estado: COMPLETADA.** `docker compose up` levanta backend + frontend + Postgres y el sistema es navegable de punta a punta con los CSV simulados (que ya vienen en el repo, en `data/csv/` — no hace falta un script de seed aparte porque no hay nada que sembrar en una base vacía). `README.md` de arranque rápido en la raíz del repo, con `.env.example`.

---

## Fases posteriores (fuera del MVP, para cuando quieras seguir)

Estas ya están detalladas en la Sección 12 del documento; se resumen acá solo como
referencia de hacia dónde escala el proyecto una vez cerrado el MVP:

| Versión | Foco | Estado |
|---|---|---|
| v1.1 | Alertas configurables, exportación de reportes, matriz ABC-XYZ como heatmap | Parcial: umbral de sobrestock y stock de seguridad extra ya son configurables por categoría (Sección 8.10, pantalla Administración → "Parámetros de categorías"); las pantallas de Alertas y Clasificación ya existen (adelantadas al MVP), pero sin exportación ni heatmap |
| v1.2 | Comparativa multi-sucursal, parámetros por categoría+sucursal | No iniciada |
| v2.0 | ETL real (Python + Pandas) + Data Warehouse en modelo estrella, reemplazando el adaptador CSV por uno Postgres/DW sin tocar el dominio | No iniciada |
| v3.0 | Forecasting estadístico/ML, estrategia de recomendación por clasificación ABC-XYZ, optimización multi-proveedor | No iniciada |

---

## Orden recomendado y por qué

1. **Dominio antes que infraestructura**: si las fórmulas están mal, no importa qué tan lindo quede el dashboard.
2. **CSV antes que Postgres transaccional**: los datos ya simulan una fuente "limpia"; no tiene sentido montar Postgres transaccional para datos que en el MVP son estáticos.
3. **API antes que Frontend**: podés validar toda la lógica con Postman antes de invertir tiempo en UI.
4. **Docker al final**: contenerizar algo que todavía cambia mucho es desperdiciar tiempo; se hace cuando el sistema ya es funcionalmente estable.

## Próximo paso concreto

Esta sección quedó desactualizada: describía la Fase 1 como punto de partida, pero
las Fases 0 a 6 ya están completas (ver el estado marcado en cada una). El proyecto
hoy está en una etapa distinta a la que este roadmap fue escrito para guiar —
`data/csv/sucursales.csv` ya refleja el despliegue real del usuario (2 sucursales
activas, no 3), y se agregaron capacidades no previstas originalmente: búsqueda
de productos por código/nombre, registro de conteo manual de stock (Secciones 8.15
y 8.16 de `InventoryIQ_Documentacion.md`) y corrección manual del lead time de
proveedores (Sección 8.11), motivadas por cómo funciona el negocio real (compras
coordinadas por WhatsApp sin ningún sistema, stock del POS que llega desactualizado,
lead time que no existe en ningún sistema).

Los próximos pasos concretos, en orden de valor para el uso real del sistema:

1. **Cargar el catálogo real** (`productos.csv`, `categorias.csv`, `proveedores.csv`)
   del usuario en lugar del simulado — es el bloqueante principal para que las
   recomendaciones dejen de ser un ejercicio con datos ficticios. **Decisión del
   usuario:** esta conversión se hace con un pipeline ETL propio (no una carga
   manual asistida), que transforma los exports reales del sistema XRP POS
   (`data/real-export/`, sin trackear en git) al formato de `data/csv/*.csv` que ya
   consumen los adaptadores CSV existentes — sin reemplazar esos adaptadores ni
   tocar el dominio, a diferencia del ETL de la fila v2.0 de la tabla de abajo (ese
   sí implica reemplazar CSV por Postgres/DW). Problemas de datos ya detectados en
   los exports reales que ese pipeline va a tener que resolver: entidades HTML
   (`&amp;`) que corrompen ~84-90 filas del export de precios al introducir un `;`
   de más; `Costo c/IVA` como campo de costo; `Cód.Barra` (único) como `sku`/sku
   interno, no `Cód.Int.` (tiene duplicados); mapeo de `Habilitación` a `activo`;
   jerarquía de categorías de `categorias_xrp.csv`; y reconciliación de los ~130
   nombres de proveedores del export de precios contra los 61 formalmente
   registrados en `Mantenimiento_de_Proveedores_` (la pantalla de Proveedores,
   Sección 8.11, ya permite cargar el lead time de cada uno a mano una vez que el
   ETL los vuelque a `proveedores.csv`).
2. **README de arranque rápido** en la raíz del repo — **completado** (ver Fase 6).
3. Recién después, evaluar si conviene avanzar hacia v1.1/v1.2 (alertas configurables,
   multi-sucursal) o directamente hacia v2.0 (ETL real + Data Warehouse), según qué
   tan bien funcione el MVP con datos reales.
