# -*- coding: utf-8 -*-
"""
Mide que ocupa cada parte de los ZIP de campana reales de SLV-002 y simula las propuestas de
PLAN-Banco-Representativo.md §6: ZIP incremental con indice de hashes, diario compacto, tramas solo
en el ZIP de soporte y nivel de compresion. No toca la app: reconstruye los ZIP en memoria con
zipfile (DEFLATE, el mismo metodo que java.util.zip.ZipOutputStream de Campanas.exportar,
Campanas.java:419-451, que usa el nivel por defecto, 6).

Uso: python medir_zip_campana.py
"""
import csv
import hashlib
import io
import lzma
import os
import re
import zipfile
from collections import OrderedDict

AQUI = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(os.path.dirname(AQUI), "SLV-002", "campanas")
# Orden de exportacion de la misma campana (HUELLAS.txt): 12:00 -> 12:10 -> 12:27. La de 10:33 es de
# la app 3.6.5 y se importo en la de 12:00.
ZIPS = ["campana_SLV-002_20260919_103300.zip", "campana_SLV-002_20260919_120049.zip",
        "campana_SLV-002_20260919_121009.zip", "campana_SLV-002_20260919_122727.zip"]
CADENA = ZIPS[1:]


def parte(nombre):
    if nombre.startswith("tramas/"):
        return "tramas/"
    if nombre.startswith("actas/"):
        return "actas/"
    if nombre.startswith("diario_"):
        return "diario"
    return nombre


def leer(zname):
    z = zipfile.ZipFile(os.path.join(DIR, zname))
    return OrderedDict((i.filename, (z.read(i.filename), i.compress_size)) for i in z.infolist())


def zip_bytes(entradas, nivel=6):
    b = io.BytesIO()
    with zipfile.ZipFile(b, "w", zipfile.ZIP_DEFLATED, compresslevel=nivel) as z:
        for n, d in entradas.items():
            z.writestr(n, d)
    return len(b.getvalue())


def sha(d):
    return hashlib.sha256(d).hexdigest()


def diario_compacto(texto):
    """
    Diario compacto (propuesta): el mismo contenido, sin repetir lo que ya dice la linea SERIE.
    DISPARO,S001,1,"2026-09-19T09:57:06-0500","::956",956,1  ->  D,1,3,956
    (n, segundos desde la linea SERIE, x). La respuesta bruta solo se escribe si no es "::<x>", y la
    marca final solo si no es 1. Todo lo demas, igual.
    """
    out, t0, fmt = [], None, re.compile(r'^DISPARO,(S\d+),(\d+),"([^"]+)","([^"]*)",([^,]*),(\d+)$')
    from datetime import datetime
    for l in texto.splitlines():
        if l.startswith("SERIE,"):
            m = re.search(r'"(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d[-+]\d{4})"', l)
            t0 = datetime.strptime(m.group(1), "%Y-%m-%dT%H:%M:%S%z") if m else None
            out.append(l)
            continue
        m = fmt.match(l)
        if m and t0 is not None:
            t = datetime.strptime(m.group(3), "%Y-%m-%dT%H:%M:%S%z")
            bruta, x, flag = m.group(4), m.group(5), m.group(6)
            campos = ["D", m.group(2), str(int((t - t0).total_seconds())), x]
            if bruta != "::" + x or flag != "1":
                campos += [bruta, flag]
            out.append(",".join(campos))
        else:
            out.append(l)
    return ("\n".join(out) + "\n").encode("utf-8")


def main():
    datos = {z: leer(z) for z in ZIPS}
    print("## Tamano por parte (bytes): sin comprimir / comprimido en el ZIP real")
    partes = ["campana.csv", "resumen.txt", "diario", "pruebas.txt", "actas/", "tramas/"]
    print("| ZIP | tamano | " + " | ".join(partes) + " |")
    print("| :--- | ---: | " + " | ".join("---:" for _ in partes) + " |")
    for z in ZIPS:
        acc = OrderedDict((p, [0, 0]) for p in partes)
        for n, (d, c) in datos[z].items():
            acc[parte(n)][0] += len(d)
            acc[parte(n)][1] += c
        tam = os.path.getsize(os.path.join(DIR, z))
        print("| %s | %d | %s |" % (z[16:31], tam, " | ".join("%d / %d" % tuple(v) for v in acc.values())))

    print("\n## Lo que se repite de una exportacion a la siguiente (misma campana)")
    for a, b in zip(CADENA, CADENA[1:]):
        prev, cur = datos[a], datos[b]
        igual = crece = nuevo = 0
        comp_igual = 0
        for n, (d, c) in cur.items():
            if n in prev and prev[n][0] == d:
                igual += len(d)
                comp_igual += c
            elif n in prev and d.startswith(prev[n][0]):
                crece += len(d) - len(prev[n][0])
            else:
                nuevo += len(d)
        tam = os.path.getsize(os.path.join(DIR, b))
        print("- %s -> %s: identico a la anterior %d B sin comprimir (%d B comprimidos, %.0f %% del ZIP); "
              "anadido al final de un fichero que ya estaba %d B; nuevo o cambiado %d B"
              % (a[16:31], b[16:31], igual, comp_igual, 100.0 * comp_igual / tam, crece, nuevo))

    print("\n## Simulacion sobre el ZIP de las 12:27 (tamanos en bytes)")
    cur = datos[CADENA[-1]]
    prev = datos[CADENA[-2]]
    base = OrderedDict((n, d) for n, (d, c) in cur.items())
    real = os.path.getsize(os.path.join(DIR, CADENA[-1]))
    print("- ZIP real: %d" % real)
    print("- Reconstruido con zipfile, nivel 6: %d (control del metodo)" % zip_bytes(base, 6))
    print("- Nivel 9: %d" % zip_bytes(base, 9))
    tar = b"".join(base.values())
    print("- Referencia fuera de java.util.zip: todo en un solo flujo xz: %d" % len(lzma.compress(tar, preset=9)))

    # diario compacto
    dn = [n for n in base if n.startswith("diario_")][0]
    dc = diario_compacto(base[dn].decode("utf-8"))
    b2 = OrderedDict(base)
    b2[dn] = dc
    print("- Diario compacto: %d -> %d B sin comprimir; ZIP %d" % (len(base[dn]), len(dc), zip_bytes(b2, 6)))

    # sin campana.csv (derivable del diario: ImportadorCampana.java:66-67 prefiere el diario)
    b3 = OrderedDict((n, d) for n, d in base.items() if n != "campana.csv")
    print("- Sin campana.csv (se regenera del diario): %d" % zip_bytes(b3, 6))

    # tramas fuera (solo en soporte) con indice de hashes
    indice = "".join("%s  %d  %s\n" % (sha(d), len(d), n) for n, d in base.items() if n.startswith("tramas/"))
    b4 = OrderedDict((n, d) for n, d in b3.items() if not n.startswith("tramas/"))
    b4["indice_tramas.sha256"] = indice.encode()
    print("- Sin campana.csv y sin tramas (indice SHA-256 de tramas en su lugar): %d" % zip_bytes(b4, 6))
    b5 = OrderedDict(b4)
    b5[dn] = dc
    print("- Lo anterior con diario compacto: %d" % zip_bytes(b5, 6))

    # incremental frente a la exportacion anterior (12:10): lo nuevo y la cola de lo que crecio
    inc = OrderedDict()
    man = ["# indice de la exportacion: sha256  bytes  desde  nombre (desde>0: tramo anadido)",
           "# anterior: %s sha256 %s" % (CADENA[-2], sha(open(os.path.join(DIR, CADENA[-2]), "rb").read()))]
    for n, d in base.items():
        if n in prev and prev[n][0] == d:
            man.append("%s  %d  0  %s  (sin cambios: en el ZIP anterior)" % (sha(d), len(d), n))
        elif n in prev and d.startswith(prev[n][0]):
            desde = len(prev[n][0])
            inc[n + ".desde_%d" % desde] = d[desde:]
            man.append("%s  %d  %d  %s" % (sha(d), len(d), desde, n))
        else:
            inc[n] = d
            man.append("%s  %d  0  %s" % (sha(d), len(d), n))
    inc["indice.sha256"] = ("\n".join(man) + "\n").encode()
    print("- Incremental frente a la de las 12:10 (nuevo + tramos anadidos + indice): %d" % zip_bytes(inc, 6))
    inc2 = OrderedDict((n, d) for n, d in inc.items() if not n.startswith("tramas/") and not n.startswith("campana.csv"))
    print("- Incremental, sin tramas ni campana.csv: %d" % zip_bytes(inc2, 6))
    print("  entradas del incremental: " + ", ".join("%s (%d B)" % (n, len(d)) for n, d in inc.items()))


if __name__ == "__main__":
    main()
