package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Arq ALTO (sobre 0.3.2): antes, {@code EnlaceBluetooth.vivo()} delegaba entero en
 * {@code socket.isConnected()} (`EnlaceBluetooth.java:69-71` de 385fc92), que Android sólo pone a
 * {@code false} cuando la app llama {@code close()} — si el equipo se cae, el hilo lector traga la
 * {@code IOException} (`EnlaceBluetooth.java:82-84` de 385fc92) y el socket sigue "conectado" para
 * siempre a ojos de Android.
 *
 * <p>{@link EstadoEnlace} es una clase nueva de esta entrega: no existía en 385fc92, así que
 * copiar este fichero solo contra ese commit daría "cannot find symbol" — la condición pide
 * explícitamente NO usar eso como demostración de "rojo" (no dice nada del comportamiento). En su
 * lugar, la demostración fue: <b>invertir la aserción</b> del tercer caso de abajo y correrla contra
 * el código YA corregido. Con {@code assertTrue(estado.vivo(true))} tras {@code marcarCaido()} (la
 * fórmula ANTIGUA — sólo {@code socket.isConnected()} — habría dicho "vivo" ahí), JUnit dio:</p>
 *
 * <pre>
 * JUnit version 4.13.2
 * .E...
 * Time: 0,005
 * There was 1 failure:
 * 1) unEnlaceCaidoNoEstaVivoAunqueElSocketSigaConectado(com.dpi.retrousuario.dominio.EstadoEnlaceTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertTrue(Assert.java:42)
 *     at org.junit.Assert.assertTrue(Assert.java:53)
 *     at com.dpi.retrousuario.dominio.EstadoEnlaceTest.unEnlaceCaidoNoEstaVivoAunqueElSocketSigaConectado(EstadoEnlaceTest.java:56)
 *
 * FAILURES!!!
 * Tests run: 4,  Failures: 1
 * </pre>
 *
 * <p>— capturado tal cual con {@code JUnitCore} contra esta misma clase (README "Tests JVM"), rojo
 * por comportamiento (una aserción real que falló con la fórmula vieja, no un símbolo que faltara).
 * Corregida a {@code assertFalse} abajo, que es la fórmula nueva; con ella, verde.</p>
 */
public class EstadoEnlaceTest {

    @Test
    public void unEnlaceNuevoEstaVivoSiElSocketEstaConectado() {
        EstadoEnlace estado = new EstadoEnlace();
        assertTrue(estado.vivo(true));
    }

    @Test
    public void unEnlaceNuncaMarcadoCaidoNoEstaVivoSiElSocketNoEstaConectado() {
        EstadoEnlace estado = new EstadoEnlace();
        assertFalse(estado.vivo(false));
    }

    /** El caso que motiva la condición ALTO: el hilo lector marcó "caído" (catch de la
     *  {@code IOException}, o fin de flujo sin excepción — EOF —, {@code EnlaceBluetooth.leerSinParar})
     *  pero Android SIGUE reportando el socket conectado porque nadie llamó {@code close()}. La
     *  fórmula antigua ({@code socket.isConnected()} a secas) diría "vivo" aquí; la nueva, no —
     *  ver el rojo/verde de este caso concreto en el Javadoc de la clase. */
    @Test
    public void unEnlaceCaidoNoEstaVivoAunqueElSocketSigaConectado() {
        EstadoEnlace estado = new EstadoEnlace();
        estado.marcarCaido();
        assertFalse(estado.vivo(true));
    }

    /** Llamar {@code marcarCaido()} dos veces (una vez desde el catch de la IOException Y otra vez
     *  al salir del bucle de lectura, {@code EnlaceBluetooth.leerSinParar}) no "revive" el enlace. */
    @Test
    public void marcarCaidoEsIrreversibleParaLaVidaDeEsteEnlace() {
        EstadoEnlace estado = new EstadoEnlace();
        estado.marcarCaido();
        estado.marcarCaido();
        assertFalse(estado.vivo(true));
        assertTrue(estado.caido());
    }
}
