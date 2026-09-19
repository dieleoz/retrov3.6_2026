#include "ecuacionesCalibracion.h"

void amarilloIntenso(void){
    reflectivityValue = (double)(-0.000000073*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue + 0.000282508*(double)reflectivityValue*(double)reflectivityValue + 0.124075350*(double)reflectivityValue - 130);
}
void blancoIntenso(void){
    reflectivityValue = (double)(-0.000086541*(double)reflectivityValue*(double)reflectivityValue + 0.619668128*(double)reflectivityValue - 303);                   
}
void naranjaIntenso(void){
    reflectivityValue = (double)(0.000090731*(double)reflectivityValue*(double)reflectivityValue + 0.001064329*(double)reflectivityValue - 21);
}
void rojoIntenso(void){
    reflectivityValue = (double)(0.000000147*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000668624*(double)reflectivityValue*(double)reflectivityValue + 1.082394050*(double)reflectivityValue - 393);
}
void azulIntenso(void){
    reflectivityValue = (double)(0.000000026*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000018402*(double)reflectivityValue*(double)reflectivityValue + 0.102152670*(double)reflectivityValue - 49);
}
void verdeIntenso(void){
    reflectivityValue = (double)(0.000000163*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000756695*(double)reflectivityValue*(double)reflectivityValue + 1.213142724*(double)reflectivityValue - 442);
}



        
void amarilloOpaco (void){
    reflectivityValue = (double)(0.000000086*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000299026*(double)reflectivityValue*(double)reflectivityValue + 0.446572329*(double)reflectivityValue - 161);
}
void blancoOpaco(void){
    reflectivityValue = (double)(0.000087404*(double)reflectivityValue*(double)reflectivityValue + 0.013617682*(double)reflectivityValue - 27);
}
void naranjaOpaco(void){
    reflectivityValue = (double)(0.000090731*(double)reflectivityValue*(double)reflectivityValue + 0.001064329*(double)reflectivityValue - 21);                            
}
void rojoOpaco(void){
    reflectivityValue = (double)(0.000113098*(double)reflectivityValue*(double)reflectivityValue - 0.076130418*(double)reflectivityValue + 10);                               
}
void azulOpaco(void){
    reflectivityValue = (double)(0.000000026*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000018402*(double)reflectivityValue*(double)reflectivityValue + 0.102152670*(double)reflectivityValue - 49);
}
void verdeOpaco(void){
    reflectivityValue = (double)(0.000000163*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000756695*(double)reflectivityValue*(double)reflectivityValue + 1.213142724*(double)reflectivityValue - 442);
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


