package com.dpi.retrousuario;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.dpi.retrousuario.dominio.SesionMedicion;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * A4 (ALTO): vía {@code MediaStore.Downloads} de exportar (API 29+, almacenamiento con ámbito) —
 * fuera de {@code dominio} porque usa {@code ContentResolver}/{@code MediaStore}, Android puro.
 * {@link com.dpi.retrousuario.dominio.EstrategiaExportacion} decide CUÁNDO se usa esta vía (esa
 * decisión sí está probada en la JVM); esta clase sólo la ejecuta.
 */
final class ExportadorAndroid {

    private static final String RUTA_RELATIVA = Environment.DIRECTORY_DOWNLOADS + "/RetroUsuario/";

    private ExportadorAndroid() {
    }

    /**
     * Inserta el ZIP en Descargas/RetroUsuario/ vía MediaStore.
     *
     * <p>QA-4 (corrige 32c785d): consulta el {@code ContentResolver} ANTES de insertar para conocer
     * los {@code DISPLAY_NAME} ya usados en esa carpeta, y resuelve la colisión con el sufijo
     * {@code _2}, {@code _3}... (SPEC-App-Usuario-V3.6.md :279-280) — antes se dejaba que MediaStore
     * resolviera solo un nombre repetido, que añade "(1)", "(2)", no el esquema que pide la SPEC.</p>
     */
    static Uri exportarMediaStore(Context contexto, SesionMedicion sesion, Date instante) throws IOException {
        ContentResolver resolver = contexto.getContentResolver();
        Set<String> nombresExistentes = nombresExistentesEnCarpeta(resolver);
        String nombre = sesion.nombreZipResuelto(instante, TimeZone.getDefault(), nombresExistentes);

        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre);
        valores.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        valores.put(MediaStore.Downloads.RELATIVE_PATH, RUTA_RELATIVA);
        valores.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) {
            throw new IOException("No se pudo crear la entrada en Descargas para " + nombre);
        }
        escribirYMarcarListo(resolver, uri, sesion, nombre);
        return uri;
    }

    /**
     * B-2 (condición QA-8, corrige 32c785d): limpia la entrada {@code IS_PENDING} ante CUALQUIER
     * excepción de la escritura (antes sólo capturaba {@link IOException}: una {@link RuntimeException}
     * de {@code sesion.exportarBytes()}, p. ej. del ZIP, dejaba una entrada a medias en Descargas sin
     * borrarla).
     */
    private static void escribirYMarcarListo(ContentResolver resolver, Uri uri, SesionMedicion sesion, String nombre)
            throws IOException {
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("No se pudo abrir la salida de Descargas para " + nombre);
            }
            out.write(sesion.exportarBytes());
        } catch (RuntimeException | IOException fallo) {
            resolver.delete(uri, null, null); // B-2: no deja una entrada a medias, sea cual sea la excepción.
            if (fallo instanceof IOException) {
                throw (IOException) fallo;
            }
            throw new IOException("Fallo exportando a Descargas: " + nombre, fallo);
        }
        ContentValues listo = new ContentValues();
        listo.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, listo, null, null);
    }

    /** QA-4: los {@code DISPLAY_NAME} ya presentes en Descargas/RetroUsuario/, para resolver la colisión. */
    private static Set<String> nombresExistentesEnCarpeta(ContentResolver resolver) {
        Set<String> nombres = new HashSet<>();
        String[] proyeccion = { MediaStore.Downloads.DISPLAY_NAME };
        String seleccion = MediaStore.Downloads.RELATIVE_PATH + "=?";
        String[] argumentos = { RUTA_RELATIVA };
        try (Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, proyeccion, seleccion,
                argumentos, null)) {
            if (c != null) {
                int idx = c.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME);
                while (idx >= 0 && c.moveToNext()) {
                    nombres.add(c.getString(idx));
                }
            }
        }
        return nombres;
    }
}
