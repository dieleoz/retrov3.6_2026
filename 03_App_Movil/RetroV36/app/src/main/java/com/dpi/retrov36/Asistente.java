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
 * Que curvas se pueden ajustar con P1-P31 (SPEC-V3.6 RF-APP-14 y aviso del
 * coordinador del 18-sep-2026): SOLO blanco y amarillo intensos (codigos 1 y
 * 2). Verde, azul y rojo tienen un unico nivel (solo verificar); naranja no
 * tiene patrones; ninguno es de tipo I, asi que las opacas (7, 8, a-d) no se
 * ajustan con este juego.
 */
public final class Asistente {

    private Asistente() { }

    public static final char[] AJUSTABLES = {'1', '2'};

    public static boolean ajustable(char k) {
        return k == '1' || k == '2';
    }

    /** Por que un codigo no se puede ajustar con P1-P31 ("" si se puede). */
    public static String motivoNoAjustable(char k) {
        if (ajustable(k)) {
            return "";
        }
        if (!Fabrica.esIntensa(k)) {
            return "opaca (PAPEL TIPO I): ningún patrón P1-P31 es de tipo I";
        }
        switch (k) {
            case '3': return "verde: un único nivel (164-173), sólo verificar";
            case '4': return "rojo: dos niveles muy próximos (194-227), sólo verificar";
            case '5': return "azul: un único nivel (83-85), sólo verificar";
            case '6': return "naranja: no hay patrones";
            default: return "sin patrones";
        }
    }

    public static String cobertura() {
        StringBuilder sb = new StringBuilder("Con P1-P31 sólo se pueden ajustar las curvas 1 (blanco intenso) y 2 (amarillo intenso).\n");
        for (char k : Fabrica.CODIGOS) {
            if (!ajustable(k)) {
                sb.append(k).append(": ").append(motivoNoAjustable(k)).append('\n');
            }
        }
        sb.append("Es un ajuste contra patrones, no una calibración trazable: faltan la geometría y el certificado de origen de P1-P31.");
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
        String color = Fabrica.color(k);
        Map<String, List<Double>> xs = new LinkedHashMap<>();
        Map<String, Patron> pat = new LinkedHashMap<>();
        for (Medida m : medidas) {
            if (!m.valida() || !m.patron.color.equalsIgnoreCase(color)) {
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
    public static Propuesta proponer(char k, int grado, List<Medida> medidas, Ecuacion vigente) {
        List<String> bloqueos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        List<Punto> p = puntos(medidas, k);
        StringBuilder inf = new StringBuilder();
        inf.append("Código ").append(k).append(" (").append(Fabrica.nombre(k)).append("), grado ").append(grado).append('\n');
        if (!ajustable(k)) {
            bloqueos.add("no ajustable con P1-P31: " + motivoNoAjustable(k));
        }
        if (p.size() < grado + 2) {
            bloqueos.add("hacen falta al menos " + (grado + 2) + " patrones distintos de color "
                    + Fabrica.color(k) + " medidos; hay " + p.size());
        }
        Set<String> tipos = new LinkedHashSet<>();
        for (Punto q : p) {
            tipos.add(q.patron.tipo);
        }
        if (tipos.size() > 1) {
            avisos.add("mezcla tipos de lámina " + tipos + " en una sola curva (P-06)");
        }
        Ajuste.Resultado a = null;
        if (p.size() >= grado + 1) {
            double[] x = new double[p.size()];
            double[] r = new double[p.size()];
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
