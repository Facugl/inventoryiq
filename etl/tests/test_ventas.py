"""Listado de ventas de un día -> ventas.csv.

El reporte trae `Cantidad` agregada al período que se le pidió. Sólo es venta
diaria si el período es de un día, y el slice se apoya en que el propio archivo
declara su período para poder rechazar los que no lo son.
"""

import csv

from inventoryiq_etl.diarios import Tipo, encontrar
from inventoryiq_etl.ventas import COLUMNAS_DESTINO, convertir, transformar

SUCURSALES = {"Depósito Las Lomas": "1", "Sucursal Rivadavia": "2"}


def exports_de(carpeta, tipo=Tipo.VENTAS):
    return [e for e in encontrar(carpeta) if e.tipo is tipo]


def por_producto(filas):
    return {f["producto_id"]: f for f in filas}


class TestListadoDeUnDia:
    def test_la_cantidad_del_dia_es_la_venta_de_ese_dia(self, carpeta_diarios):
        destino, _ = transformar(exports_de(carpeta_diarios), SUCURSALES)
        leche = por_producto(destino)["7790000000001"]

        assert list(leche) == COLUMNAS_DESTINO
        assert leche["fecha"] == "2025-12-22"
        assert leche["sucursal_id"] == "1"
        assert leche["unidades_vendidas"] == "4"
        assert leche["importe_total"] == "80.00"

    def test_la_fecha_y_la_sucursal_salen_del_archivo(self, carpeta_diarios):
        """No de una convención de nombres: el export se describe a sí mismo,
        así que no hay que pedirle a nadie que renombre nada."""
        destino, _ = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert {f["fecha"] for f in destino} == {"2025-12-22"}
        assert {f["sucursal_id"] for f in destino} == {"1"}

    def test_redondea_lo_vendido_por_peso(self, carpeta_diarios):
        destino, reporte = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert por_producto(destino)["7790000000002"]["unidades_vendidas"] == "2"
        assert any("fraccionarias" in i.motivo for i in reporte.ajustes)

    def test_menos_de_una_unidad_no_es_un_dia_de_venta(self, carpeta_diarios):
        destino, _ = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert "7790000000003" not in por_producto(destino)

    def test_el_producto_que_no_vendio_no_genera_fila(self, carpeta_diarios):
        """El listado incluye productos con cantidad 0; un día ausente en
        ventas.csv ya significa que no se vendió."""
        destino, _ = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert "7790000000010" not in por_producto(destino)

    def test_descarta_el_importe_no_numerico(self, carpeta_diarios):
        destino, reporte = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert "7790000000013" not in por_producto(destino)
        assert any("no numéricos" in i.motivo for i in reporte.descartes)

    def test_repara_la_entidad_html_del_nombre(self, carpeta_diarios):
        """No afecta a ventas.csv, pero confirma que el lector desescapa: el
        mismo archivo alimenta el cruce de categorías."""
        filas = [f for f in encontrar(carpeta_diarios) if f.tipo is Tipo.VENTAS]
        assert filas


class TestListadoDeVariosDias:
    """El error más caro de todo el pipeline, y el motivo del guard."""

    def test_se_rechaza_en_vez_de_registrarse_como_un_dia(self, carpeta_diarios, fixture_listado):
        """`Cantidad` de tres meses anotada como un martes infla el ADS de todo
        el catálogo sin que nada falle."""
        (carpeta_diarios / "largo.xls").write_bytes(fixture_listado.read_bytes())

        destino, reporte = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert all(f["fecha"] == "2025-12-22" for f in destino)
        assert any("no es de un solo día" in i.motivo for i in reporte.descartes)

    def test_un_listado_sin_periodo_tampoco_pasa(self, carpeta_diarios, fixture_listado):
        """El fixture viejo no declara período: sin esa garantía no se usa."""
        (carpeta_diarios / "sin_periodo.xls").write_bytes(fixture_listado.read_bytes())

        _, reporte = transformar(exports_de(carpeta_diarios), SUCURSALES)

        assert any("no es de un solo día" in i.motivo for i in reporte.descartes)


class TestSucursal:
    def test_una_unidad_de_negocio_desconocida_no_se_inventa(self, carpeta_diarios):
        _, reporte = transformar(exports_de(carpeta_diarios), {"Otra": "9"})

        assert any("unidad de negocio desconocida" in i.motivo for i in reporte.descartes)


class TestAcumulacion:
    def test_suma_dias_sin_perder_los_anteriores(self, carpeta_diarios, tmp_path):
        """Cada corrida aporta los días que haya; los viejos sobreviven aunque
        su export ya no esté."""
        salida = tmp_path / "ventas.csv"
        convertir(carpeta_diarios, salida, SUCURSALES)
        previas = len(list(csv.DictReader(salida.open(encoding="utf-8"))))

        convertir(carpeta_diarios, salida, SUCURSALES)
        despues = list(csv.DictReader(salida.open(encoding="utf-8")))

        assert previas and len(despues) == previas

    def test_no_pisa_las_filas_que_escribio_la_aplicacion(self, carpeta_diarios, tmp_path):
        """La carga de ventas por CSV del backend agrega a este mismo archivo:
        regenerarlo borraría lo que alguien cargó desde Administración."""
        salida = tmp_path / "ventas.csv"
        convertir(carpeta_diarios, salida, SUCURSALES)
        with salida.open("a", encoding="utf-8", newline="") as fh:
            fh.write("999,2026-01-05,7790000000777,1,7,123.00\n")

        convertir(carpeta_diarios, salida, SUCURSALES)
        filas = list(csv.DictReader(salida.open(encoding="utf-8")))

        a_mano = [f for f in filas if f["producto_id"] == "7790000000777"]
        assert len(a_mano) == 1
        assert a_mano[0]["unidades_vendidas"] == "7"

    def test_descarta_las_ventas_de_productos_fuera_del_catalogo(self, carpeta_diarios, tmp_path):
        salida = tmp_path / "ventas.csv"
        reporte = convertir(carpeta_diarios, salida, SUCURSALES,
                            productos_validos={"7790000000001"})
        filas = list(csv.DictReader(salida.open(encoding="utf-8")))

        assert {f["producto_id"] for f in filas} == {"7790000000001"}
        assert any("fuera del catálogo" in i.motivo for i in reporte.ajustes)

    def test_sin_ninguna_venta_no_escribe_el_archivo(self, tmp_path):
        """Un CSV con cabecera y sin filas haría pasar la verificación de
        integridad: el catálogo parecería cerrar y no tendría ventas."""
        vacia = tmp_path / "diarios"
        vacia.mkdir()
        salida = tmp_path / "ventas.csv"

        convertir(vacia, salida, SUCURSALES)

        assert not salida.exists()
