# Convenciones del repositorio

## Mensajes de commit: en inglés, Conventional Commits

**Los mensajes de commit se escriben en inglés**, asunto y cuerpo.

El historial tiene las dos cosas y conviene saber por qué: hasta
`70ed7e0` (2026-09-13) estaba en inglés, y desde `2faa811` (2026-10-06) pasó a
español sin una decisión explícita. **El inglés es la convención; el tramo en
español es la desviación**, y no se arrastra para adelante.

Formato:

```
<tipo>(<scope>): <asunto en imperativo, minúscula, sin punto final>

<cuerpo: qué cambió, y sobre todo POR QUÉ>
```

- **Tipos** en uso: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ops`,
  `revert`.
- **Scopes** en uso: `etl`, `frontend`, `backend`, `domain`, `ci`, y nombres de
  capacidad (`recommendations`, `overstock`, `forecast`, `kpis`,
  `classification`, `suppliers`, `alerts`, `categories`). El scope es opcional.
- Asunto en **imperativo** y en una línea (~72 caracteres).

El **cuerpo importa más que el asunto** en este repo, y es el estilo que hay
que mantener: explica el motivo de la decisión, no la lista de archivos
tocados. Cuando un cambio sale de una medición, el número va en el cuerpo
(cuántas filas se leyeron, cuántas se descartaron y por qué). Cuando se
descartó una alternativa, se dice cuál y por qué. El diff ya cuenta el *qué*.

## Lo que sigue en español

**Solo los commits cambian de idioma.** Todo lo demás se queda como está:

- La documentación (`README.md`, `docs/*.md`, `etl/README.md`).
- Los comentarios y docstrings del código.
- Los nombres de los tests del ETL (`test_descarta_el_producto_sin_precio`).

El código en sí —identificadores, clases, métodos— sigue en inglés en el
backend y el frontend, como ya está.

## Verificación antes de commitear

```bash
cd backend  && ./mvnw test                       # 351 tests
cd frontend && pnpm lint && pnpm test && pnpm build
cd etl      && .venv/Scripts/python -m pytest    # 124 tests
```

Los tests del backend necesitan Docker: varios de integración levantan un
PostgreSQL efímero con Testcontainers.

**CI no construye las imágenes Docker.** Corre lint, tests y build. Un
`Dockerfile` roto pasa CI y rompe el deploy de Render sin que nada avise — ya
pasó con el del frontend, que usaba `npm` en un proyecto `pnpm`. Si tocás un
`Dockerfile`, construí la imagen **y levantá el contenedor**: que compile no
garantiza que el proceso escuche donde tiene que escuchar.

## Dónde está el proyecto

Pausado a propósito, en un punto conocido. Ver "Dónde se detuvo" en
[`docs/InventoryIQ_Roadmap.md`](docs/InventoryIQ_Roadmap.md) antes de retomar.
