#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cálculos de 06_Calibracion/SLV-002/COTEJO-Formulas-Historicas.md (19-sep-2026).

EN PAPEL: no escribe nada en el equipo ni en ningún otro fichero. Imprime por pantalla.

Qué hace:
  1. Reajusta, con los datos de la hoja «concesion sabana occidente» de
     06_Calibracion/2020_calibraciones VERTICALES.xlsx, las 8 ecuaciones distintas de fábrica de
     ecuacionesCalibracion.c (2020) y dice qué filas y qué grado reproducen cada una.
  2. Calcula la sensibilidad k = R / (x - x_oscuro) de cada patrón de 2020 (hojas «concesion sabana
     occidente», oscuro 488, y «coviandina», oscuro 450) y de SLV-002 (oscuro 565,36).
  3. Pendiente local dR/dx de las curvas de fábrica en la x medida en SLV-002.
  4. Código 5 (azul intenso): recta anclada en el oscuro con P17-P19 (series aceptadas de 10:33,
     campana.csv del ZIP de 12:27), misma fórmula que Ajuste.anclada (Ajuste.java:99), respuesta
     float32 y truncado como el firmware, y comparación con la fábrica.

Uso (desde la raíz del repositorio D:\\IT\\P_RetroVertical_V3.6):
  python 06_Calibracion/SLV-002/tools/cotejo_formulas_historicas.py
Necesita numpy y openpyxl.
"""
import csv
import hashlib
import io
import os
import statistics as st
import zipfile

import numpy as np
import openpyxl

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
HOJA_2020 = os.path.join(RAIZ, "06_Calibracion", "2020_calibraciones VERTICALES.xlsx")
ZIP_1227 = os.path.join(RAIZ, "06_Calibracion", "SLV-002", "campanas",
                        "campana_SLV-002_20260919_122727.zip")
MD5_1227 = "ce1f35fc64439cbb602d014b725fadfb"   # HUELLAS.txt
X_OSCURO = 565.36                                # REFORMULACION §2.1, S060, 45 disparos

# ecuacionesCalibracion.c de 2020 (base_2020_d089f962), coeficientes (c3, c2, c1, c0) tal cual
FABRICA = {
    "amarilloIntenso :4 (2)": (-0.000000073, 0.000282508, 0.124075350, -130),
    "blancoIntenso :7 (1)": (0, -0.000086541, 0.619668128, -303),
    "naranja :10,:32 (6,d)": (0, 0.000090731, 0.001064329, -21),
    "rojoIntenso :13 (4)": (0.000000147, -0.000668624, 1.082394050, -393),
    "azul :16,:38 (5,c)": (0.000000026, -0.000018402, 0.102152670, -49),
    "verde :19,:41 (3,a)": (0.000000163, -0.000756695, 1.213142724, -442),
    "amarilloOpaco :26 (8)": (0.000000086, -0.000299026, 0.446572329, -161),
    "blancoOpaco :29 (7)": (0, 0.000087404, 0.013617682, -27),
    "rojoOpaco :35 (b)": (0, 0.000113098, -0.076130418, 10),
}

# Filas de la hoja «concesion sabana occidente» (columna C = x «valor lcd», D = R «VALOR ESPERADO»)
GRUPOS = [
    ("amarilloOpaco :26 (8)", 2, 8, 3),
    ("amarilloIntenso :4 (2)", 9, 15, 3),
    ("blancoOpaco :29 (7)", 16, 19, 2),
    ("blancoIntenso :7 (1)", 20, 26, 2),
    ("naranja :10,:32 (6,d)", 27, 33, 2),
    ("rojoOpaco :35 (b)", 34, 38, 2),
    ("rojoIntenso :13 (4)", 39, 44, 3),
    ("azul :16,:38 (5,c)", 45, 51, 3),
    ("verde :19,:41 (3,a)", 52, 60, 3),
]


def f32(v):
    return float(np.float32(v))


def r_firmware(c, x):
    """Mismo orden que ecuacionesCalibracion.c / aplicarEcuacion (calibracion_v36.c:229-233), en float32."""
    c3, c2, c1, c0 = (np.float32(v) for v in c)
    x = np.float32(x)
    r = np.float32(np.float32(np.float32(c3 * x) * x) * x)
    r = np.float32(r + np.float32(np.float32(c2 * x) * x))
    r = np.float32(r + np.float32(c1 * x))
    r = np.float32(r + c0)
    return float(r)


def evaluar(c, x):
    c3, c2, c1, c0 = c
    return c3 * x ** 3 + c2 * x ** 2 + c1 * x + c0


def pendiente(c, x):
    c3, c2, c1, _ = c
    return 3 * c3 * x ** 2 + 2 * c2 * x + c1


def filas(ws, a, b):
    out = []
    for r in range(a, b + 1):
        x, y = ws.cell(r, 3).value, ws.cell(r, 4).value
        if isinstance(x, (int, float)) and isinstance(y, (int, float)):
            out.append((r, ws.cell(r, 1).value, float(x), float(y)))
    return out


def seccion1(wb):
    print("## 1. Reajuste de las ecuaciones de fábrica desde la hoja de 2020")
    ws = wb["concesion sabana occidente"]
    for nombre, a, b, g in GRUPOS:
        p = filas(ws, a, b)
        x = np.array([q[2] for q in p])
        y = np.array([q[3] for q in p])
        c = np.polyfit(x, y, g)
        c = tuple([0.0] * (3 - g) + list(c))
        fw = FABRICA[nombre]
        dif = max(abs(evaluar(c, v) - evaluar(fw, v)) for v in np.arange(450, 3001, 10))
        reales = [q for q in p if q[1] is not None]
        print(f"- {nombre}: filas {a}-{b}, grado {g}, puntos {[(q[0], q[2], q[3]) for q in p]}")
        print(f"    reajuste c3..c0 = {c}")
        print(f"    |R reajuste - R firmware| máx. en x 450-3000 = {dif:.2f} (el firmware copia la etiqueta de Excel con 9 decimales, A64-A85)")
        print(f"    patrones reales: {len(reales)}, R máx. real = {max(q[3] for q in reales):.2f}")


def seccion2(wb):
    print("\n## 2. Sensibilidad k = R / (x - x_oscuro) en 2020")
    for hoja, x0 in (("concesion sabana occidente", 488.0), ("coviandina", 450.0)):
        ws = wb[hoja]
        print(f"### {hoja} (oscuro {x0:.0f}, celda C2/C9/.../C45)")
        grupo = None
        acum = {}
        for r in range(3, 60):
            b = ws.cell(r, 2).value
            if b:
                grupo = b
            a, x, y = ws.cell(r, 1).value, ws.cell(r, 3).value, ws.cell(r, 4).value
            if a and isinstance(x, (int, float)) and isinstance(y, (int, float)) and y > 0 and x > x0:
                acum.setdefault(grupo, []).append((x - x0, y))
        for g, v in acum.items():
            dx = np.array([q[0] for q in v])
            y = np.array([q[1] for q in v])
            k = float(np.sum(y * dx) / np.sum(dx ** 2))
            print(f"  recta anclada en {x0:.0f} (sólo patrones reales) {g:12s}: c1 = {k:.4f}, "
                  f"n = {len(v)}, dx {dx.min():.0f}-{dx.max():.0f}, R {y.min():.1f}-{y.max():.1f}")
        grupo = None
        for r in range(3, 60):
            b = ws.cell(r, 2).value
            if b:
                grupo = b
            a, x, y = ws.cell(r, 1).value, ws.cell(r, 3).value, ws.cell(r, 4).value
            if a and isinstance(x, (int, float)) and isinstance(y, (int, float)) and y > 0 and x > x0:
                print(f"  {grupo:12s} {a:6s} fila {r:2d}: x={x:6.0f} R={y:8.3f} dx={x - x0:6.0f} "
                      f"k={y / (x - x0):.3f}")


def campana_1227():
    with open(ZIP_1227, "rb") as fh:
        datos = fh.read()
    md5 = hashlib.md5(datos).hexdigest()
    assert md5 == MD5_1227, f"md5 del ZIP de 12:27 distinto: {md5}"
    with zipfile.ZipFile(io.BytesIO(datos)) as z:
        txt = z.read("campana.csv").decode("utf-8")
    return list(csv.DictReader(io.StringIO(txt)))


def seccion3y4():
    global X_OSCURO
    filas_c = campana_1227()
    pat = {}
    for r in filas_c:
        if r["aceptada"] == "1" and r["elegida"] == "1" and r["descartado"] == "0":
            pat.setdefault(r["patron"], {"cert": float(r["valor_certificado"]), "tipo": r["tipo_lamina"],
                                         "color": r["color"], "serie": r["serie_id"], "x": []})
            pat[r["patron"]]["x"].append(float(r["x"]))
    osc = [float(r["x"]) for r in filas_c if r["serie_id"] == "S060"]
    print(f"\nOscuro S060 (campana.csv de 12:27): n = {len(osc)}, x media = {st.mean(osc):.4f}")
    X_OSCURO = st.mean(osc)   # 565,3556: la de REFORMULACION §2.1 sin redondear
    print("\n## 3. SLV-002: k = R / (x - 565,36) y pendiente local de la fábrica (series de 10:33)")
    cod = {("rojo", "XI"): "rojoIntenso :13 (4)", ("rojo", "IV"): "rojoIntenso :13 (4)",
           ("rojo", "I"): "rojoOpaco :35 (b)", ("azul", "XI"): "azul :16,:38 (5,c)",
           ("azul", "I"): "azul :16,:38 (5,c)", ("verde", "XI"): "verde :19,:41 (3,a)",
           ("blanco", "XI"): "blancoIntenso :7 (1)", ("amarillo", "I"): "amarilloOpaco :26 (8)",
           ("blanco", "I"): "blancoOpaco :29 (7)"}
    for p in sorted(pat, key=lambda s: (len(s), s)):
        d = pat[p]
        clave = (d["color"], d["tipo"])
        if clave not in cod:
            continue
        x = st.mean(d["x"])
        c = FABRICA[cod[clave]]
        print(f"  {p:5s} {d['color']:8s} {d['tipo']:3s} {d['serie']} n={len(d['x'])} x={x:7.1f} "
              f"cert={d['cert']:5.0f} k={d['cert'] / (x - X_OSCURO):.3f} "
              f"fábrica R={r_firmware(c, round(x)):6.1f} dR/dx fábrica={pendiente(c, x):.3f}")

    print("\n## 4. Código 5: recta anclada en el oscuro con P17-P19")
    xs, rs = [], []
    for p in ("P17", "P18", "P19"):
        d = pat[p]
        xs.append(st.mean(d["x"]))
        rs.append(d["cert"])
        print(f"  {p}: serie {d['serie']}, disparos {d['x']}, x media {xs[-1]:.3f}, cert {d['cert']:.0f}")
    x, r = np.array(xs), np.array(rs)
    for x0, et in ((X_OSCURO, "ancla 565,3556 (S060)"), (575.0, "ancla 575 (Asistente.java:218)")):
        k = float(np.sum(r * (x - x0)) / np.sum((x - x0) ** 2))   # Ajuste.anclada con r0 = 0
        c1, c0 = f32(k), f32(-k * x0)
        cf = (0.0, 0.0, c1, c0)
        res = [r_firmware(cf, round(v)) for v in x]
        err = [round(float((int(v) - q) / q * 100), 1) for v, q in zip(res, r)]
        rms_u = float(np.sqrt(np.mean((r - (k * (x - x0))) ** 2)))
        print(f"  {et}: c1 = {c1:.8E}  c0 = {c0:.8E}  RMS {rms_u:.2f} unidades de R")
        print(f"    respuesta firmware (entera) {[int(v) for v in res]}  error % {err}")
        print(f"    R(600) = {r_firmware(cf, 600):.2f}  R(4300) = {r_firmware(cf, 4300):.1f}  "
              f"R(oscuro) = {r_firmware(cf, round(X_OSCURO)):.2f}")
    # sensibilidad a un patrón: dejar uno fuera
    for i in range(3):
        m = [j for j in range(3) if j != i]
        k = float(np.sum(r[m] * (x[m] - X_OSCURO)) / np.sum((x[m] - X_OSCURO) ** 2))
        print(f"  sin P{17 + i}: c1 = {k:.5f}")
    fab = FABRICA["azul :16,:38 (5,c)"]
    for v in (600, 700, 800, 828, 900, 1000, 1200):
        print(f"  x={v}: fábrica azul R={evaluar(fab, v):6.1f} (dR/dx {pendiente(fab, v):.3f}); "
              f"anclada R={0.3273 * (v - X_OSCURO):6.1f}")


def main():
    wb = openpyxl.load_workbook(HOJA_2020, data_only=True)
    seccion1(wb)
    seccion2(wb)
    seccion3y4()


if __name__ == "__main__":
    main()
