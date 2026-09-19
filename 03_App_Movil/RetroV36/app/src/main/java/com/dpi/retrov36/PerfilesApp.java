package com.dpi.retrov36;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Lado Android de los perfiles (RTV 1.0): equipos.csv y firmwares.csv de los assets, y los perfiles aprendidos
 * en un fichero local (perfiles_aprendidos.txt). La logica esta en {@link PerfilesEquipo}, {@link Deteccion} y
 * {@link PerfilFirmware}, que son Java puro.
 */
public final class PerfilesApp {

    private static final String FICHERO = "perfiles_aprendidos.txt";
    private static volatile PerfilesEquipo.Firmados firmados;
    private static volatile Context app;

    /** El contexto de la aplicacion guardado en iniciar() (para Pruebas, que no es una Activity). */
    public static Context app() {
        return app;
    }

    private PerfilesApp() { }

    private static String asset(Context ctx, String n) {
        try (InputStream in = ctx.getAssets().open(n)) {
            return new String(ImportadorCampana.leer(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** Carga equipos.csv y firmwares.csv (una vez por proceso). */
    public static synchronized void iniciar(Context ctx) {
        app = ctx.getApplicationContext();
        if (firmados != null) {
            return;
        }
        firmados = PerfilesEquipo.leerFirmados(asset(ctx, PerfilesEquipo.ASSET));
        for (String r : firmados.rechazados) {
            Registro.nota("equipos.csv: fila rechazada " + r);
        }
        PerfilFirmware.cargar(asset(ctx, PerfilFirmware.ASSET));
    }

    public static PerfilesEquipo.Perfil firmado(Context ctx, String mac) {
        iniciar(ctx);
        return firmados.de(mac);
    }

    private static String leerTexto(File f) throws IOException {
        try (InputStream in = new java.io.FileInputStream(f)) {
            return new String(ImportadorCampana.leer(in), StandardCharsets.UTF_8);
        }
    }

    private static File fichero(Context ctx) {
        return new File(ctx.getFilesDir(), FICHERO);
    }

    public static synchronized PerfilesEquipo.Aprendido aprendido(Context ctx, String mac) {
        File f = fichero(ctx);
        if (!f.exists() || mac == null) {
            return null;
        }
        try {
            String t = leerTexto(f);
            return PerfilesEquipo.leerAprendidos(t).get(mac.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IOException e) {
            return null;
        }
    }

    public static synchronized void guardar(Context ctx, PerfilesEquipo.Aprendido a) {
        File f = fichero(ctx);
        try {
            Map<String, PerfilesEquipo.Aprendido> m = PerfilesEquipo.leerAprendidos(
                    f.exists() ? leerTexto(f) : "");
            m.put(a.mac, a);
            try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) {
                o.write(PerfilesEquipo.escribirAprendidos(m).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            Registro.nota("no se pudo guardar el perfil aprendido: " + e.getMessage());
        }
    }
}
