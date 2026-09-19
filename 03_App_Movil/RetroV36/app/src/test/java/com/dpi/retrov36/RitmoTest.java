package com.dpi.retrov36;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * RTV 1.0.0-rc3: el ritmo con el que la app habla con el equipo, EJECUTADO.
 *
 * Habia dos cuentas incompatibles sobre lo que cuesta intercalar una "#T#" entre dos disparos del banco de
 * la V4.6, y ninguna de las dos se podia comprobar porque la regla vivia dentro de {@link Cliente}, que
 * depende de android.os y no corre en la JVM. Ahora vive en {@link Ritmo} y esta prueba la ejecuta.
 *
 * <b>Las dos cuentas y quien tenia razon:</b>
 * <ul>
 * <li>"anade 150 ms por disparo, +3 %": se quedaba corta por arriba. La pausa corta es de 150 ms desde el
 *     ENVIO anterior, y la respuesta de "#X" tarda ~2 s, asi que esos 150 ms ya han pasado: la "#T#" sale
 *     sin esperar nada. Lo unico que se paga son los 142 ms que el "#X" siguiente espera tras una "#T#"
 *     que contesta en 8 ms. <b>75 ms por disparo</b>, no 150.</li>
 * <li>"rompe el ritmo corto y paga 1500 + 1500, duplicando la campana": el mecanismo es real, pero NO se
 *     aplica a "#T#", que empieza por '#' igual que "#X". Se aplica a cualquier peticion que no empiece por
 *     '#', y entonces si: <b>1050 ms por disparo</b>, porque la penalizacion la pagan la peticion intrusa y
 *     tambien la siguiente.</li>
 * </ul>
 *
 * <b>Y aun asi la temperatura va por SERIE, no por disparo</b>, porque el coste nunca fue el problema: el
 * canal de temperatura del firmware se refresca cada 10 s (V4.6:Aplicacion.h:14,16, usados en
 * Aplicacion.c:193) y los disparos van cada ~2 s, asi que por disparo devolveria el mismo numero cinco
 * veces seguidas. Se mide lo que cuesta para poder decir que NO se descarta por caro.
 *
 * Los tiempos de respuesta son cotas del fuente del firmware, NO cronometradas en hardware: "#X" ~2010 ms
 * (1000 ms de calentamiento de luz + 1000 de muestreo a 1 ms, V4.6:Optical_Capture.c:137,181 y
 * Optical_Capture.h:13) y "#T#" ~8 ms, que no mide (V4.6:Calibracion.c:983-990).
 */
public class RitmoTest {

    /** Respuesta de "#X,k#" en la V4.6, del fuente del firmware. */
    private static final long X_MS = 2010;
    /** Respuesta de "#T#": no mide. */
    private static final long T_MS = 8;

    private static final Protocolo V46 = new ProtocoloV46();

    /** Recorre una secuencia de peticiones y devuelve los ms totales (esperas + respuestas). */
    private static long secuencia(String[] peticiones, long[] respuestas) {
        boolean anterior = true;               // el banco viene encadenando tramas '#'
        long previa = X_MS;                    // ...y la ultima respuesta fue un disparo
        long total = 0;
        for (int i = 0; i < peticiones.length; i++) {
            boolean corta = Ritmo.pausaCorta(peticiones[i], anterior, V46);
            total += Ritmo.esperaMs(corta, previa) + respuestas[i];
            anterior = Ritmo.esAlmohadilla(peticiones[i]);
            previa = respuestas[i];
        }
        return total;
    }

    @Test
    public void laPausaCortaSeDecideComoDiceElCodigo() {
        // "#X" tras "#X" con la V4.6: corta, porque ProtocoloV46.administra() es true
        assertTrue(V46.administra());
        assertTrue(Ritmo.pausaCorta("#X,1#", true, V46));
        assertTrue("#T# tambien empieza por '#'", Ritmo.pausaCorta("#T#", true, V46));
        // lo que NO empieza por '#' rompe la cadena
        assertFalse(Ritmo.pausaCorta("9", true, V46));
        assertFalse(Ritmo.pausaCorta("e", true, V46));
        // y con un protocolo que no administra, nunca es corta
        assertFalse(Ritmo.pausaCorta("#X,1#", true, new ProtocoloV2020()));
        assertFalse(Ritmo.pausaCorta("#X,1#", true, null));
        // tras algo que no era '#', la siguiente tampoco es corta
        assertFalse(Ritmo.pausaCorta("#X,1#", false, V46));
    }

    @Test
    public void laEsperaEsElMaximoDeLasDosCondicionesNoLaSuma() {
        // corta: 150 desde el envio, 0 desde el rx. Tras una respuesta de 2010 ms no se espera nada.
        assertEquals(0, Ritmo.esperaMs(true, 2010));
        // tras una respuesta de 8 ms quedan 142 de los 150
        assertEquals(142, Ritmo.esperaMs(true, 8));
        // larga: max(1500 - 8, 600) = 1492, no 1500 + 600
        assertEquals(1492, Ritmo.esperaMs(false, 8));
        assertEquals("max(1500-2010, 600) = 600", 600, Ritmo.esperaMs(false, 2010));
    }

    /** El numero que zanja la discusion: lo que anade de verdad una "#T#" entre dos disparos. */
    @Test
    public void unaTemperaturaIntercaladaAnade75MsPorDisparo() {
        long sinT = secuencia(new String[]{"#X,1#", "#X,1#"}, new long[]{X_MS, X_MS});
        long conT = secuencia(new String[]{"#X,1#", "#T#", "#X,1#"}, new long[]{X_MS, T_MS, X_MS});
        assertEquals(2 * X_MS, sinT);
        assertEquals("los 8 ms de la #T# mas los 142 que espera el #X siguiente", 2 * X_MS + 150, conT);
        assertEquals("por disparo", 75, (conT - sinT) / 2);

        // Banco representativo V4.6: 1410 disparos, base 6579 s (BancoCola.segundos). +1,6 %.
        double base = 6579.0;
        double anade = 75.0 * 1410 / 1000.0;
        assertTrue("anade menos del 2 % del banco: " + (100 * anade / base), 100 * anade / base < 2.0);
    }

    /** Y lo que habria costado si la trama intercalada no empezase por '#': catorce veces mas. */
    @Test
    public void algoQueNoEmpiezaPorAlmohadillaSiSaldriaCaro() {
        long sinT = secuencia(new String[]{"#X,1#", "#X,1#"}, new long[]{X_MS, X_MS});
        long conOtro = secuencia(new String[]{"#X,1#", "9", "#X,1#"}, new long[]{X_MS, T_MS, X_MS});
        assertEquals("la intrusa paga 600 y el #X siguiente 1492", 2 * X_MS + T_MS + 600 + 1492, conOtro);
        assertEquals(1050, (conOtro - sinT) / 2);
        assertTrue("catorce veces mas caro que una trama '#'", (conOtro - sinT) / (double) (150) > 13);
    }

    /** Cliente sigue usando exactamente estos valores: los alias no se han desviado. */
    @Test
    public void clienteYRitmoNoSeHanDesviado() {
        assertEquals(Ritmo.PAUSA_ENTRE_ENVIOS_MS, Cliente.PAUSA_ENTRE_ENVIOS_MS);
        assertEquals(Ritmo.PAUSA_TRAS_RX_MS, Cliente.PAUSA_TRAS_RX_MS);
        assertEquals(Ritmo.PAUSA_ENTRE_ALMOHADILLAS_MS, Cliente.PAUSA_ENTRE_ALMOHADILLAS_MS);
        assertEquals(1500, Ritmo.PAUSA_ENTRE_ENVIOS_MS);
        assertEquals(600, Ritmo.PAUSA_TRAS_RX_MS);
        assertEquals(150, Ritmo.PAUSA_ENTRE_ALMOHADILLAS_MS);
    }
}
