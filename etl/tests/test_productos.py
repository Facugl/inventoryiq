"""Transformación de Cambio_de_precios_xrp.csv al formato del backend.

Productos es el único slice que cruza varios exports: el maestro de precios no
trae ni la categoría ni el id del proveedor. Los tests separan las dos cosas —
el cruce (qué categoría le toca a cada código de barras) y la transformación
(qué fila sale) — porque fallan por motivos distintos.
"""

import csv

from inventoryiq_etl import decisiones
from inventoryiq_etl.fuentes import leer_csv_pos, leer_spreadsheetml
from inventoryiq_etl.productos import (
    COLUMNAS_DESTINO,
    categoria_por_barra,
    convertir,
    transformar,
)

# Los que el fixture de categorías deja válidos, más la de "sin asignar".
CATEGORIAS_VALIDAS = {"0", "1", "2", "3", "5", "6", "7", "270"}

PROVEEDORES = {
    "Sin Proveedor": "0",
    "LACTEOS DEL SUR SA": "1",
    "DISTRIBUIDORA M&M": "2",
    "Molinos del Litoral S.A.": "3",
}

RUTAS = {
    "No Categorizado": "0",
    "ALMACEN": "1",
    "LACTEOS": "2",
    "FIAMBRERIA": "3",
    "BAZAR": "270",
}


def transformar_fixture(ruta_precios, categorias=None):
    filas, _ = leer_csv_pos(ruta_precios)
    return transformar(
        filas,
        categorias if categorias is not None else {},
        PROVEEDORES,
        CATEGORIAS_VALIDAS,
    )


def por_id(destino):
    return {f["producto_id"]: f for f in destino}


class TestCruceDeCategorias:
    """El listado de ventas es el único export que trae la categoría (5.1)."""

    def test_resuelve_la_ruta_completa_contra_el_export_de_categorias(self, fixture_listado):
        """`Categoría` ya viene como ruta ("ALMACEN, BEBIDAS"), que es la misma
        forma de la columna `catego`: el cruce es directo, sin partir nada."""
        por_barra, _ = categoria_por_barra(leer_spreadsheetml(fixture_listado), RUTAS)

        assert por_barra["7790000000001"] == "2"
        assert por_barra["7790000000002"] == "3"

    def test_delata_las_rutas_que_el_export_de_categorias_no_trae(self, fixture_listado):
        """Significa que los dos exports se sacaron en momentos distintos."""
        por_barra, sin_resolver = categoria_por_barra(
            leer_spreadsheetml(fixture_listado), RUTAS
        )

        assert sin_resolver == {"ALMACEN, INEXISTENTE"}
        assert "7790000000005" not in por_barra

    def test_ignora_las_filas_sin_categoria(self, fixture_listado):
        por_barra, _ = categoria_por_barra(leer_spreadsheetml(fixture_listado), RUTAS)

        assert "7790000000004" not in por_barra

    def test_gana_el_primer_listado_cuando_dos_discrepan(self):
        """La categoría es un atributo del producto, no de la sucursal: una
        discrepancia entre listados es dato sucio, no una distinción real."""
        filas = [
            {"Barra": "7790000000001", "Categoría": "LACTEOS"},
            {"Barra": "7790000000001", "Categoría": "BAZAR"},
        ]
        por_barra, _ = categoria_por_barra(filas, RUTAS)

        assert por_barra["7790000000001"] == "2"


class TestMapeo:
    def test_traduce_los_campos_al_formato_del_backend(self, fixture_precios):
        destino, _ = transformar_fixture(fixture_precios, {"7790000000001": "2"})
        leche = por_id(destino)["7790000000001"]

        assert list(leche) == COLUMNAS_DESTINO
        assert leche["nombre"] == "LECHE ENTERA 1L"
        assert leche["categoria_id"] == "2"
        assert leche["proveedor_id"] == "1"
        assert leche["activo"] == "True"
        assert leche["lead_time_dias"] == decisiones.LEAD_TIME_DIAS

    def test_producto_id_y_sku_son_el_codigo_de_barras(self, fixture_precios):
        """Tiene que ser estable entre corridas: las recomendaciones guardadas
        en Postgres apuntan al producto por id."""
        destino, _ = transformar_fixture(fixture_precios)
        leche = por_id(destino)["7790000000001"]

        assert leche["sku"] == "7790000000001"

    def test_conserva_la_precision_de_los_precios_del_pos(self, fixture_precios):
        """No se redondea a centavos: sería un ajuste silencioso sobre las
        10.000 filas del catálogo."""
        destino, _ = transformar_fixture(fixture_precios)
        leche = por_id(destino)["7790000000001"]

        assert leche["precio_costo"] == "1428.57400"
        assert leche["precio_venta"] == "2000.00000"

    def test_la_entidad_html_no_parte_la_fila(self, fixture_precios):
        """Sin reparar antes de partir, el `;` de `&amp;` corre las columnas y
        el proveedor queda en 'DISTRIBUIDORA M&amp'."""
        destino, _ = transformar_fixture(fixture_precios)
        queso = por_id(destino)["7790000000002"]

        assert queso["nombre"] == "QUESO & CREMA"
        assert queso["proveedor_id"] == "2"

    def test_deriva_la_unidad_de_medida_de_la_presentacion(self, fixture_precios):
        destino, _ = transformar_fixture(fixture_precios)

        assert por_id(destino)["7790000000001"]["unidad_medida"] == "UN"
        assert por_id(destino)["7790000000002"]["unidad_medida"] == "KG"

    def test_una_presentacion_sin_unidad_no_descarta_el_producto(self, fixture_precios):
        """`unitOfMeasure` hoy no alimenta ningún cálculo: el dominio lo
        transporta y nadie lo lee."""
        destino, reporte = transformar_fixture(fixture_precios)

        assert por_id(destino)["7790000000013"]["unidad_medida"] == "UN"
        assert any("sin unidad reconocible" in i.motivo for i in reporte.ajustes)

    def test_compra_no_se_trata_como_inactivo(self, fixture_precios):
        """Decisión 4.5: son 8 productos y el criterio no está claro."""
        destino, _ = transformar_fixture(fixture_precios)

        assert por_id(destino)["7790000000011"]["activo"] == "True"


class TestProductosSinCategoria:
    """Decisión 4.7: el listado cubre los que se vendieron, no el catálogo."""

    def test_van_a_la_categoria_que_el_pos_ya_trae(self, fixture_precios):
        destino, reporte = transformar_fixture(fixture_precios)
        sin_categoria = por_id(destino)["7790000000010"]

        assert sin_categoria["categoria_id"] == decisiones.CATEGORIA_SIN_ASIGNAR_ID
        assert any("sin categoría en los listados" in i.motivo for i in reporte.ajustes)

    def test_no_se_inventa_la_categoria(self):
        """`categorias_xrp` declara idcate=0 "No Categorizado": el centinela
        existe en el origen, igual que el proveedor de la decisión 4.2."""
        assert decisiones.CATEGORIA_SIN_ASIGNAR_NOMBRE == "No Categorizado"

    def test_se_reporta_una_vez_y_no_por_fila(self, fixture_precios):
        """Son miles: una incidencia por producto ahogaría el resto."""
        _, reporte = transformar_fixture(fixture_precios)
        sin_categoria = [i for i in reporte.ajustes if "sin categoría" in i.motivo]

        assert len(sin_categoria) == 1

    def test_sin_la_categoria_en_el_catalogo_se_descartan(self, fixture_precios):
        """Emitirlos dejaría una referencia colgada, y el backend carga
        productos y categorías todo-o-nada: no fallaría la fila, fallaría el
        arranque."""
        filas, _ = leer_csv_pos(fixture_precios)
        destino, reporte = transformar(filas, {}, PROVEEDORES, {"1", "2"})

        assert "7790000000010" not in por_id(destino)
        assert any("sin dónde ubicarlo" in i.motivo for i in reporte.descartes)


class TestDescartes:
    def test_descarta_el_producto_sin_codigo_de_barras(self, fixture_precios):
        destino, reporte = transformar_fixture(fixture_precios)

        assert not any(f["nombre"] == "SIN CODIGO DE BARRAS" for f in destino)
        assert any("código de barras vacío" in i.motivo for i in reporte.descartes)

    def test_descarta_el_codigo_de_barras_repetido(self, fixture_precios):
        """Es el producto_id: un duplicado pisaría al primero."""
        destino, reporte = transformar_fixture(fixture_precios)

        assert not any(f["nombre"] == "LECHE ENTERA REPETIDA" for f in destino)
        assert any("duplicado" in i.motivo for i in reporte.descartes)

    def test_descarta_el_producto_sin_precio(self, fixture_precios):
        """El backend parsea el precio con BigDecimal y no tolera un vacío; un
        0 inventado distorsiona el capital inmovilizado y la clasificación."""
        destino, reporte = transformar_fixture(fixture_precios)

        assert "7790000000012" not in por_id(destino)
        assert any("precio ausente" in i.motivo for i in reporte.descartes)

    def test_descarta_el_proveedor_sin_correspondencia(self, fixture_precios):
        """No se cae al proveedor sin asignar: eso le cambiaría el lead time al
        producto en silencio, y de ahí sale el punto de pedido."""
        destino, reporte = transformar_fixture(fixture_precios)

        assert "7790000000014" not in por_id(destino)
        assert any("proveedor sin correspondencia" in i.motivo for i in reporte.descartes)


class TestConvertir:
    """El slice entero contra una carpeta de exports armada como la real."""

    def test_escribe_el_csv_que_lee_el_backend(self, export_pos, tmp_path):
        salida = tmp_path / "productos.csv"
        convertir(export_pos / "Cambio_de_precios_xrp.csv", salida)

        with salida.open(encoding="utf-8", newline="") as fh:
            filas = list(csv.DictReader(fh))

        assert list(filas[0]) == COLUMNAS_DESTINO
        assert por_id(filas)["7790000000001"]["categoria_id"] == "2"
        assert por_id(filas)["7790000000002"]["proveedor_id"] == "2"

    def test_resuelve_los_auxiliares_desde_la_misma_carpeta(self, export_pos, tmp_path):
        """No necesita el catálogo ya generado: deriva categorías y proveedores
        de los mismos exports y transformaciones, así que los ids coinciden."""
        reporte = convertir(export_pos / "Cambio_de_precios_xrp.csv", tmp_path / "p.csv")

        assert reporte.emitidas
        assert any("listado de ventas" in i.motivo for i in reporte.ajustes)

    def test_avisa_cuando_no_hay_ningun_listado(self, export_pos, tmp_path):
        """Sin listado no hay categorías: todo el catálogo caería en "No
        Categorizado" sin que nada lo diga."""
        (export_pos / "Listado_ventas.xls").unlink()
        reporte = convertir(export_pos / "Cambio_de_precios_xrp.csv", tmp_path / "p.csv")

        assert any("no se encontró ningún listado" in i.motivo for i in reporte.ajustes)
