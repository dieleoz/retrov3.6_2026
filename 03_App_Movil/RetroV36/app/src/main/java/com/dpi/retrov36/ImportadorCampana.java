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
        public int pasos;
        public int anuladas;
        public int renombrados;
        /**
         * RTV 1.0.0-rc6 (D-1): tipo de banco del ZIP que la campana ADOPTO al importar, o "" si no cambio de
         * banco. Se adopta cuando la campana no tiene ningun paso propio: quedarse en el banco anterior deja
         * sin medir todos los AJUSTE de la cola nueva y ningun codigo sale calibrable (BancoCola.calibrable:316).
         */
        public String bancoAdoptado = "";
        /**
         * RTV 1.0.0-rc6 (D-1): tipo de banco del ZIP que NO se pudo adoptar porque la campana ya tiene pasos
         * propios (cambiar de cola los borraria, Campana.elegirCola:586-590). La pantalla lo pregunta UNA vez,
         * con su boton; "" si no hay nada que preguntar.
         */
        public String bancoPorResolver = "";
        public final List<String> avisos = new ArrayList<>();
        public String texto() {
            return series + " series importadas (" + disparos + " disparos), " + yaEstaban + " ya estaban"
                    + (pasos > 0 ? ", " + pasos + " pasos del banco" : "") + (anuladas > 0 ? ", " + anuladas + " anuladas" : "") + "."
                    + (avisos.isEmpty() ? "" : " Avisos: " + String.join("; ", avisos));
        }
    }

    /** Diario de campana dentro de un ZIP (diario_*.csv); null si no lo trae. */
    /** QA-3615-07: true si el ZIP es un incremental (ligero): trae indice.sha256 y no se puede importar. */
    public static boolean esIncremental(byte[] zip) throws IOException {
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if ("indice.sha256".equals(e.getName()) || e.getName().contains(".desde_")) {
                    return true;
                }
            }
        }
        return false;
    }

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
            if (!c.esDeEsteEquipo(s.mac) || !mismaSerie(c, todo, s.equipo)) {
                throw new IllegalArgumentException("la campaña es de otro equipo (" + s.equipo + ", " + s.mac
                        + "); la abierta es de " + c.equipo + " (" + c.mac + "): no se importa nada");
            }
        }
        // RTV 1.0.0-rc6: la FAMILIA de firmware bloquea igual que el equipo y la MAC, y por la misma razon: lo
        // que entra ya no se puede separar. La MAC no basta — no cambia al grabar, porque es del modulo
        // Bluetooth — y al SLV-003-2026 se le grabo la V4.6 el 19-sep sin que cambiara la suya.
        // Se comprueban TODAS las familias del diario, no solo la primera: si el propio ZIP mezclara dos, la
        // guarda pasaria y luego nuevaSerie lanzaria a mitad de la escritura, rompiendo la atomicidad (P10-C8).
        comprobarFamilia(c, todo.familia());
        for (Campana.Serie s : todo.series()) {
            comprobarFamilia(c, Familia.de(s.firmware));
            if (!Familia.compatibles(todo.familia(), Familia.de(s.firmware))) {
                throw new IllegalArgumentException("el ZIP mezcla firmwares de familias distintas ("
                        + Familia.texto(todo.familia()) + " y " + Familia.texto(Familia.de(s.firmware))
                        + "): no se importa nada");
            }
        }
        // QA-3610-08: atomico tambien por diario. Todo patron (original y reasignado) tiene que estar en
        // el catalogo ANTES de escribir nada; si no, reasignar lanzaria a mitad de la importacion.
        for (Campana.Serie s : todo.series()) {
            for (String n : new String[]{s.patronOriginal, s.patron}) {
                if (c.patron(n) == null) {
                    throw new IllegalArgumentException("la serie " + s.id + " es de un patrón que no está en el catálogo ("
                            + n + "): no se importa nada");
                }
            }
        }
        for (Map.Entry<Integer, String> e : todo.pasos().entrySet()) {
            String sid = todo.seriePaso(e.getKey());
            if (sid != null && !sid.isEmpty() && todo.serie(sid) == null) {
                throw new IllegalArgumentException("el paso " + e.getKey() + " del banco apunta a una serie que no está ("
                        + sid + "): no se importa nada");
            }
        }
        // QA-3615-03 / P14-B01: el tipo de banco viaja. Un diario sin COLA pero con PASO es de la cola completa
        // (3.6.10 a 3.6.14). Los ordenes de una cola no valen en otra, asi que los PASO solo se traen si el banco
        // del ZIP es el de la campana.
        //
        // RTV 1.0.0-rc6 (D-1, campo del 19-sep, noche): hasta la rc5 la campana se quedaba en SU banco y la
        // importacion solo lo AVISABA. Con un banco distinto no llega ni un PASO, asi que ningun codigo sale
        // calibrable (BancoCola.calibrable:316 -> FlujoCalibracion.plan:584 "no calibrable: faltan patrones de
        // AJUSTE o RE-MEDIDA"), y el operador tenia que adivinar que se arreglaba en otra pantalla. Ahora:
        //   - si la campana no tiene NINGUN paso propio, adopta el banco del ZIP (no se pierde nada: no hay
        //     estado de cola que borrar) y los PASO entran;
        //   - si ya tiene pasos propios, no se pisan en silencio: se deja dicho en bancoPorResolver para que la
        //     pantalla lo pregunte una sola vez, con su boton.
        String tipoOrigen = todo.colaElegida() ? todo.colaTipo() : todo.pasos().isEmpty() ? null : "COMPLETO";
        boolean destinoConPasos = !c.pasos().isEmpty();
        boolean mismoBanco = tipoOrigen != null && tipoOrigen.equals(c.colaTipo());
        boolean adoptar = tipoOrigen != null && !mismoBanco && !destinoConPasos;
        boolean pasosValen = tipoOrigen != null && (mismoBanco || adoptar);
        Set<String> vistas = claves(c);
        Map<String, String> idPorClave = new java.util.HashMap<>();
        for (Campana.Serie s : c.series()) {
            idPorClave.put(clave(s), s.id);
        }
        Map<String, String> nuevoId = new java.util.HashMap<>();
        Resultado r = new Resultado();
        if (adoptar) {
            String antes = c.colaTipo();
            boolean elegidoAntes = c.colaElegida();
            c.elegirCola(tipoOrigen, todo.colaMd5());
            r.bancoAdoptado = tipoOrigen;
            if (elegidoAntes) {
                // El texto es para el operador: nada de "pasos" ni de "colas".
                r.avisos.add("esta campaña pasa al banco " + tipoOrigen + ", que es con el que se midió el ZIP (iba "
                        + "con el " + antes + "). Así cuenta todo lo que trae y no hay que volver a medirlo");
            }
        }
        if (tipoOrigen != null && !pasosValen) {
            r.bancoPorResolver = tipoOrigen;
            // Texto para el operador: dice qué pasa, qué se pierde y qué hacer.
            r.avisos.add("el ZIP se midió con el banco " + tipoOrigen + " y esta campaña va con el banco "
                    + c.colaTipo() + ", en el que ya hay trabajo hecho. Las MEDIDAS del ZIP entran todas; lo que no "
                    + "entra es el AVANCE del banco, porque los dos bancos no llevan los mismos patrones ni en el "
                    + "mismo orden. Mientras sigan en bancos distintos, al calibrar saldrá \"no calibrable\"");
        }
        // QA-3615-02: el historial de series (RENOMBRA) viaja: otro telefono reconoce la serie nueva.
        r.renombrados = c.copiarRenombrados(todo.renombrados(), origen);
        compararFirmware(c, firmwares(todo.series()), r);
        for (Campana.Serie s : todo.series()) {
            if (vistas.contains(clave(s))) {
                r.yaEstaban++;
                String id = idPorClave.get(clave(s));
                nuevoId.put(s.id, id);
                // QA-3614-01: la serie ya estaba, pero en el origen se anulo despues: se anula aqui tambien.
                Campana.Serie aqui = c.serie(id);
                if (s.anulada != null && aqui != null && aqui.anulada == null) {
                    c.anular(aqui, s.anulada, s.anuladaFecha);
                    r.anuladas++;
                }
                continue;
            }
            Campana.Serie n = c.nuevaSerie(s.fecha, s.equipo, s.mac, s.firmware, s.patronOriginal, s.orientacion, s.codigo);
            nuevoId.put(s.id, n.id);
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
            if (s.anulada != null) {
                // La anulada se trae anulada, con su motivo y su fecha (QA-3614-07).
                c.anular(n, s.anulada, s.anuladaFecha);
                r.anuladas++;
            }
            r.series++;
        }
        // Elegidas, DESPUES de aplicar las anulaciones (QA-3614-01): si la elegida de aqui quedo anulada, manda la
        // elegida del origen; si aqui hay una elegida viva distinta, se mantiene la del operador y se avisa.
        for (Campana.Serie s : todo.series()) {
            if (todo.elegida(s.patron) != s) {
                continue;
            }
            Campana.Serie n = c.serie(nuevoId.get(s.id));
            Campana.Serie actual = c.elegida(s.patron);
            if (n == null || n == actual || n.anulada != null) {
                continue;
            }
            if (actual == null) {
                c.elegir(n);
            } else {
                r.avisos.add(s.patron + ": se mantiene la serie elegida por el operador (" + actual.id + ")");
            }
        }
        // QA-3610-09: se traen los PASO del banco (con la serie renumerada). No pisan un paso que ya
        // tenga estado aqui, salvo SALTADO frente a HECHO. BATERIA no se trae: una lectura de otro dia
        // no debe bloquear ni desbloquear las escrituras de hoy.
        Map<Integer, String> aqui = c.pasos();          // efectivo: HECHO con la serie anulada = REHACER
        Map<Integer, String> alli = pasosValen ? todo.pasos() : new java.util.HashMap<Integer, String>();
        for (Map.Entry<Integer, String> e : alli.entrySet()) {
            String est = aqui.get(e.getKey());
            boolean mejor = "HECHO".equals(e.getValue()) && ("SALTADO".equals(est) || "REHACER".equals(est));
            if (est == null || mejor || ("REHACER".equals(e.getValue()) && "HECHO".equals(est))) {
                String sid = todo.seriePaso(e.getKey());
                String nid = sid == null || sid.isEmpty() ? "" : nuevoId.get(sid);
                c.anotarPaso(e.getKey(), e.getValue(), nid == null ? "" : nid, "", "importado de " + origen);
                r.pasos++;
            }
        }
        return r;
    }

    /**
     * 3.6.15: la serie de lo importado es la del equipo si esta en el historial de la campana (RENOMBRA), o si
     * el propio diario importado la renombra a una serie del historial. La MAC se exige aparte, siempre.
     */
    static boolean mismaSerie(Campana c, Campana origen, String serie) {
        if (c.equipo.isEmpty() || c.esSerie(serie)) {
            return true;
        }
        if (origen != null && origen.esSerie(serie)) {
            for (String h : origen.historialSeries()) {
                if (c.esSerie(h)) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * RTV 1.0.0-rc6: guarda dura de familia. Si la campaña abierta y lo que viene son de familias distintas,
     * no entra nada, igual que con otra MAC. Una familia desconocida —un diario anterior a la rc6 que ni
     * siquiera diga el firmware— no bloquea: de no saber no se concluye nada.
     *
     * @throws IllegalArgumentException si no son compatibles.
     */
    static void comprobarFamilia(Campana c, String viene) {
        if (!Familia.compatibles(c.familia(), viene)) {
            throw new IllegalArgumentException(Familia.porQueNoSeMezclan(c.familia(), viene)
                    + " No se importa nada.");
        }
    }

    /** Familia de un conjunto de textos de firmware; la primera que se identifique. */
    static String familiaDe(Iterable<String> firmwares) {
        for (String f : firmwares) {
            String fam = Familia.de(f);
            if (!fam.isEmpty()) {
                return fam;
            }
        }
        return Familia.DESCONOCIDA;
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
            if (!c.esDeEsteEquipo(mac) || !mismaSerie(c, null, equipo)) {
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
        // RTV 1.0.0-rc6: la familia bloquea tambien por esta via (un campana.csv suelto). Antes de escribir nada.
        comprobarFamilia(c, familiaDe(fws));
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
