# Genera los arneses de simulador de T-A23 (modo 1) y T-A30 (modo 2) para la V3.6.1.
# El fuente real calibracion_v36.c se incluye tal cual.
import random, struct, json
import numpy as np

random.seed(20260920)
FW = r"D:/IT/P_RetroVertical_V3.6/01_Firmware/RetroVertical_V3.6.X"

fab = [
    (0.0, -0.000086541, 0.619668128, -303), (-0.000000073, 0.000282508, 0.124075350, -130),
    (0.000000163, -0.000756695, 1.213142724, -442), (0.000000147, -0.000668624, 1.082394050, -393),
    (0.000000026, -0.000018402, 0.102152670, -49), (0.0, 0.000090731, 0.001064329, -21),
    (0.0, 0.000087404, 0.013617682, -27), (0.000000086, -0.000299026, 0.446572329, -161),
    (0.000000163, -0.000756695, 1.213142724, -442), (0.0, 0.000113098, -0.076130418, 10),
    (0.000000026, -0.000018402, 0.102152670, -49), (0.0, 0.000090731, 0.001064329, -21)]
temp = (0.0, 0.00043212, 0.90148651)

def txt(v):  # lo que manda la app: String.format(Locale.US, "%.8E", double)
    return "%.8E" % v

# ---- T-A23: valores de entrada (texto de la app) ----
vals = [c for row in fab for c in row] + list(temp)          # 51, 35 distintos (se repiten en el lote grande)
rnd = []
for i in range(2016 - 51):
    g = i % 3
    if g == 0:   # c2
        rnd.append(random.uniform(-1e-3, 1e-3) if i % 2 == 0 else
                   random.choice((-1, 1)) * 10 ** random.uniform(-7, -3))
    elif g == 1:  # c1
        rnd.append(random.uniform(0, 2))
    else:         # c0
        rnd.append(random.uniform(-1000, 1000))
vals += rnd
json.dump({'vals': vals, 'nfab': 51}, open('valsX.json', 'w'))

def cstr(s):
    return '"' + s + '"'

def f32bits(v):
    return struct.unpack('<I', struct.pack('<f', v))[0]

# ---- T-A30 / #S: curvas aleatorias para comparar la decision con un barrido fuera ----
curvas = []
for i in range(120):
    g = i % 3
    if g == 0:   # grado 1
        c = (0.0, 0.0, random.uniform(0, 1.2), random.uniform(-500, 300))
    elif g == 1:  # grado 2
        c = (0.0, random.uniform(-3e-4, 3e-4), random.uniform(-0.5, 2), random.uniform(-800, 300))
    else:         # grado 3
        c = (random.uniform(-2e-7, 2e-7), random.uniform(-8e-4, 8e-4), random.uniform(-0.5, 2), random.uniform(-600, 300))
    curvas.append(c)
temps = []
for i in range(120):
    temps.append((random.uniform(-4e-6, 4e-6), random.uniform(-1.5e-3, 1.5e-3), random.uniform(0.4, 1.6)))
json.dump({'curvas': curvas, 'temps': temps}, open('curvasX.json', 'w'))

tramas = [
    "#V#",
    "#ST,0,0,1#",                                           # sin sesion
    "#L,2026#",
    # T-A30 (TDD-V3.6 T-A30, cinco pasos)
    "#ST,0.00000000E+00,4.32119996E-04,9.01486516E-01#",    # dentro -> OK
    "#ST,0,4.3E-04,4.9E-01#",                               # X_0 fuera
    "#ST,0,1.0E-03,9.0E-01#",                               # X_1 extremo (F(831)=1,731)
    "#ST,0,-5.0E-04,9.0E-01#",                              # X_1 extremo negativo (F(831)=0,4845)
    "#ST,1.0E-06,0,9.0E-01#",                               # X_2 extremo (F(831)=1,5906)
    # vertice
    "#ST,-2.0E-06,1.7E-03,9.0E-01#",                        # vertice 1,261 -> OK
    "#ST,-4.0E-06,3.4E-03,9.0E-01#",                        # solo el vertice (1,6225) falla
    "#ST,0,0,1.5#",                                         # borde -> OK
    "#ST,0,0,1.6#",
    "#ST,0,0,nan#",
    "#ST,0.00000000E+00,4.32119996E-04,9.01486516E-01#",    # vuelve a fabrica
    "#GT#",
    # #S
    "#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#",  # TDD T-C23 -> OK
    "#E,1,1000#",
    "#S,2,-7.30000025E-08,2.82508001E-04,1.24075353E-01,-1.30000000E+02#",   # fabrica '2': negativa desde x=4175
    "#S,3,1.62999996E-07,-7.56694993E-04,1.21314275E+00,-4.42000000E+02#",   # fabrica '3': max 3743 -> OK
    "#S,1,0,0,1,0#",                                        # R = x: 4300 > 4000
    "#S,1,0,0,0.9,0#",                                      # 3870 -> OK
    "#S,1,0,0,1,-700#",                                     # R(600) = -100
    "#S,1,0,-1.0E-03,5.0,-2000#",                           # maximo interior 4250
    "#S,1,0,1.0E-03,-5.0,6500#",                            # minimo interior 250 -> OK
    "#S,1,0,1.0E-03,-5.0,6200#",                            # minimo interior -50
    "#S,1,1.0E-07,-1.0E-03,3.0,-1000#",                     # cubica, maximo local 1827 -> OK
    "#S,1,1.0E-07,-1.0E-03,3.0,1300#",                      # cubica, maximo local 4127
    "#S,1,1.0E+30,0,0,0#",                                  # desborda
    "#S,1,0,0,1.0E+38,0#",
    "#S,1,nan,0,0,0#",
    "#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#",
    "#G,1#",
    "#F,*#",
    "#V#",
]

def gen(modo):
    L = []
    L.append('/* Arnes T-A23 (modo 1) / T-A30 (modo 2), V3.6.1. Generado por gen.py. */')
    L.append('#include <xc.h>\n#include <stdint.h>\n#include <string.h>\n#include <stdlib.h>\n#include <stdio.h>')
    L.append('#pragma config WDTE = OFF')
    L.append('unsigned int reflectivityValue;')
    L.append('double X_2 = 0.0;\ndouble X_1 =  0.00043212;\ndouble X_0 = 0.90148651;   /* gui.c:42-44 */')
    L.append('void arreglar_dato(void){ if ((reflectivityValue > 4000 )) { reflectivityValue = 0; } }')
    L.append('char salida[200];\nunsigned int nSal;')
    L.append('void UART1_Write(uint8_t b) { if (nSal < sizeof(salida) - 1) salida[nSal++] = (char)b; salida[nSal] = 0; }')
    L.append('void sendUartStr(char *s) { while (*s) UART1_Write((uint8_t)*s++); }')
    L.append('unsigned long msSim;\nunsigned long getMillis(void) { return msSim; }')
    L.append('unsigned char ee[1024];')
    L.append('uint8_t DATAEE_ReadByte(uint16_t a) { return ee[a & 0x3FF]; }')
    L.append('void DATAEE_WriteByte(uint16_t a, uint8_t d) { ee[a & 0x3FF] = d; }')
    L.append('#include "calibracion_v36.c"')
    L.append('union U { float f; uint32_t u; };')
    L.append('volatile unsigned char fin;')
    if modo == 1:
        n = len(vals)
        L.append('#define NA %d' % n)
        L.append('const char * const txtA[NA] = {' + ','.join(cstr(txt(v)) for v in vals) + '};')
        L.append('#define NP (HASTA - DESDE)\nuint32_t bitsA[NP];\nchar impA[NP*16];\nchar impF[51*16];\nunsigned char malos;')
        L.append('''static void copiar(char *d) { unsigned char j; for (j = 0; j < 15; j++) d[j] = salida[j]; d[15] = 0; }
void main(void) {
    unsigned int i; double v; union FloatBytes fb; union U u;
    memset(ee, 0xFF, sizeof(ee));
    calibracionIniciar();
    /* #G / #GT: impresion de los valores de ROM (coefFabrica y temperatura de gui.c) */
    for (i = 0; i < 48; i++) { nSal = 0; enviarNumero(coefFabrica[i / 4][i % 4]); copiar(&impF[i * 16]); }
    for (i = 0; i < 3; i++) { nSal = 0; enviarNumero(tempFabrica[i]); copiar(&impF[(48 + i) * 16]); }
    /* #S -> #G: texto de la app -> leerNumero (strtod) -> float -> enviarNumero (%.8E) */
    for (i = DESDE; i < HASTA; i++) {
        if (!leerNumero(txtA[i], &v)) { malos++; bitsA[i - DESDE] = 0xFFFFFFFFUL; continue; }
        fb.valor = v; memcpy(&u.u, fb.b, 4); bitsA[i - DESDE] = u.u;
        nSal = 0; enviarNumero(v); copiar(&impA[(i - DESDE) * 16]);
    }
    fin = 1;
    while (1) { NOP(); }
}''')
    else:
        L.append('#define NT %d' % len(tramas))
        L.append('const char * const tramas[NT] = {' + ','.join(cstr(t) for t in tramas) + '};')
        L.append('#define NC %d' % len(curvas))
        L.append('const uint32_t curvasB[NC][4] = {' + ','.join('{' + ','.join('0x%08XUL' % f32bits(x) for x in c) + '}' for c in curvas) + '};')
        L.append('const uint32_t tempsB[NC][3] = {' + ','.join('{' + ','.join('0x%08XUL' % f32bits(x) for x in c) + '}' for c in temps) + '};')
        L.append('char registro[1500];\nunsigned int nReg;\nunsigned char decC[NC];\nunsigned char decT[NC];\nstatic char trama[100];')
        L.append('''void main(void) {
    unsigned int i, j; unsigned char g; double c[4]; union FloatBytes fb; uint32_t w;
    memset(ee, 0xFF, sizeof(ee));
    calibracionIniciar();
    for (i = 0; i < NT; i++) {
        strcpy(trama, tramas[i]);
        nSal = 0; salida[0] = 0;
        adminProcesarTrama(trama, (unsigned char)strlen(trama));
        for (j = 0; salida[j] && nReg < sizeof(registro) - 2; j++) registro[nReg++] = salida[j];
        registro[nReg++] = '|';
    }
    for (i = 0; i < NC; i++) {
        for (g = 0; g < 4; g++) { w = curvasB[i][g]; memcpy(fb.b, &w, 4); c[g] = fb.valor; }
        decC[i] = curvaValida(c);
        for (g = 0; g < 3; g++) { w = tempsB[i][g]; memcpy(fb.b, &w, 4); c[g] = fb.valor; }
        decT[i] = temperaturaValida(c);
    }
    fin = 1;
    while (1) { NOP(); }
}''')
    src = '\n'.join(L) + '\n'
    open('hX%d.c' % modo, 'w').write(src)
    ln = [k + 1 for k, l in enumerate(src.split('\n')) if 'fin = 1;' in l][0]
    return ln

print(gen(1))
