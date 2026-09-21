package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Condición C1 (arquitecto-iot sobre RetroUsuario 0.3.1, MainActivity.java:147-154 de 766e6f2): la
 * guarda de cambio de equipo se sacó a {@link GestorEnlace}, dominio puro, para poder aseverar los
 * cuatro casos que describe la condición sin Bluetooth real. Vistas en rojo por comportamiento
 * contra 766e6f2: esa clase no existe en ese commit ("cannot find symbol").
 */
public class GestorEnlaceTest {

    private static final String MAC_A = "AA:BB:CC:DD:EE:01";
    private static final String MAC_B = "AA:BB:CC:DD:EE:02";

    /** No compatible → el operador reselecciona el MISMO equipo: el enlace de la sonda fallida
     *  (NO_COMPATIBLE) sigue vivo (el equipo no cerró el socket sólo por no ser 3.6.x): se reutiliza,
     *  no se abre un segundo RFCOMM. */
    @Test
    public void noCompatibleYMismoEquipoReutiliza() {
        assertEquals(GestorEnlace.Decision.REUTILIZAR, GestorEnlace.decidir(MAC_A, true, MAC_A));
    }

    /** Reintentos de la sonda agotados (SIN_RESPUESTA_REINTENTAR sin más reintentos) → el operador
     *  reselecciona el MISMO equipo: mismo caso que el anterior, mismo enlace vivo, se reutiliza. */
    @Test
    public void reintentosAgotadosYMismoEquipoReutiliza() {
        assertEquals(GestorEnlace.Decision.REUTILIZAR, GestorEnlace.decidir(MAC_A, true, MAC_A));
    }

    /** El operador elige OTRO equipo (distinta MAC): el enlace anterior, aunque siga vivo, se cierra
     *  antes de conectar el nuevo — nunca conviven dos sockets RFCOMM. */
    @Test
    public void otroEquipoCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, true, MAC_B));
    }

    /** Giro de pantalla con sesión viva: el enlace sigue en {@code SesionHolder} (sobrevive a que la
     *  Activity se recree) y el operador reselecciona el MISMO equipo de la lista: se reutiliza, sin
     *  abrir un enlace nuevo que rompería la sesión en curso. */
    @Test
    public void giroConSesionVivaYMismoEquipoReutiliza() {
        assertEquals(GestorEnlace.Decision.REUTILIZAR, GestorEnlace.decidir(MAC_A, true, MAC_A));
    }

    /** Giro de pantalla con sesión viva, pero el operador elige OTRO equipo: se cierra el de la
     *  sesión anterior y se conecta al nuevo (A2). */
    @Test
    public void giroConSesionVivaYOtroEquipoCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, true, MAC_B));
    }

    /** El enlace anterior ya no está vivo (el socket se cayó solo, sin que el operador cambiara de
     *  equipo): aunque la MAC coincida, no se reutiliza un socket muerto — se cierra (no-op, ya
     *  está roto) y se conecta uno nuevo. */
    @Test
    public void enlaceAnteriorMuertoConMismaMacCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, false, MAC_A));
    }

    /** Sin ningún enlace anterior (primera conexión de la sesión de la app): nada que cerrar. */
    @Test
    public void sinEnlaceAnteriorConectaNuevo() {
        assertEquals(GestorEnlace.Decision.CONECTAR_NUEVO, GestorEnlace.decidir(null, false, MAC_A));
    }
}
