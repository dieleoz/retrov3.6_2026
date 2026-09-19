# -*- coding: utf-8 -*-
"""
validar_stone.py - Cruza el proyecto de pantalla STONE contra el firmware del
Retrorreflectometro Vertical y dice que falta en cada lado.

Metodo adaptado del banco de packs de Camion _G
(01_Firmware/simulacion_pc/banco/packs/contrato_01_widgets.py y fuente.py):
  - la extraccion es EXPLICITA (regex por construccion conocida), no un grep ingenuo;
  - cada inventario tiene un SUELO: si sale sospechosamente corto, se ABORTA en vez
    de dar un cruce vacio en verde;
  - un CONTROL NEGATIVO borra un codigo en una copia del inventario y exige que el
    cruce lo detecte;
  - tres estados: 0 PASS, 1 FALLA, 2 ABORTADO. ABORTADO no dice nada del equipo.

Uso:
  python validar_stone.py v3 --vt <proyecto.vt> --fw <carpeta .X> [--imagenes <IMAGE>]
  python validar_stone.py v4 --fw <carpeta .X> [--st <proyecto.st> | --manifest <manifest.json>]

Solo lee. No escribe nada en disco.
"""
import argparse
import io
import json
import os
import re
import sys
import zipfile

PASS, FALLA, ABORTADO = 0, 1, 2


class Abortado(Exception):
    pass


# ---------------------------------------------------------------------------
# Utilidades de lectura del fuente C
# ---------------------------------------------------------------------------

def fuentes_c(carpeta):
    """Los .c y .h de la carpeta del proyecto MPLAB, sin build/, dist/, nbproject/."""
    salida = []
    for raiz, dirs, archivos in os.walk(carpeta):
        dirs[:] = [d for d in dirs if d not in ("build", "dist", "debug", "nbproject")]
        for a in archivos:
            if a.lower().endswith((".c", ".h")):
                salida.append(os.path.join(raiz, a))
    if not salida:
        raise Abortado("no hay ficheros .c/.h en %s. Comprobarlo con una segunda "
                       "herramienta antes de concluir que no existen." % carpeta)
    return sorted(salida)


def leer(ruta):
    with open(ruta, "rb") as f:
        return f.read().decode("latin-1")


def quitar_comentarios(texto):
    """Sustituye comentarios por espacios del mismo largo: las posiciones (y por tanto
    los numeros de linea) se conservan."""
    def blanco(m):
        return re.sub(r"[^\n]", " ", m.group(0))
    return re.sub(r"//[^\n]*|/\*.*?\*/", blanco, texto, flags=re.S)


def linea_de(texto, pos):
    return texto.count("\n", 0, pos) + 1


def cita(ruta, base, linea):
    return "%s:%d" % (os.path.relpath(ruta, base).replace("\\", "/"), linea)


RE_FUNC = re.compile(r"^[A-Za-z_][\w \t\*]*?\b([A-Za-z_]\w*)\s*\([^;{)]*\)\s*\{", re.M)


def funcion_contenedora(texto, pos):
    ultima = None
    for m in RE_FUNC.finditer(texto, 0, pos):
        if m.group(1) not in ("if", "while", "for", "switch"):
            ultima = m.group(1)
    return ultima


def referencias(nombre, textos):
    """Veces que aparece `nombre` en todos los fuentes (sin comentarios)."""
    patron = re.compile(r"\b%s\b" % re.escape(nombre))
    return sum(len(patron.findall(t)) for t in textos.values())


# ---------------------------------------------------------------------------
# 2.a generacion (STVA*, protocolo binario): el .vt
# ---------------------------------------------------------------------------

TIPOS_VT = ("KeyCodeReturn", "ASCII", "basic", "HardwareSet", "IncrementAdjust",
            "PicAnimation")
RE_REG_VT = re.compile(
    r"([A-Za-z]+):(-?[\d.]+),(-?[\d.]+),(-?[\d.]+),(-?[\d.]+),([^,;]*),([^,;]*),([^;]*)")


def tipo_normalizado(t):
    # El byte que precede al tipo es un prefijo binario del registro; cuando cae en
    # el rango imprimible se pega al nombre ("Ybasic", "\\basic"). Se normaliza por
    # sufijo para no perder el registro.
    for conocido in TIPOS_VT:
        if t.endswith(conocido):
            return conocido
    return t


def paginas_por_imagen(carpeta):
    paginas = {}
    if carpeta and os.path.isdir(carpeta):
        for a in os.listdir(carpeta):
            m = re.match(r"(\d+)", a)
            if m:
                paginas[int(m.group(1))] = a
    return paginas


def extraer_vt(ruta_vt):
    """Devuelve dict con botones de codigo, botones de navegacion, variables y otros.

    Campos medidos sobre lcd_RetroVertical.vt de 2020 (ver SKILL.md):
      KeyCodeReturn: x1,y1,x2,y2,nombre,tipo,0,DESTINO,-1,65029,PAGINA,0,VALOR,0,VALOR,...
      basic (Button): x1,y1,x2,y2,nombre,tipo,0,DESTINO,-1,0,PAGINA,...
      ASCII (Data variable): x1,y1,x2,y2,nombre,tipo,0,PAGINA,23056,65535,13,DIRECCION,...
    """
    crudo = open(ruta_vt, "rb").read().decode("latin-1")
    botones, navegacion, variables, otros = [], [], [], []
    for m in RE_REG_VT.finditer(crudo):
        tipo = tipo_normalizado(m.group(1))
        x1, y1, x2, y2 = (int(float(m.group(i))) for i in range(2, 6))
        resto = [c.strip() for c in m.group(8).split(",")]
        caja = (x1, y1, x2, y2)
        try:
            if tipo == "KeyCodeReturn":
                valor = int(resto[6])
                botones.append(dict(pagina=int(resto[4]), destino=int(resto[1]),
                                    valor=valor, codigo=valor & 0xFF, alto=valor >> 8,
                                    caja=caja, nombre=m.group(6)))
            elif tipo == "basic":
                navegacion.append(dict(pagina=int(resto[4]), destino=int(resto[1]),
                                       caja=caja, nombre=m.group(6)))
            elif tipo == "ASCII":
                variables.append(dict(pagina=int(resto[1]), direccion=int(resto[5]),
                                      caja=caja, nombre=m.group(6)))
            elif tipo == "IncrementAdjust":
                # Ajuste local de la pantalla (+/-) sobre una variable: no pasa por el PIC.
                otros.append(dict(tipo=tipo, caja=caja, nombre=m.group(6), pagina=int(resto[4]),
                                  direccion=int(resto[6]), campos=",".join(resto[:8])))
            else:
                otros.append(dict(tipo=tipo, caja=caja, nombre=m.group(6),
                                  campos=",".join(resto[:8])))
        except (IndexError, ValueError) as e:
            otros.append(dict(tipo=tipo + " (ILEGIBLE: %s)" % e, caja=caja,
                              nombre=m.group(6), campos=m.group(8)[:60]))
    if len(botones) < 1 or len(variables) < 1:
        raise Abortado("el .vt dio %d botones de codigo y %d variables. La lectura esta "
                       "rota o el archivo no es un proyecto STONE de 2.a generacion."
                       % (len(botones), len(variables)))
    # Suelo cruzado: cada registro KeyCodeReturn del archivo tiene que haberse leido.
    brutos = crudo.count("KeyCodeReturn:")
    if brutos != len(botones):
        raise Abortado("el archivo tiene %d 'KeyCodeReturn:' y se leyeron %d. No se cruza "
                       "un inventario incompleto." % (brutos, len(botones)))
    return dict(botones=botones, navegacion=navegacion, variables=variables, otros=otros)


def extraer_fw_v3(carpeta):
    textos = {r: quitar_comentarios(leer(r)) for r in fuentes_c(carpeta)}
    codigos = {}      # codigo -> [cita]
    offsets = set()
    direcciones = []  # dict(dir, efectiva, cita, funcion, viva)
    trunca = None
    for ruta, t in textos.items():
        for m in re.finditer(r"bufferPantalla\s*\[\s*(\d+)\s*\]\s*==\s*(0[xX][0-9a-fA-F]+|\d+)", t):
            offsets.add(int(m.group(1)))
            codigos.setdefault(int(m.group(2), 0), []).append(
                cita(ruta, carpeta, linea_de(t, m.start())))
        # Como arma la direccion enviarDatos2: solo el byte bajo?
        m = re.search(r"void\s+enviarDatos2\s*\([^)]*\)\s*\{", t)
        if m:
            cuerpo = t[m.end():m.end() + 1500]
            solo_bajo = re.search(r"direccion\s*&\s*0x00FF", cuerpo, re.I) and not \
                re.search(r"direccion\s*&\s*0xFF00", cuerpo, re.I)
            trunca = (bool(solo_bajo), cita(ruta, carpeta, linea_de(t, m.start())))
    for ruta, t in textos.items():
        for m in re.finditer(r"\benviarDatos2\s*\(([^;]*?),\s*(0[xX][0-9a-fA-F]+|\d+)\s*\)\s*;", t):
            d = int(m.group(2), 0)
            f = funcion_contenedora(t, m.start())
            viva = f is None or referencias(f, textos) > 1
            direcciones.append(dict(dir=d, efectiva=(d & 0xFF) if (trunca and trunca[0]) else d,
                                    cita=cita(ruta, carpeta, linea_de(t, m.start())),
                                    funcion=f, viva=viva))
    if not codigos:
        raise Abortado("no se encontro ninguna comparacion bufferPantalla[n] == codigo en "
                       "el firmware. La extraccion esta rota o no es el firmware de 2020.")
    return dict(codigos=codigos, offsets=offsets, direcciones=direcciones, trunca=trunca)


def cruzar_v3(vt, fw):
    """Devuelve (lista de (nivel, texto)). nivel: FALLA / AVISO / OK."""
    res = []
    enviados = {}
    for b in vt["botones"]:
        enviados.setdefault(b["codigo"], []).append(b)
    esperados = fw["codigos"]
    for c in sorted(set(enviados) - set(esperados)):
        res.append(("FALLA", "la pantalla envia 0x%02X (pag. %s) y el firmware no tiene rama: "
                    "el boton no hace nada" % (c, ",".join(str(b["pagina"]) for b in enviados[c]))))
    for c in sorted(set(esperados) - set(enviados)):
        res.append(("FALLA", "el firmware espera 0x%02X (%s) y ningun boton del .vt lo envia"
                    % (c, ", ".join(esperados[c]))))
    comunes = sorted(set(enviados) & set(esperados))
    res.append(("OK", "%d codigos presentes en los dos lados: %s" % (
        len(comunes), " ".join("0x%02X" % c for c in comunes))))
    huecos = [c for c in range(min(esperados), max(esperados) + 1)
              if c not in esperados and c not in enviados]
    for c in huecos:
        res.append(("AVISO", "0x%02X no lo envia ningun boton ni lo atiende el firmware "
                    "(hueco consistente en los dos lados)" % c))
    altos = sorted(set(b["alto"] for b in vt["botones"]))
    if len(altos) > 1:
        res.append(("AVISO", "los botones usan mas de un byte alto (%s); el firmware solo "
                    "mira el bajo" % altos))
    # Variables
    en_pantalla = set(v["direccion"] for v in vt["variables"])
    for d in fw["direcciones"]:
        if d["efectiva"] not in en_pantalla:
            nivel = "FALLA" if d["viva"] else "AVISO"
            extra = "" if d["viva"] else " -- codigo muerto: %s() no se llama en ningun sitio" % d["funcion"]
            conv = "" if d["efectiva"] == d["dir"] else " (llega como %d: enviarDatos2 solo manda el byte bajo)" % d["efectiva"]
            res.append((nivel, "el firmware escribe la direccion %d%s en %s y la pantalla no tiene "
                        "esa variable%s" % (d["dir"], conv, d["cita"], extra)))
    escritas = set(d["efectiva"] for d in fw["direcciones"])
    locales = set(o["direccion"] for o in vt["otros"] if o.get("direccion") is not None)
    for a in sorted(en_pantalla - escritas):
        if a in locales:
            res.append(("OK", "la variable %d la gobierna la propia pantalla (IncrementAdjust); "
                        "el firmware no tiene por que escribirla" % a))
        else:
            res.append(("AVISO", "la pantalla tiene la variable %d y el firmware no la escribe" % a))
    return res


def control_negativo_v3(vt, fw):
    """Quita el ultimo codigo en una COPIA del inventario y exige que el cruce lo vea."""
    victima = max(b["codigo"] for b in vt["botones"])
    copia = dict(vt)
    copia["botones"] = [b for b in vt["botones"] if b["codigo"] != victima]
    detectado = any(n == "FALLA" and ("0x%02X" % victima) in t for n, t in cruzar_v3(copia, fw))
    return victima, detectado


def informe_v3(args):
    vt = extraer_vt(args.vt)
    fw = extraer_fw_v3(args.fw)
    imagenes = args.imagenes or os.path.join(os.path.dirname(args.vt), "IMAGE")
    paginas = paginas_por_imagen(imagenes)

    def pag(n):
        return "%d (%s)" % (n, paginas.get(n, "?"))

    print("== Proyecto: %s" % args.vt)
    print("== Firmware: %s" % args.fw)
    print()
    print("-- Botones que envian codigo (KeyCodeReturn): %d" % len(vt["botones"]))
    print("   %-22s %-24s %-8s %-6s %s" % ("pagina", "destino", "valor", "cod.", "zona"))
    for b in sorted(vt["botones"], key=lambda b: (b["codigo"], b["pagina"])):
        print("   %-22s %-24s 0x%04X  0x%02X   %s" % (pag(b["pagina"]), pag(b["destino"]),
                                                      b["valor"], b["codigo"], b["caja"]))
    print()
    print("-- Cadenas de codigo (se sigue el destino de cada boton hasta cerrar el ciclo)")
    por_pagina = {}
    for b in vt["botones"]:
        por_pagina.setdefault(b["pagina"], []).append(b)
    raices = [b for b in vt["botones"]
              if b["pagina"] not in set(x["destino"] for x in vt["botones"])]
    for r in sorted(raices, key=lambda b: (b["pagina"], b["codigo"])):
        codigos, vistas, b = [], set(), r
        while b is not None and (b["pagina"], b["codigo"]) not in vistas:
            vistas.add((b["pagina"], b["codigo"]))
            codigos.append("0x%02X" % b["codigo"])
            siguientes = por_pagina.get(b["destino"], [])
            b = siguientes[0] if len(siguientes) == 1 else None
        print("   desde pag. %-3d %s" % (r["pagina"], " -> ".join(codigos)))
    print()
    print("-- Botones de navegacion (no envian nada al PIC): %d" % len(vt["navegacion"]))
    for b in sorted(vt["navegacion"], key=lambda b: b["pagina"]):
        print("   %-22s -> %-24s %s" % (pag(b["pagina"]), pag(b["destino"]), b["caja"]))
    print()
    por_dir = {}
    for v in vt["variables"]:
        por_dir.setdefault(v["direccion"], set()).add(v["pagina"])
    print("-- Variables (Data variable): %d registros, %d direcciones" % (len(vt["variables"]), len(por_dir)))
    for d in sorted(por_dir):
        print("   dir %-6d paginas %s" % (d, sorted(por_dir[d])))
    if vt["otros"]:
        print()
        print("-- Otros widgets (no cruzados, se listan para no perderlos): %d" % len(vt["otros"]))
        for o in vt["otros"]:
            print("   %-18s %s %s" % (o["tipo"], o["caja"], o["campos"]))
    print()
    print("-- Firmware: bufferPantalla[%s]" % ",".join(str(o) for o in sorted(fw["offsets"])))
    for c in sorted(fw["codigos"]):
        print("   0x%02X  %s" % (c, ", ".join(fw["codigos"][c])))
    if fw["trunca"]:
        print("   enviarDatos2 (%s): %s" % (fw["trunca"][1], "manda SOLO el byte bajo de la direccion"
                                          if fw["trunca"][0] else "manda la direccion de 16 bits"))
    for d in fw["direcciones"]:
        print("   enviarDatos2(..., %d) -> %d  %s  en %s()%s" % (
            d["dir"], d["efectiva"], d["cita"], d["funcion"], "" if d["viva"] else "  [MUERTA]"))
    print()
    print("== Cruce")
    res = cruzar_v3(vt, fw)
    victima, detectado = control_negativo_v3(vt, fw)
    for n, t in res:
        print("   [%s] %s" % (n, t))
    print("   [%s] control negativo: quitar 0x%02X de una copia del .vt %s" % (
        "OK" if detectado else "FALLA", victima,
        "se detecta" if detectado else "NO se detecta: el cruce no mide"))
    if not detectado:
        return ABORTADO
    return FALLA if any(n == "FALLA" for n, _ in res) else PASS


# ---------------------------------------------------------------------------
# 3.a generacion (STWA*, JSON ST<{...}>ET)
# ---------------------------------------------------------------------------

def extraer_fw_v4(carpeta):
    textos = {r: quitar_comentarios(leer(r)) for r in fuentes_c(carpeta)}
    escritos, ventanas, recibidos = {}, {}, {}
    for ruta, t in textos.items():
        # tramas salientes: {"cmd_code":"X",...,"widget":"Y"} escritas en C con \"
        for m in re.finditer(r'ST<\{([^}]*)\}>ET', t):
            trama = m.group(1).replace('\\"', '"')
            cmd = re.search(r'"cmd_code"\s*:\s*"([^"]+)"', trama)
            wid = re.search(r'"widget"\s*:\s*"([^"%]+)"', trama)
            if not wid:
                continue
            destino = ventanas if (cmd and cmd.group(1) == "open_win") else escritos
            destino.setdefault(wid.group(1), []).append(
                "%s %s" % (cita(ruta, carpeta, linea_de(t, m.start())), cmd.group(1) if cmd else "?"))
        # tramas entrantes: el nombre del widget entre < y >. Serial2.c descarta los
        # bytes <= 0x10 (codigo de orden, longitud, valor), asi que la trama del boton
        # queda "ST<button1>ET" y se busca "<button1>". Las busquedas sin <> son de
        # otro enlace (p. ej. LEERV del Bluetooth) y no entran.
        for m in re.finditer(r'strstr\s*\(\s*\w+\s*,\s*(?:\(char\s*\*\)\s*)?"<([A-Za-z_]\w*)>"', t):
            recibidos.setdefault(m.group(1), []).append(cita(ruta, carpeta, linea_de(t, m.start())))
    total = len(escritos) + len(ventanas) + len(recibidos)
    if total < 5:
        raise Abortado("solo se extrajeron %d nombres del firmware V4. La extraccion esta rota."
                       % total)
    return dict(escritos=escritos, ventanas=ventanas, recibidos=recibidos)


def widgets_st(ruta_st=None, ruta_manifest=None):
    """Inventario del proyecto de 3.a generacion (metodo de Camion _G): manifest.json
    dentro del .st (un ZIP). Incluye los widgets guardados fuera de widgets[]."""
    if ruta_st:
        with zipfile.ZipFile(ruta_st) as z:
            m = json.load(io.TextIOWrapper(z.open("manifest.json"), encoding="utf-8"))
    else:
        m = json.load(open(ruta_manifest, encoding="utf-8"))
    ventanas = m.get("windows")
    if not ventanas:
        raise Abortado("el manifest.json no trae 'windows': formato inesperado")
    por_ventana, fuera = {}, set()
    for wid, w in ventanas.items():
        nombre = (w.get("properties") or {}).get("name") or wid
        por_ventana[nombre] = set(v.get("name") for v in w.get("widgets", []) if v.get("name"))
        for k, v in w.items():
            if k not in ("properties", "widgets", "save_status") and isinstance(v, dict) and v.get("name"):
                fuera.add(v["name"])
    todos = set().union(*por_ventana.values()) if por_ventana else set()
    if len(todos) < 10:
        raise Abortado("inventario de pantalla sospechosamente corto: %d widgets" % len(todos))
    return por_ventana, todos, fuera


def informe_v4(args):
    fw = extraer_fw_v4(args.fw)
    print("== Firmware: %s" % args.fw)
    for titulo, clave in (("Widgets que el firmware ESCRIBE (set_text, set_value...)", "escritos"),
                          ("Ventanas que el firmware ABRE (open_win)", "ventanas"),
                          ("Nombres que el firmware ESPERA RECIBIR (strstr)", "recibidos")):
        d = fw[clave]
        print()
        print("-- %s: %d" % (titulo, len(d)))
        for n in sorted(d, key=lambda s: (re.sub(r"\d+", "", s), int(re.sub(r"\D", "", s) or 0))):
            print("   %-12s %s" % (n, "; ".join(d[n])))
    if not (args.st or args.manifest):
        print()
        print("== Cruce: ABORTADO. No hay proyecto de pantalla (.st / manifest.json) que cruzar.")
        print("   Lo anterior es lo que la pantalla TIENE que tener. Sin el proyecto, solo se")
        print("   valida en el equipo, con un sniffer en la linea TX de la STONE.")
        return ABORTADO
    por_ventana, todos, fuera = widgets_st(args.st, args.manifest)
    res = cruzar_v4(fw, por_ventana, todos, fuera)
    print()
    print("== Cruce")
    for n, t in res or [("OK", "todo nombre que usa el firmware existe en la pantalla")]:
        print("   [%s] %s" % (n, t))
    # Control negativo: se quita un nombre usado en una COPIA del inventario y se exige
    # que el cruce lo acuse. Sin esto, un inventario mal leido aprobaria cualquier cosa.
    necesarios = sorted((set(fw["escritos"]) | set(fw["recibidos"])) & todos)
    if not necesarios:
        print("   [FALLA] control negativo imposible: ningun nombre del firmware esta en la pantalla")
        return FALLA
    victima = necesarios[0]
    copia = set(todos) - {victima}
    detectado = any(n == "FALLA" and ("'%s'" % victima) in t
                    for n, t in cruzar_v4(fw, por_ventana, copia, fuera - {victima}))
    print("   [%s] control negativo: quitar '%s' de una copia del inventario %s" % (
        "OK" if detectado else "FALLA", victima, "se detecta" if detectado else "NO se detecta"))
    if not detectado:
        return ABORTADO
    return FALLA if any(n == "FALLA" for n, _ in res) else PASS


def cruzar_v4(fw, por_ventana, todos, fuera):
    necesarios = set(fw["escritos"]) | set(fw["recibidos"])
    res = []
    for n in sorted(necesarios):
        if n in todos:
            continue
        if n in fuera:
            res.append(("AVISO", "'%s' solo existe fuera de widgets[] (resto del editor): "
                        "comprobar en el equipo" % n))
        else:
            res.append(("FALLA", "el firmware usa '%s' y la pantalla no lo tiene: el comando se "
                        "pierde en silencio" % n))
    for v in sorted(fw["ventanas"]):
        if v not in por_ventana:
            res.append(("FALLA", "el firmware abre la ventana '%s' y no existe" % v))
    botones = sorted(n for n in todos if n.startswith("button") and n not in fw["recibidos"])
    if botones:
        res.append(("AVISO", "%d botones de la pantalla que el firmware no escucha: %s"
                    % (len(botones), ", ".join(botones))))
    return res


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="gen", required=True)
    a = sub.add_parser("v3", help="2.a generacion: .vt binario + bufferPantalla/enviarDatos2")
    a.add_argument("--vt", required=True)
    a.add_argument("--fw", required=True)
    a.add_argument("--imagenes")
    b = sub.add_parser("v4", help="3.a generacion: JSON ST<{...}>ET")
    b.add_argument("--fw", required=True)
    b.add_argument("--st")
    b.add_argument("--manifest")
    args = p.parse_args()
    try:
        codigo = informe_v3(args) if args.gen == "v3" else informe_v4(args)
    except Abortado as e:
        print("ABORTADO: %s" % e)
        codigo = ABORTADO
    print()
    print("RESULTADO: %s" % {PASS: "PASS", FALLA: "FALLA", ABORTADO: "ABORTADO"}[codigo])
    sys.exit(codigo)


if __name__ == "__main__":
    main()
