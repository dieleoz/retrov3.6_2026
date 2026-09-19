#include "eeprom_manager.h"
#include "lcd.h"

union UnionUInt {
    unsigned int value;
    unsigned char chars[2];
};

union UnionFloat{
    float value;
    unsigned char chars[4];
};


void saveFloat(unsigned int address, float value){
    union UnionFloat unionFloat;
    
    unionFloat.value=value;
    DATAEE_WriteByte(address,unionFloat.chars[0]);
    DATAEE_WriteByte(address+1,unionFloat.chars[1]);
    DATAEE_WriteByte(address+2,unionFloat.chars[2]);
    DATAEE_WriteByte(address+3,unionFloat.chars[3]);
}

float readFloat(unsigned int address){
    union UnionFloat unionFloat;
    
    unionFloat.chars[0]=DATAEE_ReadByte(address);
    unionFloat.chars[1]=DATAEE_ReadByte(address+1);
    unionFloat.chars[2]=DATAEE_ReadByte(address+2);
    unionFloat.chars[3]=DATAEE_ReadByte(address+3);
    
    return unionFloat.value;
    
}

void saveUint(unsigned int address, unsigned int value) {
    union UnionUInt unionUInt;

    unionUInt.value = value;
    DATAEE_WriteByte(address, unionUInt.chars[0]);
    DATAEE_WriteByte(address + 1, unionUInt.chars[1]);
}

unsigned int readUint(unsigned int address) {
    union UnionUInt unionUInt;

    unionUInt.chars[0] = DATAEE_ReadByte(address);
    unionUInt.chars[1] = DATAEE_ReadByte(address + 1);
    
    return unionUInt.value;
}

unsigned char getModel() {
    unsigned char reflectometerModel;

    reflectometerModel = DATAEE_ReadByte(EEPROM_ADDRESS_RETRO_MODEL);
    if (reflectometerModel > 1) {
        reflectometerModel = RETRO_MODEL_BASIC;
    }
    return reflectometerModel;
}

void writeCustomerName(char *customerName) {
    unsigned char i;
    unsigned int len;
    char chr;

    len = strlen(customerName);
    for (i = 0; i < 16; i++) {
        if (i < len) {
            chr = customerName[i];
            if (isprint(chr)) {
                DATAEE_WriteByte(EEPROM_ADDRESS_OWNER + i, chr);
            } else {
                DATAEE_WriteByte(EEPROM_ADDRESS_OWNER + i, ' ');
            }
        } else {
            DATAEE_WriteByte(EEPROM_ADDRESS_OWNER + i, ' ');
        }
    }
}

void readCustomerName(char *customerName) {
    unsigned char i;
    char chr;

    for (i = 0; i < 16; i++) {
        chr = DATAEE_ReadByte(EEPROM_ADDRESS_OWNER + i);
        if (isprint(chr)) {
            customerName[i] = chr;
        } else {
            customerName[i] = ' ';
        }
    }
}

void writeSerial(char *serial) {
    unsigned char i;
    unsigned int len;
    char chr;

    len = strlen(serial);
    for (i = 0; i < 16; i++) {
        if (i < len) {
            chr = serial[i];
            if (isprint(chr)) {
                DATAEE_WriteByte(EEPROM_ADDRESS_SERIAL + i, chr);
            } else {
                DATAEE_WriteByte(EEPROM_ADDRESS_SERIAL + i, ' ');
            }
        } else {
            DATAEE_WriteByte(EEPROM_ADDRESS_SERIAL + i, ' ');
        }
    }
}

void readSerial(char *serial) {
    unsigned char i;
    char chr;

    for (i = 0; i < 16; i++) {
        chr = DATAEE_ReadByte(EEPROM_ADDRESS_SERIAL + i);
        if (isprint(chr)) {
            serial[i] = chr;
        } else {
            serial[i] = ' ';
        }
    }
}

void writeGeometry(char *strGeometry) {
    unsigned char geometry;

    geometry = (unsigned char) atoi(strGeometry);
    DATAEE_WriteByte(EEPROM_ADDRESS_ENABLED_GEOMETRY, geometry);
}

unsigned char readGeometry() {
    unsigned char geometry;

    geometry = DATAEE_ReadByte(EEPROM_ADDRESS_ENABLED_GEOMETRY);
    if (geometry > 2) {
        geometry = 0;
    }

    return geometry;
}

void writeFunction15Params(char *strBlackPatternValue, char *strWhitePatternValue){
    unsigned int blackPatternValue, whitePatternValue;
    
    blackPatternValue=(unsigned int) atoi(strBlackPatternValue);
    whitePatternValue=(unsigned int) atoi(strWhitePatternValue);
    
    saveUint(EEPROM_ADDRESS_BLACK_PATTERN_15_VALUE,blackPatternValue);
    saveUint(EEPROM_ADDRESS_WHITE_PATTERN_15_VALUE,whitePatternValue);
}

void writeFunction30Params(char *strBlackPatternValue, char *strWhitePatternValue){
    unsigned int blackPatternValue, whitePatternValue;
    
    blackPatternValue=(unsigned int) atoi(strBlackPatternValue);
    whitePatternValue=(unsigned int) atoi(strWhitePatternValue);
    
    saveUint(EEPROM_ADDRESS_BLACK_PATTERN_30_VALUE,blackPatternValue);
    saveUint(EEPROM_ADDRESS_WHITE_PATTERN_30_VALUE,whitePatternValue);
}


