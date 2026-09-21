package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * RF-USR-15 bis (nuevo r6, C4): serie saneada a [A-Za-z0-9_-], sin "..", "SIN_SERIE" si no hay
 * serie. Ficha que fija comportamiento de este trabajo (nombre y ruta), sin cita de Diego.
 */
public class NombreZipTest {

    @Test
    public void caracteresFueraDelJuegoPasanAGuionBajo() {
        assertEquals("SLV_002_A", NombreZip.sanear("SLV;002\"A"));
    }

    @Test
    public void secuenciasDobleDobleSeEliminan() {
        assertEquals("SLV002", NombreZip.sanear("..SLV..002.."));
    }

    @Test
    public void sinSerieDaElLiteral() {
        assertEquals("SIN_SERIE", NombreZip.sanear(""));
        assertEquals("SIN_SERIE", NombreZip.sanear(null));
    }
}
