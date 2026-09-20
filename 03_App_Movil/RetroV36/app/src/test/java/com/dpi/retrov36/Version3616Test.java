package com.dpi.retrov36;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * App 3.6.16: decisiones de 6048453 (PROTOCOLO-AJUSTE, TIPO-I-REPETIR, RF-CAL-15 estricto), banco mas corto
 * (lo ya medido cuenta, cola representativa v2, cuanto queda), cambio de cola e importacion (QA-3615-03), resumen
 * corto del ZIP ligero (RF-APP-50) y s_rep por sesion (C-P14-3). El flujo de calibracion (P14-01/02/04/05/06/10,
 * QA-3615-01/02/04/05/06) esta en FlujoCalibracionTest, contra el equipo simulado.
 */
public class Version3616Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String ZIP_1510 = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    private static final List<String> REPETIR = Arrays.asList("P34", "P37", "P43", "P44", "P38", "P39", "P49");
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

    private static String asset(String n) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    private static Decisiones decisiones() throws Exception {
        return Decisiones.leer(asset("decisiones.csv"));
    }

    private static BancoPrevio.Grupos grupos() throws Exception {
        return BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv"));
    }

    private static BancoCola.Paso paso(BancoCola q, String patron) {
        for (BancoCola.Paso p : q.pasos) {
            if ("PATRON".equals(p.tipo) && patron.equals(p.patron)) {
                return p;
            }
        }
        throw new AssertionError(patron);
    }

    /** Serie de k colocaciones x m disparos, aceptada y elegida (sin paso). */
    private static Campana.Serie medir(Campana c, String patron, double x, int k, int m) throws Exception {
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int j = 1; j <= k; j++) {
            for (int i = 0; i < m; i++) {
                c.agregarDisparo(s, j, "t" + x + j + i, "::" + Math.round(x + i % 2), x + i % 2);
            }
        }
        c.cerrar(s, "OK", true, "");
        c.elegir(s);
        return s;
    }

    // ---------------------------------------------------------- decisiones de 6048453

    @Test
    public void lasDecisionesDe6048453SonDefinitivas() throws Exception {
        Decisiones d = decisiones();
        assertTrue(d.rechazadas().toString(), d.rechazadas().isEmpty());
        assertEquals("PRECISO", d.valor("PROTOCOLO-AJUSTE", "SLV-002"));
        assertEquals(REPETIR, d.decision("TIPO-I-REPETIR", "SLV-002").repetidos);
        assertTrue(d.decision("TIPO-I-REPETIR", "SLV-002").alcance.isEmpty());
        // P14-08: RF-CAL-15 del b, 11,5 % estricto (sin +-3).
        TablaCalibracion.Fila b = TablaCalibracion.fila('b', d, "SLV-002");
        assertTrue(b.dispensado("RF-CAL-15", "I", 11.5));
        assertFalse(b.dispensado("RF-CAL-15", "I", 11.6));
        assertTrue(b.dispensado("RF-CAL-14", "P39", 17.9));
        // P14-11: la version de las reglas no lleva la del APK.
        assertFalse(TablaCalibracion.VERSION, TablaCalibracion.VERSION.contains("APK"));
        assertTrue(TablaCalibracion.VERSION.contains("r5"));
    }

    @Test
    public void losPatronesARepetirVanA5x4YLoDeAjusteTambien() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        for (String p : REPETIR) {
            int[] km = ProtocoloDisparos.efectivo(paso(q, p), true, "PRECISO", REPETIR);
            assertEquals(p, 5, km[0]);
            assertEquals(p, 4, km[1]);
        }
        // Un tipo I que no se repite y no es de lo que se escribe: 1 x 4 en rapido.
        for (BancoCola.Paso p : q.pasos) {
            if ("PATRON".equals(p.tipo) && !ProtocoloDisparos.deAjuste(p) && !REPETIR.contains(p.patron)) {
                assertEquals(1, ProtocoloDisparos.efectivo(p, true, "PRECISO", REPETIR)[0]);
                break;
            }
        }
        assertEquals(5, ProtocoloDisparos.requerido("PRECISO")[0]);
        assertNull(ProtocoloDisparos.requerido("LIBRE"));
    }

    // ---------------------------------------------------------- cola representativa v2

    @Test
    public void laColaRepresentativaV2SoloTieneA5YOscuroDeControl() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        assertEquals("70ef3b868db85ef75743936a6218935e", q.md5);
        // El fichero de 06_Calibracion se versiona en LF; un checkout con autocrlf lo deja en CRLF: se normaliza.
        byte[] repo = new String(Files.readAllBytes(new File("../../../06_Calibracion/cola_banco_representativo_v2.csv")
                .toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
        assertEquals("la del APK es la de 06_Calibracion", q.md5, PaquetesZip.md5(repo));
        for (BancoCola.Paso p : q.pasos) {
            // solo la bateria de inicio de sesion (orden 9); la de cada color salio en la v2
            assertFalse(p.orden + " " + p.tipo, "BATERIA".equals(p.tipo) && !"INICIO".equals(p.bloque));
        }
        int a5 = 0;
        int osc = 0;
        for (BancoCola.Paso p : q.pasos) {
            a5 += "A5".equals(p.tipo) ? 1 : 0;
            osc += "OSCURO".equals(p.tipo) ? 1 : 0;
        }
        assertTrue(a5 > 0 && osc > 0);
    }

    // ---------------------------------------------------------- banco mas corto

    /** Lo ya medido cuenta; un equivalente del grupo tambien; un AJUSTE de lo que se escribe a K < 5, no. */
    @Test
    public void loYaMedidoYSusEquivalentesCuentan() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        BancoPrevio.Grupos g = grupos();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        c.elegirCola("REPRESENTATIVO", q.md5);
        // Un paso no de ajuste cuyo patron tiene un equivalente en su grupo.
        BancoCola.Paso conEq = null;
        String eq = null;
        for (BancoCola.Paso p : q.pasos) {
            if (!"PATRON".equals(p.tipo) || ProtocoloDisparos.deAjuste(p) || REPETIR.contains(p.patron)) {
                continue;
            }
            for (String e : g.equivalentes(p.patron)) {
                if (!e.equals(p.patron)) {
                    conEq = p;
                    eq = e;
                }
            }
            if (conEq != null) {
                break;
            }
        }
        assertNotNull("hay algun grupo con dos patrones", conEq);
        medir(c, eq, 1500, 1, 4);
        // Un patron de lo que se escribe (no de los que se repiten) medido a 3 x 3: no cuenta.
        BancoCola.Paso aj8 = null;
        for (BancoCola.Paso p : q.pasos) {
            if (ProtocoloDisparos.deAjuste(p) && !REPETIR.contains(p.patron) && g.equivalentes(p.patron).size() <= 1) {
                aj8 = p;
                break;
            }
        }
        assertNotNull(aj8);
        medir(c, aj8.patron, 1200, 3, 3);
        int antes = BancoPrevio.patronesPendientes(q, c.pasos());
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, g, decisiones(), "SLV-002", "t");
        assertEquals(r.texto.toString(), "HECHO", c.pasos().get(conEq.orden));
        assertTrue(r.texto.toString(), r.texto.toString().contains(conEq.patron + " por " + eq));
        assertNull(c.pasos().get(aj8.orden));
        assertEquals(antes - r.hechos, BancoPrevio.patronesPendientes(q, c.pasos()));
        // idempotente
        assertEquals(0, BancoPrevio.aplicar(c, q, g, decisiones(), "SLV-002", "t").hechos);
        // a 5 x 4 si cuenta
        medir(c, aj8.patron, 1200, 5, 4);
        BancoPrevio.aplicar(c, q, g, decisiones(), "SLV-002", "t");
        assertEquals("HECHO", c.pasos().get(aj8.orden));
    }

    /** TIPO-I-REPETIR: lo que esta HECHO fuera de protocolo vuelve a la cola (serie ANULADA con motivo), una vez. */
    @Test
    public void loQueDiegoMandaRepetirVuelveALaCola() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        BancoCola.Paso p34 = paso(q, "P34");
        Campana.Serie s = medir(c, "P34", 900, 3, 3);
        c.anotarPaso(p34.orden, "HECHO", s.id, "t", "");
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t");
        assertEquals(1, r.repetir);
        assertEquals("REHACER", c.pasos().get(p34.orden));
        assertTrue(s.anulada, s.anulada.contains("TIPO-I-REPETIR"));
        assertEquals(0, BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t").repetir);
        // medido otra vez a 5 x 4: no se vuelve a pedir
        Campana.Serie b = medir(c, "P34", 900, 5, 4);
        c.anotarPaso(p34.orden, "HECHO", b.id, "t", "");
        assertEquals(0, BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t").repetir);
        assertEquals("HECHO", c.pasos().get(p34.orden));
    }

    /** El ZIP real de las 15:10 en las dos colas: cuanto queda (sale en el informe). */
    @Test
    public void elZipDeLas1510EnLasDosColas() throws Exception {
        byte[] zip = Files.readAllBytes(new File(ZIP_1510).toPath());
        String diario = ImportadorCampana.diarioDeZip(zip);
        Decisiones d = decisiones();
        for (BancoCola.Tipo t : new BancoCola.Tipo[]{BancoCola.Tipo.COMPLETO, BancoCola.Tipo.REPRESENTATIVO}) {
            BancoCola q = cola(t);
            Campana c = new Campana(catalogo, "SLV-002", MAC);
            c.escribirEn(new StringWriter());
            if (t == BancoCola.Tipo.REPRESENTATIVO) {
                c.elegirCola(t.name(), q.md5);
            }
            ImportadorCampana.Resultado ir = ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
            assertTrue(ir.texto(), ir.series >= 38);
            int total = BancoPrevio.patronesPendientes(q, new java.util.HashMap<Integer, String>());
            BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, grupos(), d, "SLV-002", "t");
            Map<Integer, String> est = c.pasos();
            int pend = BancoPrevio.patronesPendientes(q, est);
            double min = BancoPrevio.minutosPendientes(q, est, true, "PRECISO", REPETIR);
            System.out.println(String.format(Locale.US, "ZIP 15:10, banco %s: %d patrones en la cola, %d ya medidos "
                    + "cuentan, %d a repetir; quedan %d patrones, %.0f min", t, total, r.hechos, r.repetir, pend, min));
            assertTrue(pend < total);
            for (String p : REPETIR) {
                for (BancoCola.Paso x : q.pasos) {
                    if ("PATRON".equals(x.tipo) && p.equals(x.patron)) {
                        assertTrue(t + " " + p, !"HECHO".equals(est.get(x.orden)));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------- QA-3615-03: cola e importacion

    @Test
    public void cambiarDeColaDejaLosPasosSinEstadoYSeReproduce() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        StringWriter d = new StringWriter();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(d);
        c.elegirCola("COMPLETO", q.md5);
        Campana.Serie s = medir(c, "P1", 1500, 1, 4);
        c.anotarPaso(paso(q, "P1").orden, "HECHO", s.id, "t", "");
        assertFalse(c.pasos().isEmpty());
        c.elegirCola("REPRESENTATIVO", cola(BancoCola.Tipo.REPRESENTATIVO).md5);
        assertTrue(c.pasos().isEmpty());
        assertEquals(1, c.series().size());
        Campana r = new Campana(catalogo, "SLV-002", MAC);
        r.leerDiario(new java.io.StringReader(d.toString()));
        assertEquals("REPRESENTATIVO", r.colaTipo());
        assertTrue(r.pasos().isEmpty());
    }

    @Test
    public void importarConservaElTipoDeBancoOTraeSoloLasSeries() throws Exception {
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);
        StringWriter d = new StringWriter();
        Campana o = new Campana(catalogo, "SLV-002", MAC);
        o.escribirEn(d);
        o.elegirCola("REPRESENTATIVO", rep.md5);
        BancoCola.Paso p = null;
        for (BancoCola.Paso x : rep.pasos) {
            if ("PATRON".equals(x.tipo)) {
                p = x;
                break;
            }
        }
        Campana.Serie s = medir(o, p.patron, 1500, 1, 4);
        o.anotarPaso(p.orden, "HECHO", s.id, "t", "");
        // destino sin cola: adopta REPRESENTATIVO con sus pasos
        Campana a = new Campana(catalogo, "SLV-002", MAC);
        a.escribirEn(new StringWriter());
        ImportadorCampana.importarDiario(a, d.toString(), catalogo, "zip");
        assertEquals("REPRESENTATIVO", a.colaTipo());
        assertEquals("HECHO", a.pasos().get(p.orden));
        // RTV 1.0.0-rc6 (D-1). Hasta la rc5 esta prueba aseveraba que un destino en COMPLETO SIN trabajo propio
        // se quedaba en COMPLETO y no recibia ni un paso. Eso es lo que rompio en campo la noche del 19-sep: el
        // codigo 8 salia "no calibrable" y quedaban 81 patrones por medir (109 min), medido en Rc6DefectosTest.
        // La prueba consagraba el defecto, asi que se corrige el valor esperado, no el arreglo.
        //
        // destino COMPLETO SIN trabajo propio: adopta el banco del ZIP, que es con el que se midio lo que trae
        Campana b = new Campana(catalogo, "SLV-002", MAC);
        b.escribirEn(new StringWriter());
        b.elegirCola("COMPLETO", cola(BancoCola.Tipo.COMPLETO).md5);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(b, d.toString(), catalogo, "zip");
        assertEquals("REPRESENTATIVO", b.colaTipo());
        assertEquals(1, b.series().size());
        assertEquals("HECHO", b.pasos().get(p.orden));
        assertEquals("REPRESENTATIVO", r.bancoAdoptado);

        // destino COMPLETO CON trabajo propio: no se pisa; trae las series, no los pasos, y lo dice
        Campana b2 = new Campana(catalogo, "SLV-002", MAC);
        b2.escribirEn(new StringWriter());
        b2.elegirCola("COMPLETO", cola(BancoCola.Tipo.COMPLETO).md5);
        b2.anotarPaso(1, "HECHO", "", "t", "calentamiento");
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(b2, d.toString(), catalogo, "zip");
        assertEquals("COMPLETO", b2.colaTipo());
        assertEquals(1, b2.series().size());
        assertEquals("el paso propio sigue ahí", "HECHO", b2.pasos().get(1));
        assertEquals("y no entra ninguno del ZIP", null, b2.pasos().get(p.orden));
        assertEquals("REPRESENTATIVO", r2.bancoPorResolver);
        assertTrue(r2.texto(), r2.avisos.toString().contains("no entra es el AVANCE del banco"));
    }

    // ---------------------------------------------------------- RF-APP-50 y C-P14-3

    @Test
    public void elResumenDelZipLigeroCabeEn10kB() throws Exception {
        byte[] zip = Files.readAllBytes(new File(ZIP_1510).toPath());
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        ImportadorCampana.importarDiario(c, ImportadorCampana.diarioDeZip(zip), catalogo, "zip 15:10");
        int completo = c.resumen().getBytes(StandardCharsets.UTF_8).length;
        int corto = c.resumenCorto().getBytes(StandardCharsets.UTF_8).length;
        System.out.println("resumen completo " + completo + " B, corto " + corto + " B");
        assertTrue(corto + " B", corto <= Campana.TOPE_RESUMEN_CORTO);
        assertTrue(corto < completo);
        assertTrue(c.resumenCorto().contains("Falta medir"));
    }

    @Test
    public void laSrepEsDeLaA5DeLaSesionDelCodigo() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        for (BancoCola.Paso p : q.pasos) {
            if ("A5".equals(p.tipo) && "A5-INICIO".equals(p.bloque) && p.sesion == 1) {
                Campana.Serie s = c.nuevaSerie("t", "SLV-002", MAC, "V3.6", p.patron, 0, 'e');
                double[] m = {0.98, 0.99, 1.0, 1.01, 1.02};
                for (int k = 0; k < 5; k++) {
                    for (int i = 0; i < 4; i++) {
                        c.agregarDisparo(s, k + 1, "f", "::1", p.xEsperada * m[k]);
                    }
                }
                c.cerrar(s, A5.VEREDICTO, false, "");
                c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
            }
        }
        Anclas.Valor v1 = Anclas.sRep(q, c, 1);
        assertTrue(v1.texto, v1.texto.contains("sesión 1"));
        Anclas.Valor v2 = Anclas.sRep(q, c, 2);
        assertTrue(v2.texto, v2.texto.contains("la sesión 2 no tiene A5 del inicio medida"));
        assertEquals(v1.valor, v2.valor, 1e-12);
    }

    @Test
    public void elTiempoPorPasoSaleDelPlan() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        BancoCola.Paso p = paso(q, "P1");
        double s14 = BancoCola.segundos(p, new int[]{1, 4});
        double s54 = BancoCola.segundos(p, new int[]{5, 4});
        assertEquals(20 + 1 * (4 + Math.max(1, p.asentamiento)) * 1.5, s14, 1e-9);
        assertEquals(20 + 5 * (4 + Math.max(1, p.asentamiento)) * 1.5 + 4 * 11, s54, 1e-9);
        assertTrue(BancoPrevio.textoPendiente(q, new java.util.HashMap<Integer, String>(), true, "PRECISO", REPETIR)
                .startsWith("Quedan "));
    }
}
