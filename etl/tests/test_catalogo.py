"""Armado y verificación del catálogo completo."""

from datetime import date

from helpers import escribir
from inventoryiq_etl.catalogo import (
    ARCHIVOS_REQUERIDOS,
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

        assert {r.entidad for r in resultado.reportes} == {"categorias", "proveedores", "inventario"}
        assert set(resultado.arrastrados) == {"sucursales.csv", "ventas.csv"}
        assert (tmp_path / "salida" / "categorias.csv").is_file()

    def test_hoy_el_catalogo_no_cierra_porque_falta_productos(
        self, tmp_path, fixture_categorias, fixture_proveedores, fixture_stock, catalogo_completo
    ):
        """Es el estado real del proyecto, y el comando tiene que decirlo."""
        desde = tmp_path / "export"
        desde.mkdir()
        (desde / "categorias_xrp.csv").write_bytes(fixture_categorias.read_bytes())
        (desde / "Mantenimiento_de_Proveedores_.csv").write_bytes(fixture_proveedores.read_bytes())
        (desde / "Consulta_de_stock_xrp.xls").write_bytes(fixture_stock.read_bytes())

        resultado = migrar(desde, tmp_path / "salida", catalogo_completo, FECHA)

        assert not resultado.sirve
        assert "productos.csv" in resultado.faltantes
        assert "NO UTILIZABLE" in resultado.resumen()

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
