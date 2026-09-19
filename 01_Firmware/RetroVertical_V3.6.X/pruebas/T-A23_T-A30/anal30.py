import re, json, struct
import numpy as np
exec(open('anal23.py').read().split('vals = json.load')[0])
o = open('m2/out.txt', encoding='latin-1').read()
n = bloque(o, 'nReg')[0]
reg = bytes(bloque(o, 'registro')[:n]).decode('ascii').split('|')
src = open('gen.py', encoding='utf-8').read()
tr = re.findall(r'^    "(#[^"]*)",', src, re.M)
for t, r in zip(tr, reg): print('%-72s -> %s' % (t, r))
decC = bloque(o, 'decC'); decT = bloque(o, 'decT')
cj = json.load(open('curvas.json'))
x = np.arange(600, 4301, dtype=np.float32)
dis = 0; acc = 0
for c, d in zip(cj['curvas'], decC):
    c = [np.float32(v) for v in c]
    r = c[0]*x*x*x + c[1]*x*x + c[2]*x + c[3]
    ok = int(bool(np.all(np.isfinite(r)) and r.min() >= 0 and r.max() <= 4000))
    acc += d
    if ok != d: dis += 1; print('DISCREPA curva', c, d, ok, r.min(), r.max())
print('curvas: firmware acepta %d/%d; discrepancias con barrido x entero 600-4300 en float32: %d' % (acc, len(decC), dis))
T = np.linspace(0, 831, 831*20+1).astype(np.float32)
dis = 0; acc = 0
for c, d in zip(cj['temps'], decT):
    c = [np.float32(v) for v in c]
    f = c[0]*T*T + c[1]*T + c[2]
    ok = int(bool(f.min() >= 0.5 and f.max() <= 1.5))
    acc += d
    if ok != d: dis += 1; print('DISCREPA temp', c, d, ok, f.min(), f.max())
print('temperatura: firmware acepta %d/%d; discrepancias con barrido T paso 0,05: %d' % (acc, len(decT), dis))
