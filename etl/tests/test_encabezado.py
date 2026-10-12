"""Lo que cada export dice de sí mismo: fecha, sucursal y período.

De acá sale todo lo que el pipeline necesita y no está en las filas de datos,
y es lo que evita tener que pedirle a nadie que renombre archivos.
"""

from datetime import date

from inventoryiq_etl.encabezado import Metadatos, fecha_larga, leer


class TestFechaLarga:
    def test_lee_el_pie_del_reporte(self):
        """Los dos exports cierran con una línea así, y es su fecha real."""
        assert fecha_larga(
            "XRP - Administración - Domingo 6 de Septiembre de 2026 00:43:56"
        ) == date(2026, 9, 6)

    def test_acepta_la_variante_sin_p(self):
        assert fecha_larga("Lunes 1 de Setiembre de 2025 10:00:00") == date(2025, 9, 1)

    def test_un_mes_que_no_existe_no_inventa_una_fecha(self):
        assert fecha_larga("Lunes 1 de Brumario de 2025") is None

    def test_sin_fecha_devuelve_none(self):
        assert fecha_larga("Consulta de stock") is None


class TestStock:
    def test_saca_la_fecha_del_pie_y_la_unidad(self, fixture_stock):
        meta = leer(fixture_stock)

        assert meta.generado == date(2026, 9, 6)
        assert meta.unidad_de_negocio == "Todas las Unidades"

    def test_el_export_de_stock_no_declara_periodo(self, fixture_stock):
        meta = leer(fixture_stock)

        assert meta.desde is None
        assert not meta.es_de_un_solo_dia


class TestListado:
    def test_lee_periodo_y_sucursal(self, fixture_listado_dia):
        meta = leer(fixture_listado_dia)

        assert meta.unidad_de_negocio == "Depósito Las Lomas"
        assert meta.desde == date(2025, 12, 22)
        assert meta.hasta == date(2025, 12, 22)
        assert meta.es_de_un_solo_dia

    def test_no_se_lleva_por_delante_el_rotulo_siguiente(self, fixture_listado_dia):
        """Los rótulos están en celdas contiguas: al sacar las marcas quedan
        pegados, y sin acotar la búsqueda la unidad se come el resto."""
        assert "Tipo de Listado" not in leer(fixture_listado_dia).unidad_de_negocio

    def test_un_rango_largo_no_es_de_un_solo_dia(self, fixture_listado):
        assert not leer(fixture_listado).es_de_un_solo_dia


def test_un_export_sin_metadatos_no_rompe(tmp_path):
    vacio = tmp_path / "x.xls"
    vacio.write_text("<table></table>", encoding="cp1252")

    assert leer(vacio) == Metadatos()
