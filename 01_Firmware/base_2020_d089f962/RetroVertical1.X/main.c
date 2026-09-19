//programa basado en lo realizado por el ingeniero fabio 
#include "mcc_generated_files/mcc.h"
#include "lcd.h"
#include "dhtA.h"
#include "io.h"
#include "gui.h"
#include "uart_module.h"
#include "Uart_stone.h"
#include "measurement.h"
#include "ecuacionesCalibracion.h"
/*
 *  para el manejo de este microcontrolador se hace uso de una tecnica llamada protothreads muy ligero para micros de 8 bits
 *  es diferente a multitask ó multi hilos ó RTOS no dejarse confundir por rumores
 *  busque algo de documentacion es facil 
 */
  void main(void)
{
    SYSTEM_Initialize();
//    dhtlib_init();
//    Enable the Global Interrupts
    INTERRUPT_GlobalInterruptEnable();
//      Disable the Global Interrupts
//      INTERRUPT_GlobalInterruptDisable();

    startIOModule();  // esto hace que prenda continuamente, indica que el microcontrolador esta funcionando
    startGuiModule(); // se inica la tarea principal
    startPantalla(); // se inicia la tarea de la pantalla tft STONE ver documentacion en pagina oficial
    startUartModule(); // se inicai la tarea para comunicarse con el bluetooth
//    startMeasurement(); // esto no hace nada
    while (1) {
        // se recorre constantemente esto no confundisre por los while(1) que se encuentra en cada funcion al llegar al final de cada funcion sigue con otra
        executeIOModule();  // primera tarea cambia de tarea al encontrar YIELD   
        executeGuiModule(); // segunda tarea empiece aqui primero viendo el flujo normal
        executePantalla();  // tercera tarea
        executeUartModule(); // cuarta tarea 
    }
    
}
