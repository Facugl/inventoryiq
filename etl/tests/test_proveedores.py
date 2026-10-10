"""Transformación de Mantenimiento_de_Proveedores_.csv al formato del backend."""

import csv

from inventoryiq_etl import decisiones
from inventoryiq_etl.fuentes import leer_csv_pos
from inventoryiq_etl.proveedores import COLUMNAS_DESTINO, convertir, transformar


def transformar_fixture(ruta):
    filas, _ = leer_csv_pos(ruta)
    return transformar(filas)


def por_id(destino):
    return {f["proveedor_id"]: f for f in destino}


class TestMapeo:
    def test_traduce_los_campos_al_formato_del_backend(self, fixture_proveedores):
        destino, _ = transformar_fixture(fixture_proveedores)
        lacteos = por_id(destino)["1"]

        assert lacteos["razon_social"] == "LACTEOS DEL SUR SA"
        assert lacteos["activo"] == "True"
        assert list(lacteos) == COLUMNAS_DESTINO

    def test_la_entidad_html_no_parte_la_razon_social(self, fixture_proveedores):
        """Sin reparar, el `;` de `&amp;` dejaba 'DISTRIBUIDORA M&amp' y corría el resto."""
        destino, _ = transformar_fixture(fixture_proveedores)

        assert por_id(destino)["2"]["razon_social"] == "DISTRIBUIDORA M&M"

    def test_aplica_el_lead_time_por_defecto(self, fixture_proveedores):
        """Decisión 4.3: el POS no lo registra. Se corrige después desde la pantalla."""
        destino, _ = transformar_fixture(fixture_proveedores)

        assert {f["lead_time_promedio_dias"] for f in destino} == {"3"}

    def test_la_condicion_de_pago_queda_vacia_en_vez_de_inventada(self, fixture_proveedores):
        destino, _ = transformar_fixture(fixture_proveedores)

        assert {f["condicion_pago"] for f in destino} == {""}


class TestProveedorSinAsignar:
    """Decisión 4.2: el 36% del catálogo real no declara proveedor."""

    def test_se_agrega_para_que_esos_productos_entren_al_sistema(self, fixture_proveedores):
        destino, reporte = transformar_fixture(fixture_proveedores)
        sin_asignar = por_id(destino)[decisiones.PROVEEDOR_SIN_ASIGNAR_ID]

        assert sin_asignar["razon_social"] == "Sin Proveedor"
        assert sin_asignar["lead_time_promedio_dias"] == decisiones.LEAD_TIME_DIAS
        assert any("no declaran uno" in i.motivo for i in reporte.ajustes)

    def test_conserva_el_literal_del_pos(self):
        """Productos va a resolver el proveedor por nombre: el export de
        precios no trae id. Conservar el literal evita un caso especial."""
        assert decisiones.PROVEEDOR_SIN_ASIGNAR_NOMBRE == "Sin Proveedor"

    def test_usa_el_id_que_el_pos_reserva_para_lo_mismo(self):
        assert decisiones.PROVEEDOR_SIN_ASIGNAR_ID == "0"

    def test_no_se_duplica_si_el_pos_ya_lo_registra(self, fixture_proveedores_con_sin_proveedor):
        """Se respeta el del POS, con su id real, en vez de agregar otro."""
        destino, reporte = transformar_fixture(fixture_proveedores_con_sin_proveedor)
        con_ese_nombre = [f for f in destino if f["razon_social"] == "Sin Proveedor"]

        assert len(con_ese_nombre) == 1
        assert con_ese_nombre[0]["proveedor_id"] == "7"
        assert not any("no declaran uno" in i.motivo for i in reporte.ajustes)

    def test_el_id_cero_esta_reservado(self, fixture_proveedores_id_cero):
        """Si el POS emitiera un proveedor con id 0, los productos sin
        proveedor terminarían apuntando a uno real. Se descarta y se avisa."""
        destino, reporte = transformar_fixture(fixture_proveedores_id_cero)

        assert por_id(destino)["0"]["razon_social"] == "Sin Proveedor"
        assert any("reservado" in i.motivo for i in reporte.descartes)


class TestFilasProblematicas:
    def test_descarta_las_que_no_se_pueden_representar(self, fixture_proveedores):
        destino, reporte = transformar_fixture(fixture_proveedores)
        motivos = [i.motivo for i in reporte.descartes]

        assert "razón social vacía" in motivos
        assert "Id duplicado" in motivos
        assert "Id no es un entero" in motivos
        assert "4" not in por_id(destino)
        assert "xx" not in por_id(destino)

    def test_el_duplicado_conserva_la_primera_aparicion(self, fixture_proveedores):
        destino, _ = transformar_fixture(fixture_proveedores)

        assert por_id(destino)["1"]["razon_social"] == "LACTEOS DEL SUR SA"


class TestReporte:
    def test_cuadra_contando_el_proveedor_agregado(self, fixture_proveedores):
        destino, reporte = transformar_fixture(fixture_proveedores)

        assert reporte.emitidas == len(destino)
        # Lo emitido son las filas que pasaron, más el proveedor sintético.
        assert reporte.emitidas == reporte.leidas - len(reporte.descartes) + 1


class TestEscritura:
    def test_escribe_un_csv_que_el_backend_puede_leer(self, fixture_proveedores, tmp_path):
        salida = tmp_path / "proveedores.csv"

        reporte = convertir(fixture_proveedores, salida)

        contenido = salida.read_text(encoding="utf-8")
        assert contenido.startswith(",".join(COLUMNAS_DESTINO))
        assert "\r\n" not in contenido
        assert "DISTRIBUIDORA M&M" in contenido

        leidas = list(csv.DictReader(salida.open(encoding="utf-8")))
        assert len(leidas) == reporte.emitidas
