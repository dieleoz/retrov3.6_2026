
/* 
 * File:
 * Comments: El puerto a usar se conectará de la siguiente forma:
 * RS al Bit0
 * RW al Bit1
 * Enable al Bit 2
 * D4 al Bit 4
 * D5 al Bit 5
 * D6 al Bit 6
 * D7 al Bit 7
 * Revision history: 
 */

// This is a guard condition so that contents of this file are not included
// more than once.  

/*   en el main se debe crear las siguientes funciones
 
 void delay_MyLCD_m()
{
    __delay_ms(5);
}
void delay_MyLCD_u()
{
    __delay_us(40);
}
 * 
 */

#ifndef LCD_H
#define	LCD_H
#include <xc.h> // include processor files - each processor file is guarded.

#define Port_MyLCD   LATB               //definir el puerto en el cual se conectará la LCD
#define Tris_MYLCD   TRISB              //Definir el TRIS del puerto donde se conectará la LCD

void Com_MyLCD(char MyComand);          //Función para enviar comandos
void Init_MyLCD ();                     //inicializar la LCD
void Char_MyLCD(char MyChar);           //función para enviar un caracter
void SetCur_MyLCD(char li, char car);   //Función para posicionar el cursor
void String_MyLCD(char *a);             //Función para enviar cadena de caracteres

extern void delay_MyLCD_m();
extern void delay_MyLCD_u();


#endif	/* MyLCD_H */

