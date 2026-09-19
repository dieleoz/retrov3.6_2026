package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Prueba 4: las 12 respuestas de una misma superficie, invertidas con su
 * ecuacion, tienen que dar la misma x. Java puro.
 *
 * Revision del 19-sep-2026, tras la prueba real en SLV-002 con la app 3.6.2
 * (06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md, G4), que dio NO
 * APTO por el criterio, no por el equipo:
 *
 * 1. Referencia con deriva: en V3.6 se lee 'e' antes y despues de los 12
 *    codigos, y la x de referencia de cada codigo es la recta entre las dos
 *    segun su orden de envio (medido: e = 3004 -> 3021 -> 3023).
 * 2. Tolerancia por codigo: base + resolucion LOCAL en x_ref (1/|f'(x_ref)|
 *    cuentas por unidad de R) + deriva observada |e_fin - e_ini|.
 * 3. Codigos no invertibles en la zona (respuesta ambigua entre X_MIN y X_MAX,
 *    o pendiente <= 0 en x_ref: el blanco y el amarillo intensos cerca o por
 *    encima de su vertice) se EXCLUYEN y se informan como "no evaluable en esta
 *    zona", sin contar como fallo.
 * 4. Colocacion: si |e_fin - e_ini| > DERIVA_MAXIMA o las x invertidas forman
 *    dos grupos (primera pasada real: ocho codigos en x ~ 575 y cuatro en
 *    ~ 3000), la prueba es INVALIDA ("el equipo se movio o no estaba apoyado;
 *    repetir"), nunca un fallo del equipo.
 */
public final class Coherencia {

    /**
     * Tolerancia base en cuentas de x: ~2,5 s, con s = 4,0 cuentas medida con
     * 'e' sobre P1 en SLV-002 (19-sep-2026). Se suma a resolucion local y deriva.
     */
    public static final double TOLERANCIA_POR_DEFECTO = 10;
    /** Minimo de codigos evaluables para dar la prueba por valida. */
    public static final int MINIMO_EVALUABLES = 6;
    /** |e_fin - e_ini| por encima de esto: el equipo se movio. */
    public static final double DERIVA_MAXIMA = 50;
    /** Separacion entre grupos de x invertidas que indica dos superficies. */
    public static final double SEPARACION_GRUPOS = 300;

    public static final String MENSAJE_INVALIDA = "el equipo se movió o no estaba apoyado; repetir";

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

    public enum Estado { OK, FUERA, NO_EVALUABLE, NO_EVALUABLE_ZONA, SIN_RESPUESTA }

    public static final class Linea {
        public final char codigo;
        public final Integer respuesta;
        public final double x;
        public final double xRef;
        public final double tolerancia;
        public final double desvio;
        public final Estado estado;
        public final String nota;

        Linea(char codigo, Integer respuesta, double x, double xRef, double tolerancia, double desvio,
              Estado estado, String nota) {
            this.codigo = codigo;
            this.respuesta = respuesta;
            this.x = x;
            this.xRef = xRef;
            this.tolerancia = tolerancia;
            this.desvio = desvio;
            this.estado = estado;
            this.nota = nota;
        }

        public String texto() {
            switch (estado) {
                case SIN_RESPUESTA:
                    return codigo + ": sin respuesta";
                case NO_EVALUABLE:
                    return codigo + ": R=" + respuesta + " no evaluable (" + nota + ")";
                case NO_EVALUABLE_ZONA:
                    return codigo + ": R=" + respuesta + " no evaluable en esta zona (" + nota + "); excluido";
                default:
                    return String.format(Locale.US, "%c: R=%d -> x=%.1f, ref %.1f, desvio %+.1f, tol +/-%.1f %s",
                            codigo, respuesta, x, xRef, desvio, tolerancia, estado == Estado.OK ? "OK" : "FUERA");
            }
        }
    }

    public static final class Resultado {
        public final List<Linea> lineas;
        public final double xRef;
        public final boolean apto;
        /** true: la prueba no vale (colocacion); no es un fallo del equipo. */
        public final boolean invalida;
        public final String resumen;

        Resultado(List<Linea> lineas, double xRef, boolean apto, boolean invalida, String resumen) {
            this.lineas = lineas;
            this.xRef = xRef;
            this.apto = apto;
            this.invalida = invalida;
            this.resumen = resumen;
        }
    }

    /** Compatibilidad: una sola x directa (o ninguna). */
    public static Resultado evaluar(List<Entrada> entradas, Integer xDirecta, double tolBase) {
        return evaluar(entradas, xDirecta, xDirecta, tolBase);
    }

    /**
     * @param xIni 'e' antes de los 12 codigos, o null (V3 2020: sin 'e').
     * @param xFin 'e' despues de los 12 codigos, o null.
     */
    public static Resultado evaluar(List<Entrada> entradas, Integer xIni, Integer xFin, double tolBase) {
        int n = entradas.size();
        boolean hayIni = xIni != null && xIni > 0;
        boolean hayFin = xFin != null && xFin > 0;
        double deriva = (hayIni && hayFin) ? xFin - xIni : 0;

        // Inversion sin referencia (solo las no ambiguas), para grupos y mediana.
        List<Double> unicas = new ArrayList<>();
        for (Entrada e : entradas) {
            if (e.respuesta != null && e.respuesta > 0) {
                Inversion.Resultado r = Inversion.invertir(e.ecuacion, e.respuesta, Double.NaN);
                if (r.valido && r.intervalos == 1) {
                    unicas.add(r.x);
                }
            }
        }

        // Colocacion.
        String motivoInvalida = null;
        if (Math.abs(deriva) > DERIVA_MAXIMA) {
            motivoInvalida = String.format(Locale.US, "'e' paso de %d a %d (%+.0f cuentas, maximo %.0f)",
                    xIni, xFin, deriva, DERIVA_MAXIMA);
        } else {
            motivoInvalida = dosGrupos(Estadistica.aVector(unicas));
        }

        // Referencia por codigo.
        double[] ref = new double[n];
        String origen;
        if (hayIni || hayFin) {
            double a = hayIni ? xIni : xFin;
            double b = hayFin ? xFin : xIni;
            for (int i = 0; i < n; i++) {
                ref[i] = a + (b - a) * (i + 1) / (double) (n + 1);
            }
            origen = hayIni && hayFin ? String.format(Locale.US, "recta entre e=%d y e=%d", xIni, xFin)
                    : "x de 'e'";
        } else if (!unicas.isEmpty()) {
            Arrays.fill(ref, Estadistica.mediana(Estadistica.aVector(unicas)));
            origen = "mediana de " + unicas.size() + " codigos (sin 'e')";
        } else {
            Arrays.fill(ref, Double.NaN);
            origen = "sin referencia";
        }

        List<Linea> lineas = new ArrayList<>();
        int evaluables = 0;
        int fuera = 0;
        int sinResp = 0;
        int excluidos = 0;
        for (int i = 0; i < n; i++) {
            Entrada e = entradas.get(i);
            double xr = ref[i];
            if (e.respuesta == null) {
                sinResp++;
                lineas.add(new Linea(e.codigo, null, Double.NaN, xr, Double.NaN, Double.NaN, Estado.SIN_RESPUESTA, ""));
                continue;
            }
            Inversion.Resultado r = Inversion.invertir(e.ecuacion, e.respuesta, xr);
            if (!r.valido || Double.isNaN(xr)) {
                lineas.add(new Linea(e.codigo, e.respuesta, Double.NaN, xr, Double.NaN, Double.NaN,
                        Estado.NO_EVALUABLE, r.valido ? "sin referencia" : r.motivo));
                continue;
            }
            double pendiente = e.ecuacion.derivada(xr);
            if (r.intervalos > 1 || pendiente <= 0) {
                excluidos++;
                lineas.add(new Linea(e.codigo, e.respuesta, r.x, xr, Double.NaN, r.x - xr, Estado.NO_EVALUABLE_ZONA,
                        r.intervalos > 1 ? "la misma respuesta sale en " + r.intervalos + " zonas de x"
                                : "la curva no sube en esta x"));
                continue;
            }
            evaluables++;
            double resolLocal = 1.0 / pendiente;
            double tol = tolBase + resolLocal + Math.abs(deriva);
            double d = r.x - xr;
            boolean ok = Math.abs(d) <= tol;
            if (!ok) {
                fuera++;
            }
            lineas.add(new Linea(e.codigo, e.respuesta, r.x, xr, tol, d, ok ? Estado.OK : Estado.FUERA, ""));
        }

        boolean invalida = motivoInvalida != null;
        boolean apto = !invalida && sinResp == 0 && fuera == 0 && evaluables >= MINIMO_EVALUABLES;
        StringBuilder res = new StringBuilder();
        if (invalida) {
            res.append("INVALIDA: ").append(MENSAJE_INVALIDA).append(" (").append(motivoInvalida).append("). ");
        }
        res.append(String.format(Locale.US,
                "Referencia: %s; tolerancia = %.0f + resolucion local + deriva (%.0f); %d evaluables, %d fuera, "
                        + "%d excluidos por zona, %d sin respuesta",
                origen, tolBase, Math.abs(deriva), evaluables, fuera, excluidos, sinResp));
        if (!invalida && evaluables < MINIMO_EVALUABLES) {
            res.append("; faltan codigos evaluables (minimo ").append(MINIMO_EVALUABLES)
                    .append("): use un patron de nivel medio o alto");
        }
        return new Resultado(lineas, n > 0 ? Estadistica.media(ref) : Double.NaN, apto, invalida, res.toString());
    }

    /**
     * Motivo si las x forman dos grupos separados mas de SEPARACION_GRUPOS con
     * al menos 2 codigos en cada uno; null si no.
     */
    static String dosGrupos(double[] x) {
        if (x.length < 4) {
            return null;
        }
        double[] c = x.clone();
        Arrays.sort(c);
        int corte = -1;
        double mayor = 0;
        for (int i = 1; i < c.length; i++) {
            double h = c[i] - c[i - 1];
            if (h > mayor) {
                mayor = h;
                corte = i;
            }
        }
        if (mayor > SEPARACION_GRUPOS && corte >= 2 && c.length - corte >= 2) {
            return String.format(Locale.US, "las x invertidas forman dos grupos: %d codigos hacia %.0f y %d hacia %.0f",
                    corte, Estadistica.mediana(Arrays.copyOfRange(c, 0, corte)), c.length - corte,
                    Estadistica.mediana(Arrays.copyOfRange(c, corte, c.length)));
        }
        return null;
    }
}
