#include "Uart_stone.h"
#include "lcd.h"

//#define CMD_RESULT_OK 0
//#define CMD_RESULT_EEPROM_ERROR 1
//#define CMD_RESULT_BAD_PARAMETER 2
//#define CMD_RESULT_BAD_INTERVAL 3
//
//#define MESSAGE_TYPE_COMMAND 0
//
//#define COMMAND_RESET 0
//#define COMMAND_READ_LOCAL_TEMPERATURE 1
//#define COMMAND_READ_REMOTE_TEMPERATURE 2

#define BUFFER_RX_SIZE 50

struct pt ptPantallaRx, ptPantallaTimeout;
static char datoRX;

volatile unsigned int bufferindice = 0;
char bufferPantalla[BUFFER_RX_SIZE];

int ultimoDatoRecibido;

void send_string2(const char *x) {
    while (*x) {
        UART2_Write(*x++);
    }
}

// limpia las dos siguientes variables
void borrarBuffer(void) {
//    bufferindice = 0;
//    memset(bufferPantalla, 0, sizeof (bufferPantalla)); // copia en bufferdata cero sizeof (bufferData) lo borra
}

static int taskUartTimeout(struct pt *pt) {
    static unsigned long delayTimeout;


    PT_BEGIN(pt);
    while (1) {
//        PT_WAIT_UNTIL(pt, bufferindice > 0);
//        delayTimeout = getMillis() + 1500;
//        PT_WAIT_UNTIL(pt, getMillis() > delayTimeout);
//        borrarBuffer();
    }
    PT_END(pt);
}

void sendUartStr2(char *str) {
    unsigned int strL;
    unsigned char i;

    strL = strlen(str);
    for (i = 0; i < strL; i++) {
        UART2_Write(str[i]);
    }

}

static void sendAck2(char *command, char *params) {
//
//    sendUartStr(command);
//    sendUartStr(",A");
//    if (params != NULL) {
//        UART2_Write(',');
//        sendUartStr(params);
//    }
//    UART2_Write('#');
}


 void gets1USART(char *buffer, unsigned char len)
 {
   char i;    // Length counter
   unsigned char data;
 }
 
int mostrarDato(struct pt *pt){
    PT_BEGIN(pt);

    PT_EXIT(pt);
    PT_END(pt);
}

#include "calibracion_v36.h"   // V3.6: registro de tramas para #K#

void readUartStr2(){
    unsigned char b;
    registroStoneInicio();   // V3.6: registro para #K#; no altera bufferPantalla ni bufferindice
    while (UART2_is_rx_ready()) {
        b = UART2_Read();
        registroStoneByte(b);
        bufferPantalla[bufferindice] = b; // Lee la cadena de UART Rx FIFO
//        UART2_Write(bufferPantalla[bufferindice]);
//        ultimoDatoRecibido = (int)bufferPantalla[bufferindice];
        bufferindice++;
    }
    registroStoneFin();
//    bufferindice = 0;
}

static int leer2(struct pt *pt) {
    unsigned long time;
    PT_BEGIN(pt);
        __delay_ms(20);
        if (UART2_is_rx_ready()){   // ¿Algún dato en Rx FIFO?
            readUartStr2();         //bufferData);
        }
//    sendUartStr2(bufferPantalla);
    PT_EXIT(pt);
    PT_END(pt);
}

void enviarDatos2(uint16_t valor, uint16_t direccion) {
    unsigned long time;
    uint8_t enviar; // 0XA55A058254320001
//    PT_BEGIN(pt);
                enviar = 0xA5;
        UART2_Write(enviar);
        
                enviar = 0x5A;
        UART2_Write(enviar);
        
                enviar = 0x05;
        UART2_Write(enviar);
        
                enviar = 0x82;
        UART2_Write(enviar);
        
                enviar = 0x00;
        UART2_Write(enviar);
        
                enviar = (direccion & 0x00FF)>>0;
        UART2_Write(enviar);
        
                enviar = (valor & 0xFF00)>>8;
        UART2_Write(enviar);
        
                enviar = (valor & 0x00FF)>>0;
        UART2_Write(enviar);
//    PT_EXIT(pt);
//    PT_END(pt);
}

static int taskPantallaRx(struct pt *pt) {
    
    
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
        PT_WAIT_UNTIL(pt, UART2_is_rx_ready());
        PT_SPAWN(pt, &ptSubTask, leer2(&ptSubTask));  // leer2   
//        enviarDatos2();
//        UART2_Write('B');
        
    }
    PT_EXIT(pt);
    PT_END(pt);
}

void startPantalla(void) {
    PT_INIT(&ptPantallaRx);
//    PT_INIT(&ptPantallaTimeout);
//    borrarBuffer();  // aqui se borra el buffer para la inicialización
    
}

void executePantalla(void) {
    taskPantallaRx(&ptPantallaRx);
    // taskUartTimeout(&ptPantallaTimeout);
}