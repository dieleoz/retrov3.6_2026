/*
 * calibracion_v36.c
 *
 * Firmware V3.6 del retrorreflectometro vertical (PIC18F47K42, XC8 2.10).
 *
 *  - Las 12 ecuaciones de 2020 (ecuacionesCalibracion.c de la base, lineas 3-42)
 *    pasan a la forma R = c3*x^3 + c2*x^2 + c1*x + c0 con coeficientes en RAM.
 *  - Los coeficientes y el factor de temperatura se cargan desde EEPROM (desde
 *    0x100, cabecera "V36" y un registro con CRC-16 por codigo). Lo que no sea
 *    valido se queda con los valores de fabrica de 2020.
 *  - Ordenes de administracion #...# por UART1 (PROTOCOLO-V3.6.md, sec. 3 y 4 bis).
 *  - Registro de las ultimas 8 tramas recibidas de la pantalla STONE (#K#, #KC#).
 *
 * Fichero ASCII puro a proposito (sin tildes).
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "mcc_generated_files/mcc.h"
#include "mcc_generated_files/memory.h"
#include "calibracion_v36.h"
#include "uart_module.h"
#include "timming.h"

extern unsigned int reflectivityValue;   /* gui.c */

/* ------------------------------------------------------------------------- */
/* Valores de fabrica: copia literal de las constantes de 2020                */
/* (base: ecuacionesCalibracion.c:3-42). Orden c3, c2, c1, c0.                */
/* Los terminos restados en 2020 aparecen aqui con signo negativo.            */
/* ------------------------------------------------------------------------- */
static const double coefFabrica[EC_NUM][4] = {
    /* 0 '1' blancoIntenso   (:7)  */ { 0.0,          -0.000086541,  0.619668128, -303 },
    /* 1 '2' amarilloIntenso (:4)  */ {-0.000000073,   0.000282508,  0.124075350, -130 },
    /* 2 '3' verdeIntenso    (:19) */ { 0.000000163,  -0.000756695,  1.213142724, -442 },
    /* 3 '4' rojoIntenso     (:13) */ { 0.000000147,  -0.000668624,  1.082394050, -393 },
    /* 4 '5' azulIntenso     (:16) */ { 0.000000026,  -0.000018402,  0.102152670,  -49 },
    /* 5 '6' naranjaIntenso  (:10) */ { 0.0,           0.000090731,  0.001064329,  -21 },
    /* 6 '7' blancoOpaco     (:29) */ { 0.0,           0.000087404,  0.013617682,  -27 },
    /* 7 '8' amarilloOpaco   (:26) */ { 0.000000086,  -0.000299026,  0.446572329, -161 },
    /* 8 'a' verdeOpaco      (:41) */ { 0.000000163,  -0.000756695,  1.213142724, -442 },
    /* 9 'b' rojoOpaco       (:35) */ { 0.0,           0.000113098, -0.076130418,   10 },
    /*10 'c' azulOpaco       (:38) */ { 0.000000026,  -0.000018402,  0.102152670,  -49 },
    /*11 'd' naranjaOpaco    (:32) */ { 0.0,           0.000090731,  0.001064329,  -21 }
};

double coefCal[EC_NUM][4];

static const char codigosApp[EC_NUM] = {'1','2','3','4','5','6','7','8','a','b','c','d'};

/* ------------------------------------------------------------------------- */
/* EEPROM (revision 1.1 del protocolo: un registro por codigo)                */
/*                                                                            */
/*  0x100  'V','3','6', version (1)            cabecera, sin CRC              */
/*  0x104 + 18*k (k = 0..11, orden 1-8, a-d)   c3,c2,c1,c0 (4 float) + CRC-16 */
/*  0x1DC                                       X_2,X_1,X_0 + PIN (4 ASCII)   */
/*                                              + CRC-16                      */
/*  CRC-16/CCITT-FALSE (poli 0x1021, inicio 0xFFFF) sobre los 16 bytes de     */
/*  datos del registro; se guarda byte bajo primero. float en little endian.  */
/*  Cabecera mala: todo de fabrica. CRC malo: solo ese registro de fabrica.   */
/* ------------------------------------------------------------------------- */
#define EE_BASE        0x100
#define EE_VERSION     1
#define EE_REG_LEN     18                       /* 16 datos + 2 CRC */
#define EE_REG_DIR(r)  (EE_BASE + 4 + (unsigned int)(r) * EE_REG_LEN)
#define EE_NUM_REG     (EC_NUM + 1)             /* 12 codigos + temperatura/PIN */
#define REG_TEMP       EC_NUM

static char pinAdmin[4];
static double tempFabrica[3];       /* copia de gui.c:41-43 tomada al arrancar */
static unsigned char modoAdmin;     /* arranca cerrado; se cierra con #Q#, al apagar o a los 10 min */
static unsigned char fallosPin;     /* 5 fallos seguidos (#L o #P) bloquean #L y #P hasta apagar */
static unsigned long tUltimaTrama;

union FloatBytes {
    double valor;
    unsigned char b[4];
};

static unsigned int crc16Paso(unsigned int crc, unsigned char dato) {
    unsigned char i;
    crc ^= ((unsigned int)dato) << 8;
    for (i = 0; i < 8; i++) {
        if (crc & 0x8000) {
            crc = (crc << 1) ^ 0x1021;
        } else {
            crc = crc << 1;
        }
    }
    return crc;
}

/* Byte j (0..15) de los datos del registro r, tomado de la RAM. */
static unsigned char byteRegistro(unsigned char r, unsigned char j) {
    union FloatBytes fb;
    if (r < EC_NUM) {
        fb.valor = coefCal[r][j / 4];
        return fb.b[j % 4];
    }
    if (j < 12) {
        if (j < 4) fb.valor = X_2;
        else if (j < 8) fb.valor = X_1;
        else fb.valor = X_0;
        return fb.b[j % 4];
    }
    return (unsigned char)pinAdmin[j - 12];
}

static unsigned int crcRegistroRam(unsigned char r) {
    unsigned int crc = 0xFFFF;
    unsigned char j;
    for (j = 0; j < 16; j++) crc = crc16Paso(crc, byteRegistro(r, j));
    return crc;
}

static void eeEscribirSiDistinto(unsigned int dir, unsigned char dato) {
    if (DATAEE_ReadByte(dir) != dato) {
        DATAEE_WriteByte(dir, dato);
    }
}

static const unsigned char cabecera[4] = {'V', '3', '6', EE_VERSION};

/* Escribe cabecera y los 13 registros desde la RAM (solo los bytes que cambian)
 * y relee todo. Devuelve 1 si la EEPROM coincide con la RAM. */
static unsigned char guardarEeprom(void) {
    unsigned char r, j;
    unsigned int crc, dir;

    for (j = 0; j < 4; j++) eeEscribirSiDistinto(EE_BASE + j, cabecera[j]);
    for (r = 0; r < EE_NUM_REG; r++) {
        dir = EE_REG_DIR(r);
        for (j = 0; j < 16; j++) eeEscribirSiDistinto(dir + j, byteRegistro(r, j));
        crc = crcRegistroRam(r);
        eeEscribirSiDistinto(dir + 16, (unsigned char)(crc & 0xFF));
        eeEscribirSiDistinto(dir + 17, (unsigned char)(crc >> 8));
    }
    /* relectura */
    for (j = 0; j < 4; j++) {
        if (DATAEE_ReadByte(EE_BASE + j) != cabecera[j]) return 0;
    }
    for (r = 0; r < EE_NUM_REG; r++) {
        dir = EE_REG_DIR(r);
        for (j = 0; j < 16; j++) {
            if (DATAEE_ReadByte(dir + j) != byteRegistro(r, j)) return 0;
        }
        crc = crcRegistroRam(r);
        if (DATAEE_ReadByte(dir + 16) != (unsigned char)(crc & 0xFF)) return 0;
        if (DATAEE_ReadByte(dir + 17) != (unsigned char)(crc >> 8)) return 0;
    }
    return 1;
}

static double leerFloatEe(unsigned int dir) {
    union FloatBytes fb;
    unsigned char i;
    for (i = 0; i < 4; i++) {
        fb.b[i] = DATAEE_ReadByte(dir + i);
    }
    return fb.valor;
}

static unsigned char registroEeValido(unsigned char r) {
    unsigned int crc = 0xFFFF, dir = EE_REG_DIR(r);
    unsigned char j;
    for (j = 0; j < 16; j++) crc = crc16Paso(crc, DATAEE_ReadByte(dir + j));
    return (DATAEE_ReadByte(dir + 16) == (unsigned char)(crc & 0xFF) &&
            DATAEE_ReadByte(dir + 17) == (unsigned char)(crc >> 8)) ? 1 : 0;
}

void calibracionIniciar(void) {
    unsigned char r, g, j;
    unsigned int dir;

    /* Estado de fabrica. X_2, X_1, X_0 valen ya lo de 2020 (gui.c:41-43). */
    memcpy(coefCal, coefFabrica, sizeof(coefCal));
    tempFabrica[0] = X_2; tempFabrica[1] = X_1; tempFabrica[2] = X_0;
    pinAdmin[0] = '2'; pinAdmin[1] = '0'; pinAdmin[2] = '2'; pinAdmin[3] = '6';
    modoAdmin = 0;
    fallosPin = 0;

    for (j = 0; j < 4; j++) {
        if (DATAEE_ReadByte(EE_BASE + j) != cabecera[j]) return;   /* todo de fabrica */
    }
    for (r = 0; r < EC_NUM; r++) {
        if (!registroEeValido(r)) continue;                        /* ese codigo, de fabrica */
        dir = EE_REG_DIR(r);
        for (g = 0; g < 4; g++) coefCal[r][g] = leerFloatEe(dir + (unsigned int)g * 4);
    }
    if (registroEeValido(REG_TEMP)) {
        dir = EE_REG_DIR(REG_TEMP);
        X_2 = leerFloatEe(dir);
        X_1 = leerFloatEe(dir + 4);
        X_0 = leerFloatEe(dir + 8);
        for (j = 0; j < 4; j++) pinAdmin[j] = (char)DATAEE_ReadByte(dir + 12 + j);
    }
}

/* Mascara de #V#: bit k = codigo k distinto de fabrica; bit 12 = temperatura. */
static unsigned int mascaraAjustes(void) {
    unsigned int m = 0;
    unsigned char k, i, j;
    union FloatBytes a, b;
    for (k = 0; k < EC_NUM; k++) {
        for (i = 0; i < 4; i++) {
            a.valor = coefCal[k][i];
            b.valor = coefFabrica[k][i];
            for (j = 0; j < 4; j++) {
                if (a.b[j] != b.b[j]) { m |= (1u << k); }
            }
        }
    }
    for (i = 0; i < 3; i++) {
        a.valor = (i == 0) ? X_2 : ((i == 1) ? X_1 : X_0);
        b.valor = tempFabrica[i];
        for (j = 0; j < 4; j++) {
            if (a.b[j] != b.b[j]) { m |= (1u << 12); }
        }
    }
    return m;
}

/* ------------------------------------------------------------------------- */
/* Ecuacion general. Misma forma y mismo orden de operaciones que las de 2020 */
/* (producto de izquierda a derecha, suma de izquierda a derecha).            */
/* ------------------------------------------------------------------------- */
void aplicarEcuacion(unsigned char k) {
    reflectivityValue = (double)(coefCal[k][0]*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue
                               + coefCal[k][1]*(double)reflectivityValue*(double)reflectivityValue
                               + coefCal[k][2]*(double)reflectivityValue
                               + coefCal[k][3]);
}

/* ------------------------------------------------------------------------- */
/* Registro de tramas de la STONE (UART2)                                     */
/* ------------------------------------------------------------------------- */
#define STONE_REG_N     8
#define STONE_REG_MAX   16     /* bytes guardados por trama; el resto se descarta */

static unsigned char stoneReg[STONE_REG_N][STONE_REG_MAX];
static unsigned char stoneRegLen[STONE_REG_N];
static unsigned char stoneRegSig;       /* siguiente ranura a escribir     */
static unsigned char stoneRegUsadas;    /* ranuras con dato (0..8)         */
static unsigned long stoneRegTotal;     /* tramas vistas desde el arranque */

void registroStoneInicio(void) {
    stoneRegLen[stoneRegSig] = 0;
}

void registroStoneByte(unsigned char b) {
    unsigned char n = stoneRegLen[stoneRegSig];
    if (n < STONE_REG_MAX) {
        stoneReg[stoneRegSig][n] = b;
        stoneRegLen[stoneRegSig] = n + 1;
    }
}

void registroStoneFin(void) {
    stoneRegSig++;
    if (stoneRegSig >= STONE_REG_N) stoneRegSig = 0;
    if (stoneRegUsadas < STONE_REG_N) stoneRegUsadas++;
    stoneRegTotal++;
}

static void enviarHex(unsigned char b) {
    static const char hx[] = "0123456789ABCDEF";
    UART1_Write(hx[b >> 4]);
    UART1_Write(hx[b & 0x0F]);
}

static void responderRegistroStone(void) {
    char num[12];
    unsigned char i, j, ranura;

    sprintf(num, "%lu", stoneRegTotal);
    sendUartStr("#K,");
    sendUartStr(num);
    UART1_Write(',');
    /* de la mas antigua a la mas reciente */
    ranura = (unsigned char)((stoneRegSig + STONE_REG_N - stoneRegUsadas) % STONE_REG_N);
    for (i = 0; i < stoneRegUsadas; i++) {
        if (i > 0) UART1_Write(';');
        for (j = 0; j < stoneRegLen[ranura]; j++) {
            enviarHex(stoneReg[ranura][j]);
        }
        ranura++;
        if (ranura >= STONE_REG_N) ranura = 0;
    }
    UART1_Write('#');
}

/* ------------------------------------------------------------------------- */
/* Ordenes de administracion                                                  */
/* ------------------------------------------------------------------------- */
#define MAX_CAMPOS 6
#define ADMIN_CADUCIDAD_MS 600000UL     /* 10 min sin tramas '#' */
#define PIN_MAX_FALLOS 5
#define TEMP_X0_MIN 0.5                 /* limites de #ST para X_0 */
#define TEMP_X0_MAX 1.5

extern void arreglar_dato(void);        /* ecuacionesCalibracion.c:49-54 */

/* Copia del estado para deshacer si la EEPROM no se relee bien (RF-FW-19). */
static double copiaCoef[EC_NUM][4];
static double copiaTemp[3];
static char copiaPin[4];

static void responder(const char *s) {
    sendUartStr((char *)s);
}

void adminErrorFormato(void) {
    responder("#ERR,FORMATO#");
}

/* Llamada en cada vuelta del bucle principal: caducidad del modo admin. */
void adminTick(void) {
    if (modoAdmin && (getMillis() - tUltimaTrama) > ADMIN_CADUCIDAD_MS) {
        modoAdmin = 0;
    }
}

/* Indice de ecuacion para el codigo k ('1'-'8', 'a'-'d'); 0xFF si no existe. */
static unsigned char indiceCodigo(const char *campo) {
    unsigned char i;
    if (campo[0] == 0 || campo[1] != 0) return 0xFF;
    for (i = 0; i < EC_NUM; i++) {
        if (codigosApp[i] == campo[0]) return i;
    }
    return 0xFF;
}

/* Convierte un campo numerico completo. Devuelve 1 si es valido y finito. */
static unsigned char leerNumero(const char *campo, double *valor) {
    char *fin;
    union FloatBytes fb;
    if (campo[0] == 0) return 0;
    fb.valor = strtod(campo, &fin);
    if (fin == campo || *fin != 0) return 0;
    /* exponente 0xFF: infinito o NaN */
    if (((fb.b[3] & 0x7F) == 0x7F) && (fb.b[2] & 0x80)) return 0;
    *valor = fb.valor;
    return 1;
}

/* Entero decimal sin signo de 0 a 65535, solo digitos. */
static unsigned char leerEntero(const char *campo, unsigned int *valor) {
    unsigned long v = 0;
    unsigned char n = 0;
    while (campo[n]) {
        if (campo[n] < '0' || campo[n] > '9' || n >= 5) return 0;
        v = v * 10 + (unsigned long)(campo[n] - '0');
        n++;
    }
    if (n == 0 || v > 65535UL) return 0;
    *valor = (unsigned int)v;
    return 1;
}

static unsigned char pinCorrecto(const char *campo) {
    return (strlen(campo) == 4 && memcmp(campo, pinAdmin, 4) == 0) ? 1 : 0;
}

static unsigned char pinValido(const char *campo) {
    unsigned char i;
    if (strlen(campo) != 4) return 0;
    for (i = 0; i < 4; i++) {
        if (campo[i] < '0' || campo[i] > '9') return 0;
    }
    return 1;
}

static void enviarNumero(double v) {
    char num[24];
    sprintf(num, "%.8E", v);
    sendUartStr(num);
}

/* Fecha de compilacion en AAAA-MM-DD a partir de __DATE__ ("Mmm dd aaaa"). */
static void enviarFecha(void) {
    static const char meses[] = "JanFebMarAprMayJunJulAugSepOctNovDec";
    const char *d = __DATE__;
    char f[11];
    unsigned char m;

    for (m = 0; m < 12; m++) {
        if (d[0] == meses[m * 3] && d[1] == meses[m * 3 + 1] && d[2] == meses[m * 3 + 2]) break;
    }
    m++;
    f[0] = d[7]; f[1] = d[8]; f[2] = d[9]; f[3] = d[10];
    f[4] = '-';
    f[5] = (char)('0' + m / 10); f[6] = (char)('0' + m % 10);
    f[7] = '-';
    f[8] = (d[4] == ' ') ? '0' : d[4]; f[9] = d[5];
    f[10] = 0;
    sendUartStr(f);
}

static void hacerCopia(void) {
    memcpy(copiaCoef, coefCal, sizeof(copiaCoef));
    copiaTemp[0] = X_2; copiaTemp[1] = X_1; copiaTemp[2] = X_0;
    memcpy(copiaPin, pinAdmin, 4);
}

/* Guarda en EEPROM y responde. Si la relectura falla, la RAM vuelve al valor
 * anterior, se intenta dejar la EEPROM como estaba y se responde #ERR,EEPROM#. */
static void guardarYResponder(void) {
    if (guardarEeprom()) {
        responder("#OK#");
        return;
    }
    memcpy(coefCal, copiaCoef, sizeof(coefCal));
    X_2 = copiaTemp[0]; X_1 = copiaTemp[1]; X_0 = copiaTemp[2];
    memcpy(pinAdmin, copiaPin, 4);
    (void)guardarEeprom();
    responder("#ERR,EEPROM#");
}

/*
 * trama: bytes recibidos, desde el '#' inicial hasta el '#' final incluidos,
 * terminada en 0. len: numero de bytes (>= 2).
 */
void adminProcesarTrama(char *trama, unsigned char len) {
    char *campo[MAX_CAMPOS];
    char num[8];
    unsigned char nc = 0;
    unsigned char i, k;
    unsigned int x, guardado;
    char *p;
    double v[4];

    tUltimaTrama = getMillis();

    /* quitar los '#' y trocear por comas */
    trama[len - 1] = 0;
    p = trama + 1;
    campo[nc++] = p;
    while (*p) {
        if (*p == ',') {
            *p = 0;
            if (nc >= MAX_CAMPOS) { adminErrorFormato(); return; }
            campo[nc++] = p + 1;
        }
        p++;
    }

    if (strcmp(campo[0], "V") == 0 && nc == 1) {
        x = mascaraAjustes();
        responder("#V," FW_VERSION_STR ",");
        enviarFecha();
        responder(x ? ",CAL," : ",DEF,");
        sprintf(num, "%04X", x);
        sendUartStr(num);
        UART1_Write('#');
    }
    else if (strcmp(campo[0], "L") == 0 && nc == 2) {
        if (fallosPin >= PIN_MAX_FALLOS) {
            modoAdmin = 0;
            responder("#ERR,BLOQUEADO#");
        } else if (pinCorrecto(campo[1])) {
            fallosPin = 0;
            modoAdmin = 1;
            responder("#OK#");
        } else {
            fallosPin++;
            modoAdmin = 0;
            responder("#ERR,PIN#");
        }
    }
    else if (strcmp(campo[0], "Q") == 0 && nc == 1) {
        modoAdmin = 0;
        responder("#OK#");
    }
    else if (strcmp(campo[0], "G") == 0 && nc == 2) {
        k = indiceCodigo(campo[1]);
        if (k == 0xFF) { adminErrorFormato(); return; }
        responder("#G,");
        UART1_Write((uint8_t)codigosApp[k]);
        for (i = 0; i < 4; i++) {
            UART1_Write(',');
            enviarNumero(coefCal[k][i]);
        }
        UART1_Write('#');
    }
    else if (strcmp(campo[0], "E") == 0 && nc == 3) {
        /* Evalua la ecuacion k en x sin medir; devuelve lo que saldria en "::" */
        k = indiceCodigo(campo[1]);
        if (k == 0xFF || !leerEntero(campo[2], &x)) { adminErrorFormato(); return; }
        guardado = reflectivityValue;
        reflectivityValue = x;
        aplicarEcuacion(k);
        arreglar_dato();
        x = reflectivityValue;
        reflectivityValue = guardado;
        responder("#E,");
        UART1_Write((uint8_t)codigosApp[k]);
        UART1_Write(',');
        sprintf(num, "%u", x);
        sendUartStr(num);
        UART1_Write('#');
    }
    else if (strcmp(campo[0], "S") == 0 && nc == 6) {
        if (!modoAdmin) { responder("#ERR,BLOQUEADO#"); return; }
        k = indiceCodigo(campo[1]);
        if (k == 0xFF) { adminErrorFormato(); return; }
        for (i = 0; i < 4; i++) {
            if (!leerNumero(campo[2 + i], &v[i])) { adminErrorFormato(); return; }
        }
        hacerCopia();
        for (i = 0; i < 4; i++) coefCal[k][i] = v[i];
        guardarYResponder();
    }
    else if (strcmp(campo[0], "F") == 0 && nc == 2) {
        if (!modoAdmin) { responder("#ERR,BLOQUEADO#"); return; }
        if (strcmp(campo[1], "*") == 0) {
            hacerCopia();
            memcpy(coefCal, coefFabrica, sizeof(coefCal));
        } else {
            k = indiceCodigo(campo[1]);
            if (k == 0xFF) { adminErrorFormato(); return; }
            hacerCopia();
            for (i = 0; i < 4; i++) coefCal[k][i] = coefFabrica[k][i];
        }
        guardarYResponder();
    }
    else if (strcmp(campo[0], "GT") == 0 && nc == 1) {
        responder("#GT,");
        enviarNumero(X_2);
        UART1_Write(',');
        enviarNumero(X_1);
        UART1_Write(',');
        enviarNumero(X_0);
        UART1_Write('#');
    }
    else if (strcmp(campo[0], "ST") == 0 && nc == 4) {
        if (!modoAdmin) { responder("#ERR,BLOQUEADO#"); return; }
        for (i = 0; i < 3; i++) {
            if (!leerNumero(campo[1 + i], &v[i])) { adminErrorFormato(); return; }
        }
        /* X_0 fuera de [0,5 ; 1,5] llevaria las medidas a 0 o las duplicaria */
        if (v[2] < TEMP_X0_MIN || v[2] > TEMP_X0_MAX) { adminErrorFormato(); return; }
        hacerCopia();
        X_2 = v[0];
        X_1 = v[1];
        X_0 = v[2];
        guardarYResponder();
    }
    else if (strcmp(campo[0], "P") == 0 && nc == 3) {
        if (!modoAdmin) { responder("#ERR,BLOQUEADO#"); return; }
        if (fallosPin >= PIN_MAX_FALLOS) { modoAdmin = 0; responder("#ERR,BLOQUEADO#"); return; }
        if (!pinCorrecto(campo[1])) {      /* cuenta para el bloqueo, como #L */
            fallosPin++;
            if (fallosPin >= PIN_MAX_FALLOS) modoAdmin = 0;
            responder("#ERR,PIN#");
            return;
        }
        fallosPin = 0;
        if (!pinValido(campo[2])) { adminErrorFormato(); return; }
        hacerCopia();
        memcpy(pinAdmin, campo[2], 4);
        guardarYResponder();
    }
    else if (strcmp(campo[0], "K") == 0 && nc == 1) {
        responderRegistroStone();
    }
    else if (strcmp(campo[0], "KC") == 0 && nc == 1) {
        stoneRegUsadas = 0;
        stoneRegSig = 0;
        responder("#OK#");
    }
    else {
        adminErrorFormato();
    }
}
