package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decisiones de Diego que cambian lo que la app puede escribir (3.6.13; 3.6.14 por equipo y con alcance).
 * Java puro. Se leen de assets/decisiones.csv, que viaja dentro del APK y esta versionado en git: la firma
 * es el commit que anade la linea, con la fecha y el documento que la sostiene.
 *
 * Formato (una decision por linea; '#' comenta):
 *   id,equipo,valor,fecha,firmante,documento,alcance,texto
 *
 * - equipo: serie a la que se aplica (QA-3613-03). Una decision de SLV-002 no vale para otro equipo.
 * - valor: SI para las dispensas (PA-24, PA-14); para la regla de re-medida de un codigo con dispensa
 *   (REMEDIDA-b, QA-3612-06), RF-CAL-18 o CERTIFICADO.
 * - alcance (P12 §5.2): lo que se dispensa, y nada mas: "RF-CAL-14 P39 +15 3; RF-CAL-14 P49 -10 3" =
 *   criterio, patron, desviacion en % y margen en puntos. Un incumplimiento que no este aqui no queda
 *   dispensado.
 *
 * Una decision vale solo si la fecha es AAAA-MM-DD valida, el firmante nombra a Diego y el documento no
 * esta vacio. Mientras no haya una linea valida, la decision NO esta tomada: la casilla del operador no la
 * sustituye nunca.
 */
public final class Decisiones {

    public static final String ASSET = "decisiones.csv";

    /** Una entrada del alcance: criterio, patron, desviacion (%) y margen (puntos). */
    public static final class Alcance {
        public final String criterio;
        public final String patron;
        public final double desviacion;
        public final double margen;

        Alcance(String criterio, String patron, double desviacion, double margen) {
            this.criterio = criterio;
            this.patron = patron;
            this.desviacion = desviacion;
            this.margen = margen;
        }

        /**
         * P13-02 (decision de Diego del 19-sep ~15:40, c836cae): la cifra es un TECHO con margen, no una
         * ventana: mismo signo y |dev| <= |cifra| + margen. Un valor mejor que el aceptado siempre pasa.
         */
        public boolean cubre(String crit, String pat, double dev) {
            if (!criterio.equals(crit) || !patron.equals(pat)) {
                return false;
            }
            boolean mismoSigno = desviacion >= 0 ? dev >= 0 : dev <= 0;
            return mismoSigno && Math.abs(dev) <= Math.abs(desviacion) + margen + 1e-9;
        }

        public String texto() {
            return String.format(Locale.US, "%s %s %+.1f %% ±%.1f", criterio, patron, desviacion, margen);
        }
    }

    public static final class Decision {
        public final String id;
        public final String equipo;
        public final String valor;
        public final String fecha;
        public final String firmante;
        public final String documento;
        public final List<Alcance> alcance;
        /** Patrones que la decision saca del ajuste ("EXCLUYE P81" en el alcance). */
        public final List<String> excluidos;
        /** Patrones que la decision manda repetir ("REPITE P34" en el alcance). */
        public List<String> repetidos = new ArrayList<>();
        /** Lo que decidio, con sus palabras (puede ir vacio). */
        public final String palabras;

        Decision(String id, String equipo, String valor, String fecha, String firmante, String documento,
                 List<Alcance> alcance, List<String> excluidos, String palabras) {
            this.excluidos = excluidos;
            this.id = id;
            this.equipo = equipo;
            this.valor = valor;
            this.fecha = fecha;
            this.firmante = firmante;
            this.documento = documento;
            this.alcance = alcance;
            this.palabras = palabras == null ? "" : palabras;
        }

        public String texto() {
            StringBuilder a = new StringBuilder();
            for (Alcance x : alcance) {
                a.append(a.length() == 0 ? "" : "; ").append(x.texto());
            }
            for (String x : excluidos) {
                a.append(a.length() == 0 ? "" : "; ").append("excluye ").append(x);
            }
            return id + " (" + equipo + ") = " + valor + ", decidida por " + firmante + " el " + fecha + " ("
                    + documento + ")" + (a.length() == 0 ? "" : ", alcance: " + a) + (palabras.isEmpty() ? "" : ": " + palabras);
        }
    }

    private final Map<String, Decision> validas = new LinkedHashMap<>();
    private final List<String> rechazadas = new ArrayList<>();

    private Decisiones() { }

    public static Decisiones ninguna() {
        return new Decisiones();
    }

    private static final Pattern ALC = Pattern.compile("\\s*(RF-CAL-\\d+)\\s+(P\\d+[a-z]?|[IVX]+)\\s+([+-]?\\d+(?:\\.\\d+)?)\\s+(\\d+(?:\\.\\d+)?)\\s*");

    static List<Alcance> alcance(String t) {
        List<Alcance> l = new ArrayList<>();
        if (t == null || t.trim().isEmpty()) {
            return l;
        }
        for (String p : t.split(";")) {
            if (EXC.matcher(p).matches() || REP.matcher(p).matches()) {
                continue;
            }
            Matcher m = ALC.matcher(p);
            if (!m.matches()) {
                throw new IllegalArgumentException("alcance no entendido: " + p.trim());
            }
            l.add(new Alcance(m.group(1), m.group(2), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4))));
        }
        return l;
    }

    private static final Pattern EXC = Pattern.compile("\\s*EXCLUYE\\s+(P\\d+[a-z]?)\\s*");
    private static final Pattern REP = Pattern.compile("\\s*REPITE\\s+(P\\d+[a-z]?)\\s*");

    static List<String> excluidos(String t) {
        return lista(t, EXC);
    }

    /** "REPITE Pxx": patrones que Diego manda volver a medir (TIPO-I-REPETIR, 6048453). */
    static List<String> repetidos(String t) {
        return lista(t, REP);
    }

    private static List<String> lista(String t, Pattern pat) {
        List<String> l = new ArrayList<>();
        if (t != null) {
            for (String p : t.split(";")) {
                Matcher m = pat.matcher(p);
                if (m.matches()) {
                    l.add(m.group(1));
                }
            }
        }
        return l;
    }

    private static String clave(String id, String equipo) {
        return id + "|" + equipo;
    }

    public static Decisiones leer(String csv) {
        Decisiones d = new Decisiones();
        if (csv == null) {
            return d;
        }
        for (String l : csv.split("\r?\n")) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("id,")) {
                continue;
            }
            List<String> c = Csv.partir(t);
            if (c.size() < 6) {
                d.rechazadas.add(t + " (faltan campos: id,equipo,valor,fecha,firmante,documento[,alcance,texto])");
                continue;
            }
            String id = c.get(0).trim();
            String equipo = c.get(1).trim();
            String valor = c.get(2).trim();
            String fecha = c.get(3).trim();
            String firmante = c.get(4).trim();
            String doc = c.get(5).trim();
            if (equipo.isEmpty() || valor.isEmpty() || !Calibracion.fechaValida(fecha)
                    || !firmante.toLowerCase(Locale.ROOT).contains("diego") || doc.isEmpty()) {
                d.rechazadas.add(t + " (no vale: equipo, valor, fecha AAAA-MM-DD, firmante Diego y documento)");
                continue;
            }
            if (id.startsWith("PA-") && !"SI".equals(valor)) {
                d.rechazadas.add(t + " (una dispensa PA-* solo vale con SI)");
                continue;
            }
            List<Alcance> alc;
            try {
                alc = alcance(c.size() > 6 ? c.get(6) : "");
            } catch (IllegalArgumentException e) {
                d.rechazadas.add(t + " (" + e.getMessage() + ")");
                continue;
            }
            Decision dec = new Decision(id, equipo, valor, fecha, firmante, doc,
                    Collections.unmodifiableList(alc), Collections.unmodifiableList(excluidos(c.size() > 6 ? c.get(6) : "")),
                    c.size() > 7 ? c.get(7).trim() : "");
            dec.repetidos = Collections.unmodifiableList(repetidos(c.size() > 6 ? c.get(6) : ""));
            d.validas.put(clave(id, equipo), dec);
        }
        return d;
    }

    public boolean tomada(String id, String equipo) {
        return validas.containsKey(clave(id, equipo));
    }

    /** 3.6.17 (B-01): todos los patrones que alguna decision de este equipo saca del ajuste ("EXCLUYE Pxx"). */
    public List<String> excluidosDe(String equipo) {
        List<String> l = new ArrayList<>();
        for (Decision x : validas.values()) {
            if (x.equipo.equals(equipo)) {
                for (String p : x.excluidos) {
                    if (!l.contains(p)) {
                        l.add(p);
                    }
                }
            }
        }
        return l;
    }

    public Decision decision(String id, String equipo) {
        return validas.get(clave(id, equipo));
    }

    /** Valor de la decision para el equipo; null si no esta. */
    public String valor(String id, String equipo) {
        Decision x = decision(id, equipo);
        return x == null ? null : x.valor;
    }

    public List<String> rechazadas() {
        return rechazadas;
    }

    public String texto(String equipo) {
        StringBuilder sb = new StringBuilder();
        for (Decision x : validas.values()) {
            if (x.equipo.equals(equipo)) {
                sb.append(sb.length() == 0 ? "" : "; ").append(x.texto());
            }
        }
        return sb.length() == 0 ? "ninguna decisión registrada para " + equipo : sb.toString();
    }
}
