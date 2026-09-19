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
char registro[1400];
unsigned int nReg;
static char trama[100];
unsigned char copia[0x1EE];
volatile unsigned int pisados, pisados2, fin;
static void enviar(const char *t) {
    unsigned int j;
    strcpy(trama, t); nSal = 0; salida[0] = 0;
    adminProcesarTrama(trama, (unsigned char)strlen(trama));
    for (j = 0; salida[j] && nReg < sizeof(registro) - 2; j++) registro[nReg++] = salida[j];
    registro[nReg++] = '|';
}
static unsigned int compara(void) {
    unsigned int i, n = 0;
    for (i = 0; i < 0x1EE; i++) if (ee[i] != copia[i]) n++;
    return n;
}
void main(void) {
    memset(ee, 0xFF, sizeof(ee));
    calibracionIniciar();
    enviar("#GC#"); enviar("#GN#"); enviar("#V#");                              /* 1-3 en blanco */
    enviar("#SC,2026-09-19#"); enviar("#SN,SLV-002#");                          /* 4-5 sin sesion */
    enviar("#L,2026#");                                                         /* 6 */
    /* estado con coeficientes y temperatura escritos, para ver que no se pisan */
    enviar("#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#");  /* 7 */
    enviar("#ST,0,5.0E-04,9.5E-01#");                                           /* 8 */
    memcpy(copia, ee, sizeof(copia));
    enviar("#SC,2026-09-19#"); enviar("#GC#");                                  /* 9-10 */
    enviar("#SN,SLV-002#"); enviar("#GN#");                                     /* 11-12 */
    enviar("#SC,2026-02-29#"); enviar("#SC,2028-02-29#"); enviar("#GC#");       /* 13-15 */
    enviar("#SC,2019-12-31#"); enviar("#SC,2100-01-01#"); enviar("#SC,2026-13-01#");  /* 16-18 */
    enviar("#SC,2026-04-31#"); enviar("#SC,2026-09-00#"); enviar("#SC,2026-9-19#");   /* 19-21 */
    enviar("#SC,2026/09/19#"); enviar("#SC,#"); enviar("#SC,2026-09-19,x#");    /* 22-24 */
    enviar("#GC#");                                                             /* 25: sigue 2028-02-29 */
    enviar("#SC,2026-09-19#");                                                  /* 26 */
    enviar("#SN,#"); enviar("#SN,ABCDEFGHIJKLM#"); enviar("#SN,NONE#");         /* 27-29 */
    enviar("#SN,A,B#"); enviar("#SN,AB#");                                 /* 30-31 */
    enviar("#GN#");                                                             /* 32: sigue SLV-002 */
    enviar("#SN,ABCDEFGHIJKL#"); enviar("#GN#");                                /* 33-34 */
    enviar("#SN,SLV 002#"); enviar("#GN#");                                     /* 35-36 */
    enviar("#SN,SLV-002#");                                                     /* 37 */
    pisados = compara();                                                        /* 0 esperado */
    enviar("#F,*#"); enviar("#FT#"); enviar("#V#"); enviar("#GC#"); enviar("#GN#");   /* 38-42 */
    enviar("#Q#"); enviar("#GC#"); enviar("#GN#");                              /* 43-45 libres */
    /* rearranque */
    X_2 = 0.0; X_1 = 0.00043212; X_0 = 0.90148651;
    calibracionIniciar();
    enviar("#GC#"); enviar("#GN#"); enviar("#V#");                              /* 46-48 */
    /* CRC corrupta */
    ee[EE_FECHA + 2] ^= 0x01; enviar("#GC#");                                   /* 49 NONE */
    ee[EE_SERIE + 17] ^= 0x10; enviar("#GN#");                                  /* 50 NONE */
    enviar("#L,2026#"); enviar("#SC,2026-09-20#"); enviar("#GC#");              /* 51-53 se rehace */
    memcpy(copia, ee, sizeof(copia));
    enviar("#SC,NONE#"); enviar("#GC#");                                        /* 54-55 borrada */
    pisados2 = compara();
    enviar("#GN#");                                                             /* 56 NONE (sigue corrupta) */
    fin = 1;
    while (1) { NOP(); }
}
