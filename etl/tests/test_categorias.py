"""Transformación de categorias_xrp.csv al formato que lee el backend."""

import csv

from inventoryiq_etl.categorias import COLUMNAS_DESTINO, convertir, transformar
from inventoryiq_etl.fuentes import leer_csv_pos


def transformar_fixture(ruta):
    filas, _ = leer_csv_pos(ruta)
    return transformar(filas)


def por_id(destino):
    return {f["categoria_id"]: f for f in destino}


class TestMapeo:
    def test_traduce_los_campos_al_formato_del_backend(self, fixture_categorias):
        destino, _ = transformar_fixture(fixture_categorias)
        almacen = por_id(destino)["1"]

        assert almacen["nombre"] == "ALMACEN"
        assert almacen["categoria_padre_id"] == ""
        assert list(almacen) == COLUMNAS_DESTINO

    def test_conserva_la_jerarquia(self, fixture_categorias):
        destino, _ = transformar_fixture(fixture_categorias)

        assert por_id(destino)["2"]["categoria_padre_id"] == "1"

    def test_resuelve_un_padre_declarado_mas_abajo_en_el_archivo(self, fixture_categorias):
        """El orden del archivo no es topológico; el padre puede venir después."""
        destino, reporte = transformar_fixture(fixture_categorias)

        assert por_id(destino)["7"]["categoria_padre_id"] == "270"
        assert not any("270" in i.detalle for i in reporte.ajustes)

    def test_aplica_los_parametros_por_defecto(self, fixture_categorias):
        """Decisión 4.4: el POS no los exporta, se usan los de los simulados."""
        destino, _ = transformar_fixture(fixture_categorias)

        assert {f["umbral_max_cobertura_dias"] for f in destino} == {"20.0"}
        assert {f["dias_cobertura_extra_default"] for f in destino} == {"4.0"}


class TestRaices:
    def test_idparent_cero_significa_sin_padre(self, fixture_categorias):
        """No significa "hija de la categoría 0": la propia 0 se declara así."""
        destino, _ = transformar_fixture(fixture_categorias)

        assert por_id(destino)["0"]["categoria_padre_id"] == ""
        assert por_id(destino)["1"]["categoria_padre_id"] == ""

    def test_la_categoria_cero_se_emite(self, fixture_categorias):
        """Es a donde van a apuntar los productos sin categoría conocida."""
        destino, _ = transformar_fixture(fixture_categorias)

        assert por_id(destino)["0"]["nombre"] == "No Categorizado"


class TestFilasProblematicas:
    def test_un_padre_inexistente_promueve_a_raiz_y_se_avisa(self, fixture_categorias):
        """Descartarla arrastraría a sus hijas y a los productos que la usen."""
        destino, reporte = transformar_fixture(fixture_categorias)

        assert por_id(destino)["5"]["categoria_padre_id"] == ""
        assert any("idparent inexistente" in i.motivo for i in reporte.ajustes)

    def test_una_categoria_que_se_apunta_a_si_misma_queda_como_raiz(self, fixture_categorias):
        destino, reporte = transformar_fixture(fixture_categorias)

        assert por_id(destino)["6"]["categoria_padre_id"] == ""
        assert any("sí misma" in i.motivo for i in reporte.ajustes)

    def test_descarta_las_que_no_se_pueden_representar(self, fixture_categorias):
        destino, reporte = transformar_fixture(fixture_categorias)
        motivos = [i.motivo for i in reporte.descartes]

        assert "denom vacío" in motivos          # sin nombre no sirve
        assert "idcate duplicado" in motivos     # la segunda rompería la clave
        assert "idcate no es un entero" in motivos
        assert "4" not in por_id(destino)
        assert "abc" not in por_id(destino)

    def test_el_duplicado_conserva_la_primera_aparicion(self, fixture_categorias):
        destino, _ = transformar_fixture(fixture_categorias)

        assert por_id(destino)["1"]["nombre"] == "ALMACEN"  # no "DUPLICADA"


class TestReporte:
    def test_cuadra_lo_leido_con_lo_emitido_y_lo_descartado(self, fixture_categorias):
        destino, reporte = transformar_fixture(fixture_categorias)

        assert reporte.leidas == reporte.emitidas + len(reporte.descartes)
        assert reporte.emitidas == len(destino)

    def test_el_resumen_nombra_las_filas_afectadas(self, fixture_categorias):
        _, reporte = transformar_fixture(fixture_categorias)
        resumen = reporte.resumen()

        assert "categorias" in resumen
        assert "descartes" in resumen
        assert "fila " in resumen  # cada incidencia dice de qué fila salió


class TestEscritura:
    def test_escribe_un_csv_que_el_backend_puede_leer(self, fixture_categorias, tmp_path):
        salida = tmp_path / "categorias.csv"

        reporte = convertir(fixture_categorias, salida)

        contenido = salida.read_text(encoding="utf-8")
        assert contenido.startswith(",".join(COLUMNAS_DESTINO))
        assert "\r\n" not in contenido  # LF, como los data/csv existentes
        assert "Fiambrería" in contenido  # UTF-8 de salida, cp1252 de entrada

        leidas = list(csv.DictReader(salida.open(encoding="utf-8")))
        assert len(leidas) == reporte.emitidas
