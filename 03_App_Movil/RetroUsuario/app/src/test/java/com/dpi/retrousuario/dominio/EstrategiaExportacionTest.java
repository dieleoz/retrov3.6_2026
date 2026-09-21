package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A4 (ALTO): API 24-28 escribe un fichero directo; API 29+ usa MediaStore.Downloads (almacenamiento
 * con ámbito). Fuente del corte: documentación de Android sobre almacenamiento con ámbito desde
 * Android 10 (API 29, {@code Build.VERSION_CODES.Q}), no deducida del propio código que se prueba.
 */
public class EstrategiaExportacionTest {

    @Test
    public void api24EsArchivoDirecto() {
        assertEquals(EstrategiaExportacion.Via.ARCHIVO_DIRECTO, EstrategiaExportacion.paraApi(24));
    }

    @Test
    public void api28EsArchivoDirecto() {
        assertEquals(EstrategiaExportacion.Via.ARCHIVO_DIRECTO, EstrategiaExportacion.paraApi(28));
    }

    @Test
    public void api29EsMediaStore() {
        assertEquals(EstrategiaExportacion.Via.MEDIA_STORE, EstrategiaExportacion.paraApi(29));
    }

    @Test
    public void api30EsMediaStore() {
        assertEquals(EstrategiaExportacion.Via.MEDIA_STORE, EstrategiaExportacion.paraApi(30));
    }
}
