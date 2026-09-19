#ifndef UART_STONE_H
#define	UART_STONE_H

#include "pt.h"
#include "mcc_generated_files/mcc.h"
#include <string.h>
#include "timming.h"
#include "binary_utils.h"
#include "measurement.h"
#include <stdio.h>
#include "eeprom_manager.h"
#include <stdlib.h>

void startPantalla(void);
void executePantalla(void);
void enviarDatos2(uint16_t valor, uint16_t direccion);

#endif	/* UART_Stone */
