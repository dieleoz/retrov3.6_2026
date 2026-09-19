
#ifndef ECUACIONES_CALIBRACION
#define	ECUACIONES_CALIBRACION
#define BUFFER_RX_SIZE 50;

#include "Uart_stone.h"
#include "mcc_generated_files/mcc.h"
#include <xc.h>
#include <math.h>
#include "pt.h"
#include "pt-sem.h"
#include "binary_utils.h"
#include <string.h>
#include "gui.h"


extern unsigned int reflectivityValue;
extern char reflectStr[17];
extern char dataEnvia[8];
extern char bufferPantalla[50];
extern char bufferData[50];
extern int beepOnce(struct pt *pt);
extern void presentarDato(void);
void arreglar_dato(void);
int conversionDatoEnviar(struct pt * pt);
int ecuacionesColoresGatillo(struct pt * pt);
int ecuacionesColoresApp(struct pt * pt);
#endif	

