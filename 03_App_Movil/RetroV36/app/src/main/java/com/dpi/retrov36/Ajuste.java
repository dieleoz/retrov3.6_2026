package com.dpi.retrov36;

/**
 * Ajuste por minimos cuadrados de R certificado frente a x medida, grado 1 o 2
 * (PROCEDIMIENTO-Calibracion-V3-K42.md §5). Java puro.
 *
 * Para no perder precision con x de hasta 4300 (x^2 ~ 1,8e7), se ajusta sobre
 * u = (x - media) / escala y despues se vuelve a los coeficientes en x.
 */
public final class Ajuste {

    private Ajuste() { }

    public static final class Resultado {
        public final int grado;
        public final Ecuacion ecuacion;
        /** R certificado - R calculado, por punto. */
        public final double[] residuos;
        public final double errorMaximo;
        public final double rms;

        Resultado(int grado, Ecuacion ecuacion, double[] residuos) {
            this.grado = grado;
            this.ecuacion = ecuacion;
            this.residuos = residuos;
            double max = 0;
            double s = 0;
            for (double r : residuos) {
                max = Math.max(max, Math.abs(r));
                s += r * r;
            }
            this.errorMaximo = max;
            this.rms = residuos.length == 0 ? Double.NaN : Math.sqrt(s / residuos.length);
        }
    }

    /**
     * @throws IllegalArgumentException si el grado no es 1 o 2, si hay menos
     *         de grado+2 x distintas (RF-APP-15) o si los vectores no casan.
     */
    public static Resultado ajustar(double[] x, double[] r, int grado) {
        if (grado != 1 && grado != 2) {
            throw new IllegalArgumentException("grado " + grado + ": solo se admite 1 o 2");
        }
        if (x.length != r.length) {
            throw new IllegalArgumentException("x y R no tienen el mismo numero de puntos");
        }
        // RF-APP-15: al menos grado + 2 niveles distintos (un grado de libertad).
        if (distintas(x) < grado + 2) {
            throw new IllegalArgumentException("hacen falta al menos " + (grado + 2)
                    + " valores de x distintos para grado " + grado);
        }
        int n = x.length;
        double mx = Estadistica.media(x);
        double esc = 0;
        for (double v : x) {
            esc = Math.max(esc, Math.abs(v - mx));
        }
        if (esc == 0) {
            esc = 1;
        }
        int m = grado + 1;
        double[][] a = new double[m][m + 1];
        for (int i = 0; i < n; i++) {
            double u = (x[i] - mx) / esc;
            double[] pot = new double[m];
            pot[0] = 1;
            for (int j = 1; j < m; j++) {
                pot[j] = pot[j - 1] * u;
            }
            for (int f = 0; f < m; f++) {
                for (int c = 0; c < m; c++) {
                    a[f][c] += pot[f] * pot[c];
                }
                a[f][m] += pot[f] * r[i];
            }
        }
        double[] b = resolver(a); // b[j] coeficiente de u^j
        // Vuelta a x: u = (x - mx)/esc
        double b0 = b[0];
        double b1 = b[1];
        double b2 = grado == 2 ? b[2] : 0;
        double c2 = b2 / (esc * esc);
        double c1 = b1 / esc - 2 * b2 * mx / (esc * esc);
        double c0 = b0 - b1 * mx / esc + b2 * mx * mx / (esc * esc);
        Ecuacion e = new Ecuacion(0, c2, c1, c0);
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            res[i] = r[i] - e.evaluar(x[i]);
        }
        return new Resultado(grado, e, res);
    }

    private static int distintas(double[] x) {
        java.util.HashSet<Double> s = new java.util.HashSet<>();
        for (double v : x) {
            s.add(v);
        }
        return s.size();
    }

    /** Gauss con pivote parcial sobre la matriz ampliada. */
    private static double[] resolver(double[][] a) {
        int m = a.length;
        for (int col = 0; col < m; col++) {
            int piv = col;
            for (int f = col + 1; f < m; f++) {
                if (Math.abs(a[f][col]) > Math.abs(a[piv][col])) {
                    piv = f;
                }
            }
            if (Math.abs(a[piv][col]) < 1e-12) {
                throw new IllegalArgumentException("sistema singular: puntos insuficientes o repetidos");
            }
            double[] t = a[col];
            a[col] = a[piv];
            a[piv] = t;
            for (int f = col + 1; f < m; f++) {
                double k = a[f][col] / a[col][col];
                for (int c = col; c <= m; c++) {
                    a[f][c] -= k * a[col][c];
                }
            }
        }
        double[] s = new double[m];
        for (int f = m - 1; f >= 0; f--) {
            double v = a[f][m];
            for (int c = f + 1; c < m; c++) {
                v -= a[f][c] * s[c];
            }
            s[f] = v / a[f][f];
        }
        return s;
    }
}
