package com.dpi.retrov36;

import android.content.Context;

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
            texto(z, "resumen.txt", "Campaña de calibración\nEquipo: serie " + serieAbierta + ", MAC " + macAbierta
                    + "\nFirmware: " + Sesion.get().firmware() + "\nFecha de exportación: " + Sesion.ahoraIso()
                    + "\n\nEste ZIP es el único envío: lleva las series (campana.csv), las pruebas del equipo "
                    + "(pruebas.txt), todos los registros de tramas de la campaña (tramas/) y el diario.\n\n"
                    + c.resumen());
            fichero(z, "diario_" + fichero.getName(), fichero);
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
