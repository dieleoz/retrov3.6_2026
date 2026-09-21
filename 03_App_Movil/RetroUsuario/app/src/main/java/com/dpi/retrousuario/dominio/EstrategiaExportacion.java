package com.dpi.retrousuario.dominio;

/**
 * A4 (ALTO, revisión P16 de RetroUsuario 0.2.0): decide POR QUÉ VÍA se exporta el ZIP según el
 * {@code Build.VERSION.SDK_INT} del teléfono, sin tocar Android (probada en la JVM; la parte que sí
 * usa Android — {@code MediaStore.Downloads}, el permiso en tiempo de ejecución — vive fuera de
 * {@code dominio}, en la capa Android, que consulta esta decisión).
 *
 * <p><b>API 29 (Android 10, {@code Build.VERSION_CODES.Q}) en adelante:</b> almacenamiento con
 * ámbito; escribir un fichero directo en {@code Environment.getExternalStoragePublicDirectory} deja
 * de estar garantizado ({@code requestLegacyExternalStorage} del manifiesto sólo se respeta hasta
 * Android 10 si la app no apunta a 30, y esta app apunta a 30): la vía es
 * {@code MediaStore.Downloads}, con {@code RELATIVE_PATH = "Download/RetroUsuario"}.</p>
 *
 * <p><b>API 24-28:</b> el permiso {@code WRITE_EXTERNAL_STORAGE} sigue existiendo y hay que pedirlo
 * en tiempo de ejecución (concedido en la instalación sólo hasta API 22); la vía es un fichero
 * directo en {@code Download/RetroUsuario/}.</p>
 */
public final class EstrategiaExportacion {

    public enum Via { MEDIA_STORE, ARCHIVO_DIRECTO }

    /** {@code Build.VERSION_CODES.Q}: primer API que exige almacenamiento con ámbito para Descargas. */
    public static final int PRIMER_API_MEDIA_STORE = 29;

    private EstrategiaExportacion() {
    }

    public static Via paraApi(int sdkInt) {
        return sdkInt >= PRIMER_API_MEDIA_STORE ? Via.MEDIA_STORE : Via.ARCHIVO_DIRECTO;
    }
}
