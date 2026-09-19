/* 
 * File:   binary_utils.h
 * Author: Fabio
 *
 * Created on January 2, 2019, 3:43 PM
 */

#ifndef BINARY_UTILS_H
#define	BINARY_UTILS_H

#include <ctype.h>
#include <stdlib.h>

unsigned int byte2UInt(unsigned char msb, unsigned char lsb);
int isNumeric(const char *testStr);
void removeChars(char* str, char c);
float calculateLinearValue(float x, float x1, float y1, float x2, float y2);
void orderArray(unsigned int *sampleArray, unsigned char arrayLen);


#endif	/* BINARY_UTILS_H */

