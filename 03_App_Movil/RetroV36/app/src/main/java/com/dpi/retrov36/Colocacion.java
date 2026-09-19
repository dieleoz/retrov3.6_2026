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

    /** null si el patron esta presente (o no hay x esperada); si no, el motivo. */
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
