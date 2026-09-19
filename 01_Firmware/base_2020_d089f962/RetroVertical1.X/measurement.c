#include "measurement.h"
#include "eeprom_manager.h"



static float localTemperatureValue;
static float remoteTemperatureValue;
static unsigned int reflectivity15Adc;
static unsigned int reflectivity15Value;
static unsigned int reflectivity30Adc;
static unsigned int reflectivity30Value;

static unsigned int calibrationBlack15Adc;
static unsigned int calibrationWhite15Adc;
static unsigned int calibrationBlack30Adc;
static unsigned int calibrationWhite30Adc;

static float batteryVoltage;
static float batteryFactor;
float filtroAcumulado;
uint8_t tb = 0, hb = 0;
extern unsigned int adcBlack, adcWhite, T0;


/**
 * In order to execute this function correctly, you must to:
 * 1) call "getLocalTemperature", the temperature will be in static variable "localTemperature"
 * 2) call  this function (getLocalTemperature)
 * @return 
 */
float getLocalTemperatureValue(void) {
    return localTemperatureValue;
}

uint8_t gettemdht(){
    return tb;
}

uint8_t gethumdht(){
    return hb;
}


void setCalibration15Adc(unsigned int adcBlack, unsigned int adcWhite) {
    calibrationBlack15Adc = adcBlack;
    calibrationWhite15Adc = adcWhite;
}

void setCalibration30Adc(unsigned int adcBlack, unsigned int adcWhite) {
    calibrationBlack30Adc = adcBlack;
    calibrationWhite30Adc = adcWhite;
}


int acquireLocalTemperatureValue(struct pt *pt) {
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int tempSamples[5];
    float localTemperatureVoltage;
    unsigned int localTempAdc;
    static unsigned int muestras;

    PT_BEGIN(pt);
        float alpha = 0.98;
        float alpha2 = 0.02;
        filtroAcumulado = ADCC_GetSingleConversion(INREFLECT30 ); // se amplifica la señanl TEMLOCAL2 asi que se quita y se manda por inreflect
        for (long i = 1; i < 200; i++) {
            muestras = ADCC_GetSingleConversion(INREFLECT30); // TEMLOCAL2   
            filtroAcumulado = (filtroAcumulado*alpha) + (alpha2*muestras); //1 - alpha
        }
        //Se calcula la temperatura con la formula del sensor  que es casi 5 veces asi que ya 
        localTemperatureValue = (filtroAcumulado); //(localTemperatureVoltage);  //- 0.4) / 0.0195;
        localTemperatureValue = localTemperatureValue/4.928;
    PT_EXIT(pt);
    PT_END(pt);
}


int acquireLm35TemperatureValue(struct pt *pt) {  // esto no sirve mide rirectamente asi que solo se puede medir la temperatura de la PCB
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int tempSamples[5];
    float localTemperatureVoltage;
    unsigned int localTempAdc;
    static unsigned int muestras;

    PT_BEGIN(pt);
        float alpha = 0.98;
        float alpha2 = 0.02;
        filtroAcumulado = ADCC_GetSingleConversion(TEMLOCAL1); // se amplifica la señanl TEMLOCAL2 asi que se quita y se manda por inreflect
        for (long i = 1; i < 200; i++) {
            muestras = ADCC_GetSingleConversion(TEMLOCAL1); // TEMLOCAL2   
            filtroAcumulado = (filtroAcumulado*alpha) + (alpha2*muestras); //1 - alpha
        }
        localTemperatureValue = (filtroAcumulado); //se queda igual
    PT_EXIT(pt);
    PT_END(pt);
}


float getRemoteTemperatureValue(void) {
    return remoteTemperatureValue;
}

int getADCValue(void) {
    return reflectivity15Adc;
}

float getBatteryVoltage(void) {
    return batteryVoltage;
}

unsigned int getBatteryPercentage(float batteryVoltage) {  // no se para que se usa ya estba de antes
    float currentPercentage;
    currentPercentage = calculateLinearValue(batteryVoltage, readFloat(EEPROM_ADDRESS_BAT_PERCENTAGE_VOLTAGE_MINIMUM), readFloat(EEPROM_ADDRESS_BAT_PERCENTAGE_PERCENTAGE_MINIMUM), readFloat(EEPROM_ADDRESS_BAT_PERCENTAGE_VOLTAGE_MAXIMUM), readFloat(EEPROM_ADDRESS_BAT_PERCENTAGE_PERCENTAGE_MAXIMUM));
    if (currentPercentage > 100.0) {
        currentPercentage = 100.0;
    } else if (currentPercentage < 0.0) {
        currentPercentage = 0.0;
    }
    return (unsigned int) currentPercentage;
}

int acquireBatteryVoltage(struct pt *pt) {
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int tempSamples[5];
    float remoteTemperatureVoltage;
    unsigned int remoteTempAdc;
    static float vbat;

    PT_BEGIN(pt);
        //Se obtienen 5 muestras con intervalos de 5 mseg
        for (i = 0; i < 5; i++) {
            delayIntTemp = getMillis() + 5;
            PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);
            tempSamples[i] = ADCC_GetSingleConversion(BATERIA);
        }
        //se calcula la media aritmetica 
        orderArray(tempSamples, 5);
        remoteTempAdc = (tempSamples[2] + tempSamples[3] + tempSamples[4]) / 3;
        //se calcula el voltage real en la entrada analoga
        remoteTemperatureVoltage = (float) remoteTempAdc * 0.001; //4.096 / 4096;
        batteryVoltage = remoteTemperatureVoltage*4.3; // relacion de resistencias 10k y 33k esas son las que estan
        if (batteryVoltage < 0.0) { // si al medir y procesar se obtiene una medida 
            batteryVoltage = 0.0;
        }
        //Se calcula la temperatura con la formula del sensor
    PT_EXIT(pt);
    PT_END(pt);
}

int acquireRemoteTemperatureValue(struct pt *pt) {  // no se para que se uas no hay  documentacion ni nada
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int tempSamples[5];
    float remoteTemperatureVoltage;
    unsigned int remoteTempAdc;


    PT_BEGIN(pt);

        //Se cambia la referencia negativa a GND
        ADREFbits.NREF = 0;
        //Espera de 5ms por el cambio de referencia
        delayIntTemp = getMillis() + 5;
        PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);

        //Se obtienen 5 muestras con intervalos de 5 mseg
        for (i = 0; i < 5; i++) {
            delayIntTemp = getMillis() + 5;
            PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);
            tempSamples[i] = ADCC_GetSingleConversion(TEMLOCAL2);
        }

        //se calcula la media aritmetica 
        orderArray(tempSamples, 5);
        remoteTempAdc = (tempSamples[1] + tempSamples[2] + tempSamples[3]) / 3;

        //se calcula el voltage real en la entrada analoga
        remoteTemperatureVoltage = (float) remoteTempAdc * 4.096 / 4095;

        //Se calcula la temperatura con la formula del sensor
        remoteTemperatureValue = (remoteTemperatureVoltage - 0.4) / 0.0195;

    PT_EXIT(pt);
    PT_END(pt);
}


float ajusteMalasMedidas(float tempReflecP){
    
    if (tempReflecP < 75.0  || tempReflecP > 3000.0) {
        tempReflecP = 0.0;
    }
    return  tempReflecP;
    
}

unsigned int getReflectivity15Adc(void) {
    return reflectivity15Adc;
}

unsigned int getReflectivity15Value(void) {
    return reflectivity15Value;
}

int acquireReflectivity15Adc(struct pt *pt) {  // esto es lo que mide el voltaje del sensor optico
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int muestras;
    static unsigned int tempSamples[15];
    float remoteTemperatureVoltage;
    unsigned int remoteTempAdc;
    unsigned int valorMax = 0;
    unsigned int valorMin;
    float calculoRef, calculoRefBaja, calculoRefAlta, pwm; 
    float alpha;

    PT_BEGIN(pt);

        SALIDA15_SetHigh();

        //Espera de 250mseg para estabilizar la medida respuesta amps leds ...  sobre todo el sensor tiene una respuesta critica
        delayIntTemp = getMillis() + 400;  
        PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);

        for (i = 0; i < 16; i++) {  // mido el promedio rapido luego si hago una mejor medida
            delayIntTemp = getMillis() + 1;  
            PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);
            tempSamples[i] = ADCC_GetSingleConversion(INREFLECT15);
        }
        reflectivity15Adc = 0;
        for (i = 1; i < 16; i++) {    // sumo todo
            reflectivity15Adc += (unsigned int)(tempSamples[i]);
        }
        reflectivity15Adc = reflectivity15Adc/15;  // y lo promedio
        /* aqui hago una mejor medicion que dura un rato*/
        alpha = 0.995;
        float alpha2 = 0.005;
        filtroAcumulado = reflectivity15Adc;
        for (long i = 1; i < 600; i++) {
            muestras = ADCC_GetSingleConversion(INREFLECT15);
            filtroAcumulado = (filtroAcumulado*alpha) + (alpha2*muestras); //1 - alpha
        }
        SALIDA15_SetLow();  // apago la luz y sumo 200 a la medida, no importa al final en la calibracion se arregla esto vita manejar valores posiblemente negativos
        reflectivity15Adc   = (int)filtroAcumulado + 200;
    PT_EXIT(pt);
    PT_END(pt);
}

int acquireReflectivity15Value(struct pt *pt) { // aqui esto no se usa solo se usa en el horizontal
    static struct pt ptSubTask;
    static float Reflec;
    static float ganancia0, cambioTemperatura;
    static float offset0, offset1;
    static float ecuacionAux1 = 0, ecuacionAux2 = 0;
    static double auxmun, mmm =0.000000001, resta;
    
    /********************************************************************/ 
    /*********** estos son los unicos valores a cambiar******************/
    ganancia0 = 0.00028; 
    offset0 = 0.10185;
    cambioTemperatura = 2.4946;
    offset1 = 5;
    /********************************************************************/         
    /********************************************************************/ 
     
    PT_BEGIN(pt);
        PT_SPAWN(pt, &ptSubTask, acquireReflectivity15Adc(&ptSubTask));  // se sacan muestras a la señal y se promedian ha un problema de desperdiciar medidas
        Reflec = reflectivity15Adc;// - calibrationBlack15Adc 0.2644 * reflectivity15Adc - 110.18;//se espera una nueva formula calculateLinearValue(getReflectivity15Adc(), calibrationBlack15Adc, readUint(EEPROM_ADDRESS_BLACK_PATTERN_15_VALUE), calibrationWhite15Adc, readUint(EEPROM_ADDRESS_WHITE_PATTERN_15_VALUE));
    //    ecuacionAux1 = adcBlack - (cambioTemperatura*(localTemperatureValue - T0));
    //    ecuacionAux2 = Reflec - ecuacionAux1;
    //    Reflec = (((ganancia0*localTemperatureValue) + offset0)*ecuacionAux2) + offset1;
        resta =adcWhite-adcBlack;
        mmm = ((465-30)/resta);
        //mmm = mmm/10000;
        auxmun = (mmm*Reflec)+(30-mmm*adcBlack);
    //    Reflec =(0.00000011*localTemperatureValue*localTemperatureValue+0.00001417*localTemperatureValue+0.2662)*Reflec+(-0.0017988*localTemperatureValue*localTemperatureValue+0.60528*localTemperatureValue-238.2);
    //=(0.00000021*localTemperatureValue*localTemperatureValue-0.00001965*localTemperatureValue+0.222)*Reflec+(-0.0011774*localTemperatureValue*localTemperatureValue+0.3408*localTemperatureValue-65.06);
        Reflec = (float)auxmun;
        Reflec = ajusteMalasMedidas(Reflec);  // se ajusta la medida en caso de obtener medidas erroneas
    //    m =    (440-0)/( adcWhite-adcBlack);
    //    Reflec = reflectivity15Adc;
    //    Reflec = m * Reflec;
        reflectivity15Value = (unsigned int) Reflec;

    PT_EXIT(pt);
    PT_END(pt);
}

unsigned int getReflectivity30Adc(void) {
    return reflectivity30Adc;
}

unsigned int getReflectivity30Value(void) {
    return reflectivity30Value;
}

int acquireReflectivity30Adc(struct pt *pt) { // no se usa y no lo hare
    static unsigned long delayIntTemp;
    static unsigned char i;
    static unsigned int tempSamples[10];
    float remoteTemperatureVoltage;
    unsigned int remoteTempAdc;


    PT_BEGIN(pt);

        //Se cambia la referencia negativa a externa
        ADREFbits.NREF = 0;

       //LED_30_SetHigh();
        SALIDA30_SetHigh();

        //Espera de 100mseg para estabilizar la medida
        delayIntTemp = getMillis() + 10;
        PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);

        //Se obtienen 10 muestras con intervalos de 5 mseg
        for (i = 0; i < 10; i++) {
            delayIntTemp = getMillis() + 5;
            PT_WAIT_UNTIL(pt, getMillis() >= delayIntTemp);
            tempSamples[i] = ADCC_GetSingleConversion(INREFLECT15);
        }

        SALIDA30_SetLow();

        //se calcula la media aritmetica de las 6 muestras centrales
        orderArray(tempSamples, 10);
        reflectivity30Adc = (unsigned int) ((tempSamples[2] + tempSamples[3] + tempSamples[4] + tempSamples[5] + tempSamples[6] + tempSamples[7]) / 6);


    PT_EXIT(pt);
    PT_END(pt);
}

int acquireReflectivity30Value(struct pt *pt) {  // esto al final es usado para la temperatura 
    static struct pt ptSubTask;
    static float tempReflec;

    PT_BEGIN(pt);
    PT_SPAWN(pt, &ptSubTask, acquireReflectivity30Adc(&ptSubTask));
    tempReflec = 981;//calculateLinearValue(getReflectivity30Adc(), calibrationBlack30Adc, readUint(EEPROM_ADDRESS_BLACK_PATTERN_30_VALUE), calibrationWhite30Adc, readUint(EEPROM_ADDRESS_WHITE_PATTERN_30_VALUE));
    tempReflec = ajusteMalasMedidas(tempReflec);  // se ajusta la medida en caso de obtener medidas erroneas
    reflectivity30Value = (unsigned int) tempReflec;
    PT_EXIT(pt);
    PT_END(pt);
}

void startMeasurement(void) {
    
    batteryFactor = readFloat(EEPROM_ADDRESS_BATTERY_FACTOR);
    
    
}

/*
 * esto es una libreria que permite usar el sensor DHT11 pero por el momento no se usa dentro del codigo 
 * pero la libreria si funciona 
 */



///////////////////////////////////////////////////////////////////////////////////////////////////





/*	Library for DHT11 & DHT22 Temperature & Humidity Sensors
	Copyright (C) 2014 Jesus Ruben Santa Anna Zamudio.

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License

	Author e-mail: ruben at geekfactory dot mx
 */

/**
 * Buffer to store data from sensors
 */
unsigned char bits[5];
struct pt ptTaskUartRx, ptTaskTimeout;
/*-------------------------------------------------------------*/
/*		Function prototypes	(this file only)	*/
/*-------------------------------------------------------------*/
static void dhtlib_start();

/*-------------------------------------------------------------*/
/*		API Functions Implementation			*/
/*-------------------------------------------------------------*/
void dhtlib_init(void )
{
	// Initialization just sets DHT11 pin as input
//	dhtlib_setin();
    DHT11_SetDigitalInput();
}


int prueba (struct pt *pt)
{
    PT_BEGIN(pt);
    
    PT_EXIT(pt);
    PT_END(pt);
}

static enum dht_status dhtlib_read()
{
	unsigned char i = 0;
	unsigned char aindex = 0;
	unsigned char bcount = 7;

	// This is a variable used as timeout counter, type of this variable should
	// be chosen according to the processor speed.
	// When this variable overflows, timeout is detected
	int tocounter = 0;

	// Clear all bits on data reception buffer
	for (i = 0; i < 5; i++) bits[i] = 0;

	// Disable interrupts to keep timing accurate, especially on slow procesors
//	dhtlib_disint();

	// Generate MCU start signal
	dhtlib_start();

	// Wait for response from DHT11 max 80 uS low
	tocounter = 1;
	while (!DHT11_GetValue()) { 
		if (!tocounter++)
			goto timeout;
	}
	// Wait for response from DHT11 max 80 uS high
	tocounter = 1;
	while (DHT11_GetValue()) {
		if (!tocounter++)
			goto timeout;
	}
	// Begin data reception, 40 bits to be received
	for (i = 0; i < 40; i++) {
		tocounter = 1;
		while (!DHT11_GetValue()) {
			if (!tocounter++)
				goto timeout;
		}
		// If after 50 uS the pin is low we're on the start of another bit
		__delay_us(40);
		if (!DHT11_GetValue()) {
			if (bcount == 0) {
				bcount = 7;
				aindex++;
			} else {
				bcount--;
			}
			continue;
		}
		// If pin is high after 50 us we're receiving logic high
		tocounter = 1;
		while (DHT11_GetValue()) {
			if (!tocounter++)
				goto timeout;
		}
		// Set the bit and shift left
		bits[aindex] |= (1 << bcount);
		if (bcount == 0) {
			bcount = 7;
			aindex++;
		} else {
			bcount--;
		}
	}
	// Exit for normal operation
//	dhtlib_enaint();
	return E_DHTLIB_OK;
	// Exit when timeout occurs
timeout:
//	dhtlib_enaint(); // Re-enable interrupts and return
	return E_DHTLIB_TIMEOUT_ERROR;
}

int setdir(struct pt *pt){
    PT_BEGIN(pt);
    dhtlib_read11(&tb, &hb);
    PT_EXIT(pt);
    PT_END(pt);
            
}


 enum dht_status dhtlib_read11(uint8_t * temp, uint8_t * hum)
{
	// Read operation
     
	enum dht_status s = dhtlib_read();
	if (s != E_DHTLIB_OK)
		return s;
	// Checksum comprobation
	unsigned char chksum = bits[0] + bits[1] + bits[2] + bits[3];
	if (chksum != bits[4])
		return E_DHTLIB_CHKSUM_ERROR;
	// Copy results
	* hum = bits[0];
	* temp = bits[2];
	// Return Ok code
	return E_DHTLIB_OK;
}

enum dht_status dhtlib_read22(uint16_t * temp, uint16_t * hum)
{
	// Read operation
	enum dht_status s = dhtlib_read();
	if (s != E_DHTLIB_OK)
		return s;
	// Checksum comprobation
	unsigned char chksum = bits[0] + bits[1] + bits[2] + bits[3];
	if (chksum != bits[4])
		return E_DHTLIB_CHKSUM_ERROR;
	// Copy results
	* hum = ((bits[0] << 8) + bits[1]) & 0x7FFF;
	* temp = (bits[2] << 8) + bits[3];
	if (* temp & 0x8000) {
		* temp = -((* temp) & 0x7FFF);
	}
	// Return Ok code
	return E_DHTLIB_OK;

}

enum dht_status dhtlib_float22(float * temp, float * hum)
{
	// Read operation
	enum dht_status s = dhtlib_read();
	if (s != E_DHTLIB_OK)
		return s;
	// Checksum comprobation
	unsigned char chksum = bits[0] + bits[1] + bits[2] + bits[3];
	if (chksum != bits[4])
		return E_DHTLIB_CHKSUM_ERROR;
	// Copy results
	* hum = 0.1 * (((bits[0] << 8) + bits[1]) & 0x7FFF);
	if (bits[2] & 0x80) {
		*temp = -0.1 * ((bits[2] << 8) + bits[3]);
	} else {
		*temp = 0.1 * ((bits[2] << 8) + bits[3]);
	}
	// Return Ok code
	return E_DHTLIB_OK;
}

/**
 * @brief Generates start signal on the bus
 *
 * This function generates the signal needed to start the comunication with the
 * tamperature and humidity sensor.
 */
static void dhtlib_start()
{
	DHT11_SetDigitalOutput(); // Set pin as output
	DHT11_SetLow(); // Pull bus to low state

	__delay_ms(20);

	DHT11_SetHigh();  // pull bus to high
	DHT11_SetDigitalInput();; // Turn pin to input

	__delay_us(60);
}

void probando(void){
    __delay_ms(4);
}

