package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Medida puente P9-A5 (REVISION-Arquitectura-P9-V3.6.md §8.1). Java puro.
 *
 * P22, P28 y P4 (x ~ 1500, 2280, 3320), 5 colocaciones independientes cada uno. Da la
 * s_rep entre colocaciones (§4 ter) y el puente: la media de las 5 colocaciones de cada
 * patron frente a su serie de la campana, dentro de max(2 * s_rep ; 4 %). Criterio de
 * reversion a la 3.6.1: los tres desplazados en el mismo sentido mas alla de ese margen.
 *
 * Asentamiento + 9 disparos por colocacion (3.6.9). Las series A5 se guardan con veredicto "A5" y sin aceptar:
 * no entran en el ajuste ni cambian la serie elegida de la campana.
 */
public final class A5 {

    public static final String[] PATRONES = {"P22", "P28", "P4"};
    public static final int COLOCACIONES = 5;
    /** 3.6.9: 5 x 9 (mas el asentamiento por colocacion), como pide el documento. La 3.6.8 usaba 5 x 3. */
    public static final int DISPAROS = 9;
    public static final double MARGEN_REL = 0.04;
    public static final String VEREDICTO = "A5";

    private A5() { }

    public static final class Resultado {
        public final String texto;
        /** PENDIENTE, OK, NO_CONCLUYENTE o REVERTIR. */
        public final String veredicto;
        /** Media de s_rep / media, en los patrones medidos (NaN si ninguno). */
        public final double sRepRel;

        Resultado(String texto, String veredicto, double sRepRel) {
            this.texto = texto;
            this.veredicto = veredicto;
            this.sRepRel = sRepRel;
        }
    }

    public static Campana.Serie ultimaA5(Campana c, String patron) {
        Campana.Serie u = null;
        for (Campana.Serie s : c.seriesA5(patron)) {
            u = s;
        }
        return u;
    }

    public static Resultado evaluar(Campana c) {
        StringBuilder t = new StringBuilder("Medida puente P9-A5 (P22, P28, P4; 5 colocaciones):\n");
        int medidos = 0;
        int fuera = 0;
        int arriba = 0;
        int abajo = 0;
        List<Double> reps = new ArrayList<>();
        for (String nombre : PATRONES) {
            Campana.Serie a = ultimaA5(c, nombre);
            Campana.Serie ref = c.elegida(nombre);
            if (a == null) {
                t.append("  ").append(nombre).append(": sin medir\n");
                continue;
            }
            List<double[]> g = a.colocaciones();
            double m5 = a.media();
            double sRep = g.size() >= 2 ? Veredicto.sEntre(g) : Double.NaN;
            if (!Double.isNaN(sRep)) {
                reps.add(sRep / m5);
            }
            if (ref == null) {
                t.append(String.format(Locale.US, "  %s: media %.1f, s_rep %.1f (%.2f %%) entre %d colocaciones; "
                        + "sin serie de campaña con la que comparar\n", nombre, m5, sRep, 100 * sRep / m5, g.size()));
                continue;
            }
            medidos++;
            double r = ref.media();
            double margen = Math.max(Double.isNaN(sRep) ? 0 : 2 * sRep, MARGEN_REL * r);
            double d = m5 - r;
            boolean dentro = Math.abs(d) <= margen;
            if (!dentro) {
                fuera++;
                if (d > 0) {
                    arriba++;
                } else {
                    abajo++;
                }
            }
            t.append(String.format(Locale.US, "  %s: media %.1f (%d colocaciones), s_rep %.1f (%.2f %%); campaña %.1f (%s); "
                            + "diferencia %+.1f (%+.2f %%), margen máx(2·s_rep ; 4 %%) = %.1f -> %s\n",
                    nombre, m5, g.size(), sRep, 100 * sRep / m5, r, ref.id, d, 100 * d / r, margen,
                    dentro ? "dentro" : "FUERA"));
        }
        double sRepRel = reps.isEmpty() ? Double.NaN : Estadistica.media(Estadistica.aVector(reps));
        String ver;
        if (medidos < PATRONES.length) {
            ver = "PENDIENTE";
            t.append("Veredicto: pendiente (faltan patrones o su serie de campaña).\n");
        } else if (arriba == PATRONES.length || abajo == PATRONES.length) {
            ver = "REVERTIR";
            t.append("Veredicto: los tres desplazados en el mismo sentido más allá del margen: criterio de "
                    + "REVERSIÓN A LA 3.6.1 (P9-A5).\n");
        } else if (fuera > 0) {
            ver = "NO_CONCLUYENTE";
            t.append("Veredicto: puente no conforme en ").append(fuera)
                    .append(" patrón(es), sin desplazamiento común: no revierte; revisar colocación y repetir.\n");
        } else {
            ver = "OK";
            t.append("Veredicto: puente conforme; la 3.6.2 mide como la campaña.\n");
        }
        if (!Double.isNaN(sRepRel)) {
            t.append(String.format(Locale.US, "s_rep media: %.2f %% (para los criterios P9-B5).\n", 100 * sRepRel));
        }
        t.append("Protocolo: 5 colocaciones × 9 disparos, más el asentamiento por colocación.\n");
        return new Resultado(t.toString(), ver, sRepRel);
    }
}
