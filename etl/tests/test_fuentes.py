"""El lector de exports del POS: encoding, separador y entidades HTML."""

from inventoryiq_etl.fuentes import leer_csv_pos, reparar_entidades


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
