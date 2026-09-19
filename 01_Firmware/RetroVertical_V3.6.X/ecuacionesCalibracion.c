#include "ecuacionesCalibracion.h"

void amarilloIntenso(void){
    aplicarEcuacion(EC_AMARILLO_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void blancoIntenso(void){
    aplicarEcuacion(EC_BLANCO_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void naranjaIntenso(void){
    aplicarEcuacion(EC_NARANJA_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void rojoIntenso(void){
    aplicarEcuacion(EC_ROJO_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void azulIntenso(void){
    aplicarEcuacion(EC_AZUL_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void verdeIntenso(void){
    aplicarEcuacion(EC_VERDE_INTENSO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}



        
void amarilloOpaco (void){
    aplicarEcuacion(EC_AMARILLO_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void blancoOpaco(void){
    aplicarEcuacion(EC_BLANCO_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void naranjaOpaco(void){
    aplicarEcuacion(EC_NARANJA_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void rojoOpaco(void){
    aplicarEcuacion(EC_ROJO_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void azulOpaco(void){
    aplicarEcuacion(EC_AZUL_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}
void verdeOpaco(void){
    aplicarEcuacion(EC_VERDE_OPACO);   /* V3.6: coeficientes en RAM/EEPROM; formula 2020 en calibracion_v36.c */
}






void arreglar_dato(void){
    
    if ((reflectivityValue > 4000 )) {
            reflectivityValue = 0;
    }
}

int conversionDatoEnviar(struct pt * pt){
    static struct pt ptSubTask;
    PT_BEGIN(pt);
        arreglar_dato(); //  ajuste malo
        sprintf(reflectStr, "%u", reflectivityValue); // guardamos 
        strcpy(dataEnvia, "::");   // le sumamos dos puntos para que la aplicacion reconosca el dato  ::DATO
        strcat(dataEnvia, reflectStr);  // sumamos los datos y listo a enviar
        sendUartStr(dataEnvia); 
        PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
    PT_EXIT(pt);
    PT_END(pt);
}


int ecuacionesColoresGatillo(struct pt * pt){
    static struct pt ptSubTask;
    PT_BEGIN(pt);
                if (bufferPantalla[8] == 0x01){ // blanco intenso
                    blancoIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                } else if (bufferPantalla[8] == 0x02){// amarillo intenso
                    amarilloIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                else if (bufferPantalla[8] == 0x03){// verde INTENSO
                    verdeIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                else if (bufferPantalla[8] == 0x04){//rojo muy regular inteneso
                    rojoIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                else if (bufferPantalla[8] == 0x05){ // azul regular intenso
                    azulIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                else if (bufferPantalla[8] == 0x06){ // naranja unico que funciona bien    intenso
                    naranjaIntenso();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                
                else if (bufferPantalla[8] == 0x07){// blanco BAJO 
                    blancoOpaco();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));
                }
                else if (bufferPantalla[8] == 0x08){// amarillo bajo
                    amarilloOpaco ();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
                else if (bufferPantalla[8] == 0x0a){// verde BAJO
                    verdeOpaco();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
                else if (bufferPantalla[8] == 0x0b){//rojo muy regular BAJO
                    rojoOpaco();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
                else if (bufferPantalla[8] == 0x0c){// azul regular BAJO
                    azulOpaco();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
                else if (bufferPantalla[8] == 0x0d){// naranja unico que funciona bien
                    naranjaOpaco();
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
                else if (bufferPantalla[8] == 0x0e){// naranja unico que funciona bien
                    presentarDato();
                    PT_SPAWN(pt, &ptSubTask, beepOnce(&ptSubTask));}
    PT_EXIT(pt);
    PT_END(pt);

}


int ecuacionesColoresApp(struct pt * pt){
    static struct pt ptSubTask;
    PT_BEGIN(pt);
                
            if ((strncmp(bufferData, "1", 1) == 0)){   // blanco intenso 
                blancoIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "2", 1) == 0) {   // amarillo intenso
                amarilloIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "3", 1) == 0) {  // verde intenso
                verdeIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "4", 1) == 0) {  //rojo intenso
                rojoIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "5", 1) == 0) {  // azul  intenso
                azulIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "6", 1) == 0) {  // naranja intenso
                naranjaIntenso();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }


            else if (strncmp(bufferData, "7", 1) == 0) { // blanco   opaco
                blancoOpaco();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "8", 1) == 0) { // amarillo opaco
                amarilloOpaco ();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "a", 1) == 0) { // verde opaco
                verdeOpaco();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "b", 1) == 0) { // rojo opaco
                rojoOpaco();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "c", 1) == 0) { // azul opaco
                azulOpaco();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "d", 1) == 0) {  // naranja opaco
                naranjaOpaco();
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            else if (strncmp(bufferData, "e", 1) == 0) {  // naranja opaco
                PT_SPAWN(pt, &ptSubTask, conversionDatoEnviar(&ptSubTask));
            }
            
    PT_EXIT(pt);
    PT_END(pt);

}


