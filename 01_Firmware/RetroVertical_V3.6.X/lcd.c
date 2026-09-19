#include "lcd.h"

void lcdInit(void) {
    //init MCU pins (set as output)
    RS_PIN;
    EN_PIN;
    D4_PIN;
    D5_PIN;
    D6_PIN;
    D7_PIN;

    //init LCD controller
    INTERRUPT_GlobalInterruptDisable();
    __delay_ms(20); //delay on power up
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0011);
    INTERRUPT_GlobalInterruptDisable();
    __delay_ms(5); //wait for the instruction to complete
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0011);
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(200); //wait for the instruction to complete
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0011);
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(200);
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0010); //enable 4-bit mode
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(200);
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0010);
    lcdCmd(0b1000); //4-bit mode, 2-line, 5x8 font
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(50);
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0000);
    lcdCmd(0b1000); //display off
    INTERRUPT_GlobalInterruptDisable();
    __delay_ms(5);
    INTERRUPT_GlobalInterruptEnable();
    lcdClear();
    lcdCmd(0b0000);
    lcdCmd(0b0110); //entry mode set
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(50);
    INTERRUPT_GlobalInterruptEnable();
    lcdCmd(0b0000);
    lcdCmd(0b1100); //display on, cursor off, blink off
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(50);
    INTERRUPT_GlobalInterruptEnable();
    lcdGoto(1, 1);
}


///////////////////////////////////////////////////////////////////////////////

void lcdWrite(uint8_t data) //write data to LCD
{
    if (data & 1) D4_H;
    else D4_L;

    if (data & 2) D5_H;
    else D5_L;

    if (data & 4) D6_H;
    else D6_L;

    if (data & 8) D7_H;
    else D7_L;

    EN_H;
    INTERRUPT_GlobalInterruptDisable();
    __delay_us(100);
    INTERRUPT_GlobalInterruptEnable();
    EN_L;
}


///////////////////////////////////////////////////////////////////////////////

void lcdCmd(uint8_t data) //send command to LCD
{
    RS_L;
    lcdWrite(data);
}


///////////////////////////////////////////////////////////////////////////////

void lcdClear(void) //clear screen
{
    lcdCmd(0b0000);
    lcdCmd(0b0001);
    INTERRUPT_GlobalInterruptDisable();
    __delay_ms(5);
      INTERRUPT_GlobalInterruptEnable();
    
}


///////////////////////////////////////////////////////////////////////////////

void lcdGoto(uint8_t line, uint8_t column) //line 0..1, column 0..39
{
    lcdCmd(((0x80 + (line << 6)) + column) >> 4);
    lcdCmd((0x80 + (line << 6)) + column);
}


///////////////////////////////////////////////////////////////////////////////

void lcdChar(uint8_t sign) //print a character
{
    RS_H;
    lcdWrite(sign >> 4);
    lcdWrite(sign);
}


///////////////////////////////////////////////////////////////////////////////

void lcdPrint(const char *str) //print a string
{
    while (*str) lcdChar(*str++);
}


///////////////////////////////////////////////////////////////////////////////

void lcdRight(void) //shift right display 
{
    lcdCmd(0x01);
    lcdCmd(0x0C);
}


///////////////////////////////////////////////////////////////////////////////

void lcdLeft(void) //shift left display
{
    lcdCmd(0x01);
    lcdCmd(0x08);
}

