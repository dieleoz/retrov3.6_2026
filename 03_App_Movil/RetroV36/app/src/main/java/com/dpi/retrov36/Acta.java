package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Acta de calibracion de una sesion de administrador (3.6.8). Java puro.
 *
 * Condiciones de REVISION-Arquitectura-P9-V3.6.md §8.2 que caen del lado de la app:
 * - P9-B3: el protocolo de disparos (K colocaciones x M disparos + asentamiento) queda
 *   fijo desde el primer #S hasta cerrar el acta, y sale escrito en ella.
 * - P9-B8: un codigo cada vez: no se escribe otro hasta que el anterior tenga su
 *   re-medida de verificacion (RF-CAL-18) conforme.
 * - P9-B9: la curva que certifica el acta es la leida con #G tras #S, y la prediccion
 *   de la re-medida se calcula con ella en float32 (respuestaFloat32).
 * - P9-B5: la tolerancia de la re-medida usa la s ENTRE colocaciones (reproducibilidad),
 *   no la s dentro de la serie: max(2 ; 2 * s_rep).
 * - #SC (fecha de calibracion) solo cuando el acta se acepta, una vez; si se rechaza,
 *   no se graba fecha.
 */
public final class Acta {

    public final String equipo;
    public final String mac;
    public final String firmware;
    public final int colocaciones;
    public final int disparos;
    public final int asentamiento;
    public final String abierta;
    private final List<Codigo> codigos = new ArrayList<>();
    /** null: pendiente; si no, ACEPTADA o RECHAZADA con fecha. */
    private String cierre;
    private String fechaGrabada;

    public Acta(String equipo, String mac, String firmware, int colocaciones, int disparos, int asentamiento,
                String abierta) {
        this.equipo = equipo;
        this.mac = mac;
        this.firmware = firmware;
        this.colocaciones = colocaciones;
        this.disparos = disparos;
        this.asentamiento = asentamiento;
        this.abierta = abierta;
    }

    public static final class Remedida {
        public final String patron;
        public final double rMedida;
        public final double rPredicha;
        public final double sRep;
        public final double tolerancia;
        public final boolean ok;
        public final String texto;

        Remedida(String patron, double rMedida, double rPredicha, double sRep, double tolerancia, boolean ok, String texto) {
            this.patron = patron;
            this.rMedida = rMedida;
            this.rPredicha = rPredicha;
            this.sRep = sRep;
            this.tolerancia = tolerancia;
            this.ok = ok;
            this.texto = texto;
        }
    }

    public static final class Codigo {
        public final char k;
        public final String tramaG;
        public final Ecuacion leida;
        public final String oscuro;
        public Remedida remedida;

        Codigo(char k, String tramaG, Ecuacion leida, String oscuro) {
            this.k = k;
            this.tramaG = tramaG;
            this.leida = leida;
            this.oscuro = oscuro;
        }
    }

    public List<Codigo> codigos() {
        return codigos;
    }

    public boolean cerrada() {
        return cierre != null;
    }

    public Codigo codigo(char k) {
        for (int i = codigos.size() - 1; i >= 0; i--) {
            if (codigos.get(i).k == k) {
                return codigos.get(i);
            }
        }
        return null;
    }

    /** P9-B8: null si se puede escribir el codigo k; si no, el motivo. */
    public String motivoNoEscribir(char k) {
        if (cerrada()) {
            return "el acta está cerrada (" + cierre + "): abra otra sesión de administrador";
        }
        for (Codigo c : codigos) {
            if (c.k != k && (c.remedida == null || !c.remedida.ok)) {
                return "falta la re-medida de verificación conforme del código " + c.k
                        + " (P9-B8: un código cada vez)";
            }
        }
        return null;
    }

    /** #S verificado con #E: queda anotado con la curva leida con #G (P9-B9). */
    public void escrito(char k, String tramaG, Ecuacion leida, String oscuro) {
        codigos.add(new Codigo(k, tramaG, leida, oscuro));
    }

    public void remedida(char k, Remedida r) {
        Codigo c = codigo(k);
        if (c == null) {
            throw new IllegalArgumentException("el código " + k + " no se ha escrito en esta acta");
        }
        c.remedida = r;
    }

    /**
     * RF-CAL-18 con P9-B5. rPorColocacion: respuestas del codigo k; xPorColocacion: las x
     * leidas con 'e' en los mismos disparos. La prediccion usa la curva leida con #G.
     * Con una sola colocacion no hay s_rep: se usa sRepRelPorDefecto * R.
     */
    public static Remedida evaluarRemedida(String patron, List<double[]> rPorColocacion, List<double[]> xPorColocacion,
                                           Ecuacion leida, double sRepRelPorDefecto) {
        List<double[]> pred = new ArrayList<>();
        for (double[] xs : xPorColocacion) {
            double[] p = new double[xs.length];
            for (int i = 0; i < xs.length; i++) {
                p[i] = leida.respuestaFloat32((int) Math.round(xs[i]));
            }
            pred.add(p);
        }
        double rMed = mediaDeMedias(rPorColocacion);
        double rPred = mediaDeMedias(pred);
        double sRep = rPorColocacion.size() >= 2 ? Veredicto.sEntre(rPorColocacion) : sRepRelPorDefecto * rMed;
        double tol = Math.max(2, 2 * sRep);
        boolean ok = Math.abs(rMed - rPred) <= tol;
        String t = String.format(Locale.US, "%s: R medida %.1f, R predicha %.1f (curva #G, float32), diferencia %+.1f; "
                        + "tolerancia máx(2 ; 2·s_rep) = %.1f con s_rep %.1f%s -> %s",
                patron, rMed, rPred, rMed - rPred, tol, sRep,
                rPorColocacion.size() >= 2 ? " (entre " + rPorColocacion.size() + " colocaciones)" : " (supuesta)",
                ok ? "CONFORME" : "NO CONFORME");
        return new Remedida(patron, rMed, rPred, sRep, tol, ok, t);
    }

    static double mediaDeMedias(List<double[]> g) {
        List<Double> m = new ArrayList<>();
        for (double[] v : g) {
            if (v.length > 0) {
                m.add(Estadistica.media(v));
            }
        }
        return Estadistica.media(Estadistica.aVector(m));
    }

    /** null si se puede aceptar (y grabar #SC); si no, el motivo. */
    public String motivoNoAceptable() {
        if (cerrada()) {
            return "el acta ya está cerrada (" + cierre + ")";
        }
        if (codigos.isEmpty()) {
            return "no se ha escrito ningún código";
        }
        for (Codigo c : codigos) {
            if (c.remedida == null) {
                return "falta la re-medida de verificación del código " + c.k;
            }
            if (!c.remedida.ok) {
                return "la re-medida del código " + c.k + " no es conforme";
            }
        }
        return null;
    }

    public void aceptar(String fecha, String fechaGrabadaEnEquipo) {
        if (motivoNoAceptable() != null) {
            throw new IllegalStateException(motivoNoAceptable());
        }
        cierre = "ACEPTADA " + fecha;
        fechaGrabada = fechaGrabadaEnEquipo;
    }

    public void rechazar(String fecha, String motivo) {
        cierre = "RECHAZADA " + fecha + (motivo == null || motivo.isEmpty() ? "" : ": " + motivo);
    }

    public String texto() {
        StringBuilder sb = new StringBuilder("ACTA DE CALIBRACIÓN\n");
        sb.append("Equipo: ").append(equipo).append(" (MAC ").append(mac).append(")\n");
        sb.append("Firmware: ").append(firmware).append('\n');
        sb.append("Abierta: ").append(abierta).append('\n');
        sb.append(String.format(Locale.US, "Protocolo de disparos (fijo, P9-B3): %d colocaciones × %d disparos, "
                + "%d de asentamiento por colocación\n", colocaciones, disparos, asentamiento));
        sb.append("Ajuste contra patrones, no calibración trazable. Por debajo del patrón más bajo de cada código "
                + "la lectura no está calibrada.\n\n");
        for (Codigo c : codigos) {
            sb.append("Código ").append(c.k).append(" (").append(Fabrica.nombre(c.k)).append(")\n");
            sb.append("  Curva certificada = leída con #G tras #S (P9-B9): ").append(c.tramaG).append('\n');
            sb.append("  #E en 5 puntos: coincide con la curva enviada (±1)\n");
            if (c.oscuro != null && !c.oscuro.isEmpty()) {
                sb.append("  ").append(c.oscuro).append('\n');
            }
            sb.append("  Re-medida (RF-CAL-18): ").append(c.remedida == null ? "PENDIENTE" : c.remedida.texto).append('\n');
        }
        sb.append('\n').append("Estado: ").append(cierre == null ? "PENDIENTE" : cierre).append('\n');
        if (cierre != null && cierre.startsWith("ACEPTADA")) {
            sb.append("Fecha de calibración en el equipo (#SC/#GC): ")
                    .append(fechaGrabada == null ? "no grabada (firmware sin #SC)" : fechaGrabada).append('\n');
        }
        return sb.toString();
    }
}
