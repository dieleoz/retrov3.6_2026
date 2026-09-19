package com.dpi.retrov36;

/**
 * RTV 1.0.0-rc3: el ritmo con el que la app habla con el equipo, en Java puro.
 *
 * Estaba dentro de {@link Cliente}, que depende de android.os y no corre en la JVM: la regla de la pausa
 * solo se podia razonar leyendola. Aqui se puede EJECUTAR, que es lo que hace {@code RitmoTest}. La
 * diferencia importa: de esta regla depende cuanto dura una jornada de banco, y ese numero se habia
 * estimado de dos formas distintas sin poder decidir cual valia.
 *
 * Las tres constantes son las que {@link Cliente} usa; aqui solo vive la decision.
 */
public final class Ritmo {

    private Ritmo() { }

    /** Pausa normal desde el envio anterior. */
    public static final long PAUSA_ENTRE_ENVIOS_MS = 1500;
    /** Pausa normal desde el ultimo byte recibido. */
    public static final long PAUSA_TRAS_RX_MS = 600;
    /**
     * Pausa entre dos tramas '#' seguidas con un protocolo que administra.
     *
     * La justificacion historica ({@link Cliente}) es que "una trama '#' nunca dispara medida". En la V3.6
     * es cierto. En la V4.6 NO: "#X,k#" mide (1000 muestras encendiendo la luz del codigo,
     * V4.6:Calibracion.c:980 y Optical_Capture.c:137). La regla no se cambia aqui —es una contradiccion
     * abierta, anotada en 05_Documentacion/CONTRADICCIONES-ABIERTAS-RTV.md— pero deja de estar escondida.
     */
    public static final long PAUSA_ENTRE_ALMOHADILLAS_MS = 150;

    public static boolean esAlmohadilla(String peticion) {
        return peticion != null && peticion.startsWith("#");
    }

    /**
     * true si esta peticion va con la pausa corta: es trama '#', la anterior tambien lo era, y el protocolo
     * habla el contrato de administracion.
     *
     * Consecuencia que conviene tener delante: una peticion que NO empieza por '#' rompe la cadena, y no
     * solo ella paga la pausa larga, tambien la siguiente, porque deja {@code anteriorFueAlmohadilla} en
     * false.
     */
    public static boolean pausaCorta(String peticion, boolean anteriorFueAlmohadilla, Protocolo proto) {
        return esAlmohadilla(peticion) && anteriorFueAlmohadilla && proto != null && proto.administra();
    }

    /** Pausa exigida desde el ENVIO anterior, en ms. */
    public static long pausaEnvioMs(boolean corta) {
        return corta ? PAUSA_ENTRE_ALMOHADILLAS_MS : PAUSA_ENTRE_ENVIOS_MS;
    }

    /** Pausa exigida desde el ULTIMO BYTE RECIBIDO, en ms. Con la corta es cero. */
    public static long pausaRxMs(boolean corta) {
        return corta ? 0 : PAUSA_TRAS_RX_MS;
    }

    /**
     * Lo que se espera de verdad antes de enviar, en ms: es el maximo de las dos condiciones, no su suma
     * (Cliente.esperarPausa). Si el equipo tardo {@code respuestaMs} en contestar, ese tiempo ya cuenta
     * como pausa desde el envio anterior; desde el ultimo byte recibido no ha pasado nada.
     *
     * @param respuestaMs cuanto tardo la peticion ANTERIOR desde su envio hasta su ultimo byte.
     */
    public static long esperaMs(boolean corta, long respuestaMs) {
        long porEnvio = pausaEnvioMs(corta) - Math.max(0, respuestaMs);
        long porRx = pausaRxMs(corta);
        return Math.max(0, Math.max(porEnvio, porRx));
    }
}
