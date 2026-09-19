package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** App 3.6.9: importar campana exportada, recta anclada en oscuro, preajustes y conformidad. */
public class Version369Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final File ZIP = new File("../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_103300.zip");

    private static List<Patron> catalogo() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P31.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    private static byte[] zip() throws Exception {
        assertTrue("falta " + ZIP.getAbsolutePath(), ZIP.exists());
        return Files.readAllBytes(ZIP.toPath());
    }

    /** Campana reconstruida importando el ZIP real (por su diario, como hace la app). */
    private static Campana importada() throws Exception {
        Campana c = new Campana(catalogo(), "SLV-002", MAC);
        ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(zip()), catalogo(), "zip de prueba");
        return c;
    }

    @Test
    public void importarElZipRealDaLas56SeriesTalCual() throws Exception {
        byte[] z = zip();
        assertTrue(ImportadorCampana.esZip(z));
        assertTrue(Resumen.hex(z, "MD5").startsWith("4c50dbf6"));
        List<String> csv = ImportadorCampana.csvDeZip(z);
        assertNotNull(csv);
        assertTrue(ImportadorCampana.esCsvDeCampana(csv));
        // Desde el diario del ZIP (lo que hace la app): las 56, incluida S010, que no tiene disparos.
        Campana c = new Campana(catalogo(), "SLV-002", MAC);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(z), catalogo(),
                "campana_SLV-002_20260919_103300.zip md5 4c50dbf6");
        assertEquals(r.texto(), 56, r.series);
        // Solo con campana.csv salen 55: una serie sin disparos no tiene filas.
        Campana soloCsv = new Campana(catalogo(), "SLV-002", MAC);
        assertEquals(55, ImportadorCampana.importar(soloCsv, csv, "csv").series);
        // Igual que leyendo el diario del mismo ZIP: mismas elegidas, mismas medias, sin re-juzgar.
        String diario = null;
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(ZIP)) {
            for (java.util.zip.ZipEntry e : java.util.Collections.list(zf.entries())) {
                if (e.getName().startsWith("diario_")) {
                    diario = new String(ImportadorCampana.leer(zf.getInputStream(e)), StandardCharsets.UTF_8);
                }
            }
        }
        Campana d = new Campana(catalogo(), "SLV-002", MAC);
        d.leerDiario(new StringReader(diario));
        for (Patron p : catalogo()) {
            Campana.Serie a = c.elegida(p.nombre);
            Campana.Serie b = d.elegida(p.nombre);
            assertEquals(p.nombre, b == null, a == null);
            if (a != null) {
                assertEquals(p.nombre, b.media(), a.media(), 1e-9);
                assertEquals(p.nombre, b.veredicto, a.veredicto);
            }
        }
        assertTrue(c.series().get(0).nota.contains("importada de campana_SLV-002_20260919_103300.zip"));
        // Reimportar no duplica (ni por el diario ni por el csv).
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(z), catalogo(), "otra vez");
        assertEquals(0, r2.series);
        assertEquals(56, r2.yaEstaban);
        assertEquals(0, ImportadorCampana.importar(c, csv, "otra vez").series);
    }

    @Test
    public void laCampanaDeOtroEquipoNoSeImporta() throws Exception {
        Campana otro = new Campana(catalogo(), "SLV-003", "00:21:13:AA:BB:CC");
        try {
            ImportadorCampana.importarDiario(otro, ImportadorCampana.diarioDeZip(zip()), catalogo(), "x");
            org.junit.Assert.fail("importó la campaña de otro equipo");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("otro equipo"));
        }
        assertTrue(otro.series().isEmpty());
        try {
            ImportadorCampana.importar(otro, ImportadorCampana.csvDeZip(zip()), "x");
            org.junit.Assert.fail("importó el csv de otro equipo");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("otro equipo"));
        }
        assertTrue(otro.series().isEmpty());
    }

    @Test
    public void rectaAncladaReproduceLaPropuesta() throws Exception {
        // PROPUESTA-Ajuste-SLV-002-2026-09-19.md:270: k = 0,3675 (c1 3.67534027E-01, c0 -2.11332065E+02),
        // IV -6,9 / 8,7 %, IX +0,3 / 2,5 %, XI +2,1 / 5,8 %.
        Campana c = importada();
        List<Asistente.Punto> p = Asistente.puntos(c.medidasElegidas(), '2');
        double[] x = new double[p.size()];
        double[] r = new double[p.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = p.get(i).x;
            r[i] = p.get(i).patron.valor;
        }
        Ajuste.Resultado a = Ajuste.anclada(x, r, 575, 0);
        assertEquals(0.367534027, a.ecuacion.c1, 1e-8);
        assertEquals(-211.332065, a.ecuacion.c0, 1e-5);
        List<Asistente.Punto> iv = new ArrayList<>();
        List<Asistente.Punto> xi = new ArrayList<>();
        List<Asistente.Punto> ix = new ArrayList<>();
        for (Asistente.Punto q : p) {
            ("IV".equals(q.patron.tipo) ? iv : "XI".equals(q.patron.tipo) ? xi : ix).add(q);
        }
        assertEquals(-6.9, Asistente.rel(iv, a.ecuacion)[0], 0.05);
        assertEquals(8.7, Asistente.rel(iv, a.ecuacion)[1], 0.05);
        assertEquals(2.5, Asistente.rel(ix, a.ecuacion)[1], 0.05);
        assertEquals(5.8, Asistente.rel(xi, a.ecuacion)[1], 0.05);
        // En la app: opcion "Recta anclada en oscuro", pasa #S, C2 y el oscuro (R(600) = 9,2).
        Asistente.X_ANCLA = 575;
        Asistente.R_ANCLA = 0;
        Asistente.ORIGEN_ANCLA = "prueba";
        Asistente.Propuesta pr = Asistente.proponer('2', Asistente.GRADO_ANCLADA, c.medidasElegidas(),
                Fabrica.ecuacion('2'), catalogo());
        assertTrue(pr.informe, pr.escribible());
        assertTrue(pr.informe, pr.informe.contains("RECTA ANCLADA EN OSCURO"));
        assertTrue(pr.metodo, pr.metodo.startsWith("recta anclada en oscuro"));
        assertEquals(9.2, pr.ajuste.ecuacion.evaluar(600), 0.05);
    }

    @Test
    public void blancoGrado1AvisaPeroNoBloquea() throws Exception {
        Campana c = importada();
        Asistente.Propuesta pr = Asistente.proponer('1', 1, c.medidasElegidas(), Fabrica.ecuacion('1'), catalogo());
        assertTrue(pr.informe, pr.escribible());
        String inc = pr.incumplimientos.toString();
        assertTrue(inc, inc.contains("RF-CAL-14: P3"));
        assertTrue(inc, inc.contains("RF-CAL-15: RMS de IV"));
        assertTrue(inc, inc.contains("RF-CAL-16: en XI"));
        // La conformidad queda en el acta con el metodo.
        Acta acta = new Acta("SLV-002", MAC, "fw", 1, 9, 1, "f");
        acta.escrito('1', "#G,1,...#", pr.ajuste.ecuacion, "oscuro", pr.metodo,
                "Conformidad del superadministrador pese a incumplir " + inc + ". Nota: Diego, 11:20");
        assertTrue(acta.texto(), acta.texto().contains("Conformidad del superadministrador"));
        assertTrue(acta.texto(), acta.texto().contains("Método: mínimos cuadrados, grado 1"));
    }

    @Test
    public void oscuroYA5() throws Exception {
        assertEquals(5, A5.COLOCACIONES);
        assertEquals(9, A5.DISPAROS);
        Campana c = new Campana(catalogo(), "SLV-002", MAC);
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", MAC, "V3.6", Campana.OSCURO.nombre, 0, 'e');
        for (int k = 1; k <= 5; k++) {
            for (int i = 0; i < 9; i++) {
                c.agregarDisparo(s, k, "f", "::" + (570 + k), 570 + k);
            }
        }
        c.cerrar(s, "OK", true, "");
        assertEquals(573, c.xOscuro(), 1e-9);                      // media de las 5 colocaciones
        assertTrue(c.medidasElegidas().isEmpty());                 // no entra en el ajuste de ningun codigo
        for (Cola.Paso p : Cola.construir(c, 0.03, new ArrayList<String>())) {
            assertFalse(Campana.OSCURO.nombre.equals(p.patron));  // ni en la cola
        }
        assertFalse(c.avance().contains("oscuro"));
        assertTrue(c.resumen(), c.resumen().contains("Oscuro: serie S001, x = 573.0"));
    }
}
