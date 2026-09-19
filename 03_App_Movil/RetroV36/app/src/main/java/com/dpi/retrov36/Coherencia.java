package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prueba 4: las 12 respuestas de una misma superficie, invertidas con su
 * ecuacion, tienen que dar la misma x. Java puro.
 *
 * Criterio por codigo: |x_k - x_ref| <= tolerancia + u_k, donde u_k es la
 * resolucion del codigo en ese punto (semiancho del intervalo de x que da la
 * misma respuesta). Asi un codigo de poca resolucion, como 'b' (rojo opaco,
 * ~15 cuentas de x por unidad de R hacia x = 600), no se castiga por su
 * cuantizacion.
 *
 * x_ref: la x directa de 'e' si la hay; si no, la mediana de las x de los
 * codigos con inversion no ambigua.
 */
public final class Coherencia {

    public static final double TOLERANCIA_POR_DEFECTO = 15;
    /** Minimo de codigos evaluables (R > 0) para dar la prueba por valida. */
    public static final int MINIMO_EVALUABLES = 6;

    private Coherencia() { }

    public static final class Entrada {
        public final char codigo;
        /** null si no respondio. */
        public final Integer respuesta;
        public final Ecuacion ecuacion;

        public Entrada(char codigo, Integer respuesta, Ecuacion ecuacion) {
            this.codigo = codigo;
            this.respuesta = respuesta;
            this.ecuacion = ecuacion;
        }
    }

    public enum Estado { OK, FUERA, NO_EVALUABLE, SIN_RESPUESTA }

    public static final class Linea {
        public final char codigo;
        public final Integer respuesta;
        public final double x;
        public final double u;
        public final double desvio;
        public final Estado estado;
        public final String nota;

        Linea(char codigo, Integer respuesta, double x, double u, double desvio, Estado estado, String nota) {
            this.codigo = codigo;
            this.respuesta = respuesta;
            this.x = x;
            this.u = u;
            this.desvio = desvio;
            this.estado = estado;
            this.nota = nota;
        }

        public String texto() {
            if (estado == Estado.SIN_RESPUESTA) {
                return codigo + ": sin respuesta";
            }
            if (estado == Estado.NO_EVALUABLE) {
                return codigo + ": R=" + respuesta + " no evaluable (" + nota + ")";
            }
            return String.format(Locale.US, "%c: R=%d -> x=%.1f +/-%.1f, desvio %+.1f %s%s",
                    codigo, respuesta, x, u, desvio, estado == Estado.OK ? "OK" : "FUERA",
                    nota.isEmpty() ? "" : " (" + nota + ")");
        }
    }

    public static final class Resultado {
        public final List<Linea> lineas;
        public final double xRef;
        public final boolean apto;
        public final String resumen;

        Resultado(List<Linea> lineas, double xRef, boolean apto, String resumen) {
            this.lineas = lineas;
            this.xRef = xRef;
            this.apto = apto;
            this.resumen = resumen;
        }
    }

    /**
     * @param xDirecta x leida con 'e' sobre la misma superficie, o null.
     */
    public static Resultado evaluar(List<Entrada> entradas, Integer xDirecta, double tolerancia) {
        // 1) Inversion sin referencia, para calcular la mediana.
        List<Double> unicas = new ArrayList<>();
        for (Entrada e : entradas) {
            if (e.respuesta != null && e.respuesta > 0) {
                Inversion.Resultado r = Inversion.invertir(e.ecuacion, e.respuesta, Double.NaN);
                if (r.valido && r.intervalos == 1) {
                    unicas.add(r.x);
                }
            }
        }
        double xRef;
        String origen;
        if (xDirecta != null && xDirecta > 0) {
            xRef = xDirecta;
            origen = "x de 'e'";
        } else if (!unicas.isEmpty()) {
            xRef = Estadistica.mediana(Estadistica.aVector(unicas));
            origen = "mediana de " + unicas.size() + " codigos";
        } else {
            xRef = Double.NaN;
            origen = "sin referencia";
        }
        // 2) Evaluacion contra la referencia.
        List<Linea> lineas = new ArrayList<>();
        int evaluables = 0;
        int fuera = 0;
        int sinResp = 0;
        for (Entrada e : entradas) {
            if (e.respuesta == null) {
                sinResp++;
                lineas.add(new Linea(e.codigo, null, Double.NaN, Double.NaN, Double.NaN,
                        Estado.SIN_RESPUESTA, ""));
                continue;
            }
            Inversion.Resultado r = Inversion.invertir(e.ecuacion, e.respuesta, xRef);
            if (!r.valido || Double.isNaN(xRef)) {
                lineas.add(new Linea(e.codigo, e.respuesta, Double.NaN, Double.NaN, Double.NaN,
                        Estado.NO_EVALUABLE, r.valido ? "sin referencia" : r.motivo));
                continue;
            }
            evaluables++;
            double d = r.x - xRef;
            boolean ok = Math.abs(d) <= tolerancia + r.u;
            if (!ok) {
                fuera++;
            }
            lineas.add(new Linea(e.codigo, e.respuesta, r.x, r.u, d, ok ? Estado.OK : Estado.FUERA,
                    r.intervalos > 1 ? "ambigua, se toma el intervalo mas cercano" : ""));
        }
        boolean apto = sinResp == 0 && fuera == 0 && evaluables >= MINIMO_EVALUABLES;
        String resumen = String.format(Locale.US,
                "x_ref = %.1f (%s); tolerancia +/-%.0f + resolucion; %d evaluables, %d fuera, %d sin respuesta",
                xRef, origen, tolerancia, evaluables, fuera, sinResp);
        if (evaluables < MINIMO_EVALUABLES) {
            resumen += "; faltan codigos evaluables (minimo " + MINIMO_EVALUABLES
                    + "): use un patron de nivel medio o alto";
        }
        return new Resultado(lineas, xRef, apto, resumen);
    }
}
