package com.dpi.retrov36;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * App 3.6.17: REVISION-P15-QA-3.6.16.md (ef7ea10) y las peticiones de Diego del 19-sep noche. Banco en
 * REPRESENTATIVO por defecto y cambio de cola en cualquier momento sin perder series; B-01 (equivalentes solo de
 * fuera de la cola y nunca de un excluido); B-03 (los 7 tipo I, ANULADOS con motivo); S-01 (el revierte viaja);
 * PRECISO-5x9 firmada. El flujo (F-01, F-02, F-03, #SC y P14-B02) esta en FlujoCalibracionTest.
 */
public class Version3617Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String ZIP_1510 = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    private static final List<String> REP = Arrays.asList("P34", "P37", "P43", "P44", "P38", "P39", "P49");
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

    /** La campana tal como la deja la 3.6.16 al importar el ZIP de las 15:10 (sin evento COLA). */
    private Campana campana1510(StringWriter w) throws Exception {
        String diario = ImportadorCampana.diarioDeZip(Files.readAllBytes(new File(ZIP_1510).toPath()));
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(w);
        ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
        return c;
    }

    private static Campana.Serie medir(Campana c, String patron, double x, int k, int m) throws Exception {
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int j = 1; j <= k; j++) {
            for (int i = 0; i < m; i++) {
                c.agregarDisparo(s, j, "t", "::" + Math.round(x), x + (i % 2 == 0 ? 0.5 : -0.5));
            }
        }
        c.cerrar(s, "OK", true, "");
        c.elegir(s);
        return s;
    }

    // ---------------------------------------------------------------- Representativo por defecto

    /** Abrir el banco con la campana del ZIP de las 15:10: REPRESENTATIVO y "Quedan 51". */
    @Test
    public void conElZipDeLas1510ElBancoAbreEnRepresentativoYQuedan51() throws Exception {
        Campana c = campana1510(new StringWriter());
        // la importacion adopta la cola COMPLETO del ZIP (sin "manual"): no cuenta como eleccion del operador
        BancoCola.Tipo t = BancoPrevio.tipoPorDefecto(c, false);
        assertEquals(BancoCola.Tipo.REPRESENTATIVO, t);
        BancoCola q = cola(t);
        c.elegirCola(t.name(), q.md5);
        BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t");
        String txt = BancoPrevio.textoPendiente(q, c.pasos(), true, "PRECISO", REP);
        assertTrue(txt, txt.startsWith("Quedan 51 patrones"));
        // Un equipo sin nada medido empieza por el completo; un COMPLETO elegido a mano se respeta.
        Campana nueva = new Campana(catalogo, "SLV-002", MAC);
        nueva.escribirEn(new StringWriter());
        assertEquals(BancoCola.Tipo.COMPLETO, BancoPrevio.tipoPorDefecto(nueva, false));
        assertEquals(BancoCola.Tipo.REPRESENTATIVO, BancoPrevio.tipoPorDefecto(nueva, true));
        StringWriter w = new StringWriter();
        Campana m = campana1510(w);
        m.elegirCola("COMPLETO", cola(BancoCola.Tipo.COMPLETO).md5, true);
        assertEquals(BancoCola.Tipo.COMPLETO, BancoPrevio.tipoPorDefecto(m, false));
        Campana r = new Campana(catalogo, "SLV-002", MAC);
        int malas = r.leerDiario(new StringReader(w.toString()));
        String dw = w.toString();
        assertTrue(malas + " " + dw.substring(Math.max(0, dw.length() - 300)), r.colaManual());
    }

    /**
     * Diario de la 3.6.16 en COMPLETO, parado en el paso 12; se instala la 3.6.17 y se pasa a REPRESENTATIVO: quedan
     * unos 51 y no se pierde ninguna serie. El paso actual se recalcula en la cola nueva.
     */
    @Test
    public void unBancoCompletoEmpezadoPasaARepresentativoSinPerderSeries() throws Exception {
        Campana c = campana1510(new StringWriter());
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        c.elegirCola("COMPLETO", comp.md5);          // la 3.6.16 lo anotaba sin "manual"
        for (BancoCola.Paso p : comp.pasos) {
            if (p.orden > 12) {
                break;
            }
            if (p.esMedida()) {
                String pat = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
                Campana.Serie s = medir(c, pat, p.xEsperada > 0 ? p.xEsperada : 1500, 1, 4);
                c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
            } else {
                c.anotarPaso(p.orden, "HECHO", "", "t", "");
            }
        }
        int series = c.series().size();
        assertEquals(BancoCola.Tipo.REPRESENTATIVO, BancoPrevio.tipoPorDefecto(c, false));
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);
        c.elegirCola("REPRESENTATIVO", rep.md5);
        BancoPrevio.aplicar(c, rep, grupos(), decisiones(), "SLV-002", "t");
        assertEquals("ninguna serie se pierde", series, c.series().size());
        int quedan = BancoPrevio.patronesPendientes(rep, c.pasos());
        assertTrue("quedan " + quedan, quedan <= 51 && quedan >= 40);
        BancoCola.Paso sig = rep.siguiente(c.pasos(), 0);
        assertTrue(sig != null && sig.orden >= 1);
        System.out.println("3.6.16 COMPLETO paso 12 -> REPRESENTATIVO: " + BancoPrevio.textoPendiente(rep, c.pasos(), true,
                "PRECISO", REP) + "; siguiente paso " + sig.orden + " " + sig.tipo + " " + sig.patron);
    }

    @Test
    public void enElCompletoSeMarcanLosEquivalentes() throws Exception {
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        BancoPrevio.Grupos g = grupos();
        int marcados = 0;
        for (BancoCola.Paso p : comp.pasos) {
            if ("PATRON".equals(p.tipo) && !BancoPrevio.marcaEquivalente(comp, g, p.patron).isEmpty()) {
                marcados++;
                assertTrue(BancoPrevio.marcaEquivalente(comp, g, p.patron).startsWith(" (equivalente a P"));
            }
        }
        assertTrue(marcados > 0);
    }

    // ---------------------------------------------------------------- B-01 y B-03

    /** B-01: P127 (AJUSTE del 5) no cuenta con la serie de P81 (excluido), ni P19 con la de P18 (paso de la cola). */
    @Test
    public void b01LosEquivalentesNoSaltanEntrePasosDeLaCola() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        c.elegirCola("REPRESENTATIVO", q.md5);
        Campana.Serie s81 = medir(c, "P81", 846, 5, 4);
        c.anotarPaso(82, "HECHO", s81.id, "t", "");
        Campana.Serie s18 = medir(c, "P18", 813, 5, 4);
        c.anotarPaso(84, "HECHO", s18.id, "t", "");
        assertEquals("P127", q.paso(83).patron);
        assertEquals("P19", q.paso(85).patron);
        for (int i = 0; i < 3; i++) {                       // salir y entrar del Banco varias veces
            BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t");
        }
        assertEquals(null, c.pasos().get(83));
        assertEquals(null, c.pasos().get(85));
        assertTrue(decisiones().excluidosDe("SLV-002").contains("P81"));
    }

    /** B-03: con el ZIP de las 15:10 los 7 tipo I vuelven a la cola con su serie ANULADA y el motivo, una vez. */
    @Test
    public void b03LosSieteTipoIQuedanAnuladosConMotivo() throws Exception {
        Campana c = campana1510(new StringWriter());
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        c.elegirCola("REPRESENTATIVO", q.md5);
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t");
        assertEquals(r.texto.toString(), 7, r.anuladas);
        int conMotivo = 0;
        for (Campana.Serie s : c.seriesAnuladas()) {
            if (REP.contains(s.patron) && s.anulada.contains("TIPO-I-REPETIR")) {
                conMotivo++;
            }
        }
        assertTrue(conMotivo >= 7);
        for (String p : REP) {
            Campana.Serie e = c.elegida(p);
            assertTrue(p, e == null || ProtocoloDisparosCompat.cumple(p, e));
        }
        assertEquals(0, BancoPrevio.aplicar(c, q, grupos(), decisiones(), "SLV-002", "t").anuladas);
    }

    /** Puente para no depender del nombre de la clase de protocolo de disparos. */
    static final class ProtocoloDisparosCompat {
        static boolean cumple(String p, Campana.Serie s) {
            return Protocolo.incumple(p, s, new int[]{5, 4}) == null;
        }
    }

    // ---------------------------------------------------------------- S-01, decisiones

    @Test
    public void s01ElRevierteViajaAOtraCampana() throws Exception {
        Campana a = new Campana(catalogo, "SLV-002", MAC);
        a.escribirEn(new StringWriter());
        a.renombrar("SLV-002", "SLV-002-2026", "t1", "Diego");
        a.revertirRenombrado("SLV-002-2026", "SLV-002", "t2", "#SN no entró");
        assertEquals("SLV-002", a.serieActual());
        Campana b = new Campana(catalogo, "SLV-002", MAC);
        b.escribirEn(new StringWriter());
        assertEquals(2, b.copiarRenombrados(a.renombrados(), "otra campaña"));
        assertEquals("SLV-002", b.serieActual());
        assertEquals(0, b.copiarRenombrados(a.renombrados(), "otra campaña"));
        // y por el diario (importar en otro telefono)
        StringWriter w = new StringWriter();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(w);
        c.renombrar("SLV-002", "SLV-002-2026", "t1", "Diego");
        c.revertirRenombrado("SLV-002-2026", "SLV-002", "t2", "no entró");
        Campana d = new Campana(catalogo, "SLV-002", MAC);
        d.leerDiario(new StringReader(w.toString()));
        assertEquals(2, d.renombrados().size());
        Campana e = new Campana(catalogo, "SLV-002", MAC);
        e.escribirEn(new StringWriter());
        e.copiarRenombrados(d.renombrados(), "zip");
        assertEquals("SLV-002", e.serieActual());
    }

    @Test
    public void s02UnRevierteFalsoSeCorrigeAlRepetirElCambio() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        assertTrue(FlujoCalibracion.renombrarSerie(sim, c, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t")
                .startsWith("Serie cambiada"));
        c.revertirRenombrado("SLV-002-2026", "SLV-002", "t", "revierte falso");
        assertNotEquals("SLV-002-2026", c.serieActual());
        String t = FlujoCalibracion.renombrarSerie(sim, c, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t");
        assertTrue(t, t.startsWith("El equipo ya tiene la serie SLV-002-2026"));
        assertEquals("SLV-002-2026", c.serieActual());
    }

    @Test
    public void laReglaPreciso5x9EstaFirmada() throws Exception {
        Decisiones d = decisiones();
        assertTrue(d.rechazadas().toString(), d.rechazadas().isEmpty());
        assertTrue(d.tomada("PRECISO-5x9", "SLV-002"));
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        assertTrue(ProtocoloDisparosCompat.cumple("P28", medir(c, "P28", 1000, 5, 9)));
        assertFalse(ProtocoloDisparosCompat.cumple("P34", medir(c, "P34", 1000, 3, 3)));
    }
}
