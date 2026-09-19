package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Asistente de calibracion, parte de calculo (Java puro).
 *
 * Que curva se puede ajustar lo decide el catalogo de patrones, no una lista
 * fija (3.6.4, con los patrones TIPO I P32a-P50 del 19-sep-2026):
 * - cada codigo usa los patrones de su color y de su clase: intensas (1-6,
 *   "OTROS PAPELES") con tipos II-XI; opacas (7, 8, a-d, "PAPEL TIPO I") con
 *   tipo I (ecuacionesCalibracion.c:103-128 y :167-187);
 * - grado maximo = niveles certificados distintos - 2 (RF-APP-15), hasta 2;
 * - sin extrapolar a ciegas: si los valores certificados cubren un rango
 *   estrecho (menos de RANGO_MIN_ABS unidades de R o menos de RANGO_MIN_REL
 *   del mayor), el codigo NO se ajusta: solo se verifica. La validacion de
 *   forma C2 (validarForma) comprueba la curva en todo x = 200-4400, pero solo
 *   su forma (creciente, sin negativos, sin pasar de 4000); no puede saber si
 *   los valores fuera del rango medido son buenos. Por eso ademas el informe
 *   muestra R actual y R nueva en x = 500..4000 marcando que puntos son
 *   extrapolacion.
 */
public final class Asistente {

    private Asistente() { }

    /** Rango minimo de valores certificados para ajustar, en unidades de R. */
    public static final double RANGO_MIN_ABS = 20;
    /** Rango minimo relativo: (max - min) / max. */
    public static final double RANGO_MIN_REL = 0.30;

    /** true si el patron sirve para el codigo k: mismo color y misma clase (tipo I = opacas). */
    public static boolean deCodigo(Patron p, char k) {
        if (!p.color.equalsIgnoreCase(Fabrica.color(k))) {
            return false;
        }
        boolean tipoI = "I".equalsIgnoreCase(p.tipo.trim());
        return Fabrica.esIntensa(k) != tipoI;
    }

    public static final class Cobertura {
        public final char codigo;
        public final int patrones;
        public final int niveles;
        public final double rMin;
        public final double rMax;
        /** 0 si no se puede ajustar. */
        public final int gradoMaximo;
        public final String motivo;

        Cobertura(char codigo, int patrones, int niveles, double rMin, double rMax, int gradoMaximo, String motivo) {
            this.codigo = codigo;
            this.patrones = patrones;
            this.niveles = niveles;
            this.rMin = rMin;
            this.rMax = rMax;
            this.gradoMaximo = gradoMaximo;
            this.motivo = motivo;
        }

        public String texto() {
            String base = codigo + " " + Fabrica.nombre(codigo) + ": " + patrones + " patrones"
                    + (patrones > 0 ? String.format(Locale.US, ", %d niveles, R %.0f-%.0f", niveles, rMin, rMax) : "");
            return base + (gradoMaximo > 0 ? " -> ajustable hasta grado " + gradoMaximo : " -> NO ajustable: " + motivo);
        }
    }

    /** Cobertura de un codigo con un conjunto de valores certificados. */
    static Cobertura coberturaValores(char k, List<Double> valores) {
        Set<Double> distintos = new LinkedHashSet<>(valores);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : valores) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        int n = valores.size();
        int niv = distintos.size();
        if (n == 0) {
            return new Cobertura(k, 0, 0, Double.NaN, Double.NaN, 0, "no hay patrones de este color y clase");
        }
        if (niv < 3) {
            return new Cobertura(k, n, niv, min, max, 0, niv + (niv == 1 ? " nivel" : " niveles")
                    + " certificados: hacen falta 3 para grado 1; sólo verificar");
        }
        double rango = max - min;
        if (rango < RANGO_MIN_ABS || rango / max < RANGO_MIN_REL) {
            return new Cobertura(k, n, niv, min, max, 0, String.format(Locale.US,
                    "rango certificado estrecho (%.0f-%.0f): ajustar sería extrapolar a ciegas; sólo verificar", min, max));
        }
        return new Cobertura(k, n, niv, min, max, Math.min(2, niv - 2), "");
    }

    /** Cobertura del codigo k con el catalogo de patrones. */
    public static Cobertura cobertura(char k, List<Patron> catalogo) {
        List<Double> v = new ArrayList<>();
        for (Patron p : catalogo) {
            if (deCodigo(p, k)) {
                v.add(p.valor);
            }
        }
        return coberturaValores(k, v);
    }

    public static String cobertura(List<Patron> catalogo) {
        StringBuilder sb = new StringBuilder("Cobertura del catálogo de patrones por código:\n");
        for (char k : Fabrica.CODIGOS) {
            sb.append(cobertura(k, catalogo).texto()).append('\n');
        }
        sb.append("Es un ajuste contra patrones, no una calibración trazable: faltan la geometría y el certificado de origen.");
        return sb.toString();
    }

    /** Punto del ajuste: un patron con la media de sus lecturas. */
    public static final class Punto {
        public final Patron patron;
        public final int n;
        public final double x;
        public final double sd;

        Punto(Patron patron, int n, double x, double sd) {
            this.patron = patron;
            this.n = n;
            this.x = x;
            this.sd = sd;
        }
    }

    /** Medias por patron de las medidas validas del color del codigo k. */
    public static List<Punto> puntos(List<Medida> medidas, char k) {
        Map<String, List<Double>> xs = new LinkedHashMap<>();
        Map<String, Patron> pat = new LinkedHashMap<>();
        for (Medida m : medidas) {
            if (!m.valida() || !deCodigo(m.patron, k)) {
                continue;
            }
            List<Double> l = xs.get(m.patron.nombre);
            if (l == null) {
                l = new ArrayList<>();
                xs.put(m.patron.nombre, l);
                pat.put(m.patron.nombre, m.patron);
            }
            l.add(m.x);
        }
        List<Punto> out = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : xs.entrySet()) {
            double[] v = Estadistica.aVector(e.getValue());
            out.add(new Punto(pat.get(e.getKey()), v.length, Estadistica.media(v),
                    v.length > 1 ? Estadistica.desviacion(v) : Double.NaN));
        }
        return out;
    }

    /** Rango de x en que se valida la forma de una curva (condicion C2). */
    public static final int X_USO_MIN = 200;
    public static final int X_USO_MAX = 4400;

    public static final class Forma {
        public final List<String> bloqueos = new ArrayList<>();
        public final List<String> avisos = new ArrayList<>();
    }

    /**
     * Condicion C2 de la revision de arquitectura:
     * - creciente en todo X_USO_MIN..X_USO_MAX (sin techo): bloqueante;
     * - no pasa de 4000 en ese rango (el firmware lo pondria a 0): bloqueante;
     * - no negativa desde la x minima medida hasta X_USO_MAX: bloqueante.
     *   Por debajo de la x minima medida, un negativo solo se avisa: TODAS las
     *   curvas de fabrica son negativas hacia x = 200 (f1(200) = -182), porque
     *   x = ADC + 200 y en oscuro el equipo responde 0 por diseno.
     *   Contradiccion abierta con la letra de C2; la decide el propietario.
     */
    public static Forma validarForma(Ecuacion e, double xMinMedida) {
        Forma f = new Forma();
        int cero = -1;
        boolean creciente = true;
        boolean techo = false;
        boolean negativa = false;
        for (int x = X_USO_MIN; x <= X_USO_MAX; x++) {
            double v = e.evaluar(x);
            if (creciente && e.derivada(x) <= 0) {
                creciente = false;
                f.bloqueos.add("la curva no es creciente en x = " + x + " (techo dentro de " + X_USO_MIN
                        + "-" + X_USO_MAX + ")");
            }
            if (!techo && v > Ecuacion.TECHO_FIRMWARE) {
                techo = true;
                f.bloqueos.add("la curva pasa de 4000 en x = " + x + " (el firmware lo pondría a 0)");
            }
            if (v < 0) {
                cero = x;
                if (!negativa && x >= xMinMedida) {
                    negativa = true;
                    f.bloqueos.add("la curva da negativo en x = " + x + ", dentro del rango medido o por encima");
                }
            }
        }
        if (cero >= 0 && !negativa) {
            f.avisos.add("la curva es negativa hasta x = " + cero + " (por debajo de los patrones): ahí el equipo responderá 0");
        }
        return f;
    }

    /**
     * P9-B13 (REVISION-Arquitectura-P9-V3.6.md §4 bis): la curva nueva evaluada en la x de
     * OSCURO no puede dar bastante mas que la de fabrica en ese punto. Caso de referencia:
     * blanco grado 2 de la campana del 19-sep, R(575) ~ 219 frente a 25 de fabrica: una
     * lamina degradada, o nada, leeria ~200. El arquitecto no fija cifra (lo acepta Diego
     * por escrito); umbral del coordinador, configurable: R_oscuro <= max(fabrica + 10 ; 25).
     * x de oscuro por defecto 575 (medida con la V3.6 en SLV-002, ACTA-antes-y-despues:40).
     */
    public static volatile double X_OSCURO = 575;
    public static volatile double OSCURO_MARGEN = 10;
    public static volatile double OSCURO_MINIMO = 25;
    /**
     * P9-B5 / §4 ter: s de reproducibilidad entre colocaciones, relativa, para los criterios
     * que dependen de dispersion. 3 % hasta que P9-A5 la mida (2-4 % segun el coordinador).
     */
    public static volatile double S_REP_REL = 0.03;

    /** Motivo de bloqueo por oscuro, o null si la curva cumple. */
    public static String comprobarOscuro(Ecuacion nueva, char k, double xOscuro, double margen, double minimo) {
        double rN = nueva.evaluar(xOscuro);
        double rF = Fabrica.ecuacion(k).evaluar(xOscuro);
        double lim = Math.max(rF + margen, minimo);
        if (rN > lim) {
            return String.format(Locale.US, "en oscuro (x = %.0f) la curva nueva da R = %.0f frente a %.0f de fábrica; "
                    + "límite máx(fábrica + %.0f ; %.0f) = %.0f. Una lámina degradada, o nada, leería %.0f (P9-B13)",
                    xOscuro, rN, rF, margen, minimo, lim, rN);
        }
        return null;
    }

    /** Limites de #S del firmware V3.6.1 (calibracion_v36.c:588, CAMBIOS-V3.6.md §7). */
    public static final int S_X_MIN = 600;
    public static final int S_X_MAX = 4300;

    /**
     * El mismo criterio que aplica #S en el firmware V3.6.1: R(x) finito y en
     * [0; 4000] para todo x de 600 a 4300 (600: limite fijado por el
     * coordinador; 4300: ADC a fondo + 200; 4000: arreglar_dato). Se evalua en
     * float32 en el orden de 2020 en cada x entera, lo que cubre los bordes y
     * los extremos interiores que mira el firmware.
     * @return null si cumple; si no, que falla y en que x.
     */
    public static String criterioFirmwareS(Ecuacion e) {
        float a = (float) e.c3;
        float b = (float) e.c2;
        float c = (float) e.c1;
        float d = (float) e.c0;
        for (int x = S_X_MIN; x <= S_X_MAX; x++) {
            float xf = x;
            float v = a * xf * xf * xf + b * xf * xf + c * xf + d;
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                return "la curva no es finita en x = " + x;
            }
            if (v < 0) {
                return "la curva se hace negativa en x = " + x + " (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)";
            }
            if (v > Ecuacion.TECHO_FIRMWARE) {
                return "la curva pasa de 4000 en x = " + x + " (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)";
            }
        }
        return null;
    }

    /**
     * RF-CAL-15/16 (informativo, lo decide Diego): sesgo y RMS relativos por tipo de lamina,
     * curva nueva frente a fabrica. "No empeorar" con margen de reproducibilidad (P9-B5):
     * empeora solo si RMS_nueva > RMS_fabrica + S_REP_REL.
     */
    public static String residuoPorTipo(List<Punto> puntos, Ecuacion nueva, Ecuacion fabrica) {
        Map<String, List<Punto>> porTipo = new LinkedHashMap<>();
        for (Punto q : puntos) {
            List<Punto> l = porTipo.get(q.patron.tipo);
            if (l == null) {
                l = new ArrayList<>();
                porTipo.put(q.patron.tipo, l);
            }
            l.add(q);
        }
        StringBuilder sb = new StringBuilder("Residuo por tipo (nueva / fábrica), relativo al certificado:\n");
        for (Map.Entry<String, List<Punto>> e : porTipo.entrySet()) {
            double[] rn = rel(e.getValue(), nueva);
            double[] rf = rel(e.getValue(), fabrica);
            boolean empeora = rn[1] > rf[1] + 100 * S_REP_REL;
            sb.append(String.format(Locale.US, "  %-4s n=%d  sesgo %+.1f %% / %+.1f %%  RMS %.1f %% / %.1f %%%s\n",
                    e.getKey(), e.getValue().size(), rn[0], rf[0], rn[1], rf[1],
                    empeora ? "  EMPEORA más allá de la reproducibilidad" : ""));
        }
        return sb.toString();
    }

    /** {sesgo %, RMS %} de (R_curva - cert) / cert. */
    static double[] rel(List<Punto> l, Ecuacion e) {
        double s = 0;
        double s2 = 0;
        for (Punto q : l) {
            double r = 100 * (e.evaluar(q.x) - q.patron.valor) / q.patron.valor;
            s += r;
            s2 += r * r;
        }
        return new double[]{s / l.size(), Math.sqrt(s2 / l.size())};
    }

    public static final class Propuesta {
        public final char codigo;
        public final List<Punto> puntos;
        public final Ajuste.Resultado ajuste;
        /** Motivos que impiden escribir; vacio si se puede. */
        public final List<String> bloqueos;
        public final List<String> avisos;
        public final String informe;

        Propuesta(char codigo, List<Punto> puntos, Ajuste.Resultado ajuste, List<String> bloqueos,
                  List<String> avisos, String informe) {
            this.codigo = codigo;
            this.puntos = puntos;
            this.ajuste = ajuste;
            this.bloqueos = bloqueos;
            this.avisos = avisos;
            this.informe = informe;
        }

        public boolean escribible() {
            return ajuste != null && bloqueos.isEmpty();
        }
    }

    /**
     * Ajusta el codigo k con las medidas de la sesion.
     * @param vigente ecuacion que tiene hoy el equipo (para "como llego").
     */
    /** Sin catalogo: la cobertura se juzga con los patrones medidos. */
    public static Propuesta proponer(char k, int grado, List<Medida> medidas, Ecuacion vigente) {
        return proponer(k, grado, medidas, vigente, null);
    }

    /**
     * @param catalogo todos los patrones disponibles (para el grado maximo del
     *                 codigo), o null para juzgar solo con los medidos.
     */
    public static Propuesta proponer(char k, int grado, List<Medida> medidas, Ecuacion vigente,
                                     List<Patron> catalogo) {
        List<String> bloqueos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        List<Punto> p = puntos(medidas, k);
        StringBuilder inf = new StringBuilder();
        inf.append("Código ").append(k).append(" (").append(Fabrica.nombre(k)).append("), grado ").append(grado).append('\n');
        if (catalogo != null) {
            Cobertura cc = cobertura(k, catalogo);
            inf.append("Catálogo: ").append(cc.texto()).append('\n');
            if (cc.gradoMaximo < grado) {
                bloqueos.add(cc.gradoMaximo == 0 ? "no ajustable con el catálogo: " + cc.motivo
                        : "el catálogo sólo permite hasta grado " + cc.gradoMaximo);
            }
        }
        // Cobertura de lo MEDIDO: niveles certificados distintos y rango.
        List<Double> vals = new ArrayList<>();
        for (Punto q : p) {
            vals.add(q.patron.valor);
        }
        Cobertura cm = coberturaValores(k, vals);
        if (cm.gradoMaximo < grado) {
            bloqueos.add("con los patrones medidos (" + p.size() + "): "
                    + (cm.gradoMaximo == 0 ? cm.motivo : "sólo cabe hasta grado " + cm.gradoMaximo)
                    + "; hacen falta " + (grado + 2) + " niveles certificados distintos");
        }
        Set<String> tipos = new LinkedHashSet<>();
        for (Punto q : p) {
            tipos.add(q.patron.tipo);
        }
        if (tipos.size() > 1) {
            avisos.add("mezcla tipos de lámina " + tipos + " en una sola curva (P-06)");
        }
        Ajuste.Resultado a = null;
        double[] x = new double[p.size()];
        double[] r = new double[p.size()];
        if (p.size() >= grado + 1) {
            for (int i = 0; i < x.length; i++) {
                x[i] = p.get(i).x;
                r[i] = p.get(i).patron.valor;
            }
            try {
                a = Ajuste.ajustar(x, r, grado);
            } catch (IllegalArgumentException e) {
                bloqueos.add(e.getMessage());
            }
        }
        if (a != null) {
            Ecuacion e = a.ecuacion;
            inf.append(String.format(Locale.US, "Nuevos: c3=%s c2=%s c1=%s c0=%s\n",
                    Tramas.coeficiente(e.c3), Tramas.coeficiente(e.c2), Tramas.coeficiente(e.c1),
                    Tramas.coeficiente(e.c0)));
            inf.append("patrón  tipo  cert      x     n  R nueva  resid   resid%  R actual  desvío actual\n");
            double xmin = Double.MAX_VALUE;
            double xmax = -Double.MAX_VALUE;
            for (int i = 0; i < p.size(); i++) {
                Punto q = p.get(i);
                xmin = Math.min(xmin, q.x);
                xmax = Math.max(xmax, q.x);
                double rn = e.evaluar(q.x);
                double ra = vigente.evaluar(q.x);
                inf.append(String.format(Locale.US, "%-6s %-4s %5.0f %7.1f %3d %7.1f %+6.1f %+7.1f%% %8.1f %+8.1f\n",
                        q.patron.nombre, q.patron.tipo, q.patron.valor, q.x, q.n, rn, a.residuos[i],
                        100 * a.residuos[i] / q.patron.valor, ra, ra - q.patron.valor));
            }
            inf.append(String.format(Locale.US, "Error máximo %.1f, RMS %.2f (unidades de R)\n", a.errorMaximo, a.rms));
            // Forma de la curva en TODO el rango de uso (condicion C2): bloqueante.
            Forma f = validarForma(e, xmin);
            bloqueos.addAll(f.bloqueos);
            avisos.addAll(f.avisos);
            String fw = criterioFirmwareS(e);
            if (fw != null) {
                bloqueos.add(fw);
            }
            // P9-B13: oscuro y parte baja.
            String osc = comprobarOscuro(e, k, X_OSCURO, OSCURO_MARGEN, OSCURO_MINIMO);
            if (osc != null) {
                bloqueos.add(osc);
            }
            Ecuacion fab = Fabrica.ecuacion(k);
            inf.append(String.format(Locale.US, "Oscuro (x = %.0f): R nueva %.1f, fábrica %.1f. Patrón más bajo (x = %.0f): "
                    + "R nueva %.1f, fábrica %.1f. Por debajo del patrón más bajo la lectura NO está calibrada.\n",
                    X_OSCURO, e.evaluar(X_OSCURO), fab.evaluar(X_OSCURO), xmin, e.evaluar(xmin), fab.evaluar(xmin)));
            inf.append(residuoPorTipo(p, e, fab));
            if (grado == 2 && (!f.bloqueos.isEmpty() || fw != null) && x.length >= 3) {
                // Si la parabola no puede, se mira si una recta si.
                try {
                    Ajuste.Resultado a1 = Ajuste.ajustar(x, r, 1);
                    boolean ok1 = validarForma(a1.ecuacion, xmin).bloqueos.isEmpty()
                            && criterioFirmwareS(a1.ecuacion) == null;
                    avisos.add(ok1 ? String.format(Locale.US,
                            "la parábola no cumple; una recta (grado 1) sí, con error máximo %.1f: pruebe grado 1",
                            a1.errorMaximo) : "ni la parábola ni una recta cumplen los límites de la curva");
                } catch (IllegalArgumentException ex) {
                    avisos.add("la parábola no cumple y no hay puntos para probar una recta");
                }
            }
            // Extrapolacion: C2 solo mira la forma. Se ensena que valores salen
            // fuera del rango medido para que nadie los tome por medidos.
            inf.append(String.format(Locale.US, "Rango medido: x %.0f-%.0f. Fuera de él la curva es EXTRAPOLACIÓN:\n", xmin, xmax));
            inf.append("     x  R actual  R nueva\n");
            for (int xx : new int[]{500, 1000, 2000, 3000, 4000}) {
                boolean dentro = xx >= xmin && xx <= xmax;
                inf.append(String.format(Locale.US, "%6d %9.1f %8.1f %s\n", xx, vigente.evaluar(xx), e.evaluar(xx),
                        dentro ? "" : "extrapolado"));
            }
        }
        for (String s : avisos) {
            inf.append("Aviso: ").append(s).append('\n');
        }
        for (String s : bloqueos) {
            inf.append("NO SE PUEDE ESCRIBIR: ").append(s).append('\n');
        }
        return new Propuesta(k, p, a, bloqueos, avisos, inf.toString());
    }
}
