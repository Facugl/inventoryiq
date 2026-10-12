"""Transformación de Consulta_de_stock_xrp.xls al formato del backend."""

import csv
from datetime import date

from inventoryiq_etl.fuentes import leer_tabla_html
from inventoryiq_etl.inventario import COLUMNAS_DESTINO, convertir, transformar

FECHA = date(2026, 9, 6)

# Los nombres del fixture son inventados, igual que los de `sucursales.csv`:
# los reales son de un negocio concreto y el repositorio es público.
SUCURSALES = {"Depósito Las Lomas": "1", "Sucursal Rivadavia": "2"}


def transformar_fixture(ruta, sucursales=None, **kwargs):
    return transformar(
        leer_tabla_html(ruta), FECHA, sucursales or SUCURSALES, **kwargs
    )


def por_clave(destino):
    return {(f["sucursal_id"], f["producto_id"]): f for f in destino}


class TestLecturaDelHtml:
    def test_ignora_los_encabezados_sueltos_del_reporte(self, fixture_stock):
        """El export abre con 'Empresa:' y el título antes de la tabla."""
        filas = leer_tabla_html(fixture_stock)

        assert len(filas) == 9
        assert all("Stk.(U)" in f for f in filas)

    def test_usa_la_cabecera_aunque_venga_al_doble_de_ancho(self, fixture_stock):
        """El POS une dos filas de header en una, con celdas vacías de más."""
        filas = leer_tabla_html(fixture_stock)

        assert filas[0]["Unidad de Negocio"] == "Depósito Las Lomas"
        assert filas[0]["Código Barra"] == "7790000000001"


class TestMapeo:
    def test_traduce_los_campos_al_formato_del_backend(self, fixture_stock):
        destino, _ = transformar_fixture(fixture_stock)
        fila = por_clave(destino)[("1", "7790000000001")]

        assert fila["stock_actual"] == "120"
        assert fila["fecha_snapshot"] == "2026-09-06"
        assert fila["stock_en_transito"] == "0"
        assert list(fila) == COLUMNAS_DESTINO

    def test_el_mismo_producto_convive_en_dos_sucursales(self, fixture_stock):
        destino, _ = transformar_fixture(fixture_stock)
        claves = por_clave(destino)

        assert claves[("1", "7790000000001")]["stock_actual"] == "120"
        assert claves[("2", "7790000000001")]["stock_actual"] == "40"

    def test_entiende_el_separador_de_miles(self, fixture_stock):
        """Los números vienen como "2,700.00000"."""
        destino, _ = transformar_fixture(fixture_stock)

        assert por_clave(destino)[("1", "7790000000005")]["stock_actual"] == "2700"

    def test_el_inventario_id_es_correlativo(self, fixture_stock):
        destino, _ = transformar_fixture(fixture_stock)

        assert [f["inventario_id"] for f in destino] == ["1", "2", "3", "4"]


class TestStock:
    """Decisión 4.1: en el export real, el 40% viene negativo y el 43% vacío."""

    def test_el_negativo_se_registra_en_cero_y_se_avisa(self, fixture_stock):
        destino, reporte = transformar_fixture(fixture_stock)

        assert por_clave(destino)[("1", "7790000000002")]["stock_actual"] == "0"
        assert any("stock negativo" in i.motivo for i in reporte.ajustes)

    def test_la_fila_sin_dato_se_omite_en_vez_de_asumir_cero(self, fixture_stock):
        """Un 0 inventado afirmaría que está agotado, y eso dispara reposición."""
        destino, reporte = transformar_fixture(fixture_stock)

        assert ("1", "7790000000003") not in por_clave(destino)
        assert ("1", "7790000000004") not in por_clave(destino)
        assert sum(1 for i in reporte.descartes if i.motivo == "sin dato de stock") == 2


class TestFilasProblematicas:
    def test_descarta_las_que_no_se_pueden_representar(self, fixture_stock):
        destino, reporte = transformar_fixture(fixture_stock)
        motivos = [i.motivo for i in reporte.descartes]

        assert "código de barras vacío o no numérico" in motivos
        assert "unidad de negocio desconocida" in motivos
        assert "producto repetido en la misma sucursal" in motivos
        assert len(destino) == 4

    def test_una_unidad_de_negocio_nueva_no_se_inventa_una_sucursal(self, fixture_stock):
        """Mapearla por las malas pondría el stock en la sucursal equivocada:
        no falla, simplemente recomienda mal."""
        _, reporte = transformar_fixture(fixture_stock)

        assert any("Sucursal Desconocida" in i.detalle for i in reporte.descartes)

    def test_el_repetido_conserva_la_primera_aparicion(self, fixture_stock):
        destino, _ = transformar_fixture(fixture_stock)

        assert por_clave(destino)[("1", "7790000000001")]["stock_actual"] == "120"


class TestIntegridadReferencial:
    def test_descarta_el_stock_de_un_producto_fuera_del_catalogo(self, fixture_stock):
        """El backend carga productos e inventario todo-o-nada: una
        referencia colgada lo hace fallar al arrancar."""
        destino, reporte = transformar_fixture(
            fixture_stock, productos_validos={"7790000000001"}
        )

        assert {f["producto_id"] for f in destino} == {"7790000000001"}
        assert any("no está en el catálogo" in i.motivo for i in reporte.descartes)

    def test_avisa_cuando_no_puede_verificarla(self, fixture_stock):
        _, reporte = transformar_fixture(fixture_stock)

        assert any("sin verificar" in i.motivo for i in reporte.ajustes)


class TestEscritura:
    def test_la_fecha_sale_del_pie_del_export(self, fixture_stock, tmp_path):
        """Y no del `mtime`: copiar un archivo se lo cambia, y un snapshot
        atribuido al día equivocado no falla, recomienda mal."""
        reporte = convertir(fixture_stock, tmp_path / "inventario.csv", export=tmp_path)
        filas = csv.DictReader((tmp_path / "inventario.csv").open(encoding="utf-8"))

        assert {f["fecha_snapshot"] for f in filas} == {"2026-09-06"}
        assert any("snapshots de 1 día" in i.motivo for i in reporte.ajustes)

    def test_deja_asentada_la_fecha_que_uso(self, fixture_stock, tmp_path):
        reporte = convertir(fixture_stock, tmp_path / "inventario.csv", fecha_snapshot=FECHA,
                            export=tmp_path)

        assert any("2026-09-06" in i.detalle for i in reporte.ajustes)

    def test_escribe_un_csv_que_el_backend_puede_leer(self, fixture_stock, tmp_path):
        salida = tmp_path / "inventario.csv"

        reporte = convertir(fixture_stock, salida, fecha_snapshot=FECHA, export=tmp_path)

        contenido = salida.read_text(encoding="utf-8")
        assert contenido.startswith(",".join(COLUMNAS_DESTINO))
        assert "\r\n" not in contenido

        leidas = list(csv.DictReader(salida.open(encoding="utf-8")))
        assert len(leidas) == reporte.emitidas


class TestAcumulacion:
    """El stock no se puede pedir hacia atrás: cada export es un día que no se
    recupera, así que el archivo se acumula en vez de regenerarse."""

    def _export_con_fecha(self, carpeta, fixture_stock, nombre, dia, mes_largo, anio):
        crudo = fixture_stock.read_text(encoding="cp1252")
        crudo = crudo.replace("Domingo 6 de Septiembre de 2026",
                              f"Lunes {dia} de {mes_largo} de {anio}")
        (carpeta / nombre).write_text(crudo, encoding="cp1252")

    def test_suma_un_dia_por_cada_export(self, fixture_stock, tmp_path):
        diarios = tmp_path / "diarios"
        diarios.mkdir()
        self._export_con_fecha(diarios, fixture_stock, "a.xls", 6, "Septiembre", 2026)
        self._export_con_fecha(diarios, fixture_stock, "b.xls", 7, "Septiembre", 2026)
        salida = tmp_path / "inventario.csv"

        convertir(diarios, salida, export=tmp_path)
        filas = list(csv.DictReader(salida.open(encoding="utf-8")))

        assert {f["fecha_snapshot"] for f in filas} == {"2026-09-06", "2026-09-07"}

    def test_conserva_los_dias_viejos_aunque_su_export_ya_no_este(
        self, fixture_stock, tmp_path
    ):
        diarios = tmp_path / "diarios"
        diarios.mkdir()
        self._export_con_fecha(diarios, fixture_stock, "a.xls", 6, "Septiembre", 2026)
        salida = tmp_path / "inventario.csv"
        convertir(diarios, salida, export=tmp_path)

        (diarios / "a.xls").unlink()
        self._export_con_fecha(diarios, fixture_stock, "b.xls", 7, "Septiembre", 2026)
        convertir(diarios, salida, export=tmp_path)
        filas = list(csv.DictReader(salida.open(encoding="utf-8")))

        assert {f["fecha_snapshot"] for f in filas} == {"2026-09-06", "2026-09-07"}

    def test_no_pisa_el_conteo_manual_de_la_aplicacion(self, fixture_stock, tmp_path):
        """La pantalla de Administración agrega a este mismo archivo
        (`CsvInventoryRepositoryAdapter.appendToFile`): regenerarlo borraría
        las correcciones hechas a mano."""
        diarios = tmp_path / "diarios"
        diarios.mkdir()
        self._export_con_fecha(diarios, fixture_stock, "a.xls", 6, "Septiembre", 2026)
        salida = tmp_path / "inventario.csv"
        convertir(diarios, salida, export=tmp_path)
        with salida.open("a", encoding="utf-8", newline="") as fh:
            fh.write("999,2026-09-20,7790000000777,1,42,0\n")

        convertir(diarios, salida, export=tmp_path)
        filas = list(csv.DictReader(salida.open(encoding="utf-8")))

        a_mano = [f for f in filas if f["producto_id"] == "7790000000777"]
        assert len(a_mano) == 1
        assert a_mano[0]["stock_actual"] == "42"

    def test_el_export_sin_fecha_se_descarta_en_vez_de_adivinar(self, tmp_path):
        diarios = tmp_path / "diarios"
        diarios.mkdir()
        (diarios / "sin_pie.xls").write_text(
            "<table><tr><td>Unidad de Negocio</td></tr></table>", encoding="cp1252"
        )

        reporte = convertir(diarios, tmp_path / "inventario.csv", export=tmp_path)

        assert any("no dice de qué día es" in i.motivo for i in reporte.descartes)
