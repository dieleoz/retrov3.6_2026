package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Re-medida guiada, RF-CAL-39 corregida en 1f1c4e3 (P10-C1, D-19). Java puro.
 *
 * Por colocacion (antes de contarla): patron presente (asentamiento en [0,8 ; 1,2] x_banco) y
 * coherencia por par (|d_i| <= max(3 ; 2 % R_#G(x_e,i))). Una colocacion no valida se registra y se
 * repite; nunca cuenta.
 * Con las K colocaciones validas:
 *   2. reproduccion de la campana: |x_rem - x_banco| <= 2 s_rep x_banco raiz(1/K_rem + 1/K_banco),
 *      con s_rep MEDIDA (RF-CAL-38): si no la hay, el criterio no se evalua (NO_EVALUABLE);
 *   3. frente al certificado: |R - R_cert| <= max(10 % ; 2 unidades).
 * Conforme si pasan la 2 y la 3.
 */
public final class Remedida3611 {

    public static final int K = 5;
    public static final int M = 4;

    private Remedida3611() { }

    public static final class Resultado {
        /** CONFORME, NO_CONFORME o NO_EVALUABLE. */
        public final String estado;
        public final double xRem;
        public final double rMedida;
        public final String texto;

        Resultado(String estado, double xRem, double rMedida, String texto) {
            this.estado = estado;
            this.xRem = xRem;
            this.rMedida = rMedida;
            this.texto = texto;
        }
    }

    /** Colocacion: null si valida; si no, el motivo (se registra como NO_VALIDA y se repite). */
    public static String colocacion(String patron, double xAsentamiento, double xBanco, double[] xE, double[] rK,
                                    Ecuacion curvaG) {
        String aus = Colocacion.patronAusente(patron, xAsentamiento, xBanco);
        if (aus != null) {
            return aus;
        }
        double[] rG = new double[xE.length];
        for (int i = 0; i < xE.length; i++) {
            rG[i] = curvaG.respuestaFloat32((int) Math.round(xE[i]));
        }
        return Colocacion.paresIncoherentes(rK, rG);
    }

    /** Criterios 2 y 3 con los valores ya agregados. sRepRel NaN = no hay s_rep medida. */
    public static Resultado criterios(String patron, double cert, double xRem, int kRem, double xBanco, int kBanco,
                                      double sRepRel, String origenSrep, double rMedida) {
        if (Double.isNaN(sRepRel) || sRepRel <= 0) {
            return new Resultado("NO_EVALUABLE", xRem, rMedida, patron
                    + ": no hay s_rep medida (A5 del inicio o K >= 3 en la serie); el criterio no se evalúa (RF-CAL-38)");
        }
        if (Double.isNaN(xBanco)) {
            return new Resultado("NO_EVALUABLE", xRem, rMedida, patron + ": falta la serie del banco del patrón de re-medida");
        }
        double lim2 = 2 * sRepRel * xBanco * Math.sqrt(1.0 / kRem + 1.0 / kBanco);
        boolean ok2 = Math.abs(xRem - xBanco) <= lim2;
        double lim3 = Math.max(0.10 * cert, 2);
        boolean ok3 = Math.abs(rMedida - cert) <= lim3;
        String t = String.format(Locale.US, "%s: x %.1f frente a %.1f del banco (%+.1f %%, límite ±%.1f; s_rep %.2f %% %s) %s; "
                        + "R %.1f frente a cert. %.0f (%+.1f %%, límite ±%.1f) %s",
                patron, xRem, xBanco, 100 * (xRem - xBanco) / xBanco, lim2, 100 * sRepRel, origenSrep, ok2 ? "OK" : "FALLA",
                rMedida, cert, 100 * (rMedida - cert) / cert, lim3, ok3 ? "OK" : "FALLA");
        return new Resultado(ok2 && ok3 ? "CONFORME" : "NO_CONFORME", xRem, rMedida, t);
    }

    /** Agrega las colocaciones validas (media de medias) y evalua. */
    public static Resultado evaluar(String patron, double cert, List<double[]> xPorCol, List<double[]> rPorCol,
                                    double xBanco, int kBanco, double sRepRel, String origenSrep) {
        return criterios(patron, cert, mediaDeMedias(xPorCol), xPorCol.size(), xBanco, kBanco, sRepRel, origenSrep,
                mediaDeMedias(rPorCol));
    }

    static double mediaDeMedias(List<double[]> g) {
        List<Double> m = new ArrayList<>();
        for (double[] v : g) {
            if (v.length > 0) {
                m.add(Estadistica.media(v));
            }
        }
        return Estadistica.media(Estadistica.aVector(m));
    }
}
