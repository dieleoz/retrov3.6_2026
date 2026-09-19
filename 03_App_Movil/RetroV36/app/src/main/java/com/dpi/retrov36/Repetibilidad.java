package com.dpi.retrov36;

import java.util.Locale;

/**
 * Prueba 6: 5 lecturas seguidas del mismo codigo sobre el mismo patron.
 *
 * El umbral SD_MAXIMA es PROVISIONAL: no sale de ninguna medida. Hay que
 * fijarlo con la repetibilidad real del equipo (PROCEDIMIENTO §4, paso 3).
 */
public final class Repetibilidad {

    public static final int LECTURAS = 5;
    public static final double SD_MAXIMA = 10;

    private Repetibilidad() { }

    public static final class Resultado {
        public final double media;
        public final double desviacion;
        public final boolean apto;
        public final String texto;

        Resultado(double media, double desviacion, boolean apto, String texto) {
            this.media = media;
            this.desviacion = desviacion;
            this.apto = apto;
            this.texto = texto;
        }
    }

    /** @param x lecturas validas; @param fallidas lecturas sin respuesta o no invertibles. */
    public static Resultado evaluar(double[] x, int fallidas) {
        double m = Estadistica.media(x);
        double s = Estadistica.desviacion(x);
        boolean apto = fallidas == 0 && x.length >= 2 && s <= SD_MAXIMA;
        String t = String.format(Locale.US, "%d lecturas validas, %d fallidas; media %.1f, desviacion %.2f cuentas (umbral provisional %.0f)",
                x.length, fallidas, m, s, SD_MAXIMA);
        return new Resultado(m, s, apto, t);
    }
}
