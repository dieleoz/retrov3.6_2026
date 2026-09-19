package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** App 3.6.7: deteccion 3.6.1/3.6.2, serie y fecha de calibracion, K x M. */
public class Version367Test {

    private static List<Patron> catalogo() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/test/resources/patrones_P1-P50_hasta_3.6.9.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    @Test
    public void deteccion361Frente362() {
        assertEquals(Calibracion.Variante.V362, Calibracion.variante("#GC,2026-09-19#"));
        assertEquals(Calibracion.Variante.V362, Calibracion.variante("#GC,NONE#"));
        assertEquals(Calibracion.Variante.V361, Calibracion.variante("#ERR,FORMATO#"));
        assertEquals(Calibracion.Variante.DESCONOCIDA, Calibracion.variante(null));          // timeout
        assertEquals(Calibracion.Variante.DESCONOCIDA, Calibracion.variante("#ERR,BLOQUEADO#"));
        assertEquals("2026-09-19", Calibracion.fechaDe("#GC,2026-09-19#"));
        assertEquals("NONE", Calibracion.fechaDe("#GC,NONE#"));
        assertNull(Calibracion.fechaDe("#GC,2026-02-30#"));
        assertEquals("SLV-002", Calibracion.serieDe("#GN,SLV-002#"));
    }

    @Test
    public void vencimientoMasUnAnoY29DeFebrero() {
        assertEquals("2027-09-19", Calibracion.vencimiento("2026-09-19"));
        // 29 de febrero: vence el 28 de febrero del ano siguiente (no el 1 de marzo).
        assertEquals("2029-02-28", Calibracion.vencimiento("2028-02-29"));
        assertEquals("2033-02-28", Calibracion.vencimiento("2032-02-29"));
        assertEquals("2029-03-01", Calibracion.vencimiento("2028-03-01"));
    }

    @Test
    public void estadoDelEquipo() {
        assertEquals("Sin fecha", Calibracion.estado("NONE", "CAL", "2026-09-19"));
        assertTrue(Calibracion.estado("2026-09-19", "DEF", "2026-09-20").startsWith("No calibrado"));
        assertTrue(Calibracion.estado("2025-09-18", "CAL", "2026-09-19").startsWith("Calibración vencida"));
        assertTrue(Calibracion.estado("2025-09-19", "CAL", "2026-09-19").startsWith("Calibrado"));  // el dia del vencimiento vale
        assertTrue(Calibracion.estado(null, "CAL", "2026-09-19").contains("no disponible"));
    }

    @Test
    public void serieGrabable() {
        assertNull(Calibracion.motivoSerieInvalida("SLV-002"));
        assertNull(Calibracion.motivoSerieInvalida("ABCDEFGHIJKL"));
        assertNotNull(Calibracion.motivoSerieInvalida("ABCDEFGHIJKLM"));
        assertNotNull(Calibracion.motivoSerieInvalida(""));
        assertNotNull(Calibracion.motivoSerieInvalida("SLV#2"));
        assertNotNull(Calibracion.motivoSerieInvalida("SLV,2"));
        assertNotNull(Calibracion.motivoSerieInvalida("NONE"));
        assertNotNull(Calibracion.motivoSerieInvalida("SLÑ"));
    }

    @Test
    public void colocacionesKPorMAgregacion() {
        // 3 colocaciones x 3 disparos: medias 1000, 1030 y 1015.
        List<double[]> g = Arrays.asList(new double[]{999, 1000, 1001}, new double[]{1029, 1030, 1031},
                new double[]{1014, 1015, 1016});
        Patron p = new Patron("P2", 414, "IV", "blanco");
        Veredicto.Resultado v = Veredicto.evaluarColocaciones(g, p, new java.util.LinkedHashMap<Patron, Double>(),
                0.03, 0.03);
        assertEquals(1015, v.media, 1e-9);
        assertEquals(15, Veredicto.sEntre(g), 1e-9);          // s de 1000, 1030, 1015
        assertEquals(1, Veredicto.sDentro(g), 1e-9);
        assertEquals(9, v.n);
        assertEquals("OK", v.veredicto);                     // 15/1015 = 1,5 % < 3 %
        // Reproducibilidad del 4 %: repetir.
        List<double[]> mal = Arrays.asList(new double[]{1000, 1000, 1001}, new double[]{1080, 1081, 1080},
                new double[]{1040, 1040, 1041});
        assertEquals("REPETIR", Veredicto.evaluarColocaciones(mal, p, new java.util.LinkedHashMap<Patron, Double>(),
                0.03, 0.03).veredicto);
        // Con umbral configurable al 5 % pasa.
        assertEquals("OK", Veredicto.evaluarColocaciones(mal, p, new java.util.LinkedHashMap<Patron, Double>(),
                0.03, 0.05).veredicto);
    }

    @Test
    public void colocacionesEnLaCampanaYSuDiario() throws Exception {
        StringWriter w = new StringWriter();
        Campana c = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        c.escribirEn(w);
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", "P2", 0, 'e');
        double[][] x = {{999, 1000, 1001}, {1029, 1030, 1031}, {1014, 1015, 1016}};
        for (int k = 0; k < 3; k++) {
            for (double v : x[k]) {
                c.agregarDisparo(s, k + 1, "f", "::" + (int) v, v);
            }
        }
        c.cerrar(s, "OK", true, "");
        assertEquals(1015, s.media(), 1e-9);                // media de las medias, no de los 9
        assertEquals(15, s.desviacion(), 1e-9);            // s entre colocaciones
        Campana r = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        assertEquals(0, r.leerDiario(new StringReader(w.toString())));
        assertEquals(3, r.serie("S001").colocaciones().size());
        assertEquals(1015, r.serie("S001").media(), 1e-9);
        assertTrue(r.exportarCsv().split("\n")[0].endsWith(",colocacion"));
        assertTrue(r.resumen(), r.resumen().contains("3 colocaciones"));
    }

    @Test
    public void laCampanaRealDeLas1033SigueAbriendose() throws Exception {
        // Versionada en 06_Calibracion/SLV-002/campanas/ (hecha con la 3.6.5, 1 x 9, sin columna de colocacion).
        File zip = new File("../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_103300.zip");
        assertTrue("falta " + zip.getAbsolutePath(), zip.exists());
        String diario = null;
        try (ZipFile z = new ZipFile(zip)) {
            for (ZipEntry e : java.util.Collections.list(z.entries())) {
                if (e.getName().startsWith("diario_")) {
                    diario = new String(readAll(z.getInputStream(e)), StandardCharsets.UTF_8);
                }
            }
        }
        assertNotNull(diario);
        Campana c = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        assertEquals(0, c.leerDiario(new StringReader(diario)));
        assertEquals(56, c.series().size());
        for (Campana.Serie s : c.series()) {
            assertTrue(s.colocaciones().size() <= 1);      // todas de una colocacion
        }
        assertFalse(c.medidasElegidas().isEmpty());
        // Otro equipo no la ve.
        Campana otro = new Campana(catalogo(), "SLV-003", "00:21:13:AA:BB:CC");
        otro.leerDiario(new StringReader(diario));
        assertTrue(otro.series().isEmpty());
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            o.write(b, 0, n);
        }
        in.close();
        return o.toByteArray();
    }

    static List<String> vacia() {
        return new ArrayList<>();
    }
}
