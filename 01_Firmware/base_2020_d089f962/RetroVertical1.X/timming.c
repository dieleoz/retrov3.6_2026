#include "timming.h"

volatile unsigned long globalMillisCounter = 0; //contador global de milisegundos

unsigned long getMillis(void) {
    unsigned long localMillisCounter;

    TMR0IE = 0;
        localMillisCounter = globalMillisCounter;
    TMR0IE = 1;

    return localMillisCounter;    
}

