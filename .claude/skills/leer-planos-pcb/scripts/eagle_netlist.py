#!/usr/bin/env python3
"""Netlist de un diseno Eagle XML (.sch y .brd), con numero de linea en cada dato.

Deriva la conectividad por NOMBRE DE RED, que es como la guarda Eagle:
  .sch  <net name=..> / <segment> / <pinref part gate pin>   (pin -> pad por <connect> de la libreria)
  .brd  <signal name=..> / <contactref element pad>
Nunca por proximidad de coordenadas.

Uso (desde la raiz del repositorio):
  python eagle_netlist.py HW/Retro_smd_v1.sch --resumen
  python eagle_netlist.py HW/Retro_smd_v1.sch --parte JP1
  python eagle_netlist.py HW/Retro_smd_v1.sch --red MCLR
  python eagle_netlist.py HW/Retro_smd_v1.sch --parte MCU1 --pin-manager FW/mcc_generated_files
  python eagle_netlist.py HW/Retro_smd_v1.sch --cruce        # .sch contra .brd, pad a pad

El .brd se busca junto al .sch con el mismo nombre (o --brd RUTA).
Solo biblioteca estandar de Python.
"""
import argparse
import os
import sys
import xml.sax
from collections import defaultdict


class _EagleHandler(xml.sax.ContentHandler):
    """Recorre el XML guardando lo necesario y la linea de cada elemento."""

    def __init__(self):
        super().__init__()
        self.loc = None
        self.stack = []
        self.version = None
        # sch
        self.parts = {}          # name -> dict(library, urn, deviceset, device, value, line)
        self.lib_connects = {}   # (lib, urn, deviceset, device) -> {(gate, pin): [pads], ...}
        self.lib_package = {}    # (lib, urn, deviceset, device) -> package
        self.pinrefs = []        # (net, part, gate, pin, line)
        self.net_lines = {}      # net -> line
        # brd
        self.elements = {}       # name -> dict(package, value, line)
        self.contactrefs = []    # (signal, element, pad, line)
        self.signal_lines = {}
        # contexto
        self._lib = None
        self._urn = None
        self._ds = None
        self._dev = None
        self._net = None
        self._signal = None

    def setDocumentLocator(self, locator):
        self.loc = locator

    def _line(self):
        return self.loc.getLineNumber() if self.loc else 0

    def startElement(self, name, attrs):
        a = dict(attrs)
        parent = self.stack[-1] if self.stack else None
        self.stack.append(name)
        if name == 'eagle':
            self.version = a.get('version')
        elif name == 'library' and 'libraries' in self.stack[:-1]:
            self._lib = a.get('name')
            self._urn = a.get('urn', '')
        elif name == 'deviceset' and self._lib is not None:
            self._ds = a.get('name')
        elif name == 'device' and self._ds is not None and parent == 'devices':
            self._dev = a.get('name', '')
            key = (self._lib, self._urn, self._ds, self._dev)
            self.lib_connects.setdefault(key, {})
            self.lib_package[key] = a.get('package', '')
        elif name == 'connect' and self._dev is not None:
            key = (self._lib, self._urn, self._ds, self._dev)
            pads = a.get('pad', '').split()
            self.lib_connects[key][(a.get('gate'), a.get('pin'))] = pads
        elif name == 'part' and parent == 'parts':
            self.parts[a['name']] = dict(
                library=a.get('library'), urn=a.get('library_urn', ''),
                deviceset=a.get('deviceset'), device=a.get('device', ''),
                value=a.get('value', ''), line=self._line())
        elif name == 'net' and parent == 'nets':
            self._net = a.get('name')
            self.net_lines.setdefault(self._net, self._line())
        elif name == 'pinref' and self._net is not None:
            self.pinrefs.append((self._net, a.get('part'), a.get('gate'), a.get('pin'), self._line()))
        elif name == 'element' and parent == 'elements':
            self.elements[a['name']] = dict(package=a.get('package', ''), value=a.get('value', ''),
                                            library=a.get('library', ''), line=self._line())
        elif name == 'signal' and parent == 'signals':
            self._signal = a.get('name')
            self.signal_lines.setdefault(self._signal, self._line())
        elif name == 'contactref' and self._signal is not None:
            self.contactrefs.append((self._signal, a.get('element'), a.get('pad'), self._line()))

    def endElement(self, name):
        self.stack.pop()
        if name == 'library' and 'libraries' in self.stack:
            self._lib = self._urn = None
        elif name == 'deviceset':
            self._ds = None
        elif name == 'device':
            self._dev = None
        elif name == 'net':
            self._net = None
        elif name == 'signal':
            self._signal = None


def cargar(ruta):
    h = _EagleHandler()
    parser = xml.sax.make_parser()
    # El DTD de Eagle no viene con el fichero: no resolver entidades externas.
    parser.setFeature(xml.sax.handler.feature_external_ges, False)
    parser.setFeature(xml.sax.handler.feature_external_pes, False)
    parser.setContentHandler(h)
    parser.parse(ruta)
    return h


def _clave_lib(sch, parte):
    p = sch.parts[parte]
    exacta = (p['library'], p['urn'], p['deviceset'], p['device'])
    if exacta in sch.lib_connects:
        return exacta
    for k in sch.lib_connects:  # respaldo: mismo nombre de libreria sin urn
        if k[0] == p['library'] and k[2] == p['deviceset'] and k[3] == p['device']:
            return k
    return None


class Diseno:
    def __init__(self, ruta_sch, ruta_brd=None):
        self.ruta_sch = ruta_sch
        self.sch = cargar(ruta_sch)
        if ruta_brd is None:
            cand = os.path.splitext(ruta_sch)[0] + '.brd'
            ruta_brd = cand if os.path.exists(cand) else None
        self.ruta_brd = ruta_brd
        self.brd = cargar(ruta_brd) if ruta_brd else None
        self.avisos = []
        # (parte, pad) -> (red, pin, linea sch)
        self.sch_pad = {}
        self.sch_red = defaultdict(list)   # red -> [(parte, pin, [pads], linea)]
        for red, parte, gate, pin, linea in self.sch.pinrefs:
            pads = []
            if parte in self.sch.parts:
                k = _clave_lib(self.sch, parte)
                if k is not None:
                    pads = self.sch.lib_connects[k].get((gate, pin), [])
            self.sch_red[red].append((parte, pin, pads, linea))
            for pad in pads:
                self.sch_pad[(parte, pad)] = (red, pin, linea)
        self.brd_pad = {}
        self.brd_red = defaultdict(list)
        if self.brd:
            for red, elem, pad, linea in self.brd.contactrefs:
                self.brd_pad[(elem, pad)] = (red, linea)
                self.brd_red[red].append((elem, pad, linea))

    def pines_de(self, parte):
        """Todos los pines de la parte segun su libreria: [(gate, pin, [pads])]."""
        k = _clave_lib(self.sch, parte)
        if k is None:
            return []
        return [(g, p, pads) for (g, p), pads in self.sch.lib_connects[k].items()]

    def tiene_encapsulado(self, parte):
        k = _clave_lib(self.sch, parte)
        return bool(k and self.sch.lib_package.get(k))


# ---------------------------------------------------------------- salidas

def _fmt_miembros(d, miembros, excluir=None):
    out, simbolos = [], 0
    for parte, pin, pads, _l in miembros:
        if parte == excluir:
            continue
        if not d.tiene_encapsulado(parte):   # simbolo de alimentacion, marco...: no va a la placa
            simbolos += 1
            continue
        out.append(f"{parte}.{'/'.join(pads) or '?'}({pin})")
    txt = ', '.join(sorted(out))
    return txt + (f"  [+{simbolos} simbolos sin encapsulado]" if simbolos else '')


def _orden_pad(pad):
    return (0, int(pad), '') if pad.isdigit() else (1, 0, pad)


def informe_parte(d, parte, pm=None):
    if parte not in d.sch.parts:
        print(f"La parte {parte} no esta en {d.ruta_sch}. Negativo: comprobar con grep antes de afirmarlo.")
        return 1
    info = d.sch.parts[parte]
    print(f"## {parte}  ({info['deviceset']}{info['device']}, valor '{info['value']}')  sch:{info['line']}")
    if d.brd and parte in d.brd.elements:
        e = d.brd.elements[parte]
        print(f"   brd: element {parte} package {e['package']}  brd:{e['line']}")
    print()
    cols = ['Pad', 'Pin', 'Red (.sch)', 'linea sch', 'Red (.brd)', 'linea brd', 'Coincide']
    if pm is not None:
        cols += ['TRIS', 'ANSEL', 'WPU', 'PPS', 'Alias FW']
    print('| ' + ' | '.join(cols) + ' |')
    print('|' + '---|' * len(cols))
    filas = []
    for gate, pin, pads in d.pines_de(parte):
        for pad in pads:
            filas.append((pad, pin))
    for pad, pin in sorted(filas, key=lambda f: _orden_pad(f[0])):
        red_s, _p, l_s = d.sch_pad.get((parte, pad), ('-', None, '-'))
        red_b, l_b = d.brd_pad.get((parte, pad), ('-', '-')) if d.brd else ('(sin .brd)', '-')
        if not d.brd:
            ok = 'n/a'
        elif red_s == red_b:
            ok = 'si' if red_s != '-' else 'NC en ambos'
        else:
            ok = 'NO'
        fila = [pad, pin, red_s, str(l_s), red_b, str(l_b), ok]
        if pm is not None:
            fila += pm.columnas(pin)
        print('| ' + ' | '.join(fila) + ' |')
    print()
    print('Otros miembros de cada red (.sch):')
    vistas = set()
    for pad, pin in sorted(filas, key=lambda f: _orden_pad(f[0])):
        red = d.sch_pad.get((parte, pad), (None,))[0]
        if red and red not in vistas:
            vistas.add(red)
            print(f"  {red} (sch:{d.sch.net_lines.get(red)}): {_fmt_miembros(d, d.sch_red[red], excluir=parte)}")
    return 0


def informe_red(d, red):
    if red not in d.sch_red and red not in d.brd_red:
        print(f"La red '{red}' no aparece ni en .sch ni en .brd. Negativo: comprobar con grep antes de afirmarlo.")
        return 1
    print(f"## Red {red}")
    print(f"   .sch (net en linea {d.sch.net_lines.get(red, '-')}):")
    for parte, pin, pads, linea in sorted(d.sch_red.get(red, []), key=lambda m: (m[0], m[1])):
        print(f"     {parte}.{'/'.join(pads) or '?'}  pin '{pin}'  sch:{linea}")
    if d.brd:
        print(f"   .brd (signal en linea {d.brd.signal_lines.get(red, '-')}):")
        for elem, pad, linea in sorted(d.brd_red.get(red, [])):
            print(f"     {elem}.{pad}  brd:{linea}")
    return 0


def informe_cruce(d):
    if not d.brd:
        print('Sin .brd: no hay cruce posible.')
        return 1
    difs = []
    for (parte, pad), (red_s, pin, l_s) in d.sch_pad.items():
        red_b, l_b = d.brd_pad.get((parte, pad), (None, None))
        if red_b != red_s:
            difs.append((parte, pad, pin, red_s, f'sch:{l_s}', red_b or '-', f'brd:{l_b}' if l_b else '-'))
    for (elem, pad), (red_b, l_b) in d.brd_pad.items():
        if (elem, pad) not in d.sch_pad:
            difs.append((elem, pad, '?', '-', '-', red_b, f'brd:{l_b}'))
    print(f"Cruce .sch <-> .brd: {len(d.sch_pad)} pads con red en .sch, {len(d.brd_pad)} contactref en .brd, "
          f"{len(difs)} diferencias.")
    if difs:
        print('| Parte | Pad | Pin | Red sch | linea | Red brd | linea |')
        print('|---|---|---|---|---|---|---|')
        for f in sorted(difs, key=lambda f: (f[0], _orden_pad(f[1]))):
            print('| ' + ' | '.join(map(str, f)) + ' |')
        print('\nUn pad presente en .brd y ausente en .sch suele ser un pin de alimentacion implicito '
              '(gate de potencia no invocado). No es un error por si mismo: citarlo como tal.')
    return 0


def informe_resumen(d):
    s = d.sch
    print(f"Fichero: {d.ruta_sch}  (Eagle {s.version})")
    print(f"  partes: {len(s.parts)}  (con encapsulado: {sum(1 for p in s.parts if d.tiene_encapsulado(p))})")
    print(f"  redes: {len(d.sch_red)}  pinref: {len(s.pinrefs)}")
    if d.brd:
        print(f"Fichero: {d.ruta_brd}  (Eagle {d.brd.version})")
        print(f"  elements: {len(d.brd.elements)}  signals: {len(d.brd_red)}  contactref: {len(d.brd.contactrefs)}")
        solo_sch = sorted(p for p in s.parts if d.tiene_encapsulado(p) and p not in d.brd.elements)
        solo_brd = sorted(e for e in d.brd.elements if e not in s.parts)
        print(f"  partes con encapsulado en .sch y no en .brd: {solo_sch or 'ninguna'}")
        print(f"  elements en .brd y no en .sch: {solo_brd or 'ninguno'}")
    sin_pad = [(r, p, pin, l) for r, m in d.sch_red.items() for (p, pin, pads, l) in m
               if not pads and d.tiene_encapsulado(p)]
    if sin_pad:
        print(f"  AVISO: {len(sin_pad)} pinref sin pad resuelto (libreria no encontrada):")
        for r, p, pin, l in sin_pad[:20]:
            print(f"    {p}.{pin} en {r}  sch:{l}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('sch')
    ap.add_argument('--brd')
    ap.add_argument('--parte', action='append', default=[])
    ap.add_argument('--red', action='append', default=[])
    ap.add_argument('--cruce', action='store_true')
    ap.add_argument('--resumen', action='store_true')
    ap.add_argument('--pin-manager', help='carpeta mcc_generated_files para cruzar pines del MCU')
    a = ap.parse_args(argv)

    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    d = Diseno(a.sch, a.brd)
    pm = None
    if a.pin_manager:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        from pin_manager_pic import PinManager
        pm = PinManager(a.pin_manager)
    rc = 0
    if a.resumen or not (a.parte or a.red or a.cruce):
        rc |= informe_resumen(d)
    for p in a.parte:
        print()
        rc |= informe_parte(d, p, pm)
    for r in a.red:
        print()
        rc |= informe_red(d, r)
    if a.cruce:
        print()
        rc |= informe_cruce(d)
    return rc


if __name__ == '__main__':
    sys.exit(main())
