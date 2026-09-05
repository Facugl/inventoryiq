# InventoryIQ — Roadmap de Implementación (MVP)

Este roadmap toma el alcance del MVP definido en la documentación (Sección 1.5 y 12.1)
y lo baja a fases ejecutables, en orden, para que puedas ir de cero a un sistema
funcionando de punta a punta. Cada fase tiene un objetivo claro, entregables
concretos y un criterio de "terminado" (Definition of Done) para que sepas cuándo
avanzar a la siguiente.

**Duración estimada total (part-time, proyecto de portfolio):** 8 a 12 semanas.

---

## Fase 0 — Preparación del entorno (ya en curso)

**Objetivo:** tener los datos y el entorno de trabajo listos antes de escribir una línea de dominio.

- [x] Datos CSV simulados generados (`productos`, `categorías`, `proveedores`, `sucursales`, `ventas`, `compras`, `inventario`, `movimientos`).
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

---

## Fase 2 — Puertos y adaptadores CSV (ingesta de datos)

**Objetivo:** conectar el dominio con los datos reales simulados, respetando la arquitectura hexagonal.

**Entregables:**
- Puertos de salida: `ProductoRepository`, `VentaRepository`, `InventarioRepository`, `ProveedorRepository`, `CompraRepository`, `MovimientoRepository`.
- Adaptadores `CsvXxxRepositoryAdapter` que leen y parsean los CSV generados en la Fase 0.
- Validación básica de datos al ingerir (tipos, FKs existentes, fechas válidas) — importante porque en v2.0 esto se reemplaza por el ETL, pero el MVP necesita defenderse de un CSV mal cargado.
- Casos de uso (Application Services) que orquestan dominio + repos: `ObtenerProductosCriticosUseCase`, `GenerarRecomendacionesDeCompraUseCase`, `DetectarSobrestockUseCase`.

**Definition of Done:** con los 8 CSV cargados, podés ejecutar `GenerarRecomendacionesDeCompraUseCase` desde un test de integración y obtener una lista de recomendaciones coherente (podés cruzarla a mano contra 2-3 productos que ya viste en el análisis exploratorio de los CSV).

---

## Fase 3 — Persistencia operacional en PostgreSQL

**Objetivo:** persistir configuración, estados calculados e histórico de recomendaciones (12.1), aunque el dato transaccional siga viniendo de CSV.

**Entregables:**
- Esquema Postgres para: parámetros de negocio configurables (por categoría), snapshots de recomendaciones generadas, histórico de ejecución de recálculos.
- Migraciones versionadas (Flyway o Liquibase).
- Job/scheduler (o endpoint manual) que dispara el recálculo completo y persiste el resultado.

**Definition of Done:** correr el recálculo dos veces en días distintos deja dos snapshots distintos en Postgres, consultables por fecha.

---

## Fase 4 — API REST

**Objetivo:** exponer los casos de uso al frontend según la Sección 8 del documento.

**Entregables:**
- Controladores REST (adaptadores de entrada) para: listado de productos críticos, listado de sobrestock, detalle de producto, KPIs agregados, alertas.
- Manejo de errores y códigos HTTP consistentes.
- Documentación de la API (OpenAPI/Swagger).

**Definition of Done:** Postman/curl contra cada endpoint devuelve JSON válido y consistente con lo que viste en la Fase 2-3.

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

---

## Fase 6 — Contenerización y cierre del MVP

**Objetivo:** que todo el sistema sea reproducible con un solo comando, como pide 2.7.

**Entregables:**
- Docker Compose completo (backend + frontend + Postgres) documentado en un `README.md` de arranque rápido.
- Carga inicial de los CSV simulados automatizada (script o endpoint de seed).
- Checklist final contra el alcance del MVP (Sección 1.5) para verificar que no falta nada.

**Definition of Done:** en una máquina limpia, `docker compose up` + un comando de seed dejan el sistema navegable end-to-end con los datos simulados.

---

## Fases posteriores (fuera del MVP, para cuando quieras seguir)

Estas ya están detalladas en la Sección 12 del documento; se resumen acá solo como
referencia de hacia dónde escala el proyecto una vez cerrado el MVP:

| Versión | Foco |
|---|---|
| v1.1 | Alertas configurables, exportación de reportes, matriz ABC-XYZ como heatmap |
| v1.2 | Comparativa multi-sucursal, parámetros por categoría+sucursal |
| v2.0 | ETL real (Python + Pandas) + Data Warehouse en modelo estrella, reemplazando el adaptador CSV por uno Postgres/DW sin tocar el dominio |
| v3.0 | Forecasting estadístico/ML, estrategia de recomendación por clasificación ABC-XYZ, optimización multi-proveedor |

---

## Orden recomendado y por qué

1. **Dominio antes que infraestructura**: si las fórmulas están mal, no importa qué tan lindo quede el dashboard.
2. **CSV antes que Postgres transaccional**: los datos ya simulan una fuente "limpia"; no tiene sentido montar Postgres transaccional para datos que en el MVP son estáticos.
3. **API antes que Frontend**: podés validar toda la lógica con Postman antes de invertir tiempo en UI.
4. **Docker al final**: contenerizar algo que todavía cambia mucho es desperdiciar tiempo; se hace cuando el sistema ya es funcionalmente estable.

## Próximo paso concreto

Con los CSV ya generados, el siguiente paso natural es la **Fase 1**: escribir las
entidades de dominio y las fórmulas de la Sección 4 como Java puro, con tests
unitarios armados a mano (no hace falta leer un solo CSV todavía para esta fase).
