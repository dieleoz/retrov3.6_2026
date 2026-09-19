#!/usr/bin/env python3
"""Netlist de un fichero IPC-D-356 / 356A (netlist de prueba electrica que acompana a los gerbers).

Registros que se leen (columnas fijas, 1-indexadas, segun IPC-D-356A):
  P  NNAMEnnnnn <nombre largo>  -> alias de red (Proteus, Altium y otros acortan asi los nombres)
  317 / 327 / 367               -> punto de prueba: pasante / SMD / ...
     col 4-17  red   col 21-26  referencia   col 27 '-'   col 28-31  pin
  N/C en el campo de red        -> pad sin conexion
  referencia en blanco          -> via (se lista como VIA)
Limite del formato: la referencia tiene 6 caracteres. 'R16 (PRE...)' llega como 'R16 (P'.
Nombres NNAME 'GND=POWER' son el nombre de red de Proteus con su clase; no se reescriben.
Todo lo demas (C, 999, 0xx) se ignora.

Uso:
  python ipc356_netlist.py FICHERO.IPC --resumen
  python ipc356_netlist.py FICHERO.IPC --parte PICKIT
  python ipc356_netlist.py FICHERO.IPC --red MCLR
"""
import argparse
import sys
from collections import defaultdict


def cargar(ruta):
    alias = {}
    pads = []   # (red, ref, pin, linea)
    with open(ruta, encoding='latin-1') as f:
        for n, linea in enumerate(f, 1):
            linea = linea.rstrip('\r\n')
            if linea.startswith('P  NNAME'):
                partes = linea[3:].split(None, 1)
                if len(partes) == 2:
                    alias[partes[0]] = partes[1].strip()
            elif linea[:3] in ('317', '327', '367') and len(linea) > 27:
                red = linea[3:17].strip()
                ref = linea[20:26].strip() or 'VIA'   # referencia en blanco = via / punto sin componente
                pin = linea[27:31].strip() if linea[26] == '-' else linea[26:31].strip()
                pads.append((red, ref, pin, n))
    return alias, pads


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('ipc')
    ap.add_argument('--parte', action='append', default=[])
    ap.add_argument('--red', action='append', default=[])
    ap.add_argument('--resumen', action='store_true')
    a = ap.parse_args(argv)
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    alias, pads = cargar(a.ipc)
    nombre = lambda r: alias.get(r, r)
    por_red = defaultdict(list)
    for red, ref, pin, n in pads:
        if red != 'N/C':
            por_red[nombre(red)].append((ref, pin, n))
    if a.resumen or not (a.parte or a.red):
        refs = sorted({p[1] for p in pads})
        print(f"{a.ipc}: {len(pads)} pads, {len(por_red)} redes, {len(refs)} referencias, "
              f"{sum(1 for p in pads if p[0] == 'N/C')} pads N/C")
    for parte in a.parte:
        filas = [p for p in pads if p[1] == parte]
        print(f"\n## {parte}" + ('' if filas else '  -- no aparece. Negativo: comprobar con grep.'))
        for red, ref, pin, n in sorted(filas, key=lambda p: (len(p[2]), p[2])):
            otros = [f"{r}-{p}" for r, p, _ in por_red.get(nombre(red), [])
                     if r not in (parte, 'VIA')] if red != 'N/C' else []
            print(f"  pin {pin:>4}  red {nombre(red):<16} linea {n:<5} con: {', '.join(otros)}")
    for red in a.red:
        miembros = por_red.get(red, [])
        print(f"\n## Red {red}" + ('' if miembros else '  -- no aparece. Negativo: comprobar con grep.'))
        for ref, pin, n in sorted(miembros):
            print(f"  {ref}-{pin}  linea {n}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
