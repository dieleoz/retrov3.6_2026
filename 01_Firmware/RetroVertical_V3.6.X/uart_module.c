#include "uart_module.h"
#include "calibracion_v36.h"

#define CMD_RESULT_OK 0
#define CMD_RESULT_EEPROM_ERROR 1
#define CMD_RESULT_BAD_PARAMETER 2
#define CMD_RESULT_BAD_INTERVAL 3

#define MESSAGE_TYPE_COMMAND 0

#define COMMAND_RESET 0
#define COMMAND_READ_LOCAL_TEMPERATURE 1
#define COMMAND_READ_REMOTE_TEMPERATURE 2

#define BUFFER_RX_SIZE 50

struct pt ptTaskUartRx, ptTaskTimeout;

volatile unsigned int bufferIndex = 0;
char bufferData[50];

// limpia las dos siguientes variables
 void clearBuffer(void) {
    bufferIndex = 0;
    memset(bufferData, 0, sizeof (bufferData)); // copia en bufferdata cero sizeof (bufferData)
}

static int taskUartTimeout(struct pt *pt) {
    static unsigned long delayTimeout;


    PT_BEGIN(pt);
    while (1) {
        PT_WAIT_UNTIL(pt, bufferIndex > 0);
        delayTimeout = getMillis() + 2000;
        PT_WAIT_UNTIL(pt, getMillis() > delayTimeout);
        clearBuffer();
    }
    PT_END(pt);
}

void sendUartStr(char *str) {
    unsigned int strL;
    unsigned char i;

    strL = strlen(str);
    for (i = 0; i < strL; i++) {
        UART1_Write(str[i]);
    }
}

/*
 * V3.6: tramas de administracion #...#
 *  - Un '#' fuera de trama abre una trama de administracion, que
 *    se guarda en tramaAdmin y NO en bufferData: gui.c no la ve y no mide.
 *  - Trama de administracion: hasta ADMIN_MAX bytes con los dos '#'. Si se pasa,
 *    se descarta entera, hasta el siguiente '#', sin responder.
 *  - Trama incompleta: se descarta a los 2 s.
 *  - Bytes sueltos (1-9, a-e): como en 2020, pero con limite de BUFFER_RX_SIZE.
 */
#define ADMIN_MAX 96              // protocolo rev. 1.1 (O-01): trama '#' de hasta 96 bytes
static char tramaAdmin[100];      // buffer de recepcion de las tramas '#': 100 bytes
static unsigned char adminIndex = 0;
static unsigned char adminLista = 0;
static unsigned char adminDescartando = 0;
static unsigned long adminTiempo;

void readUartStr(){
    char c;
    while (UART1_is_rx_ready()) {
        if (adminLista) {
            break;      // trama pendiente de atender: el resto espera en la FIFO del driver
        }
        c = UART1_Read(); // Lee la cadena de UART Rx FIFO
        if (adminDescartando) {
            adminTiempo = getMillis();
            if (c == '#') {
                adminDescartando = 0;   // trama demasiado larga: se descarta sin responder (protocolo 4 bis, O-01)
            }
            continue;
        }
        if (adminIndex > 0) {
            tramaAdmin[adminIndex++] = c;
            if (c == '#') {
                tramaAdmin[adminIndex] = 0;
                adminLista = 1;
            } else if (adminIndex >= ADMIN_MAX) {
                adminIndex = 0;
                adminDescartando = 1;
                adminTiempo = getMillis();
            }
            continue;
        }
        if (c == '#') {   // abre trama aunque haya bytes sueltos pendientes (O-05): no la ve clearBuffer()
            tramaAdmin[0] = c;
            adminIndex = 1;
            adminTiempo = getMillis();
            continue;
        }
        if (bufferIndex < BUFFER_RX_SIZE - 1) {   // V3.6: limite comprobado (en 2020 no lo habia)
            bufferData[bufferIndex] = c;
//        UART1_Write(bufferData[bufferIndex]);
            bufferIndex++;
        }
    }
}

static void atenderAdmin(void) {
    if (adminLista) {
        adminProcesarTrama(tramaAdmin, adminIndex);
        adminIndex = 0;
        adminLista = 0;
    } else if ((adminIndex > 0 || adminDescartando) && (getMillis() - adminTiempo) > 2000) {
        adminIndex = 0;
        adminDescartando = 0;
    }
}

static int leer(struct pt *pt) {
    unsigned long time;
    PT_BEGIN(pt);
        if (UART1_is_rx_ready()){ // ¿Algún dato en Rx FIFO?
            readUartStr();
        }
    PT_EXIT(pt);
    PT_END(pt);
}

static int taskUartRx(struct pt *pt) {
    static unsigned long delayRx;
    static float tempFloat;
    static char numericStr[10];
    static unsigned char tempChar;
    static unsigned int tempUInt;
    static struct pt ptSubTask;
    static char ackParams[20];

    #define PARAM_SIZE 20

    static char rxChar;
    static char strCommand[PARAM_SIZE];
    static char strModifier[PARAM_SIZE];
    static char strParam1[PARAM_SIZE];
    static char strParam2[PARAM_SIZE];
    static char strParam3[PARAM_SIZE];
    static char strParam4[PARAM_SIZE];


    PT_BEGIN(pt);

    while (1) {
        PT_WAIT_UNTIL(pt, UART1_is_rx_ready());
        PT_SPAWN(pt, &ptSubTask, leer(&ptSubTask));        
    }

    PT_END(pt);
}

void startUartModule(void) {

    PT_INIT(&ptTaskUartRx);
    PT_INIT(&ptTaskTimeout);

//    clearBuffer();
}

void executeUartModule(void) {
    taskUartRx(&ptTaskUartRx);
    atenderAdmin();   // V3.6
    adminTick();      // V3.6: caducidad del modo administrador
//    taskUartTimeout(&ptTaskTimeout);
}
