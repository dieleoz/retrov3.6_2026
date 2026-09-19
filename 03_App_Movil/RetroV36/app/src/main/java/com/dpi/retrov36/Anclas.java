package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Oscuro y s_rep sacados de la campana (RF-APP-48, RF-CAL-36, RF-CAL-38). Java puro.
 *
 * - Ancla de un codigo: media de los OSCURO de inicio y fin de la sesion del banco en que se midio
 *   ese color. Si el OSCURO tiene deriva (|x_fin - x_ini| > max(5 ; 3 s_osc raiz(1/K_ini + 1/K_fin))),
 *   no hay ancla. Sin banco (campanas anteriores), la serie OSCURO elegida de la campana.
 * - s_rep: la de la A5 del inicio del banco (media de s entre colocaciones / media de P22, P28, P4);
 *   si no, la de la A5 mas reciente con K >= 2; si no, NaN (el criterio no se evalua).
 */
public final class Anclas {

    private Anclas() { }

    public static final class Valor {
        /** NaN si no hay. */
        public final double valor;
        public final String texto;

        Valor(double valor, String texto) {
            this.valor = valor;
            this.texto = texto;
        }
    }

    private static Campana.Serie seriePaso(Campana c, BancoCola.Paso p) {
        String id = c.seriePaso(p.orden);
        Campana.Serie s = id == null || id.isEmpty() ? null : c.serie(id);
        return s == null || s.anulada != null ? null : s;   // 3.6.14: una serie anulada no cuenta
    }

    /** Sesion del banco en que se midieron los patrones del codigo k (la del primer paso con ese codigo). */
    public static int sesionDe(BancoCola cola, char k) {
        for (BancoCola.Paso p : cola.pasos) {
            if ("PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo)) {
                return p.sesion;
            }
        }
        return -1;
    }

    public static Valor oscuro(BancoCola cola, Campana c, char k) {
        if (cola != null) {
            int ses = sesionDe(cola, k);
            List<Campana.Serie> osc = new ArrayList<>();
            for (BancoCola.Paso p : cola.pasos) {
                if ("OSCURO".equals(p.tipo) && p.sesion == ses) {
                    Campana.Serie s = seriePaso(c, p);
                    if (s != null && s.validos().length > 0) {
                        osc.add(s);
                    }
                }
            }
            if (osc.size() >= 2) {
                Campana.Serie ini = osc.get(0);
                Campana.Serie fin = osc.get(osc.size() - 1);
                double sOsc = Estadistica.media(new double[]{ini.desviacion(), fin.desviacion()});
                int ki = Math.max(1, ini.colocaciones().size());
                int kf = Math.max(1, fin.colocaciones().size());
                double lim = Math.max(5, 3 * (Double.isNaN(sOsc) ? 0 : sOsc) * Math.sqrt(1.0 / ki + 1.0 / kf));
                double dif = fin.media() - ini.media();
                if (Math.abs(dif) > lim) {
                    return new Valor(Double.NaN, String.format(Locale.US, "OSCURO con deriva en la sesión %d: %.1f → %.1f "
                            + "(%+.1f, límite %.1f): no hay ancla", ses, ini.media(), fin.media(), dif, lim));
                }
                double x = (ini.media() + fin.media()) / 2;
                return new Valor(x, String.format(Locale.US, "Oscuro (x = %.1f): media de %s y %s, sesión %d",
                        x, ini.id, fin.id, ses));
            }
            if (osc.size() == 1) {
                return new Valor(Double.NaN, "falta el OSCURO del final de la sesión " + ses + ": no hay ancla");
            }
            // P11 §5.3 (SPEC-Calibracion-V3.6.md:750-751): con cola, el ancla es la de la sesion; no se cae
            // al OSCURO de toda la campana.
            return new Valor(Double.NaN, "no calculable: falta el OSCURO de la sesión " + ses + " del banco");
        }
        Campana.Serie so = c.serieOscuro();
        if (so == null) {
            return new Valor(Double.NaN, "no calculable: falta OSCURO");
        }
        return new Valor(so.media(), String.format(Locale.US, "Oscuro (x = %.1f): serie %s de la campaña", so.media(), so.id));
    }

    public static Valor sRep(BancoCola cola, Campana c) {
        List<Double> rel = new ArrayList<>();
        if (cola != null) {
            for (BancoCola.Paso p : cola.pasos) {
                if ("A5".equals(p.tipo) && "A5-INICIO".equals(p.bloque) && p.sesion == 1) {
                    Campana.Serie s = seriePaso(c, p);
                    if (s != null && s.colocaciones().size() >= 2) {
                        rel.add(Veredicto.sEntre(s.colocaciones()) / s.media());
                    }
                }
            }
            if (!rel.isEmpty()) {
                double v = Estadistica.media(Estadistica.aVector(rel));
                return new Valor(v, String.format(Locale.US, "s_rep %.2f %% (A5 del inicio del banco, %d patrones)",
                        100 * v, rel.size()));
            }
        }
        A5.Resultado a = A5.evaluar(c);
        if (!Double.isNaN(a.sRepRel)) {
            return new Valor(a.sRepRel, String.format(Locale.US, "s_rep %.2f %% (A5)", 100 * a.sRepRel));
        }
        return new Valor(Double.NaN, "sin s_rep medida (falta la A5): los criterios de dispersión no se evalúan");
    }
}
