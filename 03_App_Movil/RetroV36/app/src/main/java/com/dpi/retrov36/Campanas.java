package com.dpi.retrov36;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * La campana abierta y su fichero: files/campanas/campana_<serie>_<mac>.csv
 * (diario de solo anadir). Una campana por EQUIPO (serie + MAC): la optica
 * cambia de un equipo a otro y nunca se mezclan series. Se retoma al volver a
 * abrirla. Las pruebas del equipo hechas con la campana abierta se anotan en
 * pruebas_<serie>_<mac>.txt y van en el ZIP.
 */
public final class Campanas {

    private static Campana abierta;
    private static String claveAbierta;
    private static String serieAbierta;
    private static String macAbierta;
    private static Context app;
    private static File fichero;
    private static Writer escritor;
    private static long inicioMs;
    private static int lineasMalas;

    private Campanas() { }

    private static String limpio(String s) {
        String r = s == null ? "" : s.replaceAll("[^A-Za-z0-9-]", "");
        return r.isEmpty() ? "equipo" : r;
    }

    public static synchronized void iniciar(Context ctx) {
        app = ctx.getApplicationContext();
    }

    private static String clave(String serie, String mac) {
        return limpio(serie) + "_" + limpio(mac);
    }

    public static synchronized Campana abrir(Context ctx, String serie, String mac) throws IOException {
        if (serie == null || serie.trim().isEmpty() || mac == null || mac.trim().isEmpty()) {
            throw new IOException("equipo sin identificar (serie y MAC): no se abre campaña");
        }
        app = ctx.getApplicationContext();
        File dir = new File(ctx.getFilesDir(), "campanas");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        String k = clave(serie, mac);
        if (!new File(dir, "campana_" + k + ".csv").exists()) {
            // 3.6.15: el equipo se renombro (RENOMBRA): su campana sigue siendo la de la serie anterior, misma MAC.
            String alias = claveRenombrada(dir, serie.trim(), mac.trim());
            if (alias != null) {
                k = alias;
            }
        }
        if (abierta != null && k.equals(claveAbierta)) {
            return abierta;
        }
        cerrar();
        fichero = new File(dir, "campana_" + k + ".csv");
        Campana c = new Campana(Sesion.get().patrones(ctx), serie.trim(), mac.trim());
        inicioMs = System.currentTimeMillis();
        lineasMalas = 0;
        if (fichero.exists()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fichero),
                    StandardCharsets.UTF_8))) {
                String primera = r.readLine();
                if (primera != null && primera.startsWith("# inicio_ms,")) {
                    try {
                        inicioMs = Long.parseLong(primera.substring("# inicio_ms,".length()).trim());
                    } catch (NumberFormatException e) {
                        // se queda con ahora
                    }
                }
            }
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(fichero), StandardCharsets.UTF_8)) {
                lineasMalas = c.leerDiario(r);
            }
        }
        boolean nuevo = !fichero.exists();
        escritor = new OutputStreamWriter(new FileOutputStream(fichero, true), StandardCharsets.UTF_8);
        if (nuevo) {
            escritor.write("# inicio_ms," + inicioMs + "\n");
            escritor.write(Campana.CABECERA_DIARIO + "\n");
            escritor.flush();
        }
        c.escribirEn(escritor);
        abierta = c;
        claveAbierta = k;
        serieAbierta = serie.trim();
        macAbierta = mac.trim();
        Registro.nota("campana abierta: " + fichero.getName() + ", " + c.series().size() + " series"
                + (lineasMalas > 0 ? ", " + lineasMalas + " lineas del diario no entendidas" : ""));
        return c;
    }

    /** RF-APP-49: true si este equipo (MAC) tiene otra campana ademas de la abierta (su primer banco ya se hizo). */
    public static synchronized boolean hayOtraCampanaDeEsteEquipo(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "campanas");
        File[] fs = dir.listFiles();
        if (fs == null || macAbierta == null) {
            return false;
        }
        String fin = "_" + limpio(macAbierta) + ".csv";
        for (File f : fs) {
            String n = f.getName();
            if (n.startsWith("campana_") && n.endsWith(fin) && !n.equals("campana_" + claveAbierta + ".csv")) {
                return true;
            }
        }
        return false;
    }

    /** Clave de una campana de esta MAC cuyo diario renombra el equipo a 'serie'; null si ninguna. */
    static String claveRenombrada(File dir, String serie, String mac) throws IOException {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return null;
        }
        String fin = "_" + limpio(mac) + ".csv";
        for (File f : fs) {
            String n = f.getName();
            if (!n.startsWith("campana_") || !n.endsWith(fin)) {
                continue;
            }
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                if (Campana.diarioRenombraA(r, serie)) {
                    return n.substring("campana_".length(), n.length() - ".csv".length());
                }
            }
        }
        return null;
    }

    public static synchronized Campana abierta() {
        return abierta;
    }

    public static synchronized int lineasMalas() {
        return lineasMalas;
    }

    private static void cerrar() {
        if (escritor != null) {
            try {
                escritor.close();
            } catch (IOException e) {
                // ya volcado linea a linea
            }
        }
        escritor = null;
        abierta = null;
        claveAbierta = null;
    }

    private static File ficheroPruebas() {
        if (app == null || claveAbierta == null) {
            return null;
        }
        return new File(new File(app.getFilesDir(), "campanas"), "pruebas_" + claveAbierta + ".txt");
    }

    /** Anota el resultado de "Pruebas del equipo" en la campana abierta (si la hay y es de este equipo). */
    public static synchronized void anotarPruebas(String mac, String texto) {
        if (abierta == null || !abierta.esDeEsteEquipo(mac)) {
            return;
        }
        File f = ficheroPruebas();
        if (f == null) {
            return;
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8)) {
            w.write(texto);
            w.write("\n\n");
        } catch (IOException e) {
            Registro.nota("no se pudo anotar las pruebas en la campaña: " + e.getMessage());
        }
    }

    /**
     * Copia del ZIP fuera de la app, en Download/RTV/ (RF-APP-40): sobrevive a desinstalar.
     * API 29+: MediaStore.Downloads; API 24-28: carpeta publica (permiso WRITE_EXTERNAL_STORAGE).
     * @return la ruta legible de la copia.
     */
    public static String copiarADescargas(Context ctx, File zip) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, zip.getName());
            v.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RTV");
            Uri u = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) {
                throw new IOException("MediaStore no creó la copia en Download/RTV/");
            }
            try (java.io.OutputStream o = ctx.getContentResolver().openOutputStream(u);
                 FileInputStream in = new FileInputStream(zip)) {
                if (o == null) {
                    throw new IOException("no se pudo abrir Download/RTV/" + zip.getName());
                }
                copiar(in, o);
            }
        } else {
            File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RTV");
            if (!d.isDirectory() && !d.mkdirs()) {
                throw new IOException("no se pudo crear " + d + " (¿permiso de almacenamiento?)");
            }
            try (FileInputStream in = new FileInputStream(zip);
                 FileOutputStream o = new FileOutputStream(new File(d, zip.getName()))) {
                copiar(in, o);
            }
        }
        return "Download/RTV/" + zip.getName();
    }

    private static void copiar(java.io.InputStream in, java.io.OutputStream o) throws IOException {
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            o.write(b, 0, n);
        }
    }

    /** Series sin exportar en todas las campanas del telefono (aviso de no desinstalar). */
    public static synchronized int seriesSinExportarTodas(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "campanas");
        File[] fs = dir.listFiles();
        if (fs == null) {
            return 0;
        }
        int total = 0;
        for (File f : fs) {
            if (!f.getName().startsWith("campana_") || !f.getName().endsWith(".csv") || f.getName().contains("_archivada_")) {
                continue;
            }
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                Campana c = new Campana(Sesion.get().patrones(ctx));
                c.leerDiario(r);
                total += c.seriesSinExportar();
            } catch (IOException | RuntimeException e) {
                // un diario ilegible no impide el aviso de los demas
            }
        }
        return total;
    }

    // ----------------------------------------------------------- acta en disco (RF-APP-36)

    private static File ficheroActaEnCurso(Context ctx) {
        Sesion s = Sesion.get();
        String k = claveAbierta != null ? claveAbierta : clave(s.serie(), s.mac);
        return new File(new File(ctx.getFilesDir(), "campanas"), "acta_" + k + "_curso.csv");
    }

    private static Writer escritorActa;

    /**
     * El acta a medias de este equipo, leida de disco (reconectar no la borra). La deja en
     * Sesion.acta y sigue escribiendo en su diario. null si no hay ninguna abierta.
     */
    public static synchronized Acta actaEnCurso(Context ctx) throws IOException {
        Sesion s = Sesion.get();
        if (s.acta != null && !s.acta.cerrada()) {
            return s.acta;
        }
        File f = ficheroActaEnCurso(ctx);
        if (!f.exists()) {
            return null;
        }
        Acta a;
        try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            a = Acta.leer(r);
        }
        if (a == null || a.cerrada() || !a.mac.equalsIgnoreCase(s.mac)) {
            return null;
        }
        cerrarEscritorActa();
        escritorActa = new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8);
        a.continuarEn(escritorActa);
        s.acta = a;
        Registro.nota("acta a medias recuperada de " + f.getName());
        return a;
    }

    /** Un acta nueva pasa a vivir en disco: ABRE en su diario (sustituye al acta en curso anterior). */
    public static synchronized void adjuntarActa(Context ctx, Acta a) throws IOException {
        File f = ficheroActaEnCurso(ctx);
        File dir = f.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        if (f.exists()) {
            archivarActa(f);
        }
        cerrarEscritorActa();
        escritorActa = new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8);
        a.escribirEn(escritorActa);
        Sesion.get().acta = a;
    }

    /** Al cerrar (aceptada o rechazada): el diario se archiva con fecha y se guarda el texto. */
    public static synchronized File cerrarActaEnDisco(Context ctx, Acta a) throws IOException {
        File f = ficheroActaEnCurso(ctx);
        cerrarEscritorActa();
        if (f.exists()) {
            archivarActa(f);
        }
        return guardarActa(ctx, "App RTV " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n" + a.texto());
    }

    /**
     * P12 §6.3: true si entre las actas archivadas de este equipo (diarios acta_<clave>_*_diario.csv) hay una
     * ACEPTADA con el codigo k conforme y no restaurado.
     */
    public static synchronized boolean aceptadoAntes(Context ctx, char k) throws IOException {
        File curso = ficheroActaEnCurso(ctx);
        File dir = curso.getParentFile();
        String prefijo = curso.getName().replace("_curso.csv", "_");
        File[] fs = dir == null ? null : dir.listFiles();
        if (fs == null) {
            return false;
        }
        for (File f : fs) {
            if (!f.getName().startsWith(prefijo) || !f.getName().endsWith("_diario.csv")) {
                continue;
            }
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                Acta a = Acta.leer(r);
                Acta.Codigo c = a == null ? null : a.codigo(k);
                if (a != null && a.aceptada() && c != null && c.conforme() && c.restaurado == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void archivarActa(File f) throws IOException {
        String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dest = new File(f.getParentFile(), f.getName().replace("_curso.csv", "_" + sello + "_diario.csv"));
        if (!f.renameTo(dest)) {
            throw new IOException("no se pudo archivar " + f.getName());
        }
    }

    private static void cerrarEscritorActa() {
        if (escritorActa != null) {
            try {
                escritorActa.close();
            } catch (IOException e) {
                // ya volcado linea a linea
            }
            escritorActa = null;
        }
    }

    /** Guarda un acta de calibracion (nunca pisa otra); va en el ZIP de la campana de su equipo. */
    public static synchronized File guardarActa(Context ctx, String texto) throws IOException {
        Sesion s = Sesion.get();
        File dir = new File(ctx.getFilesDir(), "campanas");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File f = new File(dir, "acta_" + clave(s.serie(), s.mac) + "_" + sello + ".txt");
        for (int i = 2; f.exists(); i++) {
            f = new File(dir, "acta_" + clave(s.serie(), s.mac) + "_" + sello + "_" + i + ".txt");
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(texto);
        }
        return f;
    }

    /** Archiva la campana actual (se renombra, no se borra) y empieza otra. */
    public static synchronized void nueva(Context ctx, String serie, String mac) throws IOException {
        Campana c = abrir(ctx, serie, mac);
        File f = fichero;
        cerrar();
        if (c != null && f != null && f.exists()) {
            String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File dest = new File(f.getParentFile(), f.getName().replace(".csv", "_archivada_" + sello + ".csv"));
            if (!f.renameTo(dest)) {
                throw new IOException("no se pudo archivar " + f.getName());
            }
            Registro.nota("campana archivada como " + dest.getName());
        }
        abrir(ctx, serie, mac);
    }

    /**
     * Un unico ZIP con el CSV de todas las series, el diario, resumen.txt y los
     * registros de tramas escritos desde el inicio de la campana.
     */
    /** ZIP exportado y sus huellas. */
    public static final class Exportacion {
        public final File zip;
        public final String md5;
        public final String sha256;
        /** Ruta de la copia en Download/RTV/, o el motivo si no se pudo. */
        public final String copia;

        Exportacion(File zip, String md5, String sha256, String copia) {
            this.zip = zip;
            this.md5 = md5;
            this.sha256 = sha256;
            this.copia = copia;
        }
    }

    /** "Cerrar campana": la deja de solo lectura (evento CIERRE en el diario). */
    public static synchronized void cerrarCampana(Context ctx, String serie, String mac) throws IOException {
        abrir(ctx, serie, mac).cerrarCampana(Sesion.ahoraIso());
        Registro.nota("campana cerrada (solo lectura)");
    }

    public static synchronized Exportacion exportarConHuellas(Context ctx, String serie, String mac) throws IOException {
        File zip = exportar(ctx, serie, mac);
        String md5 = Resumen.hex(zip, "MD5");
        String sha = Resumen.hex(zip, "SHA-256");
        // El ZIP no puede llevar su propia huella: se anota en el diario (sale en el
        // resumen de la siguiente exportacion), en el registro y en el texto del envio.
        abierta.anotarExportacion(Sesion.ahoraIso(), zip.getName(), md5, sha);
        String copia;
        try {
            copia = copiarADescargas(ctx, zip);
        } catch (IOException | RuntimeException e) {
            copia = "SIN COPIA en Download/RTV/: " + e.getMessage();
        }
        Registro.nota("campana exportada: " + zip.getName() + " md5 " + md5 + " sha256 " + sha + "; " + copia);
        return new Exportacion(zip, md5, sha, copia);
    }

    /** Lee un fichero entero. */
    private static byte[] leerTodo(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            return ImportadorCampana.leer(in);
        }
    }

    /**
     * Piezas de la campana abierta. completo = false: las del ZIP ligero (diario, actas, pruebas; RF-APP-50);
     * true: todo lo de soporte (tambien campana.csv y las tramas; RF-APP-51). Sin resumen.txt.
     */
    private static List<PaquetesZip.Pieza> piezas(Campana c, boolean completo) throws IOException {
        List<PaquetesZip.Pieza> l = new ArrayList<>();
        l.add(new PaquetesZip.Pieza("diario_" + fichero.getName(), leerTodo(fichero)));
        File[] actas = fichero.getParentFile().listFiles();
        if (actas != null) {
            java.util.Arrays.sort(actas);
            for (File a : actas) {
                if (a.getName().startsWith("acta_" + claveAbierta + "_")) {
                    l.add(new PaquetesZip.Pieza("actas/" + a.getName(), leerTodo(a)));
                }
            }
        }
        File fp = ficheroPruebas();
        l.add(new PaquetesZip.Pieza("pruebas.txt", fp != null && fp.exists() ? leerTodo(fp)
                : PaquetesZip.utf8("No se hicieron \"Pruebas del equipo\" con esta campaña abierta.\n")));
        if (completo) {
            l.add(new PaquetesZip.Pieza("campana.csv", PaquetesZip.utf8(c.exportarCsv())));
            File dir = new File(app.getFilesDir(), "registros");
            File[] logs = dir.listFiles();
            if (logs != null) {
                java.util.Arrays.sort(logs);
                for (File f : logs) {
                    if (f.getName().startsWith("rtv36_") && f.getName().endsWith(".txt")
                            && f.lastModified() >= inicioMs - 60_000) {
                        l.add(new PaquetesZip.Pieza("tramas/" + f.getName(), leerTodo(f)));
                    }
                }
            }
        }
        return l;
    }

    private static String cabeceraResumen(Campana c, String que) {
        return "Campaña de calibración - app RTV " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + que + "\nEquipo: serie " + c.serieConHistoria() + ", MAC " + macAbierta
                + "\nFirmware: " + Sesion.get().firmware() + "\n" + Sesion.get().datosCalibracion()
                + "\nBanco: " + c.colaTipo() + "\nFecha de exportación: " + Sesion.ahoraIso() + "\n\n";
    }

    /**
     * ZIP ligero e incremental (RF-APP-50, 52 y 53): solo lo nuevo desde la exportacion anterior de este equipo,
     * con indice.sha256 y resumen.txt (siempre). Sin tramas ni campana.csv: van en el ZIP de soporte.
     */
    public static synchronized File exportar(Context ctx, String serie, String mac) throws IOException {
        Campana c = abrir(ctx, serie, mac);
        File dir = new File(ctx.getFilesDir(), "registros");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File zip = new File(dir, "campana_" + limpio(c.serieActual()) + "_" + sello + ".zip");
        for (int i = 2; zip.exists(); i++) {
            zip = new File(dir, "campana_" + limpio(c.serieActual()) + "_" + sello + "_" + i + ".zip");
        }
        File est = new File(fichero.getParentFile(), "indice_" + claveAbierta + ".txt");
        String estado = est.exists() ? new String(leerTodo(est), StandardCharsets.UTF_8) : "";
        String zipAnt = "";
        String shaAnt = "";
        if (estado.startsWith("zip ")) {
            String[] cab = estado.substring(0, estado.indexOf('\n')).split(" ");
            zipAnt = cab.length > 1 ? cab[1] : "";
            shaAnt = cab.length > 2 ? cab[2] : "";
            estado = estado.substring(estado.indexOf('\n') + 1);
        }
        PaquetesZip.Incremental inc = PaquetesZip.incremental(piezas(c, false), PaquetesZip.leerEstado(estado), zipAnt, shaAnt);
        List<PaquetesZip.Pieza> salida = new ArrayList<>(inc.piezas);
        salida.add(new PaquetesZip.Pieza("indice.sha256", PaquetesZip.utf8(inc.indice)));
        String resumen = cabeceraResumen(c, "ZIP LIGERO E INCREMENTAL: lo nuevo desde " + (zipAnt.isEmpty() ? "el principio" : zipAnt)
                + ". Las tramas y campana.csv van en el ZIP de soporte.") + c.resumen() + PaquetesZip.listaHashes(salida);
        salida.add(0, new PaquetesZip.Pieza("resumen.txt", PaquetesZip.utf8(resumen)));
        try (FileOutputStream out = new FileOutputStream(zip)) {
            PaquetesZip.escribir(out, salida);
        }
        String sha = PaquetesZip.sha256(leerTodo(zip));
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(est), StandardCharsets.UTF_8)) {
            w.write("zip " + zip.getName() + " " + sha + "\n" + PaquetesZip.estadoTexto(inc.estado));
        }
        File dirInd = new File(fichero.getParentFile(), "indices_" + claveAbierta);
        if (dirInd.isDirectory() || dirInd.mkdirs()) {
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(new File(dirInd,
                    zip.getName().replace(".zip", ".sha256"))), StandardCharsets.UTF_8)) {
                w.write(inc.indice);
            }
        }
        Registro.nota("campana exportada (incremental): " + zip.getName());
        return zip;
    }

    /**
     * ZIP de soporte (RF-APP-51): todo lo de hoy entero (tramas, campana.csv, diario, pruebas, actas y resumen) y
     * los indices de todos los incrementales. Se comparte aparte y se copia en Download/RTV/. Es el que se importa.
     */
    public static synchronized Exportacion exportarSoporte(Context ctx, String serie, String mac) throws IOException {
        Campana c = abrir(ctx, serie, mac);
        File dir = new File(ctx.getFilesDir(), "registros");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File zip = new File(dir, "soporte_" + limpio(c.serieActual()) + "_" + sello + ".zip");
        List<PaquetesZip.Pieza> salida = piezas(c, true);
        File dirInd = new File(fichero.getParentFile(), "indices_" + claveAbierta);
        File[] inds = dirInd.listFiles();
        if (inds != null) {
            java.util.Arrays.sort(inds);
            for (File f : inds) {
                salida.add(new PaquetesZip.Pieza("indices/" + f.getName(), leerTodo(f)));
            }
        }
        String resumen = cabeceraResumen(c, "ZIP DE SOPORTE: todo, entero (es el que se importa en otro teléfono).")
                + c.resumen() + PaquetesZip.listaHashes(salida);
        salida.add(0, new PaquetesZip.Pieza("resumen.txt", PaquetesZip.utf8(resumen)));
        try (FileOutputStream out = new FileOutputStream(zip)) {
            PaquetesZip.escribir(out, salida);
        }
        String md5 = Resumen.hex(zip, "MD5");
        String sha = Resumen.hex(zip, "SHA-256");
        String copia;
        try {
            copia = copiarADescargas(ctx, zip);
        } catch (IOException | RuntimeException e) {
            copia = "SIN COPIA en Download/RTV/: " + e.getMessage();
        }
        Registro.nota("ZIP de soporte: " + zip.getName() + " md5 " + md5 + " sha256 " + sha + "; " + copia);
        return new Exportacion(zip, md5, sha, copia);
    }

    private static void texto(ZipOutputStream z, String nombre, String contenido) throws IOException {
        z.putNextEntry(new ZipEntry(nombre));
        z.write(contenido.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    private static void fichero(ZipOutputStream z, String nombre, File f) throws IOException {
        z.putNextEntry(new ZipEntry(nombre));
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                z.write(b, 0, n);
            }
        }
        z.closeEntry();
    }
}
