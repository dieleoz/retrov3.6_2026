#include "uart_module.h"

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

void readUartStr(){
    while (UART1_is_rx_ready()) {
        bufferData[bufferIndex] = UART1_Read(); // Lee la cadena de UART Rx FIFO
//        UART1_Write(bufferData[bufferIndex]);
        bufferIndex++;
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
//    taskUartTimeout(&ptTaskTimeout);
}
