package com.dpi.retrousuario;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.dpi.retrousuario.dominio.SesionMedicion;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;

/**
 * A4 (ALTO): vía {@code MediaStore.Downloads} de exportar (API 29+, almacenamiento con ámbito) —
 * fuera de {@code dominio} porque usa {@code ContentResolver}/{@code MediaStore}, Android puro.
 * {@link com.dpi.retrousuario.dominio.EstrategiaExportacion} decide CUÁNDO se usa esta vía (esa
 * decisión sí está probada en la JVM); esta clase sólo la ejecuta.
 */
final class ExportadorAndroid {

    private ExportadorAndroid() {
    }

    /** Inserta el ZIP en Descargas/RetroUsuario/ vía MediaStore; el sistema resuelve un nombre repetido. */
    static Uri exportarMediaStore(Context contexto, SesionMedicion sesion, Date instante) throws IOException {
        String nombre = sesion.nombreZipSugerido(instante, TimeZone.getDefault());
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre);
        valores.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        valores.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RetroUsuario");
        valores.put(MediaStore.Downloads.IS_PENDING, 1);

        ContentResolver resolver = contexto.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) {
            throw new IOException("No se pudo crear la entrada en Descargas para " + nombre);
        }
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("No se pudo abrir la salida de Descargas para " + nombre);
            }
            out.write(sesion.exportarBytes());
        } catch (IOException e) {
            resolver.delete(uri, null, null); // no deja una entrada a medias si falló la escritura.
            throw e;
        }
        ContentValues listo = new ContentValues();
        listo.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, listo, null, null);
        return uri;
    }
}
