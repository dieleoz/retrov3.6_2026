
#ifndef GUI_H
#define	GUI_H

#include "pt.h"
#include "lcd.h"
#include "eeprom_manager.h"
#include "timming.h"
#include <string.h>
#include "mcc_generated_files/mcc.h"
#include "measurement.h"
#include "pt-sem.h"
#include <math.h>
#include "Uart_stone.h"
#include "uart_module.h"
#include "dhtA.h"
#include "ecuacionesCalibracion.h"
void startGuiModule(void);

void executeGuiModule(void);
int beepOnce(struct pt *pt);
void presentarDato(void);
#endif	/* GUI_H */

