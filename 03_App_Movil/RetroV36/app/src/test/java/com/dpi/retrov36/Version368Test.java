package com.dpi.retrov36;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** App 3.6.8: condiciones (b) de REVISION-Arquitectura-P9-V3.6.md del lado de la app. */
public class Version368Test {

    private static List<Patron> catalogo() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P31.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    // ------------------------------------------------------------------ B13

    @Test
    public void b13OscuroConLasCurvasDelArquitecto() {
        // §4 bis: blanco grado 2 da ~219 en x = 575 (fabrica 25): rechazado.
        Ecuacion blanco2 = new Ecuacion(0, 5.947e-5, -3.694e-3, 201.13);
        String m = Asistente.comprobarOscuro(blanco2, '1', 575, 10, 25);
        assertNotNull(m);
        assertTrue(m, m.contains("R = 219"));
        // Blanco grado 1: ~9 en oscuro: pasa.
        assertNull(Asistente.comprobarOscuro(new Ecuacion(0, 0, 0.298477, -162.28), '1', 575, 10, 25));
        // Amarillo grado 1: ~84 frente a 21 de fabrica: tambien lo para el umbral por defecto...
        Ecuacion amarillo1 = new Ecuacion(0, 0, 0.318077, -98.64);
        assertNotNull(Asistente.comprobarOscuro(amarillo1, '2', 575, 10, 25));
        // ...y es configurable (lo decide Diego por escrito, P9-B13).
        assertNull(Asistente.comprobarOscuro(amarillo1, '2', 575, 10, 90));
    }

    // ------------------------------------------------------ acta, #SC y B8

    private static Acta acta() {
        return new Acta("SLV-002", "00:21:13:05:19:3B", "V3.6 (3.6.2)", 1, 9, 1, "2026-09-19T11:00:00-0500");
    }

    private static Acta.Remedida remedidaOk(Ecuacion e) {
        double[] x = {2000, 2001, 2002};
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            r[i] = e.respuestaFloat32((int) x[i]);
        }
        return Acta.evaluarRemedida("P2", Arrays.asList(r), Arrays.asList(x), e, 0.03);
    }

    @Test
    public void laFechaSoloSeGrabaAlAceptarElActa() {
        Ecuacion e = new Ecuacion(0, 0, 0.298477, -162.28);
        Acta a = acta();
        assertNotNull("sin codigos no se acepta", a.motivoNoAceptable());
        a.escrito('1', "#G,1,...#", e, "oscuro 9");
        // Tras #S verificado con #E, sin re-medida: ni se acepta (no hay #SC) ni se escribe otro codigo.
        assertTrue(a.motivoNoAceptable().contains("re-medida"));
        assertTrue(a.motivoNoEscribir('2').contains("P9-B8"));
        assertNull(a.motivoNoEscribir('1'));                 // repetir el mismo si se puede
        a.remedida('1', remedidaOk(e));
        assertNull(a.motivoNoAceptable());
        assertNull(a.motivoNoEscribir('2'));
        a.aceptar("2026-09-19T12:00:00-0500", "2026-09-19");
        assertTrue(a.cerrada());
        assertTrue(a.texto(), a.texto().contains("ACEPTADA"));
        assertTrue(a.texto(), a.texto().contains("#SC/#GC): 2026-09-19"));
        assertNotNull(a.motivoNoEscribir('2'));             // cerrada
    }

    @Test
    public void actaRechazadaNoLlevaFecha() {
        Acta a = acta();
        a.escrito('1', "#G,1,...#", new Ecuacion(0, 0, 0.3, -160), "");
        a.rechazar("2026-09-19T12:00:00-0500", "re-medida fuera");
        assertTrue(a.cerrada());
        assertTrue(a.texto().contains("RECHAZADA"));
        assertFalse(a.texto().contains("#SC/#GC)"));
        try {
            a.aceptar("f", "2026-09-19");
            org.junit.Assert.fail("se acepto un acta rechazada");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("cerrada"));
        }
    }

    // ------------------------------------------------------------------ B3

    @Test
    public void b3ProtocoloDeLaCampanaEnElActa() throws Exception {
        Campana c = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        assertEquals(Arrays.asList(1, 9), Arrays.asList(c.protocolo()[0], c.protocolo()[1]));   // sin series
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", "P2", 0, 'e');
        for (int k = 1; k <= 3; k++) {
            for (int i = 0; i < 3; i++) {
                c.agregarDisparo(s, k, "f", "::2000", 2000 + i);
            }
        }
        c.cerrar(s, "OK", true, "");
        assertEquals(3, c.protocolo()[0]);
        assertEquals(3, c.protocolo()[1]);
        Acta a = new Acta("SLV-002", "m", "fw", 3, 3, 1, "f");
        assertTrue(a.texto(), a.texto().contains("3 colocaciones × 3 disparos, 1 de asentamiento"));
    }

    // --------------------------------------------------------------- B5 / RF-CAL-18

    @Test
    public void b5LaReMedidaUsaLaSEntreColocaciones() {
        Ecuacion e = new Ecuacion(0, 0, 0.3, -160);                  // R(2000) = 440
        // Tres colocaciones con medias de R 430, 440, 451 (s_rep 10,5) y x fijas en 2000.
        List<double[]> r = Arrays.asList(new double[]{430, 430}, new double[]{440, 440}, new double[]{450, 452});
        List<double[]> x = Arrays.asList(new double[]{2000, 2000}, new double[]{2000, 2000}, new double[]{2000, 2000});
        Acta.Remedida m = Acta.evaluarRemedida("P2", r, x, e, 0.03);
        assertEquals(10.5, m.sRep, 0.05);                         // medias 430, 440, 451
        assertEquals(2 * m.sRep, m.tolerancia, 1e-9);                // no la s dentro (~1)
        assertTrue(m.texto, m.ok);                                   // 440,3 frente a 440
        // Desplazado 30: fuera de 2 s_rep.
        List<double[]> r2 = Arrays.asList(new double[]{460, 460}, new double[]{470, 470}, new double[]{480, 482});
        assertFalse(Acta.evaluarRemedida("P2", r2, x, e, 0.03).ok);
    }

    // ------------------------------------------------------------------ A5

    private static void serie(Campana c, String patron, String veredicto, boolean aceptada, double[][] g) throws Exception {
        Campana.Serie s = c.nuevaSerie("f", "SLV-002", "00:21:13:05:19:3B", "V3.6", patron, 0, 'e');
        for (int k = 0; k < g.length; k++) {
            for (double v : g[k]) {
                c.agregarDisparo(s, k + 1, "f", "::" + (int) v, v);
            }
        }
        c.cerrar(s, veredicto, aceptada, "");
    }

    private static double[][] cinco(double m) {
        return new double[][]{{m - 10, m - 9, m - 11}, {m + 20, m + 21, m + 19}, {m, m + 1, m - 1},
                {m - 20, m - 21, m - 19}, {m + 10, m + 9, m + 11}};
    }

    @Test
    public void a5PuenteYCriterioDeReversion() throws Exception {
        Campana c = new Campana(catalogo(), "SLV-002", "00:21:13:05:19:3B");
        // Series de la campana (1 x 9): P22 1467, P28 2280, P4 3320.
        serie(c, "P22", "OK", true, new double[][]{{1465, 1467, 1469}});
        serie(c, "P28", "OK", true, new double[][]{{2278, 2280, 2282}});
        serie(c, "P4", "OK", true, new double[][]{{3318, 3320, 3322}});
        assertEquals("PENDIENTE", A5.evaluar(c).veredicto);
        serie(c, "P22", A5.VEREDICTO, false, cinco(1480));
        serie(c, "P28", A5.VEREDICTO, false, cinco(2250));
        serie(c, "P4", A5.VEREDICTO, false, cinco(3350));
        A5.Resultado r = A5.evaluar(c);
        assertEquals(r.texto, "OK", r.veredicto);
        assertTrue(r.texto, r.texto.contains("s_rep"));
        // Las series A5 no entran en el ajuste ni cambian la elegida.
        assertEquals(1467, c.elegida("P22").media(), 1e-9);
        assertEquals(1, c.seriesDe("P22").size());
        // Los tres desplazados hacia abajo mas de max(2 s_rep ; 4 %): revertir a la 3.6.1.
        serie(c, "P22", A5.VEREDICTO, false, cinco(1467 * 0.93));
        serie(c, "P28", A5.VEREDICTO, false, cinco(2280 * 0.93));
        serie(c, "P4", A5.VEREDICTO, false, cinco(3320 * 0.93));
        A5.Resultado rv = A5.evaluar(c);
        assertEquals(rv.texto, "REVERTIR", rv.veredicto);
        assertTrue(rv.texto.contains("3.6.1"));
        // Solo uno fuera: no concluyente, no revierte.
        serie(c, "P22", A5.VEREDICTO, false, cinco(1480));
        serie(c, "P28", A5.VEREDICTO, false, cinco(2250));
        assertEquals("NO_CONCLUYENTE", A5.evaluar(c).veredicto);
    }
}
