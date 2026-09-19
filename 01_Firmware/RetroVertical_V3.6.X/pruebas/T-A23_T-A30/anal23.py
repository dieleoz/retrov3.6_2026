import sys
if len(sys.argv) > 1:
    VALS = 'valsX.json'; DIRS = [('x%d' % r, 126 * r) for r in range(16)]
else:
    VALS = 'vals.json'; DIRS = [('m1a', 0), ('m1b', 126)]
# Analisis de T-A23: lee la salida de mdb y mide los errores en ulp de float32.
import re, json, struct, sys
from fractions import Fraction
import numpy as np

def bloque(txt, nombre):
    i = txt.index('\n' + nombre + '=')
    j = txt.find('\nprint ', i + 1)
    if j < 0: j = txt.find('\nquit', i + 1)
    return [int(x) for x in re.findall(r'-?\d+', txt[i + len(nombre) + 2:j])]

def cadenas(bs):
    out = []
    for k in range(0, len(bs), 16):
        out.append(bytes(b for b in bs[k:k + 16] if b).decode('ascii'))
    return out

def f32(x):
    return np.float32(x)

def bits2f(u):
    return np.frombuffer(struct.pack('<I', u), dtype=np.float32)[0]

def f2bits(f):
    return struct.unpack('<I', np.float32(f).tobytes())[0]

def correcto(s):
    """float32 correctamente redondeado del texto decimal s (exacto, con Fraction)."""
    q = Fraction(s)
    c = f32(float(q))
    best = None
    for d in (-2, -1, 0, 1, 2):
        u = f2bits(c)
        if c == 0: cand = c
        else:
            cand = bits2f(u + d) if (u + d) >= 0 else c
        e = abs(Fraction(float(cand)) - q)
        if best is None or e < best[0] or (e == best[0] and (f2bits(cand) & 1) == 0):
            best = (e, cand)
    return best[1]

def ulp_java(f):  # Math.ulp(float)
    f = abs(np.float32(f))
    return float(np.nextafter(f, np.float32(np.inf)) - f)

def dist_ulp(a, b):  # distancia en pasos de float32 (mismo signo)
    ia = f2bits(a); ib = f2bits(b)
    sa = -(ia & 0x7FFFFFFF) if ia >> 31 else ia & 0x7FFFFFFF
    sb = -(ib & 0x7FFFFFFF) if ib >> 31 else ib & 0x7FFFFFFF
    return abs(sa - sb)

vals = json.load(open(VALS))['vals']
txtA = ['%.8E' % v for v in vals]
res = []
impF = None
for d, lo in DIRS:
    o = open(d + '/out.txt', encoding='latin-1').read()
    assert bloque(o, 'malos') == [0], d
    b = bloque(o, 'bitsA'); s = cadenas(bloque(o, 'impA'))
    impF = cadenas(bloque(o, 'impF'))
    for k in range(len(b)):
        res.append((lo + k, b[k], s[k]))
assert len(res) == len(vals)

hist_strtod = {}; hist_printf = {}; hist_app = {}; peor = []
for i, bits, impreso in res:
    a = txtA[i]
    ffw = bits2f(bits)
    ok = correcto(a)
    e1 = dist_ulp(ffw, ok)                       # strtod de XC8
    e2 = dist_ulp(correcto(impreso), ffw)        # %.8E de XC8 (texto -> float exacto)
    enviada = f32(float(a)); leida = f32(float(impreso))   # lo que hace la app
    e3 = abs(float(leida) - float(enviada)) / ulp_java(leida)
    hist_strtod[e1] = hist_strtod.get(e1, 0) + 1
    hist_printf[e2] = hist_printf.get(e2, 0) + 1
    k3 = round(e3, 3); hist_app[k3] = hist_app.get(k3, 0) + 1
    peor.append((e3, i, a, impreso, e1, e2))
print('N =', len(res))
print('strtod (ulp frente al correcto):', dict(sorted(hist_strtod.items())))
print('%.8E  (ulp):', dict(sorted(hist_printf.items())))
print('ida y vuelta, metrica de la app |leida-enviada|/Math.ulp(leida):', dict(sorted(hist_app.items())))
peor.sort(reverse=True)
for p in peor[:8]: print('  ', p)
# fabrica #G / #GT
fab = vals[:51]
hg = {}
for i in range(51):
    rom = f32(float(fab[i]))    # el compilador redondea el literal (T-A20 cuadra con esta emulacion)
    leida = f32(float(impF[i]))
    e = abs(float(leida) - float(rom)) / ulp_java(leida) if leida != 0 else abs(float(leida) - float(rom))
    hg[round(e, 3)] = hg.get(round(e, 3), 0) + 1
    if e > 0: pass
print('#G de fabrica, metrica de la app:', dict(sorted(hg.items())))
print('distintos de fabrica:', len(set(fab)))
