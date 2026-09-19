package com.dpi.retrov36;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Registro de la sesion en un fichero de texto del almacenamiento propio de la
 * aplicacion (getFilesDir()/registros/).
 *
 * Se abre un fichero nuevo en cada conexion lograda, con cabecera: fecha,
 * modelo del movil, nombre y MAC del dispositivo y version de la aplicacion.
 *
 * Formato de cada evento:   t_ms;TX|RX;hex;ascii
 * Las notas (conexion, barrido, resumen) van en lineas que empiezan por '#',
 * para que las lineas TX/RX se puedan filtrar sin ambiguedad.
 *
 * t_ms son milisegundos desde la apertura del fichero, con reloj monotono
 * (SystemClock.elapsedRealtime): no le afecta un cambio de hora del movil.
 * Cada linea se vuelca al disco al escribirla, para no perder nada si la
 * aplicacion se cierra de golpe.
 */
public final class Registro {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final Object CERROJO = new Object();
    private static Writer escritor;
    private static File fichero;
    private static volatile long t0 = SystemClock.elapsedRealtime();

    private Registro() { }

    /** Milisegundos desde la apertura del registro actual. */
    public static long ahora() {
        return SystemClock.elapsedRealtime() - t0;
    }

    public static void abrir(Context ctx, String nombre, String mac) {
        synchronized (CERROJO) {
            cerrarSinBloqueo();
            File dir = new File(ctx.getFilesDir(), "registros");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return;
            }
            Date fecha = new Date();
            String sello = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(fecha);
            fichero = new File(dir, "rtv36_" + sello + ".txt");
            // Nunca se pisa ni se continua un registro anterior.
            for (int i = 2; fichero.exists(); i++) {
                fichero = new File(dir, "rtv36_" + sello + "_" + i + ".txt");
            }
            try {
                escritor = new OutputStreamWriter(new FileOutputStream(fichero, true), UTF8);
                t0 = SystemClock.elapsedRealtime();
                String version = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
                escribir("# RTV V3.6 - registro de tramas Bluetooth");
                escribir("# fecha: " + new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss Z", Locale.US).format(fecha));
                escribir("# movil: " + Build.MANUFACTURER + " " + Build.MODEL
                        + " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")");
                escribir("# dispositivo: " + nombre + " " + mac);
                escribir("# app: " + BuildConfig.APPLICATION_ID + " " + version);
                escribir("# formato: t_ms;TX|RX;hex;ascii  (t_ms desde la apertura;"
                        + " en ascii, '.' = no imprimible o ';')");
            } catch (IOException e) {
                escritor = null;
            }
        }
    }

    public static void tx(long t, byte[] datos) {
        evento(t, "TX", datos);
    }

    public static void rx(long t, byte[] datos) {
        evento(t, "RX", datos);
    }

    private static void evento(long t, String sentido, byte[] datos) {
        datos = Hex.ocultarPin(datos);
        synchronized (CERROJO) {
            escribir(t + ";" + sentido + ";" + Hex.hex(datos) + ";" + Hex.ascii(datos));
        }
    }

    public static void nota(String texto) {
        texto = texto.replaceAll("#([LP]),[0-9,]*#", "#$1,****#");
        synchronized (CERROJO) {
            long t = ahora();
            for (String linea : texto.split("\n")) {
                escribir("# " + t + " " + linea);
            }
        }
    }

    private static void escribir(String linea) {
        if (escritor == null) {
            return;
        }
        try {
            escritor.write(linea);
            escritor.write("\n");
            escritor.flush();
        } catch (IOException e) {
            // Disco lleno o fichero perdido: no hay a quien avisar desde aqui
            // sin bloquear la recepcion. Se deja de escribir.
            cerrarSinBloqueo();
        }
    }

    private static void cerrarSinBloqueo() {
        if (escritor != null) {
            try {
                escritor.close();
            } catch (IOException e) {
                // Nada que hacer: el fichero queda con lo ya volcado.
            }
        }
        escritor = null;
    }

    public static File ficheroActual() {
        synchronized (CERROJO) {
            return fichero;
        }
    }

    /**
     * Intent ACTION_SEND con el registro como adjunto text/plain via
     * FileProvider, listo para WhatsApp o correo. Null si aun no hay registro.
     */
    public static Intent intentCompartir(Context ctx) {
        File f = ficheroActual();
        if (f == null || !f.exists()) {
            return null;
        }
        Uri uri = FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".ficheros", f);
        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType("text/plain");
        envio.putExtra(Intent.EXTRA_STREAM, uri);
        envio.putExtra(Intent.EXTRA_SUBJECT, "RTV " + BuildConfig.VERSION_NAME + " - " + f.getName());
        envio.setClipData(ClipData.newRawUri(f.getName(), uri));
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(envio, "Compartir registro");
    }
}
