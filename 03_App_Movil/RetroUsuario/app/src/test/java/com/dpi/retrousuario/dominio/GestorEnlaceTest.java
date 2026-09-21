package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Condición C1 (arquitecto-iot sobre RetroUsuario 0.3.1, MainActivity.java:147-154 de 766e6f2): la
 * guarda de cambio de equipo se sacó a {@link GestorEnlace}, dominio puro, para poder aseverar los
 * cuatro casos que describe la condición sin Bluetooth real. Vistas en rojo por comportamiento
 * contra 766e6f2: esa clase no existe en ese commit ("cannot find symbol").
 *
 * <p><b>B-2 (arquitecto-iot sobre 0.3.2):</b> hasta esta entrega había 7 métodos aquí para sólo 4
 * entradas distintas de {@link GestorEnlace#decidir} — tres pares repetían exactamente los mismos
 * tres argumentos bajo un nombre de escenario distinto (p. ej. "no compatible y mismo equipo" y
 * "giro con sesión viva y mismo equipo" llamaban ambos a {@code decidir(MAC_A, true, MAC_A)}), así
 * que en realidad probaban la misma línea dos o tres veces con etiquetas distintas. Se deja UN
 * método por entrada distinta, con las narrativas que aplican documentadas en su Javadoc, más una
 * quinta entrada que no estaba cubierta ({@code enlaceAnteriorMuertoConDistintaMac}).</p>
 */
public class GestorEnlaceTest {

    private static final String MAC_A = "AA:BB:CC:DD:EE:01";
    private static final String MAC_B = "AA:BB:CC:DD:EE:02";

    /** Entrada {@code decidir(MAC_A, true, MAC_A)}: el enlace anterior está vivo y el operador
     *  reselecciona el MISMO equipo. Se reutiliza, no se abre un segundo RFCOMM. Aplica a los tres
     *  escenarios narrativos que antes tenían un método cada uno: "no compatible y reselecciona el
     *  mismo" (el equipo no cerró el socket sólo por no ser 3.6.x), "reintentos de la sonda
     *  agotados y reselecciona el mismo", y "giro de pantalla con sesión viva y reselecciona el
     *  mismo" (el enlace sigue en {@link SesionHolder}, sobrevive a que la Activity se recree) —
     *  los tres llaman a {@code decidir} con los mismos tres argumentos. */
    @Test
    public void enlaceAnteriorVivoYMismoEquipoReutiliza() {
        assertEquals(GestorEnlace.Decision.REUTILIZAR, GestorEnlace.decidir(MAC_A, true, MAC_A));
    }

    /** Entrada {@code decidir(MAC_A, true, MAC_B)}: el enlace anterior está vivo pero el operador
     *  elige OTRO equipo. Se cierra el anterior antes de conectar el nuevo — nunca conviven dos
     *  sockets RFCOMM. Aplica tanto a elegir otro equipo directamente como a hacerlo tras un giro de
     *  pantalla con sesión viva (A2): mismos tres argumentos en los dos casos. */
    @Test
    public void enlaceAnteriorVivoYOtroEquipoCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, true, MAC_B));
    }

    /** Entrada {@code decidir(MAC_A, false, MAC_A)}: el enlace anterior ya no está vivo (el socket
     *  se cayó solo, sin que el operador cambiara de equipo) y la MAC coincide. No se reutiliza un
     *  socket muerto aunque la MAC sea la misma — se cierra (no-op, ya está roto) y se conecta uno
     *  nuevo. */
    @Test
    public void enlaceAnteriorMuertoConMismaMacCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, false, MAC_A));
    }

    /** Entrada {@code decidir(MAC_A, false, MAC_B)}: enlace anterior muerto Y el operador elige otro
     *  equipo — no cubierta antes de la B-2 (las 7 entradas viejas nunca combinaban "muerto" con
     *  "otra MAC"). Sigue siendo CERRAR_Y_CONECTAR, pero por una entrada genuinamente distinta a la
     *  anterior. */
    @Test
    public void enlaceAnteriorMuertoConDistintaMacCierraYConecta() {
        assertEquals(GestorEnlace.Decision.CERRAR_Y_CONECTAR, GestorEnlace.decidir(MAC_A, false, MAC_B));
    }

    /** Entrada {@code decidir(null, false, MAC_A)}: sin ningún enlace anterior (primera conexión de
     *  la sesión de la app). Nada que cerrar. */
    @Test
    public void sinEnlaceAnteriorConectaNuevo() {
        assertEquals(GestorEnlace.Decision.CONECTAR_NUEVO, GestorEnlace.decidir(null, false, MAC_A));
    }

    // --- Arq ALTO, segunda parte (sobre 0.3.2): GestorEnlace.debeReintentarConexionNueva. Las 4
    // combinaciones de la tabla de verdad, cada una una entrada distinta (sin B-2 en éstas).
    // debeReintentarConexionNueva no existe en 385fc92 ("cannot find symbol" si se copiara este
    // fichero solo contra ese commit; la condición pide explícitamente no usar eso como "rojo").
    // Demostración real: reutilizadoYSinRespuestaReintenta() se corrió primero invertida
    // (assertFalse en vez de assertTrue) contra el código YA corregido y dio, con JUnitCore:
    //   1) reutilizadoYSinRespuestaReintenta(com.dpi.retrousuario.dominio.GestorEnlaceTest)
    //   java.lang.AssertionError
    //       at org.junit.Assert.fail(Assert.java:87)
    //       at org.junit.Assert.assertFalse(Assert.java:65/75)
    //       at com.dpi.retrousuario.dominio.GestorEnlaceTest.reutilizadoYSinRespuestaReintenta(...)
    //   Tests run: 9, Failures: 1
    // — rojo por comportamiento, no por compilación; corregida de vuelta a assertTrue abajo (verde). ---

    /** Reutilizado + silencio real a "#V#": puede ser un socket muerto que `vivo()` todavía no supo
     *  detectar — toca reintentar con una conexión nueva antes de concluir "no compatible". */
    @Test
    public void reutilizadoYSinRespuestaReintenta() {
        assertTrue(GestorEnlace.debeReintentarConexionNueva(true, true));
    }

    /** Reutilizado pero SÍ hubo respuesta (otro firmware, "#ERR#"...): es una respuesta de verdad,
     *  no un enlace muerto — "no compatible" a la primera, sin reintentar. */
    @Test
    public void reutilizadoYConRespuestaNoReintenta() {
        assertFalse(GestorEnlace.debeReintentarConexionNueva(true, false));
    }

    /** Conexión nueva (no reutilizada) con silencio: nunca fue un enlace reutilizado que pudiera
     *  estar "muerto sin saberlo" — un socket recién abierto que no contesta es, sencillamente, un
     *  equipo no compatible (o inalcanzable). Este es también el caso de la SEGUNDA vuelta tras un
     *  primer reintento: ya no reutilizado, así que no hay una tercera vuelta ("UNA vez"). */
    @Test
    public void noReutilizadoYSinRespuestaNoReintenta() {
        assertFalse(GestorEnlace.debeReintentarConexionNueva(false, true));
    }

    /** Conexión nueva con respuesta: caso normal de "no compatible", sin nada que reintentar. */
    @Test
    public void noReutilizadoYConRespuestaNoReintenta() {
        assertFalse(GestorEnlace.debeReintentarConexionNueva(false, false));
    }
}
