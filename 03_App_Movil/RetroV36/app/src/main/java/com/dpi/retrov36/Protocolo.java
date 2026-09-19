package com.dpi.retrov36;

/**
 * Protocolo de disparos (3.6.15, decision PROTOCOLO-MIN de Diego en DECISIONES-Diego-2026-09-19.md). Java puro.
 *
 * - Por defecto, 1 colocacion x 4 disparos (mas el asentamiento), en la campana y en el banco ("rapido").
 * - En el banco, "preciso" = el K x M de la cola (K3/K5 x 4).
 * - Excepcion obligatoria: A5 y OSCURO se quedan en K = 5 (fuente de la s_rep y del ancla; P10, P13).
 * - La re-medida de cada codigo NO cambia: 5 x 4 (Remedida3611, P12 y P13), que es mas que 1 x 4.
 */
public final class Protocolo {

    public static final int K_DEFECTO = 1;
    public static final int M_DEFECTO = 4;
    /** A5 y OSCURO: nunca menos de 5 colocaciones. */
    public static final int K_CONTROL = 5;

    private Protocolo() { }

    /** {K, M} para un paso del banco. */
    public static int[] efectivo(BancoCola.Paso p, boolean rapido) {
        if ("OSCURO".equals(p.tipo) || "A5".equals(p.tipo)) {
            return new int[]{Math.max(p.k, K_CONTROL), p.m > 0 ? p.m : M_DEFECTO};
        }
        return rapido ? new int[]{K_DEFECTO, M_DEFECTO} : new int[]{p.k, p.m};
    }

    public static String texto(int[] km, boolean rapido) {
        return km[0] + " × " + km[1] + (rapido ? " (rápido)" : " (preciso, de la cola)");
    }
}
