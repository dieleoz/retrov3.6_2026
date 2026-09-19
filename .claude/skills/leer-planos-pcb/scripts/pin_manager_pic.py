#!/usr/bin/env python3
"""Configuracion de pines de un PIC18 (MCC) leida de pin_manager.c/.h, con archivo:linea.

Decodifica, bit a bit, los registros que MCC escribe en PIN_MANAGER_Initialize():
  TRISx (1 = entrada), ANSELx (1 = analogico), WPUx (pull-up), ODCONx (drenador abierto), LATx
y las asignaciones PPS:
  RxyPPS = 0xNN   -> salida de un periferico por el pin Rxy (codigo de funcion; el comentario de MCC lo nombra)
  <fn>PPS = 0xNN  -> entrada de un periferico desde el pin: puerto = (NN >> 3) & 7 (A..E), bit = NN & 7
y los alias de pin_manager.h (#define NOMBRE_TRIS TRISxbits.TRISxn).

Solo dice lo que el firmware CONFIGURA. Que la placa lo conecte es otra cuestion (eagle_netlist.py).

Uso:
  python pin_manager_pic.py FW/mcc_generated_files            # tabla de los 36 pines de puerto
  python pin_manager_pic.py FW/mcc_generated_files --pin RC4
"""
import argparse
import os
import re
import sys

PUERTOS = 'ABCDE'
_RE_REG = re.compile(r'^\s*(TRIS|ANSEL|WPU|ODCON|LAT)([A-E])\s*=\s*(0x[0-9A-Fa-f]+|\d+)\s*;')
_RE_PPS_OUT = re.compile(r'^\s*R([A-E])([0-7])PPS\s*=\s*(0x[0-9A-Fa-f]+|\d+)\s*;\s*(?://\s*(.*))?')
_RE_PPS_IN = re.compile(r'^\s*(\w+?)PPS\s*=\s*(0x[0-9A-Fa-f]+|\d+)\s*;\s*(?://\s*(.*))?')
_RE_ALIAS = re.compile(r'^\s*#define\s+(\w+)_TRIS\s+TRIS([A-E])bits\.TRIS[A-E]([0-7])')
_RE_PIN = re.compile(r'R([A-E])([0-7])')


class PinManager:
    def __init__(self, carpeta):
        self.c = os.path.join(carpeta, 'pin_manager.c')
        self.h = os.path.join(carpeta, 'pin_manager.h')
        self.reg = {}      # (tipo, puerto) -> (valor, linea)
        self.pps = {}      # 'RC4' -> [(texto, 'pin_manager.c:N')]
        self.alias = {}    # 'RA0' -> [(nombre, 'pin_manager.h:N')]
        self._leer_c()
        if os.path.exists(self.h):
            self._leer_h()

    def _leer_c(self):
        with open(self.c, encoding='utf-8', errors='replace') as f:
            for n, linea in enumerate(f, 1):
                m = _RE_REG.match(linea)
                if m:
                    self.reg[(m.group(1), m.group(2))] = (int(m.group(3), 0), n)
                    continue
                m = _RE_PPS_OUT.match(linea)
                if m:
                    pin = f'R{m.group(1)}{m.group(2)}'
                    txt = (m.group(4) or f'salida PPS {m.group(3)}').strip()
                    self.pps.setdefault(pin, []).append((f'OUT {txt}', f'pin_manager.c:{n}'))
                    continue
                m = _RE_PPS_IN.match(linea)
                if m and m.group(1) not in ('', 'PPSLOCK'):
                    v = int(m.group(2), 0)
                    puerto = (v >> 3) & 7
                    if puerto < len(PUERTOS):
                        pin = f'R{PUERTOS[puerto]}{v & 7}'
                        txt = (m.group(3) or f'{m.group(1)} <- {pin}').strip()
                        self.pps.setdefault(pin, []).append((f'IN {m.group(1)}PPS ({txt})', f'pin_manager.c:{n}'))

    def _leer_h(self):
        with open(self.h, encoding='utf-8', errors='replace') as f:
            for n, linea in enumerate(f, 1):
                m = _RE_ALIAS.match(linea)
                if m:
                    self.alias.setdefault(f'R{m.group(2)}{m.group(3)}', []).append((m.group(1), f'pin_manager.h:{n}'))

    def bit(self, tipo, puerto, b):
        if (tipo, puerto) not in self.reg:
            return None, None
        v, n = self.reg[(tipo, puerto)]
        return (v >> b) & 1, n

    def columnas(self, nombre_pin):
        """[TRIS, ANSEL, WPU, PPS, Alias] para un nombre de pin de esquematico ('RC4', 'RB6/PGC'...)."""
        m = _RE_PIN.search(nombre_pin or '')
        if not m:
            return ['-', '-', '-', '-', '-']
        p, b = m.group(1), int(m.group(2))
        pin = f'R{p}{b}'
        out = []
        for tipo, uno, cero in (('TRIS', 'in', 'out'), ('ANSEL', 'ana', 'dig'), ('WPU', 'pu', '-')):
            v, n = self.bit(tipo, p, b)
            out.append('?' if v is None else f"{uno if v else cero} (:{n})")
        out.append('; '.join(f'{t} ({l})' for t, l in self.pps.get(pin, [])) or '-')
        out.append('; '.join(f'{a} ({l})' for a, l in self.alias.get(pin, [])) or '-')
        return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('carpeta', help='mcc_generated_files')
    ap.add_argument('--pin', action='append', default=[])
    a = ap.parse_args(argv)
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    pm = PinManager(a.carpeta)
    pines = a.pin or [f'R{p}{b}' for p in PUERTOS for b in range(8) if not (p == 'E' and b > 3)]
    print('Lineas citadas: pin_manager.c salvo que se indique .h')
    print('| Pin | TRIS | ANSEL | WPU | PPS | Alias FW |')
    print('|---|---|---|---|---|---|')
    for pin in pines:
        print('| ' + ' | '.join([pin] + pm.columnas(pin)) + ' |')
    return 0


if __name__ == '__main__':
    sys.exit(main())
