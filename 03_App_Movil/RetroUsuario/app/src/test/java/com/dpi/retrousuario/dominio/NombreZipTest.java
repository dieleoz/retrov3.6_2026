package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    /** QA-4: sin colisión, el nombre base tal cual. */
    @Test
    public void sinColisionDaElNombreBase() {
        assertEquals("RTVU_SLV002_20260921-100000.zip",
                NombreZip.resolverColisionEntreNombres("RTVU_SLV002_20260921-100000", Collections.emptySet()));
    }

    /** QA-4: con el nombre base ya ocupado, sufijo "_2" (no "(1)", que es lo que MediaStore añadiría solo). */
    @Test
    public void conColisionDaElSufijoGuionBajo2() {
        Set<String> existentes = new HashSet<>();
        existentes.add("RTVU_SLV002_20260921-100000.zip");
        assertEquals("RTVU_SLV002_20260921-100000_2.zip",
                NombreZip.resolverColisionEntreNombres("RTVU_SLV002_20260921-100000", existentes));
    }

    /** QA-4: con "_2" también ocupado, sigue a "_3". */
    @Test
    public void conDosColisionesDaElSufijoGuionBajo3() {
        Set<String> existentes = new HashSet<>();
        existentes.add("RTVU_SLV002_20260921-100000.zip");
        existentes.add("RTVU_SLV002_20260921-100000_2.zip");
        assertEquals("RTVU_SLV002_20260921-100000_3.zip",
                NombreZip.resolverColisionEntreNombres("RTVU_SLV002_20260921-100000", existentes));
    }
}
