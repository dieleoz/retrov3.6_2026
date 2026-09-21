package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Parser genérico de §3 de SPEC-App-Usuario-V3.6.md: "#...#" y "#ERR,<motivo>#". Fuente del formato
 * de motivo: PROTOCOLO-V3.6.md:70-72 ("motivo es texto corto sin comas: PIN, BLOQUEADO, FORMATO,
 * EEPROM"). Sin ficha T-USR propia (T-USR-01b y Sonda362Test lo ejercitan de forma indirecta); estas
 * pruebas fijan el comportamiento del parser en sí, no un requisito citado aparte.
 */
public class RespuestaTramaTest {

    @Test
    public void reconoceUnaRespuestaOkDeTresCampos() {
        RespuestaTrama t = RespuestaTrama.analizar("#GN,SLV-002#");
        assertFalse(t.esInvalida());
        assertFalse(t.esError());
        assertEquals("GN", t.campo(0));
        assertEquals("SLV-002", t.campo(1));
        assertEquals(2, t.numeroCampos());
    }

    @Test
    public void reconoceErrFormatoConSuMotivo() {
        RespuestaTrama t = RespuestaTrama.analizar("#ERR,FORMATO#");
        assertTrue(t.esError());
        assertTrue(t.esErrorFormato());
        assertEquals("FORMATO", t.motivoError());
    }

    /** "El parser no debe fallar si llega otro motivo" (SPEC §3): PIN, BLOQUEADO, EEPROM se
     *  reconocen como ERR, pero no como esErrorFormato(). */
    @Test
    public void reconoceOtrosMotivosDeErrorSinFallarNiConfundirlosConFormato() {
        for (String motivo : new String[] {"PIN", "BLOQUEADO", "EEPROM"}) {
            RespuestaTrama t = RespuestaTrama.analizar("#ERR," + motivo + "#");
            assertTrue(motivo, t.esError());
            assertFalse(motivo, t.esErrorFormato());
            assertEquals(motivo, t.motivoError());
        }
    }

    @Test
    public void unaCadenaSinLosDosNumeralesEsInvalida() {
        assertTrue(RespuestaTrama.analizar("GN,SLV-002").esInvalida());
        assertTrue(RespuestaTrama.analizar("#GN,SLV-002").esInvalida());
        assertTrue(RespuestaTrama.analizar("").esInvalida());
        assertTrue(RespuestaTrama.analizar("#").esInvalida());
    }

    /** QA-9: convierte esta prueba en una que asevera (CLAUDE.md §7, "una prueba que no asevera no
     *  cuenta"): antes, sólo confiaba en que JUnit marca fallo si algo lanza dentro del test, sin
     *  comprobar el ESTADO que resuelve cada entrada rara. Cada caso, con lo que espera §3 de la SPEC. */
    @Test
    public void nuncaLanzaUnaExcepcionYCadaEntradaRaraResuelveElEstadoEsperado() {
        // "El parser no debe fallar": estas entradas no tienen forma de "#...#" completa y valida,
        // o el primer campo viene vacio: todas invalidas.
        assertTrue(RespuestaTrama.analizar(null).esInvalida());
        assertTrue(RespuestaTrama.analizar("").esInvalida());
        assertTrue(RespuestaTrama.analizar("#").esInvalida());
        assertTrue(RespuestaTrama.analizar("##").esInvalida()); // cuerpo vacio: campos[0] vacio.
        assertTrue(RespuestaTrama.analizar("#,#").esInvalida()); // primer campo vacio.
        assertTrue(RespuestaTrama.analizar("basura sin numerales").esInvalida());

        // "#ERR#" SÍ tiene forma valida de "#...#": es un ERR con motivo vacio, no invalida (distinto
        // de las entradas de arriba, que no llegan ni a reconocerse como "#...#" bien formada).
        RespuestaTrama err = RespuestaTrama.analizar("#ERR#");
        assertFalse(err.esInvalida());
        assertTrue(err.esError());
        assertFalse(err.esErrorFormato());
        assertEquals("", err.motivoError());

        // "#V,3.6,2026-09-19,CAL#": 4 campos validos (le falta la mascara, RespuestaV la acepta con 4
        // o 5, T-USR-22(c)); este parser generico sólo ve los campos, ninguno vacio.
        RespuestaTrama v = RespuestaTrama.analizar("#V,3.6,2026-09-19,CAL#");
        assertFalse(v.esInvalida());
        assertEquals(4, v.numeroCampos());
        assertEquals("V", v.campo(0));
    }
}
