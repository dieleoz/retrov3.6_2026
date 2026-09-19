package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Veredicto de una serie al momento, antes de guardarla (campana, 3.6.5). Java puro.
 *
 * 1. Disparo descolgado (el operador levanta la pistola): a mas de
 *    max(K_MAD * 1,4826 * MAD ; UMBRAL_MIN) cuentas de la mediana de su serie.
 *    Origen (C-46, 19-sep-2026): el ruido medido es s ~ 5 cuentas y el
 *    descolgado real (P7: 3031 entre ~3230) esta a ~200. Con solo 5 MAD, un
 *    MAD pequeno dejaba el umbral en ~10-15 cuentas y marcaba disparos buenos
 *    (serie real de P2: 1999 ... 2022). El suelo de 50 cuentas es 10 s.
 *    Caso real: P7, 3031 entre valores de ~3230 (registro del 19-sep-2026).
 * 2. Serie ruidosa: s > SD_MAX cuentas (sin los descolgados) -> REPETIR.
 * 3. "Es este el patron?", frente a lo ya medido en la campana:
 *    a) orden de certificados dentro del mismo color y tipo, con tolerancia
 *       relativa (3 % por defecto): mas certificado no puede dar claramente
 *       menos x;
 *    b) parecido: si la media esta a +/-1 % de la de otro patron ya medido del
 *       mismo color y clase (tipo I u otros) con certificado distinto, y no
 *       esta mas cerca de la x esperada (recta x-certificado del mismo color y
 *       tipo). Caso de referencia: una serie de 2804 elegida como P24 con P20
 *       ya medido en 2798.
 *    Los XI pueden desordenarse por orientacion: se dice en el mensaje, pero
 *    el aviso no se suprime.
 */
public final class Veredicto {

    public static final double K_MAD = 5;
    public static final double SD_MAX = 15;
    public static final double TOL_ORDEN = 0.03;
    public static final double TOL_PARECIDO = 0.01;
    /** Suelo del umbral de descolgado, en cuentas (C-46): 10 veces el ruido medido (s ~ 5). */
    public static final double UMBRAL_MIN = 50;
    /** Con menos disparos no se buscan descolgados. */
    public static final int N_MIN_DESCOLGADOS = 4;

    private Veredicto() { }

    public static final class Resultado {
        public final boolean[] descartar;
        public final String[] motivos;
        public final double media;
        public final double s;
        public final int n;
        public final boolean ruidosa;
        public final List<String> dudas;
        /** OK, REPETIR o DUDOSO. */
        public final String veredicto;
        public final String texto;

        Resultado(boolean[] descartar, String[] motivos, double media, double s, int n, boolean ruidosa,
                  List<String> dudas, String veredicto, String texto) {
            this.descartar = descartar;
            this.motivos = motivos;
            this.media = media;
            this.s = s;
            this.n = n;
            this.ruidosa = ruidosa;
            this.dudas = dudas;
            this.veredicto = veredicto;
            this.texto = texto;
        }
    }

    /** Indices de los disparos descolgados y su motivo (null si no lo es). */
    public static String[] descolgados(double[] x) {
        String[] m = new String[x.length];
        if (x.length < N_MIN_DESCOLGADOS) {
            return m;
        }
        double med = Estadistica.mediana(x);
        double[] dev = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            dev[i] = Math.abs(x[i] - med);
        }
        double mad = Estadistica.mediana(dev);
        double umbral = Math.max(K_MAD * 1.4826 * mad, UMBRAL_MIN);
        for (int i = 0; i < x.length; i++) {
            if (dev[i] > umbral) {
                m[i] = String.format(Locale.US, "descolgado: %.0f a %.0f cuentas de la mediana %.0f (límite %.0f = máx(5 s robusta; %.0f))",
                        x[i], dev[i], med, umbral, UMBRAL_MIN);
            }
        }
        return m;
    }

    /**
     * @param medidos medias de los patrones ya medidos en la campana (sin este).
     */
    public static Resultado evaluar(double[] x, Patron p, Map<Patron, Double> medidos, double tolOrden) {
        String[] mot = descolgados(x);
        boolean[] desc = new boolean[x.length];
        List<Double> ok = new ArrayList<>();
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            desc[i] = mot[i] != null;
            if (desc[i]) {
                t.append("Disparo ").append(i + 1).append(" ").append(mot[i]).append(": se descarta.\n");
            } else {
                ok.add(x[i]);
            }
        }
        double[] v = Estadistica.aVector(ok);
        double media = Estadistica.media(v);
        double s = Estadistica.desviacion(v);
        boolean ruidosa = v.length >= 2 && s > SD_MAX;
        t.append(String.format(Locale.US, "Media %.1f, s %.2f, n %d.\n", media, s, v.length));
        if (ruidosa) {
            t.append(String.format(Locale.US, "Serie ruidosa: s = %.1f > %.0f cuentas. Repetir.\n", s, SD_MAX));
        }
        List<String> dudas = new ArrayList<>();
        if (v.length > 0) {
            dudas.addAll(orden(p, media, medidos, tolOrden));
            dudas.addAll(parecido(p, media, medidos, tolOrden));
        }
        if (!dudas.isEmpty()) {
            t.append("¿Es este el patrón?\n");
            for (String d : dudas) {
                t.append("- ").append(d).append('\n');
            }
            if ("XI".equalsIgnoreCase(p.tipo.trim())) {
                t.append("(Es un XI: los XI pueden desordenarse por orientación. Compruébelo; el aviso se mantiene.)\n");
            }
        }
        String ver = ruidosa ? "REPETIR" : (dudas.isEmpty() ? "OK" : "DUDOSO");
        return new Resultado(desc, mot, media, s, v.length, ruidosa, dudas, ver, t.toString());
    }

    static List<String> orden(Patron p, double media, Map<Patron, Double> medidos, double tol) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Patron, Double> e : medidos.entrySet()) {
            Patron q = e.getKey();
            double mq = e.getValue();
            if (!q.color.equalsIgnoreCase(p.color) || !q.tipo.trim().equalsIgnoreCase(p.tipo.trim())
                    || q.valor == p.valor) {
                continue;
            }
            if (p.valor > q.valor && media < mq * (1 - tol)) {
                out.add(String.format(Locale.US, "orden: %s (cert %.0f) debería dar más x que %s (cert %.0f, x=%.0f) y da %.0f",
                        p.nombre, p.valor, q.nombre, q.valor, mq, media));
            } else if (p.valor < q.valor && media > mq * (1 + tol)) {
                out.add(String.format(Locale.US, "orden: %s (cert %.0f) debería dar menos x que %s (cert %.0f, x=%.0f) y da %.0f",
                        p.nombre, p.valor, q.nombre, q.valor, mq, media));
            }
        }
        return out;
    }

    /** x esperada para p por recta x-certificado de los medidos del mismo color y tipo; NaN si no hay base. */
    static double esperada(Patron p, Map<Patron, Double> medidos) {
        List<Double> c = new ArrayList<>();
        List<Double> xs = new ArrayList<>();
        for (Map.Entry<Patron, Double> e : medidos.entrySet()) {
            Patron q = e.getKey();
            if (q.color.equalsIgnoreCase(p.color) && q.tipo.trim().equalsIgnoreCase(p.tipo.trim())) {
                c.add(q.valor);
                xs.add(e.getValue());
            }
        }
        if (c.size() < 2 || new java.util.HashSet<>(c).size() < 2) {
            return Double.NaN;
        }
        double[] cv = Estadistica.aVector(c);
        double[] xv = Estadistica.aVector(xs);
        double mc = Estadistica.media(cv);
        double mx = Estadistica.media(xv);
        double num = 0;
        double den = 0;
        for (int i = 0; i < cv.length; i++) {
            num += (cv[i] - mc) * (xv[i] - mx);
            den += (cv[i] - mc) * (cv[i] - mc);
        }
        return mx + num / den * (p.valor - mc);
    }

    static List<String> parecido(Patron p, double media, Map<Patron, Double> medidos, double tolCert) {
        List<String> out = new ArrayList<>();
        double esp = esperada(p, medidos);
        for (Map.Entry<Patron, Double> e : medidos.entrySet()) {
            Patron q = e.getKey();
            double mq = e.getValue();
            if (!q.color.equalsIgnoreCase(p.color) || Campana.esTipoI(q) != Campana.esTipoI(p)) {
                continue;
            }
            if (Math.abs(q.valor - p.valor) <= tolCert * Math.max(q.valor, p.valor)) {
                continue; // certificados casi iguales: parecerse es lo esperable
            }
            if (Math.abs(media - mq) > TOL_PARECIDO * mq) {
                continue;
            }
            if (!Double.isNaN(esp) && Math.abs(media - esp) <= Math.abs(media - mq)) {
                continue;
            }
            out.add(String.format(Locale.US, "la media %.0f se parece (±1 %%) a la de %s (cert %.0f, x=%.0f)%s",
                    media, q.nombre, q.valor, mq, Double.isNaN(esp) ? ""
                            : String.format(Locale.US, " más que a la esperada para %s (x≈%.0f)", p.nombre, esp)));
        }
        return out;
    }
}
