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

    @Test
    public void nuncaLanzaUnaExcepcionConEntradaRara() {
        // "El parser no debe fallar": ninguna de estas entradas puede lanzar.
        String[] entradas = {null, "", "#", "##", "#,#", "#ERR#", "basura sin numerales",
                "#V,3.6,2026-09-19,CAL#" /* le falta la mascara: 4 campos, no 5 */};
        for (String e : entradas) {
            RespuestaTrama.analizar(e); // no debe lanzar
        }
    }
}
