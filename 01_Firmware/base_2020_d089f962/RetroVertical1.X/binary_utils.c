#include "binary_utils.h"

unsigned int byte2UInt(unsigned char msb, unsigned char lsb) {
    unsigned int res;

    res = msb;
    res = (res << 8) | lsb;
    return res;
}

int isNumeric(const char *testStr) {
    if (testStr == 0 || *testStr == '\0' || isspace(*testStr))
        return 0;
    char * p;
    strtod(testStr, &p);

    return *p == '\0';
}

void removeChars(char* str, char c) {
    char *pr = str, *pw = str;
    while (*pr) {
        *pw = *pr++;
        pw += (*pw != c);
    }
    *pw = '\0';
}


//y=(((y2-y1)*(x-x1))/(x2-x1))+y1
float calculateLinearValue(float x, float x1, float y1, float x2, float y2) {
    return (((y2 - y1) / (x2 - x1)) * (x - x1))+y1;
}

void orderArray(unsigned int *sampleArray, unsigned char arrayLen) {
    unsigned char i, j;
    unsigned int a;

    for (i = 0; i < arrayLen; ++i) {
        for (j = i + 1; j < arrayLen; ++j) {
            if (sampleArray[i] > sampleArray[j]) {
                a = sampleArray[i];
                sampleArray[i] = sampleArray[j];
                sampleArray[j] = a;
            }
        }
    }
}
