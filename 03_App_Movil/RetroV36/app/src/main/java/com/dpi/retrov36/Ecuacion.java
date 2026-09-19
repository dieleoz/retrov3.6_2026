package com.dpi.retrov36;

import java.util.Locale;

/**
 * Ecuacion de calibracion de un codigo: R = c3*x^3 + c2*x^2 + c1*x + c0
 * (PROTOCOLO-V3.6.md §2). Clase Java pura: se prueba en la JVM.
 */
public final class Ecuacion {

    /** Valor por encima del cual arreglar_dato() pone 0 (ecuacionesCalibracion.c:49-54). */
    public static final int TECHO_FIRMWARE = 4000;

    public final double c3;
    public final double c2;
    public final double c1;
    public final double c0;

    public Ecuacion(double c3, double c2, double c1, double c0) {
        this.c3 = c3;
        this.c2 = c2;
        this.c1 = c1;
        this.c0 = c0;
    }

    public double evaluar(double x) {
        return ((c3 * x + c2) * x + c1) * x + c0;
    }

    /** Derivada dR/dx en x. */
    public double derivada(double x) {
        return (3 * c3 * x + 2 * c2) * x + c1;
    }

    /**
     * Lo que previsiblemente responde el firmware para una x entera:
     * el double se convierte a unsigned int (trunca), y arreglar_dato() pone 0
     * si pasa de 4000. Un negativo es comportamiento indefinido en C; se supone
     * 0, como anota PROCEDIMIENTO-Calibracion-V3-K42.md §1.6 (sin comprobar).
     */
    public int respuestaFirmware(int x) {
        double v = evaluar(x);
        if (!(v >= 0)) {
            return 0;
        }
        long r = (long) v;
        return r > TECHO_FIRMWARE ? 0 : (int) r;
    }

    /**
     * Emulacion de la revision 1.1 (§4 bis): float de 32 bits, en el orden de
     * 2020 (c3*x*x*x + c2*x*x + c1*x + c0, no Horner), truncado a entero y
     * corte en 4000. Supone que la libreria de XC8 redondea como IEEE-754
     * (riesgo R-05 de la SPEC: sin comprobar).
     */
    public int respuestaFloat32(int x) {
        float xf = (float) x;
        float a = (float) c3;
        float b = (float) c2;
        float c = (float) c1;
        float d = (float) c0;
        float v = a * xf * xf * xf + b * xf * xf + c * xf + d;
        if (!(v >= 0)) {
            return 0;
        }
        long r = (long) v;
        return r > TECHO_FIRMWARE ? 0 : (int) r;
    }

    /** Igualdad coeficiente a coeficiente como float32, con 1 ulp de margen (O-02). */
    public boolean igualFloat32(Ecuacion o) {
        double[] a = comoVector();
        double[] b = o.comoVector();
        for (int i = 0; i < 4; i++) {
            float fa = (float) a[i];
            float fb = (float) b[i];
            if (Math.abs(fa - fb) > Math.ulp(fa)) {
                return false;
            }
        }
        return true;
    }

    public double[] comoVector() {
        return new double[]{c3, c2, c1, c0};
    }

    /** Diferencia relativa maxima entre coeficientes (0 si son iguales). */
    public double diferenciaRelativa(Ecuacion o) {
        double[] a = comoVector();
        double[] b = o.comoVector();
        double max = 0;
        for (int i = 0; i < 4; i++) {
            double escala = Math.max(Math.abs(a[i]), Math.abs(b[i]));
            if (escala == 0) {
                continue;
            }
            max = Math.max(max, Math.abs(a[i] - b[i]) / escala);
        }
        return max;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "c3=%.7E c2=%.7E c1=%.7E c0=%.7E", c3, c2, c1, c0);
    }
}
