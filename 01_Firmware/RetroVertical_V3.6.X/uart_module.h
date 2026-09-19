/* 
 * File:   uart_module.h
 * Author: Fabio
 *
 * Created on January 2, 2019, 2:39 PM
 */

#ifndef UART_MODULE_H
#define	UART_MODULE_H

#include "pt.h"
#include "mcc_generated_files/mcc.h"
#include <string.h>
#include "timming.h"
#include "binary_utils.h"
#include "measurement.h"
#include <stdio.h>
#include "eeprom_manager.h"
#include <stdlib.h>

void startUartModule(void);
void executeUartModule(void);
void sendUartStr(char *str);
#endif	/* UART_MODULE_H */

