package com.dpi.retrov36;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tramas partidas en varios read(), como las muestra el registro real. */
public class ReceptorTest {

    private static byte[] b(int... v) {
        byte[] r = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = (byte) v[i];
        }
        return r;
    }

    @Test
    public void medidaPartidaComoEnElRegistroReal() {
        Receptor r = new Receptor();
        r.agregar(1000, b(0x3A));                 // ":"
        assertNull(r.completa(Tramas.Tipo.MEDIDA, 1010));
        r.agregar(1020, b(0x3A, 0x34, 0x32));     // ":42"
        assertNull("sin silencio no se cierra", r.completa(Tramas.Tipo.MEDIDA, 1100));
        assertEquals("::42", r.completa(Tramas.Tipo.MEDIDA, 1020 + Receptor.SILENCIO_MS));
        assertEquals(Receptor.Desenlace.VALIDA, r.desenlace(Tramas.Tipo.MEDIDA, 1300));
    }

    @Test
    public void noCortaUnNumeroQueSigueLlegando() {
        Receptor r = new Receptor();
        r.agregar(0, "::12".getBytes());
        assertNull(r.completa(Tramas.Tipo.MEDIDA, 100));
        r.agregar(120, "34".getBytes());
        assertNull(r.completa(Tramas.Tipo.MEDIDA, 250));
        assertEquals("::1234", r.completa(Tramas.Tipo.MEDIDA, 120 + Receptor.SILENCIO_MS));
    }

    @Test
    public void adminSeCierraPorSuAlmohadilla() {
        Receptor r = new Receptor();
        r.agregar(0, "#V,3.".getBytes());
        assertNull(r.completa(Tramas.Tipo.ADMIN, 1));
        r.agregar(5, "6,2026-09-20,DEF#".getBytes());
        assertEquals("#V,3.6,2026-09-20,DEF#", r.completa(Tramas.Tipo.ADMIN, 6));
    }

    @Test
    public void tresDesenlaces() {
        Receptor r = new Receptor();
        assertEquals(Receptor.Desenlace.TIMEOUT, r.desenlace(Tramas.Tipo.MEDIDA, 5000));
        r.agregar(10, "xyz".getBytes());
        assertEquals(Receptor.Desenlace.INESPERADA, r.desenlace(Tramas.Tipo.MEDIDA, 5000));
        r.vaciar();
        r.agregar(10, "::7".getBytes());
        assertEquals(Receptor.Desenlace.VALIDA, r.desenlace(Tramas.Tipo.MEDIDA, 5000));
    }

    @Test
    public void acotado() {
        Receptor r = new Receptor();
        byte[] mucho = new byte[Receptor.MAX_BYTES + 100];
        r.agregar(0, mucho);
        assertTrue(r.desbordado());
        assertEquals(Receptor.MAX_BYTES, r.texto().length());
    }
}
