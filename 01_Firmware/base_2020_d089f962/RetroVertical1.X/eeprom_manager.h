#ifndef EEPROM_MANAGER_H
#define	EEPROM_MANAGER_H



#define RETRO_MODEL_BASIC 0
#define RETRO_MODEL_IOT 1

#define EEPROM_ADDRESS_RETRO_MODEL 0 //1 byte
#define EEPROM_ADDRESS_OWNER 1 //16 bytes
#define EEPROM_ADDRESS_SERIAL 18 //16 bytes
#define EEPROM_ADDRESS_ENABLED_GEOMETRY 36 //1 byte
#define EEPROM_ADDRESS_BLACK_PATTERN_15_VALUE 37 //2 bytes
#define EEPROM_ADDRESS_WHITE_PATTERN_15_VALUE 39 //2 bytes
#define EEPROM_ADDRESS_BLACK_PATTERN_30_VALUE 41 //2 bytes
#define EEPROM_ADDRESS_WHITE_PATTERN_30_VALUE 43 //2 bytes
#define EEPROM_ADDRESS_OFFSET_15 45 //2 bytes
#define EEPROM_ADDRESS_OFFSET_30 47 //2 bytes
#define EEPROM_ADDRESS_BATTERY_FACTOR 49 //4 bytes
#define EEPROM_ADDRESS_BAT_PERCENTAGE_VOLTAGE_MINIMUM 54
#define EEPROM_ADDRESS_BAT_PERCENTAGE_VOLTAGE_MAXIMUM 60
#define EEPROM_ADDRESS_BAT_PERCENTAGE_PERCENTAGE_MINIMUM 64
#define EEPROM_ADDRESS_BAT_PERCENTAGE_PERCENTAGE_MAXIMUM 70





#include "mcc_generated_files/memory.h"
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>

unsigned char getModel();
void writeCustomerName(char *customerName);
void readCustomerName(char *customerName);
void writeSerial(char *serial);
void readSerial(char *serial);
void writeGeometry(char *strGeometry);
unsigned char readGeometry();

void saveUint(unsigned int address, unsigned int value);

unsigned int readUint(unsigned int address);
void saveFloat(unsigned int address, float value);
float readFloat(unsigned int address);
void writeFunction15Params(char *strBlackPatternValue, char *strWhitePatternValue);
void writeFunction30Params(char *strBlackPatternValue, char *strWhitePatternValue);


#endif	/* EEPROM_MANAGER_H */

