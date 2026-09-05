# Flujo de recálculo de estado de productos

Este documento representa exclusivamente el flujo **IMPLEMENTADO y verificable** del caso de uso `RecalculateProductStatusUseCase` (impl: `RecalculateProductStatusService`), verificado contra el código fuente actual. Es el orquestador final del proyecto: dispara, en secuencia, a `GetCriticalProductsUseCase`, `DetectOverstockUseCase`, `RecalculateRecommendationsUseCase` y `GenerateAlertsUseCase` por cada sucursal en alcance.

**Nota importante de alcance:** este caso de uso no persiste una tabla de "estado de producto" propia. El único efecto persistido real es el que ya cubre `RecalculateRecommendationsUseCase` (recomendaciones en PostgreSQL). Los otros tres casos de uso se recalculan al vuelo, igual que en el resto del proyecto — se corren acá para completar el resumen del job, no porque tengan un efecto secundario propio que persistir.

```mermaid
flowchart TD
    SCHED["ProductStatusScheduledJob<br/>cron diario (02:00 por defecto)"]
    CTRL["ProductStatusController<br/>POST /api/v1/product-status/recalculate"]
    CMD["RecalculateProductStatusCommand<br/>(storeId opcional, referenceDate)"]
    UC["RecalculateProductStatusUseCase"]
    SCOPE{"¿storeId presente?"}
    ONE["Alcance: esa sucursal"]
    ALL["storeRepository.findAllActive()<br/>Alcance: todas las sucursales activas"]

    subgraph LOOP["Por cada sucursal del alcance, en secuencia"]
        direction TB
        CRIT["GetCriticalProductsUseCase<br/>→ cantidad de productos críticos"]
        OVER["DetectOverstockUseCase<br/>→ cantidad de productos en sobrestock"]
        RECO["RecalculateRecommendationsUseCase<br/>→ persiste recomendaciones en PostgreSQL"]
        ALERTS["GenerateAlertsUseCase<br/>→ cantidad de alertas"]
        SUMMARY["StoreRecalculationSummary<br/>(storeId, criticalCount, overstockCount,<br/>alertsCount, recommendations)"]

        CRIT --> OVER --> RECO --> ALERTS --> SUMMARY
    end

    RESULT["RecalculateProductStatusResult<br/>(storesProcessed, summaries)"]
    RESP_JOB["Se loguea storesProcessed"]
    RESP_REST["JSON, 200 OK"]

    SCHED -->|"storeId = null siempre"| CMD
    CTRL -->|"storeId del body, o null"| CMD
    CMD --> UC --> SCOPE
    SCOPE -->|"sí"| ONE --> CRIT
    SCOPE -->|"no"| ALL --> CRIT
    SUMMARY --> RESULT
    RESULT --> RESP_JOB
    RESULT --> RESP_REST

    classDef entrada fill:#dbeafe,stroke:#2563eb,color:#1e3a8a;
    classDef aplicacion fill:#fef9c3,stroke:#ca8a04,color:#713f12;

    class SCHED,CTRL,RESP_JOB,RESP_REST entrada;
    class CMD,UC,SCOPE,ONE,ALL,CRIT,OVER,RECO,ALERTS,SUMMARY,RESULT aplicacion;
```

## Notas sobre el detalle omitido

- El orden `Critical → Overstock → RecalculateRecommendations → Alerts` es literal del cuerpo de `recalculateForStore` en `RecalculateProductStatusService`, no una inferencia.
- Cada uno de los cuatro pasos internos del loop es en sí mismo un caso de uso completo con su propio flujo de datos (ver `current-architecture.md`, Tabla 2, y `reorder-suggestions-flow.md` para el detalle de `GenerateReorderSuggestionsUseCase`, invocado a su vez por `RecalculateRecommendationsUseCase`).
- `referenceDate` nunca lo elige quien dispara el flujo: el job lo resuelve con `LocalDate.now(clock)` y el controller REST hace lo mismo — no es un parámetro de request en ningún trigger.
- No existe endpoint documentado en la Sección 8 de `InventoryIQ_Documentacion.md` para este caso de uso; `ProductStatusController` se diseñó en este slice para poder disparar el job manualmente, además del trigger programado real.
