/* Prueba de #FT# (V3.6.2) con el calibracion_v36.c real. */
#include <xc.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#pragma config WDTE = OFF
unsigned int reflectivityValue;
double X_2 = 0.0;
double X_1 =  0.00043212;
double X_0 = 0.90148651;   /* gui.c:42-44 */
void arreglar_dato(void){ if ((reflectivityValue > 4000 )) { reflectivityValue = 0; } }
char salida[200];
unsigned int nSal;
void UART1_Write(uint8_t b) { if (nSal < sizeof(salida) - 1) salida[nSal++] = (char)b; salida[nSal] = 0; }
void sendUartStr(char *s) { while (*s) UART1_Write((uint8_t)*s++); }
unsigned long msSim;
unsigned long getMillis(void) { return msSim; }
unsigned char ee[1024];
uint8_t DATAEE_ReadByte(uint16_t a) { return ee[a & 0x3FF]; }
void DATAEE_WriteByte(uint16_t a, uint8_t d) { ee[a & 0x3FF] = d; }
#include "calibracion_v36.c"
const char * const tramas[] = {
    "#V#", "#FT#", "#L,2026#",
    "#ST,0,5.0E-04,9.5E-01#", "#GT#", "#V#",
    "#FT,1#", "#FT#", "#GT#", "#V#",
    "#P,2026,1234#", "#ST,0,5.0E-04,9.5E-01#", "#FT#", "#V#", "#Q#", "#FT#",
    0 };
const char * const tramas2[] = { "#V#", "#GT#", "#L,1234#", "#OK?#", 0 };
char registro[900];
unsigned int nReg;
static char trama[100];
volatile unsigned char difRom, difRom2, fin;
static void enviar(const char * const *t) {
    unsigned int i, j;
    for (i = 0; t[i]; i++) {
        strcpy(trama, t[i]); nSal = 0; salida[0] = 0;
        adminProcesarTrama(trama, (unsigned char)strlen(trama));
        for (j = 0; salida[j] && nReg < sizeof(registro) - 2; j++) registro[nReg++] = salida[j];
        registro[nReg++] = '|';
    }
}
static unsigned char comparaRom(void) {
    union FloatBytes a; unsigned char i, n = 0;
    const double rom[3] = {0.0, 0.00043212, 0.90148651};   /* literales de gui.c:42-44 */
    for (i = 0; i < 3; i++) {
        double x = (i == 0) ? X_2 : ((i == 1) ? X_1 : X_0);
        a.valor = x; { union FloatBytes b; b.valor = rom[i]; if (memcmp(a.b, b.b, 4)) n++; }
    }
    return n;
}
void main(void) {
    memset(ee, 0xFF, sizeof(ee));
    calibracionIniciar();
    enviar(tramas);
    difRom = comparaRom();
    /* rearranque: lo que quedo en EEPROM */
    X_2 = 0.0; X_1 = 0.00043212; X_0 = 0.90148651;   /* lo que deja gui.c:42-44 tras un reset */
    calibracionIniciar();
    difRom2 = comparaRom();
    enviar(tramas2);
    fin = 1;
    while (1) { NOP(); }
}
