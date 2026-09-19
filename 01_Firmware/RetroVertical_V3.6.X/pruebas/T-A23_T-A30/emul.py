# Emulacion en float32 IEEE (redondeo al par) de strtof (SMALLCODE) y de efgtoa('E', prec 8)
# de XC8 v2.10 (pic/sources/c99/common/strtof.c y doprnt.c). Se valida contra el simulador:
# 2267/2267 iguales en strtod y en %.8E (lotes 1 y 2). Operaciones en float32 al par, salvo la
# conversion uint32 -> float, que en XC8 redondea el medio hacia arriba (con redondeo al par
# discrepan 26 de 2267; con truncado, 695).
import numpy as np
F = np.float32

def u2f(v):
    n = v.bit_length()
    if n <= 24: return F(v)
    sh = n - 24
    return F(((v + (1 << (sh - 1))) >> sh) << sh)

def strtof(s):
    neg = False
    i = 0
    if s[i] == '-': neg = True; i += 1
    elif s[i] == '+': i += 1
    v = 0; eexp = 0; expon = 0; dot = False
    while i < len(s):
        ch = s[i]
        if not dot and ch == '.': dot = True; i += 1; continue
        if not ch.isdigit(): break
        if eexp != 9:
            if dot: expon -= 1
            eexp += 1
            v = (v * 10 + int(ch)) & 0xFFFFFFFF
        elif not dot:
            expon += 1
        i += 1
    ue = 0
    if i < len(s) and s[i] in 'eE':
        i += 1; en = False
        if s[i] == '-': en = True; i += 1
        elif s[i] == '+': i += 1
        while i < len(s) and s[i] == '0': i += 1
        digs = ''
        while i < len(s) and s[i].isdigit() and len(digs) < 3: digs += s[i]; i += 1
        ue = int(digs) if digs else 0
        if en: ue = -ue
    expon += ue
    l = u2f(v)                     # (float)v de uint32 en XC8: medio hacia arriba
    if l == 0: return F(0)
    if expon < 0:
        expon = -expon
        while expon >= 10: l = F(l / F(1e10)); expon -= 10
        while expon != 0: l = F(l / F(10.0)); expon -= 1
    elif expon > 0:
        while expon >= 10: l = F(l * F(1e10)); expon -= 10
        while expon != 0: l = F(l * F(10.0)); expon -= 1
    return F(-l) if neg else l

def efmt(f, prec=8):
    g = F(f); sign = g < 0
    if sign: g = F(-g)
    u = F(1.0); e = 0
    if not (g == 0):
        while not (g < F(u * F(10.0))):
            u = F(u * F(10.0)); e += 1
        while g < u:
            u = F(u / F(10.0)); e -= 1
    m = prec + 1
    i = 0; h = g; ou = u; d = 0
    while i < m:
        l = F(np.floor(F(h / u))); d = int(l)
        h = F(h - F(l * u)); u = F(u / F(10.0)); i += 1
    l = F(u * F(5.0))
    if h < l: l = F(0.0)
    elif h == l and not (d % 2): l = F(0.0)
    h = F(g + l)
    u = ou; i = 0; out = ''
    while i < m:
        l = F(np.floor(F(h / u))); d = int(l)
        out += chr(ord('0') + d)
        if i == 0: out += '.'
        h = F(h - F(l * u)); u = F(u / F(10.0)); i += 1
    es = '-' if e < 0 else '+'
    return ('-' if sign else '') + out + 'E' + es + '%02d' % abs(e)
