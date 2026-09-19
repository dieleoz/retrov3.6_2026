# -*- coding: utf-8 -*-
"""
Genera los iconos SVG de 08_Senales/iconos/ a partir de 08_Senales/senales.csv.

Uso (desde cualquier directorio):
    python 08_Senales/tools/generar_iconos.py

Los iconos son dibujos propios, esquemáticos, de 64x64 de viewBox: forma, colores
de fondo/orla/símbolo tomados del CSV y un pictograma simplificado. No se extrae
ninguna imagen del Manual. Cuando el pictograma real es complejo (animales,
paisajes, edificios...), el icono lleva una etiqueta de texto corta en su lugar.
El código de la señal va siempre visible en la franja inferior.

Colores de pantalla: Tabla I-1 del Manual de Señalización Vial 2024, p. 894
(valores RGB de "Visualización"). Los que la tabla no trae (anaranjado
fluorescente, rosado fluorescente) y el gris de símbolo son tonos propios,
marcados abajo. Son colores de pantalla, NO coordenadas cromáticas: no sirven
para aceptar o rechazar una lámina.

Regenerar tras editar el CSV; también reescribe 08_Senales/indice.html.
"""
import csv
import html
import math
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(BASE, "senales.csv")
OUT_DIR = os.path.join(BASE, "iconos")
INDEX = os.path.join(BASE, "indice.html")

# --- colores ------------------------------------------------------------
COL = {
    "blanco": "#FEFEFE",            # p.894
    "negro": "#373435",             # p.894
    "gris": "#9D9FA2",              # propio (p.894 da 230,231,232: ilegible como símbolo sobre blanco)
    "amarillo": "#FFCC29",          # p.894
    "azul": "#2E4C72",              # p.894
    "rojo": "#ED3237",              # p.894
    "verde": "#1B7A57",             # propio, ver nota
    "amarillo_verde_fluorescente": "#B4D343",  # p.894
    "marron": "#634431",            # p.894
    "anaranjado": "#F47A35",        # p.894
    "purpura": "#EE3A82",           # p.894 (sin uso en ninguna señal)
    "anaranjado_fluorescente": "#FF5A1F",      # propio
    "rosado_fluorescente": "#FF4FA3",          # propio
}
# Nota verde: p.894 da 85,186,94 (#55BA5E); con letra blanca da contraste
# insuficiente en 64 px. Se usa un verde oscuro propio. Cambiar aquí si se
# prefiere el literal de la tabla.


def c(name):
    return COL.get(name.split("|")[0].split("+")[0].strip(), "#888888")


def sym_colors(s):
    parts = [p.strip() for p in s.split("+")]
    return [COL.get(p, "#888888") for p in parts]


# --- utilidades de dibujo ------------------------------------------------
def poly(pts, fill, extra=""):
    return '<polygon points="%s" fill="%s"%s/>' % (" ".join("%.1f,%.1f" % p for p in pts), fill, extra)


def regular(n, cx, cy, r, rot):
    return [(cx + r * math.cos(math.radians(rot + 360.0 * i / n)),
             cy + r * math.sin(math.radians(rot + 360.0 * i / n))) for i in range(n)]


def text(t, x, y, size, fill, weight="700", anchor="middle"):
    return ('<text x="%.1f" y="%.1f" font-family="Arial,Helvetica,sans-serif" font-size="%.1f" '
            'font-weight="%s" text-anchor="%s" fill="%s">%s</text>'
            % (x, y, size, weight, anchor, fill, html.escape(t)))


def multitext(lines, cx, cy, maxw, maxh, fill):
    """Bloque de texto centrado que cabe en maxw x maxh."""
    n = len(lines)
    longest = max(len(l) for l in lines) or 1
    size = min(maxh / (n * 1.1), maxw / (longest * 0.72), 12)
    out = []
    top = cy - (n * size * 1.05) / 2 + size * 0.85
    for i, l in enumerate(lines):
        out.append(text(l, cx, top + i * size * 1.05, size, fill))
    return "".join(out)


# --- glifos en caja 40x40 (origen arriba-izquierda) ----------------------
def g_arrow(fg, rot=0):
    s = poly([(20, 3), (31, 15), (24, 15), (24, 37), (16, 37), (16, 15), (9, 15)], fg)
    return '<g transform="rotate(%d 20 20)">%s</g>' % (rot, s) if rot else s


def stroke(d, fg, w=5, cap="butt"):
    return '<path d="%s" fill="none" stroke="%s" stroke-width="%s" stroke-linecap="%s" stroke-linejoin="round"/>' % (d, fg, w, cap)


def head(x, y, ang, fg, size=7):
    # punta de flecha en (x,y) apuntando a 'ang' grados (0 = arriba)
    pts = [(0, -size), (size, size * 0.6), (-size, size * 0.6)]
    a = math.radians(ang)
    rp = [(x + px * math.cos(a) - py * math.sin(a), y + px * math.sin(a) + py * math.cos(a)) for px, py in pts]
    return poly(rp, fg)


def mirror(svg):
    return '<g transform="translate(40 0) scale(-1 1)">%s</g>' % svg


def g_turn_left(fg):   # giro en ángulo recto
    return stroke("M24 38 V18 H12", fg, 6) + head(9, 18, -90, fg, 8)


def g_curve_left(fg):  # curva suave
    return stroke("M24 38 V26 Q24 12 12 12", fg, 6) + head(9, 12, -90, fg, 8)


def g_reverse_left(fg):  # curva y contracurva cerrada
    return stroke("M26 38 V30 H14 V20 H22 V12", fg, 5) + head(22, 8, 0, fg, 7)


def g_scurve_left(fg):
    return stroke("M24 38 C24 26 12 28 14 20 C16 12 24 16 24 10", fg, 5) + head(24, 7, 0, fg, 7)


def g_winding_left(fg):
    return stroke("M20 38 C12 34 12 30 20 27 C28 24 28 20 20 17 C12 14 14 12 20 10", fg, 5) + head(20, 7, 10, fg, 7)


def g_hairpin_left(fg):
    return stroke("M26 38 V16 A6 6 0 0 0 14 16 V28", fg, 6) + head(14, 32, 180, fg, 8)


def g_uturn(fg):
    return stroke("M26 38 V16 A6 6 0 0 0 14 16 V26", fg, 5) + head(14, 30, 180, fg, 7)


def roads(d, fg):
    return stroke(d, fg, 7)


GLYPH_BASIC = {
    "arrow_up": lambda fg: g_arrow(fg),
    "arrow_left": lambda fg: g_arrow(fg, -90),
    "arrow_right": lambda fg: g_arrow(fg, 90),
    "arrow_ur": lambda fg: g_arrow(fg, 35),
    "arrow_ul": lambda fg: g_arrow(fg, -35),
    "arrow_lr": lambda fg: poly([(2, 20), (11, 11), (11, 17), (29, 17), (29, 11), (38, 20), (29, 29), (29, 23), (11, 23), (11, 29)], fg),
    "updown": lambda fg: '<g transform="translate(-8 0) scale(1 1)">%s</g><g transform="translate(8 0)">%s</g>' % (g_arrow(fg, 180), g_arrow(fg)),
    "three_1": lambda fg: '<g transform="translate(-11 0)">%s</g><g>%s</g><g transform="translate(11 0)">%s</g>' % (g_arrow(fg, 180), g_arrow(fg), g_arrow(fg)),
    "three_2": lambda fg: '<g transform="translate(-11 0)">%s</g><g>%s</g><g transform="translate(11 0)">%s</g>' % (g_arrow(fg, 180), g_arrow(fg, 180), g_arrow(fg)),
    "turn_left": g_turn_left,
    "turn_right": lambda fg: mirror(g_turn_left(fg)),
    "curve_left": g_curve_left,
    "curve_right": lambda fg: mirror(g_curve_left(fg)),
    "reverse_left": g_reverse_left,
    "reverse_right": lambda fg: mirror(g_reverse_left(fg)),
    "scurve_left": g_scurve_left,
    "scurve_right": lambda fg: mirror(g_scurve_left(fg)),
    "winding_left": g_winding_left,
    "winding_right": lambda fg: mirror(g_winding_left(fg)),
    "hairpin_left": g_hairpin_left,
    "hairpin_right": lambda fg: mirror(g_hairpin_left(fg)),
    "uturn": g_uturn,
    "cross": lambda fg: roads("M20 4 V36 M4 20 H36", fg),
    "side_left": lambda fg: roads("M22 4 V36 M6 20 H22", fg),
    "side_right": lambda fg: roads("M18 4 V36 M18 20 H34", fg),
    "tee": lambda fg: roads("M20 36 V14 M4 14 H36", fg),
    "y": lambda fg: roads("M20 36 V22 L8 6 M20 22 L32 6", fg),
    "fork_left": lambda fg: roads("M22 36 V4 M22 22 L10 8", fg),
    "fork_right": lambda fg: roads("M18 36 V4 M18 22 L30 8", fg),
    "stagger_left": lambda fg: roads("M20 4 V36 M6 26 H20 M20 14 H34", fg),
    "stagger_right": lambda fg: roads("M20 4 V36 M34 26 H20 M20 14 H6", fg),
    "merge_left": lambda fg: roads("M24 36 V4 M8 36 L24 16", fg),
    "merge_right": lambda fg: roads("M16 36 V4 M32 36 L16 16", fg),
    "roundabout": lambda fg: stroke("M20 8 A12 12 0 1 1 8 20", fg, 4) + head(8, 22, 180, fg, 5)
                               + stroke("M32 20 A12 12 0 0 1 20 32", fg, 4) + head(18, 32, -90, fg, 5),
    "narrow_both": lambda fg: stroke("M10 36 V24 L15 16 V4 M30 36 V24 L25 16 V4", fg, 4),
    "narrow_left": lambda fg: stroke("M10 36 V24 L16 16 V4 M26 36 V4", fg, 4),
    "narrow_right": lambda fg: stroke("M14 36 V4 M30 36 V24 L24 16 V4", fg, 4),
    "widen_both": lambda fg: stroke("M15 36 V24 L10 16 V4 M25 36 V24 L30 16 V4", fg, 4),
    "widen_left": lambda fg: stroke("M16 36 V24 L10 16 V4 M26 36 V4", fg, 4),
    "widen_right": lambda fg: stroke("M14 36 V4 M24 36 V24 L30 16 V4", fg, 4),
    "bridge": lambda fg: stroke("M8 36 L14 28 V12 L8 4 M32 36 L26 28 V12 L32 4", fg, 4),
    "bumps": lambda fg: '<path d="M4 30 Q9 18 14 30 Q20 18 26 30 Q31 18 36 30 Z" fill="%s"/>' % fg,
    "bump": lambda fg: '<path d="M4 28 Q20 8 36 28 Z" fill="%s"/>' % fg,
    "bump_arrow": lambda fg: '<path d="M4 20 Q20 2 36 20 Z" fill="%s"/>' % fg + poly([(20, 38), (12, 28), (17, 28), (17, 22), (23, 22), (23, 28), (28, 28)], fg),
    "trap": lambda fg: poly([(4, 28), (12, 18), (28, 18), (36, 28)], fg),
    "trap_arrow": lambda fg: poly([(4, 20), (12, 11), (28, 11), (36, 20)], fg) + poly([(20, 38), (12, 28), (17, 28), (17, 22), (23, 22), (23, 28), (28, 28)], fg),
    "dip": lambda fg: '<path d="M4 16 H10 Q20 32 30 16 H36 V20 H32 Q20 38 8 20 H4 Z" fill="%s"/>' % fg,
    "slope_down": lambda fg: poly([(4, 10), (36, 30), (4, 30)], fg) + text("10%", 27, 16, 8, fg),
    "slope_up": lambda fg: poly([(36, 10), (36, 30), (4, 30)], fg) + text("10%", 13, 16, 8, fg),
    "lights": lambda fg: '<rect x="13" y="3" width="14" height="34" rx="3" fill="%s"/>' % fg
                         + '<circle cx="20" cy="10" r="4" fill="%s"/><circle cx="20" cy="20" r="4" fill="%s"/><circle cx="20" cy="30" r="4" fill="%s"/>'
                         % (COL["rojo"], COL["amarillo"], "#2FA84F"),
    "chevron": lambda fg: poly([(8, 4), (22, 4), (34, 20), (22, 36), (8, 36), (20, 20)], fg),
    "rail": lambda fg: stroke("M6 34 L34 6 M6 6 L34 34", fg, 5) + stroke("M4 20 H36", fg, 2),
    "tunnel": lambda fg: '<path d="M6 36 V18 A14 14 0 0 1 34 18 V36 H28 V20 A8 8 0 0 0 12 20 V36 Z" fill="%s"/>' % fg,
    "rocks": lambda fg: poly([(6, 34), (16, 12), (22, 22), (34, 34)], fg) + '<circle cx="30" cy="12" r="3" fill="%s"/><circle cx="34" cy="20" r="2.5" fill="%s"/>' % (fg, fg),
    "height": lambda fg: poly([(12, 4), (28, 4), (20, 11)], fg) + poly([(12, 36), (28, 36), (20, 29)], fg) + text("4,50", 20, 24, 9, fg),
    "width": lambda fg: poly([(4, 12), (4, 28), (11, 20)], fg) + poly([(36, 12), (36, 28), (29, 20)], fg) + text("3,20", 20, 24, 9, fg),
    "sep_two": lambda fg: stroke("M14 36 V26 Q14 18 8 12", fg, 4) + head(8, 10, -40, fg, 5) + stroke("M26 36 V26 Q26 18 32 12", fg, 4) + head(32, 10, 40, fg, 5),
    "fog": lambda fg: text("NIEBLA", 20, 25, 10, fg),
}


def g_car(fg):
    return ('<path d="M6 26 L9 17 Q10 14 14 14 H26 Q30 14 31 17 L34 26 Z" fill="%s"/>'
            '<rect x="4" y="24" width="32" height="7" rx="2" fill="%s"/>'
            '<circle cx="11" cy="32" r="3.5" fill="%s"/><circle cx="29" cy="32" r="3.5" fill="%s"/>') % (fg, fg, fg, fg)


def g_truck(fg):
    return ('<rect x="3" y="10" width="22" height="18" fill="%s"/><path d="M27 16 H33 L37 22 V28 H27 Z" fill="%s"/>'
            '<circle cx="10" cy="31" r="3.5" fill="%s"/><circle cx="31" cy="31" r="3.5" fill="%s"/>') % (fg, fg, fg, fg)


def g_bus(fg):
    return ('<rect x="3" y="11" width="34" height="18" rx="3" fill="%s"/>'
            '<rect x="6" y="14" width="6" height="6" fill="white"/><rect x="14" y="14" width="6" height="6" fill="white"/>'
            '<rect x="22" y="14" width="6" height="6" fill="white"/><rect x="30" y="14" width="5" height="6" fill="white"/>'
            '<circle cx="10" cy="31" r="3.5" fill="%s"/><circle cx="30" cy="31" r="3.5" fill="%s"/>') % (fg, fg, fg)


def g_bike(fg):
    return ('<circle cx="10" cy="26" r="7" fill="none" stroke="%s" stroke-width="2.5"/>'
            '<circle cx="30" cy="26" r="7" fill="none" stroke="%s" stroke-width="2.5"/>'
            '<path d="M10 26 L16 15 H27 L30 26 M16 15 L21 26 L27 15 M14 12 H19" fill="none" stroke="%s" stroke-width="2.5" stroke-linejoin="round"/>') % (fg, fg, fg)


def g_moto(fg):
    return (poly([(14, 22), (18, 14), (28, 14), (30, 20), (24, 24)], fg) + '<circle cx="9" cy="27" r="6" fill="none" stroke="%s" stroke-width="3"/>'
            '<circle cx="31" cy="27" r="6" fill="none" stroke="%s" stroke-width="3"/>'
            '<path d="M9 27 L17 17 H27 L31 27 M22 17 L26 11" fill="none" stroke="%s" stroke-width="3.5" stroke-linejoin="round"/>') % (fg, fg, fg)


def g_ped(fg):
    return ('<circle cx="21" cy="7" r="4" fill="%s"/>'
            '<path d="M20 12 L17 24 L12 36 M17 24 L24 36 M19 15 L12 21 M20 15 L28 20" fill="none" stroke="%s" stroke-width="4" stroke-linecap="round"/>') % (fg, fg)


def g_peds(fg):
    return ('<g transform="translate(-7 2) scale(0.9)">%s</g><g transform="translate(9 8) scale(0.75)">%s</g>' % (g_ped(fg), g_ped(fg)))


def g_letter(ch, size=30):
    return lambda fg: text(ch, 20, 20 + size * 0.36, size, fg)


def g_redcross(fg):
    return '<rect x="15" y="5" width="10" height="30" fill="%s"/><rect x="5" y="15" width="30" height="10" fill="%s"/>' % (COL["rojo"], COL["rojo"])


def g_plane(fg):
    return poly([(20, 3), (23, 14), (37, 22), (37, 25), (23, 21), (22, 32), (27, 36), (27, 38), (20, 36), (13, 38), (13, 36), (18, 32), (17, 21), (3, 25), (3, 22), (17, 14)], fg)


def g_fuel(fg):
    return ('<rect x="8" y="6" width="16" height="30" rx="2" fill="%s"/><rect x="11" y="9" width="10" height="8" fill="white"/>'
            '<path d="M24 14 H29 V30 Q29 33 32 33 Q35 33 35 30 V14 L31 10" fill="none" stroke="%s" stroke-width="2.5"/>') % (fg, fg)


def g_phone(fg):
    return '<path d="M8 8 Q8 4 12 4 L16 12 L12 15 Q16 24 25 28 L28 24 L36 28 Q36 36 32 36 Q10 34 8 8 Z" fill="%s"/>' % fg


def g_ext(fg):
    return ('<rect x="14" y="12" width="12" height="24" rx="5" fill="%s"/><rect x="17" y="6" width="6" height="7" fill="%s"/>'
            '<path d="M22 8 H32 L34 12" fill="none" stroke="%s" stroke-width="2.5"/>') % (COL["rojo"], COL["rojo"], fg)


GLYPH_BASIC.update({
    "car": g_car, "truck": g_truck, "bus": g_bus, "bike": g_bike, "moto": g_moto,
    "ped": g_ped, "peds": g_peds, "P": g_letter("P"), "H": g_letter("H"), "i": g_letter("i"),
    "redcross": g_redcross, "plane": g_plane, "fuel": g_fuel, "phone": g_phone, "ext": g_ext,
    "moto_green": g_moto,
})


# --- asignación código -> glifo ------------------------------------------
# Prefijo "txt:" = texto literal (líneas separadas por "/").
G = {
    # SR
    "SR-01": "txt:PARE", "SR-02": "txt:CEDA/EL/PASO", "SR-04": "noentry", "SR-49": "updown_red",
    "SR-06": "turn_left", "SR-08": "turn_right", "SR-10": "uturn", "SR-14": "scurve_right",
    "SR-14A": "scurve_left", "SR-26": "car", "SR-50": "txt:NO GIRO/LUZ ROJA", "SR-56": "txt:PRIORIDAD/PEATONAL",
    "SR-16": "car", "SR-18": "truck", "SR-18A": "truck", "SR-18B": "truck", "SR-21": "txt:CABAL.",
    "SR-22": "bike", "SR-23": "moto", "SR-24": "txt:MAQ./AGR.", "SR-25": "txt:TRACC./ANIMAL",
    "SR-51": "txt:CARRO/MANO", "SR-52": "bus", "SR-53": "txt:MOTO-/CARRO", "SR-54": "txt:CUATRI-/MOTO",
    "SR-20": "ped", "SR-28": "P", "SR-28A": "P", "SR-29": "txt:PITO", "SR-41": "bus", "SR-43": "truck",
    "SR-47": "cross", "SR-11": "updown", "SR-12": "three_1", "SR-13": "three_2", "SR-30": "txt:50",
    "SR-30A": "txt:50/MIN", "SR-30B": "txt:50", "SR-31": "txt:20t", "SR-32": "height", "SR-33": "width",
    "SR-55": "txt:10 m", "SR-48": "txt:FIN", "SR-03": "arrow_up", "SR-05": "turn_left", "SR-07": "turn_right",
    "SR-09": "uturn", "SR-17": "truck", "SR-19": "ped", "SR-35": "txt:LUCES/BAJAS", "SR-36": "bar",
    "SR-38": "arrow_right", "SR-39": "arrow_lr", "SR-44": "car", "SR-45": "arrow_ul", "SR-46": "arrow_ur",
    "SR-58": "bus", "SR-59": "txt:1,5 m", "SR-34": "txt:SOLO/TAXI", "SR-40": "bus", "SR-42": "truck",
    # SP (y SPPO por número)
    "SP-01": "turn_left", "SP-02": "turn_right", "SP-03": "curve_left", "SP-04": "curve_right",
    "SP-05": "reverse_left", "SP-06": "reverse_right", "SP-07": "winding_left", "SP-08": "winding_right",
    "SP-09": "scurve_left", "SP-10": "scurve_right", "SP-69": "hairpin_left", "SP-70": "hairpin_right",
    "SP-75": "chevron", "SP-27": "slope_down", "SP-27A": "slope_up", "SP-24": "bumps", "SP-25": "bump",
    "SP-25A": "bump_arrow", "SP-25B": "trap", "SP-25C": "trap_arrow", "SP-26": "dip", "SP-57": "txt:FIN/PAV.",
    "SP-57A": "txt:TEXTURA", "SP-28": "narrow_both", "SP-30": "narrow_left", "SP-31": "narrow_right",
    "SP-32": "widen_both", "SP-34": "widen_left", "SP-35": "widen_right", "SP-36": "bridge", "SP-38": "txt:20t",
    "SP-50": "height", "SP-51": "width", "SP-76": "txt:10 m", "SP-11": "cross", "SP-12": "side_left",
    "SP-13": "side_right", "SP-14": "tee", "SP-15": "y", "SP-16": "fork_left", "SP-17": "fork_right",
    "SP-18": "stagger_left", "SP-19": "stagger_right", "SP-20": "roundabout", "SP-21": "merge_left",
    "SP-22": "merge_right", "SP-52": "rail", "SP-52A": "rail", "SP-53": "car", "SP-54": "txt:FERROCARRIL",
    "SP-81": "rail", "SP-23": "lights", "SP-29": "txt:PARE", "SP-33": "txt:CEDA", "SP-39": "updown",
    "SP-41": "three_1", "SP-43": "three_2", "SP-45": "txt:MAQ./AGR.", "SP-79": "truck", "SP-46": "ped",
    "SP-46A": "ped", "SP-46B": "ped", "SP-46C": "ped", "SP-47": "peds", "SP-47A": "peds", "SP-47B": "peds",
    "SP-48": "peds", "SP-49": "txt:ANIMAL", "SP-49A": "txt:ANIMAL", "SP-55": "sep_two", "SP-55A": "sep_two",
    "SP-56": "sep_two", "SP-56A": "sep_two", "SP-59": "bike", "SP-59A": "bike", "SP-59B": "bike",
    "SP-80": "txt:TRICI-/MÓVIL", "SP-37": "tunnel", "SP-42": "rocks", "SP-44": "car", "SP-67": "car",
    "SP-71": "txt:GRAVILLA", "SP-72": "truck", "SP-73": "txt:VIENTO", "SP-74": "txt:DESNIVEL", "SP-77": "fog",
    "SP-78": "txt:PUENTE/LEVADIZO",
    # SI
    "SI-01": "txt:45", "SI-01A": "txt:20", "SI-02": "txt:PANAM.", "SI-03": "txt:SELVA", "SI-04": "txt:Pr/7",
    "SI-05": "txt:DESTINO", "SI-05A": "txt:SALIDA", "SI-05B": "roundabout", "SI-05C": "txt:RUTA/ALTERNA",
    "SI-05D": "txt:PRESEÑAL", "SI-06": "txt:DESTINO 32", "SI-26": "txt:Carrera 50",
    "SI-07": "P", "SI-07A": "P", "SI-07B": "P", "SI-07C": "P", "SI-08": "bus", "SI-09": "txt:TAXI",
    "SI-09A": "txt:TRICI", "SI-10": "txt:FERRY", "SI-11": "bike", "SI-11A": "bike", "SI-13": "txt:MILITAR",
    "SI-14": "plane", "SI-15": "txt:HOTEL", "SI-16": "redcross", "SI-16A": "H", "SI-17": "txt:WC",
    "SI-18": "txt:REST.", "SI-19": "phone", "SI-20": "txt:IGLESIA", "SI-21": "txt:TALLER", "SI-22": "fuel",
    "SI-22A": "txt:EV", "SI-23": "txt:LLANTAS", "SI-25": "txt:ACCES.", "SI-25A": "ped", "SI-27": "txt:SEG./VIAL",
    "SI-27A": "ped", "SI-27B": "txt:RADAR", "SI-29": "txt:TREN", "SI-30": "bus", "SI-30A": "txt:CABLE",
    "SI-31": "txt:RECREO", "SI-32": "txt:TSUNAMI", "SI-33": "txt:TSUNAMI", "SI-34": "txt:TSUNAMI",
    "SI-35": "txt:DETEC.", "SI-35A": "txt:DETEC.",
    # ST
    "ST-06": "i", "ST-12": "car", "ST-12A": "bike",
    # SIT
    "SIT-01": "ped", "SIT-02": "ped", "SIT-03": "ped", "SIT-04": "ped", "SIT-05": "phone", "SIT-06": "ext",
    "SIT-07": "txt:HIDRANTE", "SIT-08": "txt:SOS", "SIT-09": "txt:RADIO",
    # Capítulo 6
    "SIP-01": "txt:PASO/PEATONAL", "SIP-02": "rail", "SIP-03": "arrow_lr", "SIP-04": "txt:10",
    "SRC-01": "arrow_right", "SRC-02": "ped", "SRC-03": "bike", "SRC-04": "txt:MASCOTAS", "SRC-05": "ped",
    "SRC-06": "bike", "SRC-07": "bike", "SRC-08": "bike", "SRC-09": "bike",
    "SPC-01": "car", "SPC-02": "slope_down", "SPC-03": "slope_up", "SPC-04": "updown",
    "SIC-01": "bike", "SIC-02": "txt:AV. 26", "SIC-02A": "txt:CL. 85", "SIC-02B": "side_right", "SIC-04": "bike",
    "SIC-05": "bike", "SIC-06": "peds", "SIC-09": "txt:EXCEPTO", "SIC-11": "bus", "SIC-12": "txt:AV SUBA",
    "SIC-12A": "txt:U NACIONAL", "SRM-01": "moto", "SIM-01": "moto", "SIM-02": "moto", "SIM-03": "moto",
    "SIM-04": "moto", "SIM-05": "moto", "SIM-06": "P",
    # Capítulo 7
    "SRO-01": "txt:VÍA/CERRADA", "SRO-02": "txt:DESVÍO", "SRO-03": "txt:UNO/A/UNO", "SRO-04": "paleta",
    "SPO-01": "ped", "SPO-02": "truck", "SPO-03": "ped", "SPO-04": "narrow_both", "SPO-05": "narrow_right",
    "SPO-06": "narrow_left",
    "SIO-02": "txt:INICIO DE/OBRA", "SIO-03": "txt:FIN DE/OBRA", "SIO-05": "txt:DESVÍO/A XXX m",
    "SIO-07": "txt:DESVÍO", "SIO-08": "txt:FIN/DESVÍO", "SIO-23": "bus", "SIO-24": "txt:SENDERO/PEATONAL",
    "SIO-25": "txt:SEMÁFORO/FUERA DE/SERVICIO", "SIO-26": "txt:CRUCE/PEATONAL/CERRADO", "SIO-27": "txt:OBRA",
}
for _n, _g in ((9, "merge_right"), (10, "merge_right"), (11, "merge_left"), (12, "merge_left"),
               (13, "fork_right"), (14, "fork_right"), (15, "fork_left"), (16, "fork_left"),
               (17, "scurve_right"), (18, "scurve_right"), (19, "scurve_left"), (20, "scurve_left"),
               (21, "scurve_right"), (22, "scurve_left")):
    G["SIO-%02d" % _n] = _g

PROHIB = {"SR-06", "SR-08", "SR-10", "SR-14", "SR-14A", "SR-26", "SR-16", "SR-18", "SR-18A", "SR-18B",
          "SR-21", "SR-22", "SR-23", "SR-24", "SR-25", "SR-51", "SR-52", "SR-53", "SR-54", "SR-20", "SR-28",
          "SR-28A", "SR-29", "SR-41", "SR-43", "SR-47", "SRC-04", "SRO-01"}


def short_label(nombre):
    """Etiqueta corta de respaldo a partir del nombre oficial."""
    stop = {"DE", "LA", "EL", "EN", "Y", "A", "DEL", "LAS", "LOS", "O", "POR", "CON", "PARA", "AL"}
    words = [w for w in re.split(r"[\s/()–-]+", nombre.upper()) if w and w not in stop]
    lines = []
    for w in words[:2]:
        lines.append(w[:10])
    return lines or ["?"]


def glyph_svg(code, row, fg):
    key = G.get(code)
    if key is None and code.startswith("SPPO-"):
        key = G.get("SP-" + code.split("-", 1)[1])
    if key is None:
        key = "txt:" + "/".join(short_label(row["nombre"]))
    if key.startswith("txt:"):
        lines = key[4:].split("/")
        return ("TEXT", lines)
    if key == "lanes":
        return ("RAW", "".join('<g transform="translate(%d 0) scale(1 1)">%s</g>' % (dx, g_arrow(fg)) for dx in (-9, 9)))
    if key == "updown_red":
        return ("RAW", '<g transform="translate(-8 0)">%s</g><g transform="translate(8 0)">%s</g>'
                % (g_arrow(COL["rojo"], 180), g_arrow(fg)))
    if key in ("noentry", "bar", "paleta"):
        return ("SPECIAL", key)
    return ("RAW", GLYPH_BASIC[key](fg))


# --- formas -----------------------------------------------------------------
def shape_svg(forma, fondo, orla):
    f, o = c(fondo), c(orla)
    forma = forma.split("|")[0]
    if forma == "circulo":
        return ('<circle cx="32" cy="27" r="24" fill="%s"/><circle cx="32" cy="27" r="19" fill="%s"/>' % (o, f),
                (32, 27, 30, 0.72))
    if forma == "octogono":
        return (poly(regular(8, 32, 27, 25, 22.5), f) + poly(regular(8, 32, 27, 22.5, 22.5), "none",
                ' stroke="%s" stroke-width="1.5"' % o), (32, 27, 38, 0.9))
    if forma == "triangulo_invertido":
        return (poly([(6, 4), (58, 4), (32, 50)], o) + poly([(14, 8.5), (50, 8.5), (32, 40)], f), (32, 18, 22, 0.55))
    if forma == "rombo":
        return (poly([(32, 2), (57, 27), (32, 52), (7, 27)], f)
                + poly([(32, 5), (54, 27), (32, 49), (10, 27)], "none", ' stroke="%s" stroke-width="1.8"' % o),
                (32, 27, 26, 0.65))
    if forma == "pentagono":
        return (poly([(32, 2), (55, 19), (55, 52), (9, 52), (9, 19)], f)
                + poly([(32, 5), (52, 20), (52, 49), (12, 49), (12, 20)], "none", ' stroke="%s" stroke-width="1.8"' % o),
                (32, 31, 30, 0.72))
    if forma == "rectangulo_vertical":
        return ('<rect x="13" y="2" width="38" height="50" rx="4" fill="%s"/>'
                '<rect x="15.5" y="4.5" width="33" height="45" rx="3" fill="none" stroke="%s" stroke-width="1.8"/>' % (f, o),
                (32, 27, 30, 0.72))
    if forma == "rectangulo_horizontal":
        return ('<rect x="3" y="11" width="58" height="32" rx="4" fill="%s"/>'
                '<rect x="5.5" y="13.5" width="53" height="27" rx="3" fill="none" stroke="%s" stroke-width="1.8"/>' % (f, o),
                (32, 27, 50, 0.6))
    if forma == "cuadrado":
        return ('<rect x="8" y="2" width="48" height="50" rx="4" fill="%s"/>'
                '<rect x="10.5" y="4.5" width="43" height="45" rx="3" fill="none" stroke="%s" stroke-width="1.8"/>' % (f, o),
                (32, 27, 38, 0.9))
    if forma == "escudo":
        d = "M10 4 H54 V30 Q54 44 32 52 Q10 44 10 30 Z"
        return ('<path d="%s" fill="%s" stroke="%s" stroke-width="2.5"/>' % (d, f, o), (32, 26, 30, 0.7))
    if forma == "cruz_san_andres":
        bar = '<rect x="2" y="21" width="60" height="12" fill="%s" stroke="%s" stroke-width="1.5" transform="rotate(%d 32 27)"/>'
        return (bar % (f, o, -30) + bar % (f, o, 30), (32, 27, 44, 0.0))
    if forma == "flecha":
        return (poly([(3, 16), (48, 16), (61, 27), (48, 38), (3, 38)], f)
                + poly([(5.5, 18.5), (47, 18.5), (57.5, 27), (47, 35.5), (5.5, 35.5)], "none", ' stroke="%s" stroke-width="1.5"' % o),
                (29, 27, 40, 0.5))
    return ('<rect x="8" y="2" width="48" height="50" fill="%s"/>' % f, (32, 27, 38, 0.9))


def build_icon(row):
    code = row["codigo"]
    parts, extra = [], []
    shape, (cx, cy, tw, gs) = shape_svg(row["forma"], row["color_fondo"], row["color_orla"])
    fg_list = sym_colors(row["color_simbolo"])
    fg = fg_list[0]
    kind, g = glyph_svg(code, row, fg)
    if row["forma"].startswith("cruz_san_andres"):
        parts.append(shape)
        parts.append('<g transform="rotate(-30 32 27)">%s</g>' % text("FERROCARRIL", 32, 29.5, 6.5, fg))
    elif kind == "SPECIAL" and g == "paleta":
        parts.append(poly(regular(8, 20, 26, 15, 22.5), COL["rojo"]) + text("PARE", 20, 29, 7, COL["blanco"]))
        parts.append('<circle cx="46" cy="26" r="14" fill="%s"/>' % COL["verde"] + text("SIGA", 46, 29, 7, COL["blanco"]))
    else:
        parts.append(shape)
        if row["color_fondo"] == "azul" and row["color_simbolo"].startswith("negro"):
            # servicios: recuadro blanco con el símbolo negro (Manual p.146 y p.182)
            parts.append('<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" rx="2" fill="%s"/>'
                         % (cx - 14, cy - 16, 28, 28, COL["blanco"]))
            cy, tw, gs = cy - 2, 26, 0.62
        if kind == "TEXT":
            h = tw * 0.62 if row["forma"] not in ("rectangulo_horizontal", "flecha") else 22
            if row["forma"] == "triangulo_invertido":
                cy, tw, h = 17, 30, 20
            parts.append(multitext(g, cx, cy, tw, h, fg))
        elif kind == "SPECIAL" and g == "noentry":
            parts.append('<rect x="14" y="24" width="36" height="6" fill="%s"/>' % fg)
            parts.append(text("NO", 32, 21, 9, fg) + text("PASE", 32, 40, 9, fg))
        elif kind == "SPECIAL" and g == "bar":
            parts.append('<rect x="16" y="23" width="32" height="8" rx="3" fill="%s"/>' % fg)
        else:
            s = gs if gs else 0.7
            parts.append('<g transform="translate(%.2f %.2f) scale(%.3f)">%s</g>' % (cx - 20 * s, cy - 20 * s, s, g))
        if code in PROHIB:
            r = 17
            parts.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="4.5"/>'
                         % (cx - r * 0.72, cy - r * 0.72, cx + r * 0.72, cy + r * 0.72, COL["rojo"]))
        if row["color_simbolo"].startswith("gris"):
            for k in (-6, -2, 2, 6):
                parts.append('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="1.2"/>'
                             % (cx - 12 + k, cy + 12 + k, cx + 12 + k, cy - 12 + k, COL["negro"]))
    title = "%s %s" % (code, row["nombre"])
    label = ('<text x="32" y="62" font-family="Arial,Helvetica,sans-serif" font-size="8" font-weight="700" '
             'text-anchor="middle" fill="#222">%s</text>' % html.escape(code))
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img">'
            '<title>%s</title>%s%s</svg>\n' % (html.escape(title), "".join(parts), label))


def write_index(rows):
    fams = []
    for r in rows:
        if r["familia"] not in fams:
            fams.append(r["familia"])
    names = {"SR": "Reglamentarias (SR)", "SP": "Preventivas (SP)", "SI": "Informativas (SI)",
             "ST": "Informativas turísticas (ST)", "SIT": "Informativas de túnel (SIT)",
             "SIP": "Informativas para peatones (SIP)", "SRC": "Reglamentarias ciclo-infraestructura (SRC)",
             "SPC": "Preventivas ciclo-infraestructura (SPC)", "SIC": "Informativas ciclo-infraestructura (SIC)",
             "SRM": "Reglamentaria motociclistas (SRM)", "SIM": "Informativas motociclistas (SIM)",
             "SRO": "Reglamentarias en obra (SRO)", "SPO": "Preventivas de obra (SPO)",
             "SPPO": "Preventivas propias de obra (SPPO)", "SIO": "Informativas de obra (SIO)"}
    out = ["<!DOCTYPE html>", '<html lang="es"><head><meta charset="utf-8">',
           '<meta name="viewport" content="width=device-width, initial-scale=1">',
           "<title>Iconos de señales</title>",
           "<style>",
           ":root{--bg:#f6f6f4;--fg:#1d1d1b;--mut:#666;--card:#fff;--line:#ddd}",
           "@media (prefers-color-scheme: dark){:root{--bg:#1b1b1a;--fg:#eee;--mut:#aaa;--card:#2a2a28;--line:#444}}",
           "body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px/1.4 Arial,Helvetica,sans-serif}",
           "h1{font-size:20px;margin:0 0 4px}h2{font-size:16px;margin:28px 0 8px;border-bottom:1px solid var(--line);padding-bottom:4px}",
           "p{color:var(--mut);max-width:70ch}",
           ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(128px,1fr));gap:8px}",
           ".c{background:var(--card);border:1px solid var(--line);border-radius:6px;padding:8px;text-align:center}",
           ".c img{width:64px;height:64px;background:#e9e9e6;border-radius:4px}",
           ".k{font-weight:700;margin-top:4px}.n{font-size:11px;color:var(--mut)}",
           "</style></head><body>",
           "<h1>Señales verticales del Manual de Señalización Vial 2024</h1>",
           "<p>%d iconos esquemáticos dibujados por script a partir de <code>senales.csv</code>. "
           "No son reproducción del Manual: forma y colores sí, pictograma simplificado. "
           "La página citada es la del PDF del Manual (coincide con la impresa).</p>" % len(rows)]
    for fam in fams:
        fr = [r for r in rows if r["familia"] == fam]
        out.append("<h2>%s — %d</h2><div class=\"grid\">" % (html.escape(names.get(fam, fam)), len(fr)))
        for r in fr:
            out.append('<div class="c"><img src="%s" alt="%s"><div class="k">%s</div><div class="n">%s · p.%s</div></div>'
                       % (html.escape(r["icono"]), html.escape(r["codigo"]), html.escape(r["codigo"]),
                          html.escape(r["nombre"]), html.escape(r["pagina"])))
        out.append("</div>")
    out.append("</body></html>")
    with open(INDEX, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")


def main():
    with open(CSV_PATH, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    os.makedirs(OUT_DIR, exist_ok=True)
    for r in rows:
        with open(os.path.join(BASE, r["icono"]), "w", encoding="utf-8", newline="\n") as f:
            f.write(build_icon(r))
    write_index(rows)
    print("%d iconos generados en %s" % (len(rows), OUT_DIR))


if __name__ == "__main__":
    main()
