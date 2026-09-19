#ifndef LCD_H
#define LCD_H

 //#define _XTAL_FREQ 8000000

#include "mcc_generated_files/mcc.h"
#include <xc.h>


//#define FOSC    (8000000ULL)
//#define FCY     (FOSC/2)



// define LCD lines connected to MCU's pins (can be used any pins)

// LCD line RW connected to Vss (ground)

// LCD line RS connected to RD6 pin
#define RS_PIN  LCD_RS_SetDigitalOutput() //TRISD6=0
#define RS_H    LCD_RS_SetHigh()  //RD6=1
#define RS_L    LCD_RS_SetLow() //RD6=0

// LCD line E connected to RD7 pin
#define EN_PIN  LCD_E_SetDigitalOutput() //TRISD7=0
#define EN_H    LCD_E_SetHigh() //RD7=1
#define EN_L    LCD_E_SetLow() //RD7=0

// LCD data bus (4-bit interface)
// data line 4..7 connected to RB0..RB3 pins
#define D4_PIN  LCD_D4_SetDigitalOutput() // TRISB0=0
#define D4_H    LCD_D4_SetHigh()  //RB0=1
#define D4_L    LCD_D4_SetLow()    //RB0=0

#define D5_PIN  LCD_D5_SetDigitalOutput() //TRISB1=0
#define D5_H    LCD_D5_SetHigh()  //RB1=1
#define D5_L    LCD_D5_SetLow()  //RB1=0

#define D6_PIN  LCD_D6_SetDigitalOutput() //TRISB2=0
#define D6_H    LCD_D6_SetHigh()  //RB2=1
#define D6_L    LCD_D6_SetLow() //RB2=0

#define D7_PIN  LCD_D7_SetDigitalOutput() //TRISB3=0
#define D7_H    LCD_D7_SetHigh()  //RB3=1
#define D7_L    LCD_D7_SetLow() //RB3=0

// LCD backlight line connected to RD5 pin
//#define BL_PIN  TRISD5=0
//#define BL_ON   RD5=1
//#define BL_OFF  RD5=0


typedef unsigned char uint8_t; //stdint


void lcdInit(void);
void lcdWrite(uint8_t data);
void lcdCmd(uint8_t data);
void lcdClear(void);
void lcdGoto(uint8_t line, uint8_t column);
void lcdChar(uint8_t sign);
void lcdPrint(const char *str);
void lcdRight(void);
void lcdLeft(void);



#endif