"""Armado y verificación del catálogo completo."""

from datetime import date

from helpers import escribir
from inventoryiq_etl.catalogo import (
    ARCHIVOS_REQUERIDOS,
    DIAS_DE_HISTORIA_RAZONABLE,
    dias_utiles,
    migrar,
    verificar_integridad,
)

FECHA = date(2026, 9, 6)


class TestVerificarIntegridad:
    def test_un_catalogo_cerrado_no_tiene_observaciones(self, catalogo_completo):
        faltantes, violaciones = verificar_integridad(catalogo_completo)

        assert faltantes == []
        assert violaciones == []

    def test_delata_un_archivo_que_falta(self, catalogo_completo):
        (catalogo_completo / "productos.csv").unlink()

        faltantes, _ = verificar_integridad(catalogo_completo)

        assert "productos.csv" in faltantes

    def test_delata_un_producto_que_apunta_a_una_categoria_inexistente(self, catalogo_completo):
        """Es exactamente lo que hace fallar el arranque del backend."""
        escribir(catalogo_completo, "categorias.csv",
                 ["categoria_id", "nombre", "categoria_padre_id", "umbral_max_cobertura_dias", "dias_cobertura_extra_default"],
                 [{"categoria_id": "1", "nombre": "ALMACEN", "categoria_padre_id": "", "umbral_max_cobertura_dias": "20.0", "dias_cobertura_extra_default": "4.0"}])

        _, violaciones = verificar_integridad(catalogo_completo)

        assert any(v.clave.archivo == "productos.csv" and v.clave.columna == "categoria_id"
                   for v in violaciones)

    def test_delata_ventas_que_apuntan_a_productos_de_otro_catalogo(self, catalogo_completo):
        """El caso real: productos reales con ventas simuladas al lado."""
        escribir(catalogo_completo, "ventas.csv",
                 ["venta_id", "fecha", "producto_id", "sucursal_id", "unidades_vendidas", "importe_total"],
                 [{"venta_id": "1", "fecha": "2026-08-01", "producto_id": "1001", "sucursal_id": "1", "unidades_vendidas": "3", "importe_total": "45"}])

        _, violaciones = verificar_integridad(catalogo_completo)
        rota = next(v for v in violaciones if v.clave.archivo == "ventas.csv")

        assert rota.faltantes == {"1001"}
        assert "1001" in str(rota)

    def test_una_categoria_raiz_no_es_una_referencia_colgada(self, catalogo_completo):
        """categoria_padre_id vacío significa raíz, no 'apunta a la nada'."""
        _, violaciones = verificar_integridad(catalogo_completo)

        assert not any(v.clave.columna == "categoria_padre_id" for v in violaciones)

    def test_una_referencia_colgada_cuenta_filas_y_valores(self, catalogo_completo):
        escribir(catalogo_completo, "inventario.csv",
                 ["inventario_id", "fecha_snapshot", "producto_id", "sucursal_id", "stock_actual", "stock_en_transito"],
                 [{"inventario_id": str(i), "fecha_snapshot": "2026-09-06", "producto_id": p, "sucursal_id": "1", "stock_actual": "5", "stock_en_transito": "0"}
                  for i, p in enumerate(["888", "888", "999"], start=1)])

        _, violaciones = verificar_integridad(catalogo_completo)
        rota = next(v for v in violaciones if v.clave.archivo == "inventario.csv"
                    and v.clave.columna == "producto_id")

        assert rota.filas == 3
        assert rota.faltantes == {"888", "999"}


class TestMigrar:
    def test_genera_las_entidades_y_arrastra_las_que_no_tienen_fuente(
        self, tmp_path, fixture_categorias, fixture_proveedores, fixture_stock, catalogo_completo
    ):
        desde = tmp_path / "export"
        desde.mkdir()
        (desde / "categorias_xrp.csv").write_bytes(fixture_categorias.read_bytes())
        (desde / "Mantenimiento_de_Proveedores_.csv").write_bytes(fixture_proveedores.read_bytes())
        (desde / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())

        resultado = migrar(desde, tmp_path / "salida", catalogo_completo, FECHA)

        assert {r.entidad for r in resultado.reportes} == {
            "categorias", "proveedores", "productos", "inventario", "ventas",
        }
        assert set(resultado.arrastrados) == {"sucursales.csv"}
        assert (tmp_path / "salida" / "categorias.csv").is_file()

    def test_sin_el_export_de_precios_falta_productos(
        self, tmp_path, fixture_categorias, fixture_proveedores, fixture_stock, catalogo_completo
    ):
        desde = tmp_path / "export"
        desde.mkdir()
        (desde / "categorias_xrp.csv").write_bytes(fixture_categorias.read_bytes())
        (desde / "Mantenimiento_de_Proveedores_.csv").write_bytes(fixture_proveedores.read_bytes())
        (desde / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())

        resultado = migrar(desde, tmp_path / "salida", catalogo_completo, FECHA)

        assert not resultado.sirve
        assert "productos.csv" in resultado.faltantes
        assert "NO UTILIZABLE" in resultado.resumen()

    def test_sin_listados_diarios_ventas_falta_en_vez_de_arrastrarse(
        self, tmp_path, export_pos, fixture_stock, catalogo_completo
    ):
        """Arrastrar el ventas.csv simulado sería peor que no tenerlo: el
        archivo se ACUMULA, así que la corrida siguiente tomaría esas ventas
        inventadas por datos ya cargados."""
        (export_pos / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())
        # El listado del fixture cubre un rango largo, no un día.

        resultado = migrar(export_pos, tmp_path / "salida", catalogo_completo, FECHA)

        assert "ventas.csv" in resultado.faltantes
        assert not resultado.sirve
        assert "ventas.csv" not in resultado.arrastrados

    def test_productos_se_genera_antes_que_inventario(
        self, tmp_path, export_pos, fixture_stock, catalogo_completo
    ):
        """El orden no es cosmético: inventario descarta el stock de productos
        que no están en el catálogo, y para eso necesita el catálogo escrito."""
        (export_pos / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())

        resultado = migrar(export_pos, tmp_path / "salida", catalogo_completo, FECHA)
        entidades = [r.entidad for r in resultado.reportes]
        inventario = next(r for r in resultado.reportes if r.entidad == "inventario")

        assert entidades.index("productos") < entidades.index("inventario")
        assert not any("integridad referencial sin verificar" in i.motivo
                       for i in inventario.ajustes)

    def test_avisa_si_falta_un_export_en_vez_de_escribir_a_medias(self, tmp_path, catalogo_completo):
        desde = tmp_path / "export"
        desde.mkdir()  # vacío

        resultado = migrar(desde, tmp_path / "salida", catalogo_completo, FECHA)

        assert not resultado.sirve
        assert all(r.emitidas == 0 for r in resultado.reportes)
        assert any("no está el export de origen" in i.motivo
                   for r in resultado.reportes for i in r.descartes)

    def test_no_toca_el_dataset_simulado(
        self, tmp_path, fixture_categorias, fixture_proveedores, fixture_stock, catalogo_completo
    ):
        desde = tmp_path / "export"
        desde.mkdir()
        (desde / "categorias_xrp.csv").write_bytes(fixture_categorias.read_bytes())
        (desde / "Mantenimiento_de_Proveedores_.csv").write_bytes(fixture_proveedores.read_bytes())
        (desde / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())
        antes = (catalogo_completo / "categorias.csv").read_bytes()

        migrar(desde, tmp_path / "salida", catalogo_completo, FECHA)

        assert (catalogo_completo / "categorias.csv").read_bytes() == antes


def test_los_archivos_requeridos_son_los_que_lee_el_backend():
    """Si un adaptador nuevo leyera otro CSV, esta lista quedaría corta."""
    assert set(ARCHIVOS_REQUERIDOS) == {
        "categorias.csv", "proveedores.csv", "productos.csv",
        "sucursales.csv", "ventas.csv", "inventario.csv",
    }


class TestHistorialSuficiente:
    """La integridad referencial no ve si hay con qué calcular. Un catálogo
    puede cerrar perfecto y dejar todas las pantallas vacías."""

    def _con_dias(self, carpeta, dias_venta, dias_stock):
        escribir(carpeta, "ventas.csv",
                 ["venta_id", "fecha", "producto_id", "sucursal_id", "unidades_vendidas", "importe_total"],
                 [{"venta_id": str(i), "fecha": d, "producto_id": "779", "sucursal_id": "1",
                   "unidades_vendidas": "1", "importe_total": "10"}
                  for i, d in enumerate(dias_venta, start=1)])
        escribir(carpeta, "inventario.csv",
                 ["inventario_id", "fecha_snapshot", "producto_id", "sucursal_id", "stock_actual", "stock_en_transito"],
                 [{"inventario_id": str(i), "fecha_snapshot": d, "producto_id": "779",
                   "sucursal_id": "1", "stock_actual": "5", "stock_en_transito": "0"}
                  for i, d in enumerate(dias_stock, start=1)])

    def test_un_dia_de_venta_sirve_solo_si_hay_snapshot_del_dia_anterior(self, tmp_path):
        """Es lo que exige DailySalesRecordAssembler para saber si hubo
        quiebre; sin eso descarta el producto entero."""
        self._con_dias(tmp_path, ["2026-09-07"], ["2026-09-06"])

        _, _, utiles = dias_utiles(tmp_path)

        assert utiles == {"2026-09-07"}

    def test_dias_que_no_se_cruzan_no_sirven(self, tmp_path):
        self._con_dias(tmp_path, ["2026-07-25"], ["2026-09-06"])

        dias_venta, dias_stock, utiles = dias_utiles(tmp_path)

        assert dias_venta and dias_stock
        assert utiles == set()

    def test_el_resultado_lo_dice_en_vez_de_cantar_victoria(self, tmp_path, catalogo_completo):
        """Sin esto el comando diría COMPLETO Y CONSISTENTE y el backend
        arrancaría con las pantallas vacías."""
        self._con_dias(catalogo_completo, ["2026-07-25"], ["2026-09-06"])
        desde = tmp_path / "export"
        desde.mkdir()

        resultado = migrar(desde, catalogo_completo, tmp_path / "sin-simulados", FECHA)

        assert resultado.advertencias
        assert not resultado.alcanza_para_calcular
        assert "ningún día de venta tiene un snapshot" in resultado.resumen()

    def test_con_historia_de_sobra_no_advierte(self, tmp_path):
        dias = [f"2026-0{m}-{d:02d}" for m in (1, 2) for d in range(1, 21)]
        self._con_dias(tmp_path, [f"2026-03-{d:02d}" for d in range(2, 32)],
                       [f"2026-03-{d:02d}" for d in range(1, 32)])

        _, _, utiles = dias_utiles(tmp_path)

        assert len(utiles) >= DIAS_DE_HISTORIA_RAZONABLE
