package com.dpi.retrov36;

import java.util.Arrays;
import java.util.List;

/**
 * ProtocoloDisparos de disparos (3.6.15, decision PROTOCOLO-MIN de Diego; 3.6.16 con P14 §3). Java puro.
 *
 * - Por defecto, 1 colocacion x 4 disparos (mas el asentamiento), en la campana y en el banco ("rapido").
 * - En el banco, "preciso" = el K x M de la cola.
 * - A5 y OSCURO se quedan en K = 5 (fuente de la s_rep y del ancla; P10, P13).
 * - 3.6.16 (P14 §3, P10-C3): los patrones de AJUSTE y RE-MEDIDA de los codigos que se van a escribir (8, b, 5),
 *   P81 y toda RE-MEDIDA van en "preciso", 5 x 4, tambien en modo rapido. Decision de Diego (6048453), fila
 *   PROTOCOLO-AJUSTE de decisiones.csv (valor PRECISO, RAPIDO o LIBRE). Sin fila, PRECISO.
 * - Los patrones de TIPO-I-REPETIR (P34, P37, P43, P44, P38, P39, P49; 6048453) se repiten en preciso, 5 x 4.
 * - La re-medida de cada codigo NO cambia: 5 x 4 (Remedida3611, P12 y P13).
 */
public final class ProtocoloDisparos {

    public static final int K_DEFECTO = 1;
    public static final int M_DEFECTO = 4;
    /** A5 y OSCURO: nunca menos de 5 colocaciones. */
    public static final int K_CONTROL = 5;
    /** "Preciso" de los patrones de lo que se escribe (P10-C3). */
    public static final int K_PRECISO = 5;
    public static final int M_PRECISO = 4;
    /** Codigos que se van a escribir en SLV-002 (P14 §3). */
    public static final List<Character> CODIGOS_A_ESCRIBIR = Arrays.asList('8', 'b', '5');
    /** Valor provisional de PROTOCOLO-AJUSTE sin decision de Diego. */
    public static final String PROVISIONAL = "PRECISO";

    private ProtocoloDisparos() { }

    /** {K, M} para un paso del banco, con PROTOCOLO-AJUSTE = PRECISO (el provisional). */
    public static int[] efectivo(BancoCola.Paso p, boolean rapido) {
        return efectivo(p, rapido, null);
    }

    /** true si el paso es de lo que se escribe (AJUSTE/RE-MEDIDA de 8, b, 5, todos los del 5, P81 o una RE-MEDIDA). */
    public static boolean deAjuste(BancoCola.Paso p) {
        if (!"PATRON".equals(p.tipo)) {
            return false;
        }
        if ("RE-MEDIDA".equals(p.uso) || "P81".equals(p.patron)) {
            return true;
        }
        return p.codigo.length() == 1 && CODIGOS_A_ESCRIBIR.contains(p.codigo.charAt(0))
                && ("AJUSTE".equals(p.uso) || "5".equals(p.codigo));
    }

    /**
     * {K, M} para un paso del banco. protocoloAjuste: valor de PROTOCOLO-AJUSTE en decisiones.csv (null = el
     * provisional, PRECISO).
     */
    public static int[] efectivo(BancoCola.Paso p, boolean rapido, String protocoloAjuste) {
        if ("OSCURO".equals(p.tipo) || "A5".equals(p.tipo)) {
            return new int[]{Math.max(p.k, K_CONTROL), p.m > 0 ? p.m : M_DEFECTO};
        }
        String pa = protocoloAjuste == null ? PROVISIONAL : protocoloAjuste;
        if (deAjuste(p) && "PRECISO".equals(pa)) {
            return new int[]{K_PRECISO, M_PRECISO};
        }
        return rapido ? new int[]{K_DEFECTO, M_DEFECTO} : new int[]{p.k, p.m};
    }

    /** Como efectivo(p, rapido, protocoloAjuste), y ademas los patrones que Diego manda repetir van a 5 x 4. */
    public static int[] efectivo(BancoCola.Paso p, boolean rapido, String protocoloAjuste, List<String> repetir) {
        if (aRepetir(p, repetir)) {
            return new int[]{K_PRECISO, M_PRECISO};
        }
        return efectivo(p, rapido, protocoloAjuste);
    }

    /** true si el paso mide un patron de TIPO-I-REPETIR. */
    public static boolean aRepetir(BancoCola.Paso p, List<String> repetir) {
        return repetir != null && "PATRON".equals(p.tipo) && repetir.contains(p.patron);
    }

    /** {K, M} minimo que exige el ajuste de un codigo; null si no se exige nada (LIBRE). */
    public static int[] requerido(String protocoloAjuste) {
        String pa = protocoloAjuste == null ? PROVISIONAL : protocoloAjuste;
        if ("LIBRE".equals(pa)) {
            return null;
        }
        return "RAPIDO".equals(pa) ? new int[]{K_DEFECTO, M_DEFECTO} : new int[]{K_PRECISO, M_PRECISO};
    }

    /** {K, M} de una serie: colocaciones y disparos validos de la mas larga. */
    public static int[] deSerie(Campana.Serie s) {
        int m = 0;
        for (double[] g : s.colocaciones()) {
            m = Math.max(m, g.length);
        }
        return new int[]{s.colocaciones().size(), m};
    }

    /** null si la serie cumple lo requerido (K >= K_req y M >= M_req: 5 x 9 de la 3.6.11 vale como 5 x 4); si no, "P34 3×3". */
    public static String incumple(String patron, Campana.Serie s, int[] req) {
        if (req == null || s == null) {
            return null;
        }
        int[] km = deSerie(s);
        return km[0] >= req[0] && km[1] >= req[1] ? null : patron + " " + km[0] + "×" + km[1];
    }

    public static String texto(int[] km, boolean rapido) {
        return km[0] + " × " + km[1] + (rapido ? " (rápido)" : " (preciso)");
    }
}
