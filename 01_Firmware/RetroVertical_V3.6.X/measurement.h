#ifndef MEASUREMENT_H
#define	MEASUREMENT_H

#include "mcc_generated_files/mcc.h"
#include "timming.h"
#include "pt.h"
#include "binary_utils.h"

#define ENABLED_GEOMETRY_15_30 0
#define ENABLED_GEOMETRY_15 1
#define ENABLED_GEOMETRY_30 2


float getLocalTemperatureValue(void);
int acquireLocalTemperatureValue(struct pt *pt);
float getRemoteTemperatureValue(void);
int acquireRemoteTemperatureValue(struct pt *pt);
unsigned int getReflectivity15Adc(void);
unsigned int getReflectivity15Value(void);
int acquireReflectivity15Adc(struct pt *pt);
int acquireReflectivity15Value(struct pt *pt); 
unsigned int getReflectivity30Adc(void);
unsigned int getReflectivity30Value(void);
int acquireReflectivity30Adc(struct pt *pt);
int acquireReflectivity30Value(struct pt *pt);
void setCalibration15Adc(unsigned int adcBlack, unsigned int adcWhite);
void setCalibration30Adc(unsigned int adcBlack, unsigned int adcWhite);
float getBatteryVoltage(void);
int acquireBatteryVoltage(struct pt *pt);
void startMeasurement(void);
unsigned int getBatteryPercentage(float batteryVoltage);
int getADCValue(void);
int acquireLm35TemperatureValue(struct pt *pt);


///////////////////////////////////////////////////////////////////////////////////////////////////
void probando();

/*-------------------------------------------------------------*/
/*		Macros and definitions				*/
/*-------------------------------------------------------------*/

/*-------------------------------------------------------------*/
/*		Typedefs enums & structs			*/
/*-------------------------------------------------------------*/

/**
 * Defines the sensor API return codes
 */
enum dht_status {
	E_DHTLIB_OK = 0, //!< Read operation on DHT11 succesfull
	E_DHTLIB_TIMEOUT_ERROR, //!< Timeout error occured on DHT11 comunication
	E_DHTLIB_CHKSUM_ERROR, //!< Checksum verification error
};
/*-------------------------------------------------------------*/
/*		Function prototypes				*/
/*-------------------------------------------------------------*/

/**
 * @brief Prepares the DHT11 communications channel
 *
 * Prepares the communication with the DHT11 sensor, initializes IO ports and
 * any other required peripherals.
 */
void dhtlib_init(void);
/**
 * @brief Reads the current temperature and humidity from DHT11 sensor
 *
 * Reads the current temperature and humidity, returns an enumerated value
 * defining if the comunication with the sensor was succesfull. This function
 * places the temperature and humidity readings on the provided buffers.
 * 
 * @param pxTemperature Pointer to place the temperature reading
 * @param pxHumidity Pointer to place the humidity reading
 *
 * @return An enumerated value indictating the communication and device status
 */
enum dht_status dhtlib_read11(uint8_t * temp, uint8_t * hum);

/**
 * @brief Reads the current temperature and humidity from DHT22 sensor
 *
 * Reads the current temperature and humidity, returns an enumerated value
 * defining if the comunication with the sensor was succesfull. This function
 * puts the temperature and humidity readings on the provided buffers.
 *
 * @param pxTemperature Pointer to place the temperature reading
 * @param pxHumidity Pointer to place the humidity reading
 *
 * @return An enumerated value indictating the communication and device status
 */
enum dht_status dhtlib_read22(uint16_t * temp, uint16_t * hum);

/**
 * @brief Reads the current temperature and humidity from DHT22 sensor
 *
 * Reads the current temperature and humidity, returns an enumerated value
 * defining if the comunication with the sensor was succesfull. This function
 * puts the temperature and humidity readings on the provided buffers.
 *
 * @param temp Pointer to float to store the temperature reading
 * @param hum Pointer to float to store the humidity reading
 *
 * @return An enumerated value indictating the communication and device status
 */
enum dht_status dhtlib_float22(float * temp, float * hum);
int setdir(struct pt *pt);
uint8_t gettemdht();
uint8_t gethumdht();


#endif	/* MEASUREMENT_H */

