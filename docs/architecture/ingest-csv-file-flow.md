# Flujo de ingesta de archivos CSV (ventas)

Este documento representa exclusivamente el flujo **IMPLEMENTADO y verificable** del caso de uso `IngestCsvFileUseCase` (impl: `IngestCsvFileService`), verificado contra el código fuente actual. Hoy solo existe implementación para `CsvFileType.SALES` — es el único valor del enum; ingerir compras, inventario, productos, proveedores, categorías o sucursales está fuera de alcance de este flujo.

```mermaid
flowchart TD
    CLI["Cliente / API REST"]
    CTRL["CsvIngestionController<br/>POST /api/v1/csv-ingestions<br/>(fileType, file multipart)"]
    TYPE_CHECK{"¿fileType == SALES?"}
    BADTYPE["400 Bad Request<br/>(MethodArgumentTypeMismatchException,<br/>mecanismo de Spring, sin handler propio)"]
    PARSE["SalesCsvRowParser.parse<br/>valida columnas esperadas y tipos por fila"]
    BATCH["ParsedBatch<br/>(candidateRows, preValidationRejections)"]
    UC["IngestCsvFileUseCase"]

    subgraph LOOP["Por cada fila candidata"]
        direction TB
        REF{"¿producto_id y sucursal_id<br/>existen en el catálogo?"}
        DUP{"¿venta duplicada?<br/>(ya persistida, o repetida<br/>dentro del mismo archivo)"}
        ACCEPT["Fila aceptada"]
        REJECT["Fila rechazada (con motivo)"]

        REF -->|"no"| REJECT
        REF -->|"sí"| DUP
        DUP -->|"sí"| REJECT
        DUP -->|"no"| ACCEPT
    end

    RATE["Calcula % de rechazo<br/>sobre totalRowsRead"]
    THRESH{"¿% rechazo > 5%?<br/>(umbral crítico, Sección 7.4)"}
    ABORT["CsvIngestionThresholdExceededException<br/>→ 422 Unprocessable Content<br/>Nada se persiste"]
    SAVE["Persiste cada fila aceptada<br/>vía SaleIngestionRepository"]
    RESULT["IngestCsvFileResult<br/>(totalRowsRead, acceptedCount,<br/>rejectedCount, rejections)"]
    RESP["JSON, 200 OK"]

    CSVA["CsvSaleRepositoryAdapter"]
    CSVFILE[("ventas.csv")]

    CLI --> CTRL --> TYPE_CHECK
    TYPE_CHECK -->|"no"| BADTYPE
    TYPE_CHECK -->|"sí"| PARSE --> BATCH --> UC --> REF
    ACCEPT --> RATE
    REJECT --> RATE
    RATE --> THRESH
    THRESH -->|"sí"| ABORT
    THRESH -->|"no"| SAVE --> RESULT --> RESP --> CLI

    SAVE -.->|"responde vía"| CSVA
    CSVA --> CSVFILE

    classDef entrada fill:#dbeafe,stroke:#2563eb,color:#1e3a8a;
    classDef aplicacion fill:#fef9c3,stroke:#ca8a04,color:#713f12;
    classDef dominio fill:#dcfce7,stroke:#16a34a,color:#14532d;
    classDef salida fill:#fce7f3,stroke:#db2777,color:#831843;
    classDef infra fill:#f1f5f9,stroke:#64748b,color:#0f172a;

    class CLI,CTRL,BADTYPE,RESP entrada;
    class TYPE_CHECK,PARSE,BATCH,UC,REF,DUP,ACCEPT,REJECT,RATE,RESULT aplicacion;
    class THRESH,ABORT,SAVE dominio;
    class CSVA salida;
    class CSVFILE infra;
```

## Notas sobre el detalle omitido

- **`PARSE`** vive en `adapters/in/csv`, no en `application/`: valida estructura y tipos por fila (columnas esperadas, parseo de fecha/decimal/entero) apoyándose en el constructor de `Sale`, que ya verifica invariantes como `unidades_vendidas >= 0`. La integridad referencial y los duplicados quedan para la capa de aplicación, que sí tiene los puertos para resolverlos — de ahí la separación entre `PARSE` (fuera del use case) y `LOOP` (dentro).
- **`DUP`** colapsa dos chequeos independientes: `saleIngestionRepository.existsByProductStoreAndDate` (contra lo ya persistido) y una detección en memoria de duplicados dentro del mismo archivo subido (`seenInThisBatch`) — necesaria porque nada se persiste hasta el final de la corrida.
- El umbral del 5% se toma literalmente del ejemplo de la Sección 7.4 de `InventoryIQ_Documentacion.md`; no hay otro valor sugerido en la documentación, y es una constante fija en `IngestCsvFileService` (`REJECTION_THRESHOLD_PERCENT`), no configurable.
- Si se supera el umbral, la excepción se lanza **antes** del bloque de persistencia: es todo o nada por corrida, no hay persistencia parcial de las filas aceptadas hasta ese punto.
