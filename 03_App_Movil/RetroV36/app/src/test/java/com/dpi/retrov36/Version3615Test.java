package com.dpi.retrov36;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * App 3.6.15: colas del banco (RF-APP-49), P81 fuera del ajuste del 5 con las tres colas, protocolo 1 x 4
 * (PROTOCOLO-MIN), importacion y rehacer (QA-3614-01, -02, -04, -05, -06, -07), renombrar la serie (SERIE-2),
 * SPEC-App-Unica (@LEERV estricta, campana sin MAC) y el ZIP incremental y de soporte (RF-APP-50 a 53).
 */
public class Version3615Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private List<Patron> catalogo;

    @Before
    public void preparar() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
    }

    private static BancoCola cola(BancoCola.Tipo t) throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    private static Decisiones decisiones() throws Exception {
        return Decisiones.leer(new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()),
                StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- RF-APP-49

    @Test
    public void lasTresColasCarganConSuMd5() throws Exception {
        assertEquals(180, cola(BancoCola.Tipo.COMPLETO).pasos.size());
        assertEquals("3.6.16: v2, sin la bateria por color", 80, cola(BancoCola.Tipo.REPRESENTATIVO).pasos.size());
        assertEquals(35, cola(BancoCola.Tipo.VERIFICACION_ANUAL).pasos.size());
        assertEquals("70ef3b868db85ef75743936a6218935e", cola(BancoCola.Tipo.REPRESENTATIVO).md5);
        assertEquals("d86eddf7fbdfdd4ae11ee2080d220e6c", cola(BancoCola.Tipo.VERIFICACION_ANUAL).md5);
        byte[] otra = Files.readAllBytes(new File("src/main/assets/" + BancoCola.Tipo.COMPLETO.asset).toPath());
        otra[otra.length - 2] ^= 1;
        try {
            BancoCola.cargar(otra);
            fail("una cola con otro md5 no se admite");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cola no admitida"));
        }
    }

    @Test
    public void conLaVerificacionAnualNingunCodigoEsCalibrable() throws Exception {
        BancoCola a = cola(BancoCola.Tipo.VERIFICACION_ANUAL);
        Map<Integer, String> todo = new HashMap<>();
        for (BancoCola.Paso p : a.pasos) {
            todo.put(p.orden, "HECHO");
        }
        for (char k : Fabrica.CODIGOS) {
            assertFalse(String.valueOf(k), a.calibrable(String.valueOf(k), todo));
        }
    }

    /** Decision de Diego (c836cae): P81 fuera del ajuste del 5, con cualquiera de las tres colas. */
    @Test
    public void p81NoEntraEnElAjusteDel5ConNingunaCola() throws Exception {
        List<String> exc = TablaCalibracion.fila('5', decisiones(), "SLV-002").excluidos;
        assertEquals(Arrays.asList("P81"), exc);
        for (BancoCola.Tipo t : BancoCola.Tipo.values()) {
            Set<String> s = FlujoCalibracion.patronesAjuste(cola(t), '5', exc);
            assertFalse(t + ": " + s, s.contains("P81"));
        }
        // Sin la decision entraba (lo que QA-3613-07 senalaba).
        assertTrue(FlujoCalibracion.patronesAjuste(cola(BancoCola.Tipo.COMPLETO), '5', new ArrayList<String>()).contains("P81"));
        assertEquals(12, FlujoCalibracion.patronesAjuste(cola(BancoCola.Tipo.COMPLETO), '5', exc).size());
    }

    // ---------------------------------------------------------- PROTOCOLO-MIN

    @Test
    public void protocoloPorDefecto1x4ConA5YOscuroEnK5() throws Exception {
        for (BancoCola.Tipo t : BancoCola.Tipo.values()) {
            for (BancoCola.Paso p : cola(t).pasos) {
                if (!p.esMedida()) {
                    continue;
                }
                int[] r = ProtocoloDisparos.efectivo(p, true);
                int[] pr = ProtocoloDisparos.efectivo(p, false);
                if ("OSCURO".equals(p.tipo) || "A5".equals(p.tipo)) {
                    assertEquals(t + " " + p.orden, 5, r[0]);
                    assertEquals(5, pr[0]);
                } else if (ProtocoloDisparos.deAjuste(p)) {
                    // 3.6.16 (PROTOCOLO-AJUSTE, 6048453): lo que se escribe va a 5 x 4 tambien en rapido.
                    assertEquals(t + " " + p.orden, 5, r[0]);
                    assertEquals(4, r[1]);
                    assertEquals(5, pr[0]);
                    assertEquals(4, pr[1]);
                    // con RAPIDO firmado, vuelve a 1 x 4
                    assertEquals(1, ProtocoloDisparos.efectivo(p, true, "RAPIDO")[0]);
                } else {
                    assertEquals(1, r[0]);
                    assertEquals(4, r[1]);
                    assertEquals(p.k, pr[0]);
                    assertEquals(p.m, pr[1]);
                }
            }
        }
        assertEquals(1, ProtocoloDisparos.K_DEFECTO);
        assertEquals(4, ProtocoloDisparos.M_DEFECTO);
        assertEquals("la re-medida no baja de 5 x 4", 5, Remedida3611.K);
    }

    @Test
    public void laSrepSigueSaliendoDeLaA5ConPatronesA1x4() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        for (BancoCola.Paso p : q.pasos) {
            if (!p.esMedida() || p.sesion != 1) {
                continue;
            }
            int[] km = ProtocoloDisparos.efectivo(p, true);
            String pat = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
            double x = "OSCURO".equals(p.tipo) ? 565 : p.xEsperada;
            Campana.Serie s = c.nuevaSerie("f", "SLV-002", MAC, "V3.6", pat, 0, 'e');
            for (int k = 1; k <= km[0]; k++) {
                for (int i = 0; i < km[1]; i++) {
                    double v = x * (1 + 0.01 * (k - 3));
                    c.agregarDisparo(s, k, "f", "::" + Math.round(v), v);
                }
            }
            c.cerrar(s, "A5".equals(p.tipo) ? A5.VEREDICTO : "OK", !"A5".equals(p.tipo), "");
            c.anotarPaso(p.orden, "HECHO", s.id, "f", "");
        }
        Anclas.Valor v = Anclas.sRep(q, c);
        assertTrue(v.texto, v.texto.contains("A5 del inicio del banco, 3 patrones"));
        assertFalse(Double.isNaN(v.valor));
        assertFalse(Double.isNaN(Anclas.oscuro(q, c, '8').valor));
    }

    // ------------------------------------------------------ SPEC-App-Unica (3.6.14)

    @Test
    public void laRespuestaLeervEsEstricta() {
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,BLA,1@", true));        // el eco de la sonda
        assertEquals("@LEERV,127@", Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,BLA,1@@LEERV,127@/n/r", true));
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,127,45@", true));
        assertEquals("@LEERV,-3@", Tramas.extraer(Tramas.Tipo.LEERV, "***** RETROREFLECTOMETRO *****@LEERV,-3@", true));
    }

    @Test
    public void unaCampanaSinMacNoCasaConNingunEquipo() {
        Campana sinMac = new Campana(catalogo, "SLV-002", "");
        assertFalse(sinMac.esDeEsteEquipo(MAC));
        assertFalse(sinMac.esDeEsteEquipo(""));
        assertTrue(new Campana(catalogo).esDeEsteEquipo(MAC));          // el lector sin atar
        assertTrue(new Campana(catalogo, "SLV-002", MAC).esDeEsteEquipo(MAC.toLowerCase()));
    }

    // ---------------------------------------------------------- rehacer e importar

    private static Campana.Serie medir(Campana c, BancoCola.Paso p, double x) throws Exception {
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6",
                "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron, 0, 'e');
        for (int k = 1; k <= 2; k++) {
            for (int i = 0; i < 4; i++) {
                c.agregarDisparo(s, k, "t" + x + k + i, "::" + Math.round(x + i), x + i);
            }
        }
        c.cerrar(s, "OK", true, "");
        c.elegir(s);
        c.anotarPaso(p.orden, "HECHO", s.id, "f", "");
        return s;
    }

    private static BancoCola.Paso paso(BancoCola q, String patron) {
        for (BancoCola.Paso p : q.pasos) {
            if ("PATRON".equals(p.tipo) && patron.equals(p.patron)) {
                return p;
            }
        }
        throw new AssertionError(patron);
    }

    /** QA-3614-01 (R5): dos ZIP sucesivos; el segundo trae la anulacion de una serie que el destino ya tenia. */
    @Test
    public void importarDosZipSucesivosConservaLaAnulacion() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        StringWriter d = new StringWriter();
        Campana o = new Campana(catalogo, "SLV-002", MAC);
        o.escribirEn(d);
        BancoCola.Paso p37 = paso(q, "P37");
        Campana.Serie mala = medir(o, p37, 1500);
        String zip1 = d.toString();
        RehacerBanco.rehacer(o, q, p37.orden, "papel equivocado: pedía P37, puse P40", "2026-09-20T09:30:00-0500");
        Campana.Serie buena = medir(o, p37, 994);
        String zip2 = d.toString();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        ImportadorCampana.importarDiario(c, zip1, catalogo, "zip1");
        assertEquals(1502, c.elegida("P37").media(), 1);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, zip2, catalogo, "zip2");
        assertEquals(1, r.anuladas);
        assertEquals(994 + 1.5, c.elegida("P37").media(), 0.01);
        assertEquals(1, c.seriesAnuladas().size());
        assertEquals("papel equivocado: pedía P37, puse P40", c.seriesAnuladas().get(0).anulada);
        assertEquals("2026-09-20T09:30:00-0500", c.seriesAnuladas().get(0).anuladaFecha);    // QA-3614-07
        assertEquals("HECHO", c.pasos().get(p37.orden));
        assertEquals(c.elegida("P37").id, c.seriePaso(p37.orden));
        assertTrue(mala.anulada != null && buena.anulada == null);
    }

    /** QA-3614-02: un motivo con salto de linea no rompe el diario; un diario viejo con uno se lee. */
    @Test
    public void unMotivoConSaltoDeLineaNoRompeElDiario() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        StringWriter d = new StringWriter();
        Campana o = new Campana(catalogo, "SLV-002", MAC);
        o.escribirEn(d);
        BancoCola.Paso p37 = paso(q, "P37");
        medir(o, p37, 1500);
        RehacerBanco.rehacer(o, q, p37.orden, "papel equivocado:\npedía P37, puse P40", "t");
        for (String l : d.toString().split("\n")) {
            assertFalse(l, l.startsWith("pedía"));
        }
        Campana b = new Campana(catalogo, "SLV-002", MAC);
        assertEquals(0, b.leerDiario(new StringReader(d.toString())));
        assertEquals("papel equivocado: pedía P37, puse P40", b.seriesAnuladas().get(0).anulada);
        // Diario escrito por la 3.6.14 con el salto dentro de las comillas.
        String viejo = d.toString().replace("papel equivocado: pedía", "papel equivocado:\npedía");
        Campana v = new Campana(catalogo, "SLV-002", MAC);
        assertEquals(0, v.leerDiario(new StringReader(viejo)));
        Campana w = new Campana(catalogo, "SLV-002", MAC);
        w.escribirEn(new StringWriter());
        assertEquals(1, ImportadorCampana.importarDiario(w, viejo, catalogo, "zip viejo").series);
    }

    /** QA-3614-04: tras exportar, rehacer pide exportar otra vez. */
    @Test
    public void trasExportarRehacerVuelveAPedirElZip() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        for (BancoCola.Paso p : q.pasos) {
            if (p.sesion == 1 && !p.esMedida()) {
                c.anotarPaso(p.orden, "HECHO", "", "f", "");
            }
        }
        BancoCola.Paso p37 = paso(q, "P37");
        medir(c, p37, 1500);
        c.anotarExportacion("t", "z.zip", "m", "s");
        assertEquals(0, c.seriesSinExportar());
        RehacerBanco.rehacer(c, q, p37.orden, "papel", "t");
        assertEquals(1, c.seriesSinExportar());
        int exp = -1;
        for (BancoCola.Paso p : q.pasos) {
            if (p.sesion == 1 && "EXPORTAR".equals(p.tipo)) {
                exp = p.orden;
            }
        }
        assertEquals("REHACER", c.pasos().get(exp));
    }

    /** QA-3614-05: no se rehace un OSCURO de una sesion ya terminada. */
    @Test
    public void noSeRehaceUnOscuroDeUnaSesionTerminada() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        BancoCola.Paso osc1 = q.paso(3);
        assertEquals("OSCURO", osc1.tipo);
        medir(c, osc1, 565);
        assertNull(RehacerBanco.motivoNoRehacer(c, q, osc1.orden));          // sesion 1 en curso: se puede
        BancoCola.Paso s2 = null;
        for (BancoCola.Paso p : q.pasos) {
            if (p.sesion == 2 && s2 == null) {
                s2 = p;
            }
        }
        c.anotarPaso(s2.orden, "HECHO", "", "f", "");
        assertNotNull(RehacerBanco.motivoNoRehacer(c, q, osc1.orden));
        try {
            RehacerBanco.rehacer(c, q, osc1.orden, "x", "t");
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("QA-3614-05"));
        }
    }

    /** QA-3614-06: "No, rehacer" deja la serie no aceptada y anulada; el paso no queda HECHO. */
    @Test
    public void noRehacerNoDejaLaSerieAceptada() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        StringWriter d = new StringWriter();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(d);
        for (BancoCola.Paso p : q.pasos) {
            if (p.orden < paso(q, "P37").orden) {
                c.anotarPaso(p.orden, "HECHO", "", "f", "");
            }
        }
        BancoCola.Paso p37 = paso(q, "P37");
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", MAC, "V3.6", "P37", 0, 'e');
        c.agregarDisparo(s, 1, "f", "::1500", 1500);
        c.cerrar(s, "OK", false, "el operador dice que no era P37");   // lo que hace BancoActivity
        Campana b = new Campana(catalogo, "SLV-002", MAC);                 // la app muere aqui
        assertEquals(0, b.leerDiario(new StringReader(d.toString())));
        assertNull(b.elegida("P37"));
        assertEquals(p37.orden, q.siguiente(b.pasos()).orden);
        c.anular(s, "no era P37", "t");
        assertEquals(1, c.seriesAnuladas().size());
    }

    @Test
    public void laListaDeRehacerSeFiltra() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        medir(c, paso(q, "P44"), 900);
        medir(c, paso(q, "P37"), 992);
        assertEquals(2, RehacerBanco.filtrar(c, q, "").size());
        assertEquals(Arrays.asList(paso(q, "P37").orden), RehacerBanco.filtrar(c, q, "p37"));
        assertEquals(2, RehacerBanco.filtrar(c, q, "amarillo").size());
        assertEquals("P37, paso " + paso(q, "P37").orden, RehacerBanco.corto(c, q, paso(q, "P37").orden));
    }

    // --------------------------------------------------------- SERIE-2: renombrar

    private static byte[] zipAntiguo() throws Exception {
        return Files.readAllBytes(new File("../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip").toPath());
    }

    /** El ZIP real medido bajo SLV-002 se importa en la campana del equipo renombrado a SLV-002-2026. */
    @Test
    public void elZipAntiguoSeImportaEnElEquipoRenombrado() throws Exception {
        Campana c = new Campana(catalogo, "SLV-002-2026", MAC);
        c.escribirEn(new StringWriter());
        c.renombrar("SLV-002", "SLV-002-2026", "2026-09-20T09:00:00-0500", "Diego");
        assertEquals("SLV-002-2026 (antes SLV-002)", c.serieConHistoria());
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(zipAntiguo()),
                catalogo, "zip 12:27");
        assertTrue(r.texto(), r.series > 40);
        assertEquals("SLV-002", TablaCalibracion.canonico(c.historialSeries(), c.mac));
    }

    @Test
    public void conOtraMacLaSerieAntiguaSeRechaza() throws Exception {
        Campana c = new Campana(catalogo, "SLV-002-2026", "00:21:13:AA:BB:CC");
        c.escribirEn(new StringWriter());
        c.renombrar("SLV-002", "SLV-002-2026", "t", "Diego");
        try {
            ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(zipAntiguo()), catalogo, "zip");
            fail("otra MAC no se importa");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("otro equipo"));
        }
        assertTrue(c.series().isEmpty());
        assertFalse("SLV-002".equals(TablaCalibracion.canonico(c.historialSeries(), c.mac)));
    }

    /** La campana de SLV-002 se reanuda con la serie nueva: su diario la renombra (Campanas.abrir la busca asi). */
    @Test
    public void laCampanaSeReanudaTrasRenombrar() throws Exception {
        StringWriter d = new StringWriter();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(d);
        c.renombrar("SLV-002", "SLV-002-2026", "t", "Diego");
        assertTrue(Campana.diarioRenombraA(new StringReader(d.toString()), "SLV-002-2026"));
        assertFalse(Campana.diarioRenombraA(new StringReader(d.toString()), "SLV-003-2026"));
        Campana b = new Campana(catalogo, "SLV-002-2026", MAC);
        assertEquals(0, b.leerDiario(new StringReader(d.toString())));
        assertTrue(b.esSerie("SLV-002"));
        assertEquals("SLV-002-2026", b.serieActual());
        try {
            c.renombrar("SLV-002-2026", "SLV-002-20260", "t", "Diego");
            fail("más de 12 caracteres");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("12"));
        }
    }

    // ---------------------------------------------------------- RF-APP-50 a 53

    @Test
    public void zipIncrementalLlevaSoloLoNuevoYSeReconstruye() throws Exception {
        byte[] diario1 = "SERIE,S001\nDISPARO,S001,1\n".getBytes(StandardCharsets.UTF_8);
        byte[] diario2 = "SERIE,S001\nDISPARO,S001,1\nDISPARO,S001,2\n".getBytes(StandardCharsets.UTF_8);
        byte[] diario3 = "SERIE,S001\nDISPARO,S001,1\nDISPARO,S001,2\nANULA,S001,t,x\n".getBytes(StandardCharsets.UTF_8);
        byte[] pruebas = "APTO\n".getBytes(StandardCharsets.UTF_8);
        List<PaquetesZip.Pieza> v1 = Arrays.asList(new PaquetesZip.Pieza("diario_c.csv", diario1),
                new PaquetesZip.Pieza("pruebas.txt", pruebas));
        PaquetesZip.Incremental i1 = PaquetesZip.incremental(v1, new LinkedHashMap<String, PaquetesZip.Entrada>(), "", "");
        assertEquals(2, i1.piezas.size());
        List<PaquetesZip.Pieza> v2 = Arrays.asList(new PaquetesZip.Pieza("diario_c.csv", diario2),
                new PaquetesZip.Pieza("pruebas.txt", pruebas));
        PaquetesZip.Incremental i2 = PaquetesZip.incremental(v2,
                PaquetesZip.leerEstado(PaquetesZip.estadoTexto(i1.estado)), "z1.zip", "abc");
        assertEquals(1, i2.piezas.size());
        assertEquals("diario_c.csv.desde_" + diario1.length, i2.piezas.get(0).nombre);
        assertEquals("DISPARO,S001,2\n", new String(i2.piezas.get(0).datos, StandardCharsets.UTF_8));
        assertTrue(i2.indice.contains("anterior z1.zip abc"));
        List<PaquetesZip.Pieza> v3 = Arrays.asList(new PaquetesZip.Pieza("diario_c.csv", diario3),
                new PaquetesZip.Pieza("pruebas.txt", pruebas));
        PaquetesZip.Incremental i3 = PaquetesZip.incremental(v3, i2.estado, "z2.zip", "def");
        Map<String, byte[]> r = new HashMap<>();
        PaquetesZip.aplicar(r, i1.piezas, i1.indice);
        PaquetesZip.aplicar(r, i2.piezas, i2.indice);
        PaquetesZip.aplicar(r, i3.piezas, i3.indice);
        assertTrue(Arrays.equals(diario3, r.get("diario_c.csv")));
        // Si falta un incremental intermedio, la reconstruccion falla y lo dice.
        Map<String, byte[]> r2 = new HashMap<>();
        PaquetesZip.aplicar(r2, i1.piezas, i1.indice);
        try {
            PaquetesZip.aplicar(r2, i3.piezas, i3.indice);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("falta un incremental anterior"));
        }
    }

    @Test
    public void elZipVaConDeflate9YResumenConHashes() throws Exception {
        List<PaquetesZip.Pieza> p = new ArrayList<>();
        StringBuilder mucho = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            mucho.append("DISPARO,S001,").append(i).append(",2026-09-20,::1500,1500.0,1\n");
        }
        p.add(new PaquetesZip.Pieza("diario_c.csv", mucho.toString().getBytes(StandardCharsets.UTF_8)));
        String lista = PaquetesZip.listaHashes(p);
        assertTrue(lista.contains(PaquetesZip.md5(p.get(0).datos)));
        assertTrue(lista.contains(PaquetesZip.sha256(p.get(0).datos)));
        byte[] z = PaquetesZip.zip(p);
        assertTrue("comprime", z.length < p.get(0).datos.length / 10);
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(z))) {
            ZipEntry e = in.getNextEntry();
            assertEquals("diario_c.csv", e.getName());
            assertTrue(Arrays.equals(p.get(0).datos, ImportadorCampana.leer(in)));
        }
    }
}
