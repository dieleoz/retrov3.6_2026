package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;

/**
 * Inversion exacta de una respuesta "::R" a la lectura interna x.
 *
 * El firmware trabaja con x entera (reflectivityValue es unsigned int) y trunca
 * el resultado de la ecuacion. Por eso no se resuelve la ecuacion: se recorren
 * todas las x enteras de 0 a X_MAX y se guardan las que dan exactamente R. El
 * resultado es un intervalo (o varios, si la ecuacion no es monotona) y su
 * centro es la estimacion de x; su semiancho es la resolucion del codigo.
 *
 * Una R de 0 no se invierte: significa negativo o saturado (§1.6 del
 * procedimiento) y no dice donde esta x.
 */
public final class Inversion {

    public static final int X_MAX = 4400;

    private Inversion() { }

    public static final class Resultado {
        /** false si R = 0 o si ninguna x entera da R. */
        public final boolean valido;
        public final double x;
        /** Semiancho del intervalo elegido, en cuentas de x. */
        public final double u;
        public final int desde;
        public final int hasta;
        /** Cuantos intervalos disjuntos dan la misma R (mas de 1: ambigua). */
        public final int intervalos;
        public final String motivo;

        Resultado(boolean valido, int desde, int hasta, int intervalos, String motivo) {
            this.valido = valido;
            this.desde = desde;
            this.hasta = hasta;
            this.x = valido ? (desde + hasta) / 2.0 : Double.NaN;
            this.u = valido ? (hasta - desde) / 2.0 : Double.NaN;
            this.intervalos = intervalos;
            this.motivo = motivo;
        }
    }

    /** Intervalos [desde, hasta] de x enteras cuya respuesta es r. */
    public static List<int[]> intervalos(Ecuacion e, int r) {
        List<int[]> out = new ArrayList<>();
        int inicio = -1;
        for (int x = 0; x <= X_MAX; x++) {
            boolean da = e.respuestaFirmware(x) == r;
            if (da && inicio < 0) {
                inicio = x;
            } else if (!da && inicio >= 0) {
                out.add(new int[]{inicio, x - 1});
                inicio = -1;
            }
        }
        if (inicio >= 0) {
            out.add(new int[]{inicio, X_MAX});
        }
        return out;
    }

    /**
     * @param referencia x aproximada para elegir entre intervalos si hay mas de
     *                   uno; NaN si no se tiene (se elige el de x mas baja).
     */
    public static Resultado invertir(Ecuacion e, int r, double referencia) {
        if (r <= 0) {
            return new Resultado(false, 0, 0, 0, "respuesta 0: negativo o saturado, no invertible");
        }
        List<int[]> iv = intervalos(e, r);
        if (iv.isEmpty()) {
            return new Resultado(false, 0, 0, 0, "ninguna x entre 0 y " + X_MAX + " da " + r);
        }
        int[] mejor = iv.get(0);
        if (!Double.isNaN(referencia)) {
            double dMin = Double.MAX_VALUE;
            for (int[] a : iv) {
                double d = Math.abs((a[0] + a[1]) / 2.0 - referencia);
                if (d < dMin) {
                    dMin = d;
                    mejor = a;
                }
            }
        }
        return new Resultado(true, mejor[0], mejor[1], iv.size(),
                iv.size() > 1 ? "ambigua: " + iv.size() + " intervalos dan la misma respuesta" : "");
    }
}
