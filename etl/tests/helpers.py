"""Utilidades compartidas por los tests."""

import csv


def escribir(carpeta, nombre, columnas, filas):
    carpeta.mkdir(parents=True, exist_ok=True)
    with (carpeta / nombre).open("w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=columnas, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)
