"""El cableado que entra en juego cuando exista el slice de productos.

Productos está bloqueado (Sección 5.1 del documento), pero el armado del
catálogo ya tiene que contemplarlo: inventario necesita el catálogo generado
para descartar el stock de productos inexistentes. Estos tests simulan ese
archivo para fijar el comportamiento antes de que el slice llegue.
"""

import csv

from inventoryiq_etl.catalogo import verificar_integridad


def escribir_productos(carpeta, ids):
    cols = ["producto_id", "sku", "nombre", "categoria_id", "proveedor_id",
            "unidad_medida", "precio_costo", "precio_venta", "lead_time_dias", "activo"]
    with (carpeta / "productos.csv").open("w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=cols, lineterminator="\n")
        w.writeheader()
        for pid in ids:
            w.writerow(dict(producto_id=pid, sku=pid, nombre="x", categoria_id="1",
                            proveedor_id="0", unidad_medida="UN", precio_costo="1",
                            precio_venta="2", lead_time_dias="3", activo="True"))


class TestCuandoLlegueProductos:
    def test_las_ventas_simuladas_no_sirven_con_un_catalogo_real(self, catalogo_completo):
        """El hallazgo que motiva la Sección 5.3: las ventas versionadas
        apuntan a los productos simulados, no a los del POS. Sin un export
        real de ventas, el catálogo no cierra aunque productos exista."""
        escribir_productos(catalogo_completo, ["7790000000001", "7790000000002"])

        _, violaciones = verificar_integridad(catalogo_completo)
        rota = next(v for v in violaciones if v.clave.archivo == "ventas.csv"
                    and v.clave.columna == "producto_id")

        assert rota.faltantes == {"779"}

    def test_el_catalogo_cierra_cuando_todo_apunta_a_lo_mismo(self, catalogo_completo):
        escribir_productos(catalogo_completo, ["779"])

        faltantes, violaciones = verificar_integridad(catalogo_completo)

        assert faltantes == []
        assert violaciones == []
