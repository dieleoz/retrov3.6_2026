#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera 06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md y
06_Calibracion/SLV-002/simulacion_antes_ahora_propuesta_SLV-002_20260919.csv.

EN PAPEL: no escribe nada en el equipo.

Qué hace:
  1. Comprueba el md5 de los tres ZIP de campaña contra HUELLAS.txt y extrae lo que usa:
     - 10:33 (campana.csv): las series aceptadas de P1-P50, 1 colocación x 9 disparos;
     - 12:00 (campana.csv): A5 (S057-S059, 5 x 9 de P22, P28, P4) y OSCURO (S060, 5 x 9);
     - 12:27 (tramas/rtv36_20260919_114644.txt): las re-medidas conformes de P28 (código 1) y P5
       (código 2), en R; se pasan a x invirtiendo la curva leída con #G.
     Y la sesión de la mañana (fixture consolidado, primer disparo de cada serie descartado).
  2. Combina, por patrón, todas las colocaciones con peso 1 / ((s_rep·x)^2 + s^2/n), s_rep = 2,2 %.
  3. Llama al código de la app (commit fijado, compilado con JDK 11) a través de
     tools/ReformulacionSLV002.java: ajuste (Asistente.proponer, Ajuste.ajustar / Ajuste.anclada),
     criterio de #S, C2, oscuro B13, respuesta float32 del firmware, Monte Carlo de colocación,
     remuestreo de patrones y validación cruzada. Aquí no se reimplementa ningún ajuste.
  4. Escribe el Markdown y el CSV.

Uso (desde la raíz del repositorio D:\\IT\\P_RetroVertical_V3.6):
  python 06_Calibracion/SLV-002/tools/reformulacion_simulacion_slv002.py
Variables: RETRO_JDK = carpeta bin de un JDK 11 (por defecto la de la máquina de Diego).
"""
import csv
import hashlib
import io
import math
import os
import re
import statistics as st
import subprocess
import sys
import tempfile
import zipfile
from datetime import datetime

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SLV = os.path.join(RAIZ, "06_Calibracion", "SLV-002")
CAMP = os.path.join(SLV, "campanas")
ZIPS = {
    "103300": ("campana_SLV-002_20260919_103300.zip", "4c50dbf63d53bd7f53cbd9856a0e5805"),
    "120049": ("campana_SLV-002_20260919_120049.zip", "3e2c913bd88eb24cf922335d69c9aad9"),
    "122727": ("campana_SLV-002_20260919_122727.zip", "ce1f35fc64439cbb602d014b725fadfb"),
}
FIXTURE = os.path.join(RAIZ, "03_App_Movil", "RetroV36", "app", "src", "test", "resources",
                       "medidas_SLV-002_20260919_consolidado.csv")
CATALOGO = os.path.join(RAIZ, "06_Calibracion", "patrones_certificados_P1-P132.csv")
COEF_ACTA = os.path.join(CAMP, "coeficientes_SLV-002_20260919_122047.csv")
DRIVER = os.path.join(SLV, "tools", "ReformulacionSLV002.java")
SALIDA_MD = os.path.join(SLV, "REFORMULACION-y-Simulacion-2026-09-19.md")
SALIDA_CSV = os.path.join(SLV, "simulacion_antes_ahora_propuesta_SLV-002_20260919.csv")
JDK = os.environ.get("RETRO_JDK", r"D:\@Proyect\Baliza\7 sw apk\jdk-11\jdk-11.0.24+8\bin")
COMMIT_APP = "f52eeb1"   # App 3.6.9: la que escribió el acta (recta anclada, A5, OSCURO)

S_REP = 0.022            # reproducibilidad entre colocaciones, A5 (HUELLAS.txt: s_rep media 2,24 %)
N_MC = 4000
SEMILLA = 20260919
CODIGOS = "12345678abcd"
NOMBRE = {"1": "blanco intenso", "2": "amarillo intenso", "3": "verde intenso", "4": "rojo intenso",
          "5": "azul intenso", "6": "naranja intenso", "7": "blanco tipo I", "8": "amarillo tipo I",
          "a": "verde tipo I", "b": "rojo tipo I", "c": "azul tipo I", "d": "naranja tipo I"}
# Café y lila se miden con el código del rojo (decisión de Diego, encargo del 19-sep-2026).
COLOR_CODIGO = {"cafe": "rojo", "café": "rojo", "lila": "rojo"}


def md5(b):
    return hashlib.md5(b).hexdigest()


def c(v, d=1):
    if v is None or (isinstance(v, float) and (math.isnan(v) or math.isinf(v))):
        return "—"
    return f"{v:.{d}f}".replace(".", ",")


def cs(v, d=1):
    if v is None or (isinstance(v, float) and (math.isnan(v) or math.isinf(v))):
        return "—"
    return f"{v:+.{d}f}".replace(".", ",")


def git(*a):
    try:
        return subprocess.run(["git", "-C", RAIZ] + list(a), capture_output=True, text=True,
                              check=True).stdout.strip()
    except Exception:
        return "?"


# =============================================================================== datos
def leer_zips():
    z = {}
    for k, (nombre, h) in ZIPS.items():
        raw = open(os.path.join(CAMP, nombre), "rb").read()
        if md5(raw) != h:
            sys.exit(f"md5 de {nombre} distinto del de HUELLAS.txt: {md5(raw)}")
        z[k] = zipfile.ZipFile(io.BytesIO(raw))
    return z


def series_campana(texto):
    ser = {}
    for r in csv.DictReader(io.StringIO(texto)):
        s = ser.setdefault(r["serie_id"], dict(id=r["serie_id"], patron=r["patron"], orient=r["orientacion"],
                                               aceptada=r["aceptada"] == "1", elegida=r["elegida"] == "1",
                                               cert=float(r["valor_certificado"]), tipo=r["tipo_lamina"],
                                               color=r["color"], hora=r["fecha_hora"][11:19],
                                               nota=r["nota"].split(" | importada")[0], col={}))
        if r["descartado"] == "0" and r["x"] != "":
            s["col"].setdefault(r.get("colocacion") or "1", []).append(float(r["x"]))
    return ser


def series_manana():
    """Series de la mañana: corte si cambia el patrón o pasan más de 6 s entre disparos (como
    propuesta_ajuste_slv002.py:series_manana). Primer disparo descartado; el 3031 de P7 fuera."""
    filas = [r for r in csv.DictReader(l for l in open(FIXTURE, encoding="utf-8") if not l.startswith("#"))]
    out, prev = [], None
    for r in filas:
        t = datetime.strptime(r["fecha_hora"][:19], "%Y-%m-%dT%H:%M:%S")
        if prev is None or r["patron"] != prev[0] or (t - prev[1]).total_seconds() > 6:
            out.append({"patron": r["patron"], "hora": r["fecha_hora"][11:19], "x": []})
        out[-1]["x"].append(float(r["x"]))
        prev = (r["patron"], t)
    for s in out:
        s["x"] = s["x"][1:]
        if s["patron"] == "P7":
            s["x"] = [v for v in s["x"] if v != 3031]
    return out


def remedidas(texto, coef):
    """Re-medidas conformes del acta, en R, tras la curva nueva. x = (R + 0,5 − c0) / c1 por disparo
    (el firmware trunca: la media de R entero queda 0,5 por debajo de la R real)."""
    lin = texto.splitlines()
    out = []
    for i, l in enumerate(lin):
        m = re.search(r"re-medida código (\d): (P\d+): R medida ([\d.]+).*-> CONFORME", l)
        if not m:
            continue
        k, p = m.group(1), m.group(2)
        rs = []
        j = i - 1
        while j >= 0 and "re-medida: disparo de asentamiento" not in lin[j]:
            mm = re.search(r"peticion " + k + r" -> ::(\d+)", lin[j])
            if mm:
                rs.append(int(mm.group(1)))
            j -= 1
        rs.reverse()
        c3, c2, c1, c0 = coef[k]
        xs = [(r + 0.5 - c0) / c1 for r in rs]
        out.append(dict(patron=p, codigo=k, r=rs, x=xs, rmed=float(m.group(3)), linea=i + 1))
    return out


def catalogo():
    return [dict(p=r["patron"], cert=float(r["valor_certificado"]), tipo=r["tipo"], color=r["color"])
            for r in csv.DictReader(open(CATALOGO, encoding="utf-8"))]


def coef_acta():
    co = {}
    for r in csv.DictReader(open(COEF_ACTA, encoding="utf-8")):
        if r["codigo"] == "T":
            continue
        co[r["codigo"]] = tuple(float(r[k]) for k in ("c3", "c2", "c1", "c0"))
    return co


FABRICA = {  # Fabrica.java:62-88 (= ecuacionesCalibracion.c:3-42 de 2020). Sólo para invertir en Python.
    "1": (0, -0.000086541, 0.619668128, -303), "2": (-0.000000073, 0.000282508, 0.124075350, -130),
    "3": (0.000000163, -0.000756695, 1.213142724, -442), "a": (0.000000163, -0.000756695, 1.213142724, -442),
    "4": (0.000000147, -0.000668624, 1.082394050, -393), "5": (0.000000026, -0.000018402, 0.102152670, -49),
    "c": (0.000000026, -0.000018402, 0.102152670, -49), "6": (0, 0.000090731, 0.001064329, -21),
    "d": (0, 0.000090731, 0.001064329, -21), "7": (0, 0.000087404, 0.013617682, -27),
    "8": (0.000000086, -0.000299026, 0.446572329, -161), "b": (0, 0.000113098, -0.076130418, 10),
}


def ev(co, x):
    return ((co[0] * x + co[1]) * x + co[2]) * x + co[3]


def invertir(co, r, xmin, xmax=4300):
    """Primera x en [xmin; xmax] con curva creciente y R(x) = r (bisección sobre el primer tramo que cruza)."""
    prev = xmin
    fp = ev(co, prev) - r
    for x in range(int(xmin) + 1, xmax + 1):
        fx = ev(co, x) - r
        if fp <= 0 < fx:
            a, b = prev, x
            for _ in range(60):
                m = (a + b) / 2
                if ev(co, m) - r > 0:
                    b = m
                else:
                    a = m
            return (a + b) / 2
        prev, fp = x, fx
    return float("nan")


# =============================================================================== java
def fuentes_app(tmp):
    r = subprocess.run(["git", "-C", RAIZ, "archive", "--format=zip", COMMIT_APP,
                        "03_App_Movil/RetroV36/app/src/main/java"], capture_output=True, check=True)
    dst = os.path.join(tmp, "app")
    zipfile.ZipFile(io.BytesIO(r.stdout)).extractall(dst)
    return os.path.join(dst, "03_App_Movil", "RetroV36", "app", "src", "main", "java")


class Java:
    def __init__(self, tmp):
        self.tmp = tmp
        self.cls = os.path.join(tmp, "cls")
        os.makedirs(self.cls, exist_ok=True)
        subprocess.run([os.path.join(JDK, "javac"), "-encoding", "UTF-8", "-d", self.cls, "-sourcepath",
                        fuentes_app(tmp), DRIVER], check=True)

    def correr(self, catalogo_csv, ordenes):
        f = os.path.join(self.tmp, "ordenes.tsv")
        with open(f, "w", encoding="utf-8", newline="\n") as h:
            h.write("\n".join("\t".join(str(v) for v in o) for o in ordenes) + "\n")
        r = subprocess.run([os.path.join(JDK, "java"), "-Dfile.encoding=UTF-8", "-cp", self.cls,
                            "ReformulacionSLV002", catalogo_csv, f], check=True, capture_output=True)
        return [l.split("\t") for l in r.stdout.decode("utf-8").splitlines()]


def por_tag(lineas):
    d = {}
    for l in lineas:
        d.setdefault(l[0], []).append(l[1:])
    return d


# =============================================================================== cálculo
def reunir(z, coefs):
    txt = {k: z[k].read("campana.csv") for k in ("103300", "120049", "122727")}
    if md5(txt["120049"]) != md5(txt["122727"]):
        sys.exit("campana.csv de 12:00 y 12:27 difieren: revisar")
    c1033 = series_campana(txt["103300"].decode("utf-8"))
    c1200 = series_campana(txt["120049"].decode("utf-8"))
    # Las series S001-S056 importadas en la de 12:00 son las de 10:33 sin cambios.
    for sid, s in c1033.items():
        t = c1200.get(sid)
        if t is None or t["col"] != s["col"]:
            sys.exit("la serie %s de 10:33 no coincide en la campaña de 12:00" % sid)
    tramas = z["122727"].read("tramas/rtv36_20260919_114644.txt").decode("utf-8")
    rem = remedidas(tramas, coefs)

    cols = {}

    def add(p, fuente, sid, xs, sesion):
        cols.setdefault(p, []).append(dict(fuente=fuente, id=sid, n=len(xs), m=st.mean(xs),
                                            s=st.stdev(xs) if len(xs) > 1 else float("nan"), sesion=sesion))

    elegida = {}
    for sid, s in c1033.items():
        if not s["aceptada"]:
            continue
        for k, xs in s["col"].items():
            add(s["patron"], "10:33 " + sid + (" %s°" % s["orient"] if s["orient"] != "0" else ""), sid, xs, "10:33")
        if s["elegida"]:
            elegida[s["patron"]] = (sid, st.mean([v for xs in s["col"].values() for v in xs]))
    for s in series_manana():
        if s["patron"] == "P24" and s["hora"] == "09:04:43":
            continue  # identidad dudosa (PROPUESTA-Ajuste-SLV-002-2026-09-19.md §1.4)
        add(s["patron"], "mañana " + s["hora"], s["hora"], s["x"], "mañana")
    a5 = {}
    for sid in ("S057", "S058", "S059"):
        s = c1200[sid]
        medias = []
        for k in sorted(s["col"], key=int):
            add(s["patron"], "A5 %s col %s" % (sid, k), sid, s["col"][k], "12:00")
            medias.append(st.mean(s["col"][k]))
        a5[s["patron"]] = dict(id=sid, medias=medias, m=st.mean(medias), s=st.stdev(medias))
    for r in rem:
        add(r["patron"], "re-medida acta código %s (tramas:%d)" % (r["codigo"], r["linea"]), "remedida", r["x"],
            "12:20")
    osc = c1200["S060"]
    todos = [v for k in osc["col"] for v in osc["col"][k]]
    oscuro = dict(x=st.mean(todos), n=len(todos), medias=[st.mean(osc["col"][k]) for k in sorted(osc["col"], key=int)])
    oscuro["s_entre"] = st.stdev(oscuro["medias"])
    return cols, elegida, a5, rem, oscuro, c1033


def combinar(cols):
    comb = {}
    for p, l in cols.items():
        w = []
        for q in l:
            s = q["s"] if not math.isnan(q["s"]) else 6.0
            w.append(1 / ((S_REP * q["m"]) ** 2 + s * s / q["n"]))
        sw = sum(w)
        m = sum(wi * q["m"] for wi, q in zip(w, l)) / sw
        comb[p] = dict(x=m, sx=1 / math.sqrt(sw), K=len(l), sesiones=sorted(set(q["sesion"] for q in l)),
                       disp=(st.stdev([q["m"] for q in l]) / m if len(l) > 1 else float("nan")))
    return comb


def fmt_coef(co):
    return ",".join("%.8E" % v for v in co)


def calcular():
    z = leer_zips()
    acta = coef_acta()
    cols, eleg, a5, rem, osc, c1033 = reunir(z, acta)
    comb = combinar(cols)
    cat = catalogo()
    x0 = osc["x"]
    fab = {k: FABRICA[k] for k in CODIGOS}
    tmp = tempfile.mkdtemp(prefix="slv002_ref_")
    java = Java(tmp)
    # Catálogo de lo MEDIDO (P1-P50) para Asistente.proponer, y el completo con café y lila como rojo.
    cat50 = os.path.join(tmp, "cat50.csv")
    cat132 = os.path.join(tmp, "cat132.csv")
    medidos = set(comb)
    with open(cat50, "w", encoding="utf-8") as h, open(cat132, "w", encoding="utf-8") as g:
        h.write("patron,valor_certificado,tipo,color\n")
        g.write("patron,valor_certificado,tipo,color\n")
        for q in cat:
            col = COLOR_CODIGO.get(q["color"], q["color"])
            linea = "%s,%g,%s,%s\n" % (q["p"], q["cert"], q["tipo"], col)
            g.write(linea)
            if q["p"] in medidos:
                h.write(linea)
    ordenes = [("COD", q["p"], q["cert"], q["tipo"], COLOR_CODIGO.get(q["color"], q["color"])) for q in cat]
    ordenes.append(("COB", "P1-P50", cat50))
    ordenes.append(("COB", "P1-P132", cat132))
    r = por_tag(java.correr(cat50, ordenes))
    codigo = {l[0]: l[1] for l in r["COD"]}
    cob = {(l[0], l[1]): (int(l[2]), l[3]) for l in r["COB"]}
    for q in cat:
        q["codigo"] = codigo[q["p"]]
        q["color_codigo"] = COLOR_CODIGO.get(q["color"], q["color"])

    # ---------------------------------------------------------------- grupos de ajuste
    ordenes = []
    grupos = {}
    for q in cat:
        k = q["codigo"]
        if q["p"] in comb:
            grupos.setdefault("N" + k, []).append(q["p"])
            ordenes.append(("P", "N" + k, q["p"], q["cert"], q["tipo"], q["color_codigo"],
                            repr(comb[q["p"]]["x"]), repr(comb[q["p"]]["sx"])))
            if k in "12" and q["p"] in eleg:
                ordenes.append(("P", "ACTA" + k, q["p"], q["cert"], q["tipo"], q["color_codigo"],
                                repr(eleg[q["p"]][1]), "1"))
            if k in "12":  # sensibilidad: cada colocación como punto propio (peso proporcional a K)
                for q2 in cols[q["p"]]:
                    ordenes.append(("P", "C" + k, q["p"], q["cert"], q["tipo"], q["color_codigo"],
                                    repr(q2["m"]), "1"))
    acta_de = {k: (acta[k] if k in "12" else fab[k]) for k in CODIGOS}
    ajustes = [  # id, grupo, código, método, vigente para el informe
        ("ACTA1", "ACTA1", "1", "1", fab["1"]), ("ACTA2", "ACTA2", "2", "A", fab["2"]),
        ("N1", "N1", "1", "1", acta["1"]), ("N2", "N2", "2", "A", acta["2"]),
        ("N2g1", "N2", "2", "1", acta["2"]), ("N1A", "N1", "1", "A", acta["1"]),
        ("N8", "N8", "8", "1", fab["8"]), ("N8A", "N8", "8", "A", fab["8"]),
        ("Nb", "Nb", "b", "1", fab["b"]), ("NbA", "Nb", "b", "A", fab["b"]),
        ("N3", "N3", "3", "1", fab["3"]), ("N4", "N4", "4", "1", fab["4"]), ("N5", "N5", "5", "1", fab["5"]),
        ("N7", "N7", "7", "1", fab["7"]), ("Na", "Na", "a", "1", fab["a"]), ("Nc", "Nc", "c", "1", fab["c"]),
        ("Nd", "Nd", "d", "1", fab["d"]), ("N3A", "N3", "3", "A", fab["3"]), ("N4A", "N4", "4", "A", fab["4"]),
        ("N5A", "N5", "5", "A", fab["5"]), ("Na_A", "Na", "a", "A", fab["a"]), ("Nc_A", "Nc", "c", "A", fab["c"]),
        ("Nd_A", "Nd", "d", "A", fab["d"]),
        ("C1", "C1", "1", "1", acta["1"]), ("C2", "C2", "2", "A", acta["2"]),
    ]
    for a in ajustes:
        ordenes.append(("FIT", a[0], a[1], a[2], a[3], repr(x0), fmt_coef(a[4])))
    for a in ajustes:
        if a[0] in ("N1", "N2", "N8", "N8A", "NbA", "N2g1", "N1A"):
            ordenes.append(("MC", a[0], a[1], a[2], a[3], repr(x0), N_MC, SEMILLA))
            ordenes.append(("BOOT", a[0], a[1], a[2], a[3], repr(x0), N_MC, SEMILLA + 1))
            ordenes.append(("LOO", a[0], a[1], a[2], a[3], repr(x0)))
    r = por_tag(java.correr(cat50, ordenes))
    res = dict(cols=cols, eleg=eleg, a5=a5, rem=rem, osc=osc, comb=comb, cat=cat, x0=x0, fab=fab, acta=acta,
               acta_de=acta_de, cob=cob, grupos=grupos, java=java, cat50=cat50, r=r, commit=git("rev-parse",
               "--short", COMMIT_APP))
    res["AJ"] = {l[0]: dict(co=tuple(float(v) for v in l[1:5]), txt=l[1:5], rms=float(l[5]), emax=float(l[6]))
                 for l in r.get("AJ", [])}
    res["ESC"] = {l[0]: (l[1] == "1", l[2] if len(l) > 2 else "") for l in r["ESC"]}
    res["INC"] = {l[0]: (int(l[1]), l[2] if len(l) > 2 else "") for l in r["INC"]}
    res["INF"] = {}
    for l in r["INF"]:
        res["INF"].setdefault(l[0], []).append(l[1] if len(l) > 1 else "")
    for t in ("MC", "BOOT"):
        res[t] = {}
        for l in r[t]:
            res[t].setdefault(l[0], {})[int(l[1])] = (float(l[2]), float(l[3]))
        res[t + "_S"] = {l[0]: (int(l[1]), int(l[2]), int(l[3])) for l in r[t + "_S"]}
    res["LOO"] = {}
    for l in r["LOO"]:
        res["LOO"].setdefault(l[0], []).append((l[1], float(l[2]), float(l[3])))
    return res


# =============================================================================== decisión y simulación
def rango_codigo(R, k):
    xs = [R["comb"][p]["x"] for p in R["grupos"].get("N" + k, [])]
    return (min(xs), max(xs)) if xs else (None, None)


def significancia(R, id_nuevo, ref, k):
    """Máximo de |R_nueva − R_ref| / sigma en la rejilla dentro del rango medido del código.
    sigma_MC = colocación (s_rep 2,2 % por colocación, combinada); sigma_BOOT = remuestreo de patrones."""
    lo, hi = rango_codigo(R, k)
    new = R["AJ"][id_nuevo]["co"]
    mc, bo = R["MC"][id_nuevo], R["BOOT"][id_nuevo]
    xg = sorted(mc)

    def interp(tab, x):
        for a, b in zip(xg, xg[1:]):
            if a <= x <= b:
                return tab[a][1] + (tab[b][1] - tab[a][1]) * (x - a) / (b - a)
        return tab[xg[-1]][1]

    filas = []
    for x in [lo] + [g for g in xg if lo < g < hi] + [hi]:
        sm, sb = interp(mc, x), interp(bo, x)
        d = ev(new, x) - ev(ref, x)
        filas.append(dict(x=x, rn=ev(new, x), rr=ev(ref, x), d=d, smc=sm, sb=sb,
                          zmc=abs(d) / sm if sm > 0 else float("inf"), zb=abs(d) / sb if sb > 0 else float("inf")))
    return filas


def simular(R):
    java, cat, comb, x0 = R["java"], R["cat"], R["comb"], R["x0"]
    fab, acta = R["fab"], R["acta_de"]
    sig = {}
    sig["1"] = significancia(R, "N1", acta["1"], "1")
    sig["2"] = significancia(R, "N2", acta["2"], "2")
    sig["8"] = significancia(R, "N8A", fab["8"], "8")
    sig["8g1"] = significancia(R, "N8", fab["8"], "8")
    sig["b"] = significancia(R, "NbA", fab["b"], "b")
    decide = {}
    for k, idn in (("1", "N1"), ("2", "N2"), ("8", "N8A"), ("b", "NbA")):
        zmax = max(f["zmc"] for f in sig[k])
        zbmax = max(f["zb"] for f in sig[k])
        decide[k] = dict(id=idn, zmc=zmax, zb=zbmax, signif=zmax > 2, esc=R["ESC"][idn][0])
    prop = dict(acta)
    for k in ("1", "2", "8", "b"):
        if decide[k]["signif"] and decide[k]["esc"]:
            prop[k] = R["AJ"][decide[k]["id"]]["co"]
    estados = {"fabrica": fab, "acta": acta, "propuesta": prop}
    R["sig"], R["decide"], R["prop"], R["estados"] = sig, decide, prop, estados

    # ---------------------------------------------------------------- órdenes: CHK, EV
    ordenes = []
    for e, cos in estados.items():
        for k in CODIGOS:
            lo, _ = rango_codigo(R, k)
            ordenes.append(("CHK", e + "|" + k, k) + tuple("%.9E" % v for v in cos[k]) +
                           (repr(lo if lo else 600.0), repr(x0)))
    # x de 10:33 y de las otras sesiones (validación fuera de muestra del acta)
    xs_ses = {}
    for p, l in R["cols"].items():
        for ses in ("10:33", "otras"):
            sel = [q for q in l if (q["sesion"] == "10:33") == (ses == "10:33")]
            if sel:
                w = [1 / ((S_REP * q["m"]) ** 2 + (q["s"] if not math.isnan(q["s"]) else 6.0) ** 2 / q["n"])
                     for q in sel]
                xs_ses[(p, ses)] = sum(wi * q["m"] for wi, q in zip(w, sel)) / sum(w)
    R["xs_ses"] = xs_ses
    # predicción P51-P132
    med = {}
    for q in cat:
        k = q["codigo"]
        q["x_fab"] = invertir(fab[k], q["cert"], x0)
        if q["p"] in comb:
            med.setdefault(k, []).append(q)
    # Modelo de x esperada por código, con lo medido en P1-P50:
    #  - 1 y 2: la curva del acta invertida (es lo que el equipo lleva escrito), con la dispersión de
    #    (x medida − oscuro) / (x acta invertida − oscuro) en sus patrones;
    #  - 3, 4, 5: recta por el oscuro con la sensibilidad medida k = cert / (x − oscuro) (mediana);
    #  - 6: sin patrón medido: sólo la fábrica invertida, con banda heurística (error máx. de fábrica en d,
    #    que tiene la misma ecuación, Fabrica.java:71-73).
    ratio = {}
    for k, l in med.items():
        if k in "12":
            c1_, c0_ = acta[k][2], acta[k][3]
            rr = [(comb[q["p"]]["x"] - x0) / ((q["cert"] - c0_) / c1_ - x0) for q in l]
            ratio[k] = dict(modelo="acta invertida", n=len(rr), med=st.median(rr), sd=st.stdev(rr))
        else:
            kk = [q["cert"] / (comb[q["p"]]["x"] - x0) for q in l]
            ratio[k] = dict(modelo="recta por el oscuro, k medida", n=len(kk), k=st.median(kk),
                            sd=(st.stdev(kk) / st.median(kk)) if len(kk) > 1 else float("nan"))
    err_d = max(abs(ev(fab["d"], comb[q["p"]]["x"]) - q["cert"]) / q["cert"] for q in med["d"])
    for q in cat:
        k = q["codigo"]
        if q["p"] in comb:
            continue
        rk = ratio.get(k)
        if k in "12":
            base = (q["cert"] - acta[k][3]) / acta[k][2]
            q["x_esp"] = x0 + rk["med"] * (base - x0)
            sdr, q["modelo"] = rk["sd"], "acta invertida (P1-P50: n = %d)" % rk["n"]
        elif rk:
            q["x_esp"] = x0 + q["cert"] / rk["k"]
            sdr, q["modelo"] = rk["sd"], "oscuro + R/k, k = %.4f (n = %d)" % (rk["k"], rk["n"])
        else:
            q["x_esp"] = q["x_fab"]
            sdr, q["modelo"] = err_d, "fábrica invertida, sin patrón medido"
        q["banda"] = 2 * math.sqrt((sdr * (q["x_esp"] - x0)) ** 2 + (S_REP * q["x_esp"]) ** 2)
        q["cerca_osc"] = (q["x_esp"] - x0) < 3 * S_REP * q["x_esp"]
    sd_pool = err_d
    R["ratio"], R["sd_pool"] = ratio, sd_pool
    # Candidatas evaluadas también sobre los patrones medidos (para comparar con el acta)
    cand = {"1": ["N1", "C1", "N1A"], "2": ["N2", "C2", "N2g1"], "8": ["N8", "N8A"], "b": ["Nb", "NbA"]}
    R["cand"] = cand
    for q in cat:
        if q["p"] in comb and q["codigo"] in cand:
            for idc in cand[q["codigo"]]:
                ordenes.append(("EV", q["p"] + "|" + idc) + tuple("%.9E" % v for v in R["AJ"][idc]["co"]) +
                               (repr(comb[q["p"]]["x"]),))
    for q in cat:
        x = comb[q["p"]]["x"] if q["p"] in comb else q.get("x_esp", float("nan"))
        if math.isnan(x):
            continue
        for e, cos in estados.items():
            ordenes.append(("EV", q["p"] + "|" + e) + tuple("%.9E" % v for v in cos[q["codigo"]]) + (repr(x),))
        if q["p"] in comb and q["codigo"] in "12":
            for ses in ("10:33", "otras"):
                if (q["p"], ses) in xs_ses:
                    ordenes.append(("EV", q["p"] + "|acta@" + ses) + tuple("%.9E" % v for v in acta[q["codigo"]]) +
                                   (repr(xs_ses[(q["p"], ses)]),))
                    ordenes.append(("EV", q["p"] + "|fabrica@" + ses) + tuple("%.9E" % v for v in fab[q["codigo"]]) +
                                   (repr(xs_ses[(q["p"], ses)]),))
    r = por_tag(java.correr(R["cat50"], ordenes))
    R["CHK"] = {l[0]: l[1:] for l in r["CHK"]}
    R["EV"] = {l[0]: (float(l[1]), int(l[2])) for l in r["EV"]}
    return R


def err(rfw, cert):
    return 100.0 * (rfw - cert) / cert


def resumen(filas, clave):
    """n, sesgo %, RMS %, máx |e| % de una lista de errores relativos."""
    v = [f[clave] for f in filas if not math.isnan(f[clave])]
    if not v:
        return (0, float("nan"), float("nan"), float("nan"))
    return (len(v), st.mean(v), math.sqrt(sum(e * e for e in v) / len(v)), max(abs(e) for e in v))


# =============================================================================== informe
def orden_p(p):
    return (int(re.sub(r"\D", "", p)), p)


def tabla(cab, filas, alin=None):
    alin = alin or ["---:"] * len(cab)
    out = ["| " + " | ".join(cab) + " |", "| " + " | ".join(alin) + " |"]
    out += ["| " + " | ".join(str(v) for v in f) + " |" for f in filas]
    return out


def cumple(t):
    return "cumple" if t == "OK" else "**no**: " + t


def escribir(R):
    cat, comb, x0, fab, acta, prop = R["cat"], R["comb"], R["x0"], R["fab"], R["acta_de"], R["prop"]
    EV, CHK, AJ, d = R["EV"], R["CHK"], R["AJ"], R["decide"]
    est = ("fabrica", "acta", "propuesta")
    ETQ = {"fabrica": "Fábrica", "acta": "Acta de hoy", "propuesta": "Propuesta"}
    medidos = sorted([q for q in cat if q["p"] in comb], key=lambda q: orden_p(q["p"]))
    banco = sorted([q for q in cat if q["p"] not in comb], key=lambda q: orden_p(q["p"]))
    for q in medidos:
        for e in est:
            q["R_" + e] = EV[q["p"] + "|" + e][1]
            q["e_" + e] = err(q["R_" + e], q["cert"])
        for idc in R["cand"].get(q["codigo"], []):
            q["e_" + idc] = err(EV[q["p"] + "|" + idc][1], q["cert"])
    for q in banco:
        for e in est:
            q["R_" + e] = EV[q["p"] + "|" + e][1]
    # El texto de los veredictos está escrito para este resultado: si los datos cambian, que falle.
    assert not d["1"]["signif"] and not d["2"]["signif"] and d["8"]["signif"] and d["b"]["signif"]
    assert R["ESC"]["N8A"][0] and R["ESC"]["NbA"][0] and not R["ESC"]["Nb"][0]
    assert R["cob"][("P1-P132", "5")][0] == 0 and all(R["cob"][("P1-P132", k)][0] > 0 for k in "346")
    L = []
    w = L.append

    w("# Reformulación de las curvas de SLV-002 y simulación antes / ahora / propuesta (19-sep-2026)")
    w("")
    w("**Sin validar en hardware y sin escribir nada en el equipo.** Es un cálculo en papel sobre las medidas del "
      "19-sep-2026. La reproducibilidad (s_rep ≈ 2,2 % por colocación) sale de **una** A5 de tres patrones, y "
      "P51-P132 **no están medidos**: su tabla (§5) es una **estimación**.")
    w("")
    w("Generado por `06_Calibracion/SLV-002/tools/reformulacion_simulacion_slv002.py`, que llama al código de la "
      "app a través de `tools/ReformulacionSLV002.java`. No editar a mano: se regenera. La tabla completa está en "
      "`simulacion_antes_ahora_propuesta_SLV-002_20260919.csv`.")
    w("")

    # ------------------------------------------------------------------ 0
    w("## 0. Resumen")
    w("")
    res = {}
    for k in CODIGOS:
        l = [q for q in medidos if q["codigo"] == k]
        res[k] = {e: resumen(l, "e_" + e) for e in est}
    filas = []
    for k in "128b":
        filas.append([k, NOMBRE[k], res[k]["fabrica"][0]] +
                     ["%s / %s" % (cs(res[k][e][1]), c(res[k][e][2])) for e in est])
    L += tabla(["Código", "Lámina", "n", "Fábrica: sesgo / RMS %", "Acta de hoy: sesgo / RMS %",
                "Propuesta: sesgo / RMS %"], filas, [":---:", ":---", "---:", "---:", "---:", "---:"])
    w("")
    w("Error = (respuesta float32 del firmware − certificado) / certificado, con la x combinada de todas las "
      "sesiones (§2.2). Los demás códigos no cambian en ningún estado (§4.2).")
    w("")
    w("- **Códigos 1 y 2: no se reescriben.** Con las dos campañas y la A5, la curva nueva se separa de la del "
      "acta como mucho %s unidades de R en el rango medido (código 1) y %s (código 2): %s y %s veces la σ de "
      "colocación. No es significativo." % (c(max(abs(f["d"]) for f in R["sig"]["1"])),
                                          c(max(abs(f["d"]) for f in R["sig"]["2"])),
                                          c(d["1"]["zmc"], 2), c(d["2"]["zmc"], 2)))
    w("- **Código 8: escribir ya, con la recta anclada en el oscuro:** `c1 = %s`, `c0 = %s`. La de grado 1 sale "
      "casi igual, pero queda en el filo de `#S` (§3.3)." % (AJ["N8A"]["txt"][2], AJ["N8A"]["txt"][3]))
    w("- **Código b: se puede escribir ya, también con la recta anclada:** `c1 = %s`, `c0 = %s`. Necesita la "
      "conformidad de Diego a RF-CAL-14. El banco P51-P132 no trae rojo tipo I: esperarlo no lo mejora." % (
          AJ["NbA"]["txt"][2], AJ["NbA"]["txt"][3]))
    w("- **Códigos 3, 4 y 6: esperan al banco**, que sí los cubre. **El código 5, ni con el banco**: el azul "
      "intenso sólo llega de R 83 a 102 y la regla de cobertura de la app lo impide.")
    w("")

    # ------------------------------------------------------------------ 1
    w("## 1. Datos y trazabilidad")
    w("")
    rows = [
        ["Campaña 10:33", "`campanas/campana_SLV-002_20260919_103300.zip`, md5 `%s` (= `HUELLAS.txt`)" % ZIPS["103300"][1],
         "Series **aceptadas** de P1-P50, 1 colocación × 9 disparos. P5 cuenta dos (0° y 90°). Las no aceptadas "
         "(S021 y S049, marcadas «repetida») no entran"],
        ["Campaña 12:00", "`campana_SLV-002_20260919_120049.zip`, md5 `%s`" % ZIPS["120049"][1],
         "A5: S057 (P22), S058 (P28) y S059 (P4), 5 × 9. OSCURO: S060, 5 × 9. Sus S001-S056 son las de 10:33 "
         "sin cambios: el script lo comprueba serie a serie"],
        ["Campaña 12:27", "`campana_SLV-002_20260919_122727.zip`, md5 `%s`" % ZIPS["122727"][1],
         "`campana.csv` idéntico al de 12:00 (mismo md5). De `tramas/rtv36_20260919_114644.txt` salen las "
         "re-medidas conformes del acta"],
        ["Sesión de la mañana", "`03_App_Movil/RetroV36/app/src/test/resources/medidas_SLV-002_20260919_consolidado.csv`",
         "08:59-09:25, firmware 3.6.0. Primer disparo de cada serie descartado y el 3031 de P7 fuera. La serie de "
         "P24 de 09:04:43 (2445,5) queda fuera por identidad dudosa (`PROPUESTA-Ajuste-SLV-002-2026-09-19.md` §1.4)"],
        ["Estado de hoy (acta)", "`campanas/coeficientes_SLV-002_20260919_122047.csv`",
         "Lectura `#G` de las 12 curvas después de escribir: los códigos 1 y 2 son nuevos, el resto es de fábrica"],
        ["Curvas de fábrica", "`Fabrica.java:62-88` (tabla de 2020 de `ecuacionesCalibracion.c`)", "Estado «fábrica»"],
        ["Catálogo", "`06_Calibracion/patrones_certificados_P1-P132.csv`",
         "133 filas (P32 cuenta dos). Medidos: P1-P50. **El café y el lila se miden con el código del rojo**: el "
         "script les asigna el color rojo antes de `Asistente.deCodigo`"],
        ["Código que calcula", "App 3.6.9, commit `%s`, compilada con JDK 11" % R["commit"],
         "`Asistente.proponer` (`Asistente.java:399`), `Ajuste.ajustar` (`Ajuste.java:41`), `Ajuste.anclada` "
         "(`Ajuste.java:99`), `criterioFirmwareS` (`Asistente.java:261`), `validarForma` (`:179`), "
         "`comprobarOscuro` (`:237`), `cobertura` (`:101`), `Ecuacion.respuestaFloat32` (`Ecuacion.java:56`)"],
        ["Firmware", "`calibracion_v36.c:229-233` (`aplicarEcuacion`); `:316-319` y `:404-420` (límites de "
         "`#S`); `ecuacionesCalibracion.c:49-54` (`arreglar_dato`)",
         "Float32 en el orden de 2020, truncado a entero; más de 4000 da 0, y un negativo da la vuelta y también "
         "sale 0 (`calibracion_v36.c:309-313`)"],
    ]
    L += tabla(["Qué", "Fuente", "Uso"], rows, [":---", ":---", ":---"])
    w("")
    w("**El canal de cálculo reproduce el acta.** Con las series elegidas de 10:33 y el código de la app salen "
      "`c1 = %s`, `c0 = %s` para el código 1, y `c1 = %s`, `c0 = %s` para el 2, con la recta anclada en "
      "x = %s. Son exactamente los `#S` enviados (`tramas/rtv36_20260919_114644.txt:1660` y `:2520`). El equipo "
      "guarda `2.98471571E-01 / -1.62263885E+02` y `3.65483810E-01 / -2.06628295E+02` (relectura `#G` en "
      "float32), y esos son los coeficientes que usa aquí el estado «acta»." % (
          AJ["ACTA1"]["txt"][2], AJ["ACTA1"]["txt"][3], AJ["ACTA2"]["txt"][2], AJ["ACTA2"]["txt"][3], c(x0, 2)))
    w("")

    # ------------------------------------------------------------------ 2
    w("## 2. Reproducibilidad y combinación de las sesiones")
    w("")
    w("### 2.1 A5, oscuro y re-medidas")
    w("")
    rows = []
    for p, a in sorted(R["a5"].items(), key=lambda t: orden_p(t[0])):
        x1033 = R["xs_ses"].get((p, "10:33"))
        rows.append([p, a["id"], " · ".join(c(v) for v in a["medias"]), c(a["m"]), c(a["s"]),
                     c(100 * a["s"] / a["m"], 2), c(x1033), cs(100 * (a["m"] - x1033) / x1033)])
    L += tabla(["Patrón", "Serie", "Media de cada colocación", "Media", "s entre colocaciones", "s_rep %",
                "x 10:33", "A5 − 10:33 %"], rows,
               [":---", ":---:", ":---", "---:", "---:", "---:", "---:", "---:"])
    w("")
    srel = [100 * a["s"] / a["m"] for a in R["a5"].values()]
    w("- s_rep media **%s %%** (%s). Se usa **2,2 %%** por colocación, como fija el método. La A5 se midió después "
      "de cambiar la batería (`HUELLAS.txt`), y los tres patrones leen **por debajo** de 10:33. Con tres "
      "patrones no se distingue un efecto de sesión de la colocación." % (
          c(st.mean(srel), 2), " / ".join(c(v, 2) for v in srel)))
    o = R["osc"]
    w("- **Oscuro (S060):** medias por colocación %s; x = **%s** (45 disparos); s entre colocaciones %s cuentas. "
      "Es el ancla de las rectas ancladas. La regla B13 se evalúa aquí en esa x y en x = 575, que es la que usa "
      "la app (`Asistente.java:218`)." % (" · ".join(c(v) for v in o["medias"]), c(o["x"], 2), c(o["s_entre"])))
    for r in R["rem"]:
        w("- **Re-medida del acta, %s (código %s):** R = %s (`tramas:%d`). La x de cada disparo es "
          "(R + 0,5 − c0) / c1 con la curva `#G`, porque el firmware trunca. Media **%s**. Entra como una "
          "colocación más." % (r["patron"], r["codigo"], ", ".join(str(v) for v in r["r"]), r["linea"],
                               c(st.mean(r["x"]))))
    w("- La primera re-medida de P28 (`tramas:2156`, R = 227,3, NO CONFORME) tiene cinco disparos de 13-18, es "
      "decir, sin patrón debajo. No entra.")
    w("")
    w("### 2.2 Combinación")
    w("")
    w("Cada colocación (una serie de la mañana, una de 10:33, cada colocación de la A5, cada re-medida) pesa "
      "`1 / ((0,022·x)² + s²/n)`, y la media combinada tiene σ = `1/√Σw`. Como s_rep domina, una serie de 2 "
      "disparos pesa casi lo mismo que una de 9: lo que cuenta es el número de colocaciones K.")
    w("")
    rows = []
    for q in medidos:
        if q["codigo"] not in "12":
            continue
        cc = comb[q["p"]]
        det = " · ".join("%s: %s" % (x["fuente"].replace("re-medida acta código", "re-medida cód.")
                                     .replace(" (tramas:", " (tr:"), c(x["m"])) for x in R["cols"][q["p"]])
        rows.append([q["p"], q["codigo"], q["tipo"], c(q["cert"], 0), cc["K"], c(cc["x"]), c(cc["sx"]),
                     c(100 * cc["disp"], 1) if cc["K"] > 1 else "—", det])
    L += tabla(["Patrón", "Cód.", "Tipo", "Cert.", "K", "x combinada", "σ", "Dispersión entre colocaciones %",
                "Colocaciones"], rows, [":---", ":---:", ":---:", "---:", "---:", "---:", "---:", "---:", ":---"])
    w("")
    w("Los demás patrones (P11-P19 y todo el tipo I) sólo tienen la colocación de 10:33: K = 1 y σ = 2,2 % de x.")
    w("")

    # ------------------------------------------------------------------ 3
    w("## 3. Reformulación por código")
    w("")
    w("**Criterio de «mejora significativa».** En cada x de la rejilla dentro del rango medido del código se "
      "compara la diferencia entre la curva nueva y la vigente con la σ de la curva nueva, obtenida de dos "
      "formas con el mismo código de ajuste de la app (%d repeticiones, semilla fija):" % N_MC)
    w("")
    w("- **σ_col (Monte Carlo de colocación):** cada x combinada se mueve N(0, σ) con su σ de §2.2, es decir, "
      "s_rep = 2,2 % por colocación dividido por √K. Es el criterio del encargo.")
    w("- **σ_pat (remuestreo de patrones):** se remuestrean los patrones con reposición. Recoge además la "
      "dispersión entre láminas, que es la que de verdad manda aquí.")
    w("")
    w("Una curva nueva **sólo se propone si en algún punto |Δ| > 2·σ_col** y la app la deja escribir.")
    w("")

    def bloque_sig(k, idn, ref_nombre):
        rows = []
        for f in R["sig"][k]:
            rows.append([c(f["x"], 0), c(f["rr"]), c(f["rn"]), cs(f["d"]), c(f["smc"]), c(f["sb"]),
                         c(f["zmc"], 2)])
        return tabla(["x", "R " + ref_nombre, "R nueva", "Δ", "σ_col", "σ_pat", "|Δ| / σ_col"], rows)

    def fila_aj(idc):
        a = AJ[idc]
        return "`c3 = %s`, `c2 = %s`, `c1 = %s`, `c0 = %s`" % tuple(a["txt"])

    def tipo_tabla(k, ids):
        tipos = sorted(set(q["tipo"] for q in medidos if q["codigo"] == k))
        rows = []
        for t in tipos:
            l = [q for q in medidos if q["codigo"] == k and q["tipo"] == t]
            fila = [t, len(l)]
            for i in ids:
                n, sg, rms, mx = resumen(l, "e_" + i)
                fila.append("%s / %s" % (cs(sg), c(rms)))
            rows.append(fila)
        return rows

    # código 1
    w("### 3.1 Código 1, blanco intenso (grado 1, decisión de Diego)")
    w("")
    w("Curva nueva con las x combinadas: %s. RMS %s unidades de R; `#S` %s; incumplimientos RF-CAL-14/15/16: "
      "%d (%s)." % (fila_aj("N1"), c(AJ["N1"]["rms"], 2), "pasa" if R["ESC"]["N1"][0] else "no",
                    R["INC"]["N1"][0], R["INC"]["N1"][1] or "ninguno"))
    w("")
    L += bloque_sig("1", "N1", "acta")
    w("")
    ids = ["fabrica", "acta", "N1", "C1"]
    L += tabla(["Tipo", "n", "Fábrica", "Acta de hoy", "Nueva (combinada)", "Nueva (una fila por colocación)"],
               tipo_tabla("1", ids), [":---:", "---:", "---:", "---:", "---:", "---:"])
    w("")
    w("Sesgo / RMS en %%, sobre las x combinadas. La variante «una fila por colocación» (%s) pesa cada patrón "
      "por su K. Monte Carlo de colocación: la curva re-ajustada falla `#S` en %d de %d repeticiones y B13 (en x = 575) "
      "en %d: la recta libre del blanco está cerca del límite del oscuro." % (
          fila_aj("C1"), R["MC_S"]["N1"][0], R["MC_S"]["N1"][2], R["MC_S"]["N1"][1]))
    w("")
    w("**Veredicto código 1: se queda el acta.** |Δ| llega a %s·σ_col, lejos del umbral de 2; la mejora de RMS es "
      "de décimas de punto y está dentro de lo que mueve una colocación." % c(d["1"]["zmc"], 2))
    w("")
    # código 2
    w("### 3.2 Código 2, amarillo intenso (recta anclada en el oscuro, decisión de Diego)")
    w("")
    w("Curva nueva: %s, anclada en x = %s. RMS %s; `#S` %s; incumplimientos: %d (%s)." % (
        fila_aj("N2"), c(x0, 2), c(AJ["N2"]["rms"], 2), "pasa" if R["ESC"]["N2"][0] else "no",
        R["INC"]["N2"][0], R["INC"]["N2"][1] or "ninguno"))
    w("")
    L += bloque_sig("2", "N2", "acta")
    w("")
    ids = ["fabrica", "acta", "N2", "C2", "N2g1"]
    L += tabla(["Tipo", "n", "Fábrica", "Acta de hoy", "Nueva anclada", "Anclada, una fila por colocación",
                "Grado 1 libre (control)"], tipo_tabla("2", ids),
               [":---:", "---:", "---:", "---:", "---:", "---:", "---:"])
    w("")
    w("El grado 1 libre (%s) sigue bloqueado por B13: %s." % (fila_aj("N2g1"), R["ESC"]["N2g1"][1]))
    w("")
    w("**Veredicto código 2: se queda el acta.** |Δ| llega a %s·σ_col, lejos del umbral de 2. La pendiente cambia "
      "un %s %%." % (
        c(d["2"]["zmc"], 2), cs(100 * (AJ["N2"]["co"][2] / acta["2"][2] - 1), 2)))
    w("")
    # código 8
    w("### 3.3 Código 8, amarillo tipo I (P34, P37, P43, P44; sólo 10:33)")
    w("")
    rows = []
    for idc, nom in (("N8", "Grado 1"), ("N8A", "Recta anclada")):
        mc = R["MC_S"][idc]
        n, sg, rms, mx = resumen([q for q in medidos if q["codigo"] == "8"], "e_" + idc)
        chk_r600 = ev(AJ[idc]["co"], 600)
        rows.append([nom, "`%s` / `%s`" % (AJ[idc]["txt"][2], AJ[idc]["txt"][3]), "%s / %s" % (cs(sg), c(rms)),
                     c(mx), c(chk_r600, 2), "%d de %d" % (mc[0], mc[2]), "sí" if R["ESC"][idc][0] else "no",
                     R["INC"][idc][0]])
    n, sg, rms, mx = res["8"]["fabrica"]
    rows.append(["Fábrica", "cúbica de 2020", "%s / %s" % (cs(sg), c(rms)), c(mx), c(ev(fab["8"], 600), 2), "—",
                 "—", "—"])
    L += tabla(["Curva", "c1 / c0", "Sesgo / RMS %", "Máx. |e| %", "R(600)", "MC: falla `#S`", "Escribible",
                "Incumple RF-CAL"], rows, [":---", ":---", "---:", "---:", "---:", "---:", ":---:", "---:"])
    w("")
    L += bloque_sig("8", "N8A", "fábrica")
    w("")
    w("**Veredicto código 8: escribir ya la recta anclada.** Frente a la fábrica, la diferencia llega a %s·σ_col: "
      "es significativa. El grado 1 da casi lo mismo en los patrones, pero corta el cero en x ≈ 598 y pasa `#S` "
      "por %s unidades en x = 600: una re-medida lo dejaría sin poder escribir en %d de %d casos. La anclada "
      "no depende de eso (0 de %d) y usa el mismo método que el código 2. **Queda con K = 1 por patrón:** su "
      "re-medida de verificación (RF-CAL-18) es la que confirma." % (
          c(d["8"]["zmc"], 1), c(ev(AJ["N8"]["co"], 600), 2), R["MC_S"]["N8"][0], R["MC_S"]["N8"][2],
          R["MC_S"]["N8A"][2]))
    w("")
    # código b
    w("### 3.4 Código b, rojo tipo I (P38, P39, P49)")
    w("")
    rows = []
    for idc, nom in (("Nb", "Grado 1"), ("NbA", "Recta anclada")):
        l = [q for q in medidos if q["codigo"] == "b"]
        n, sg, rms, mx = resumen(l, "e_" + idc)
        rows.append([nom, "`%s` / `%s`" % (AJ[idc]["txt"][2], AJ[idc]["txt"][3]),
                     " · ".join("%s %s" % (q["p"], cs(q["e_" + idc])) for q in l), "%s / %s" % (cs(sg), c(rms)),
                     "sí" if R["ESC"][idc][0] else "no: " + R["ESC"][idc][1]])
    l = [q for q in medidos if q["codigo"] == "b"]
    n, sg, rms, mx = res["b"]["fabrica"]
    rows.append(["Fábrica", "—", " · ".join("%s %s" % (q["p"], cs(q["e_fabrica"])) for q in l),
                 "%s / %s" % (cs(sg), c(rms)), "—"])
    L += tabla(["Curva", "c1 / c0", "Error por patrón %", "Sesgo / RMS %", "Escribible"], rows,
               [":---", ":---", ":---", "---:", ":---"])
    w("")
    w("Incumplimientos de la anclada: %s. MC de colocación: σ de R en x = 800 = %s; falla `#S` en %d de %d." % (
        R["INC"]["NbA"][1], c(R["MC"]["NbA"][800][1]), R["MC_S"]["NbA"][0], R["MC_S"]["NbA"][2]))
    w("")
    w("**Veredicto código b: se puede escribir ya, con la recta anclada; el banco no lo mejora.** P51-P132 no "
      "trae ningún rojo tipo I (el catálogo tiene tipo I sólo en P32a-P50), así que esperar al banco no aporta "
      "nada. La fábrica lee un 70 %% por debajo; la anclada deja los tres patrones en ±15 %%. La de grado 1 no se "
      "puede escribir: los tres patrones cubren sólo %s cuentas de x. Incumple RF-CAL-14 en dos patrones: "
      "necesita la conformidad de Diego, como la tuvieron los códigos 1 y 2." % c(
          max(comb[q["p"]]["x"] for q in l) - min(comb[q["p"]]["x"] for q in l), 0))
    w("")
    # resto
    w("### 3.5 Códigos 3, 4, 5, 6, 7, a, c y d: ¿ahora o con el banco?")
    w("")
    w("Cobertura según `Asistente.cobertura` (`Asistente.java:101`): hacen falta 3 niveles certificados y un "
      "rango de al menos 20 unidades de R y el 30 % del mayor (`:34-36`, `:93`). La recta anclada pasa por el "
      "mismo filtro (`Asistente.proponer`, `:416-432`).")
    w("")
    rows = []
    for k in "34567acd":
        hoy = R["cob"][("P1-P50", k)]
        ban = R["cob"][("P1-P132", k)]
        n, sg, rms, mx = res[k]["fabrica"]
        fabtxt = "%s / %s" % (cs(sg), c(rms)) if n else "sin patrones"
        if ban[0] > 0 and hoy[0] == 0:
            ver = "**Esperar al banco**"
        elif ban[0] == 0 and k in "5":
            ver = "**Ni con el banco**: sólo verificar"
        else:
            ver = "Sólo verificar; el banco no aporta (no trae tipo I)" if k in "7acd" else "—"
        rows.append([k, NOMBRE[k], hoy[1].split(": ", 1)[1], ban[1].split(": ", 1)[1], fabtxt, ver])
    L += tabla(["Código", "Lámina", "Hoy (P1-P50)", "Con el banco (P1-P132)", "Fábrica hoy: sesgo / RMS %",
                "Veredicto"], rows, [":---:", ":---", ":---", ":---", "---:", ":---"])
    w("")
    w("- **3 (verde):** hoy, 4 patrones de 164-173: rango estrecho. El banco añade de 51 a 121. La fábrica lee "
      "entre −14 y −18 %%, y en el oscuro da R = %s: el oscuro leería como una lámina verde de R ≈ 31." %
      c(ev(fab["3"], x0)))
    w("- **4 (rojo):** hoy, 2 niveles (194 y 227). El banco añade 24 niveles de 36 a 279, contando café y lila. "
      "La curva de fábrica es casi plana en esa zona (0,09 R por cuenta en x ≈ 1740), así que invertirla predice "
      "mal la x: véase §5.")
    w("- **5 (azul):** hoy de 83 a 85; con el banco, de 83 a 102 (19 %% del mayor, por debajo del 30 %%). **La app "
      "no lo va a dejar ajustar ni con el banco.** La fábrica lee un 55-58 %% por debajo (P17-P19). La única salida "
      "es una recta anclada con dispensa de la regla de cobertura; la anclada de hoy daría `c1 = %s`, RMS %s "
      "unidades, pero la app la bloquea por cobertura. Lo decide Diego." % (AJ["N5A"]["txt"][2],
                                                                           c(AJ["N5A"]["rms"], 2)))
    w("- **6 (naranja intenso):** ningún patrón hoy; el banco trae 15, de 80 a 173.")
    w("- **7, a, c y d (tipo I):** el banco no trae tipo I. Siguen como están: 7 lee el certificado (P35, un solo patrón), "
      "a lee 56-60 para 6-7, y c y d tienen rango estrecho.")
    w("")

    # ------------------------------------------------------------------ 4
    w("## 4. Simulación antes / ahora / propuesta")
    w("")
    w("«Antes» es la fábrica, «ahora» el acta de las 12:23 y «propuesta» el acta más los códigos 8 y b de §3. "
      "R es la respuesta del firmware: `Ecuacion.respuestaFloat32(round(x))`, con x la media combinada de §2.2.")
    w("")
    w("### 4.1 Los 50 patrones medidos")
    w("")
    rows = []
    for q in medidos:
        cc = comb[q["p"]]
        rows.append([q["p"], q["codigo"], q["tipo"], q["color"], c(q["cert"], 0), cc["K"], c(cc["x"]),
                     q["R_fabrica"], cs(q["e_fabrica"]), q["R_acta"], cs(q["e_acta"]), q["R_propuesta"],
                     cs(q["e_propuesta"])])
    L += tabla(["Patrón", "Cód.", "Tipo", "Color", "Cert.", "K", "x", "R fábrica", "e %", "R acta", "e %",
                "R propuesta", "e %"], rows,
               [":---", ":---:", ":---:", ":---", "---:", "---:", "---:", "---:", "---:", "---:", "---:", "---:",
                "---:"])
    w("")
    w("### 4.2 Resumen por código")
    w("")
    rows = []
    for k in CODIGOS:
        n = res[k]["fabrica"][0]
        if n == 0:
            rows.append([k, NOMBRE[k], 0, "—", "—", "—", "sin patrones"])
            continue
        cambia = "sí" if any(prop[k][i] != fab[k][i] for i in range(4)) else "no"
        rows.append([k, NOMBRE[k], n] + ["%s / %s / %s" % (cs(res[k][e][1]), c(res[k][e][2]), c(res[k][e][3]))
                                         for e in est] + ["acta" if k in "12" else ("nueva" if cambia == "sí"
                                                                                    else "fábrica")])
    L += tabla(["Código", "Lámina", "n", "Fábrica: sesgo / RMS / máx %", "Acta", "Propuesta", "Curva propuesta"],
               rows, [":---:", ":---", "---:", "---:", "---:", "---:", ":---"])
    w("")
    w("### 4.3 Resumen por tipo de lámina")
    w("")
    rows = []
    for t in ("IV", "IX", "XI", "I"):
        for grupo, cods in (("intensas 1-6", "123456"), ("1 y 2", "12"), ("tipo I 7-d", "78abcd"),
                            ("8 y b", "8b")):
            l = [q for q in medidos if q["tipo"] == t and q["codigo"] in cods]
            if not l or (t == "I") != (grupo in ("tipo I 7-d", "8 y b")):
                continue
            rows.append([t, grupo, len(l)] + ["%s / %s" % (cs(resumen(l, "e_" + e)[1]), c(resumen(l, "e_" + e)[2]))
                                              for e in est])
    L += tabla(["Tipo", "Códigos", "n", "Fábrica: sesgo / RMS %", "Acta", "Propuesta"], rows,
               [":---:", ":---", "---:", "---:", "---:", "---:"])
    w("")
    w("En las intensas 3-5 los tres estados coinciden (fábrica): es donde queda el error grande, y es lo que el "
      "banco tiene que resolver.")
    w("")
    w("### 4.4 ¿El acta predice lo que no usó?")
    w("")
    w("El acta se ajustó sólo con 10:33. Con la mañana, la A5 y las re-medidas, que no usó, se ve cómo "
      "predice fuera de su muestra:")
    w("")
    rows = []
    for k in "12":
        for sesn, nom in (("10:33", "10:33 (en muestra)"), ("otras", "mañana + A5 + re-medidas (fuera)")):
            l = []
            for q in medidos:
                if q["codigo"] != k or (q["p"], sesn) not in R["xs_ses"] or (q["p"], "otras") not in R["xs_ses"]:
                    continue
                l.append(dict(ea=err(EV[q["p"] + "|acta@" + sesn][1], q["cert"]),
                              ef=err(EV[q["p"] + "|fabrica@" + sesn][1], q["cert"])))
            rows.append([k, nom, len(l), "%s / %s" % (cs(resumen(l, "ef")[1]), c(resumen(l, "ef")[2])),
                         "%s / %s" % (cs(resumen(l, "ea")[1]), c(resumen(l, "ea")[2]))])
    L += tabla(["Código", "x de", "n", "Fábrica: sesgo / RMS %", "Acta: sesgo / RMS %"], rows,
               [":---:", ":---", "---:", "---:", "---:"])
    w("")
    w("Mismos patrones en las dos filas: los que tienen alguna colocación fuera de 10:33.")
    w("")
    w("### 4.5 Oscuro y regla B13")
    w("")
    w("B13: R en el oscuro ≤ máx(fábrica + 10 ; 25) (`Asistente.java:237-247`). R es la respuesta float32.")
    w("")
    rows = []
    for k in CODIGOS:
        fila = [k, NOMBRE[k]]
        for e in est:
            ch = CHK[e + "|" + k]
            fila.append("%s / %s" % (ch[5], ch[7]))
        chp = CHK["propuesta|" + k]
        fila.append("cumple" if chp[2] == "OK" and chp[3] == "OK" else "**no**")
        rows.append(fila)
    L += tabla(["Código", "Lámina", "Fábrica: R(%s) / R(575)" % c(x0, 1), "Acta", "Propuesta",
                "B13 de la propuesta"], rows, [":---:", ":---", "---:", "---:", "---:", ":---:"])
    w("")
    w("### 4.6 Criterio de `#S` y forma (C2)")
    w("")
    w("`#S`: R finito y en [0 ; 4000] en toda x de 600 a 4300, en float32, lo que cubre los bordes y los "
      "extremos interiores (`Asistente.criterioFirmwareS`, igual en efecto a `calibracion_v36.c:404-420`). C2: "
      "creciente, sin negativos desde el patrón más bajo y ≤ 4000 en 200-4400 (`Asistente.validarForma`).")
    w("")
    rows = []
    for k in CODIGOS:
        fila = [k]
        for e in est:
            ch = CHK[e + "|" + k]
            fila.append("%s; C2 %s" % ("`#S` sí" if ch[0] == "OK" else "`#S` **no** (" + ch[0].split(" (")[0] + ")",
                                       "sí" if ch[1] == "OK" else "no (" + ch[1].split(" / ")[0] + ")"))
        fila.append("%s / %s" % (CHK["propuesta|" + k][9], CHK["propuesta|" + k][11]))
        rows.append(fila)
    L += tabla(["Código", "Fábrica", "Acta", "Propuesta", "Propuesta: R(600) / R(4300)"], rows,
               [":---:", ":---", ":---", ":---", "---:"])
    w("")
    w("Las curvas de fábrica que no pasan `#S` o C2 están en ROM y no se escriben con `#S`: el dato es "
      "informativo. **Todas las curvas escritas o propuestas pasan `#S`.**")
    w("")

    # ------------------------------------------------------------------ 5
    w("## 5. Predicción para P51-P132 (ESTIMACIÓN, sin medir)")
    w("")
    w("**Nada de esta tabla está medido.** Sirve para que, en el banco, un patrón que caiga fuera de su banda "
      "se levante y se vuelva a colocar en el momento.")
    w("")
    w("- **x fábrica:** la curva de fábrica invertida, primera x ≥ oscuro con R(x) = certificado. Es lo que "
      "pide el encargo, pero en 3, 4 y 5 la fábrica se equivoca mucho (§4.2), y en el rojo es casi plana: esa "
      "x no sirve para vigilar.")
    w("- **x esperada:** la que hay que usar. En 1 y 2, la curva del acta invertida, corregida por la mediana "
      "medida en P1-P50 (dispersión %s %% y %s %%). En 3, 4 y 5, recta por el oscuro con la sensibilidad "
      "medida, k = cert / (x − oscuro): verde %s, rojo %s y azul %s R/cuenta (n = 4, 2 y 3). En 6 no hay "
      "ningún patrón medido: sólo la fábrica." % (
          c(100 * R["ratio"]["1"]["sd"], 1), c(100 * R["ratio"]["2"]["sd"], 1), c(R["ratio"]["3"]["k"], 4),
          c(R["ratio"]["4"]["k"], 4), c(R["ratio"]["5"]["k"], 4)))
    w("- **Banda (±):** 2·√((s_modelo·(x − oscuro))² + (0,022·x)²). s_modelo es la dispersión relativa medida "
      "en el código (1: %s %%, 2: %s %%, 3: %s %%, 4: %s %%, 5: %s %%). En el 6 es el error máximo de fábrica "
      "del naranja tipo I (d, que tiene la misma ecuación): %s %%, una **heurística**." % (
          c(100 * R["ratio"]["1"]["sd"], 1), c(100 * R["ratio"]["2"]["sd"], 1), c(100 * R["ratio"]["3"]["sd"], 1),
          c(100 * R["ratio"]["4"]["sd"], 1), c(100 * R["ratio"]["5"]["sd"], 1), c(100 * R["sd_pool"], 0)))
    w("- **R de cada estado:** respuesta float32 en la x esperada. En 3-6 los tres estados son la fábrica. En 1 "
      "y 2 el acta da el certificado por construcción: lo informativo es cuánto leería la fábrica.")
    w("")
    rows = []
    for q in banco:
        rows.append([q["p"], q["codigo"], q["tipo"], q["color"], c(q["cert"], 0), c(q["x_fab"], 0),
                     c(q["x_esp"], 0), "±" + c(q["banda"], 0),
                     "%s-%s" % (c(q["x_esp"] - q["banda"], 0), c(q["x_esp"] + q["banda"], 0)),
                     q["R_fabrica"], q["R_acta"], q["R_propuesta"]])
    L += tabla(["Patrón", "Cód.", "Tipo", "Color", "Cert.", "x fábrica", "x esperada", "Banda", "Aceptar x en",
                "R fábrica", "R acta", "R propuesta"], rows,
               [":---", ":---:", ":---:", ":---", "---:", "---:", "---:", "---:", ":---:", "---:", "---:", "---:"])
    w("")
    w("A tener en cuenta en el banco:")
    w("")
    v51 = [q for q in banco if q["codigo"] == "3" and q["cert"] <= 54]
    w("- **Verdes de R 51-54** (%s): x esperada ≈ %s, sólo unas %s cuentas por encima del oscuro, con la "
      "fábrica leyendo ≈ %s. Son los patrones con menos señal del banco." % (
          ", ".join(q["p"] for q in v51), c(st.mean(q["x_esp"] for q in v51), 0),
          c(st.mean(q["x_esp"] for q in v51) - x0, 0), c(st.mean(q["R_fabrica"] for q in v51), 0)))
    cafe = [q for q in banco if q["color"] in ("cafe", "café")]
    w("- **Café** (%s, R 36-68): con el código del rojo, la fábrica leería %s. La curva de fábrica del rojo "
      "ya da R ≈ %s en el propio oscuro." % (", ".join(q["p"] for q in cafe),
                                           " / ".join(str(q["R_fabrica"]) for q in cafe), c(ev(fab["4"], x0), 0)))
    w("- **Rojo, café y lila:** la x esperada sale de dos patrones IV (P11, P12). Es la estimación más floja "
      "de la tabla. La k de los XI puede ser otra.")
    w("- **Naranja intenso:** sin ninguna referencia medida. La banda es ancha a propósito.")
    w("")

    # ------------------------------------------------------------------ 6
    w("## 6. Veredicto")
    w("")
    rows = [
        ["1", "**No reescribir.** Se queda el acta", "Δ máx %s·σ_col; mejora dentro de la reproducibilidad" % c(
            d["1"]["zmc"], 2), "Con el banco (P51, P52, P56, P58, P73, P88, P103, P118): 16 niveles de 347 a 828"],
        ["2", "**No reescribir.** Se queda el acta", "Δ máx %s·σ_col" % c(d["2"]["zmc"], 2),
         "Con el banco: baja hasta R 207 (P61) y da puntos por debajo de P22, donde hoy manda el ancla"],
        ["8", "**Escribir ya: recta anclada** %s" % fila_aj("N8A"),
         "Frente a fábrica, Δ hasta %s·σ_col; tipo I sesgo %s %% y RMS %s %% frente a %s / %s de fábrica; pasa "
         "`#S` (R(600) = %s) y B13; cumple RF-CAL-14/15/16" % (
             c(d["8"]["zmc"], 1), cs(res["8"]["propuesta"][1]), c(res["8"]["propuesta"][2]),
             cs(res["8"]["fabrica"][1]), c(res["8"]["fabrica"][2]), c(ev(AJ["N8A"]["co"], 600), 1)),
         "El banco no trae tipo I"],
        ["b", "**Se puede escribir ya: recta anclada** %s, con conformidad de Diego a RF-CAL-14" % fila_aj("NbA"),
         "De −70 %% a sesgo %s %% y RMS %s %%; pasa `#S` y B13" % (cs(res["b"]["propuesta"][1]),
                                                               c(res["b"]["propuesta"][2])),
         "El banco no trae rojo tipo I: no lo mejora"],
        ["3, 4, 6", "**Esperar al banco**", "Hoy la app no los deja ajustar (cobertura)",
         "El banco los hace ajustables hasta grado 2"],
        ["5", "**Ni ahora ni con el banco**", "Rango 83-102: cobertura insuficiente", "Recta anclada sólo con "
                                                                                     "dispensa de Diego"],
        ["7, a, c, d", "Sin cambios", "Uno o dos niveles, o rango estrecho", "El banco no trae tipo I"],
    ]
    L += tabla(["Código", "Qué hacer", "Por qué", "Banco P51-P132"], rows, [":---:", ":---", ":---", ":---"])
    w("")
    w("**Orden sugerido, uno cada vez (P9-B8, `tramas:1698`):** 8 y, si Diego da la conformidad, b; cada uno con su re-medida "
      "de verificación (RF-CAL-18), P43 o P34 para el 8 y P49 para el b. Los códigos 1 y 2 no se tocan hasta el "
      "banco.")
    w("")
    w("## 7. Qué no dice este documento")
    w("")
    w("- Que s_rep = 2,2 % valga para todo el rango: salió de P22, P28 y P4 (x 1400-3300). Para el tipo I "
      "(x 600-1150) no hay A5, y 2,2 % de x allí son 13-25 cuentas de una señal de 30-600.")
    w("- Que las sesiones sean intercambiables: la mañana fue con firmware 3.6.0 y la A5 tras cambiar la "
      "batería. Se han combinado porque es lo que pide el encargo y porque, con s_rep 2,2 %, las diferencias "
      "entre sesiones de §2.1 (−1 a −6 %) caben en 2-3 colocaciones.")
    w("- Nada de P51-P132: la §5 es una estimación.")
    w("")
    return L


def escribir_csv(R):
    cat, comb = R["cat"], R["comb"]
    cab = ["patron", "valor_certificado", "tipo", "color", "codigo", "origen", "K", "sesiones", "x", "sigma_x",
           "x_fabrica_invertida", "x_esperada", "banda_x", "R_fabrica", "error_fabrica_pct", "R_acta",
           "error_acta_pct", "R_propuesta", "error_propuesta_pct"]
    with open(SALIDA_CSV, "w", encoding="utf-8", newline="") as h:
        wr = csv.writer(h, lineterminator="\n")
        wr.writerow(cab)
        for q in sorted(cat, key=lambda q: orden_p(q["p"])):
            med = q["p"] in comb
            fila = [q["p"], "%g" % q["cert"], q["tipo"], q["color"], q["codigo"], "medido" if med else "estimacion"]
            if med:
                cc = comb[q["p"]]
                fila += [cc["K"], "+".join(cc["sesiones"]), "%.1f" % cc["x"], "%.1f" % cc["sx"],
                         "%.1f" % q["x_fab"] if not math.isnan(q["x_fab"]) else "", "", ""]
            else:
                fila += ["", "", "", "", "%.1f" % q["x_fab"], "%.1f" % q["x_esp"], "%.1f" % q["banda"]]
            for e in ("fabrica", "acta", "propuesta"):
                fila.append(q["R_" + e])
                fila.append("%.1f" % q["e_" + e] if med else "")
            wr.writerow(fila)


def main():
    R = simular(calcular())
    L = escribir(R)
    with open(SALIDA_MD, "w", encoding="utf-8", newline="\n") as h:
        h.write("\n".join(L) + "\n")
    escribir_csv(R)
    print("escrito", SALIDA_MD, len(L), "lineas;", SALIDA_CSV)
    for k, v in R["decide"].items():
        print(k, v, R["AJ"][v["id"]]["txt"])


if __name__ == "__main__":
    main()
