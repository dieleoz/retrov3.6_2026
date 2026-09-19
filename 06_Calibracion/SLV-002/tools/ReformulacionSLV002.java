import com.dpi.retrov36.Ajuste;
import com.dpi.retrov36.Asistente;
import com.dpi.retrov36.Ecuacion;
import com.dpi.retrov36.Fabrica;
import com.dpi.retrov36.Medida;
import com.dpi.retrov36.Patron;
import com.dpi.retrov36.Tramas;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Reformulacion y simulacion de SLV-002 (19-sep-2026) con el MISMO codigo de la app RetroV36
 * (commit fijado por tools/reformulacion_simulacion_slv002.py): Asistente.deCodigo, Asistente.proponer,
 * Ajuste.ajustar, Ajuste.anclada, Asistente.criterioFirmwareS, Asistente.validarForma,
 * Asistente.comprobarOscuro, Ecuacion.respuestaFloat32, Fabrica.ecuacion y Tramas.coeficiente.
 * No reimplementa ningun ajuste ni ningun criterio: los llama y vuelca el resultado en TSV.
 *
 * Uso: java -cp clases ReformulacionSLV002 catalogo.csv ordenes.tsv
 * Ordenes (una por linea, campos separados por tabulador):
 *   P    grupo patron cert tipo color x sx        punto de un grupo de ajuste (x = media combinada, sx = su sigma)
 *   COD  patron cert tipo color                    codigos a los que pertenece el patron (Asistente.deCodigo)
 *   FIT  id grupo codigo metodo x0 vig(c3,c2,c1,c0) ajuste de la app; metodo 1, 2 o A (recta anclada en x0, R = 0)
 *   MC   id grupo codigo metodo x0 N semilla        cada x se mueve N(0, sx) y se reajusta con el mismo metodo
 *   BOOT id grupo codigo metodo x0 N semilla        remuestreo de patrones con reposicion y reajuste
 *   LOO  id grupo codigo metodo x0                  validacion cruzada dejando uno fuera
 *   CHK  id codigo c3 c2 c1 c0 xmin xosc            criterio de #S, C2 y oscuro (B13) de una curva dada
 *   EV   id c3 c2 c1 c0 x                           R(x) en double y respuesta float32 del firmware en round(x)
 *   COB  id catalogo.csv                           Asistente.cobertura de los 12 codigos con otro catalogo
 */
public final class ReformulacionSLV002 {

    private static final int[] REJILLA = {566, 575, 600, 700, 800, 900, 1000, 1200, 1500, 1700, 2000, 2300,
            2500, 2800, 3000, 3300, 3500, 4000, 4300};

    private static final Map<String, List<String[]>> GRUPOS = new LinkedHashMap<>();
    private static List<Patron> catalogo;

    public static void main(String[] a) throws Exception {
        try (Reader r = new InputStreamReader(new FileInputStream(a[0]), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(a[1]),
                StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) {
                if (l.isEmpty() || l.startsWith("#")) {
                    continue;
                }
                String[] c = l.split("\t");
                switch (c[0]) {
                    case "P": GRUPOS.computeIfAbsent(c[1], k -> new ArrayList<>()).add(c); break;
                    case "COD": cod(c); break;
                    case "FIT": fit(c); break;
                    case "MC": mc(c, false); break;
                    case "BOOT": mc(c, true); break;
                    case "LOO": loo(c); break;
                    case "CHK": chk(c); break;
                    case "EV": ev(c); break;
                    case "COB": cob(c); break;
                    default: throw new IllegalArgumentException("orden desconocida: " + c[0]);
                }
            }
        }
    }

    private static void cod(String[] c) {
        Patron p = new Patron(c[1], Double.parseDouble(c[2]), c[3], c[4]);
        StringBuilder sb = new StringBuilder();
        for (char k : Fabrica.CODIGOS) {
            if (Asistente.deCodigo(p, k)) {
                sb.append(k);
            }
        }
        out("COD", c[1], sb.length() == 0 ? "-" : sb.toString());
    }

    /** Una Medida por patron con x = media combinada: Asistente.puntos la devuelve tal cual (n = 1). */
    private static List<Medida> medidas(List<String[]> g, char k) {
        List<Medida> m = new ArrayList<>();
        for (String[] c : g) {
            Patron p = new Patron(c[2], Double.parseDouble(c[3]), c[4], c[5]);
            m.add(new Medida("2026-09-19", "SLV-002", "00:21:13:05:19:3B", "3.6.2", p, k, "",
                    Double.parseDouble(c[6]), 1, "combinada", ""));
        }
        return m;
    }

    private static Ajuste.Resultado ajustar(double[] x, double[] r, String metodo, double x0) {
        if (metodo.equals("A")) {
            return Ajuste.anclada(x, r, x0, 0);
        }
        return Ajuste.ajustar(x, r, Integer.parseInt(metodo));
    }

    private static void fit(String[] c) {
        String id = c[1];
        List<String[]> g = GRUPOS.get(c[2]);
        char k = c[3].charAt(0);
        String metodo = c[4];
        double x0 = Double.parseDouble(c[5]);
        String[] v = c[6].split(",");
        Ecuacion vig = new Ecuacion(Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]),
                Double.parseDouble(v[3]));
        int grado;
        if (metodo.equals("A")) {
            Asistente.X_ANCLA = x0;
            Asistente.R_ANCLA = 0;
            Asistente.ORIGEN_ANCLA = "serie OSCURO S060, 5 colocaciones";
            grado = Asistente.GRADO_ANCLADA;
        } else {
            grado = Integer.parseInt(metodo);
        }
        Asistente.Propuesta pr = Asistente.proponer(k, grado, medidas(g, k), vig, catalogo);
        for (String l : pr.informe.split("\n")) {
            out("INF", id, l);
        }
        out("ESC", id, pr.escribible() ? 1 : 0, String.join(" / ", pr.bloqueos));
        out("INC", id, pr.incumplimientos.size(), String.join(" / ", pr.incumplimientos));
        if (pr.ajuste != null) {
            Ecuacion e = pr.ajuste.ecuacion;
            // Lo que se escribiria: el texto de #S (Tramas.coeficiente) vuelto a leer como double.
            out("AJ", id, Tramas.coeficiente(e.c3), Tramas.coeficiente(e.c2), Tramas.coeficiente(e.c1),
                    Tramas.coeficiente(e.c0), f(pr.ajuste.rms), f(pr.ajuste.errorMaximo));
        }
    }

    private static void mc(String[] c, boolean boot) {
        String id = c[1];
        List<String[]> g = GRUPOS.get(c[2]);
        char k = c[3].charAt(0);
        String metodo = c[4];
        double x0 = Double.parseDouble(c[5]);
        int n = Integer.parseInt(c[6]);
        Random rnd = new Random(Long.parseLong(c[7]));
        int m = g.size();
        double[] x = new double[m];
        double[] sx = new double[m];
        double[] r = new double[m];
        for (int i = 0; i < m; i++) {
            x[i] = Double.parseDouble(g.get(i)[6]);
            sx[i] = Double.parseDouble(g.get(i)[7]);
            r[i] = Double.parseDouble(g.get(i)[3]);
        }
        double[] s1 = new double[REJILLA.length];
        double[] s2 = new double[REJILLA.length];
        int fallosS = 0;
        int fallosOsc = 0;
        int validas = 0;
        for (int t = 0; t < n; t++) {
            double[] xp = new double[m];
            double[] rp = new double[m];
            for (int i = 0; i < m; i++) {
                int j = boot ? rnd.nextInt(m) : i;
                xp[i] = boot ? x[j] : x[i] + sx[i] * rnd.nextGaussian();
                rp[i] = r[j];
            }
            Ecuacion e;
            try {
                e = ajustar(xp, rp, metodo, x0).ecuacion;
            } catch (IllegalArgumentException ex) {
                continue; // remuestreo degenerado (menos niveles distintos de los que pide el grado)
            }
            validas++;
            if (Asistente.criterioFirmwareS(e) != null) {
                fallosS++;
            }
            if (Asistente.comprobarOscuro(e, k, 575, Asistente.OSCURO_MARGEN, Asistente.OSCURO_MINIMO) != null) {
                fallosOsc++;
            }
            for (int j = 0; j < REJILLA.length; j++) {
                double vv = e.evaluar(REJILLA[j]);
                s1[j] += vv;
                s2[j] += vv * vv;
            }
        }
        String tag = boot ? "BOOT" : "MC";
        for (int j = 0; j < REJILLA.length; j++) {
            double mm = s1[j] / validas;
            out(tag, id, REJILLA[j], f(mm), f(Math.sqrt(Math.max(0, s2[j] / validas - mm * mm))));
        }
        out(tag + "_S", id, fallosS, fallosOsc, validas);
    }

    private static void loo(String[] c) {
        String id = c[1];
        List<String[]> g = GRUPOS.get(c[2]);
        String metodo = c[4];
        double x0 = Double.parseDouble(c[5]);
        int m = g.size();
        for (int i = 0; i < m; i++) {
            double[] xx = new double[m - 1];
            double[] rr = new double[m - 1];
            for (int j = 0, q = 0; j < m; j++) {
                if (j != i) {
                    xx[q] = Double.parseDouble(g.get(j)[6]);
                    rr[q] = Double.parseDouble(g.get(j)[3]);
                    q++;
                }
            }
            double xi = Double.parseDouble(g.get(i)[6]);
            try {
                double pred = ajustar(xx, rr, metodo, x0).ecuacion.evaluar(xi);
                out("LOO", id, g.get(i)[2], g.get(i)[3], f(pred));
            } catch (IllegalArgumentException ex) {
                out("LOO", id, g.get(i)[2], g.get(i)[3], "NaN");
            }
        }
    }

    private static void chk(String[] c) {
        char k = c[2].charAt(0);
        Ecuacion e = new Ecuacion(Double.parseDouble(c[3]), Double.parseDouble(c[4]), Double.parseDouble(c[5]),
                Double.parseDouble(c[6]));
        double xmin = Double.parseDouble(c[7]);
        double xosc = Double.parseDouble(c[8]);
        String s = Asistente.criterioFirmwareS(e);
        Asistente.Forma fo = Asistente.validarForma(e, xmin);
        String o1 = Asistente.comprobarOscuro(e, k, xosc, Asistente.OSCURO_MARGEN, Asistente.OSCURO_MINIMO);
        String o2 = Asistente.comprobarOscuro(e, k, 575, Asistente.OSCURO_MARGEN, Asistente.OSCURO_MINIMO);
        out("CHK", c[1], s == null ? "OK" : s, fo.bloqueos.isEmpty() ? "OK" : String.join(" / ", fo.bloqueos),
                o1 == null ? "OK" : o1, o2 == null ? "OK" : o2,
                f(e.evaluar(xosc)), e.respuestaFloat32((int) Math.round(xosc)),
                f(e.evaluar(575)), e.respuestaFloat32(575),
                f(e.evaluar(600)), e.respuestaFloat32(600), f(e.evaluar(4300)), e.respuestaFloat32(4300),
                f(Fabrica.ecuacion(k).evaluar(xosc)), f(Fabrica.ecuacion(k).evaluar(575)));
    }

    private static void cob(String[] c) throws java.io.IOException {
        List<Patron> cat;
        try (Reader r = new InputStreamReader(new FileInputStream(c[2]), StandardCharsets.UTF_8)) {
            cat = Patron.leer(r);
        }
        for (char k : Fabrica.CODIGOS) {
            Asistente.Cobertura cb = Asistente.cobertura(k, cat);
            out("COB", c[1], k, cb.gradoMaximo, cb.texto());
        }
    }

    private static void ev(String[] c) {
        Ecuacion e = new Ecuacion(Double.parseDouble(c[2]), Double.parseDouble(c[3]), Double.parseDouble(c[4]),
                Double.parseDouble(c[5]));
        double x = Double.parseDouble(c[6]);
        out("EV", c[1], f(e.evaluar(x)), e.respuestaFloat32((int) Math.round(x)));
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
