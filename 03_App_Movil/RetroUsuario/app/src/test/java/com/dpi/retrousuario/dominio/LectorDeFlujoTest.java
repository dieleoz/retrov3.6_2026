package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * QA sobre RetroUsuario 0.3.3, obligatorio en la 0.3.4 (mensaje del orquestador): "la
 * rotura 'no marcar caido en el catch de EnlaceBluetooth.leerSinParar' NO la detecta ninguna prueba
 * (126/126 en verde con la rotura)". El bucle se sacó a {@link LectorDeFlujo}, dominio puro, para
 * poder probarlo aquí con un {@link InputStream} doble, sin {@code BluetoothSocket}. Los tres casos
 * que pide el encargo: (a) {@link IOException} → caído, (b) EOF ({@code read() == -1}) → caído, (c)
 * bytes entregados → quedan en la cola Y el estado sigue vivo (mientras el flujo no ha terminado).
 *
 * <p><b>Rojo por comportamiento</b>, capturado tal cual con JUnitCore contra esta misma clase
 * (README "Tests JVM") quitando las dos llamadas a {@code estado.marcarCaido()} de
 * {@link LectorDeFlujo#leer} (código de producción, no la aserción) — los tres casos caen, incluido
 * (c), porque su aserción final de limpieza también depende de esa llamada:</p>
 *
 * <pre>
 * JUnit version 4.13.2
 * .E.E.E
 * Time: 0,012
 * There were 3 failures:
 * 1) unEofSinExcepcionMarcaElEnlaceCaido(com.dpi.retrousuario.dominio.LectorDeFlujoTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertTrue(Assert.java:42/53)
 *     at com.dpi.retrousuario.dominio.LectorDeFlujoTest.unEofSinExcepcionMarcaElEnlaceCaido(LectorDeFlujoTest.java:91)
 * 2) bytesEntregadosQuedanEnLaColaYElEstadoSigueVivo(com.dpi.retrousuario.dominio.LectorDeFlujoTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertTrue(Assert.java:42/53)
 *     at com.dpi.retrousuario.dominio.LectorDeFlujoTest.bytesEntregadosQuedanEnLaColaYElEstadoSigueVivo(LectorDeFlujoTest.java:157)
 * 3) unaIOExceptionMarcaElEnlaceCaido(com.dpi.retrousuario.dominio.LectorDeFlujoTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertTrue(Assert.java:42/53)
 *     at com.dpi.retrousuario.dominio.LectorDeFlujoTest.unaIOExceptionMarcaElEnlaceCaido(LectorDeFlujoTest.java:77)
 *
 * FAILURES!!!
 * Tests run: 3,  Failures: 3
 * </pre>
 *
 * <p>— con las dos llamadas a {@code marcarCaido()} repuestas en {@link LectorDeFlujo}, vuelve a
 * verde (ver README "Tests JVM" para la corrida completa de la suite, 134 tests).</p>
 */
public class LectorDeFlujoTest {

    /** {@link InputStream} doble: entrega los bytes de {@code datos} en un solo trozo (como hace un
     *  socket real) y luego, según {@code modoFinal}, o lanza {@link IOException} o devuelve -1
     *  (EOF) — nunca las dos cosas, como un flujo real que ya terminó. */
    private static final class FlujoDoble extends InputStream {
        enum Final { EXCEPCION, EOF }

        private final int[] datos;
        private final Final modoFinal;
        private boolean entregado = false;

        FlujoDoble(int[] datos, Final modoFinal) {
            this.datos = datos;
            this.modoFinal = modoFinal;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!entregado) {
                entregado = true;
                int n = Math.min(len, datos.length);
                for (int i = 0; i < n; i++) {
                    b[off + i] = (byte) datos[i];
                }
                return n;
            }
            if (modoFinal == Final.EXCEPCION) {
                throw new IOException("socket cerrado (doble de prueba)");
            }
            return -1; // EOF.
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException("LectorDeFlujo usa read(byte[],int,int)");
        }
    }

    /** (a) el flujo lanza {@link IOException} tras entregar sus bytes: el enlace queda caído. */
    @Test
    public void unaIOExceptionMarcaElEnlaceCaido() {
        FlujoDoble flujo = new FlujoDoble(new int[0], FlujoDoble.Final.EXCEPCION);
        Queue<Integer> cola = new ConcurrentLinkedQueue<>();
        EstadoEnlace estado = new EstadoEnlace();

        LectorDeFlujo.leer(flujo, cola, estado);

        assertTrue(estado.caido());
        assertFalse(estado.vivo(true));
    }

    /** (b) el flujo devuelve -1 (EOF) sin ninguna excepción: también caído — el otro lado pudo
     *  cerrar limpio en vez de romper. */
    @Test
    public void unEofSinExcepcionMarcaElEnlaceCaido() {
        FlujoDoble flujo = new FlujoDoble(new int[0], FlujoDoble.Final.EOF);
        Queue<Integer> cola = new ConcurrentLinkedQueue<>();
        EstadoEnlace estado = new EstadoEnlace();

        LectorDeFlujo.leer(flujo, cola, estado);

        assertTrue(estado.caido());
        assertFalse(estado.vivo(true));
    }

    /** (c) el flujo entrega bytes reales y luego SE QUEDA A MEDIAS (no ha llegado ni la excepción ni
     *  el EOF todavía): esos bytes ya están en la cola, en orden, como enteros 0-255 sin signo (0xFF
     *  no se convierte en -1), y el estado sigue vivo — {@link LectorDeFlujo} sólo marca "caído" AL
     *  TERMINAR el bucle, nunca mientras todavía hay datos en camino. Corre {@link LectorDeFlujo#leer}
     *  en un hilo aparte (es una llamada bloqueante) y usa dos pestillos para observar el estado a
     *  mitad del bucle, antes de dejarlo terminar. */
    @Test
    public void bytesEntregadosQuedanEnLaColaYElEstadoSigueVivo() throws InterruptedException {
        CountDownLatch bytesListos = new CountDownLatch(1);
        CountDownLatch dejarTerminar = new CountDownLatch(1);
        InputStream flujo = new InputStream() {
            private final int[] datos = { 0x23, 0x56, 0xFF, 0x23 };
            private boolean entregado = false;

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (!entregado) {
                    entregado = true;
                    for (int i = 0; i < datos.length; i++) {
                        b[off + i] = (byte) datos[i];
                    }
                    bytesListos.countDown();
                    return datos.length;
                }
                try {
                    dejarTerminar.await(); // se queda "a medias" hasta que el test lo libere.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return -1;
            }

            @Override
            public int read() {
                throw new UnsupportedOperationException("LectorDeFlujo usa read(byte[],int,int)");
            }
        };
        Queue<Integer> cola = new ConcurrentLinkedQueue<>();
        EstadoEnlace estado = new EstadoEnlace();

        Thread hilo = new Thread(() -> LectorDeFlujo.leer(flujo, cola, estado), "lector-de-prueba");
        hilo.setDaemon(true);
        hilo.start();
        try {
            assertTrue("el flujo doble no entregó sus bytes a tiempo", bytesListos.await(2, TimeUnit.SECONDS));
            // El hilo lector vuelca el trozo leído en la cola justo después de entregarlo; sin una
            // señal propia para ese instante exacto, se sondea con un margen generoso.
            long limite = System.currentTimeMillis() + 2000;
            while (cola.size() < 4 && System.currentTimeMillis() < limite) {
                Thread.sleep(5);
            }

            assertEquals(4, cola.size());
            assertEquals(Integer.valueOf(0x23), cola.poll());
            assertEquals(Integer.valueOf(0x56), cola.poll());
            assertEquals(Integer.valueOf(0xFF), cola.poll());
            assertEquals(Integer.valueOf(0x23), cola.poll());
            assertTrue("el bucle no ha terminado todavía: el estado no debe estar caído", estado.vivo(true));
        } finally {
            dejarTerminar.countDown();
            hilo.join(2000);
        }
        assertTrue(estado.caido()); // limpieza: al terminar (EOF tras la pausa), sí cae.
    }
}
