package com.dpi.retrov36;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Importa una campana EXPORTADA (el ZIP de "Exportar campana" o su campana.csv) a la
 * campana abierta. Java puro (3.6.9).
 *
 * Reconstruye las series tal cual (veredicto, aceptada, elegida, orientacion,
 * colocacion, descartes con su motivo, reasignaciones y notas) SIN volver a juzgarlas.
 * Cada serie nueva lleva en su nota el origen (fichero, md5 e identificador original),
 * que queda en el diario (evento VEREDICTO). Si una fila es de otro equipo (serie o MAC
 * distinta de la campana abierta), no se importa nada. Las series ya importadas (mismo
 * primer disparo) se saltan: reimportar no duplica.
 *
 * P10-C8 (3.6.10): ATOMICO. Todo se valida antes de escribir la primera linea del diario
 * (equipo, patrones, numeros, colocaciones): o entra todo, o nada. No pisa la serie que el
 * operador ya tiene elegida para un patron. Compara el firmware de las series importadas con el
 * de las que ya hay y lo avisa.
 */
public final class ImportadorCampana {

    private ImportadorCampana() { }

    public static final class Resultado {
        public int series;
        public int yaEstaban;
        public int disparos;
        public final List<String> avisos = new ArrayList<>();
        public String texto() {
            return series + " series importadas (" + disparos + " disparos), " + yaEstaban + " ya estaban."
                    + (avisos.isEmpty() ? "" : " Avisos: " + String.join("; ", avisos));
        }
    }

    /** Diario de campana dentro de un ZIP (diario_*.csv); null si no lo trae. */
    public static String diarioDeZip(byte[] zip) throws IOException {
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String n = e.getName();
                if (n.startsWith("diario_") && n.endsWith(".csv")) {
                    return new String(leer(z), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    /**
     * Importa desde el DIARIO de un ZIP exportado: es la reconstruccion exacta (incluye las
     * series sin disparos, que campana.csv no puede llevar: en el ZIP del 19-sep, S010). Se
     * prefiere al campana.csv cuando el ZIP lo trae.
     */
    public static Resultado importarDiario(Campana c, String diario, List<Patron> catalogo, String origen)
            throws IOException {
        // Primero se lee sin filtro de equipo, para poder decir de quien es.
        Campana todo = new Campana(catalogo);
        int malas = todo.leerDiario(new java.io.StringReader(diario));
        if (malas > 0) {
            throw new IllegalArgumentException(malas + " líneas del diario no se entienden: no se importa nada");
        }
        for (Campana.Serie s : todo.series()) {
            if (!c.esDeEsteEquipo(s.mac) || (!c.equipo.isEmpty() && !c.equipo.equalsIgnoreCase(s.equipo))) {
                throw new IllegalArgumentException("la campaña es de otro equipo (" + s.equipo + ", " + s.mac
                        + "); la abierta es de " + c.equipo + " (" + c.mac + "): no se importa nada");
            }
        }
        Set<String> vistas = claves(c);
        Resultado r = new Resultado();
        compararFirmware(c, firmwares(todo.series()), r);
        Set<String> yaElegidos = elegidosDe(c);
        List<Campana.Serie> elegidas = new ArrayList<>();
        for (Campana.Serie s : todo.series()) {
            if (vistas.contains(clave(s))) {
                r.yaEstaban++;
                continue;
            }
            Campana.Serie n = c.nuevaSerie(s.fecha, s.equipo, s.mac, s.firmware, s.patronOriginal, s.orientacion, s.codigo);
            for (Campana.Disparo d : s.disparos) {
                c.agregarDisparo(n, d.colocacion, d.fecha, d.bruta, d.x);
                r.disparos++;
            }
            for (Campana.Disparo d : s.disparos) {
                if (d.descartado) {
                    c.descartar(n, d.idx, d.motivo);
                }
            }
            if (!s.patron.equals(s.patronOriginal)) {
                c.reasignar(n, s.patron, "importada: ya estaba reasignada a " + s.patron);
            }
            String org = "importada de " + origen + ", serie " + s.id;
            c.cerrar(n, s.veredicto, s.aceptada, s.nota.isEmpty() ? org : s.nota + " | " + org);
            if (todo.elegida(s.patron) == s) {
                if (yaElegidos.contains(s.patron)) {
                    r.avisos.add(s.patron + ": se mantiene la serie elegida por el operador");
                } else {
                    elegidas.add(n);
                }
            }
            r.series++;
        }
        for (Campana.Serie s : elegidas) {
            c.elegir(s);
        }
        return r;
    }

    private static Set<String> elegidosDe(Campana c) {
        Set<String> s = new HashSet<>();
        for (Campana.Serie x : c.series()) {
            if (c.elegida(x.patron) != null) {
                s.add(x.patron);
            }
        }
        return s;
    }

    private static Set<String> firmwares(List<Campana.Serie> l) {
        Set<String> s = new java.util.LinkedHashSet<>();
        for (Campana.Serie x : l) {
            s.add(x.firmware);
        }
        return s;
    }

    /** Avisa si las series importadas son de otro firmware que las que ya hay. */
    private static void compararFirmware(Campana c, Set<String> importados, Resultado r) {
        Set<String> actuales = firmwares(c.series());
        for (String f : importados) {
            if (!actuales.isEmpty() && !actuales.contains(f)) {
                r.avisos.add("firmware distinto en lo importado (" + f + ") frente a " + actuales);
            }
        }
        if (importados.size() > 1) {
            r.avisos.add("lo importado mezcla firmwares: " + importados);
        }
    }

    private static String clave(Campana.Serie s) {
        if (s.disparos.isEmpty()) {
            return s.fecha + "|" + s.patronOriginal + "|sin disparos";
        }
        Campana.Disparo d = s.disparos.get(0);
        return d.fecha + "|" + s.patronOriginal + "|" + d.bruta;
    }

    private static Set<String> claves(Campana c) {
        Set<String> v = new HashSet<>();
        for (Campana.Serie s : c.series()) {
            v.add(clave(s));
        }
        return v;
    }

    /** Lineas de campana.csv dentro de un ZIP; null si el ZIP no la trae. */
    public static List<String> csvDeZip(byte[] zip) throws IOException {
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().equals("campana.csv") || e.getName().endsWith("/campana.csv")) {
                    return lineas(leer(z));
                }
            }
        }
        return null;
    }

    public static boolean esZip(byte[] b) {
        return b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
    }

    public static boolean esCsvDeCampana(List<String> l) {
        for (String s : l) {
            if (!s.trim().isEmpty() && !s.startsWith("#")) {
                return s.startsWith("serie_id,");
            }
        }
        return false;
    }

    static byte[] leer(InputStream in) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            o.write(b, 0, n);
        }
        return o.toByteArray();
    }

    public static List<String> lineas(byte[] b) {
        List<String> l = new ArrayList<>();
        for (String s : new String(b, StandardCharsets.UTF_8).split("\n")) {
            l.add(s.endsWith("\r") ? s.substring(0, s.length() - 1) : s);
        }
        return l;
    }

    /**
     * @param origen texto del origen para las notas (nombre y md5 del fichero).
     * @throws IllegalArgumentException si alguna fila es de otro equipo o el formato no es el de campana.csv.
     */
    public static Resultado importar(Campana c, List<String> lineas, String origen) throws IOException {
        Map<String, Integer> col = null;
        Map<String, List<List<String>>> porSerie = new LinkedHashMap<>();
        for (String l : lineas) {
            if (l.trim().isEmpty() || l.startsWith("#")) {
                continue;
            }
            List<String> f = Csv.partir(l);
            if (col == null) {
                if (!"serie_id".equals(f.get(0))) {
                    throw new IllegalArgumentException("no es un campana.csv (la cabecera no empieza por serie_id)");
                }
                col = new LinkedHashMap<>();
                for (int i = 0; i < f.size(); i++) {
                    col.put(f.get(i), i);
                }
                continue;
            }
            String equipo = campo(f, col, "serie");
            String mac = campo(f, col, "mac");
            if (!c.esDeEsteEquipo(mac) || (!c.equipo.isEmpty() && !c.equipo.equalsIgnoreCase(equipo))) {
                throw new IllegalArgumentException("la campaña es de otro equipo (" + equipo + ", " + mac
                        + "); la abierta es de " + c.equipo + " (" + c.mac + "): no se importa nada");
            }
            String id = campo(f, col, "serie_id");
            List<List<String>> g = porSerie.get(id);
            if (g == null) {
                g = new ArrayList<>();
                porSerie.put(id, g);
            }
            g.add(f);
        }
        if (col == null) {
            throw new IllegalArgumentException("fichero vacío");
        }
        // Validacion completa ANTES de escribir nada (P10-C8).
        Set<String> fws = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, List<List<String>>> e : porSerie.entrySet()) {
            for (List<String> f : e.getValue()) {
                String po = campo(f, col, "patron_original");
                String pa = campo(f, col, "patron");
                if (c.patron(po) == null || c.patron(pa) == null) {
                    throw new IllegalArgumentException("patrón desconocido en " + e.getKey() + ": "
                            + (c.patron(po) == null ? po : pa) + ": no se importa nada");
                }
                try {
                    String xs = campo(f, col, "x");
                    if (!xs.isEmpty()) {
                        Double.parseDouble(xs);
                    }
                    String cs = col.containsKey("colocacion") ? campo(f, col, "colocacion") : "";
                    if (!cs.isEmpty()) {
                        Integer.parseInt(cs);
                    }
                    String o = campo(f, col, "orientacion");
                    if (!o.isEmpty()) {
                        Integer.parseInt(o);
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("número no válido en " + e.getKey() + ": no se importa nada");
                }
                fws.add(campo(f, col, "firmware"));
            }
        }
        Set<String> vistas = claves(c);
        Resultado r = new Resultado();
        compararFirmware(c, fws, r);
        Set<String> yaElegidos = elegidosDe(c);
        List<Campana.Serie> elegidas = new ArrayList<>();
        for (Map.Entry<String, List<List<String>>> e : porSerie.entrySet()) {
            List<List<String>> filas = e.getValue();
            List<String> a = filas.get(0);
            String patronOrig = campo(a, col, "patron_original");
            String patron = campo(a, col, "patron");
            if (vistas.contains(campo(a, col, "fecha_hora") + "|" + patronOrig + "|" + campo(a, col, "respuesta_bruta"))) {
                r.yaEstaban++;
                continue;
            }
            if (c.patron(patronOrig) == null || c.patron(patron) == null) {
                throw new IllegalArgumentException("patrón desconocido en " + e.getKey() + ": " + patronOrig);
            }
            String o = campo(a, col, "orientacion");
            String cod = campo(a, col, "codigo");
            Campana.Serie s = c.nuevaSerie(campo(a, col, "fecha_hora"), campo(a, col, "serie"), campo(a, col, "mac"),
                    campo(a, col, "firmware"), patronOrig, o.isEmpty() ? -1 : Integer.parseInt(o),
                    cod.isEmpty() ? 'e' : cod.charAt(0));
            List<Integer> descartes = new ArrayList<>();
            List<String> motivos = new ArrayList<>();
            for (List<String> f : filas) {
                String xs = campo(f, col, "x");
                String cs = col.containsKey("colocacion") ? campo(f, col, "colocacion") : "";
                Campana.Disparo d = c.agregarDisparo(s, cs.isEmpty() ? 1 : Integer.parseInt(cs), campo(f, col, "fecha_hora"),
                        campo(f, col, "respuesta_bruta"), xs.isEmpty() ? Double.NaN : Double.parseDouble(xs));
                r.disparos++;
                if ("1".equals(campo(f, col, "descartado"))) {
                    descartes.add(d.idx);
                    motivos.add(campo(f, col, "motivo_descarte"));
                }
            }
            for (int i = 0; i < descartes.size(); i++) {
                c.descartar(s, descartes.get(i), motivos.get(i));
            }
            if (!patron.equals(patronOrig)) {
                c.reasignar(s, patron, "importada: ya estaba reasignada a " + patron);
            }
            String nota = campo(a, col, "nota");
            String org = "importada de " + origen + ", serie " + e.getKey();
            c.cerrar(s, campo(a, col, "veredicto"), "1".equals(campo(a, col, "aceptada")),
                    nota.isEmpty() ? org : nota + " | " + org);
            if ("1".equals(campo(a, col, "elegida"))) {
                if (yaElegidos.contains(s.patron)) {
                    r.avisos.add(s.patron + ": se mantiene la serie elegida por el operador");
                } else {
                    elegidas.add(s);
                }
            }
            r.series++;
        }
        for (Campana.Serie s : elegidas) {
            if (s.aceptada) {
                c.elegir(s);
            }
        }
        return r;
    }

    private static String campo(List<String> f, Map<String, Integer> col, String nombre) {
        Integer i = col.get(nombre);
        return i == null || i >= f.size() ? "" : f.get(i);
    }
}
