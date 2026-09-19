#include "LCD1.h"

//   Funciones de MyLCD   ///
void Com_MyLCD(char MyComand)
{
    char Com_MSB,Com_LSB,Com_MSBE,Com_LSBE;
    Com_MSB=MyComand&0xF0;       //MSB con E en 0
    Com_LSB=(MyComand<<4)&0xF0;  //LSB con E en 0
    Com_MSBE=Com_MSB|0x04;       //MSB con E en 1
    Com_LSBE=Com_MSB|0x04;       //LSB Con E en 0
    Port_MyLCD=Com_MSBE;         //ENVÍO MSB del comando con E en 1
    delay_MyLCD_m();
    Port_MyLCD=Com_MSB;          // E a o
    Port_MyLCD=Com_LSBE;         //ENVÍO LSB del comando con E en 1
    delay_MyLCD_m();
    Port_MyLCD=Com_LSB;          // E a 0
    
}

void Init_MyLCD ()
{
    Tris_MYLCD=0;
    Port_MyLCD=0;
    for(char x=0;x<4;x++)
        delay_MyLCD_m();
    Port_MyLCD=0X34;
    delay_MyLCD_m();
    Port_MyLCD=0X30;
    Port_MyLCD=0X34;
    delay_MyLCD_m();
    Port_MyLCD=0X30;
    Port_MyLCD=0X34;
     delay_MyLCD_m();
    Port_MyLCD=0X30;
    Port_MyLCD=0X24;
    delay_MyLCD_m();  
    Port_MyLCD=0X20;
    
    Com_MyLCD(0x28);
    Com_MyLCD(0x0C);
    Com_MyLCD(0x06);
}

void Char_MyLCD(char MyChar)
{
    char Char_MSB,Char_LSB,Char_MSBE,Char_LSBE;
    Char_MSB=(MyChar&0xF0)|0x01; //MSB con E en 0
    Char_LSB=((MyChar<<4)&0xF0)|0x01; //LSB con E en 0
    Char_MSBE=Char_MSB|0x05; //MSB con E en 1
    Char_LSBE=Char_MSB|0x05; //LSB Con E en 0
    Port_MyLCD=Char_MSBE;//ENVÍO MSB del comando con E en 1
    delay_MyLCD_u();
    Port_MyLCD=Char_MSB;// E a o
    Port_MyLCD=Char_LSBE;//ENVÍO LSB del comando con E en 1
    delay_MyLCD_u();
    Port_MyLCD=Char_LSB;// E a 0
    
}

void SetCur_MyLCD(char li, char car)
{
    char direc;
    direc=0x80+(li-1)*0x40+(car-1); //10000000
    Com_MyLCD(direc);
}

void String_MyLCD(char *a)
{
    for (char j=0; a[j]!=0X00;j++)
        Char_MyLCD(a[j]);
}