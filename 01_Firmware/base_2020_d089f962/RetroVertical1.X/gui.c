#include "gui.h"

static struct pt ptGui;
static unsigned char reflectometerModel;

#define CURRENT_GEOMETRY_15 0
#define CURRENT_GEOMETRY_30 1

#define BUTTON_PRESSED_SELECT 0
#define BUTTON_PRESSED_START 1

static unsigned char currentGeometry;
static float startupLocalTemp;
static float startupRemoteTemp;

extern char bufferData[50];
extern char bufferPantalla[50];

static float batteryVoltage;

extern volatile unsigned int bufferIndex;
extern volatile unsigned int bufferindice;

extern void clearBuffer(void);
extern void borrarBuffer(void);

extern int ultimoDatoRecibido;
char dataEnvia[8];

unsigned int reflectivityValue;
uint8_t temdht = 0, humdht = 0;
int estadoBateria(struct pt *pt);

unsigned int adcBlack = 0, adcWhite = 0, T0 = 0;
static bool activador;
static int contador, medida1, medida2, medida3;


char reflectStr[17];

static double X_2 = 0.0;
static double X_1 =  0.00043212;
static double X_0 = 0.90148651;   // para pruebas debe quedar X_2=0; X_1=0; X_0=1; y = x + 

int beepOnce(struct pt *pt) {
    static unsigned long delayBeep;

    PT_BEGIN(pt);
        BUZZER_SetHigh();
        delayBeep = getMillis() + 80;
        PT_WAIT_UNTIL(pt, getMillis() > delayBeep);
        BUZZER_SetLow();
    PT_EXIT(pt);
    PT_END(pt);

}

static int beepTwice(struct pt *pt) {
    static unsigned long delayBeep;
    static unsigned char i;
    PT_BEGIN(pt);
        for (i = 0; i < 2; i++) {
            BUZZER_SetHigh();
            delayBeep = getMillis() + 80;
            PT_WAIT_UNTIL(pt, getMillis() > delayBeep);
            BUZZER_SetLow();
            delayBeep = getMillis() + 80;
            PT_WAIT_UNTIL(pt, getMillis() > delayBeep);
        }
    PT_EXIT(pt);
    PT_END(pt);
}

static int debounceButtonSelect(struct pt *pt) {
    static unsigned long delayDebounce;
    PT_BEGIN(pt);
        delayDebounce = getMillis() + 100;
        PT_WAIT_UNTIL(pt, getMillis() > delayDebounce);
        PT_WAIT_UNTIL(pt, BOTON_SELECCION_GetValue());
    PT_EXIT(pt);
    PT_END(pt);
}

static int debounceButtonStart(struct pt *pt) {
    static unsigned long delayDebounce;
    PT_BEGIN(pt);
        delayDebounce = getMillis() + 200;
        PT_WAIT_UNTIL(pt, getMillis() > delayDebounce);
        PT_WAIT_UNTIL(pt, BOTON_INICIO_GetValue());
    PT_EXIT(pt);
    PT_END(pt);
}

static int calibrate(struct pt *pt) {
    static unsigned long delayCalibrate;
    static struct pt ptSubTask;
    static double Temp;
    static double voltajeCompensacion;
    static long pwmD;
    PT_BEGIN(pt);
        PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask));
        Temp = (double)getLocalTemperatureValue();
        voltajeCompensacion = 0.000717*Temp*Temp - 0.115360*Temp + 1121.5;
        voltajeCompensacion = voltajeCompensacion;
        pwmD = (long)((voltajeCompensacion-2)/4.1852);
        PWM5_LoadDutyValue(pwmD);

        lcdClear();
        lcdGoto(0, 0);
        lcdPrint("MIDA NEGRO:");
        lcdGoto(1, 0);
        while (1) {
            if (!BOTON_SELECCION_GetValue() || !BOTON_INICIO_GetValue() || bufferIndex > 0){
                PT_SPAWN(pt, &ptSubTask, debounceButtonSelect(&ptSubTask));
                lcdPrint("Midiendo ....");
                if (currentGeometry == CURRENT_GEOMETRY_15 || currentGeometry == CURRENT_GEOMETRY_30) {
                    PT_SPAWN(pt, &ptSubTask, acquireReflectivity15Adc(&ptSubTask));
                    adcBlack = getReflectivity15Adc();
                    PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask));
                    T0 = (int)(getLocalTemperatureValue());
                } 
                break;
            } 
            PT_YIELD(pt);
        }

        PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
        lcdClear();
        lcdGoto(0, 0);
        lcdPrint("MIDA BLANCO:");
        lcdGoto(1, 0);
        while (1) {
            if (!BOTON_SELECCION_GetValue() || !BOTON_INICIO_GetValue() || bufferIndex > 0){
                PT_SPAWN(pt, &ptSubTask, debounceButtonSelect(&ptSubTask));
                lcdPrint("Midiendo ...");
                if (currentGeometry == CURRENT_GEOMETRY_15 || currentGeometry == CURRENT_GEOMETRY_30 ) {
                    PT_SPAWN(pt, &ptSubTask, acquireReflectivity15Adc(&ptSubTask));
                    adcWhite = getReflectivity15Adc();
                } 
                break;           
            } 
            PT_YIELD(pt);
        }
        setCalibration15Adc(adcBlack, adcWhite);
        PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
    PT_EXIT(pt);
    PT_END(pt);
}

static int guiWelcome(struct pt*pt) {
    static unsigned long delayWelcome;
    PT_BEGIN(pt);
        lcdClear();
        lcdGoto(0, 0);
        lcdPrint("  REFLECTOMETRO");
        lcdGoto(1, 0);
        if (reflectometerModel == RETRO_MODEL_BASIC) {
            lcdPrint("HORIZONTAL V3.3");
        } else{//(reflectometerModel == RETRO_MODEL_IOT) {
            lcdPrint("HORIZONTAL V3.3");
        }
        delayWelcome = getMillis() + 1500;
        PT_WAIT_UNTIL(pt, getMillis() > delayWelcome);
        PT_WAIT_UNTIL(pt, BOTON_SELECCION_GetValue() && BOTON_INICIO_GetValue());
    PT_EXIT(pt);
    PT_END(pt);
}

static int guiOwner(struct pt *pt) {
    char owner[13] = "      AIM   ";
    static unsigned long delayOwner;
    PT_BEGIN(pt);
        memset(owner, '\0', sizeof (owner));
        readCustomerName(owner);
        lcdClear();
        lcdGoto(0, 0);
        lcdPrint(" PROPIEDAD DE:");
        lcdGoto(1, 0);
        lcdPrint("       AIM");
        delayOwner = getMillis() + 2000;
    PT_WAIT_UNTIL(pt, getMillis() > delayOwner);
    PT_WAIT_UNTIL(pt, BOTON_SELECCION_GetValue() && BOTON_INICIO_GetValue());
    PT_EXIT(pt);
    PT_END(pt);
}

static int guiSerial(struct pt *pt) {
    char serial[17] = "     SLH-046";
    static unsigned long delaySerial;
    PT_BEGIN(pt);
        memset(serial, '\0', sizeof (serial));
        readSerial(serial);
        lcdClear();
        lcdGoto(0, 0);
        lcdPrint("     SERIAL:");
        lcdGoto(1, 0);
        lcdPrint("     SLH-046");
        lcdGoto(1, 0);
        delaySerial = getMillis() + 1500;
        PT_WAIT_UNTIL(pt, getMillis() > delaySerial);
        PT_WAIT_UNTIL(pt, BOTON_SELECCION_GetValue() && BOTON_INICIO_GetValue());
    PT_EXIT(pt);
    PT_END(pt);
}

static int checkTemperatureDiff(struct pt *pt) {
    static float currentLocalTemperature;
    static float currentRemoteTemperature;
    static struct pt ptSubTask;
    static unsigned long delayTemp;
    
    PT_BEGIN(pt);
    PT_SPAWN(pt, &ptSubTask, acquireRemoteTemperatureValue(&ptSubTask));
    currentRemoteTemperature = getRemoteTemperatureValue();
    PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask));
    currentLocalTemperature = getLocalTemperatureValue();
    
    if (fabs(currentLocalTemperature-startupLocalTemp)>5.0 || fabs(currentRemoteTemperature - startupRemoteTemp)>5.0){
        lcdClear();
        lcdGoto(0,0);
        lcdPrint(" CAMBIO TERMICO");
        lcdGoto(1,0);
        lcdPrint("  RE-CALIBRAR!");
        BUZZER_SetHigh();
        delayTemp=getMillis()+2000;
        PT_WAIT_UNTIL(pt,getMillis()>delayTemp);                              
        BUZZER_SetLow();
        while(1){
            PT_YIELD(pt);
        }
    }
    PT_END(pt);
}

void presentarDato(void){
    contador++;  // incrementamos el contador para desplazarlo por la pantalla
    arreglar_dato();   // si el dato es muy grande o negativo se manda a cero  !otro desoconcierto!
    // cada dato se va guardando para realizar el promedio al final y mostrar el promedio en la pantalla
    if (contador == 1){      // poner el primer dato
        medida1 = reflectivityValue;
        enviarDatos2(reflectivityValue, 199);
        enviarDatos2(0, 205);
        enviarDatos2(0, 210);
        enviarDatos2(reflectivityValue, 215);
    }
    if (contador == 2){       // poner el segundo dato
        medida2 = reflectivityValue;
        enviarDatos2(reflectivityValue, 205);
        enviarDatos2(((medida1 + medida2)/2), 215);
    }
    if (contador == 3){       // poner el tercer dato
        medida3 = reflectivityValue;
        enviarDatos2(reflectivityValue, 210);
        enviarDatos2(((medida1 + medida2 + medida3)/3), 215);
        contador = 0;
    }
}


static int measure(struct pt * pt) {
    static struct pt ptSubTask;
    static unsigned char buttonPressed;
    static float reflectivity;
    static long delayTimeout;
    static signed int nivelBateria;
    unsigned int reflectivity15Adc;
    char nivel[2];
    char chartemp[5];
    char charHumedad[5];
    static int localTemp;
    static double Temp;
    static double voltajeCompensacion;
    static long pwmD;
    static long contadorBateria = 0;
    bool medida_mVoltios = false; // true sirve para medir en m voltios y sisualizarlo en la pantalla 
    
    PT_BEGIN(pt);
    
    while (1) {
//        SALIDA15_SetHigh();
        PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask));  // procesamos la temperatura  recuerde que se esta tmando de medida 30 
        Temp = (double)getLocalTemperatureValue();  //  guardamos la medida en Temp

        if (bufferindice > 0){      //  bufferindice aumenta cuando le llega algun dato a buffer2 del microcontrolador lo que quiere decir cuando cambiamos de color (antes se usaban los colores porque el quipo no mide bien es deficiente  y me refiero a la parte fisica al manejo de la luz))
            // enviarDatos pide dos parametros en dato y la direccion donde se encuentra almacenda la variable, configurada en la pantalla
            enviarDatos2(0, 199);   //  primer dato
            enviarDatos2(0, 205);   // segundo  dato
            enviarDatos2(0, 210);   // tercer dato
            enviarDatos2(0, 215);   // cuarto dato
            
            contador = 0;           // contador es un indicador de la medida en la que va el dato puesto en la pantalla
            bufferindice = 0;       // borramos el indicador para que no ingrese nuevamente aqui
        }
        activador =  !BOTON_INICIO_GetValue(); // gatillo de la pistola algunas pistolas funcionan deficientes como interruptores (este es el pin mas alejado del microcontrolador))
        if (activador || bufferIndex > 0 ){// si se presiona el gatillo o llega un dato de la aplicacion ingresa
                
            PT_SPAWN(pt, &ptSubTask, acquireReflectivity15Value(&ptSubTask)); //   tomamos el valor mediante un filtro digital de primer orden
            reflectivityValue = getReflectivity15Adc();
            // compensacion de la medida por efecto termico debe medir y sacar suspropias conclusiones
            reflectivityValue = (double)reflectivityValue * (X_2 * Temp * Temp + X_1* Temp + X_0) ;
            
            /* en el siguiente pedazo de codigo se cambia la ecuacion a usar, cada vez que cambia el color !!!!!!!  
             * asi que se tienen 12 diferentes ecuaciones, solo deberia existit una unica ecuacion 
             * algunas ecuaciones son de primer oreden otras de segundo e incluso de tercer orden!!!!!!!!!!!!!!!!!!!!!!!!
            */
            
            //if (activador && medida_mVoltios ){
            //            presentarDato(); // se usa para mostrar los datos en la lcd stone
            //            PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
            //}
            if (activador && !medida_mVoltios){
                // ecuaciones para colores 
                PT_SPAWN(pt, &ptSubTask, ecuacionesColoresGatillo(&ptSubTask));
                // ecuaciones para  bajas intensidades o colores uniformes sin rombos ni diseños
            }
            
            /*
             * el siguiente pedazo de codigo es usado para comparar el dato
             * enviado por la aplicacion y regresale el respectivo dato !alterado!
             * las ecuaciones osn las mismas que se encuentran en la parte anterior
             * 
             * la apliacion reconoce dos tipos de datos dato de medida     DATO::  y  :NIVELBATERIA:
             */
            // arreglar el codigo que se repite  poner otra funcion que haga lo mismo
            if (bufferIndex > 0 ){
                PT_SPAWN(pt, &ptSubTask, ecuacionesColoresApp(&ptSubTask));
            }
            if (strncmp(bufferData, "9", 1) == 0) {    // esto es usado para el indicador de bateria que  se encentra en la aplicacion 
//                    PT_SPAWN(pt, &ptSubTask, acquireBatteryVoltage(&ptSubTask));
//                    batteryVoltage = getBatteryVoltage();  // mayor a 13 es 5 lineas
                    batteryVoltage = (2*batteryVoltage) - 21; // el valor a enviar va entre cero a cinco
                    if (batteryVoltage > 5 && batteryVoltage < 10){batteryVoltage = 5;}
                    if (batteryVoltage < 0 ){batteryVoltage = 0;}
                    nivelBateria = (int) batteryVoltage;
                    sprintf(nivel, "%u", nivelBateria);
                    strcpy(dataEnvia, ":");
                    strcat(dataEnvia, nivel);
                    strcat(dataEnvia, ":");
                    sendUartStr(dataEnvia);   // esto envia el dato a la aplicacion
            }

            clearBuffer();   // borramos todo en las sigueientes lineas, ya que se han procesado los datos
            bufferIndex = 0; 
            memset(dataEnvia, 0, sizeof (dataEnvia)); // se limpia lo que se envio
            delayTimeout = getMillis() + 500; // esperamos un momneto y ya se puede quetar pero no lo hice
            PT_WAIT_UNTIL(pt, getMillis() > delayTimeout);
        }
        PT_SPAWN(pt, &ptSubTask, acquireBatteryVoltage(&ptSubTask));
        batteryVoltage = getBatteryVoltage() - 10;  // mayor a 13 es 5 lineas
        batteryVoltage = (45.455*batteryVoltage) - 4.5455;
        contadorBateria = contadorBateria+1;
        if (contadorBateria > 200){
            if (batteryVoltage < 0) {batteryVoltage = 0;}
            if (batteryVoltage > 99) {batteryVoltage = 99;}
            enviarDatos2((long)batteryVoltage, 207);   // bateria
            contadorBateria = 0;
        }  
        PT_YIELD(pt);  // le damos paso a otras tareas para que se ejecuten
    }

    PT_EXIT(pt);    
    PT_END(pt);
}

int chooseUserGeometry(struct pt *pt) {
    static struct pt ptSubTask;
    static unsigned long delay;
    PT_BEGIN(pt);
    currentGeometry = CURRENT_GEOMETRY_15;
    lcdClear();
    lcdGoto(0, 0);
    lcdPrint("SELECCIONE:");
    lcdGoto(1, 0);
    lcdPrint("GEOMETRIA 15");
    while (1) {
        enviarDatos2(1234, 21554);
        delay= getMillis() + 1500;
        PT_WAIT_UNTIL(pt, getMillis() > delay);
        
        if (!BOTON_SELECCION_GetValue()) {
            delay= getMillis() + 300;
            PT_WAIT_UNTIL(pt, getMillis() > delay);
            PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
            PT_SPAWN(pt, &ptSubTask, debounceButtonSelect(&ptSubTask));
            if (currentGeometry == CURRENT_GEOMETRY_15) {
                currentGeometry = CURRENT_GEOMETRY_30;
                lcdGoto(1, 0);
                lcdPrint("GEOMETRIA 30");
            } else if (currentGeometry == CURRENT_GEOMETRY_30) {
                currentGeometry = CURRENT_GEOMETRY_15;
                lcdGoto(1, 0);
                lcdPrint("GEOMETRIA 15");
            }
        }
        if (!BOTON_INICIO_GetValue()) {
            PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
            PT_SPAWN(pt, &ptSubTask, debounceButtonStart(&ptSubTask));
            break;
        }
        PT_YIELD(pt);
    }




    PT_EXIT(pt);
    PT_END(pt);
}

int taskCharger(struct pt *pt) {
    static unsigned long delayCharger;
    static float batteryVoltage;
    static unsigned int batteryPercentage;
    char infoStr[17];
    static struct pt ptSubTask;
    static float batteryFactor;
    static char numericStr[10];

    PT_BEGIN(pt);


    lcdClear();
    lcdGoto(0, 0);
    lcdPrint("CARGANDO ...");
    while (1) {
        delayCharger = getMillis() + 1000;
        memset(infoStr, 0, sizeof (infoStr));
//        dhtlib_init();
        PT_WAIT_UNTIL(pt, getMillis() > delayCharger);
        PT_SPAWN(pt, &ptSubTask, acquireBatteryVoltage(&ptSubTask));
        batteryVoltage = getBatteryVoltage();
        
        batteryPercentage = (batteryVoltage - 11)*100/2.5; // el 13.5 es el maximo 
        if (batteryPercentage > 150){batteryPercentage = 110;}
        if (batteryPercentage < 1){batteryPercentage = 0;}
        sprintf(numericStr, "%u", batteryPercentage);
        strcat(infoStr, numericStr);
        strcat(infoStr, "%");
        lcdGoto(1, 0);
        lcdPrint("                ");
        lcdGoto(1, 0);
        lcdPrint(infoStr);
        
       
    }
    PT_END(pt);
}


int estadoBateria(struct pt *pt) {
    static unsigned long delayCharger;
    static float batteryVoltage;
    static unsigned int batteryPercentage;
    char infoStr[17];
    static struct pt ptSubTask;
    static float batteryFactor;
    static char numericStr[10];

    PT_BEGIN(pt);

    lcdClear();
    lcdGoto(0, 0);
    lcdPrint("NIVEL BATERIA:");
    lcdGoto(1, 0);
    memset(infoStr, 0, sizeof (infoStr));
    PT_SPAWN(pt, &ptSubTask, acquireBatteryVoltage(&ptSubTask));
    batteryVoltage = getBatteryVoltage();
    batteryPercentage = (batteryVoltage - 11)*100/2.5; // el 13.5 es el maximo
    if (batteryPercentage > 200){
        batteryPercentage = 0;
    }
    sprintf(numericStr, "%u", batteryPercentage);
    strcat(infoStr, numericStr);
    strcat(infoStr, "%");
    lcdPrint(infoStr);
    delayCharger = getMillis() + 1000;
    PT_WAIT_UNTIL(pt, getMillis() > delayCharger);
    lcdGoto(1, 0);
    lcdPrint("                ");
    PT_END(pt);
}

int startupTemperature(struct pt *pt) {
    static struct pt ptSubTask;
    PT_BEGIN(pt);
    PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask));
    startupLocalTemp = getLocalTemperatureValue();
    PT_SPAWN(pt, &ptSubTask, acquireRemoteTemperatureValue(&ptSubTask));
    startupRemoteTemp = getLocalTemperatureValue();
    PT_END(pt);
}

int taskGui(struct pt *pt) {
    static unsigned long delayCharger;
    static unsigned char delayBuzzer;
    static struct pt ptSubTask;
    static unsigned char enabledGeometry;
    static int localTemp;
    static long pwmD;
    PT_BEGIN(pt);   // inicia la tarea
    
    while (1) {
        PT_SPAWN(pt, &ptSubTask, acquireLocalTemperatureValue(&ptSubTask)); //  medimos la temperatura y la multiplicamos por 10  con lo cual 20 grados seran 200 unidades
        localTemp = (int)getLocalTemperatureValue();  // tomamos la temperatura y la guardamos
        PWM5_LoadDutyValue(0); // se establece el pwm5 = pin 23 a 0
        PWM7_LoadDutyValue(0); // se establece a cero pin 24
        
        delayCharger = getMillis() + 2000;
        PT_WAIT_UNTIL(pt, getMillis() > delayCharger);
        PT_SPAWN(pt, &ptSubTask, acquireBatteryVoltage(&ptSubTask));
        batteryVoltage = getBatteryVoltage() - 10;  // mayor a 13 es 5 lineas
        batteryVoltage = (45.455*batteryVoltage) - 4.5455;
        
        if (batteryVoltage < 0) {batteryVoltage = 0;}
        if (batteryVoltage > 99) {batteryVoltage = 99;}
        enviarDatos2((long)batteryVoltage, 207);
        
        PT_SPAWN(pt, &ptSubTask, measure(&ptSubTask));    // empezamos la tarea de medida comunicacion y de mas tareas
        PT_YIELD(pt);    // si llega aqui cambia de tarea
    }
    PT_END(pt);

}

void startGuiModule(void) {
    PT_INIT(&ptGui); // iniciamos la 2 tarea  // inicializa la tarea no mover no cambiar, recuerde que en c solo se pueden llamar funciones previamente creadas
    lcdInit();
    
    /*
     * *****************************************************************************************************************************
     * 
     * 
     * cuidado hay funciones que no se usan pero siguen aqui, debido a que esta es una variante de el codigo para el retro horizontal     
     * 
     * 
     * 
     * *****************************************************************************************************************************
     */

}

void executeGuiModule(void) {
    taskGui(&ptGui);
}
