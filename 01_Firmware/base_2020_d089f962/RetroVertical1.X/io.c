#include "io.h"


static struct pt ptSystemLed;

static int taskSystemLed(struct pt *pt) {
    static unsigned long delayLed;

    PT_BEGIN(pt);
    while (1) {
        delayLed = getMillis() + 200;
        PT_WAIT_UNTIL(pt, getMillis() >= delayLed);
        LED_ROJO_Toggle();
    }
    PT_END(pt);
}

void executeIOModule(void) {
    taskSystemLed(&ptSystemLed);   // con la tarea se empieza la ejecucion 
}

void startIOModule(void) {
    PT_INIT(&ptSystemLed);   
}
