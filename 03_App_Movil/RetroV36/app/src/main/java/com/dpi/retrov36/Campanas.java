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
        String k = clave(serie, mac);
        if (abierta != null && k.equals(claveAbierta)) {
            return abierta;
        }
        cerrar();
        File dir = new File(ctx.getFilesDir(), "campanas");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
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

    public static synchronized File exportar(Context ctx, String serie, String mac) throws IOException {
        Campana c = abrir(ctx, serie, mac);
        File dir = new File(ctx.getFilesDir(), "registros");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("no se pudo crear " + dir);
        }
        String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File zip = new File(dir, "campana_" + limpio(serie) + "_" + sello + ".zip");
        for (int i = 2; zip.exists(); i++) {
            zip = new File(dir, "campana_" + limpio(serie) + "_" + sello + "_" + i + ".zip");
        }
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip))) {
            texto(z, "campana.csv", c.exportarCsv());
            texto(z, "resumen.txt", "Campaña de calibración - app RTV " + BuildConfig.VERSION_NAME + " ("
                    + BuildConfig.VERSION_CODE + ")\nEquipo: serie " + serieAbierta + ", MAC " + macAbierta
                    + "\nFirmware: " + Sesion.get().firmware() + "\n" + Sesion.get().datosCalibracion()
                    + "\nFecha de exportación: " + Sesion.ahoraIso()
                    + "\n\nEste ZIP es el único envío: lleva las series (campana.csv), las pruebas del equipo "
                    + "(pruebas.txt), todos los registros de tramas de la campaña (tramas/) y el diario.\n\n"
                    + c.resumen());
            fichero(z, "diario_" + fichero.getName(), fichero);
            File[] actas = fichero.getParentFile().listFiles();
            if (actas != null) {
                for (File a : actas) {
                    if (a.getName().startsWith("acta_" + claveAbierta + "_")) {
                        fichero(z, "actas/" + a.getName(), a);
                    }
                }
            }
            File fp = ficheroPruebas();
            if (fp != null && fp.exists()) {
                fichero(z, "pruebas.txt", fp);
            } else {
                texto(z, "pruebas.txt", "No se hicieron \"Pruebas del equipo\" con esta campaña abierta.\n");
            }
            File[] logs = dir.listFiles();
            if (logs != null) {
                for (File f : logs) {
                    if (f.getName().startsWith("rtv36_") && f.getName().endsWith(".txt")
                            && f.lastModified() >= inicioMs - 60_000) {
                        fichero(z, "tramas/" + f.getName(), f);
                    }
                }
            }
        }
        Registro.nota("campana exportada: " + zip.getName());
        return zip;
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
