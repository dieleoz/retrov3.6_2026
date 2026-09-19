

#ifndef PIN_MANAGER_H
#define PIN_MANAGER_H

/**
  Section: Included Files
*/

#include <xc.h>

#define INPUT   1
#define OUTPUT  0

#define HIGH    1
#define LOW     0

#define ANALOG      1
#define DIGITAL     0

#define PULL_UP_ENABLED      1
#define PULL_UP_DISABLED     0

// get/set INREFLECT30 aliases
#define INREFLECT30_TRIS                 TRISAbits.TRISA0
#define INREFLECT30_LAT                  LATAbits.LATA0
#define INREFLECT30_PORT                 PORTAbits.RA0
#define INREFLECT30_WPU                  WPUAbits.WPUA0
#define INREFLECT30_OD                   ODCONAbits.ODCA0
#define INREFLECT30_ANS                  ANSELAbits.ANSELA0
#define INREFLECT30_SetHigh()            do { LATAbits.LATA0 = 1; } while(0)
#define INREFLECT30_SetLow()             do { LATAbits.LATA0 = 0; } while(0)
#define INREFLECT30_Toggle()             do { LATAbits.LATA0 = ~LATAbits.LATA0; } while(0)
#define INREFLECT30_GetValue()           PORTAbits.RA0
#define INREFLECT30_SetDigitalInput()    do { TRISAbits.TRISA0 = 1; } while(0)
#define INREFLECT30_SetDigitalOutput()   do { TRISAbits.TRISA0 = 0; } while(0)
#define INREFLECT30_SetPullup()          do { WPUAbits.WPUA0 = 1; } while(0)
#define INREFLECT30_ResetPullup()        do { WPUAbits.WPUA0 = 0; } while(0)
#define INREFLECT30_SetPushPull()        do { ODCONAbits.ODCA0 = 0; } while(0)
#define INREFLECT30_SetOpenDrain()       do { ODCONAbits.ODCA0 = 1; } while(0)
#define INREFLECT30_SetAnalogMode()      do { ANSELAbits.ANSELA0 = 1; } while(0)
#define INREFLECT30_SetDigitalMode()     do { ANSELAbits.ANSELA0 = 0; } while(0)

// get/set INREFLECT15 aliases
#define INREFLECT15_TRIS                 TRISAbits.TRISA1
#define INREFLECT15_LAT                  LATAbits.LATA1
#define INREFLECT15_PORT                 PORTAbits.RA1
#define INREFLECT15_WPU                  WPUAbits.WPUA1
#define INREFLECT15_OD                   ODCONAbits.ODCA1
#define INREFLECT15_ANS                  ANSELAbits.ANSELA1
#define INREFLECT15_SetHigh()            do { LATAbits.LATA1 = 1; } while(0)
#define INREFLECT15_SetLow()             do { LATAbits.LATA1 = 0; } while(0)
#define INREFLECT15_Toggle()             do { LATAbits.LATA1 = ~LATAbits.LATA1; } while(0)
#define INREFLECT15_GetValue()           PORTAbits.RA1
#define INREFLECT15_SetDigitalInput()    do { TRISAbits.TRISA1 = 1; } while(0)
#define INREFLECT15_SetDigitalOutput()   do { TRISAbits.TRISA1 = 0; } while(0)
#define INREFLECT15_SetPullup()          do { WPUAbits.WPUA1 = 1; } while(0)
#define INREFLECT15_ResetPullup()        do { WPUAbits.WPUA1 = 0; } while(0)
#define INREFLECT15_SetPushPull()        do { ODCONAbits.ODCA1 = 0; } while(0)
#define INREFLECT15_SetOpenDrain()       do { ODCONAbits.ODCA1 = 1; } while(0)
#define INREFLECT15_SetAnalogMode()      do { ANSELAbits.ANSELA1 = 1; } while(0)
#define INREFLECT15_SetDigitalMode()     do { ANSELAbits.ANSELA1 = 0; } while(0)

// get/set RA3 procedures
#define RA3_SetHigh()            do { LATAbits.LATA3 = 1; } while(0)
#define RA3_SetLow()             do { LATAbits.LATA3 = 0; } while(0)
#define RA3_Toggle()             do { LATAbits.LATA3 = ~LATAbits.LATA3; } while(0)
#define RA3_GetValue()              PORTAbits.RA3
#define RA3_SetDigitalInput()    do { TRISAbits.TRISA3 = 1; } while(0)
#define RA3_SetDigitalOutput()   do { TRISAbits.TRISA3 = 0; } while(0)
#define RA3_SetPullup()             do { WPUAbits.WPUA3 = 1; } while(0)
#define RA3_ResetPullup()           do { WPUAbits.WPUA3 = 0; } while(0)
#define RA3_SetAnalogMode()         do { ANSELAbits.ANSELA3 = 1; } while(0)
#define RA3_SetDigitalMode()        do { ANSELAbits.ANSELA3 = 0; } while(0)

// get/set TEMLOCAL1 aliases
#define TEMLOCAL1_TRIS                 TRISAbits.TRISA4
#define TEMLOCAL1_LAT                  LATAbits.LATA4
#define TEMLOCAL1_PORT                 PORTAbits.RA4
#define TEMLOCAL1_WPU                  WPUAbits.WPUA4
#define TEMLOCAL1_OD                   ODCONAbits.ODCA4
#define TEMLOCAL1_ANS                  ANSELAbits.ANSELA4
#define TEMLOCAL1_SetHigh()            do { LATAbits.LATA4 = 1; } while(0)
#define TEMLOCAL1_SetLow()             do { LATAbits.LATA4 = 0; } while(0)
#define TEMLOCAL1_Toggle()             do { LATAbits.LATA4 = ~LATAbits.LATA4; } while(0)
#define TEMLOCAL1_GetValue()           PORTAbits.RA4
#define TEMLOCAL1_SetDigitalInput()    do { TRISAbits.TRISA4 = 1; } while(0)
#define TEMLOCAL1_SetDigitalOutput()   do { TRISAbits.TRISA4 = 0; } while(0)
#define TEMLOCAL1_SetPullup()          do { WPUAbits.WPUA4 = 1; } while(0)
#define TEMLOCAL1_ResetPullup()        do { WPUAbits.WPUA4 = 0; } while(0)
#define TEMLOCAL1_SetPushPull()        do { ODCONAbits.ODCA4 = 0; } while(0)
#define TEMLOCAL1_SetOpenDrain()       do { ODCONAbits.ODCA4 = 1; } while(0)
#define TEMLOCAL1_SetAnalogMode()      do { ANSELAbits.ANSELA4 = 1; } while(0)
#define TEMLOCAL1_SetDigitalMode()     do { ANSELAbits.ANSELA4 = 0; } while(0)

// get/set TEMLOCAL2 aliases
#define TEMLOCAL2_TRIS                 TRISAbits.TRISA5
#define TEMLOCAL2_LAT                  LATAbits.LATA5
#define TEMLOCAL2_PORT                 PORTAbits.RA5
#define TEMLOCAL2_WPU                  WPUAbits.WPUA5
#define TEMLOCAL2_OD                   ODCONAbits.ODCA5
#define TEMLOCAL2_ANS                  ANSELAbits.ANSELA5
#define TEMLOCAL2_SetHigh()            do { LATAbits.LATA5 = 1; } while(0)
#define TEMLOCAL2_SetLow()             do { LATAbits.LATA5 = 0; } while(0)
#define TEMLOCAL2_Toggle()             do { LATAbits.LATA5 = ~LATAbits.LATA5; } while(0)
#define TEMLOCAL2_GetValue()           PORTAbits.RA5
#define TEMLOCAL2_SetDigitalInput()    do { TRISAbits.TRISA5 = 1; } while(0)
#define TEMLOCAL2_SetDigitalOutput()   do { TRISAbits.TRISA5 = 0; } while(0)
#define TEMLOCAL2_SetPullup()          do { WPUAbits.WPUA5 = 1; } while(0)
#define TEMLOCAL2_ResetPullup()        do { WPUAbits.WPUA5 = 0; } while(0)
#define TEMLOCAL2_SetPushPull()        do { ODCONAbits.ODCA5 = 0; } while(0)
#define TEMLOCAL2_SetOpenDrain()       do { ODCONAbits.ODCA5 = 1; } while(0)
#define TEMLOCAL2_SetAnalogMode()      do { ANSELAbits.ANSELA5 = 1; } while(0)
#define TEMLOCAL2_SetDigitalMode()     do { ANSELAbits.ANSELA5 = 0; } while(0)

// get/set BATERIA aliases
#define BATERIA_TRIS                 TRISAbits.TRISA6
#define BATERIA_LAT                  LATAbits.LATA6
#define BATERIA_PORT                 PORTAbits.RA6
#define BATERIA_WPU                  WPUAbits.WPUA6
#define BATERIA_OD                   ODCONAbits.ODCA6
#define BATERIA_ANS                  ANSELAbits.ANSELA6
#define BATERIA_SetHigh()            do { LATAbits.LATA6 = 1; } while(0)
#define BATERIA_SetLow()             do { LATAbits.LATA6 = 0; } while(0)
#define BATERIA_Toggle()             do { LATAbits.LATA6 = ~LATAbits.LATA6; } while(0)
#define BATERIA_GetValue()           PORTAbits.RA6
#define BATERIA_SetDigitalInput()    do { TRISAbits.TRISA6 = 1; } while(0)
#define BATERIA_SetDigitalOutput()   do { TRISAbits.TRISA6 = 0; } while(0)
#define BATERIA_SetPullup()          do { WPUAbits.WPUA6 = 1; } while(0)
#define BATERIA_ResetPullup()        do { WPUAbits.WPUA6 = 0; } while(0)
#define BATERIA_SetPushPull()        do { ODCONAbits.ODCA6 = 0; } while(0)
#define BATERIA_SetOpenDrain()       do { ODCONAbits.ODCA6 = 1; } while(0)
#define BATERIA_SetAnalogMode()      do { ANSELAbits.ANSELA6 = 1; } while(0)
#define BATERIA_SetDigitalMode()     do { ANSELAbits.ANSELA6 = 0; } while(0)

// get/set RB0 procedures
#define RB0_SetHigh()            do { LATBbits.LATB0 = 1; } while(0)
#define RB0_SetLow()             do { LATBbits.LATB0 = 0; } while(0)
#define RB0_Toggle()             do { LATBbits.LATB0 = ~LATBbits.LATB0; } while(0)
#define RB0_GetValue()              PORTBbits.RB0
#define RB0_SetDigitalInput()    do { TRISBbits.TRISB0 = 1; } while(0)
#define RB0_SetDigitalOutput()   do { TRISBbits.TRISB0 = 0; } while(0)
#define RB0_SetPullup()             do { WPUBbits.WPUB0 = 1; } while(0)
#define RB0_ResetPullup()           do { WPUBbits.WPUB0 = 0; } while(0)
#define RB0_SetAnalogMode()         do { ANSELBbits.ANSELB0 = 1; } while(0)
#define RB0_SetDigitalMode()        do { ANSELBbits.ANSELB0 = 0; } while(0)

// get/set RB1 procedures
#define RB1_SetHigh()            do { LATBbits.LATB1 = 1; } while(0)
#define RB1_SetLow()             do { LATBbits.LATB1 = 0; } while(0)
#define RB1_Toggle()             do { LATBbits.LATB1 = ~LATBbits.LATB1; } while(0)
#define RB1_GetValue()              PORTBbits.RB1
#define RB1_SetDigitalInput()    do { TRISBbits.TRISB1 = 1; } while(0)
#define RB1_SetDigitalOutput()   do { TRISBbits.TRISB1 = 0; } while(0)
#define RB1_SetPullup()             do { WPUBbits.WPUB1 = 1; } while(0)
#define RB1_ResetPullup()           do { WPUBbits.WPUB1 = 0; } while(0)
#define RB1_SetAnalogMode()         do { ANSELBbits.ANSELB1 = 1; } while(0)
#define RB1_SetDigitalMode()        do { ANSELBbits.ANSELB1 = 0; } while(0)

// get/set RC0 procedures
#define RC0_SetHigh()            do { LATCbits.LATC0 = 1; } while(0)
#define RC0_SetLow()             do { LATCbits.LATC0 = 0; } while(0)
#define RC0_Toggle()             do { LATCbits.LATC0 = ~LATCbits.LATC0; } while(0)
#define RC0_GetValue()              PORTCbits.RC0
#define RC0_SetDigitalInput()    do { TRISCbits.TRISC0 = 1; } while(0)
#define RC0_SetDigitalOutput()   do { TRISCbits.TRISC0 = 0; } while(0)
#define RC0_SetPullup()             do { WPUCbits.WPUC0 = 1; } while(0)
#define RC0_ResetPullup()           do { WPUCbits.WPUC0 = 0; } while(0)
#define RC0_SetAnalogMode()         do { ANSELCbits.ANSELC0 = 1; } while(0)
#define RC0_SetDigitalMode()        do { ANSELCbits.ANSELC0 = 0; } while(0)

// get/set RC1 procedures
#define RC1_SetHigh()            do { LATCbits.LATC1 = 1; } while(0)
#define RC1_SetLow()             do { LATCbits.LATC1 = 0; } while(0)
#define RC1_Toggle()             do { LATCbits.LATC1 = ~LATCbits.LATC1; } while(0)
#define RC1_GetValue()              PORTCbits.RC1
#define RC1_SetDigitalInput()    do { TRISCbits.TRISC1 = 1; } while(0)
#define RC1_SetDigitalOutput()   do { TRISCbits.TRISC1 = 0; } while(0)
#define RC1_SetPullup()             do { WPUCbits.WPUC1 = 1; } while(0)
#define RC1_ResetPullup()           do { WPUCbits.WPUC1 = 0; } while(0)
#define RC1_SetAnalogMode()         do { ANSELCbits.ANSELC1 = 1; } while(0)
#define RC1_SetDigitalMode()        do { ANSELCbits.ANSELC1 = 0; } while(0)

// get/set SALIDA15 aliases
#define SALIDA15_TRIS                 TRISCbits.TRISC2
#define SALIDA15_LAT                  LATCbits.LATC2
#define SALIDA15_PORT                 PORTCbits.RC2
#define SALIDA15_WPU                  WPUCbits.WPUC2
#define SALIDA15_OD                   ODCONCbits.ODCC2
#define SALIDA15_ANS                  ANSELCbits.ANSELC2
#define SALIDA15_SetHigh()            do { LATCbits.LATC2 = 1; } while(0)
#define SALIDA15_SetLow()             do { LATCbits.LATC2 = 0; } while(0)
#define SALIDA15_Toggle()             do { LATCbits.LATC2 = ~LATCbits.LATC2; } while(0)
#define SALIDA15_GetValue()           PORTCbits.RC2
#define SALIDA15_SetDigitalInput()    do { TRISCbits.TRISC2 = 1; } while(0)
#define SALIDA15_SetDigitalOutput()   do { TRISCbits.TRISC2 = 0; } while(0)
#define SALIDA15_SetPullup()          do { WPUCbits.WPUC2 = 1; } while(0)
#define SALIDA15_ResetPullup()        do { WPUCbits.WPUC2 = 0; } while(0)
#define SALIDA15_SetPushPull()        do { ODCONCbits.ODCC2 = 0; } while(0)
#define SALIDA15_SetOpenDrain()       do { ODCONCbits.ODCC2 = 1; } while(0)
#define SALIDA15_SetAnalogMode()      do { ANSELCbits.ANSELC2 = 1; } while(0)
#define SALIDA15_SetDigitalMode()     do { ANSELCbits.ANSELC2 = 0; } while(0)

// get/set SALIDA30 aliases
#define SALIDA30_TRIS                 TRISCbits.TRISC3
#define SALIDA30_LAT                  LATCbits.LATC3
#define SALIDA30_PORT                 PORTCbits.RC3
#define SALIDA30_WPU                  WPUCbits.WPUC3
#define SALIDA30_OD                   ODCONCbits.ODCC3
#define SALIDA30_ANS                  ANSELCbits.ANSELC3
#define SALIDA30_SetHigh()            do { LATCbits.LATC3 = 1; } while(0)
#define SALIDA30_SetLow()             do { LATCbits.LATC3 = 0; } while(0)
#define SALIDA30_Toggle()             do { LATCbits.LATC3 = ~LATCbits.LATC3; } while(0)
#define SALIDA30_GetValue()           PORTCbits.RC3
#define SALIDA30_SetDigitalInput()    do { TRISCbits.TRISC3 = 1; } while(0)
#define SALIDA30_SetDigitalOutput()   do { TRISCbits.TRISC3 = 0; } while(0)
#define SALIDA30_SetPullup()          do { WPUCbits.WPUC3 = 1; } while(0)
#define SALIDA30_ResetPullup()        do { WPUCbits.WPUC3 = 0; } while(0)
#define SALIDA30_SetPushPull()        do { ODCONCbits.ODCC3 = 0; } while(0)
#define SALIDA30_SetOpenDrain()       do { ODCONCbits.ODCC3 = 1; } while(0)
#define SALIDA30_SetAnalogMode()      do { ANSELCbits.ANSELC3 = 1; } while(0)
#define SALIDA30_SetDigitalMode()     do { ANSELCbits.ANSELC3 = 0; } while(0)

// get/set RC4 procedures
#define RC4_SetHigh()            do { LATCbits.LATC4 = 1; } while(0)
#define RC4_SetLow()             do { LATCbits.LATC4 = 0; } while(0)
#define RC4_Toggle()             do { LATCbits.LATC4 = ~LATCbits.LATC4; } while(0)
#define RC4_GetValue()              PORTCbits.RC4
#define RC4_SetDigitalInput()    do { TRISCbits.TRISC4 = 1; } while(0)
#define RC4_SetDigitalOutput()   do { TRISCbits.TRISC4 = 0; } while(0)
#define RC4_SetPullup()             do { WPUCbits.WPUC4 = 1; } while(0)
#define RC4_ResetPullup()           do { WPUCbits.WPUC4 = 0; } while(0)
#define RC4_SetAnalogMode()         do { ANSELCbits.ANSELC4 = 1; } while(0)
#define RC4_SetDigitalMode()        do { ANSELCbits.ANSELC4 = 0; } while(0)

// get/set RC5 procedures
#define RC5_SetHigh()            do { LATCbits.LATC5 = 1; } while(0)
#define RC5_SetLow()             do { LATCbits.LATC5 = 0; } while(0)
#define RC5_Toggle()             do { LATCbits.LATC5 = ~LATCbits.LATC5; } while(0)
#define RC5_GetValue()              PORTCbits.RC5
#define RC5_SetDigitalInput()    do { TRISCbits.TRISC5 = 1; } while(0)
#define RC5_SetDigitalOutput()   do { TRISCbits.TRISC5 = 0; } while(0)
#define RC5_SetPullup()             do { WPUCbits.WPUC5 = 1; } while(0)
#define RC5_ResetPullup()           do { WPUCbits.WPUC5 = 0; } while(0)
#define RC5_SetAnalogMode()         do { ANSELCbits.ANSELC5 = 1; } while(0)
#define RC5_SetDigitalMode()        do { ANSELCbits.ANSELC5 = 0; } while(0)

// get/set LED_ROJO aliases
#define LED_ROJO_TRIS                 TRISCbits.TRISC6
#define LED_ROJO_LAT                  LATCbits.LATC6
#define LED_ROJO_PORT                 PORTCbits.RC6
#define LED_ROJO_WPU                  WPUCbits.WPUC6
#define LED_ROJO_OD                   ODCONCbits.ODCC6
#define LED_ROJO_ANS                  ANSELCbits.ANSELC6
#define LED_ROJO_SetHigh()            do { LATCbits.LATC6 = 1; } while(0)
#define LED_ROJO_SetLow()             do { LATCbits.LATC6 = 0; } while(0)
#define LED_ROJO_Toggle()             do { LATCbits.LATC6 = ~LATCbits.LATC6; } while(0)
#define LED_ROJO_GetValue()           PORTCbits.RC6
#define LED_ROJO_SetDigitalInput()    do { TRISCbits.TRISC6 = 1; } while(0)
#define LED_ROJO_SetDigitalOutput()   do { TRISCbits.TRISC6 = 0; } while(0)
#define LED_ROJO_SetPullup()          do { WPUCbits.WPUC6 = 1; } while(0)
#define LED_ROJO_ResetPullup()        do { WPUCbits.WPUC6 = 0; } while(0)
#define LED_ROJO_SetPushPull()        do { ODCONCbits.ODCC6 = 0; } while(0)
#define LED_ROJO_SetOpenDrain()       do { ODCONCbits.ODCC6 = 1; } while(0)
#define LED_ROJO_SetAnalogMode()      do { ANSELCbits.ANSELC6 = 1; } while(0)
#define LED_ROJO_SetDigitalMode()     do { ANSELCbits.ANSELC6 = 0; } while(0)

// get/set CARGANDO aliases
#define CARGANDO_TRIS                 TRISCbits.TRISC7
#define CARGANDO_LAT                  LATCbits.LATC7
#define CARGANDO_PORT                 PORTCbits.RC7
#define CARGANDO_WPU                  WPUCbits.WPUC7
#define CARGANDO_OD                   ODCONCbits.ODCC7
#define CARGANDO_ANS                  ANSELCbits.ANSELC7
#define CARGANDO_SetHigh()            do { LATCbits.LATC7 = 1; } while(0)
#define CARGANDO_SetLow()             do { LATCbits.LATC7 = 0; } while(0)
#define CARGANDO_Toggle()             do { LATCbits.LATC7 = ~LATCbits.LATC7; } while(0)
#define CARGANDO_GetValue()           PORTCbits.RC7
#define CARGANDO_SetDigitalInput()    do { TRISCbits.TRISC7 = 1; } while(0)
#define CARGANDO_SetDigitalOutput()   do { TRISCbits.TRISC7 = 0; } while(0)
#define CARGANDO_SetPullup()          do { WPUCbits.WPUC7 = 1; } while(0)
#define CARGANDO_ResetPullup()        do { WPUCbits.WPUC7 = 0; } while(0)
#define CARGANDO_SetPushPull()        do { ODCONCbits.ODCC7 = 0; } while(0)
#define CARGANDO_SetOpenDrain()       do { ODCONCbits.ODCC7 = 1; } while(0)
#define CARGANDO_SetAnalogMode()      do { ANSELCbits.ANSELC7 = 1; } while(0)
#define CARGANDO_SetDigitalMode()     do { ANSELCbits.ANSELC7 = 0; } while(0)

// get/set DHT11 aliases
#define DHT11_TRIS                 TRISDbits.TRISD0
#define DHT11_LAT                  LATDbits.LATD0
#define DHT11_PORT                 PORTDbits.RD0
#define DHT11_WPU                  WPUDbits.WPUD0
#define DHT11_OD                   ODCONDbits.ODCD0
#define DHT11_ANS                  ANSELDbits.ANSELD0
#define DHT11_SetHigh()            do { LATDbits.LATD0 = 1; } while(0)
#define DHT11_SetLow()             do { LATDbits.LATD0 = 0; } while(0)
#define DHT11_Toggle()             do { LATDbits.LATD0 = ~LATDbits.LATD0; } while(0)
#define DHT11_GetValue()           PORTDbits.RD0
#define DHT11_SetDigitalInput()    do { TRISDbits.TRISD0 = 1; } while(0)
#define DHT11_SetDigitalOutput()   do { TRISDbits.TRISD0 = 0; } while(0)
#define DHT11_SetPullup()          do { WPUDbits.WPUD0 = 1; } while(0)
#define DHT11_ResetPullup()        do { WPUDbits.WPUD0 = 0; } while(0)
#define DHT11_SetPushPull()        do { ODCONDbits.ODCD0 = 0; } while(0)
#define DHT11_SetOpenDrain()       do { ODCONDbits.ODCD0 = 1; } while(0)
#define DHT11_SetAnalogMode()      do { ANSELDbits.ANSELD0 = 1; } while(0)
#define DHT11_SetDigitalMode()     do { ANSELDbits.ANSELD0 = 0; } while(0)

// get/set BUZZER aliases
#define BUZZER_TRIS                 TRISDbits.TRISD1
#define BUZZER_LAT                  LATDbits.LATD1
#define BUZZER_PORT                 PORTDbits.RD1
#define BUZZER_WPU                  WPUDbits.WPUD1
#define BUZZER_OD                   ODCONDbits.ODCD1
#define BUZZER_ANS                  ANSELDbits.ANSELD1
#define BUZZER_SetHigh()            do { LATDbits.LATD1 = 1; } while(0)
#define BUZZER_SetLow()             do { LATDbits.LATD1 = 0; } while(0)
#define BUZZER_Toggle()             do { LATDbits.LATD1 = ~LATDbits.LATD1; } while(0)
#define BUZZER_GetValue()           PORTDbits.RD1
#define BUZZER_SetDigitalInput()    do { TRISDbits.TRISD1 = 1; } while(0)
#define BUZZER_SetDigitalOutput()   do { TRISDbits.TRISD1 = 0; } while(0)
#define BUZZER_SetPullup()          do { WPUDbits.WPUD1 = 1; } while(0)
#define BUZZER_ResetPullup()        do { WPUDbits.WPUD1 = 0; } while(0)
#define BUZZER_SetPushPull()        do { ODCONDbits.ODCD1 = 0; } while(0)
#define BUZZER_SetOpenDrain()       do { ODCONDbits.ODCD1 = 1; } while(0)
#define BUZZER_SetAnalogMode()      do { ANSELDbits.ANSELD1 = 1; } while(0)
#define BUZZER_SetDigitalMode()     do { ANSELDbits.ANSELD1 = 0; } while(0)

// get/set LCD_RS aliases
#define LCD_RS_TRIS                 TRISDbits.TRISD2
#define LCD_RS_LAT                  LATDbits.LATD2
#define LCD_RS_PORT                 PORTDbits.RD2
#define LCD_RS_WPU                  WPUDbits.WPUD2
#define LCD_RS_OD                   ODCONDbits.ODCD2
#define LCD_RS_ANS                  ANSELDbits.ANSELD2
#define LCD_RS_SetHigh()            do { LATDbits.LATD2 = 1; } while(0)
#define LCD_RS_SetLow()             do { LATDbits.LATD2 = 0; } while(0)
#define LCD_RS_Toggle()             do { LATDbits.LATD2 = ~LATDbits.LATD2; } while(0)
#define LCD_RS_GetValue()           PORTDbits.RD2
#define LCD_RS_SetDigitalInput()    do { TRISDbits.TRISD2 = 1; } while(0)
#define LCD_RS_SetDigitalOutput()   do { TRISDbits.TRISD2 = 0; } while(0)
#define LCD_RS_SetPullup()          do { WPUDbits.WPUD2 = 1; } while(0)
#define LCD_RS_ResetPullup()        do { WPUDbits.WPUD2 = 0; } while(0)
#define LCD_RS_SetPushPull()        do { ODCONDbits.ODCD2 = 0; } while(0)
#define LCD_RS_SetOpenDrain()       do { ODCONDbits.ODCD2 = 1; } while(0)
#define LCD_RS_SetAnalogMode()      do { ANSELDbits.ANSELD2 = 1; } while(0)
#define LCD_RS_SetDigitalMode()     do { ANSELDbits.ANSELD2 = 0; } while(0)

// get/set LCD_E aliases
#define LCD_E_TRIS                 TRISDbits.TRISD3
#define LCD_E_LAT                  LATDbits.LATD3
#define LCD_E_PORT                 PORTDbits.RD3
#define LCD_E_WPU                  WPUDbits.WPUD3
#define LCD_E_OD                   ODCONDbits.ODCD3
#define LCD_E_ANS                  ANSELDbits.ANSELD3
#define LCD_E_SetHigh()            do { LATDbits.LATD3 = 1; } while(0)
#define LCD_E_SetLow()             do { LATDbits.LATD3 = 0; } while(0)
#define LCD_E_Toggle()             do { LATDbits.LATD3 = ~LATDbits.LATD3; } while(0)
#define LCD_E_GetValue()           PORTDbits.RD3
#define LCD_E_SetDigitalInput()    do { TRISDbits.TRISD3 = 1; } while(0)
#define LCD_E_SetDigitalOutput()   do { TRISDbits.TRISD3 = 0; } while(0)
#define LCD_E_SetPullup()          do { WPUDbits.WPUD3 = 1; } while(0)
#define LCD_E_ResetPullup()        do { WPUDbits.WPUD3 = 0; } while(0)
#define LCD_E_SetPushPull()        do { ODCONDbits.ODCD3 = 0; } while(0)
#define LCD_E_SetOpenDrain()       do { ODCONDbits.ODCD3 = 1; } while(0)
#define LCD_E_SetAnalogMode()      do { ANSELDbits.ANSELD3 = 1; } while(0)
#define LCD_E_SetDigitalMode()     do { ANSELDbits.ANSELD3 = 0; } while(0)

// get/set LCD_D4 aliases
#define LCD_D4_TRIS                 TRISDbits.TRISD4
#define LCD_D4_LAT                  LATDbits.LATD4
#define LCD_D4_PORT                 PORTDbits.RD4
#define LCD_D4_WPU                  WPUDbits.WPUD4
#define LCD_D4_OD                   ODCONDbits.ODCD4
#define LCD_D4_ANS                  ANSELDbits.ANSELD4
#define LCD_D4_SetHigh()            do { LATDbits.LATD4 = 1; } while(0)
#define LCD_D4_SetLow()             do { LATDbits.LATD4 = 0; } while(0)
#define LCD_D4_Toggle()             do { LATDbits.LATD4 = ~LATDbits.LATD4; } while(0)
#define LCD_D4_GetValue()           PORTDbits.RD4
#define LCD_D4_SetDigitalInput()    do { TRISDbits.TRISD4 = 1; } while(0)
#define LCD_D4_SetDigitalOutput()   do { TRISDbits.TRISD4 = 0; } while(0)
#define LCD_D4_SetPullup()          do { WPUDbits.WPUD4 = 1; } while(0)
#define LCD_D4_ResetPullup()        do { WPUDbits.WPUD4 = 0; } while(0)
#define LCD_D4_SetPushPull()        do { ODCONDbits.ODCD4 = 0; } while(0)
#define LCD_D4_SetOpenDrain()       do { ODCONDbits.ODCD4 = 1; } while(0)
#define LCD_D4_SetAnalogMode()      do { ANSELDbits.ANSELD4 = 1; } while(0)
#define LCD_D4_SetDigitalMode()     do { ANSELDbits.ANSELD4 = 0; } while(0)

// get/set LCD_D5 aliases
#define LCD_D5_TRIS                 TRISDbits.TRISD5
#define LCD_D5_LAT                  LATDbits.LATD5
#define LCD_D5_PORT                 PORTDbits.RD5
#define LCD_D5_WPU                  WPUDbits.WPUD5
#define LCD_D5_OD                   ODCONDbits.ODCD5
#define LCD_D5_ANS                  ANSELDbits.ANSELD5
#define LCD_D5_SetHigh()            do { LATDbits.LATD5 = 1; } while(0)
#define LCD_D5_SetLow()             do { LATDbits.LATD5 = 0; } while(0)
#define LCD_D5_Toggle()             do { LATDbits.LATD5 = ~LATDbits.LATD5; } while(0)
#define LCD_D5_GetValue()           PORTDbits.RD5
#define LCD_D5_SetDigitalInput()    do { TRISDbits.TRISD5 = 1; } while(0)
#define LCD_D5_SetDigitalOutput()   do { TRISDbits.TRISD5 = 0; } while(0)
#define LCD_D5_SetPullup()          do { WPUDbits.WPUD5 = 1; } while(0)
#define LCD_D5_ResetPullup()        do { WPUDbits.WPUD5 = 0; } while(0)
#define LCD_D5_SetPushPull()        do { ODCONDbits.ODCD5 = 0; } while(0)
#define LCD_D5_SetOpenDrain()       do { ODCONDbits.ODCD5 = 1; } while(0)
#define LCD_D5_SetAnalogMode()      do { ANSELDbits.ANSELD5 = 1; } while(0)
#define LCD_D5_SetDigitalMode()     do { ANSELDbits.ANSELD5 = 0; } while(0)

// get/set LCD_D6 aliases
#define LCD_D6_TRIS                 TRISDbits.TRISD6
#define LCD_D6_LAT                  LATDbits.LATD6
#define LCD_D6_PORT                 PORTDbits.RD6
#define LCD_D6_WPU                  WPUDbits.WPUD6
#define LCD_D6_OD                   ODCONDbits.ODCD6
#define LCD_D6_ANS                  ANSELDbits.ANSELD6
#define LCD_D6_SetHigh()            do { LATDbits.LATD6 = 1; } while(0)
#define LCD_D6_SetLow()             do { LATDbits.LATD6 = 0; } while(0)
#define LCD_D6_Toggle()             do { LATDbits.LATD6 = ~LATDbits.LATD6; } while(0)
#define LCD_D6_GetValue()           PORTDbits.RD6
#define LCD_D6_SetDigitalInput()    do { TRISDbits.TRISD6 = 1; } while(0)
#define LCD_D6_SetDigitalOutput()   do { TRISDbits.TRISD6 = 0; } while(0)
#define LCD_D6_SetPullup()          do { WPUDbits.WPUD6 = 1; } while(0)
#define LCD_D6_ResetPullup()        do { WPUDbits.WPUD6 = 0; } while(0)
#define LCD_D6_SetPushPull()        do { ODCONDbits.ODCD6 = 0; } while(0)
#define LCD_D6_SetOpenDrain()       do { ODCONDbits.ODCD6 = 1; } while(0)
#define LCD_D6_SetAnalogMode()      do { ANSELDbits.ANSELD6 = 1; } while(0)
#define LCD_D6_SetDigitalMode()     do { ANSELDbits.ANSELD6 = 0; } while(0)

// get/set LCD_D7 aliases
#define LCD_D7_TRIS                 TRISDbits.TRISD7
#define LCD_D7_LAT                  LATDbits.LATD7
#define LCD_D7_PORT                 PORTDbits.RD7
#define LCD_D7_WPU                  WPUDbits.WPUD7
#define LCD_D7_OD                   ODCONDbits.ODCD7
#define LCD_D7_ANS                  ANSELDbits.ANSELD7
#define LCD_D7_SetHigh()            do { LATDbits.LATD7 = 1; } while(0)
#define LCD_D7_SetLow()             do { LATDbits.LATD7 = 0; } while(0)
#define LCD_D7_Toggle()             do { LATDbits.LATD7 = ~LATDbits.LATD7; } while(0)
#define LCD_D7_GetValue()           PORTDbits.RD7
#define LCD_D7_SetDigitalInput()    do { TRISDbits.TRISD7 = 1; } while(0)
#define LCD_D7_SetDigitalOutput()   do { TRISDbits.TRISD7 = 0; } while(0)
#define LCD_D7_SetPullup()          do { WPUDbits.WPUD7 = 1; } while(0)
#define LCD_D7_ResetPullup()        do { WPUDbits.WPUD7 = 0; } while(0)
#define LCD_D7_SetPushPull()        do { ODCONDbits.ODCD7 = 0; } while(0)
#define LCD_D7_SetOpenDrain()       do { ODCONDbits.ODCD7 = 1; } while(0)
#define LCD_D7_SetAnalogMode()      do { ANSELDbits.ANSELD7 = 1; } while(0)
#define LCD_D7_SetDigitalMode()     do { ANSELDbits.ANSELD7 = 0; } while(0)

// get/set BOTON_SELECCION aliases
#define BOTON_SELECCION_TRIS                 TRISEbits.TRISE0
#define BOTON_SELECCION_LAT                  LATEbits.LATE0
#define BOTON_SELECCION_PORT                 PORTEbits.RE0
#define BOTON_SELECCION_WPU                  WPUEbits.WPUE0
#define BOTON_SELECCION_OD                   ODCONEbits.ODCE0
#define BOTON_SELECCION_ANS                  ANSELEbits.ANSELE0
#define BOTON_SELECCION_SetHigh()            do { LATEbits.LATE0 = 1; } while(0)
#define BOTON_SELECCION_SetLow()             do { LATEbits.LATE0 = 0; } while(0)
#define BOTON_SELECCION_Toggle()             do { LATEbits.LATE0 = ~LATEbits.LATE0; } while(0)
#define BOTON_SELECCION_GetValue()           PORTEbits.RE0
#define BOTON_SELECCION_SetDigitalInput()    do { TRISEbits.TRISE0 = 1; } while(0)
#define BOTON_SELECCION_SetDigitalOutput()   do { TRISEbits.TRISE0 = 0; } while(0)
#define BOTON_SELECCION_SetPullup()          do { WPUEbits.WPUE0 = 1; } while(0)
#define BOTON_SELECCION_ResetPullup()        do { WPUEbits.WPUE0 = 0; } while(0)
#define BOTON_SELECCION_SetPushPull()        do { ODCONEbits.ODCE0 = 0; } while(0)
#define BOTON_SELECCION_SetOpenDrain()       do { ODCONEbits.ODCE0 = 1; } while(0)
#define BOTON_SELECCION_SetAnalogMode()      do { ANSELEbits.ANSELE0 = 1; } while(0)
#define BOTON_SELECCION_SetDigitalMode()     do { ANSELEbits.ANSELE0 = 0; } while(0)

// get/set BOTON_INICIO aliases
#define BOTON_INICIO_TRIS                 TRISEbits.TRISE1
#define BOTON_INICIO_LAT                  LATEbits.LATE1
#define BOTON_INICIO_PORT                 PORTEbits.RE1
#define BOTON_INICIO_WPU                  WPUEbits.WPUE1
#define BOTON_INICIO_OD                   ODCONEbits.ODCE1
#define BOTON_INICIO_ANS                  ANSELEbits.ANSELE1
#define BOTON_INICIO_SetHigh()            do { LATEbits.LATE1 = 1; } while(0)
#define BOTON_INICIO_SetLow()             do { LATEbits.LATE1 = 0; } while(0)
#define BOTON_INICIO_Toggle()             do { LATEbits.LATE1 = ~LATEbits.LATE1; } while(0)
#define BOTON_INICIO_GetValue()           PORTEbits.RE1
#define BOTON_INICIO_SetDigitalInput()    do { TRISEbits.TRISE1 = 1; } while(0)
#define BOTON_INICIO_SetDigitalOutput()   do { TRISEbits.TRISE1 = 0; } while(0)
#define BOTON_INICIO_SetPullup()          do { WPUEbits.WPUE1 = 1; } while(0)
#define BOTON_INICIO_ResetPullup()        do { WPUEbits.WPUE1 = 0; } while(0)
#define BOTON_INICIO_SetPushPull()        do { ODCONEbits.ODCE1 = 0; } while(0)
#define BOTON_INICIO_SetOpenDrain()       do { ODCONEbits.ODCE1 = 1; } while(0)
#define BOTON_INICIO_SetAnalogMode()      do { ANSELEbits.ANSELE1 = 1; } while(0)
#define BOTON_INICIO_SetDigitalMode()     do { ANSELEbits.ANSELE1 = 0; } while(0)

// get/set PIN_CALOR aliases
#define PIN_CALOR_TRIS                 TRISEbits.TRISE2
#define PIN_CALOR_LAT                  LATEbits.LATE2
#define PIN_CALOR_PORT                 PORTEbits.RE2
#define PIN_CALOR_WPU                  WPUEbits.WPUE2
#define PIN_CALOR_OD                   ODCONEbits.ODCE2
#define PIN_CALOR_ANS                  ANSELEbits.ANSELE2
#define PIN_CALOR_SetHigh()            do { LATEbits.LATE2 = 1; } while(0)
#define PIN_CALOR_SetLow()             do { LATEbits.LATE2 = 0; } while(0)
#define PIN_CALOR_Toggle()             do { LATEbits.LATE2 = ~LATEbits.LATE2; } while(0)
#define PIN_CALOR_GetValue()           PORTEbits.RE2
#define PIN_CALOR_SetDigitalInput()    do { TRISEbits.TRISE2 = 1; } while(0)
#define PIN_CALOR_SetDigitalOutput()   do { TRISEbits.TRISE2 = 0; } while(0)
#define PIN_CALOR_SetPullup()          do { WPUEbits.WPUE2 = 1; } while(0)
#define PIN_CALOR_ResetPullup()        do { WPUEbits.WPUE2 = 0; } while(0)
#define PIN_CALOR_SetPushPull()        do { ODCONEbits.ODCE2 = 0; } while(0)
#define PIN_CALOR_SetOpenDrain()       do { ODCONEbits.ODCE2 = 1; } while(0)
#define PIN_CALOR_SetAnalogMode()      do { ANSELEbits.ANSELE2 = 1; } while(0)
#define PIN_CALOR_SetDigitalMode()     do { ANSELEbits.ANSELE2 = 0; } while(0)

/**
   @Param
    none
   @Returns
    none
   @Description
    GPIO and peripheral I/O initialization
   @Example
    PIN_MANAGER_Initialize();
 */
void PIN_MANAGER_Initialize (void);

/**
 * @Param
    none
 * @Returns
    none
 * @Description
    Interrupt on Change Handling routine
 * @Example
    PIN_MANAGER_IOC();
 */
void PIN_MANAGER_IOC(void);



#endif // PIN_MANAGER_H
/**
 End of File
*/