package com.dpi.retrov36;

import java.util.Locale;

/**
 * Bateria con la orden 9 (RF-CAL-41, SPEC-Calibracion-V3.6.md §12.8, REVISION-P10 §4). Java puro.
 *
 * n = trunc(90,91 (V - 10) - 30,09), asi que V = 10 + (n + 30,09) / 90,91 es el extremo inferior
 * de un intervalo de 11 mV. Casos: n = 5 (10,39-10,44 V); n = 0 si V < 10,342 V; n = 177 solo con
 * V > 12,28 V (recorte a 99); sin recorte, n puede pasar de 177 (13 V da 242). La V es la de la vuelta
 * anterior, antes del destello de la lampara (SPEC-Calibracion §12.8, corregida en 1f1c4e3).
 * PA-17 (provisional hasta T-C44/T-C45): aviso con n < 19; bloqueo de ESCRITURAS con n = 0 o sin
 * respuesta. La medida nunca se bloquea. La 9 dispara la lampara: nunca en mitad de una serie.
 */
public final class Bateria {

    public static final int AVISO_N = 19;

    private Bateria() { }

    public static final class Lectura {
        /** null si no hubo respuesta. */
        public final Integer n;
        public final String texto;
        public final boolean aviso;
        public final boolean bloqueaEscrituras;

        Lectura(Integer n, String texto, boolean aviso, boolean bloqueaEscrituras) {
            this.n = n;
            this.texto = texto;
            this.aviso = aviso;
            this.bloqueaEscrituras = bloqueaEscrituras;
        }
    }

    /** V (extremo inferior del intervalo) para n >= 10. */
    public static double voltios(int n) {
        return 10 + (n + 30.09) / 90.91;
    }

    /** n de ":n:", o null si la trama no es de bateria. */
    public static Integer n(String trama) {
        if (trama == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(":(\\d{1,4}):").matcher(trama);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** @param n null = sin respuesta. */
    public static Lectura interpretar(Integer n) {
        if (n == null) {
            return new Lectura(null, "Batería: sin respuesta a 9. Se bloquean las escrituras (#S, #F, #SC, #SN, #FT); "
                    + "la medida sigue.", true, true);
        }
        String v;
        if (n == 0) {
            v = "< 10,34 V";
        } else if (n == 5) {
            v = "10,39-10,44 V";
        } else if (n == 177) {
            v = "> 12,28 V (posible recorte a 99)";
        } else if (n >= 10) {
            v = String.format(Locale.US, "%.2f V", voltios(n)).replace('.', ',');
        } else {
            v = "n = " + n + " (valor que la fórmula no da)";
        }
        boolean aviso = n < AVISO_N;
        boolean bloqueo = n == 0;
        String t = "Batería " + v + " (n = " + n + ")"
                + (bloqueo ? ": bloqueadas las escrituras; la medida sigue." : (aviso ? ": BAJA, cámbiela pronto." : ""));
        return new Lectura(n, t, aviso, bloqueo);
    }
}
