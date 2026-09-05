# InventoryIQ — Datos CSV simulados

Generados a partir de la Sección 5 de la documentación. No son datos aleatorios sueltos:
se simuló día a día la evolución real del stock (ventas → consumen stock → disparan
punto de pedido → generan compra → llega con lead time → repone stock), aplicando las
fórmulas de la Sección 4 (ADS corregido, ROP, stock de seguridad, estacionalidad).

## Archivos

| Archivo | Filas | Contenido |
|---|---|---|
| `categorias.csv` | 14 | Jerarquía de categorías con parámetros de cobertura |
| `proveedores.csv` | 10 | Incluye 1 proveedor inactivo (para probar filtros) |
| `sucursales.csv` | 3 | Centro, Norte, Sur |
| `productos.csv` | 62 | Distribuidos en 11 categorías-hoja, incluye 4 productos inactivos |
| `ventas.csv` | ~73.500 | Diario, por producto y sucursal, 13 meses (jul-2025 a ago-2026) |
| `compras.csv` | ~7.300 | Con estados Recibida/Parcial/Cancelada/Pendiente |
| `inventario.csv` | ~73.500 | Snapshot diario de stock por producto/sucursal |
| `movimientos.csv` | ~71.500 | Entradas y salidas de stock, trazables contra ventas/compras |

## Características de negocio simuladas a propósito

- **Curva ABC real**: ~30% de los productos explican el 80% del valor vendido (perfil Pareto).
- **Curva XYZ real**: cada producto tiene una variabilidad de demanda distinta (X/Y/Z) reflejada en el ruido de sus ventas.
- **Estacionalidad**: pico claro en diciembre para bebidas (con y sin alcohol) y panificados (pan dulce); valle de bebidas/congelados en invierno.
- **Quiebres de stock reales**: ~2,4% de los registros diarios tienen `stock_actual = 0`, útil para probar la regla 4.9 (censura de demanda).
- **Proveedores poco confiables**: ~12% de los productos tienen lead time real más variable, generando demoras ocasionales — insumo para la Estrategia 3 de la Sección 11.
- **Compras con distintos desenlaces**: recibidas completas, parciales, canceladas y pendientes (aún en tránsito a la fecha de corte).
- **Productos de baja rotación**: un grupo de productos con ventas casi nulas, candidatos a "Baja Rotación / Descontinuable" (regla 4.12).

## Fecha de corte ("hoy" del sistema)

`2026-08-01`. Todo dato con fecha posterior representa futuro (no debería existir salvo compras "Pendientes" de recepción).

## Cómo se generó

Ver `generar_datos.py` (catálogo maestro) y `simular_transacciones.py` (simulación
día a día) si querés regenerar los datos con otra semilla, otro tamaño de catálogo,
u otro rango de fechas.
