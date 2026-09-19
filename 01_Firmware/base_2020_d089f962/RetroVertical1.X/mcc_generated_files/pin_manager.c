
#include "pin_manager.h"





void PIN_MANAGER_Initialize(void)
{
    /**
    LATx registers
    */
    LATE = 0x00;
    LATD = 0x00;
    LATA = 0x00;
    LATB = 0x00;
    LATC = 0x00;

    /**
    TRISx registers
    */
    TRISE = 0x03;
    TRISA = 0xFF;
    TRISB = 0xFE;
    TRISC = 0x81;
    TRISD = 0x00;

    /**
    ANSELx registers
    */
    ANSELD = 0x00;
    ANSELC = 0x30;
    ANSELB = 0xFC;
    ANSELE = 0x04;
    ANSELA = 0xFF;

    /**
    WPUx registers
    */
    WPUD = 0x01;
    WPUE = 0x03;
    WPUB = 0x00;
    WPUA = 0x00;
    WPUC = 0x80;

    /**
    RxyI2C registers
    */
    RB1I2C = 0x00;
    RB2I2C = 0x00;
    RC3I2C = 0x00;
    RC4I2C = 0x00;
    RD0I2C = 0x00;
    RD1I2C = 0x00;

    /**
    ODx registers
    */
    ODCONE = 0x00;
    ODCONA = 0x00;
    ODCONB = 0x00;
    ODCONC = 0x00;
    ODCOND = 0x00;

    /**
    SLRCONx registers
    */
    SLRCONA = 0xFF;
    SLRCONB = 0xFF;
    SLRCONC = 0xFF;
    SLRCOND = 0xFF;
    SLRCONE = 0x07;

    /**
    INLVLx registers
    */
    INLVLA = 0xFF;
    INLVLB = 0xFF;
    INLVLC = 0xFF;
    INLVLD = 0xFF;
    INLVLE = 0x0F;





   
    
	
    U2RXPPS = 0x09;   //RB1->UART2:RX2;    
    RB0PPS = 0x16;   //RB0->UART2:TX2;    
    RC1PPS = 0x13;   //RC1->UART1:TX1;    
    RC4PPS = 0x0F;   //RC4->PWM7:PWM7;    
    RC5PPS = 0x0D;   //RC5->PWM5:PWM5;    
    U1RXPPS = 0x10;   //RC0->UART1:RX1;    
}
  
void PIN_MANAGER_IOC(void)
{   
}

/**
 End of File
*/