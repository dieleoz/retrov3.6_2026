package com.dpi.retrov36;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Campana de calibracion: todas las series de todos los patrones de un equipo.
 * Java puro.
 *
 * Persistencia: un DIARIO de solo anadir (una linea CSV por evento). Cada
 * disparo se escribe en cuanto llega, asi que la campana sobrevive a cerrar la
 * app y se retoma leyendo el diario. Nada se borra: reasignar una serie a otro
 * patron o elegir otra serie son eventos nuevos; el registro original queda.
 *
 * Eventos:
 *   SERIE,id,fecha,equipo,mac,firmware,patron,orientacion,codigo
 *   DISPARO,id,idx,fecha,respuesta_bruta,x[,colocacion]   (colocacion desde 3.6.7; si falta, 1)
 *   DESCARTE,id,idx,motivo
 *   VEREDICTO,id,veredicto,aceptada(0/1),nota
 *   REASIGNA,id,patron_nuevo,nota
 *   ELIGE,id
 *   CIERRE,fecha                     (3.6.6: la deja de solo lectura)
 *   EXPORTA,fecha,zip,md5,sha256     (3.6.6)
 * El formato es compatible hacia atras: un diario de la 3.6.5 se lee igual.
 */
public final class Campana {

    public static final String CABECERA_DIARIO = "# RTV V3.6 - diario de campana (evento,campos...). Solo se anade.";

    public static final class Disparo {
        public final int idx;
        public final String fecha;
        public final String bruta;
        public final double x;
        /** Colocacion (1..K) dentro de la serie; 1 en las campanas anteriores a la 3.6.7. */
        public final int colocacion;
        public boolean descartado;
        public String motivo = "";

        Disparo(int idx, String fecha, String bruta, double x, int colocacion) {
            this.idx = idx;
            this.fecha = fecha;
            this.bruta = bruta;
            this.x = x;
            this.colocacion = colocacion;
        }
    }

    public static final class Serie {
        public final String id;
        public final String fecha;
        public final String equipo;
        public final String mac;
        public final String firmware;
        public final String patronOriginal;
        public String patron;
        /** 0 o 90; -1 desconocida (importada). */
        public final int orientacion;
        public final char codigo;
        public final List<Disparo> disparos = new ArrayList<>();
        /** PENDIENTE hasta que el operador decide; luego OK, REPETIR o DUDOSO. */
        public String veredicto = "PENDIENTE";
        public boolean aceptada;
        public String nota = "";

        Serie(String id, String fecha, String equipo, String mac, String firmware, String patron,
              int orientacion, char codigo) {
            this.id = id;
            this.fecha = fecha;
            this.equipo = equipo;
            this.mac = mac;
            this.firmware = firmware;
            this.patronOriginal = patron;
            this.patron = patron;
            this.orientacion = orientacion;
            this.codigo = codigo;
        }

        public double[] validos() {
            List<Double> v = new ArrayList<>();
            for (Disparo d : disparos) {
                if (!d.descartado && !Double.isNaN(d.x)) {
                    v.add(d.x);
                }
            }
            return Estadistica.aVector(v);
        }

        /** Disparos validos agrupados por colocacion, en orden. */
        public List<double[]> colocaciones() {
            Map<Integer, List<Double>> g = new java.util.TreeMap<>();
            for (Disparo d : disparos) {
                if (!d.descartado && !Double.isNaN(d.x)) {
                    List<Double> l = g.get(d.colocacion);
                    if (l == null) {
                        l = new ArrayList<>();
                        g.put(d.colocacion, l);
                    }
                    l.add(d.x);
                }
            }
            List<double[]> out = new ArrayList<>();
            for (List<Double> l : g.values()) {
                out.add(Estadistica.aVector(l));
            }
            return out;
        }

        /** Media de la serie: con varias colocaciones, la media de las medias de colocacion. */
        public double media() {
            List<double[]> g = colocaciones();
            if (g.size() <= 1) {
                return Estadistica.media(validos());
            }
            double[] m = new double[g.size()];
            for (int i = 0; i < m.length; i++) {
                m[i] = Estadistica.media(g.get(i));
            }
            return Estadistica.media(m);
        }

        /** Con una colocacion, s de los disparos; con varias, s entre colocaciones. */
        public double desviacion() {
            List<double[]> g = colocaciones();
            return g.size() <= 1 ? Estadistica.desviacion(validos()) : Veredicto.sEntre(g);
        }
    }

    private final Map<String, Patron> catalogo = new LinkedHashMap<>();
    private final List<Serie> series = new ArrayList<>();
    private final Map<String, String> elegidas = new LinkedHashMap<>();
    private Writer diario;
    /** Cerrada: de solo lectura (evento CIERRE). Se puede exportar, no medir ni importar. */
    private boolean cerrada;
    private String fechaCierre = "";
    /** Exportaciones anteriores con sus hashes (evento EXPORTA), para el resumen. */
    private final List<String> exportaciones = new ArrayList<>();
    /**
     * Equipo al que pertenece la campana. La optica cambia de un equipo a otro:
     * cada uno tiene su campana y su ajuste, y NUNCA se mezclan series. Con MAC
     * vacia (solo en tests) no se comprueba.
     */
    public final String equipo;
    public final String mac;

    public Campana(List<Patron> patrones) {
        this(patrones, "", "");
    }

    public Campana(List<Patron> patrones, String equipo, String mac) {
        for (Patron p : patrones) {
            catalogo.put(p.nombre, p);
        }
        this.equipo = equipo == null ? "" : equipo;
        this.mac = mac == null ? "" : mac;
    }

    /** true si la MAC es la de este equipo (o la campana no esta atada). */
    public boolean esDeEsteEquipo(String otraMac) {
        return mac.isEmpty() || mac.equalsIgnoreCase(otraMac == null ? "" : otraMac.trim());
    }

    /** A partir de aqui, cada operacion se anade al diario. */
    public void escribirEn(Writer w) {
        diario = w;
    }

    public List<Patron> catalogo() {
        return new ArrayList<>(catalogo.values());
    }

    public Patron patron(String nombre) {
        return catalogo.get(nombre);
    }

    public List<Serie> series() {
        return series;
    }

    /** Series de un patron, SIN las de la medida puente A5 (esas no son de la campana de ajuste). */
    public List<Serie> seriesDe(String patron) {
        List<Serie> l = new ArrayList<>();
        for (Serie s : series) {
            if (s.patron.equals(patron) && !esA5(s)) {
                l.add(s);
            }
        }
        return l;
    }

    public static boolean esA5(Serie s) {
        return A5.VEREDICTO.equals(s.veredicto);
    }

    public List<Serie> seriesA5(String patron) {
        List<Serie> l = new ArrayList<>();
        for (Serie s : series) {
            if (s.patron.equals(patron) && esA5(s)) {
                l.add(s);
            }
        }
        return l;
    }

    /**
     * Protocolo de disparos de la campana (P9-B3): K x M mas frecuente entre las series
     * elegidas. {1, 9} si no hay ninguna (el de la campana del 19-sep).
     */
    public int[] protocolo() {
        Map<String, Integer> cuenta = new LinkedHashMap<>();
        for (Patron p : catalogo.values()) {
            Serie s = elegida(p.nombre);
            if (s == null) {
                continue;
            }
            List<double[]> g = s.colocaciones();
            int m = 0;
            for (double[] v : g) {
                m = Math.max(m, v.length);
            }
            int m0 = 0;
            for (Disparo d : s.disparos) {
                if (d.colocacion == 1) {
                    m0++;
                }
            }
            String k = Math.max(1, g.size()) + "x" + Math.max(m, m0);
            cuenta.put(k, cuenta.containsKey(k) ? cuenta.get(k) + 1 : 1);
        }
        String mejor = null;
        for (Map.Entry<String, Integer> e : cuenta.entrySet()) {
            if (mejor == null || e.getValue() > cuenta.get(mejor)) {
                mejor = e.getKey();
            }
        }
        if (mejor == null) {
            return new int[]{1, 9};
        }
        String[] p = mejor.split("x");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
    }

    public Serie serie(String id) {
        for (Serie s : series) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    private void evento(Object... campos) throws IOException {
        if (diario != null) {
            diario.write(Csv.unir(campos));
            diario.write("\n");
            diario.flush();
        }
    }

    public boolean cerrada() {
        return cerrada;
    }

    private void comprobarAbierta() {
        if (cerrada) {
            throw new IllegalStateException("la campaña está cerrada (" + fechaCierre + "): es de solo lectura");
        }
    }

    public void cerrarCampana(String fecha) throws IOException {
        comprobarAbierta();
        cerrada = true;
        fechaCierre = fecha;
        evento("CIERRE", fecha);
    }

    /** Anota una exportacion (el ZIP no puede llevar su propio hash: va en la siguiente). */
    public void anotarExportacion(String fecha, String nombre, String md5, String sha256) throws IOException {
        exportaciones.add(fecha + "  " + nombre + "  md5 " + md5 + "  sha256 " + sha256);
        evento("EXPORTA", fecha, nombre, md5, sha256);
    }

    public String nuevoId() {
        return String.format(Locale.US, "S%03d", series.size() + 1);
    }

    // ------------------------------------------------------------ operaciones

    public Serie nuevaSerie(String fecha, String equipo, String mac, String firmware, String patron,
                            int orientacion, char codigo) throws IOException {
        comprobarAbierta();
        if (!esDeEsteEquipo(mac)) {
            throw new IllegalArgumentException("serie de otro equipo (MAC " + mac + "); esta campaña es de "
                    + this.equipo + " " + this.mac);
        }
        Serie s = new Serie(nuevoId(), fecha, equipo, mac, firmware, patron, orientacion, codigo);
        series.add(s);
        evento("SERIE", s.id, fecha, equipo, mac, firmware, patron, orientacion, codigo);
        return s;
    }

    public Disparo agregarDisparo(Serie s, String fecha, String bruta, double x) throws IOException {
        return agregarDisparo(s, 1, fecha, bruta, x);
    }

    public Disparo agregarDisparo(Serie s, int colocacion, String fecha, String bruta, double x) throws IOException {
        comprobarAbierta();
        Disparo d = new Disparo(s.disparos.size() + 1, fecha, bruta, x, colocacion);
        s.disparos.add(d);
        evento("DISPARO", s.id, d.idx, fecha, bruta, Double.isNaN(x) ? "" : fmt(x), colocacion);
        return d;
    }

    public void descartar(Serie s, int idx, String motivo) throws IOException {
        comprobarAbierta();
        Disparo d = s.disparos.get(idx - 1);
        d.descartado = true;
        d.motivo = motivo;
        evento("DESCARTE", s.id, idx, motivo);
    }

    public void cerrar(Serie s, String veredicto, boolean aceptada, String nota) throws IOException {
        comprobarAbierta();
        s.veredicto = veredicto;
        s.aceptada = aceptada;
        s.nota = nota == null ? "" : nota;
        evento("VEREDICTO", s.id, veredicto, aceptada ? 1 : 0, s.nota);
    }

    /** "Era otro patron": la serie pasa a 'nuevo'; el original queda en patronOriginal y en el diario. */
    public void reasignar(Serie s, String nuevo, String nota) throws IOException {
        comprobarAbierta();
        if (!catalogo.containsKey(nuevo)) {
            throw new IllegalArgumentException("patron desconocido: " + nuevo);
        }
        elegidas.remove(s.patron);
        s.patron = nuevo;
        evento("REASIGNA", s.id, nuevo, nota == null ? "" : nota);
    }

    public void elegir(Serie s) throws IOException {
        comprobarAbierta();
        elegidas.put(s.patron, s.id);
        evento("ELIGE", s.id);
    }

    // ------------------------------------------------------------- consultas

    /** Serie que entra en el ajuste: la elegida a mano, si no la ultima aceptada. */
    public Serie elegida(String patron) {
        String id = elegidas.get(patron);
        if (id != null) {
            Serie s = serie(id);
            if (s != null && s.patron.equals(patron) && s.aceptada) {
                return s;
            }
        }
        Serie ultima = null;
        for (Serie s : series) {
            if (s.patron.equals(patron) && s.aceptada) {
                ultima = s;
            }
        }
        return ultima;
    }

    public enum Estado { POR_HACER, MEDIDO, REPETIR }

    public Estado estado(String patron) {
        if (elegida(patron) != null) {
            return Estado.MEDIDO;
        }
        return seriesDe(patron).isEmpty() ? Estado.POR_HACER : Estado.REPETIR;
    }

    /** Media de la serie elegida de cada patron medido, excepto 'excluir'. */
    public Map<Patron, Double> medias(String excluir) {
        Map<Patron, Double> m = new LinkedHashMap<>();
        for (Patron p : catalogo.values()) {
            if (p.nombre.equals(excluir)) {
                continue;
            }
            Serie s = elegida(p.nombre);
            if (s != null && s.validos().length > 0) {
                m.put(p, s.media());
            }
        }
        return m;
    }

    public static boolean esTipoI(Patron p) {
        return "I".equalsIgnoreCase(p.tipo.trim());
    }

    /** "amarillo 13/14, blanco 6/8, ..., tipo I 0/20". */
    public String avance() {
        Map<String, int[]> porColor = new LinkedHashMap<>();
        int tI = 0;
        int tIm = 0;
        for (Patron p : catalogo.values()) {
            boolean med = estado(p.nombre) == Estado.MEDIDO;
            if (esTipoI(p)) {
                tI++;
                if (med) {
                    tIm++;
                }
                continue;
            }
            int[] c = porColor.get(p.color);
            if (c == null) {
                c = new int[2];
                porColor.put(p.color, c);
            }
            c[1]++;
            if (med) {
                c[0]++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : porColor.entrySet()) {
            sb.append(e.getKey()).append(' ').append(e.getValue()[0]).append('/').append(e.getValue()[1]).append(", ");
        }
        sb.append("tipo I ").append(tIm).append('/').append(tI);
        return sb.toString();
    }

    /** Hueco en x entre dos patrones medidos consecutivos (por certificado) a partir del cual falta un punto. */
    public static final double HUECO_X = 600;

    /**
     * Patrones sin medir cuyo certificado cae entre dos patrones medidos del
     * mismo color y clase (tipo I u otros) cuyas x distan mas de HUECO_X: sin
     * ellos, esa zona de la curva queda sin puntos. Devuelve nombre -> motivo.
     */
    public Map<String, String> imprescindibles() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Patron p : catalogo.values()) {
            if (estado(p.nombre) == Estado.MEDIDO) {
                continue;
            }
            Patron bajo = null;
            Patron alto = null;
            Map<Patron, Double> m = medias(p.nombre);
            for (Patron q : m.keySet()) {
                if (!q.color.equalsIgnoreCase(p.color) || esTipoI(q) != esTipoI(p)) {
                    continue;
                }
                if (q.valor < p.valor && (bajo == null || q.valor > bajo.valor)) {
                    bajo = q;
                }
                if (q.valor > p.valor && (alto == null || q.valor < alto.valor)) {
                    alto = q;
                }
            }
            if (bajo != null && alto != null) {
                double xb = m.get(bajo);
                double xa = m.get(alto);
                if (Math.abs(xa - xb) > HUECO_X) {
                    out.put(p.nombre, String.format(Locale.US, "zona sin puntos entre %s (x=%.0f) y %s (x=%.0f)",
                            bajo.nombre, xb, alto.nombre, xa));
                }
            }
        }
        return out;
    }

    /** Diferencia relativa entre series aceptadas de un mismo patron que se considera conflicto. */
    public static final double CONFLICTO_REL = 0.03;

    /** Media de la ultima serie con disparos validos (aceptada o no). */
    private Double ultimaMedia(String patron) {
        List<Serie> l = seriesDe(patron);
        for (int i = l.size() - 1; i >= 0; i--) {
            if (l.get(i).validos().length > 0) {
                return l.get(i).media();
            }
        }
        return null;
    }

    /**
     * Patrones que hay que (re)medir antes que nada, con su motivo, en este orden:
     * 1. huecos: sin medir y en una zona sin puntos (imprescindibles());
     * 2. conflicto: dos series aceptadas del mismo patron que difieren mas de
     *    CONFLICTO_REL (caso real: P24, 2065 y 2443);
     * 3. orden: la ultima serie de dos patrones del mismo color y tipo
     *    contradice el orden de certificados (caso real: P29 442 -> 1723 y
     *    P30 448 -> 1661; se marcan los dos, porque no se sabe cual esta mal).
     *    Los XI se excluyen: pueden desordenarse por orientacion (se avisa al
     *    medir, pero no fuerzan la cola).
     */
    public Map<String, String> prioritarios(double tolOrden) {
        Map<String, String> out = new LinkedHashMap<>(imprescindibles());
        for (Patron p : catalogo.values()) {
            List<Double> acept = new ArrayList<>();
            for (Serie s : seriesDe(p.nombre)) {
                if (s.aceptada && s.validos().length > 0) {
                    acept.add(s.media());
                }
            }
            if (acept.size() >= 2) {
                double min = java.util.Collections.min(acept);
                double max = java.util.Collections.max(acept);
                if (max - min > CONFLICTO_REL * max && !out.containsKey(p.nombre)) {
                    out.put(p.nombre, String.format(Locale.US, "series aceptadas en conflicto: %.0f y %.0f", min, max));
                }
            }
        }
        List<Patron> lista = new ArrayList<>(catalogo.values());
        for (int i = 0; i < lista.size(); i++) {
            Patron p = lista.get(i);
            Double mp = ultimaMedia(p.nombre);
            if (mp == null || "XI".equalsIgnoreCase(p.tipo.trim())) {
                continue;
            }
            for (int j = i + 1; j < lista.size(); j++) {
                Patron q = lista.get(j);
                Double mq = ultimaMedia(q.nombre);
                if (mq == null || !q.color.equalsIgnoreCase(p.color) || !q.tipo.trim().equalsIgnoreCase(p.tipo.trim())
                        || q.valor == p.valor) {
                    continue;
                }
                Patron alto = p.valor > q.valor ? p : q;
                Patron bajo = alto == p ? q : p;
                double ma = alto == p ? mp : mq;
                double mb = alto == p ? mq : mp;
                if (ma < mb * (1 - tolOrden)) {
                    String m = String.format(Locale.US, "orden: %s (cert %.0f, x=%.0f) da menos que %s (cert %.0f, x=%.0f)",
                            alto.nombre, alto.valor, ma, bajo.nombre, bajo.valor, mb);
                    if (!out.containsKey(bajo.nombre)) {
                        out.put(bajo.nombre, m);
                    }
                    if (!out.containsKey(alto.nombre)) {
                        out.put(alto.nombre, m);
                    }
                }
            }
        }
        return out;
    }

    /** Medidas de las series elegidas (disparos validos), para el asistente de ajuste. */
    public List<Medida> medidasElegidas() {
        List<Medida> l = new ArrayList<>();
        for (Patron p : catalogo.values()) {
            Serie s = elegida(p.nombre);
            if (s == null) {
                continue;
            }
            for (Disparo d : s.disparos) {
                if (!d.descartado && !Double.isNaN(d.x)) {
                    l.add(new Medida(d.fecha, s.equipo, s.mac, s.firmware, p, s.codigo, d.bruta, d.x, 0,
                            "campana " + s.id, ""));
                }
            }
        }
        return l;
    }

    // --------------------------------------------------------------- export

    public static String cabeceraCsv() {
        return "serie_id,fecha_hora,serie,mac,firmware,patron,patron_original,valor_certificado,tipo_lamina,color,"
                + "orientacion,codigo,disparo,respuesta_bruta,x,descartado,motivo_descarte,veredicto,nota,aceptada,elegida,colocacion";
    }

    public String exportarCsv() {
        StringBuilder sb = new StringBuilder(cabeceraCsv()).append('\n');
        for (Serie s : series) {
            Patron p = catalogo.get(s.patron);
            Serie el = elegida(s.patron);
            for (Disparo d : s.disparos) {
                sb.append(Csv.unir(s.id, d.fecha, s.equipo, s.mac, s.firmware, s.patron, s.patronOriginal,
                        p == null ? "" : fmt(p.valor), p == null ? "" : p.tipo, p == null ? "" : p.color,
                        s.orientacion < 0 ? "" : String.valueOf(s.orientacion), s.codigo, d.idx, d.bruta,
                        Double.isNaN(d.x) ? "" : fmt(d.x), d.descartado ? 1 : 0, d.motivo, s.veredicto, s.nota,
                        s.aceptada ? 1 : 0, el == s ? 1 : 0, d.colocacion)).append('\n');
            }
        }
        return sb.toString();
    }

    public String resumen() {
        StringBuilder sb = new StringBuilder();
        if (!mac.isEmpty()) {
            sb.append("Equipo: serie ").append(equipo).append(", MAC ").append(mac).append('\n');
        }
        sb.append(cerrada ? "Estado: CERRADA el " + fechaCierre + " (solo lectura)\n" : "Estado: abierta\n");
        sb.append("Avance: ").append(avance()).append("\n\n");
        sb.append("patron  cert  tipo color     estado     series  elegida  n   media x    s\n");
        for (Patron p : catalogo.values()) {
            List<Serie> ls = seriesDe(p.nombre);
            Serie el = elegida(p.nombre);
            sb.append(String.format(Locale.US, "%-6s %5.0f %-4s %-9s %-10s %3d    %-6s", p.nombre, p.valor, p.tipo,
                    p.color, estado(p.nombre), ls.size(), el == null ? "-" : el.id));
            if (el != null) {
                sb.append(String.format(Locale.US, " %3d %8.1f %6.2f", el.validos().length, el.media(), el.desviacion()));
            }
            sb.append('\n');
            for (Serie s : ls) {
                int desc = 0;
                for (Disparo d : s.disparos) {
                    if (d.descartado) {
                        desc++;
                    }
                }
                List<double[]> gs = s.colocaciones();
                String rep = gs.size() <= 1 ? "" : String.format(Locale.US, " [%d colocaciones: s entre %.1f (%.1f %%), s dentro %.1f]",
                        gs.size(), Veredicto.sEntre(gs), 100 * Veredicto.sEntre(gs) / s.media(), Veredicto.sDentro(gs));
                sb.append(String.format(Locale.US, "        %s %s orient %s: n=%d (descartados %d) media %.1f s %.2f%s -> %s%s%s%s\n",
                        s.id, s.fecha, s.orientacion < 0 ? "?" : String.valueOf(s.orientacion), s.validos().length, desc,
                        s.media(), s.desviacion(), rep, s.veredicto, s.aceptada ? ", aceptada" : ", no aceptada",
                        s.patronOriginal.equals(s.patron) ? "" : " (medida como " + s.patronOriginal + ")",
                        s.nota.isEmpty() ? "" : "; nota: " + s.nota));
            }
        }
        sb.append(desvioPorPosicion());
        sb.append('\n').append(A5.evaluar(this).texto);
        if (!exportaciones.isEmpty()) {
            sb.append("\nExportaciones anteriores (el ZIP no puede llevar su propio hash):\n");
            for (String e : exportaciones) {
                sb.append("  ").append(e).append('\n');
            }
        }
        Map<String, String> imp = imprescindibles();
        sb.append("\nFalta medir: ");
        int faltan = 0;
        for (Patron p : catalogo.values()) {
            if (estado(p.nombre) != Estado.MEDIDO) {
                sb.append(p.nombre).append(imp.containsKey(p.nombre) ? "*" : "").append(' ');
                faltan++;
            }
        }
        sb.append(faltan == 0 ? "nada" : "").append("\n");
        for (Map.Entry<String, String> e : imp.entrySet()) {
            sb.append("* imprescindible ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    /**
     * T-C38 (efecto del primer disparo): por serie, desvio de cada posicion de
     * disparo respecto a la mediana de la serie (todos los disparos con lectura,
     * tambien los descartados), y la media por posicion sobre todas las series.
     * El disparo de asentamiento no esta aqui: se descarta antes y solo va al
     * registro de tramas.
     */
    public String desvioPorPosicion() {
        StringBuilder sb = new StringBuilder("\nDesvío por posición de disparo respecto a la mediana de su serie (T-C38):\n");
        List<List<Double>> porPos = new ArrayList<>();
        for (Serie s : series) {
            List<Double> xs = new ArrayList<>();
            for (Disparo d : s.disparos) {
                if (!Double.isNaN(d.x)) {
                    xs.add(d.x);
                }
            }
            if (xs.size() < 2) {
                continue;
            }
            double med = Estadistica.mediana(Estadistica.aVector(xs));
            sb.append("  ").append(s.id).append(' ').append(s.patron).append(':');
            for (int i = 0; i < s.disparos.size(); i++) {
                Disparo d = s.disparos.get(i);
                if (Double.isNaN(d.x)) {
                    sb.append(" ").append(i + 1).append(":-");
                    continue;
                }
                double dv = d.x - med;
                sb.append(String.format(Locale.US, " %d:%+.0f", i + 1, dv));
                while (porPos.size() <= i) {
                    porPos.add(new ArrayList<Double>());
                }
                porPos.get(i).add(dv);
            }
            sb.append('\n');
        }
        if (!porPos.isEmpty()) {
            sb.append("  Media por posición (n series):");
            for (int i = 0; i < porPos.size(); i++) {
                double[] v = Estadistica.aVector(porPos.get(i));
                sb.append(String.format(Locale.US, " %d:%+.1f(%d)", i + 1, Estadistica.media(v), v.length));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }

    // ---------------------------------------------------------------- lectura

    /** Reconstruye la campana leyendo el diario. Las lineas que no se entienden se cuentan y se saltan. */
    public int leerDiario(Reader r) throws IOException {
        Writer guardado = diario;
        diario = null;
        int malas = 0;
        try {
            BufferedReader br = new BufferedReader(r);
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty() || l.startsWith("#")) {
                    continue;
                }
                try {
                    if (!aplicar(Csv.partir(l))) {
                        malas++;
                    }
                } catch (RuntimeException e) {
                    malas++;
                }
            }
        } finally {
            diario = guardado;
        }
        return malas;
    }

    private boolean aplicar(List<String> c) throws IOException {
        String ev = c.get(0);
        switch (ev) {
            case "SERIE": {
                if (!esDeEsteEquipo(c.get(4))) {
                    return false; // de otro equipo: no se mezcla
                }
                Serie s = new Serie(c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), c.get(6),
                        Integer.parseInt(c.get(7)), c.get(8).charAt(0));
                series.add(s);
                return true;
            }
            case "DISPARO": {
                Serie s = serie(c.get(1));
                if (s == null) {
                    return false;
                }
                String xs = c.get(5);
                int col = c.size() > 6 && !c.get(6).isEmpty() ? Integer.parseInt(c.get(6)) : 1;
                s.disparos.add(new Disparo(Integer.parseInt(c.get(2)), c.get(3), c.get(4),
                        xs.isEmpty() ? Double.NaN : Double.parseDouble(xs), col));
                return true;
            }
            case "DESCARTE": {
                Serie s = serie(c.get(1));
                if (s == null) {
                    return false;
                }
                Disparo d = s.disparos.get(Integer.parseInt(c.get(2)) - 1);
                d.descartado = true;
                d.motivo = c.get(3);
                return true;
            }
            case "VEREDICTO": {
                Serie s = serie(c.get(1));
                if (s == null) {
                    return false;
                }
                s.veredicto = c.get(2);
                s.aceptada = "1".equals(c.get(3));
                s.nota = c.size() > 4 ? c.get(4) : "";
                return true;
            }
            case "REASIGNA": {
                Serie s = serie(c.get(1));
                if (s == null) {
                    return false;
                }
                elegidas.remove(s.patron);
                s.patron = c.get(2);
                return true;
            }
            case "ELIGE": {
                Serie s = serie(c.get(1));
                if (s == null) {
                    return false;
                }
                elegidas.put(s.patron, s.id);
                return true;
            }
            case "CIERRE":
                cerrada = true;
                fechaCierre = c.get(1);
                return true;
            case "EXPORTA":
                exportaciones.add(c.get(1) + "  " + c.get(2) + "  md5 " + c.get(3) + "  sha256 " + c.get(4));
                return true;
            default:
                return false;
        }
    }
}
