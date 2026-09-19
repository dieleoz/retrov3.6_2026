/* Prueba de uart_module.c real (incluido tal cual) con UART1 y reloj simulados. */
#include "uart_module.c"
#pragma config WDTE = OFF

static char entrada[64]; static unsigned char eIn, eOut;
bool UART1_is_rx_ready(void) { return eOut != eIn; }
uint8_t UART1_Read(void) { return (uint8_t)entrada[eOut++]; }
void UART1_Write(uint8_t b) { (void)b; }
unsigned long msSim;
unsigned long getMillis(void) { return msSim; }
char ultimaTrama[100]; volatile unsigned char nTramas;
void adminProcesarTrama(char *t, unsigned char len) { memcpy(ultimaTrama, t, len); ultimaTrama[len] = 0; nTramas++; }
volatile unsigned char nErrFormato;
void adminErrorFormato(void) { nErrFormato++; }
void adminTick(void) { }

static void meter(const char *s) { while (*s) entrada[eIn++] = *s++; }
static void correr(unsigned long ms) { unsigned long fin = msSim + ms; while (msSim < fin) { executeUartModule(); msSim++; } executeUartModule(); }

volatile unsigned char r1_idx, r1_bi, r1_b0;          /* tras "#" y 1,9 s */
volatile unsigned char r2_idx, r2_bi, r2_b0;          /* tras 2,5 s y "1" */
volatile unsigned char r3_bi, r3_n; char r3_trama[8]; /* "1" + "#V#" + clearBuffer */
volatile unsigned char r4_bi, r4_n; char r4_trama[8]; /* ventana de 500 ms: bufferIndex 0, "#V#" */
volatile unsigned char fin;

void main(void) {
    startUartModule();
    /* 1: '#' suelto */
    meter("#"); correr(1900);
    r1_idx = adminIndex; r1_bi = (unsigned char)bufferIndex; r1_b0 = (unsigned char)bufferData[0];
    correr(600);                       /* total 2,5 s */
    meter("1"); correr(5);
    r2_idx = adminIndex; r2_bi = (unsigned char)bufferIndex; r2_b0 = (unsigned char)bufferData[0];
    /* 3: trama # mientras hay un byte suelto pendiente (medida en curso) y clearBuffer() de gui.c:342 */
    clearBuffer(); nTramas = 0;
    meter("1"); correr(2);             /* bufferData = "1" */
    meter("#V"); correr(2);
    clearBuffer(); bufferIndex = 0;     /* gui.c:342-343 */
    meter("#"); correr(2);
    r3_bi = (unsigned char)bufferIndex; r3_n = nTramas; memcpy(r3_trama, ultimaTrama, 8);
    /* 4: ventana de 500 ms tras clearBuffer (bufferIndex == 0) */
    nTramas = 0; memset(ultimaTrama, 0, sizeof ultimaTrama);
    meter("#V#"); correr(2);
    r4_bi = (unsigned char)bufferIndex; r4_n = nTramas; memcpy(r4_trama, ultimaTrama, 8);
    fin = 1;
    while (1) { NOP(); }
}
