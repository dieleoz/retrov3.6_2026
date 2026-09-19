#include <xc.h>
#pragma config WDTE = OFF
#define EC_NUM 12
unsigned int reflectivityValue;
void o_amarilloIntenso(void){
    reflectivityValue = (double)(-0.000000073*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue + 0.000282508*(double)reflectivityValue*(double)reflectivityValue + 0.124075350*(double)reflectivityValue - 130);
}
void o_blancoIntenso(void){
    reflectivityValue = (double)(-0.000086541*(double)reflectivityValue*(double)reflectivityValue + 0.619668128*(double)reflectivityValue - 303);                   
}
void o_naranjaIntenso(void){
    reflectivityValue = (double)(0.000090731*(double)reflectivityValue*(double)reflectivityValue + 0.001064329*(double)reflectivityValue - 21);
}
void o_rojoIntenso(void){
    reflectivityValue = (double)(0.000000147*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000668624*(double)reflectivityValue*(double)reflectivityValue + 1.082394050*(double)reflectivityValue - 393);
}
void o_azulIntenso(void){
    reflectivityValue = (double)(0.000000026*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000018402*(double)reflectivityValue*(double)reflectivityValue + 0.102152670*(double)reflectivityValue - 49);
}
void o_verdeIntenso(void){
    reflectivityValue = (double)(0.000000163*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000756695*(double)reflectivityValue*(double)reflectivityValue + 1.213142724*(double)reflectivityValue - 442);
}



        
void o_amarilloOpaco(void){
    reflectivityValue = (double)(0.000000086*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000299026*(double)reflectivityValue*(double)reflectivityValue + 0.446572329*(double)reflectivityValue - 161);
}
void o_blancoOpaco(void){
    reflectivityValue = (double)(0.000087404*(double)reflectivityValue*(double)reflectivityValue + 0.013617682*(double)reflectivityValue - 27);
}
void o_naranjaOpaco(void){
    reflectivityValue = (double)(0.000090731*(double)reflectivityValue*(double)reflectivityValue + 0.001064329*(double)reflectivityValue - 21);                            
}
void o_rojoOpaco(void){
    reflectivityValue = (double)(0.000113098*(double)reflectivityValue*(double)reflectivityValue - 0.076130418*(double)reflectivityValue + 10);                               
}
void o_azulOpaco(void){
    reflectivityValue = (double)(0.000000026*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000018402*(double)reflectivityValue*(double)reflectivityValue + 0.102152670*(double)reflectivityValue - 49);
}
void o_verdeOpaco(void){
    reflectivityValue = (double)(0.000000163*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue - 0.000756695*(double)reflectivityValue*(double)reflectivityValue + 1.213142724*(double)reflectivityValue - 442);
}
static const double coefFabrica[EC_NUM][4] = {
    /* 0 '1' blancoIntenso   (:7)  */ { 0.0,          -0.000086541,  0.619668128, -303 },
    /* 1 '2' amarilloIntenso (:4)  */ {-0.000000073,   0.000282508,  0.124075350, -130 },
    /* 2 '3' verdeIntenso    (:19) */ { 0.000000163,  -0.000756695,  1.213142724, -442 },
    /* 3 '4' rojoIntenso     (:13) */ { 0.000000147,  -0.000668624,  1.082394050, -393 },
    /* 4 '5' azulIntenso     (:16) */ { 0.000000026,  -0.000018402,  0.102152670,  -49 },
    /* 5 '6' naranjaIntenso  (:10) */ { 0.0,           0.000090731,  0.001064329,  -21 },
    /* 6 '7' blancoOpaco     (:29) */ { 0.0,           0.000087404,  0.013617682,  -27 },
    /* 7 '8' amarilloOpaco   (:26) */ { 0.000000086,  -0.000299026,  0.446572329, -161 },
    /* 8 'a' verdeOpaco      (:41) */ { 0.000000163,  -0.000756695,  1.213142724, -442 },
    /* 9 'b' rojoOpaco       (:35) */ { 0.0,           0.000113098, -0.076130418,   10 },
    /*10 'c' azulOpaco       (:38) */ { 0.000000026,  -0.000018402,  0.102152670,  -49 },
    /*11 'd' naranjaOpaco    (:32) */ { 0.0,           0.000090731,  0.001064329,  -21 }
};
double coefCal[EC_NUM][4];
void aplicarEcuacion(unsigned char k) {
    reflectivityValue = (double)(coefCal[k][0]*(double)reflectivityValue*(double)reflectivityValue*(double)reflectivityValue
                               + coefCal[k][1]*(double)reflectivityValue*(double)reflectivityValue
                               + coefCal[k][2]*(double)reflectivityValue
                               + coefCal[k][3]);
}
typedef void (*fn_t)(void);
const fn_t orig[EC_NUM] = {o_blancoIntenso,o_amarilloIntenso,o_verdeIntenso,o_rojoIntenso,o_azulIntenso,o_naranjaIntenso,o_blancoOpaco,o_amarilloOpaco,o_verdeOpaco,o_rojoOpaco,o_azulOpaco,o_naranjaOpaco};
volatile unsigned long nComparados = 0, nDistintos = 0, suma = 0;
volatile unsigned int primerX = 0xFFFF; volatile unsigned char primerK = 0xFF;
void main(void){
    unsigned char k, i; unsigned int x, r1, r2;
    for (k = 0; k < EC_NUM; k++) for (i = 0; i < 4; i++) coefCal[k][i] = coefFabrica[k][i];
    for (k = K_DESDE; k <= K_HASTA; k++) {
        x = 0;
        do {
            reflectivityValue = x; orig[k](); r1 = reflectivityValue;
            reflectivityValue = x; aplicarEcuacion(k); r2 = reflectivityValue;
            nComparados++; suma += r1;
            if (r1 != r2) { nDistintos++; if (primerK == 0xFF) { primerK = k; primerX = x; } }
            x++;
        } while (x != 0);
    }
    NOP();
    while (1) { NOP(); }
}
