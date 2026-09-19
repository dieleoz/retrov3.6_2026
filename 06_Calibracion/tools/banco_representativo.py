# -*- coding: utf-8 -*-
"""
Banco representativo de patrones de SLV-002 (PLAN-Banco-Representativo.md).

Hace, desde los ficheros versionados y sin tocar la app:

1. Grupos de patrones equivalentes por codigo del equipo, tipo de lamina y valor certificado.
   Dos patrones son equivalentes si su R certificado difiere menos que la resolucion util del
   equipo en esa zona:
       dR(x) = max(1 ; 2 * sqrt(2/K) * s_rep * b(x) * x)
   con s_rep = 2,2 % por colocacion (A5 del 19-sep, resumen.txt:191), K = 3 colocaciones (la
   media de un patron en el banco), b(x) = R / (x - x_osc) la pendiente local de la recta por el
   oscuro (x_osc = 565,4, resumen.txt:122) y 1 unidad la resolucion de salida del firmware.
   Es el cambio de R que distingue dos medias de K = 3 colocaciones a 2 sigmas.
2. El banco representativo (cola_banco_representativo.csv) y el de verificacion anual
   (cola_verificacion_anual.csv), con el MISMO formato que cola_banco_P1-P132.csv.
3. El tiempo estimado con el modelo de PLAN-Captura-Banco-P1-P132.md §2.
4. La cobertura por codigo con la regla de Asistente.coberturaValores (Asistente.java:75-98),
   reproducida aqui y, si hay JDK, ejecutada con el codigo de la app (tools/CoberturaCola.java).
5. Lo que se pierde en incertidumbre del ajuste al quitar patrones.

Uso:  python banco_representativo.py            (escribe los CSV y el informe en stdout)
Variables: RETRO_JDK = carpeta bin de un JDK 11 (por defecto la de la maquina de Diego).
"""
import csv
import hashlib
import io
import math
import os
import subprocess
import sys
import tempfile
import zipfile
from collections import OrderedDict, defaultdict

AQUI = os.path.dirname(os.path.abspath(__file__))
CAL = os.path.dirname(AQUI)
RAIZ = os.path.dirname(CAL)
COLA_COMPLETA = os.path.join(CAL, "cola_banco_P1-P132.csv")
CATALOGO = os.path.join(CAL, "patrones_certificados_P1-P132.csv")
SALIDA_REP = os.path.join(CAL, "cola_banco_representativo.csv")
SALIDA_ANUAL = os.path.join(CAL, "cola_verificacion_anual.csv")
SALIDA_GRUPOS = os.path.join(CAL, "grupos_patrones_equivalentes.csv")
JDK = os.environ.get("RETRO_JDK", r"D:\@Proyect\Baliza\7 sw apk\jdk-11\jdk-11.0.24+8\bin")

S_REP = 0.022          # por colocacion (REFORM §2.1: 2,24 %, se usa 2,2 %)
X_OSC = 565.4          # resumen.txt:122 del ZIP de las 12:27
K_BANCO = 3
Z = 2.0

# Tiempo (PLAN-Captura-Banco-P1-P132.md §2): 1,5 s por disparo, 11 s por recolocacion,
# 20 s por cambio de patron. Bateria 10 s y exportar 20 s: con eso el modelo da los 195 min del plan.
T_DISPARO, T_RECOLOCAR, T_CAMBIO, T_BATERIA, T_EXPORTAR, T_CALENT = 1.5, 11, 20, 10, 20, 600

# Tabla RF-CAL-37 del APK (TablaCalibracion.java:134-165) y decisiones de Diego (DECISIONES-Diego-2026-09-19.md)
REMEDIDA = OrderedDict([("1", "P28"), ("2", "P25"), ("3", "P123"), ("4", "P11"), ("5", "P81"),
                        ("6", "P86"), ("8", "P43"), ("b", "P49")])
METODO = {"1": "grado 1 (escrito, no se reescribe)", "2": "recta anclada (escrita, no se reescribe)",
          "3": "grado 1 o anclada (PA-16)", "4": "grado 1 o anclada (PA-16)", "5": "recta anclada (PA-14)",
          "6": "grado 1 o anclada (PA-16)", "8": "recta anclada", "b": "recta anclada (PA-24)"}
ANCLADO = {"2", "5", "8", "b"}
LIBRE_O_ANCLADO = {"1", "3", "4", "6"}
AJUSTABLES = list(REMEDIDA.keys())
# Patrones con una contradiccion abierta: no se eligen como representante si hay otro en su grupo.
DUDOSOS = {"P2": "P2/P3 se contradicen (PLAN §5.4)", "P3": "P2/P3 se contradicen (PLAN §5.4)",
           "P5": "C-39", "P23": "C-39", "P24": "C-40", "P22": "FUERA en la A5 (-5,55 %)",
           "P32a": "C-41", "P32b": "C-41",
           "P65": "PA-16 (51 exacto)", "P79": "PA-16 (51 exacto)", "P80": "PA-16 (51 exacto)",
           "P124": "PA-16 (51 exacto)", "P125": "PA-16 (51 exacto)", "P94": "PA-16 (51 exacto)",
           "P95": "PA-16 (51 exacto)", "P109": "PA-16 (51 exacto)", "P110": "PA-16 (51 exacto)"}
# Codigos de 3-4 puntos: no se reducen (cada punto pesa un 25-33 %, PLAN §4) y la dispensa PA-24
# nombra a P39 y P49.
SIN_REDUCIR = {"8", "b"}
# RMS relativo de los residuos por lamina (dispersion entre laminas), REFORM §0 y §3:
RMS_LAMINA = {"1": 0.053, "2": 0.059, "8": 0.057, "b": 0.108}
RMS_SUPUESTO = 0.055   # 3, 4, 5, 6: sin medir; media de 1, 2 y 8
VERIF_SOLO = OrderedDict([("7", ["P35"]), ("a", ["P40"]), ("c", ["P36"]), ("d", ["P41"]),
                          ("cafe", ["P68", "P98"]), ("lila", ["P69", "P114"])])
ORDEN_TIPO = {"IV": 0, "IX": 1, "XI": 2, "I": 3}
# Contradiccion abierta sobre la identidad o la lectura: no se usan como punto de ajuste ni aunque
# sean el unico patron de su grupo (el extremo pasa al grupo siguiente). Los verdes de 51 (PA-16)
# solo se evitan dentro de su grupo: son el extremo bajo del verde y no tienen sustituto.
CONTRADICCION = {"P2", "P3", "P5", "P23", "P24", "P22", "P32a", "P32b"}
# Sesiones: dos de ~50 min en lugar de una de ~95 (PLAN §0 regla 7: sesiones de unos 60 min).
SESION_2 = {"rojo", "cafe", "lila", "naranja", "verde", "azul"}


def md5_lf(path):
    with open(path, "rb") as f:
        return hashlib.md5(f.read().replace(b"\r\n", b"\n")).hexdigest()


def leer_cola(path):
    with open(path, encoding="utf-8", newline="") as f:
        r = csv.DictReader(f)
        return r.fieldnames, list(r)


def segundos(row):
    p = row["paso"]
    if p in ("OSCURO", "A5", "PATRON"):
        k, m, a = int(row["K"]), int(row["M"]), int(row["asentamiento"])
        return k * (m + a) * T_DISPARO + (k - 1) * T_RECOLOCAR + T_CAMBIO
    return {"CALENTAMIENTO": T_CALENT, "BATERIA": T_BATERIA, "EXPORTAR": T_EXPORTAR}.get(p, 0)


def tiempos(rows):
    s = OrderedDict()
    for r in rows:
        s[r["sesion"]] = s.get(r["sesion"], 0) + segundos(r)
    return s


def resolucion(r, x):
    """dR util en la zona del patron: 2*sqrt(2/K)*s_rep*b*x, b = R/(x - x_osc); minimo 1 unidad."""
    if x is None or x <= X_OSC + 1:
        return float("nan")
    b = r / (x - X_OSC)
    return max(1.0, Z * math.sqrt(2.0 / K_BANCO) * S_REP * b * x)


# --------------------------------------------------------------------------------- grupos
def patrones_de(rows):
    pats = OrderedDict()
    for r in rows:
        if r["paso"] != "PATRON":
            continue
        x = float(r["x_esperada"]) if r["x_esperada"] else None
        pats[r["patron"]] = dict(patron=r["patron"], color=r["color"], tipo=r["tipo"],
                                 R=float(r["valor_certificado"]), codigo=r["codigo_equipo"], uso=r["uso"],
                                 x=x, origen=r["origen_x"], fila=r)
    return pats


def clave_grupo(p):
    # cafe y lila se miden con el 4 pero son otro color: grupo propio
    return (p["codigo"], p["color"], p["tipo"])


def agrupar(pats):
    por = defaultdict(list)
    for p in pats.values():
        por[clave_grupo(p)].append(p)
    grupos = []
    for key in sorted(por, key=lambda k: (k[0], k[1], ORDEN_TIPO.get(k[2], 9))):
        lst = sorted(por[key], key=lambda p: (p["R"], p["patron"]))
        actual = []
        for p in lst:
            if actual:
                base = actual[0]
                d = resolucion(base["R"], base["x"])
                if not (p["R"] - base["R"] <= d) or math.isnan(d):
                    grupos.append(actual)
                    actual = []
            actual.append(p)
        if actual:
            grupos.append(actual)
    for i, g in enumerate(grupos, 1):
        for p in g:
            p["grupo"] = "G%02d" % i
    return grupos


def representante(g, remedida):
    for p in g:
        if p["patron"] == remedida:
            return p
    med = sorted(p["R"] for p in g)[len(g) // 2]

    def pref(p):
        return (p["patron"] in CONTRADICCION, p["patron"] in DUDOSOS, p["origen"] != "medida", abs(p["R"] - med), p["R"])
    return sorted(g, key=pref)[0]


# --------------------------------------------------------------------------------- seleccion
def seleccionar(pats, grupos):
    """Devuelve {patron: (uso, K, motivo)} del banco representativo."""
    sel = OrderedDict()
    for k in AJUSTABLES:
        rem = REMEDIDA[k]
        gs = [g for g in grupos if g[0]["codigo"] == k and g[0]["color"] not in ("cafe", "lila")]
        todos = [p for g in gs for p in g]
        if k in SIN_REDUCIR:
            for p in sorted(todos, key=lambda p: p["R"]):
                uso = "RE-MEDIDA" if p["patron"] == rem else "AJUSTE"
                sel[p["patron"]] = (uso, 5, "codigo de 3-4 puntos: no se reduce")
            continue
        elegidos = OrderedDict()
        por_tipo = defaultdict(list)
        for g in gs:
            por_tipo[g[0]["tipo"]].append(g)
        for t, lg in por_tipo.items():
            lg = sorted(lg, key=lambda g: g[0]["R"])
            limpios = [g for g in lg if any(p["patron"] not in CONTRADICCION for p in g)]
            lg = limpios or lg
            extremos = [lg[0]] if len(lg) == 1 else [lg[0], lg[-1]]
            for g in extremos:
                if k == "5":
                    # P81 fuera del ajuste del 5 (decision de Diego): el representante del grupo es otro
                    p = representante([q for q in g if q["patron"] != rem] or g, None)
                else:
                    p = representante(g, rem)
                elegidos[p["patron"]] = "extremo %s del tipo %s (%s)" % (
                    "bajo" if g is lg[0] else "alto", t, p["grupo"])
            for g in lg:
                if k != "5" and any(p["patron"] == rem for p in g) and rem not in elegidos:
                    elegidos[rem] = "patron de re-medida (RF-CAL-37)"
        # verificacion independiente: fuera del ajuste, lo mas cerca del centro del rango, de un
        # grupo sin representante si lo hay, medida antes que estimada, sin contradiccion abierta.
        rs = [p["R"] for p in todos]
        centro = (min(rs) + max(rs)) / 2
        grupos_con_rep = {pats[n]["grupo"] for n in elegidos}
        cands = [p for p in todos if p["patron"] not in elegidos and p["patron"] != rem]
        ver = None
        if cands:
            ver = sorted(cands, key=lambda p: (p["patron"] in DUDOSOS, p["grupo"] in grupos_con_rep,
                                               p["origen"] != "medida", abs(p["R"] - centro)))[0]
        for n, motivo in elegidos.items():
            if n == rem:
                if k == "5":
                    # Decision P81 (DECISIONES-Diego-2026-09-19.md): fuera del ajuste del 5
                    sel[n] = ("VERIFICACION", 5, "re-medida del 5; fuera del ajuste por decision de Diego (P81)")
                else:
                    sel[n] = ("RE-MEDIDA", 5, motivo)
            else:
                sel[n] = ("AJUSTE", K_BANCO, motivo)
        if rem not in sel:
            sel[rem] = ("VERIFICACION", 5, "re-medida del 5; fuera del ajuste por decision de Diego (P81)")                 if k == "5" else ("RE-MEDIDA", 5, "patron de re-medida (RF-CAL-37)")
        if ver is not None:
            sel[ver["patron"]] = ("VERIFICACION", K_BANCO, "verificacion independiente (%s, centro del rango)"
                                  % ver["grupo"])
    for k, lst in VERIF_SOLO.items():
        for n in lst:
            sel[n] = ("VERIFICACION", K_BANCO, "solo verificar (%s)" % k)
    return sel


def construir_cola(fieldnames, rows, sel, nota_extra, dos_sesiones):
    """Misma estructura y orden por color que la cola completa; OSCURO y A5 al inicio y al final de cada sesion."""
    pats = [r for r in rows if r["paso"] == "PATRON" and r["patron"] in sel]
    bloques = OrderedDict()
    for r in pats:
        bloques.setdefault(r["bloque"], []).append(r)
    out = []

    def add(base, ses, **cambios):
        d = dict(base)
        d.update(cambios)
        d["sesion"] = str(ses)
        out.append(d)

    def control(ses_orig, bloque, pasos=None):
        return [r for r in rows if r["sesion"] == ses_orig and r["bloque"] == bloque
                and (pasos is None or r["paso"] in pasos)]

    bat = [r for r in rows if r["paso"] == "BATERIA" and r["bloque"].startswith("COLOR:")][0]
    sesiones = [[b for b in bloques if not dos_sesiones or bloques[b][0]["color"] not in SESION_2]]
    if dos_sesiones:
        sesiones.append([b for b in bloques if bloques[b][0]["color"] in SESION_2])
    for i, bs in enumerate(sesiones, 1):
        orig = "1" if i == 1 else "2"
        for r in control(orig, "INICIO") + control(orig, "A5-INICIO"):
            add(r, i)
        for b in bs:
            for r in bloques[b]:
                uso, k, motivo = sel[r["patron"]]
                nota = r["nota"]
                extra = nota_extra(r["patron"], uso, motivo)
                nota = (nota + "; " if nota and extra else nota) + extra
                add(r, i, uso=uso, K=str(k), nota=nota,
                    remedida_de_codigo=r["codigo_equipo"] if REMEDIDA.get(r["codigo_equipo"]) == r["patron"]
                    and r["color"] not in ("cafe", "lila") else "")
            add(bat, i, bloque=b)
        ultima = i == len(sesiones)
        for r in control(orig, "A5-FIN") + control(orig, "FIN", ("OSCURO", "EXPORTAR") if ultima else None):
            add(r, i)
    for i, d in enumerate(out, 1):
        d["orden"] = str(i)
    return out


def escribir(path, fieldnames, rows):
    with open(path, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, lineterminator="\n")
        w.writeheader()
        for r in rows:
            w.writerow({k: r.get(k, "") for k in fieldnames})


# --------------------------------------------------------------------------------- cobertura
def cobertura_valores(valores):
    """Asistente.coberturaValores (Asistente.java:75-98), reproducida."""
    n = len(valores)
    if n == 0:
        return 0, 0, "sin patrones"
    niv = len(set(valores))
    mn, mx = min(valores), max(valores)
    if niv < 3:
        return niv, 0, "%d niveles: hacen falta 3" % niv
    if (mx - mn) < 20 or (mx - mn) / mx < 0.30:
        return niv, 0, "rango estrecho %.0f-%.0f" % (mn, mx)
    return niv, min(2, niv - 2), "R %.0f-%.0f" % (mn, mx)


def valores_ajuste(cola, k):
    """
    FlujoCalibracion.patronesAjuste (FlujoCalibracion.java:288-303): AJUSTE y RE-MEDIDA de la cola; si el
    codigo no tiene ningun AJUSTE, todos sus patrones. Asistente.deCodigo deja fuera cafe y lila (color).
    """
    fil = [r for r in cola if r["paso"] == "PATRON" and r["codigo_equipo"] == k and r["color"] not in ("cafe", "lila")]
    hay = any(r["uso"] == "AJUSTE" for r in fil)
    return [float(r["valor_certificado"]) for r in fil if not hay or r["uso"] in ("AJUSTE", "RE-MEDIDA")]


def java_cobertura(colas):
    javac = os.path.join(JDK, "javac")
    if not (os.path.exists(javac) or os.path.exists(javac + ".exe")):
        return None, "sin JDK en %s" % JDK
    tmp = tempfile.mkdtemp(prefix="cobcola_")
    r = subprocess.run(["git", "-C", RAIZ, "archive", "--format=zip", "HEAD",
                        "03_App_Movil/RetroV36/app/src/main/java"], capture_output=True, check=True)
    zipfile.ZipFile(io.BytesIO(r.stdout)).extractall(tmp)
    src = os.path.join(tmp, "03_App_Movil", "RetroV36", "app", "src", "main", "java")
    cls = os.path.join(tmp, "cls")
    os.makedirs(cls)
    c = subprocess.run([javac, "-encoding", "UTF-8", "-d", cls, "-sourcepath", src,
                        os.path.join(AQUI, "CoberturaCola.java")], capture_output=True)
    if c.returncode != 0:
        return None, "javac fallo: " + c.stderr.decode("utf-8", "replace")[-800:]
    head = subprocess.run(["git", "-C", RAIZ, "rev-parse", "--short", "HEAD"], capture_output=True,
                          text=True).stdout.strip()
    res = {}
    for nombre, path in colas:
        o = subprocess.run([os.path.join(JDK, "java"), "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-cp", cls, "CoberturaCola", path],
                           capture_output=True)
        res[nombre] = o.stdout.decode("utf-8", "replace") + o.stderr.decode("utf-8", "replace")
    return res, "codigo de la app en HEAD %s (git archive), JDK %s" % (head, JDK)


# --------------------------------------------------------------------------------- incertidumbre
def sigma_curva(puntos, anclada, xs_eval):
    """
    sigma de la curva ajustada, relativa a R, en cada x de xs_eval. puntos = [(x, R, sigma_R)].
    Libre: minimos cuadrados ponderados de grado 1. Anclada: R = b (x - x_osc), sin error del ancla.
    """
    if anclada:
        s = sum(((x - X_OSC) / sr) ** 2 for x, r, sr in puntos)
        b = sum((x - X_OSC) * r / sr ** 2 for x, r, sr in puntos) / s
        sb = 1 / math.sqrt(s)
        return [sb / b for x in xs_eval]   # relativa: (x - x0) sb / (b (x - x0))
    sw = sum(1 / sr ** 2 for x, r, sr in puntos)
    sx = sum(x / sr ** 2 for x, r, sr in puntos)
    sxx = sum(x * x / sr ** 2 for x, r, sr in puntos)
    sy = sum(r / sr ** 2 for x, r, sr in puntos)
    sxy = sum(x * r / sr ** 2 for x, r, sr in puntos)
    det = sw * sxx - sx * sx
    b = (sw * sxy - sx * sy) / det
    a = (sy - b * sx) / sw
    out = []
    for x in xs_eval:
        var = (sxx - 2 * x * sx + x * x * sw) / det
        out.append(math.sqrt(var) / (a + b * x))
    return out


def perdida(cola_full, cola_rep, k):
    def pts(cola, modo):
        l = []
        for r in cola:
            if r["paso"] == "PATRON" and r["codigo_equipo"] == k and r["uso"] in ("AJUSTE", "RE-MEDIDA") \
                    and r["color"] not in ("cafe", "lila") and not (k == "5" and r["patron"] == "P81"):
                x, R, K = float(r["x_esperada"]), float(r["valor_certificado"]), int(r["K"])
                b = R / (x - X_OSC)
                if modo == "col":
                    sr = b * S_REP * x / math.sqrt(K)
                else:
                    sr = math.hypot(b * S_REP * x / math.sqrt(K), RMS_LAMINA.get(k, RMS_SUPUESTO) * R)
                l.append((x, R, sr))
        return l
    anclada = k in ANCLADO
    full_c, rep_c = pts(cola_full, "col"), pts(cola_rep, "col")
    full_l, rep_l = pts(cola_full, "lam"), pts(cola_rep, "lam")
    if k == "5":
        # la cola completa lleva el 5 como VERIFICACION: con PA-14 entran todos sus azules salvo P81
        def pts5(modo):
            l = []
            for r in cola_full:
                if r["paso"] == "PATRON" and r["codigo_equipo"] == "5" and r["patron"] != "P81":
                    x, R, K = float(r["x_esperada"]), float(r["valor_certificado"]), int(r["K"])
                    b = R / (x - X_OSC)
                    sc = b * S_REP * x / math.sqrt(K)
                    l.append((x, R, sc if modo == "col" else math.hypot(sc, RMS_SUPUESTO * R)))
            return l
        full_c, full_l = pts5("col"), pts5("lam")
    xs = [p[0] for p in full_c]
    xe = [min(xs), (min(xs) + max(xs)) / 2, max(xs)]
    return dict(n_full=len(full_c), n_rep=len(rep_c),
                col_full=sigma_curva(full_c, anclada, xe), col_rep=sigma_curva(rep_c, anclada, xe),
                lam_full=sigma_curva(full_l, anclada, xe), lam_rep=sigma_curva(rep_l, anclada, xe), xe=xe,
                anclada=anclada)


# --------------------------------------------------------------------------------- main
def main():
    fn, rows = leer_cola(COLA_COMPLETA)
    print("Cola completa: %s, md5 (LF) %s, %d filas" % (os.path.relpath(COLA_COMPLETA, RAIZ), md5_lf(COLA_COMPLETA),
                                                     len(rows)))
    t = tiempos(rows)
    print("Tiempo, cola completa: " + ", ".join("sesion %s %.1f min" % (s, v / 60) for s, v in t.items())
          + "; total %.1f min" % (sum(t.values()) / 60))

    pats = patrones_de(rows)
    grupos = agrupar(pats)
    sel = seleccionar(pats, grupos)

    # ---- grupos
    with open(SALIDA_GRUPOS, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["grupo", "codigo_equipo", "color", "tipo", "patron", "valor_certificado", "x_esperada",
                    "origen_x", "resolucion_R", "representante", "uso_representativo", "motivo"])
        for g in grupos:
            for p in g:
                s = sel.get(p["patron"])
                w.writerow([p["grupo"], p["codigo"], p["color"], p["tipo"], p["patron"], "%g" % p["R"],
                            "%g" % p["x"] if p["x"] else "", p["origen"],
                            "%.1f" % resolucion(p["R"], p["x"]), "si" if s else "",
                            s[0] if s else "", s[2] if s else ""])
    print("\n## Grupos (%d grupos de %d patrones)" % (len(grupos), len(pats)))
    print("| Grupo | Cód. | Color | Tipo | Patrones (R cert.) | dR util | Representante(s) |")
    print("| :--- | :---: | :--- | :---: | :--- | ---: | :--- |")
    for g in grupos:
        d = resolucion(g[0]["R"], g[0]["x"])
        reps = [p["patron"] + " (" + sel[p["patron"]][0] + ")" for p in g if p["patron"] in sel]
        print("| %s | %s | %s | %s | %s | %s | %s |" % (
            g[0]["grupo"], g[0]["codigo"], g[0]["color"], g[0]["tipo"],
            ", ".join("%s (%g)" % (p["patron"], p["R"]) for p in g),
            "%.1f" % d if not math.isnan(d) else "-", ", ".join(reps) or "—"))

    # ---- cola representativa
    def nota_rep(n, uso, motivo):
        return "banco representativo: " + motivo
    rep = construir_cola(fn, rows, sel, nota_rep, True)
    escribir(SALIDA_REP, fn, rep)

    # ---- cola anual: 1 patron por codigo (el de re-medida) y los 4 de tipo I solo verificables
    anual_sel = OrderedDict()
    for k, n in REMEDIDA.items():
        anual_sel[n] = ("VERIFICACION", K_BANCO, "verificacion anual del codigo %s (su patron de re-medida)" % k)
    for k in ("7", "a", "c", "d"):
        anual_sel[VERIF_SOLO[k][0]] = ("VERIFICACION", K_BANCO, "verificacion anual del codigo %s" % k)
    anual_sel["P81"] = ("VERIFICACION", 5, anual_sel["P81"][2] + "; K = 5 como fuerza BancoCola (P10-C3)")
    anual = construir_cola(fn, rows, anual_sel, lambda n, u, m: "verificacion anual: " + m, False)
    for r in anual:
        r["remedida_de_codigo"] = ""
    escribir(SALIDA_ANUAL, fn, anual)

    for nombre, cola in (("representativo", rep), ("verificacion anual", anual)):
        t = tiempos(cola)
        npat = sum(1 for r in cola if r["paso"] == "PATRON")
        print("\nCola %s: %d filas, %d patrones, md5 (LF) %s, %.1f min (%s)" % (
            nombre, len(cola), npat,
            md5_lf(SALIDA_REP if nombre == "representativo" else SALIDA_ANUAL), sum(t.values()) / 60,
            ", ".join("sesion %s %.1f" % (s, v / 60) for s, v in t.items())))
        porcod = defaultdict(list)
        for r in cola:
            if r["paso"] == "PATRON":
                porcod[r["codigo_equipo"] + ("/" + r["color"] if r["color"] in ("cafe", "lila") else "")].append(
                    "%s %s %s %s K%s" % (r["patron"], r["tipo"], r["valor_certificado"], r["uso"][:5], r["K"]))
        for k, l in porcod.items():
            print("  %-7s %s" % (k, "; ".join(l)))

    # ---- cobertura
    print("\n## Cobertura (regla de Asistente.coberturaValores, reproducida)")
    print("| Cód. | Método | Completa: niveles / grado máx. | Representativa: niveles / grado máx. | Con ancla (0): representativa |")
    print("| :---: | :--- | :--- | :--- | :--- |")
    for k in AJUSTABLES:
        vf, vr = valores_ajuste(rows, k), valores_ajuste(rep, k)
        cf, cr, ca = cobertura_valores(vf), cobertura_valores(vr), cobertura_valores(vr + [0.0])
        print("| %s | %s | %d / %d (%s) | %d / %d (%s) | %d / %d (%s) |" % (
            k, METODO[k], cf[0], cf[1], cf[2], cr[0], cr[1], cr[2], ca[0], ca[1], ca[2]))

    res, origen = java_cobertura([("completa", COLA_COMPLETA), ("representativa", SALIDA_REP),
                                  ("anual", SALIDA_ANUAL)])
    print("\n## Cobertura con el codigo de la app (%s)" % origen)
    if res:
        for n, txt in res.items():
            print("### " + n)
            print(txt.rstrip())

    # ---- perdida de incertidumbre
    print("\n## Incertidumbre del ajuste: sigma de la curva / R, en x min, centro y max del rango (completa -> representativa)")
    print("| Cód. | Método | n completa -> repr. | Solo colocación (s_rep 2,2 %) | Colocación + dispersión entre láminas |")
    print("| :---: | :--- | :---: | :--- | :--- |")
    for k in AJUSTABLES:
        p = perdida(rows, rep, k)
        f = lambda a: " / ".join("%.2f" % (100 * v) for v in a)
        rms = RMS_LAMINA.get(k)
        print("| %s | %s | %d -> %d | %s -> %s %% | %s -> %s %% (%s) |" % (
            k, "anclada" if p["anclada"] else "grado 1", p["n_full"], p["n_rep"], f(p["col_full"]), f(p["col_rep"]),
            f(p["lam_full"]), f(p["lam_rep"]),
            "RMS %.1f %% medido" % (100 * rms) if rms else "RMS %.1f %% supuesto" % (100 * RMS_SUPUESTO)))


if __name__ == "__main__":
    main()
