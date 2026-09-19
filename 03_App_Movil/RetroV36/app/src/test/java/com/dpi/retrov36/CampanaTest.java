package com.dpi.retrov36;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Campana de calibracion (3.6.5). Series reales de SLV-002 del 19-sep-2026
 * (fixture src/test/resources/medidas_SLV-002_20260919_consolidado.csv) donde se indica.
 */
public class CampanaTest {

    private static List<Patron> catalogo() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/test/resources/patrones_P1-P50_hasta_3.6.9.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    /** Serie aceptada con esos disparos. */
    private static Campana.Serie medir(Campana c, String patron, double... x) throws Exception {
        Campana.Serie s = c.nuevaSerie("2026-09-19T09:00:00-0500", "SLV-002", "mac", "V3.6", patron, 0, 'e');
        for (double v : x) {
            c.agregarDisparo(s, "t", "::" + (int) v, v);
        }
        c.cerrar(s, "OK", true, "");
        return s;
    }

    @Test
    public void disparoDescolgadoDeP7() {
        // Real: P7, 09:19:26-09:19:39. El operador levanto la pistola en el ultimo.
        double[] p7 = {3209, 3225, 3225, 3229, 3237, 3236, 3231, 3233, 3031};
        String[] m = Veredicto.descolgados(p7);
        for (int i = 0; i < 8; i++) {
            assertNull("disparo " + (i + 1), m[i]);
        }
        assertNotNull(m[8]);
        assertTrue(m[8], m[8].contains("descolgado"));
        Veredicto.Resultado v = Veredicto.evaluar(p7, new Patron("P7", 768, "XI", "blanco"),
                new java.util.LinkedHashMap<Patron, Double>(), Veredicto.TOL_ORDEN);
        assertTrue(v.descartar[8]);
        assertEquals(8, v.n);
        assertFalse(v.ruidosa);
        assertEquals("OK", v.veredicto);
    }

    @Test
    public void serieRuidosaSeRepite() {
        Veredicto.Resultado v = Veredicto.evaluar(new double[]{2000, 2030, 1990, 2040, 1985, 2035},
                new Patron("P2", 414, "IV", "blanco"), new java.util.LinkedHashMap<Patron, Double>(), 0.03);
        assertTrue(v.ruidosa);
        assertEquals("REPETIR", v.veredicto);
    }

    @Test
    public void p24MedidoEnRealidadSobreP20() throws Exception {
        Campana c = new Campana(catalogo());
        medir(c, "P20", 2790, 2795, 2798, 2800, 2801, 2804);   // media 2798
        double[] p24 = {2800, 2802, 2804, 2806, 2808};           // media 2804, elegida como P24
        Veredicto.Resultado v = Veredicto.evaluar(p24, c.patron("P24"), c.medias("P24"), Veredicto.TOL_ORDEN);
        assertEquals("DUDOSO", v.veredicto);
        assertTrue(v.texto, v.texto.contains("¿Es este el patrón?"));
        assertTrue(v.dudas.toString(), v.dudas.get(0).contains("P20"));
    }

    @Test
    public void lasDosSeriesRealesDeP24() throws Exception {
        // Reales: amarillos IV P22 1467, P21 1555, P29 1723 y el XI P10 2456.
        Campana c = new Campana(catalogo());
        medir(c, "P22", 1461, 1465, 1466, 1466, 1472, 1468, 1470, 1472, 1478);
        medir(c, "P21", 1540, 1546, 1552, 1550, 1552, 1557, 1553, 1552, 1554);
        medir(c, "P29", 1714, 1721, 1721, 1726, 1731, 1725, 1723, 1725, 1724);
        medir(c, "P10", 2446, 2450, 2473);
        Map<Patron, Double> m = c.medias("P24");
        // Primera serie de P24 (2048-2076): cuadra con la recta de los IV.
        Veredicto.Resultado a = Veredicto.evaluar(new double[]{2048, 2071, 2076}, c.patron("P24"), m, 0.03);
        assertEquals(a.texto, "OK", a.veredicto);
        // Segunda (2438-2453): se parece a P10 mas que a lo esperado para un IV de 593.
        Veredicto.Resultado b = Veredicto.evaluar(new double[]{2439, 2438, 2453}, c.patron("P24"), m, 0.03);
        assertEquals(b.texto, "DUDOSO", b.veredicto);
        assertTrue(b.dudas.toString(), b.dudas.toString().contains("P10"));
    }

    @Test
    public void ordenPorCertificado() throws Exception {
        Campana c = new Campana(catalogo());
        medir(c, "P21", 1540, 1546, 1552, 1550, 1552);       // 375, IV amarillo
        Veredicto.Resultado mal = Veredicto.evaluar(new double[]{1700, 1702, 1705},
                c.patron("P22"), c.medias("P22"), 0.03);     // 334 < 375 pero da mas x
        assertTrue(mal.dudas.toString(), mal.dudas.get(0).startsWith("orden"));
        Veredicto.Resultado bien = Veredicto.evaluar(new double[]{1461, 1465, 1466},
                c.patron("P22"), c.medias("P22"), 0.03);
        assertTrue(bien.dudas.isEmpty());
        // Dentro de la tolerancia (3 %) no salta.
        Veredicto.Resultado justo = Veredicto.evaluar(new double[]{1590, 1591, 1592},
                c.patron("P22"), c.medias("P22"), 0.03);
        assertTrue(justo.dudas.toString(), justo.dudas.isEmpty());
        // Otro tipo (XI) no entra en el orden de los IV.
        Veredicto.Resultado xi = Veredicto.evaluar(new double[]{1200, 1201, 1202},
                c.patron("P20"), c.medias("P20"), 0.03);
        assertTrue(xi.dudas.toString(), xi.dudas.isEmpty());
    }

    @Test
    public void avisoDeXINoSeSuprime() throws Exception {
        Campana c = new Campana(catalogo());
        medir(c, "P4", 3322, 3355, 3362);                    // 828 XI blanco
        Veredicto.Resultado v = Veredicto.evaluar(new double[]{3500, 3505, 3510},
                c.patron("P1"), c.medias("P1"), 0.03);       // 762 < 828 y da mas
        assertEquals("DUDOSO", v.veredicto);
        assertTrue(v.texto, v.texto.contains("los XI pueden desordenarse por orientación"));
    }

    @Test
    public void persistenciaDeLaCampana() throws Exception {
        List<Patron> cat = catalogo();
        StringWriter diario = new StringWriter();
        Campana c = new Campana(cat);
        c.escribirEn(diario);
        Campana.Serie a = medir(c, "P7", 3209, 3225, 3225, 3229, 3031);
        c.descartar(a, 5, "descolgado");
        Campana.Serie b = medir(c, "P24", 2800, 2804);
        c.reasignar(b, "P20", "era P20");
        Campana.Serie d = medir(c, "P7", 3217, 3233, 3241);
        c.elegir(a);                                          // se elige la primera, no la ultima
        Campana.Serie e = c.nuevaSerie("f", "SLV-002", "mac", "V3.6", "P3", 90, 'e');
        c.agregarDisparo(e, "f", "::1652", 1652);             // sin veredicto: se cerro la app

        Campana r = new Campana(cat);
        assertEquals(0, r.leerDiario(new StringReader(diario.toString())));
        assertEquals(c.exportarCsv(), r.exportarCsv());
        assertEquals(c.resumen(), r.resumen());
        assertEquals("S001", r.elegida("P7").id);
        assertEquals("P20", r.serie("S002").patron);
        assertEquals("P24", r.serie("S002").patronOriginal);
        assertEquals(Campana.Estado.MEDIDO, r.estado("P20"));
        assertEquals(Campana.Estado.POR_HACER, r.estado("P24"));
        assertEquals(Campana.Estado.REPETIR, r.estado("P3"));
        assertEquals(90, r.serie("S004").orientacion);
        assertTrue(r.serie("S001").disparos.get(4).descartado);
        // Se sigue escribiendo tras retomar: el identificador continua.
        assertEquals("S005", r.nuevoId());
    }

    @Test
    public void avanceEImprescindibles() throws Exception {
        Campana c = new Campana(catalogo());
        medir(c, "P3", 1652, 1651, 1664);
        medir(c, "P2", 2014, 2017, 2016);
        medir(c, "P1", 3069, 3072, 3071);
        medir(c, "P4", 3355, 3362, 3353);
        medir(c, "P6", 3109, 3115, 3113);
        medir(c, "P7", 3225, 3229, 3237);
        assertTrue(c.avance(), c.avance().contains("blanco 6/8"));
        assertTrue(c.avance(), c.avance().contains("tipo I 0/20"));
        Map<String, String> imp = c.imprescindibles();
        assertEquals(Arrays.asList("P27", "P28"), new ArrayList<>(imp.keySet()));
        assertTrue(imp.get("P27"), imp.get("P27").contains("P2") && imp.get("P27").contains("P1"));
    }

    @Test
    public void importarCsvDeHoySinDuplicados() throws Exception {
        String cab = Medida.cabecera();
        String f = "2026-09-19T09:19:%s-0500,SLV-002,\"00:21:13:05:19:3B\",V3.6 2026-09-18 DEF mascara 0000,P7,768,XI,blanco,e,\"::%d\",%d,0,e directa,";
        String[] t = {"26", "28", "29", "31", "32", "34", "36", "37", "39", "47", "48", "50"};
        int[] x = {3209, 3225, 3225, 3229, 3237, 3236, 3231, 3233, 3031, 3217, 3233, 3241};
        List<String> copia1 = new ArrayList<>();
        copia1.add(cab);
        for (int i = 0; i < 9; i++) {
            copia1.add(String.format(f, t[i], x[i], x[i]));
        }
        List<String> copia2 = new ArrayList<>(copia1);           // la copia siguiente, mas larga
        for (int i = 9; i < t.length; i++) {
            copia2.add(String.format(f, t[i], x[i], x[i]));
        }
        List<String> todo = new ArrayList<>(copia1);
        todo.addAll(copia2);
        Campana c = new Campana(catalogo());
        Importador.Resultado r = Importador.importar(c, todo, 0.03);
        assertEquals(9, r.duplicadas);
        assertEquals(2, r.series);                               // 8 s entre 09:19:39 y 09:19:47
        assertEquals(9, c.serie("S001").disparos.size());
        assertTrue(c.serie("S001").disparos.get(8).descartado);  // el 3031
        assertEquals("S002", c.elegida("P7").id);
        assertEquals(-1, c.serie("S001").orientacion);
        // Reimportar no duplica nada.
        Importador.Resultado r2 = Importador.importar(c, todo, 0.03);
        assertEquals(0, r2.series);
        assertEquals(21, r2.yaEnCampana);                    // las 21 filas de las dos copias
    }

    /**
     * Las 180 filas unicas de los CSV reales del 19-sep-2026 (08:59:24-09:25:12, SLV-002),
     * consolidadas en src/test/resources/medidas_SLV-002_20260919_consolidado.csv; el origen
     * de cada fila esta en la cabecera del fixture. No depende de "07 pruebas/" (no versionado).
     */
    private static List<String> csvDeHoy() throws Exception {
        java.io.File f = new java.io.File("src/test/resources/medidas_SLV-002_20260919_consolidado.csv");
        assertTrue("falta el fixture " + f.getAbsolutePath(), f.exists());
        return java.nio.file.Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
    }

    @Test
    public void importarLosCsvRealesYColaGuiada() throws Exception {
        Campana c = new Campana(catalogo());
        Importador.Resultado r = Importador.importar(c, csvDeHoy(), Veredicto.TOL_ORDEN);
        assertEquals(r.texto(), 180, r.filas - r.duplicadas - r.yaEnCampana - r.patronDesconocido - r.otroEquipo);
        // P7: dos series; en la primera, el 3031 descartado.
        List<Campana.Serie> p7 = c.seriesDe("P7");
        assertEquals(2, p7.size());
        assertTrue(p7.get(0).disparos.get(8).descartado);
        Map<String, String> prio = c.prioritarios(Veredicto.TOL_ORDEN);
        List<Cola.Paso> cola = Cola.construir(c, Veredicto.TOL_ORDEN, new ArrayList<String>());
        StringBuilder t = new StringBuilder(r.texto()).append(" | ").append(prio).append(" | ");
        for (Cola.Paso p : cola) {
            t.append(p.patron).append('@').append(p.orientacion).append(' ');
        }
        String msg = t.toString();
        // Primero los huecos del blanco IX, luego P24 (series en conflicto) y el par P29/P30.
        assertEquals(msg, "P27", cola.get(0).patron);
        assertEquals(msg, "P28", cola.get(1).patron);
        List<String> cinco = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cinco.add(cola.get(i).patron);
        }
        assertTrue(msg, cinco.containsAll(Arrays.asList("P24", "P30")));
        // Luego los tipo I que ajustan: amarillo y rojo.
        int iP34 = -1;
        int iP32a = -1;
        int iGiro0 = -1;
        for (int i = 0; i < cola.size(); i++) {
            if (cola.get(i).patron.equals("P34") && iP34 < 0) {
                iP34 = i;
            }
            if (cola.get(i).patron.equals("P32a")) {
                iP32a = i;
            }
            if (cola.get(i).patron.equals("P5") && cola.get(i).orientacion == 0) {
                iGiro0 = i;
            }
        }
        assertTrue(msg, iP34 > 0 && iP34 < iP32a && iP32a < iGiro0);
        // Lo medido bien no esta en la cola (P1, P2, P3).
        for (Cola.Paso p : cola) {
            assertFalse(msg, Arrays.asList("P1", "P2", "P3", "P21", "P22").contains(p.patron));
        }
        // Saltar P27 lo manda al final.
        List<Cola.Paso> saltada = Cola.construir(c, Veredicto.TOL_ORDEN, Arrays.asList("P27@0"));
        assertEquals("P28", saltada.get(0).patron);
        assertEquals("P27", saltada.get(saltada.size() - 1).patron);
    }

    @Test
    public void dosEquiposNoCompartenSeries() throws Exception {
        List<Patron> cat = catalogo();
        StringWriter diario = new StringWriter();
        Campana a = new Campana(cat, "SLV-002", "00:21:13:05:19:3B");
        a.escribirEn(diario);
        Campana.Serie s = a.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", "P1", 0, 'e');
        a.agregarDisparo(s, "f", "::3070", 3070);
        a.cerrar(s, "OK", true, "");
        try {
            a.nuevaSerie("f", "SLV-003", "00:21:13:AA:BB:CC", "V3.6", "P1", 0, 'e');
            org.junit.Assert.fail("admitio una serie de otro equipo");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("otro equipo"));
        }
        // El diario de SLV-002 leido en la campana de otro equipo no aporta nada.
        Campana b = new Campana(cat, "SLV-003", "00:21:13:AA:BB:CC");
        assertTrue(b.leerDiario(new StringReader(diario.toString())) > 0);
        assertTrue(b.series().isEmpty());
        assertEquals(Campana.Estado.POR_HACER, b.estado("P1"));
        assertTrue(b.medidasElegidas().isEmpty());
        // Equipo nuevo: todos pendientes y la cola en el mismo orden (tipo I que ajustan, comprobacion, giro...).
        List<Cola.Paso> cola = Cola.construir(b, 0.03, new ArrayList<String>());
        assertEquals(cat.size() + 1, cola.size());   // cada patron una vez y P5 dos (0 y 90)
        assertEquals("P34", cola.get(0).patron);
    }

    @Test
    public void elImportFiltraPorMac() throws Exception {
        List<String> l = csvDeHoy();                     // todo de SLV-002, 00:21:13:05:19:3B
        Campana otro = new Campana(catalogo(), "SLV-003", "00:21:13:AA:BB:CC");
        Importador.Resultado r = Importador.importar(otro, l, 0.03);
        assertEquals(0, r.series);
        assertEquals(r.filas, r.otroEquipo);
        Campana suyo = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        Importador.Resultado r2 = Importador.importar(suyo, l, 0.03);
        assertEquals(0, r2.otroEquipo);
        assertTrue(r2.series > 0);
    }

    @Test
    public void descolgadoC46P2RealNoSaltaP7Si() {
        // C-46: serie real de P2 (09:17:21-09:17:34). Con 5 MAD sin suelo se marcaba un disparo bueno.
        double[] p2 = {1999, 2017, 2004, 2014, 2022, 2016, 2014, 2017, 2016};
        for (String m : Veredicto.descolgados(p2)) {
            assertNull(m);
        }
        // P7 (09:19:26-09:19:39): el 3031 esta ~200 por debajo y sigue saltando.
        String[] m7 = Veredicto.descolgados(new double[]{3209, 3225, 3225, 3229, 3237, 3236, 3231, 3233, 3031});
        assertNotNull(m7[8]);
        assertEquals(50.0, Veredicto.UMBRAL_MIN, 0);
    }

    /** Diario escrito por la 3.6.5 (mismo formato que Csv.unir de esa version). */
    private static final String DIARIO_365 =
            "# inicio_ms,1758290000000\n"
            + Campana.CABECERA_DIARIO + "\n"
            + "SERIE,S001,\"2026-09-19T10:00:00-0500\",SLV-002,\"00:21:13:05:19:3B\",\"V3.6 2026-09-18 (3.6.0, sin límites de #S) DEF mascara 0000\",P2,0,e\n"
            + "DISPARO,S001,1,\"2026-09-19T10:00:02-0500\",\"::1999\",1999\n"
            + "DISPARO,S001,2,\"2026-09-19T10:00:03-0500\",\"::2017\",2017\n"
            + "DISPARO,S001,3,\"2026-09-19T10:00:05-0500\",\"::2004\",2004\n"
            + "DESCARTE,S001,3,descolgado (regla 3.6.5)\n"
            + "VEREDICTO,S001,OK,1,\n"
            + "SERIE,S002,\"2026-09-19T10:01:00-0500\",SLV-002,\"00:21:13:05:19:3B\",V3.6,P27,90,e\n"
            + "DISPARO,S002,1,\"2026-09-19T10:01:02-0500\",\"::2100\",2100\n";

    @Test
    public void laCampanaDeLa365SobreviveALaActualizacion() throws Exception {
        List<Patron> cat = catalogo();
        StringWriter nuevo = new StringWriter();
        nuevo.write(DIARIO_365);
        Campana c = new Campana(cat, "SLV-002", "00:21:13:05:19:3B");
        assertEquals(0, c.leerDiario(new StringReader(DIARIO_365)));
        assertEquals(2, c.series().size());
        assertEquals(Campana.Estado.MEDIDO, c.estado("P2"));
        assertTrue(c.serie("S001").disparos.get(2).descartado);   // lo descartado queda descartado
        assertEquals(Campana.Estado.REPETIR, c.estado("P27"));    // serie sin veredicto: se cortó
        assertFalse(c.cerrada());
        // Se sigue en el mismo diario con la 3.6.6.
        c.escribirEn(nuevo);
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", "P28", 0, 'e');
        assertEquals("S003", s.id);
        c.agregarDisparo(s, "f", "::2150", 2150);
        c.cerrar(s, "OK", true, "");
        c.anotarExportacion("f", "campana_SLV-002_x.zip", "abc", "def");
        c.cerrarCampana("2026-09-19T12:00:00-0500");
        Campana r = new Campana(cat, "SLV-002", "00:21:13:05:19:3B");
        assertEquals(0, r.leerDiario(new StringReader(nuevo.toString())));
        assertTrue(r.cerrada());
        assertEquals(3, r.series().size());
        assertTrue(r.resumen(), r.resumen().contains("CERRADA"));
        assertTrue(r.resumen(), r.resumen().contains("md5 abc"));
        try {
            r.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", "P1", 0, 'e');
            org.junit.Assert.fail("una campaña cerrada admitió una serie");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("cerrada"));
        }
    }

    @Test
    public void desvioPorPosicionEnElResumen() throws Exception {
        Campana c = new Campana(catalogo());
        medir(c, "P2", 1999, 2017, 2004, 2014, 2022, 2016, 2014, 2017, 2016);   // mediana 2016
        String t = c.resumen();
        assertTrue(t, t.contains("Desvío por posición"));
        assertTrue(t, t.contains("1:-17 2:+1 3:-12"));
        assertTrue(t, t.contains("Media por posición"));
    }
}
