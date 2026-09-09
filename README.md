# InventoryIQ

[![CI](https://github.com/Facugl/inventoryiq/actions/workflows/ci.yml/badge.svg)](https://github.com/Facugl/inventoryiq/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Herramienta de soporte a la decisión de compras para un supermercado: convierte
datos de ventas e inventario en recomendaciones de reposición (qué comprar,
cuándo y en qué cantidad). Ver `docs/InventoryIQ_Documentacion.md` para la
visión completa del producto y `docs/InventoryIQ_Arquitectura.md` para lo
efectivamente implementado (auditado contra el código).

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (incluye Docker Compose).

No hace falta instalar Java, Node ni PostgreSQL por separado: todo corre en contenedores.

## Arranque rápido

```bash
cp .env.example .env
docker compose up -d --build
```

La primera vez tarda unos minutos (build de backend y frontend). Cuando termine:

- **Frontend:** http://localhost:5173
- **Backend (health check):** http://localhost:8080/health
- **Backend (API):** http://localhost:8080/api/v1/...
- **Backend (documentación interactiva — Swagger UI):** http://localhost:8080/swagger-ui/index.html
- **Backend (spec OpenAPI cruda, JSON):** http://localhost:8080/v3/api-docs

Los tres servicios (`postgres`, `backend`, `frontend`) quedan corriendo en segundo
plano. Para ver los logs: `docker compose logs -f backend` (o `frontend`, `postgres`).

Para bajar todo: `docker compose down` (agregá `-v` si además querés borrar los
datos de Postgres).

## Datos

El sistema arranca con un catálogo **simulado** (`data/csv/*.csv`): productos,
categorías, sucursales, ventas e inventario ficticios pero realistas (ver
`docs/README_datos_simulados.md`). Sirven para navegar y probar todas las
pantallas sin depender de datos reales.

### Cargar tu catálogo real

Para reemplazar el catálogo simulado por el real, editá `data/csv/productos.csv`
y `data/csv/categorias.csv` (mismas columnas, ver `docs/InventoryIQ_Documentacion.md`
Sección 5) y reiniciá el backend: `docker compose restart backend`.

Antes de exportar desde tu sistema, tené en cuenta:

- **`activo`**: marcá en `False` los productos que ya no vendés — el sistema los
  ignora en todos los cálculos, no hace falta borrarlos de la planilla.
- **`categoria_id`**: cada producto tiene que apuntar a una categoría que exista
  en `categorias.csv`, o el backend falla al arrancar (la carga de estos dos
  archivos es todo-o-nada: no hay tolerancia a filas individuales rotas, a
  diferencia de la carga de ventas desde la pantalla de Administración).
- **`lead_time_dias`**: tiene que ser un número mayor a 0 en todas las filas.
  Si hoy no lo llevás registrado por producto, un valor genérico razonable
  (por ejemplo, el lead time típico de tu proveedor habitual) sirve como
  punto de partida.
- **`proveedor_id`**: obligatorio en cada fila y tiene que apuntar a un
  proveedor que exista en `proveedores.csv` (misma exigencia que `categoria_id`).
  La pantalla de Proveedores permite listar los proveedores activos y corregir
  a mano el lead time de cada uno, pero no crearlos — si tu catálogo de
  proveedores no es el simulado, primero hay que cargar `data/csv/proveedores.csv`
  a mano o vía script.

Con más de 10.000 filas, cualquier valor faltante o mal tipado en estas columnas
hace que el backend no arranque — conviene revisar el CSV exportado antes de
reemplazarlo (por ejemplo, abriéndolo en una planilla y filtrando por celdas
vacías en `categoria_id`, `proveedor_id` o `lead_time_dias`).

## Verificación manual

Checklist para confirmar que un `docker compose up -d --build` levantó todo
correctamente, antes de probar las pantallas a mano:

1. **Los tres contenedores están arriba y sanos:**
   ```bash
   docker compose ps
   ```
   Esperado: `postgres` (healthy), `backend` y `frontend` en estado `Up`. Si
   alguno reinicia en loop, `docker compose logs -f <servicio>` muestra el error.

2. **El backend responde:**
   ```bash
   curl http://localhost:8080/health
   ```
   Esperado: `OK`.

3. **La API expone los endpoints esperados** — abrí
   http://localhost:8080/swagger-ui/index.html y confirmá que aparecen las 13
   secciones (Alertas, Categorías, Estado de productos, Forecast, Health,
   Ingesta CSV, Inventario, KPIs, Productos, Proveedores, Recomendaciones,
   Sucursales, Sugerencias de reposición). "Try it out" en `GET /api/v1/stores`
   o `GET /api/v1/suppliers` sin parámetros debería devolver 200 con datos.

4. **El frontend carga y llega al backend:** abrí http://localhost:5173 — si
   el catálogo de sucursales/categorías no carga, la pantalla lo dice
   explícitamente ("No se pudo cargar el catálogo..."), señal de que el
   frontend no está llegando al backend (revisar `VITE_BACKEND_URL` en
   `docker-compose.yml` y los logs de `frontend`).

5. **Recorré las 9 pantallas** desde la barra de navegación — con los datos
   simulados, todas deberían mostrar contenido (nunca una pantalla en blanco
   sin mensaje de error ni de "sin datos").

6. **Tests automatizados en verde** (no reemplazan la prueba manual, pero son
   la forma más rápida de detectar una regresión antes de perder tiempo
   navegando a mano):
   ```bash
   cd backend && ./mvnw test
   cd frontend && pnpm lint && pnpm build
   ```

## Desarrollo

- **Backend:** Java 17 + Spring Boot, arquitectura hexagonal. Tests: `cd backend && ./mvnw test`.
- **Frontend:** React + TypeScript + Vite. Lint/build: `cd frontend && npm run lint && npm run build`.
- El frontend, dentro de Docker, usa `VITE_BACKEND_URL` para encontrar al backend
  (ver `docker-compose.yml`) y hot-reload vía polling (necesario en Docker Desktop
  para Windows/Mac — ver `frontend/vite.config.ts`).

## Documentación

- `docs/InventoryIQ_Documentacion.md` — especificación funcional completa (visión, reglas de negocio, modelo de datos, roadmap evolutivo).
- `docs/InventoryIQ_Arquitectura.md` — arquitectura y flujos **implementados**, auditados contra el código.
- `docs/InventoryIQ_Roadmap.md` — estado de avance por fase y próximos pasos.
- `docs/README_datos_simulados.md` — cómo se generaron los CSV de prueba.
- **Swagger UI** (con el backend corriendo) — http://localhost:8080/swagger-ui/index.html:
  documentación interactiva de cada endpoint (parámetros, request/response, "Try it out"
  contra el backend real), generada automáticamente por springdoc-openapi a partir del
  código — nunca queda desactualizada respecto a los controllers.
