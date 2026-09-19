/*
 * calibracion_v36.h
 *
 * Firmware V3.6 del retrorreflectometro vertical (PIC18F47K42, XC8 2.10).
 *
 * Calibracion en EEPROM y ordenes de administracion por Bluetooth (UART1).
 * Contrato: 05_Documentacion/PROTOCOLO-V3.6.md.
 *
 * Nota de codificacion: este fichero es ASCII puro a proposito (sin tildes),
 * porque el proyecto declara ISO-8859-1 y el resto del fuente mezcla codificaciones.
 */

#ifndef CALIBRACION_V36_H
#define CALIBRACION_V36_H

#define FW_VERSION_STR "3.6"

/* Indices de ecuacion. El orden es el de los codigos de la app: 1-8, a-d. */
#define EC_BLANCO_INTENSO    0   /* app '1', STONE 0x01 */
#define EC_AMARILLO_INTENSO  1   /* app '2', STONE 0x02 */
#define EC_VERDE_INTENSO     2   /* app '3', STONE 0x03 */
#define EC_ROJO_INTENSO      3   /* app '4', STONE 0x04 */
#define EC_AZUL_INTENSO      4   /* app '5', STONE 0x05 */
#define EC_NARANJA_INTENSO   5   /* app '6', STONE 0x06 */
#define EC_BLANCO_OPACO      6   /* app '7', STONE 0x07 */
#define EC_AMARILLO_OPACO    7   /* app '8', STONE 0x08 */
#define EC_VERDE_OPACO       8   /* app 'a', STONE 0x0a */
#define EC_ROJO_OPACO        9   /* app 'b', STONE 0x0b */
#define EC_AZUL_OPACO       10   /* app 'c', STONE 0x0c */
#define EC_NARANJA_OPACO    11   /* app 'd', STONE 0x0d */
#define EC_NUM              12

/* Coeficientes en RAM: coefCal[k][0..3] = c3, c2, c1, c0 */
extern double coefCal[EC_NUM][4];

/* Factor de temperatura (definido en gui.c, antes static) */
extern double X_2;
extern double X_1;
extern double X_0;

/* Carga la calibracion desde EEPROM; si no es valida, deja la de fabrica. */
void calibracionIniciar(void);

/* Aplica la ecuacion k a la variable global reflectivityValue. */
void aplicarEcuacion(unsigned char k);

/* Atiende una trama #...# completa (buffer propio de uart_module.c, no bufferData). */
void adminProcesarTrama(char *trama, unsigned char len);

/* Envia #ERR,FORMATO#. */
void adminErrorFormato(void);

/* Caducidad del modo administrador (10 min sin tramas '#'). */
void adminTick(void);

/* Registro de tramas de la STONE (llamadas desde uart_stone.c). */
void registroStoneInicio(void);
void registroStoneByte(unsigned char b);
void registroStoneFin(void);

#endif /* CALIBRACION_V36_H */
