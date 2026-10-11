"""El lector de exports del POS: encoding, separador y entidades HTML."""

from inventoryiq_etl.fuentes import (
    leer_csv_pos,
    leer_spreadsheetml,
    parsear_decimal,
    reparar_entidades,
)


class TestRepararEntidades:
    def test_desescapa_las_conocidas(self):
        texto, desconocidas = reparar_entidades("L&amp;CTEOS y &quot;otra&quot;")

        assert texto == 'L&CTEOS y "otra"'
        assert desconocidas == set()

    def test_delata_las_desconocidas_en_vez_de_reemplazarlas_a_ciegas(self):
        texto, desconocidas = reparar_entidades("ALGO&raquo;OTRO&#8212;MAS")

        assert desconocidas == {"&raquo;", "&#8212;"}
        assert "&raquo;" in texto  # se deja tal cual: la decide una persona

    def test_un_ampersand_suelto_no_es_una_entidad(self):
        texto, desconocidas = reparar_entidades("JUAN & PEDRO")

        assert texto == "JUAN & PEDRO"
        assert desconocidas == set()


class TestLeerCsvPos:
    """El fixture reproduce los defectos del export real (ver sección 6 del doc)."""

    def test_lee_separador_punto_y_coma_y_encoding_cp1252(self, fixture_categorias):
        filas, _ = leer_csv_pos(fixture_categorias)

        assert filas[0]["idcate"] == "0"
        assert filas[0]["denom"] == "No Categorizado"
        # El acento sobrevive: si se leyera como UTF-8 saldría mojibake.
        assert any(f["denom"] == "Fiambrería" for f in filas)

    def test_la_entidad_html_no_corre_las_columnas(self, fixture_categorias):
        """El `;` de `&amp;` coincide con el separador.

        Sin reparar antes de partir, esta fila tendría 7 campos en vez de 6 y
        `denom` quedaría en 'L&amp' con todo lo demás desplazado.
        """
        filas, _ = leer_csv_pos(fixture_categorias)
        lacteos = next(f for f in filas if f["idcate"] == "2")

        assert lacteos["denom"] == "L&CTEOS"
        assert lacteos["depto"] == "ALMACEN"
        assert lacteos["catego"] == "LACTEOS"
        assert None not in lacteos  # ningún campo sobrante sin nombre de columna

    def test_informa_que_no_encontro_entidades_desconocidas(self, fixture_categorias):
        _, desconocidas = leer_csv_pos(fixture_categorias)

        assert desconocidas == set()


class TestLeerSpreadsheetml:
    """El POS llama .xls a dos formatos, y este es el XML de Excel 2003: ni
    xlrd ni openpyxl lo abren."""

    def test_usa_la_cabecera_y_saltea_la_decoracion(self, fixture_listado):
        filas = leer_spreadsheetml(fixture_listado)

        assert filas[0]["Barra"] == "7790000000001"
        assert filas[0]["Categoría"] == "LACTEOS"
        assert not any(f.get("Barra") == "Empresa:UN NEGOCIO" for f in filas)

    def test_ignora_las_filas_de_otro_ancho(self, fixture_listado):
        """El pie de totales tiene tres celdas; rellenarlo correría columnas."""
        filas = leer_spreadsheetml(fixture_listado)

        assert len(filas) == 6
        assert all(len(f) == 9 for f in filas)

    def test_desescapa_las_entidades_xml(self, fixture_listado):
        """En XML `&amp;` es escapado legítimo, no el defecto del CSV: hay que
        desescaparlo igual, o el nombre llega con la entidad cruda."""
        filas = leer_spreadsheetml(fixture_listado)
        por_barra = {f["Barra"]: f for f in filas}

        assert por_barra["7790000000002"]["Producto"] == "QUESO & CREMA"
        assert por_barra["7790000000003"]["Producto"] == "YOGUR BEBIBLE"


class TestParsearDecimal:
    def test_saca_la_coma_de_miles(self):
        assert str(parsear_decimal("2,196.15000")) == "2196.15000"

    def test_conserva_la_precision_del_pos(self):
        """Redondear a centavos sería un ajuste silencioso sobre 10.000 filas."""
        assert str(parsear_decimal("1,428.57400")) == "1428.57400"

    def test_distingue_vacio_de_cero(self):
        """Un 0 inventado distorsiona el capital inmovilizado y el ABC."""
        assert parsear_decimal("") is None
        assert parsear_decimal("  ") is None
        assert str(parsear_decimal("0.00000")) == "0.00000"

    def test_devuelve_none_si_no_es_un_numero(self):
        assert parsear_decimal("s/d") is None
