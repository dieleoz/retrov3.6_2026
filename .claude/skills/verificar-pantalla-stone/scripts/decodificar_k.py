# -*- coding: utf-8 -*-
"""
decodificar_k.py - Decodifica tramas de la pantalla STONE de 2.a generacion
(protocolo binario A5 5A) y dice que codigo llega a bufferPantalla[8].

Entradas aceptadas (argumento o entrada estandar):
  - la respuesta de la orden #K# de la V3.6:   #K,<n>,<hex1>;<hex2>;...#
    (PROTOCOLO-V3.6.md, "Registro de la pantalla STONE")
  - un volcado hexadecimal cualquiera de un sniffer en la linea TX de la STONE
    ("A5 5A 06 83 ..."), en una o varias lineas: se corta por la cabecera A5 5A.

Con --fw <carpeta .X> se anota cada codigo con la linea del firmware que lo
atiende y su comentario (ecuacionesCalibracion.c). Sin --fw solo decodifica.

Uso:
  python decodificar_k.py "#K,3,A55A06830000011002;A55A0683000001100E#" --fw <carpeta .X>
  python decodificar_k.py --fw <carpeta .X> < volcado.txt
"""
import argparse
import os
import re
import sys


def tabla_firmware(carpeta):
    """codigo -> 'archivo:linea  comentario' leyendo el fuente, no de memoria."""
    tabla = {}
    for raiz, dirs, archivos in os.walk(carpeta):
        dirs[:] = [d for d in dirs if d not in ("build", "dist", "debug", "nbproject")]
        for a in archivos:
            if not a.lower().endswith(".c"):
                continue
            ruta = os.path.join(raiz, a)
            with open(ruta, "rb") as f:
                for i, linea in enumerate(f.read().decode("latin-1").splitlines(), 1):
                    if linea.lstrip().startswith("//"):
                        continue
                    m = re.search(r"bufferPantalla\s*\[\s*8\s*\]\s*==\s*(0[xX][0-9a-fA-F]+|\d+)", linea)
                    if m:
                        comentario = linea.split("//", 1)[1].strip() if "//" in linea else ""
                        tabla[int(m.group(1), 0)] = "%s:%d  %s" % (a, i, comentario)
    return tabla


def tramas(texto):
    texto = texto.strip()
    m = re.match(r"#K,(\d+),(.*)#\s*$", texto, re.S)
    total = None
    if m:
        total = int(m.group(1))
        trozos = [t for t in m.group(2).split(";") if t.strip()]
        crudos = [bytes.fromhex(re.sub(r"[^0-9A-Fa-f]", "", t)) for t in trozos]
    else:
        todo = bytes.fromhex(re.sub(r"[^0-9A-Fa-f]", "", texto))
        crudos = [b"\xA5\x5A" + p for p in todo.split(b"\xA5\x5A") if p]
    return total, crudos


def decodificar(b):
    if len(b) < 4 or b[0] != 0xA5 or b[1] != 0x5A:
        return None, "sin cabecera A5 5A: %s" % b.hex(" ").upper()
    largo, orden = b[2], b[3]
    nota = ""
    if len(b) != largo + 3:
        nota = "  [largo declarado %d, recibidos %d]" % (largo, len(b) - 3)
    if orden == 0x83 and len(b) >= 9:
        dire = (b[4] << 8) | b[5]
        palabras = b[6]
        valor = (b[7] << 8) | b[8]
        return b[8], "0x83 lectura: dir 0x%04X, %d palabra(s), valor 0x%04X -> bufferPantalla[8] = 0x%02X%s" % (
            dire, palabras, valor, b[8], nota)
    cod = b[8] if len(b) > 8 else None
    return cod, "orden 0x%02X: %s%s" % (orden, b.hex(" ").upper(), nota)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("texto", nargs="?")
    p.add_argument("--fw")
    a = p.parse_args()
    texto = a.texto if a.texto else sys.stdin.read()
    tabla = tabla_firmware(a.fw) if a.fw else {}
    total, crudos = tramas(texto)
    if total is not None:
        print("Tramas vistas desde el arranque: %d; en el registro: %d" % (total, len(crudos)))
    for i, b in enumerate(crudos, 1):
        cod, desc = decodificar(b)
        print("%2d  %s" % (i, desc))
        if a.fw and cod is not None:
            print("      firmware: %s" % tabla.get(cod, "SIN RAMA: el firmware no atiende 0x%02X" % cod))


if __name__ == "__main__":
    main()
