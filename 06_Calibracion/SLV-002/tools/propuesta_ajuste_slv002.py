#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera 06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md.

Propuesta EN PAPEL: no escribe nada en el equipo.

Qué hace:
  1. Comprueba el md5 del ZIP de la campaña y extrae campana.csv y el diario a un directorio temporal.
  2. Compila con JDK 11 las clases de la app (03_App_Movil/RetroV36/app/src/main/java) y
     tools/CotejoAjusteSLV002.java, y lo ejecuta: el ajuste, los criterios de #S y C2, la curva de
     fábrica, la serie elegida y la validación cruzada salen del MISMO código de la app
     (Campana, Asistente, Ajuste, Ecuacion, Fabrica). Aquí no se reimplementa ningún ajuste.
  3. Calcula en Python la reproducibilidad mañana/campaña (fixture consolidado de la mañana,
     primer disparo de cada serie descartado) y los resúmenes por tipo, y escribe el Markdown.

Uso (desde la raíz del repositorio D:\\IT\\P_RetroVertical_V3.6):
  python 06_Calibracion/SLV-002/tools/propuesta_ajuste_slv002.py
Variables: RETRO_JDK = carpeta bin de un JDK 11 (por defecto la de la máquina de Diego).
"""
import csv
import hashlib
import io
import math
import os
import statistics as st
import subprocess
import sys
import tempfile
import zipfile
from datetime import datetime

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
ZIP = os.path.join(RAIZ, "06_Calibracion", "SLV-002", "campanas", "campana_SLV-002_20260919_103300.zip")
ZIP_MD5 = "4c50dbf63d53bd7f53cbd9856a0e5805"
FIXTURE = os.path.join(RAIZ, "03_App_Movil", "RetroV36", "app", "src", "test", "resources",
                       "medidas_SLV-002_20260919_consolidado.csv")
APP_SRC = os.path.join(RAIZ, "03_App_Movil", "RetroV36", "app", "src", "main")
CATALOGO = os.path.join(APP_SRC, "assets", "patrones_certificados_P1-P31.csv")
DRIVER = os.path.join(RAIZ, "06_Calibracion", "SLV-002", "tools", "CotejoAjusteSLV002.java")
SALIDA = os.path.join(RAIZ, "06_Calibracion", "SLV-002", "PROPUESTA-Ajuste-SLV-002-2026-09-19.md")
JDK = os.environ.get("RETRO_JDK", r"D:\@Proyect\Baliza\7 sw apk\jdk-11\jdk-11.0.24+8\bin")
X_OSCURO = 575  # ACTA-antes-y-despues-grabacion.md:40 (una observación, no una serie)


def md5(b):
    return hashlib.md5(b).hexdigest()


def c(v, d=1):
    """Número con coma decimal."""
    if v is None or (isinstance(v, float) and math.isnan(v)):
        return "—"
    return f"{v:.{d}f}".replace(".", ",")


def cs(v, d=1):
    """Con signo."""
    if v is None or (isinstance(v, float) and math.isnan(v)):
        return "—"
    return f"{v:+.{d}f}".replace(".", ",")


def git(*a):
    try:
        return subprocess.run(["git", "-C", RAIZ] + list(a), capture_output=True, text=True, check=True).stdout.strip()
    except Exception:
        return "?"


# ----------------------------------------------------------------------------- datos
def extraer():
    raw = open(ZIP, "rb").read()
    if md5(raw) != ZIP_MD5:
        sys.exit("md5 del ZIP distinto del de HUELLAS.txt: " + md5(raw))
    z = zipfile.ZipFile(io.BytesIO(raw))
    tmp = tempfile.mkdtemp(prefix="slv002_")
    for n in ("campana.csv", "diario_campana_SLV-002_00211305193B.csv"):
        open(os.path.join(tmp, n), "wb").write(z.read(n))
    return tmp, {n: md5(z.read(n)) for n in ("campana.csv", "diario_campana_SLV-002_00211305193B.csv")}


def series_campana(ruta):
    ser = {}
    for r in csv.DictReader(open(ruta, encoding="utf-8")):
        s = ser.setdefault(r["serie_id"], {"patron": r["patron"], "orient": r["orientacion"],
                                            "aceptada": r["aceptada"] == "1", "elegida": r["elegida"] == "1",
                                            "cert": float(r["valor_certificado"]), "tipo": r["tipo_lamina"],
                                            "hora": r["fecha_hora"][11:19], "x": []})
        if r["descartado"] == "0" and r["x"] != "":
            s["x"].append(float(r["x"]))
    return ser


def series_manana():
    """Series de la mañana: corte si cambia el patrón o pasan más de 6 s entre disparos."""
    filas = [r for r in csv.DictReader(l for l in open(FIXTURE, encoding="utf-8") if not l.startswith("#"))]
    out, prev = [], None
    for r in filas:
        t = datetime.strptime(r["fecha_hora"][:19], "%Y-%m-%dT%H:%M:%S")
        if prev is None or r["patron"] != prev[0] or (t - prev[1]).total_seconds() > 6:
            out.append({"patron": r["patron"], "hora": r["fecha_hora"][11:19], "x": [],
                        "cert": float(r["valor_certificado"]), "tipo": r["tipo_lamina"]})
        out[-1]["x"].append(float(r["x"]))
        prev = (r["patron"], t)
    for s in out:
        s["todos"] = list(s["x"])
        s["x"] = s["x"][1:]  # primer disparo descartado (asentamiento)
        # P7 09:19:26: el 3031 es el descolgado que marca la app (CampanaTest.java, descolgadoC46P2RealNoSaltaP7Si)
        s["descolgado"] = [v for v in s["x"] if s["patron"] == "P7" and v == 3031]
        s["x"] = [v for v in s["x"] if v not in s["descolgado"]]
    return out


# ----------------------------------------------------------------------------- java
def fuentes_app(tmp, commit):
    """Fuentes de la app tal como están CONFIRMADAS en 'commit' (no el árbol de trabajo, que otros agentes
    pueden estar cambiando): git archive a un directorio temporal."""
    r = subprocess.run(["git", "-C", RAIZ, "archive", "--format=zip", commit,
                        "03_App_Movil/RetroV36/app/src/main/java"], capture_output=True, check=True)
    dst = os.path.join(tmp, "app_" + commit)
    zipfile.ZipFile(io.BytesIO(r.stdout)).extractall(dst)
    return os.path.join(dst, "03_App_Movil", "RetroV36", "app", "src", "main", "java")


def correr_java(tmp, sigma_rel, commit):
    cls = os.path.join(tmp, "cls")
    os.makedirs(cls, exist_ok=True)
    javac = os.path.join(JDK, "javac")
    java = os.path.join(JDK, "java")
    subprocess.run([javac, "-encoding", "UTF-8", "-d", cls, "-sourcepath", fuentes_app(tmp, commit), DRIVER],
                   check=True)
    r = subprocess.run([java, "-Dfile.encoding=UTF-8", "-cp", cls, "CotejoAjusteSLV002", CATALOGO,
                        os.path.join(tmp, "diario_campana_SLV-002_00211305193B.csv"), repr(sigma_rel)],
                       check=True, capture_output=True)
    return r.stdout.decode("utf-8").splitlines()


def parsear(lineas):
    d = {"PUNTO": {}, "AJUSTE": {}, "RES": {}, "LOO": {}, "EVAL": {}, "MC": {}, "S": {}, "C2": {},
         "ESCRIBIBLE": {}, "MC_S": {}, "INFORME": {}, "COBERTURA": {}, "FABRICA_S": {}, "POSICION": [],
         "ELEGIDA": {}, "SERIE": {}}
    for l in lineas:
        c_ = l.split("\t")
        t = c_[0]
        if t == "PUNTO":
            d["PUNTO"].setdefault(c_[1], []).append(dict(p=c_[2], tipo=c_[3], cert=float(c_[4]), x=float(c_[5]),
                                                         n=int(c_[6]), sd=float(c_[7]), rfab=float(c_[8]),
                                                         rfab32=int(c_[9]), dfab=float(c_[10])))
        elif t == "AJUSTE":
            d["AJUSTE"][c_[1]] = dict(c3=c_[2], c2=c_[3], c1=c_[4], c0=c_[5], rms=float(c_[6]), emax=float(c_[7]))
        elif t == "RES":
            d["RES"].setdefault(c_[1], []).append(dict(p=c_[2], tipo=c_[3], cert=float(c_[4]), x=float(c_[5]),
                                                       r=float(c_[6]), res=float(c_[7]), r32=int(c_[8]),
                                                       der=float(c_[9])))
        elif t == "LOO":
            d["LOO"].setdefault(c_[1], []).append(dict(p=c_[2], cert=float(c_[3]), pred=float(c_[4]), err=float(c_[5])))
        elif t == "EVAL":
            d["EVAL"].setdefault(c_[1], {})[int(c_[2])] = (float(c_[3]), int(c_[4]), float(c_[5]))
        elif t == "MC":
            d["MC"].setdefault(c_[1], {})[int(c_[2])] = (float(c_[3]), float(c_[4]))
        elif t in ("S", "ESCRIBIBLE", "MC_S", "FABRICA_S"):
            d[t][c_[1]] = c_[2:]
        elif t == "C2":
            d["C2"][c_[1]] = (c_[2], c_[3] if len(c_) > 3 else "")
        elif t == "INFORME":
            d["INFORME"].setdefault(c_[1], []).append(c_[2] if len(c_) > 2 else "")
        elif t == "COBERTURA":
            d["COBERTURA"][c_[1]] = (int(c_[2]), c_[3])
        elif t == "POSICION":
            d["POSICION"].append(c_[1])
        elif t == "ELEGIDA":
            d["ELEGIDA"][c_[1]] = dict(id=c_[2], orient=c_[3], n=int(c_[4]), media=float(c_[5]), sd=float(c_[6]))
        elif t == "SERIE":
            d["SERIE"][c_[1]] = dict(p=c_[2], orient=c_[3], acept=c_[4] == "1", n=int(c_[5]), media=float(c_[6]))
    return d


# ----------------------------------------------------------------------------- cálculo
def reproducibilidad(camp, man):
    """Una fila por serie de la mañana frente a la serie de la campaña a 0° del mismo patrón."""
    ref = {}
    for sid, s in camp.items():
        if s["aceptada"] and s["orient"] == "0" and s["x"]:
            ref[s["patron"]] = (sid, st.mean(s["x"]), st.stdev(s["x"]), len(s["x"]))  # la última aceptada a 0°
    filas = []
    for s in man:
        if s["patron"] not in ref:
            continue
        sid, mc, sc, nc = ref[s["patron"]]
        mm = st.mean(s["x"])
        filas.append(dict(p=s["patron"], tipo=s["tipo"], cert=s["cert"], hora=s["hora"], n=len(s["x"]), mm=mm,
                          sm=st.stdev(s["x"]) if len(s["x"]) > 1 else float("nan"), sid=sid, mc=mc, sc=sc,
                          d=mc - mm, dp=100 * (mc - mm) / mm, desc=s["descolgado"]))
    return filas


def por_tipo(filas, clave_err):
    tipos = {}
    for f in filas:
        tipos.setdefault(f["tipo"], []).append(100 * f[clave_err] / f["cert"])
    out = {}
    for t, v in tipos.items():
        out[t] = (len(v), st.mean(v), math.sqrt(sum(e * e for e in v) / len(v)))
    return out


def anclada(puntos):
    """Recta R = k·(x − oscuro) por mínimos cuadrados. FUERA del método de la app: sólo informativa."""
    k = sum(q["cert"] * (q["x"] - X_OSCURO) for q in puntos) / sum((q["x"] - X_OSCURO) ** 2 for q in puntos)
    filas = [dict(p=q["p"], tipo=q["tipo"], cert=q["cert"], e=k * (q["x"] - X_OSCURO) - q["cert"]) for q in puntos]
    return k, filas, por_tipo(filas, "e")


def main():
    tmp, md5s = extraer()
    camp = series_campana(os.path.join(tmp, "campana.csv"))
    man = series_manana()
    rep = reproducibilidad(camp, man)

    # Una comparación por patrón: la última serie de la mañana. P24 fuera del resumen (identidad dudosa).
    ultima = {}
    for f in rep:
        ultima[f["p"]] = f
    resumen = [f for p, f in ultima.items() if p != "P24"]
    dps = [f["dp"] for f in resumen]
    sd_d = st.stdev(dps)
    sigma_col = sd_d / math.sqrt(2)  # una medida (sesión + colocación), si las dos sesiones pesan igual
    sigma_rel = round(sigma_col / 100, 4)

    commit = os.environ.get("RETRO_COMMIT") or git("rev-parse", "--short", "HEAD")
    sucio = git("status", "--porcelain", "--", "03_App_Movil/RetroV36/app/src/main/java")
    lineas = correr_java(tmp, sigma_rel, commit)
    d = parsear(lineas)

    L = []
    w = L.append

    # ------------------------------------------------------------------ cabecera
    w("# Propuesta de ajuste de SLV-002 — campaña del 19-sep-2026")
    w("")
    w("**Sin validar en hardware y sin escribir nada en el equipo.** Es una propuesta en papel. Los coeficientes "
      "son **provisionales hasta P9-A5** (5 colocaciones de P22, P28 y P4, "
      "`05_Documentacion/REVISION-Arquitectura-P9-V3.6.md:454`), porque la reproducibilidad medida "
      "**entre sesiones** (2-5 %) es del mismo orden que los residuos del ajuste, y la que hay **entre colocaciones "
      "dentro de una sesión** está casi sin medir.")
    w("")
    w("Generado por `06_Calibracion/SLV-002/tools/propuesta_ajuste_slv002.py`. No editar a mano: se regenera.")
    w("")
    w("## 0. Datos y trazabilidad")
    w("")
    w("| Qué | Valor |")
    w("| :--- | :--- |")
    w(f"| Campaña | `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_103300.zip`, md5 `{ZIP_MD5}` (= `HUELLAS.txt`) |")
    w(f"| Dentro del ZIP | `campana.csv` md5 `{md5s['campana.csv']}`; diario md5 `{md5s['diario_campana_SLV-002_00211305193B.csv']}` "
      "(idénticos a los extraídos en `07 pruebas/campana_103300/`) |")
    w("| Equipo y firmware | SLV-002, MAC 00:21:13:05:19:3B; firmware 3.6.1 (`#V,3.6,2026-09-19,DEF,0000#`), app 3.6.5 |")
    w("| Sesión de la mañana | `03_App_Movil/RetroV36/app/src/test/resources/medidas_SLV-002_20260919_consolidado.csv`, "
      "08:59-09:25, firmware 3.6.0 (`V3.6 2026-09-18`), app 3.6.2. **Primer disparo de cada serie descartado** |")
    w(f"| Código que calcula | Clases de la app en `{commit}`" + (" (con cambios sin confirmar en el árbol)" if sucio else "") +
      ", compiladas con JDK 11 y llamadas desde `tools/CotejoAjusteSLV002.java`: `Campana.leerDiario` + "
      "`medidasElegidas` (`Campana.java:497`), `Asistente.puntos` (`Asistente.java:136`), `Ajuste.ajustar` "
      "(`Ajuste.java:41`), `Asistente.proponer` (`:280`), `criterioFirmwareS` (`:222`), `validarForma` (`:179`), "
      "`Ecuacion.respuestaFloat32` (`Ecuacion.java:56`), `Fabrica.ecuacion` (`Fabrica.java:62`). **El ajuste es el "
      "de la app, no una reimplementación** |")
    w("| Puntos del ajuste | Media de la serie elegida de cada patrón, como la app: sin evento `ELIGE` en el diario, "
      "la **última aceptada** (`Campana.elegida`, `Campana.java:304`). **Para P5 es S024, a 90°** (2459,3); a 0° "
      "(S023) da 2456,8. Se da el ajuste con las dos (§2.2) |")
    w("| Curva de fábrica | Tabla ROM `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:34-46`, igual a `Fabrica.java:62-88` |")
    w("| Criterio de `#S` | `calibracion_v36.c:316-319` (límites), `:407` (`curvaValida`), `:698` (llamada) en el fuente 3.6.2; "
      "en la 3.6.1 era `:587-588`. En la app, evaluación float32 en cada `x` entera de 600 a 4300 |")
    w(f"| Oscuro | x ≈ {X_OSCURO} (`ACTA-antes-y-despues-grabacion.md:40`). **Una sola observación, no una serie**: "
      "la cota \"575-620\" del encargo no tiene fuente en la V3.6 (599-634 es del firmware original, `TDD-V3.6.md:525`) |")
    w("| Criterios | `05_Documentacion/SPEC-Calibracion-V3.6.md` §5 (RF-CAL-13 `:345`, -14 `:356`, -15 `:362`, -16 `:370`, "
      "-17 `:373`), §4.4 `:294` (grado). **Todos propuestos** (P-CAL-01) |")
    w("")
    w(f"Resultado del diario leído por la app: {len(d['SERIE'])} series, "
      f"{len(d['ELEGIDA'])} entradas del catálogo con serie elegida (P32 cuenta dos: P32a y P32b).")
    ruta_as = "03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/Asistente.java"
    osc_arbol = "comprobarOscuro" in open(os.path.join(RAIZ, ruta_as), encoding="utf-8").read()
    osc_commit = "comprobarOscuro" in git("show", f"{commit}:{ruta_as}")
    if sucio and osc_arbol and not osc_commit:
        w("")
        w("**Aviso de versión.** En el árbol de trabajo hay cambios **sin confirmar** en `Asistente.java` (otro agente): "
          "`comprobarOscuro` (P9-B13), que bloquea toda curva con R(575) > máx(fábrica + 10 ; 25). Este documento usa "
          f"el código confirmado en `{commit}`; en §2 se dice qué curvas bloquearía esa regla si se confirma.")
    w("")

    # ------------------------------------------------------------------ 1. reproducibilidad
    w("## 1. Reproducibilidad: mañana frente a campaña")
    w("")
    w("Cada fila es una serie de la mañana (sin el primer disparo) frente a la serie aceptada a 0° de la campaña "
      "del mismo patrón. Δ = campaña − mañana. Las series de 08:59-09:05 son de 3 disparos (quedan 2).")
    w("")
    w("| Patrón | Tipo | Cert. | Mañana | n | Media mañana | s | Serie campaña | Media campaña | s | Δ (cuentas) | Δ % |")
    w("| :--- | :---: | ---: | :---: | ---: | ---: | ---: | :---: | ---: | ---: | ---: | ---: |")
    for f in rep:
        nota = " (sin el 3031)" if f["desc"] else ""
        w(f"| {f['p']} | {f['tipo']} | {c(f['cert'],0)} | {f['hora']}{nota} | {f['n']} | {c(f['mm'])} | {c(f['sm'])} | "
          f"{f['sid']} | {c(f['mc'])} | {c(f['sc'])} | {cs(f['d'])} | {cs(f['dp'],2)} |")
    w("")
    sdins = [f["sc"] / f["mc"] * 100 for f in resumen]
    w(f"**Resumen, una comparación por patrón** (la última serie de la mañana; {len(resumen)} patrones; P24 fuera por "
      "identidad dudosa, §1.4):")
    w("")
    w(f"- Δ medio **{cs(st.mean(dps),2)} %**, mediana {cs(st.median(dps),2)} %, desviación típica **{c(sd_d,2)} %**, "
      f"de **{cs(min(dps),2)} %** ({min(resumen, key=lambda f: f['dp'])['p']}) a **{cs(max(dps),2)} %** "
      f"({max(resumen, key=lambda f: f['dp'])['p']}). |Δ| mediano {c(st.median([abs(v) for v in dps]),2)} %.")
    w(f"- Dentro de la serie, en la campaña: s relativa mediana **{c(st.median(sdins),2)} %** "
      f"({c(min(sdins),2)}-{c(max(sdins),2)} %). La dispersión entre sesiones es "
      f"**{c(sd_d/st.median(sdins),0)} veces** la de dentro de la serie.")
    w(f"- Signos mezclados en patrones vecinos: P1 {cs(ultima['P1']['dp'],1)} % y P6 {cs(ultima['P6']['dp'],1)} % "
      f"(los dos XI blancos, x ≈ 3000-3170). **No es una deriva común de ganancia** que un `c0` pueda absorber: "
      "es un error por patrón.")
    w(f"- Si las dos sesiones pesan igual, cada medida lleva σ ≈ {c(sd_d,2)} / √2 = **{c(sigma_col,2)} %** "
      "(sesión + colocación + deriva). Es la σ que usa la simulación de §2.")
    w("")
    w("### 1.1 Dentro de una misma sesión")
    w("")
    pares = [("P5 0° / 90°", "S023", "S024"), ("P23 repetida", "S049", "S050"), ("P37 repetida", "S002", "S003"),
             ("P50 repetida", "S021", "S022")]
    w("| Par (campaña) | Serie A | Media A | Serie B | Media B | Δ % |")
    w("| :--- | :---: | ---: | :---: | ---: | ---: |")
    for nom, a, b in pares:
        ma, mb = st.mean(camp[a]["x"]), st.mean(camp[b]["x"])
        w(f"| {nom} | {a} | {c(ma)} | {b} | {c(mb)} | {cs(100*(mb-ma)/ma,2)} |")
    mpares = {}
    for s in man:
        mpares.setdefault(s["patron"], []).append(s)
    w("")
    w("| Par (mañana) | Hora A | Media A (n) | Hora B | Media B (n) | Δ % |")
    w("| :--- | :---: | ---: | :---: | ---: | ---: |")
    for p, l in mpares.items():
        if len(l) == 2 and p != "P24":
            a, b = l
            ma, mb = st.mean(a["x"]), st.mean(b["x"])
            w(f"| {p} | {a['hora']} | {c(ma)} ({len(a['x'])}) | {b['hora']} | {c(mb)} ({len(b['x'])}) | {cs(100*(mb-ma)/ma,2)} |")
    w("")
    w("Dentro de la campaña, cuatro pares repiten a **0,1-0,9 %**. Sólo el de P5 es con seguridad una recolocación "
      "(el giro obliga a levantar el equipo); en los otros tres el CSV no dice si se levantó. Dentro de la mañana, "
      "P5 se movió **+2,3 %** en 26 min y P23 +1,2 % en 9 min. Es decir: **la reproducibilidad entre colocaciones "
      "dentro de una sesión está casi sin medir** (un par seguro), y lo que sí está medido es la variación **entre "
      "sesiones**, que mezcla colocación, tiempo, temperatura, un ciclo de apagado (`pruebas.txt`, 09:52: el equipo no "
      "respondía) y el cambio de firmware 3.6.0 → 3.6.1. El cambio de firmware no toca la ruta de medida "
      "(`01_Firmware/RetroVertical_V3.6.X/CAMBIOS-V3.6.md:181,317`), pero no se ha medido que no la toque. "
      "**P9-A5 es la medida que separa las dos cosas.**")
    w("")
    w("Tercer dato de P1: a las 08:53 dio 3021,6 (5 disparos, `ACTA-antes-y-despues-grabacion.md:38`), a las 09:16 "
      f"{c(ultima['P1']['mm'])} y a las 10:15 {c(ultima['P1']['mc'])}: **3,6 % de recorrido en 80 min** sobre el mismo patrón.")
    w("")
    w("### 1.2 Deriva dentro de la serie")
    w("")
    for l in d["POSICION"]:
        if "Media" in l:
            w(f"Con `Campana.desvioPorPosicion()` (`Campana.java:601`), desvío medio frente a la mediana de su serie, por "
              f"posición (el asentamiento ya descartado): `{l.split(':',1)[1].strip()}`.")
    w("")
    w("**Confirmado** −4,0 en la posición 1 y +3,3 en la 9. **No es monótona**: la 4 (−0,9) queda por debajo de la 3 "
      "(−0,6), y la 7 y la 8 por debajo de la 6. Es una rampa de ~7 cuentas (~0,3 % en x ≈ 2500), **diez veces menor** "
      "que la variación entre sesiones. Con el mismo número de disparos en campaña, verificación y campo, se la come `c0`.")
    w("")
    w("### 1.3 Qué incertidumbre de colocación hay que asumir")
    w("")
    w(f"- **Hoy hay que asumir σ ≈ {c(sigma_col,1)} % por medida** (una serie de 9 disparos en una colocación), hasta que "
      "P9-A5 diga cuánto es colocación y cuánto sesión. Es una cota **por arriba** de la colocación pura.")
    w(f"- En R, con la recta propuesta del código 2 (pendiente 0,318 R/cuenta), un {c(sigma_col,1)} % en x = 2500 son "
      f"{c(0.318*2500*sigma_col/100,0)} unidades de R; en el blanco (0,298) y x = 3000, {c(0.298*3000*sigma_col/100,0)}.")
    w("- **Criterio propuesto RF-CAL-13, \"reproducibilidad ≤ máx(3·s ; 1 %)\"** (`SPEC-Calibracion-V3.6.md:345-355`): "
      "con s ≈ 5-8 cuentas, 3·s es 0,6-1,2 % y manda el 1 %. "
      f"**{sum(1 for v in dps if abs(v) > 1)} de {len(dps)}** patrones lo incumplen entre sesiones. Dentro de la "
      "sesión, los cuatro pares de 1.1 lo cumplen. Por tanto el criterio **no mide la reproducibilidad que importa**: "
      "si las dos series son de la misma sesión, lo pasa casi todo; si son de sesiones distintas, rechaza casi todo. "
      "Propuesta: definirlo sobre **K colocaciones repartidas en al menos dos sesiones** y con umbral en función de la "
      "σ entre colocaciones que mida P9-A5, no de la s de disparo. El 3 % de la app 3.6.7 (`Veredicto.java:71`, "
      "`REPRO_MAX`) es un umbral de s **entre colocaciones**, no de diferencia entre dos medias: son magnitudes distintas.")
    w("")
    w("### 1.4 P24")
    w("")
    p24 = [f for f in rep if f["p"] == "P24"]
    w(f"P24 (IV, 593) da ahora **{c(ultima['P24']['mc'])}** (S047). Frente a cada candidata:")
    w("")
    w("| Candidata | Origen | Δ campaña frente a ella | ¿Compatible con −4,9…+2,8 %? |")
    w("| :--- | :--- | ---: | :--- |")
    for v, o in ((p24[0]["mm"], "mañana 09:04:12 (2071, 2076; sin el 2048)"),
                 (2065, "cifra citada en `SPEC-Calibracion-V3.6.md:603` (media de los 3 disparos)"),
                 (p24[1]["mm"], "mañana 09:04:43 (2438, 2453)"), (2804, "**no es una medida**: caso sintético de "
                  "`Veredicto.java:26` y `CampanaTest.java:74`")):
        dp = 100 * (ultima['P24']['mc'] - v) / v
        ok = "sí, en el borde" if -4.95 <= dp <= 2.8 else "no"
        w(f"| {c(v)} | {o} | {cs(dp,1)} % | {ok} |")
    w("")
    w("**La compatible es la de ~2065-2074.** La de 2443 (y la 2804) caen en la zona de los XI amarillos "
      "(P5/P9/P10 a 2457-2504; P20 a 2788-2804), a −19 % y −29 %, fuera de toda reproducibilidad medida. Además "
      "1982,7 encaja entre los IV amarillos: P29 (442) 1677, P30 (448) 1654. **P24 entra en el ajuste**, pero su "
      "identidad (etiqueta de S047) la confirma Diego, como pide C-40.")
    w("")

    # ------------------------------------------------------------------ 2. ajustes
    def tabla_ajuste(ident, k, titulo):
        a = d["AJUSTE"][ident]
        res = d["RES"][ident]
        fab = {q["p"]: q for q in d["PUNTO"][k]}
        w(f"#### {titulo}")
        w("")
        w(f"`c3 = {a['c3']}`, `c2 = {a['c2']}`, `c1 = {a['c1']}`, `c0 = {a['c0']}`")
        w("")
        w("| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |")
        w("| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
        for r in res:
            fq = fab[r["p"]]
            w(f"| {r['p']} | {r['tipo']} | {c(r['cert'],0)} | {c(r['x'])} | {c(r['r'])} | {cs(r['res'])} | "
              f"{cs(100*r['res']/r['cert'])} | {r['r32']} | {c(fq['rfab'])} | {cs(100*(fq['rfab']-fq['cert'])/fq['cert'])} |")
        w("")
        # por tipo: error = R_curva - cert
        nue = por_tipo([dict(tipo=r["tipo"], cert=r["cert"], e=-r["res"]) for r in res], "e")
        fb = por_tipo([dict(tipo=r["tipo"], cert=r["cert"], e=fab[r["p"]]["rfab"] - r["cert"]) for r in res], "e")
        w("| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |")
        w("| :---: | ---: | :--- | :--- | :---: | :---: |")
        for t in sorted(nue):
            n_, b_, r_ = nue[t]
            _, bf, rf = fb[t]
            ok15 = "cumple" if abs(b_) <= 5 and r_ <= 6 else "**no cumple**"
            ok16 = "cumple" if r_ < rf else "**no cumple**"
            w(f"| {t} | {n_} | {cs(b_)} / {c(r_)} | {cs(bf)} / {c(rf)} | {ok15} | {ok16} |")
        w("")
        ev = d["EVAL"][ident]
        s_ = d["S"][ident][0]
        c2, avisos = d["C2"][ident]
        mal14 = [r["p"] for r in res if abs(r["res"]) > max(0.10 * r["cert"], 2)]
        w(f"- RMS {c(a['rms'],2)} y error máximo {c(a['emax'],1)} unidades de R (`Ajuste.Resultado`).")
        w(f"- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): " + ("cumplen todos." if not mal14 else "**no cumplen " + ", ".join(mal14) + "**."))
        w(f"- Criterio de `#S`: **{'pasa' if s_ == 'OK' else 'NO pasa — ' + s_}**. R(600) = {c(ev[600][0],1)}.")
        w(f"- C2: **{'pasa' if c2 == 'OK' else 'NO pasa — ' + c2}**." + (f" Aviso: {avisos}." if avisos else ""))
        lim = max(ev[575][2] + 10, 25)
        w(f"- **Oscuro** (x = {X_OSCURO}): nueva {c(ev[575][0],1)} → responde {ev[575][1]}; fábrica {c(ev[575][2],1)}. "
          f"Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = {c(lim,0)}): "
          + ("**la bloquearía**." if ev[575][0] > lim else "pasa."))
        if ident in d["ESCRIBIBLE"]:
            e_ = d["ESCRIBIBLE"][ident]
            w(f"- `Asistente.proponer().escribible()`: **{'sí' if e_[0] == '1' else 'no'}**" + (f" ({e_[1]})" if len(e_) > 1 and e_[1] else "") + ".")
        mc = d["MC"][ident]
        fs = d["MC_S"][ident]
        w(f"- Colocación simulada (σ = {c(100*sigma_rel,2)} % en cada x, 2000 repeticiones, `Ajuste.ajustar`): "
          f"dispersión de R en x = 1000 / 2000 / 3000: ±{c(mc[1000][1],0)} / ±{c(mc[2000][1],0)} / ±{c(mc[3000][1],0)}. "
          f"La curva re-ajustada falla `#S` en **{fs[0]} de {fs[1]}**.")
        w("")
        w("| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |")
        w("| :--- | " + " | ".join(["---:"] * 10) + " |")
        w("| R nueva | " + " | ".join(c(ev[xx][0], 0) for xx in (575, 600, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4300)) + " |")
        w("| R fábrica | " + " | ".join(c(ev[xx][2], 0) for xx in (575, 600, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4300)) + " |")
        w("")
        return nue, fb

    w("## 2. Ajustes propuestos (códigos 1, 2, 8 y b)")
    w("")
    w("Sesgo = media de (R curva − cert)/cert; RMS igual, en %. Residuo = cert − R (convención de `Ajuste.java:17`). "
      "\"Firmware float32\" es `Ecuacion.respuestaFloat32(round(x))`, lo que respondería el equipo. Ninguna curva "
      "nueva tiene c3 (`Ajuste.java:86`).")
    w("")
    w("### 2.1 Código 1, blanco intenso (P1-P4, P6, P7, P27, P28)")
    w("")
    tabla_ajuste("1|1|", "1", "Grado 1 (recomendado)")
    tabla_ajuste("1|2|", "1", "Grado 2")
    r1, r2 = d["AJUSTE"]["1|1|"]["rms"], d["AJUSTE"]["1|2|"]["rms"]
    w(f"**Recomendación: grado 1.** El grado 2 baja el RMS de {c(r1,1)} a {c(r2,1)} (−{c(100*(1-r2/r1),0)} %), por "
      "debajo del 30 % que exige la condición 3 de §4.4 (`SPEC-Calibracion-V3.6.md:302-305`); empeora la validación "
      f"cruzada (P3: {cs(d['LOO']['1|2|'][2]['err'],0)} frente a {cs(d['LOO']['1|1|'][2]['err'],0)}); con la colocación simulada, "
      f"R(1000) baila ±{c(d['MC']['1|2|'][1000][1],0)} frente a ±{c(d['MC']['1|1|'][1000][1],0)}; y en el oscuro da "
      f"{c(d['EVAL']['1|2|'][575][0],0)} (B13 del arquitecto, confirmado). El grado 1 da {c(d['EVAL']['1|1|'][575][0],0)} en el oscuro, "
      f"por debajo de los {c(d['EVAL']['1|1|'][575][2],0)} de fábrica. Pero ojo: la recta pasa `#S` con R(600) = "
      f"{c(d['EVAL']['1|1|'][600][0],1)}, y en la simulación falla `#S` en {d['MC_S']['1|1|'][0]} de 2000: **una "
      "re-medida puede dar una recta que el firmware rechace**.")
    w("")
    w("### 2.2 Código 2, amarillo intenso (P5, P8-P10, P20-P26, P29-P31)")
    w("")
    tabla_ajuste("2|1|", "2", "Grado 1 (recomendado)")
    tabla_ajuste("2|2|", "2", "Grado 2")
    a0 = d["AJUSTE"]["2|1|P5a0"]
    w(f"**Recomendación: grado 1.** El grado 2 no se puede escribir: R(600) = {c(d['EVAL']['2|2|'][600][0],0)} (`#S` lo "
      "rechaza) y no es creciente desde x ≈ 3535 (C2). Es la forma de la curva de fábrica, que tampoco pasa `#S` "
      f"({d['FABRICA_S']['2'][0]}). **Pega del grado 1 en el oscuro:** {c(d['EVAL']['2|1|'][575][0],0)} frente a "
      f"{c(d['EVAL']['2|1|'][575][2],0)} de fábrica (confirma B13): una lámina amarilla degradada o el oscuro leerían ~84. "
      "Es consecuencia de que el patrón amarillo más bajo esté en x = 1497: por debajo, la recta es extrapolación. "
      "**Si se confirma la regla P9-B13 en curso, esta recta tampoco se podrá escribir** (84 > 31).")
    w("")
    k2, f2, t2 = anclada(d["PUNTO"]["2"])
    w(f"*Opción fuera del método de la app, sólo para que Diego la valore:* recta anclada en el oscuro, "
      f"R = k·(x − {X_OSCURO}), k = {c(k2,4)} (`c1 = {k2:.8E}`, `c0 = {-k2*X_OSCURO:.8E}`). Error por tipo (sesgo / RMS): "
      + "; ".join(f"{t} {cs(v[1])} / {c(v[2])} %" for t, v in sorted(t2.items())) +
      f". Peor patrón: " + ", ".join(f"{q['p']} {cs(100*q['e']/q['cert'],1)} %" for q in sorted(f2, key=lambda q: -abs(q['e'] / q['cert']))[:3]) +
      f". R(600) = {c(k2*(600-X_OSCURO),1)}: pasaría `#S`, C2 y la regla del oscuro. Empeora el IV y el XI frente a la recta "
      "libre; se apoya en un oscuro medido una vez. Es la pregunta que tiene que contestar Diego: **¿qué pesa más, el "
      "residuo en los patrones o no leer ~84 en una lámina amarilla muerta?**")
    w("")
    w(f"**Sensibilidad a P5 (0° frente a 90°):** con S023 (0°) la recta queda `c1 = {a0['c1']}`, `c0 = {a0['c0']}`; "
      f"la diferencia en R es < 0,1 unidades en todo el rango. **Da igual cuál se elija.**")
    w("")
    sp = d["AJUSTE"]["2|1|sinP24"]
    w(f"**Sensibilidad a P24 (no es propuesta):** sin P24, `c1 = {sp['c1']}`, `c0 = {sp['c0']}`, RMS {c(sp['rms'],1)} "
      f"(con P24, {c(d['AJUSTE']['2|1|']['rms'],1)}). En x = 2000 la curva cambia de "
      f"{c(d['EVAL']['2|1|'][2000][0],1)} a {c(d['EVAL']['2|1|sinP24'][2000][0],1)}. P24 se queda: su valor actual es "
      "compatible (§1.4).")
    w("")
    w("### 2.3 Código 8, amarillo tipo I (P34, P37, P43, P44)")
    w("")
    tabla_ajuste("8|1|", "8", "Grado 1 (recomendado)")
    w(f"El grado 2 se calculó sólo como control: no pasa `#S` (R(600) = {c(d['EVAL']['8|2|'][600][0],1)}) ni C2, y deja "
      "un grado de libertad. **El grado 1 pasa `#S` por 0,3 unidades** (R(600) = "
      f"{c(d['EVAL']['8|1|'][600][0],2)}): la recta corta el cero en x ≈ 598, casi en el oscuro. Es coherente con la física "
      "(señal = x − oscuro), pero deja el criterio de `#S` en el filo: con la colocación simulada falla en "
      f"**{d['MC_S']['8|1|'][0]} de 2000**. En el oscuro responde 0, como la fábrica (~0). "
      "P37 (82) lee más que P34 (86): 992 frente a 958, un 3,5 %, dentro de la reproducibilidad; nota de campaña de S003: "
      "\"papel de baja calidad\".")
    w("")
    w("### 2.4 Código b, rojo tipo I (P38, P39, P49)")
    w("")
    tabla_ajuste("b|1|", "b", "Grado 1 (NO escribible)")
    xb = [q["x"] for q in d["PUNTO"]["b"]]
    w(f"**No se puede escribir.** R(600) = {c(d['EVAL']['b|1|'][600][0],1)}: `#S` lo rechaza y `Asistente.proponer` lo "
      f"bloquea. El motivo de fondo: los tres patrones ocupan sólo **{c(max(xb)-min(xb),0)} cuentas de x** "
      f"({c(min(xb),0)}-{c(max(xb),0)}), unas {c((max(xb)-min(xb))/(sigma_col/100*760),0)} veces la σ de una medida "
      f"en esa zona (±{c(sigma_col/100*760,0)} cuentas). La pendiente queda mal determinada: con la colocación "
      f"simulada, R(1000) = {c(d['MC']['b|1|'][1000][0],0)} ± {c(d['MC']['b|1|'][1000][1],0)}, y la recta re-ajustada "
      f"falla `#S` en {d['MC_S']['b|1|'][0]} de 2000. El residuo casi nulo (RMS "
      f"{c(d['AJUSTE']['b|1|']['rms'],2)}) no significa nada con 3 puntos y 2 parámetros.")
    w("")
    # recta por el oscuro, fuera del metodo de la app
    pb = d["PUNTO"]["b"]
    kb, fb_, _ = anclada(pb)
    w(f"*Opción fuera del método de la app, sólo para que Diego la valore:* recta anclada en el oscuro, "
      f"R = k·(x − {X_OSCURO}), k por mínimos cuadrados = {c(kb,4)} (`c1 = {kb:.8E}`, `c0 = {-kb*X_OSCURO:.8E}`). "
      "Error: " + ", ".join(f"{q['p']} {cs(100*q['e']/q['cert'],0)} %" for q in fb_) +
      f". R(600) = {c(kb*(600-X_OSCURO),1)}: pasaría `#S`. Depende de un oscuro medido una sola vez; **no se propone "
      "escribirla** sin medir el oscuro en serie.")
    w("")

    # ------------------------------------------------------------------ 3. verificar
    w("## 3. Códigos que sólo se verifican (curva de fábrica, sin ajustar)")
    w("")
    w(f"x − oscuro con oscuro = {X_OSCURO}. σ_x = {c(sigma_col,1)} % de x (§1.3). σ_R = pendiente de fábrica × σ_x.")
    w("")
    w("| Código | Patrón | Tipo | Cert. | x | x − oscuro | R fábrica | Firmware | Error % | σ_R |")
    w("| :---: | :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    for k in ("3", "4", "5", "7", "a", "c", "d"):
        for q in d["PUNTO"][k]:
            sx = sigma_col / 100 * q["x"]
            w(f"| {k} | {q['p']} | {q['tipo']} | {c(q['cert'],0)} | {c(q['x'])} | {c(q['x']-X_OSCURO,0)} | {c(q['rfab'])} | "
              f"{q['rfab32']} | {cs(100*(q['rfab']-q['cert'])/q['cert'],0)} | {c(abs(q['dfab'])*sx,1)} |")
    w("")
    w("¿Distingue el equipo un patrón de otro dentro de cada código?")
    w("")
    sx = lambda x: sigma_col / 100 * x  # noqa: E731
    for k, txt in (
        ("3", f"Verde intenso: x 831-849 para 164-173. Separación máxima de 18 cuentas frente a σ_x ≈ {c(sx(840),0)} "
              f"(σ_x·√2 ≈ {c(sx(840)*math.sqrt(2),0)} para una diferencia): **no distingue** 164 de 173, y P14 (173) y "
              "P15 (164) salen al revés. Sí está claramente fuera del oscuro (~260 cuentas). La fábrica lee −14/−18 %."),
        ("4", f"Rojo intenso: P11 (194) 1723 y P12 (227) 1762. Separación de 39 cuentas frente a σ_x·√2 ≈ "
              f"{c(sx(1740)*math.sqrt(2),0)}: **no se distinguen con una sola medida de cada uno**. La fábrica lee +23 % "
              "en P11 y +7 % en P12."),
        ("5", "Azul intenso: x 813-828 para 83-85, un nivel en la práctica: **no distingue** y P18 (84) sale el más bajo. "
              "~240 cuentas sobre el oscuro. La fábrica lee −55/−57 %: **fuera** con cualquier tolerancia."),
        ("7", "Blanco tipo I, P35: 96 → fábrica 96,8 (+1 %). Un solo patrón."),
        ("a", f"Verde tipo I, P40 (6) 625 y P45 (7) 616: 40-50 cuentas sobre el oscuro, ~{c(45/sx(620),0)} σ_x. La "
              "fábrica lee **56-60 donde el certificado dice 6-7**: la curva del verde (idéntica a la del intenso, "
              "`Fabrica.java:69-70`) en esta zona no sirve. Señal insuficiente para calibrar."),
        ("c", "Azul tipo I: x 593-644 para 7-10, **18-70 cuentas sobre un oscuro medido una vez**. P46, P47 y P48, los tres "
              "de 9, dan 644, 606 y 605: 39 cuentas de dispersión entre patrones iguales, más que lo que separa 7 de "
              "10 (P42 593, P36 639). **No distingue un valor de otro**; apenas distingue el patrón del oscuro. La "
              "fábrica lee +32/+79 %, que en unidades son 3-7."),
        ("d", "Naranja tipo I: x 803-907 para 68-73, 230-330 cuentas sobre el oscuro; P50 (71) lee más que P41 (73) y P33 "
              "(72) menos que ambos. **No distingue** 68 de 73: 104 cuentas de dispersión para 5 unidades. La fábrica "
              "lee −23/−44 %."),
    ):
        w(f"- **{k}.** {txt}")
    w("")
    w("Conclusión: con la reproducibilidad medida, verde, azul y los opacos bajos **tienen señal para decir \"hay "
      "lámina\"** (salvo c, al límite), **pero no para separar valores de 1-9 unidades**. Verificar sí; ajustar, no, "
      "y la app ya lo impide por cobertura (`Asistente.java:88-96`).")
    w("")

    # ------------------------------------------------------------------ 4. LOO
    w("## 4. Validación cruzada dejando uno fuera (códigos 1 y 2)")
    w("")
    w("Con `Ajuste.ajustar` sobre los demás patrones; error = cert − predicción. Umbral RF-CAL-17: 1,5 × máx(10 % ; 2) = 15 %.")
    w("")
    for ident, tit in (("1|1|", "Código 1, grado 1"), ("1|2|", "Código 1, grado 2"), ("2|1|", "Código 2, grado 1"),
                       ("2|2|", "Código 2, grado 2")):
        lo = d["LOO"][ident]
        mal = [q["p"] for q in lo if abs(q["err"]) > 1.5 * max(0.10 * q["cert"], 2)]
        rms = math.sqrt(sum((100 * q["err"] / q["cert"]) ** 2 for q in lo) / len(lo))
        w(f"**{tit}:** " + ", ".join(f"{q['p']} {cs(100*q['err']/q['cert'],1)} %" for q in lo) +
          f". RMS de predicción {c(rms,1)} %. " + ("Todos dentro." if not mal else "**Fuera: " + ", ".join(mal) + ".**")
          + (" Predice mejor que la recta, pero no se puede escribir (§2.2)." if ident == "2|2|" else ""))
        w("")
    w("Para 8 y b la SPEC dice que no tiene sentido (`SPEC-Calibracion-V3.6.md:379`). Como dato: en el 8, grado 1, "
      + ", ".join(f"{q['p']} {cs(100*q['err']/q['cert'],0)} %" for q in d["LOO"]["8|1|"]) + ".")
    w("")

    # ------------------------------------------------------------------ 5. veredicto
    w("## 5. Veredicto por código")
    w("")
    w("Con los criterios **propuestos** de la SPEC §5, que Diego no ha aprobado (P-CAL-01).")
    w("")
    w("| Código | Veredicto | Por qué | Patrones anómalos (ninguno quitado) |")
    w("| :---: | :--- | :--- | :--- |")
    w("| 1 | **Ajustar, grado 1, tras P9-A5 y con dispensa expresa de Diego** | Pasa `#S` (R(600) = 17), C2 y el oscuro "
      "(9 frente a 25 de fábrica). Baja el RMS de IV de 39 a 10 % y el de IX de 34 a 5 %. **Incumple tres criterios "
      "propuestos:** RF-CAL-14 (P3, +10,7 %), RF-CAL-15 (RMS de IV 10,0 %) y RF-CAL-16 en XI (2,8 % frente a 2,6 % de "
      "fábrica, diferencia muy por debajo de la reproducibilidad). Con la letra de RF-CAL-16, \"no se escribe\" | "
      "**P2 y P3** (IV) se contradicen entre sí: 36 unidades de certificado y 385 cuentas de x, cuando la recta da "
      "0,3 R/cuenta; P2 (414) lee casi lo que P27 (471). **P28**, −6,9 % |")
    w("| 2 | **Ajustar, grado 1, tras P9-A5; antes, decisión de Diego sobre el oscuro** | Pasa `#S` y C2; mejora a la "
      "fábrica en los tres tipos (sesgo IV +21 → +0,6 %, IX +24 → +1,4 %, XI +13 → 0 %). **Incumple RF-CAL-14 en P22 "
      "(−13 %) y P24 (+10,3 %), RF-CAL-15 en IV (RMS 8,2 %) y RF-CAL-17 en P22.** Lee **84 en el oscuro** frente a 21 "
      "(B13): la regla en curso la bloquearía. Alternativa: la recta anclada (§2.2) | **P22**, **P24** (identidad sin "
      "confirmar), **P23** (−8 %) y **P5** (+7,6 %): los XI amarillos no se ordenan por certificado |")
    w("| 8 | **Ajustar, grado 1, tras P9-A5** | Cumple RF-CAL-14/15/16 (tipo I, RMS 5,2 % frente a 18,6 % de fábrica). "
      "Pasa `#S` por 0,3 unidades: una re-medida puede dar una recta que el firmware rechace | P37 (lee más que P34 "
      "con menos certificado; dentro de la reproducibilidad) |")
    w("| b | **Dejar fábrica y marcar \"fuera\"** | La recta no pasa `#S` y su pendiente está mal determinada (63 cuentas "
      "de rango). La fábrica lee −67/−74 %. Hace falta un rojo tipo I de R más alto, o decidir la recta anclada en el "
      "oscuro tras medir el oscuro | — |")
    w("| 3, 4, 5 | Sólo verificar | Rango o niveles insuficientes (`Asistente.cobertura`). Fábrica: verde −14/−18 %, rojo "
      "+7/+23 %, **azul −55/−57 %: fuera** | P14/P15 invertidos; P18 |")
    w("| 7 | Sólo verificar | Fábrica +1 % | — |")
    w("| a | Sólo verificar; **fuera** | Fábrica lee 56-60 para 6-7 | — |")
    w("| c | Sólo verificar | Señal apenas por encima del oscuro; fábrica +32/+79 % (3-7 unidades) | P46 (644 frente a 605 de sus iguales) |")
    w("| d | Sólo verificar; **fuera** | Fábrica −23/−44 % | P32b (identidad de P32 pendiente, `ROADMAP.md:40-41`) |")
    w("| 6 | Sin patrones | Queda la fábrica sin verificar | — |")
    w("")

    # ------------------------------------------------------------------ 6. método
    w("## 6. Recomendación de método para la medida final")
    w("")
    sm = st.median(sdins)
    w(f"**K = 5 colocaciones por patrón (levantar y volver a apoyar), repartidas en 2 sesiones (3 + 2, con apagado y "
      f"calentamiento entre ellas), M = 4 disparos por colocación tras el de asentamiento.** Para los códigos 1, 2 y 8: "
      f"26 patrones × 5 = 130 colocaciones × 5 disparos = 650 disparos. La campaña del 19-sep hizo 55 series de 10 "
      f"disparos (550) en 36 min (09:57-10:33): del orden de **1 h en total**, más el manejo entre colocaciones.")
    w("")
    w(f"- **Por qué K y no M:** la s de disparo es ~{c(sm,2)} % y la variación entre medidas ~{c(sigma_col,1)} %. "
      f"Con M = 4 en vez de 9, el error de disparo de la media de una colocación pasa de {c(sm/3,2)} a {c(sm/2,2)} %, "
      f"invisible al lado de la colocación; cada colocación nueva, en cambio, divide la parte grande. σ de la media: "
      f"K = 3 → {c(sigma_col/math.sqrt(3),2)} %; **K = 5 → {c(sigma_col/math.sqrt(5),2)} %**; K = 9 → "
      f"{c(sigma_col/3,2)} %. K = 5 deja la incertidumbre de cada punto por debajo del 1 %, por debajo del RMS de XI "
      "que el ajuste quiere juzgar (2,8-4,8 %); K = 9 casi dobla el trabajo para ganar 0,2 puntos. Con K = 5 la "
      "misma cuenta de la app (s entre colocaciones, `Veredicto.sEntre`) tiene 4 grados de libertad, el mínimo "
      "razonable para que su umbral signifique algo.")
    w("- **Por qué en dos sesiones:** lo medido es variación entre sesiones; si todas las colocaciones son de la misma, la "
      "media hereda el error de esa sesión entero y la s entre colocaciones lo esconde (§1.1: 0,1-0,9 % dentro, 2-5 % "
      "entre). Si P9-A5 demuestra que la colocación sola ya da el 2-4 %, basta una sesión con K = 5.")
    w("- **M = 4 y no 3:** el rechazo de descolgados sólo actúa desde 4 disparos (RF-CAL-04, "
      "`SPEC-Calibracion-V3.6.md`, §3). Con el 3 × 3 por defecto de la 3.6.7 un disparo como el 3031 de P7 entraría en "
      "la media. Y el mismo M en campaña, verificación y campo, por la rampa de §1.2.")
    w("- Encaja con la app 3.6.7 (K × M configurables, 3 × 3 por defecto, `CampanaActivity.java:66-76`) cambiando a 5 × 4. "
      "**La 3.6.7 no está revisada** (nota de f7b75c4 en `REVISION-Arquitectura-P9-V3.6.md`): hasta que lo esté, esto es "
      "procedimiento, no una propiedad de la app.")
    w("- **Medir el oscuro en serie** (K = 5, tapa opaca) en cada sesión: lo necesita la decisión de B13 y la del código b.")
    w("- **Leer en voz alta la etiqueta** de P24, P32a/P32b y de los XI amarillos (L-18).")
    w("- El criterio de aceptación de cada patrón, en lugar de RF-CAL-13 actual: s entre colocaciones ≤ 3 % (el "
      "`REPRO_MAX` de la app) **y** diferencia entre las medias de las dos sesiones ≤ 2·√2·σ_col con la σ que dé P9-A5.")
    w("")

    # ------------------------------------------------------------------ 7. contraste
    w("## 7. Hallazgos del principal: confirmados y desmentidos")
    w("")
    w("| Afirmación | Resultado | Evidencia |")
    w("| :--- | :--- | :--- |")
    s23, s24 = st.mean(camp["S023"]["x"]), st.mean(camp["S024"]["x"])
    w(f"| P5 a 0° 2456,8 y a 90° 2459,3 (0,1 %); orientación refutada para P5 | **Confirmado**, con matiz: la diferencia "
      f"({c(s24-s23,1)} cuentas) es 0,8 veces su error típico. Refuta 0/90° en P5, no 180/270° ni los demás XI | S023, S024 |")
    w(f"| Reproducibilidad entre sesiones de −3,6 a +2,8 % | **Desmentido en el extremo:** llega a **{cs(min(dps),1)} %** "
      f"({min(resumen, key=lambda f: f['dp'])['p']}, serie de 09:25) y a −4,4 % en P24 frente a su serie de 2065-2074 | §1 |")
    w("| Ejemplos P1 3071→2961, P7 3234→3122, P26 2149→2209, P29 1725→1677 | **Confirmados** (P7 3234 es la media de sus "
      "dos series de la mañana, 3230,9 y 3237,0) | §1 |")
    w("| Mucho mayor que la s de serie (0,2-0,3 %) | **Confirmado**, ~9 veces | §1 |")
    w("| Deriva −4 en la posición 1 y +3,3 en la 9, **monótona** | Cifras **confirmadas**; **monótona, no** (la 4 < la 3; "
      "la 7 y la 8 < la 6) | `Campana.desvioPorPosicion` |")
    w("| P24 = 1982,7; candidatas 2065, 2443 y 2804 | Compatible **2065** (−4,0 %). **2804 no es una medida de P24**: es el caso "
      "sintético de `Veredicto.java:26` y `CampanaTest.java:74` | §1.4 |")
    w("| XI amarillos sin ordenar; blancos XI ordenados | **Confirmado** los dos. En amarillo, P23 (714) lee 2735, un 11 % "
      "más que P5 (740): fuera de la reproducibilidad, así que no es sólo colocación | §2.2 |")
    w("| Reproducibilidad entre colocaciones 2-4 % (arquitecto, r2) | Lo medido es **entre sesiones**. Dentro de la sesión "
      "hay un solo par seguro (P5, 0,1 %) | §1.1 |")
    w("| Código b ajustable en grado 1 (`ROADMAP.md:49-50`, `SPEC-Calibracion-V3.6.md` §2.2) | **Desmentido en la práctica:** "
      "la cobertura lo permite, pero la recta que sale da R(600) = −28 y la app no la deja escribir | §2.4 |")
    w("")

    # ------------------------------------------------------------------ anexo
    w("## Anexo. Informe del asistente de la app, tal cual (grado 1 de los códigos 1, 2 y 8)")
    w("")
    for ident in ("1|1|", "2|1|", "8|1|"):
        w("```")
        for l in d["INFORME"][ident]:
            w(l)
        w("```")
        w("")

    open(SALIDA, "w", encoding="utf-8", newline="\n").write("\n".join(L))
    print("escrito", SALIDA, len(L), "lineas; sigma_rel", sigma_rel)


if __name__ == "__main__":
    main()
