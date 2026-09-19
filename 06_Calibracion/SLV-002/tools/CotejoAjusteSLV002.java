import com.dpi.retrov36.Ajuste;
import com.dpi.retrov36.Asistente;
import com.dpi.retrov36.Campana;
import com.dpi.retrov36.Ecuacion;
import com.dpi.retrov36.Fabrica;
import com.dpi.retrov36.Medida;
import com.dpi.retrov36.Patron;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Propuesta de ajuste de SLV-002 (19-sep-2026) calculada con el MISMO codigo de la app RetroV36:
 * Campana.leerDiario + medidasElegidas (serie elegida de cada patron), Asistente.puntos/proponer,
 * Ajuste.ajustar, Asistente.criterioFirmwareS, Asistente.validarForma, Ecuacion.respuestaFloat32
 * y Fabrica.ecuacion. No reimplementa el ajuste: lo llama y vuelca el resultado en TSV.
 *
 * Uso: java -cp clases CotejoAjusteSLV002 catalogo.csv diario.csv sigmaRelColocacion
 * Lo lanza tools/propuesta_ajuste_slv002.py, que compila la app y este fichero con JDK 11.
 */
public final class CotejoAjusteSLV002 {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final int N_MC = 2000;

    public static void main(String[] a) throws Exception {
        List<Patron> cat;
        try (Reader r = new InputStreamReader(new FileInputStream(a[0]), StandardCharsets.UTF_8)) {
            cat = Patron.leer(r);
        }
        Campana c = new Campana(cat, "SLV-002", MAC);
        int malas;
        try (Reader r = new InputStreamReader(new FileInputStream(a[1]), StandardCharsets.UTF_8)) {
            malas = c.leerDiario(r);
        }
        double sigmaRel = Double.parseDouble(a[2]);
        out("DIARIO", "series", c.series().size(), "lineas_no_entendidas", malas);
        List<Medida> med = c.medidasElegidas();
        out("MEDIDAS", med.size());
        for (Patron p : cat) {
            Campana.Serie s = c.elegida(p.nombre);
            if (s != null) {
                out("ELEGIDA", p.nombre, s.id, s.orientacion, s.validos().length, f(s.media()), f(s.desviacion()));
            }
        }
        for (Campana.Serie s : c.series()) {
            out("SERIE", s.id, s.patron, s.orientacion, s.aceptada ? 1 : 0, s.validos().length, f(s.media()),
                    f(s.desviacion()));
        }
        // Deriva dentro de la serie, con el metodo de la app (T-C38).
        for (String l : c.desvioPorPosicion().split("\n")) {
            if (l.contains("por posici")) {
                out("POSICION", l.trim());
            }
        }

        for (char k : Fabrica.CODIGOS) {
            Asistente.Cobertura cob = Asistente.cobertura(k, cat);
            out("COBERTURA", k, cob.gradoMaximo, cob.texto());
            Ecuacion fab = Fabrica.ecuacion(k);
            out("FABRICA_S", k, nz(Asistente.criterioFirmwareS(fab)));
            for (Asistente.Punto q : Asistente.puntos(med, k)) {
                out("PUNTO", k, q.patron.nombre, q.patron.tipo, f(q.patron.valor), f(q.x), q.n, f(q.sd),
                        f(fab.evaluar(q.x)), fab.respuestaFloat32((int) Math.round(q.x)), f(fab.derivada(q.x)));
            }
        }

        char[] cod = {'1', '1', '2', '2', '8', '8', 'b'};
        int[] gr = {1, 2, 1, 2, 1, 2, 1};
        for (int i = 0; i < cod.length; i++) {
            proponer(cod[i], gr[i], med, cat, sigmaRel, null, 0, "");
        }
        // Sensibilidad: codigo 2 con P5 a 0 grados (S023) en vez de la serie elegida (S024, 90 grados).
        double p5a0 = c.serie("S023").media();
        for (int g = 1; g <= 2; g++) {
            proponer('2', g, med, cat, sigmaRel, "P5", p5a0, "P5a0");
        }
        // Sensibilidad (no propuesta): codigo 2 grado 1 sin P24 (historial C-CAL-01). x = NaN lo quita.
        proponer('2', 1, med, cat, sigmaRel, "P24", Double.NaN, "sinP24");
    }

    private static void proponer(char k, int g, List<Medida> med, List<Patron> cat, double sigmaRel,
                                 String sustituir, double xSust, String var) {
        List<Asistente.Punto> p = new java.util.ArrayList<>(Asistente.puntos(med, k));
        if (Double.isNaN(xSust)) {
            p.removeIf(q -> q.patron.nombre.equals(sustituir));
        }
        double[] x = new double[p.size()];
        double[] r = new double[p.size()];
        String[] nom = new String[p.size()];
        String[] tipo = new String[p.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = p.get(i).x;
            r[i] = p.get(i).patron.valor;
            nom[i] = p.get(i).patron.nombre;
            tipo[i] = p.get(i).patron.tipo;
            if (nom[i].equals(sustituir)) {
                x[i] = xSust;
            }
        }
        String id = k + "|" + g + "|" + var;
        Ajuste.Resultado res = Ajuste.ajustar(x, r, g);
        Ecuacion e = res.ecuacion;
        out("AJUSTE", id, String.format(Locale.US, "%.8E", e.c3), String.format(Locale.US, "%.8E", e.c2),
                String.format(Locale.US, "%.8E", e.c1), String.format(Locale.US, "%.8E", e.c0),
                f(res.rms), f(res.errorMaximo));
        if (var.isEmpty()) {
            // El informe del asistente tal cual lo daria la app, con la curva de fabrica como vigente.
            Asistente.Propuesta pr = Asistente.proponer(k, g, med, Fabrica.ecuacion(k), cat);
            for (String l : pr.informe.split("\n")) {
                out("INFORME", id, l);
            }
            out("ESCRIBIBLE", id, pr.escribible() ? 1 : 0, String.join(" / ", pr.bloqueos));
        }
        double xmin = Double.MAX_VALUE;
        for (double v : x) {
            xmin = Math.min(xmin, v);
        }
        out("S", id, nz(Asistente.criterioFirmwareS(e)));
        Asistente.Forma fo = Asistente.validarForma(e, xmin);
        out("C2", id, fo.bloqueos.isEmpty() ? "OK" : String.join(" / ", fo.bloqueos), String.join(" / ", fo.avisos));
        for (int xx : new int[]{575, 600, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4300}) {
            out("EVAL", id, xx, f(e.evaluar(xx)), e.respuestaFloat32(xx), f(Fabrica.ecuacion(k).evaluar(xx)));
        }
        for (int i = 0; i < x.length; i++) {
            out("RES", id, nom[i], tipo[i], f(r[i]), f(x[i]), f(e.evaluar(x[i])), f(res.residuos[i]),
                    e.respuestaFloat32((int) Math.round(x[i])), f(e.derivada(x[i])));
        }
        // Validacion cruzada dejando uno fuera, con Ajuste.ajustar.
        for (int i = 0; i < x.length; i++) {
            double[] xx = new double[x.length - 1];
            double[] rr = new double[x.length - 1];
            for (int j = 0, m = 0; j < x.length; j++) {
                if (j != i) {
                    xx[m] = x[j];
                    rr[m] = r[j];
                    m++;
                }
            }
            try {
                Ajuste.Resultado lo = Ajuste.ajustar(xx, rr, g);
                double pred = lo.ecuacion.evaluar(x[i]);
                out("LOO", id, nom[i], f(r[i]), f(pred), f(r[i] - pred));
            } catch (IllegalArgumentException ex) {
                out("LOO", id, nom[i], f(r[i]), "NaN", "NaN");
            }
        }
        // Monte Carlo de colocacion: cada x se mueve N(0, sigmaRel*x), semilla fija.
        Random rnd = new Random(20260919L);
        int[] xs = {700, 1000, 1500, 2000, 2500, 3000, 3300};
        double[] s1 = new double[xs.length];
        double[] s2 = new double[xs.length];
        int fallosS = 0;
        for (int t = 0; t < N_MC; t++) {
            double[] xp = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                xp[i] = x[i] * (1 + sigmaRel * rnd.nextGaussian());
            }
            Ecuacion em = Ajuste.ajustar(xp, r, g).ecuacion;
            if (Asistente.criterioFirmwareS(em) != null) {
                fallosS++;
            }
            for (int j = 0; j < xs.length; j++) {
                double v = em.evaluar(xs[j]);
                s1[j] += v;
                s2[j] += v * v;
            }
        }
        for (int j = 0; j < xs.length; j++) {
            double m = s1[j] / N_MC;
            out("MC", id, xs[j], f(m), f(Math.sqrt(Math.max(0, s2[j] / N_MC - m * m))));
        }
        out("MC_S", id, fallosS, N_MC);
    }

    private static String nz(String s) {
        return s == null ? "OK" : s;
    }

    private static String f(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    private static void out(Object... c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.length; i++) {
            if (i > 0) {
                sb.append('\t');
            }
            sb.append(String.valueOf(c[i]));
        }
        System.out.println(sb);
    }
}
