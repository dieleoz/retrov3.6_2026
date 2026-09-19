package com.dpi.retrov36;

import java.util.Locale;

/**
 * Filtros de colocacion de P10-C1 (REVISION-Arquitectura-P10-V3.6.md §2.1). Java puro.
 *
 * 1. Patron presente: el disparo de asentamiento tiene que caer en [0,8 ; 1,2] x_banco del
 *    patron. Si no, "Coloque P28" y no se gasta ningun disparo de la serie. Caso real: el
 *    12:17:02 el asentamiento dio 592, a 27 cuentas del oscuro, con P28 esperado en 2277,9.
 * 2. Coherencia por par (re-medida, 'e' y codigo alternados): cualquier par con
 *    |d_i| > max(3 ; 2 % R_#G(x_e,i)) invalida la colocacion. Sustituye a la comprobacion 1
 *    de RF-CAL-39 (|d| medio frente a 3 s_d / raiz(n)), que un par atipico dejaba pasar.
 */
public final class Colocacion {

    public static final double PRESENTE_MIN = 0.8;
    public static final double PRESENTE_MAX = 1.2;

    private Colocacion() { }

    /** Margen sobre el oscuro: un asentamiento por debajo de x_oscuro + 40 es "sin patron" (592 a 27 del oscuro, 19-sep). */
    public static final double MARGEN_OSCURO = 40;

    /**
     * 3.6.11 (QA-3610-01): filtro segun el origen de la x esperada.
     * @param tolerancia 0,20 o 0,30 (BancoCola.Paso.toleranciaX); NaN = sin rango, solo "no esta en oscuro".
     * @param xOscuro x del oscuro de la campana (o 575 si no hay).
     * @return null si el patron esta presente; si no, el motivo.
     */
    public static String patronAusente(String patron, double xAsentamiento, double xEsperada, double tolerancia,
                                       double xOscuro) {
        if (Double.isNaN(xAsentamiento)) {
            // QA-3610-13: sin lectura no es "patron ausente", es un equipo que no responde.
            return "El asentamiento no dio lectura (el equipo no respondió o la trama no era válida): "
                    + "no es un patrón ausente. Compruebe la conexión y repita la colocación.";
        }
        if (Double.isNaN(tolerancia) || Double.isNaN(xEsperada) || xEsperada <= 0) {
            double lim = xOscuro + MARGEN_OSCURO;
            if (xAsentamiento <= lim) {
                return String.format(Locale.US, "Coloque %s: el asentamiento dio x = %.0f, en el oscuro (x oscuro %.0f; "
                        + "hace falta más de %.0f). No se ha gastado ningún disparo de la serie.", patron, xAsentamiento,
                        xOscuro, lim);
            }
            return null;
        }
        double min = (1 - tolerancia) * xEsperada;
        double max = (1 + tolerancia) * xEsperada;
        if (xAsentamiento < min || xAsentamiento > max) {
            return String.format(Locale.US, "Coloque %s: el asentamiento dio x = %.0f y se esperaba %.0f "
                    + "(entre %.0f y %.0f, ±%.0f %%). No se ha gastado ningún disparo de la serie.",
                    patron, xAsentamiento, xEsperada, min, max, 100 * tolerancia);
        }
        return null;
    }

    /** null si el patron esta presente (o no hay x esperada); si no, el motivo. Criterio de P10-C1 (x_banco, +/-20 %). */
    public static String patronAusente(String patron, double xAsentamiento, double xEsperada) {
        if (Double.isNaN(xEsperada) || xEsperada <= 0) {
            return null;
        }
        if (Double.isNaN(xAsentamiento) || xAsentamiento < PRESENTE_MIN * xEsperada
                || xAsentamiento > PRESENTE_MAX * xEsperada) {
            return String.format(Locale.US, "Coloque %s: el asentamiento dio x = %.0f y se esperaba %.0f "
                    + "(entre %.0f y %.0f). No se ha gastado ningún disparo de la serie.",
                    patron, xAsentamiento, xEsperada, PRESENTE_MIN * xEsperada, PRESENTE_MAX * xEsperada);
        }
        return null;
    }

    /** Umbral de un par: max(3 ; 2 % R_#G). */
    public static double umbralPar(double rG) {
        return Math.max(3, 0.02 * Math.abs(rG));
    }

    /**
     * @param rCodigo respuesta del codigo en cada par; @param rG R de la curva #G en la x del 'e' del par.
     * @return null si la colocacion es valida; si no, el primer par que la invalida.
     */
    public static String paresIncoherentes(double[] rCodigo, double[] rG) {
        for (int i = 0; i < rCodigo.length; i++) {
            double d = rCodigo[i] - rG[i];
            if (Math.abs(d) > umbralPar(rG[i])) {
                return String.format(Locale.US, "colocación no válida: par %d con d = %+.1f (umbral %.1f); "
                        + "el equipo se movió entre 'e' y el código", i + 1, d, umbralPar(rG[i]));
            }
        }
        return null;
    }
}
