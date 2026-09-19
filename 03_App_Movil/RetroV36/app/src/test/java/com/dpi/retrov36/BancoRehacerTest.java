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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 3.6.14, peticion de Diego: rehacer un patron del banco antes de exportar. La serie anulada queda en el
 * diario con su motivo (ANULA), sale del ajuste, de la A5 y de la s_rep, y el paso vuelve a la cola.
 * Nunca se borra nada. Se prueba sobre Campana y BancoCola, que es lo que hace "Rehacer" en BancoActivity.
 */
public class BancoRehacerTest {

    private static final String MAC = "00:21:13:05:19:3B";
    private List<Patron> catalogo;
    private BancoCola cola;
    private Campana c;
    private StringWriter diario;

    @Before
    public void preparar() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        cola = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
        c = new Campana(catalogo, "SLV-002", MAC);
        diario = new StringWriter();
        c.escribirEn(diario);
        // Los pasos anteriores a los del 8 (orden < 51), hechos: el banco va por ahi.
        for (BancoCola.Paso p : cola.pasos) {
            if (p.orden < 51) {
                c.anotarPaso(p.orden, "HECHO", "", "f", "");
            }
        }
    }

    /** Mide el paso como lo hace el banco: serie aceptada, elegida y paso HECHO. */
    private Campana.Serie medir(BancoCola.Paso p, double x, boolean a5) throws Exception {
        String patron = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int k = 1; k <= 3; k++) {
            for (int i = 0; i < 4; i++) {
                double v = x * (1 + 0.01 * (k - 2)) + (i % 2 == 0 ? 0.5 : -0.5);
                c.agregarDisparo(s, k, "f" + k + i, "::" + Math.round(v), v);
            }
        }
        if (a5) {
            c.cerrar(s, A5.VEREDICTO, false, "banco paso " + p.orden);
        } else {
            c.cerrar(s, "OK", true, "banco paso " + p.orden);
            c.elegir(s);
        }
        c.anotarPaso(p.orden, "HECHO", s.id, "f", "OK");
        return s;
    }

    private BancoCola.Paso paso(String patron) {
        for (BancoCola.Paso p : cola.pasos) {
            if ("PATRON".equals(p.tipo) && patron.equals(p.patron)) {
                return p;
            }
        }
        throw new AssertionError(patron);
    }

    @Test
    public void anularLaUltimaDevuelveElPasoALaCola() throws Exception {
        BancoCola.Paso p44 = paso("P44");
        BancoCola.Paso p37 = paso("P37");
        medir(p44, 900, false);
        Campana.Serie mal = medir(p37, 1500, false);          // papel equivocado
        assertEquals(Integer.valueOf(p37.orden), c.ultimoPasoRehacible());
        assertEquals(mal, c.elegida("P37"));
        c.rehacer(p37.orden, "papel equivocado: pedía P37, puse P40", "2026-09-20T09:10:00-0500");
        assertEquals("P37 sale del ajuste", null, c.elegida("P37"));
        assertEquals("REHACER", c.pasos().get(p37.orden));
        assertEquals(p37.orden, cola.siguiente(c.pasos()).orden);
        assertEquals("papel equivocado: pedía P37, puse P40", mal.anulada);
        // Se vuelve a medir: la nueva pasa a ser la elegida; la anulada sigue en el diario.
        Campana.Serie buena = medir(p37, 992, false);
        assertEquals(buena, c.elegida("P37"));
        assertTrue(diario.toString().contains("ANULA," + mal.id));
        assertTrue(c.resumen().contains("Series ANULADAS"));
        assertTrue(c.resumen().contains("papel equivocado: pedía P37, puse P40"));
        assertTrue(c.exportarCsv().contains("ANULADA: papel equivocado"));
    }

    @Test
    public void rehacerUnPasoIntermedio() throws Exception {
        BancoCola.Paso p44 = paso("P44");
        BancoCola.Paso p37 = paso("P37");
        BancoCola.Paso p34 = paso("P34");
        Campana.Serie s44 = medir(p44, 900, false);
        medir(p37, 992, false);
        medir(p34, 958, false);
        List<Integer> l = c.pasosRehacibles();
        assertEquals(3, l.size());
        assertEquals(Integer.valueOf(p34.orden), l.get(2));
        c.rehacer(p44.orden, "apoyo torcido", "t");
        assertNull(c.elegida("P44"));
        assertEquals("el primer pendiente es el rehecho", p44.orden, cola.siguiente(c.pasos()).orden);
        assertFalse(c.pasosRehacibles().contains(p44.orden));
        Campana.Serie nueva = medir(p44, 899, false);
        assertEquals(nueva, c.elegida("P44"));
        assertTrue(s44.anulada != null);
        assertEquals("el rehecho es ahora el ultimo", Integer.valueOf(p44.orden), c.ultimoPasoRehacible());
    }

    @Test
    public void laAnuladaNoEntraEnElAjusteNiEnLaA5NiEnLaSrep() throws Exception {
        // A5 del inicio: tres patrones, uno de ellos se anula.
        Campana.Serie anulada = null;
        for (BancoCola.Paso p : cola.pasos) {
            if ("A5".equals(p.tipo) && "A5-INICIO".equals(p.bloque) && p.sesion == 1) {
                Campana.Serie s = medir(p, p.xEsperada, true);
                if ("P22".equals(p.patron)) {
                    anulada = s;
                }
            }
        }
        Anclas.Valor antes = Anclas.sRep(cola, c);
        assertTrue(antes.texto, antes.texto.contains("3 patrones"));
        assertTrue(c.seriesA5("P22").contains(anulada));
        c.anular(anulada, "no era P22", "t");
        assertFalse(c.seriesA5("P22").contains(anulada));
        Anclas.Valor despues = Anclas.sRep(cola, c);
        assertTrue(despues.texto, despues.texto.contains("2 patrones"));
        // En el ajuste: medidasElegidas no la trae.
        Campana.Serie s43 = medir(paso("P43"), 1146, false);
        c.anular(s43, "papel equivocado", "t");
        for (Medida m : c.medidasElegidas()) {
            assertFalse(m.patron.nombre.equals("P43"));
        }
        assertTrue(c.medias("").keySet().stream().noneMatch(p -> p.nombre.equals("P43")));
        try {
            c.anular(s43, " ", "t");
            fail("sin motivo no se anula");
        } catch (IllegalArgumentException e) {
            // motivo obligatorio
        }
    }

    /** La app se cierra entre el ANULA y el PASO REHACER: al volver, el paso ya esta en la cola. */
    @Test
    public void reanudarTrasCerrarLaAppAMitadDelRehacer() throws Exception {
        medir(paso("P44"), 900, false);
        BancoCola.Paso p37 = paso("P37");
        Campana.Serie s = medir(p37, 1500, false);
        c.anular(s, "papel equivocado", "t");                      // la app muere aqui
        Campana b = new Campana(catalogo, "SLV-002", MAC);
        assertEquals(0, b.leerDiario(new StringReader(diario.toString())));
        assertEquals("REHACER", b.pasos().get(p37.orden));
        assertEquals(p37.orden, cola.siguiente(b.pasos()).orden);
        assertNull(b.elegida("P37"));
        assertEquals("papel equivocado", b.serie(s.id).anulada);
        // Y con los dos eventos, igual.
        c.anotarPaso(p37.orden, "REHACER", "", "t", "rehacer");
        Campana d = new Campana(catalogo, "SLV-002", MAC);
        assertEquals(0, d.leerDiario(new StringReader(diario.toString())));
        assertEquals(p37.orden, cola.siguiente(d.pasos()).orden);
        assertFalse(cola.calibrable("8", d.pasos()));
    }

    @Test
    public void importarUnZipConUnaAnuladaLaTraeAnulada() throws Exception {
        BancoCola.Paso p37 = paso("P37");
        Campana.Serie s = medir(p37, 1500, false);
        c.rehacer(p37.orden, "papel equivocado", "t");
        Campana otra = new Campana(catalogo, "SLV-002", MAC);
        otra.escribirEn(new StringWriter());
        ImportadorCampana.importarDiario(otra, diario.toString(), catalogo, "zip");
        Campana.Serie imp = otra.series().get(0);
        assertEquals("papel equivocado", imp.anulada);
        assertNull(otra.elegida("P37"));
        assertEquals("REHACER", otra.pasos().get(p37.orden));
        assertTrue(s.anulada != null);
    }
}
