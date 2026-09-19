package com.dpi.retrov36;

import java.util.Arrays;
import java.util.List;

/** Media, desviacion tipica muestral y mediana. Java puro. */
public final class Estadistica {

    private Estadistica() { }

    public static double media(double[] v) {
        if (v.length == 0) {
            return Double.NaN;
        }
        double s = 0;
        for (double d : v) {
            s += d;
        }
        return s / v.length;
    }

    /** Desviacion tipica muestral (n-1). NaN con menos de 2 valores. */
    public static double desviacion(double[] v) {
        if (v.length < 2) {
            return Double.NaN;
        }
        double m = media(v);
        double s = 0;
        for (double d : v) {
            s += (d - m) * (d - m);
        }
        return Math.sqrt(s / (v.length - 1));
    }

    public static double mediana(double[] v) {
        if (v.length == 0) {
            return Double.NaN;
        }
        double[] c = v.clone();
        Arrays.sort(c);
        int n = c.length;
        return (n % 2 == 1) ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2.0;
    }

    public static double[] aVector(List<Double> l) {
        double[] v = new double[l.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = l.get(i);
        }
        return v;
    }
}
