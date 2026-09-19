# Genera harness_eq.c: funciones de 2020 copiadas literalmente de la base (lineas 3-42)
# frente a coefFabrica + aplicarEcuacion copiados literalmente del fuente V3.6.
import re, sys
base = open(r"D:/IT/P_RetroVertical_V3.6/01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c", 'rb').read().decode('latin-1')
new = open(r"D:/IT/P_RetroVertical_V3.6/01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c", 'rb').read().decode('latin-1')
orig = '\n'.join(base.replace('\r\n', '\n').split('\n')[2:42])
names = re.findall(r'void (\w+) ?\(void\)', orig)
for n in names:
    orig = re.sub(r'void ' + n + r' ?\(void\)', 'void o_' + n + '(void)', orig)
tabla = re.search(r'static const double coefFabrica\[EC_NUM\]\[4\] = \{.*?\n\};', new, re.S).group(0)
apl = re.search(r'void aplicarEcuacion\(unsigned char k\) \{.*?\n\}', new, re.S).group(0)
order = ['blancoIntenso', 'amarilloIntenso', 'verdeIntenso', 'rojoIntenso', 'azulIntenso', 'naranjaIntenso',
         'blancoOpaco', 'amarilloOpaco', 'verdeOpaco', 'rojoOpaco', 'azulOpaco', 'naranjaOpaco']
assert sorted(order) == sorted(names), names
src = '''#include <xc.h>
#pragma config WDTE = OFF
#define EC_NUM 12
unsigned int reflectivityValue;
''' + orig + '\n' + tabla + '\ndouble coefCal[EC_NUM][4];\n' + apl + '''
typedef void (*fn_t)(void);
const fn_t orig[EC_NUM] = {''' + ','.join('o_' + n for n in order) + '''};
volatile unsigned long nComparados = 0, nDistintos = 0, suma = 0;
volatile unsigned int primerX = 0xFFFF; volatile unsigned char primerK = 0xFF;
void main(void){
    unsigned char k, i; unsigned int x, r1, r2;
    for (k = 0; k < EC_NUM; k++) for (i = 0; i < 4; i++) coefCal[k][i] = coefFabrica[k][i];
    for (k = K_DESDE; k <= K_HASTA; k++) {
        x = 0;
        do {
            reflectivityValue = x; orig[k](); r1 = reflectivityValue;
            reflectivityValue = x; aplicarEcuacion(k); r2 = reflectivityValue;
            nComparados++; suma += r1;
            if (r1 != r2) { nDistintos++; if (primerK == 0xFF) { primerK = k; primerX = x; } }
            x++;
        } while (x != 0);
    }
    NOP();
    while (1) { NOP(); }
}
'''
open('harness_eq.c', 'w').write(src)
print('ok', len(src))
