package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625) (revisor Fable, opción A, sobre RetroUsuario 0.3.5): {@link EstadoMedida} saca la
 * operación "medir" de {@code MedirActivity} (dueña hasta esta entrega, ver el Javadoc de la clase) al
 * dominio, mismo patrón que {@link EstadoDeteccion} (C1/C2 sobre la 0.3.3/0.3.4). El HILO que llama a
 * {@link EstadoMedida#ejecutar} hace de "hilo de medir" (nunca principal, como en producción); el HILO
 * DE PRUEBA (este) hace de "la Activity nueva" — pregunta {@link EstadoMedida#fase()}/{@link
 * EstadoMedida#preguntaPendiente()} y llama a {@link EstadoMedida#responder} desde fuera, exactamente
 * como haría una Activity recreada tras un giro que no es la que lanzó el hilo.
 *
 * <p>Rojo real (no "cannot find symbol": {@link EstadoMedida} es nueva de esta entrega, así que
 * copiar esta clase de prueba contra un commit sin ella ni compila — eso no demuestra nada de
 * comportamiento, CLAUDE.md §7): se anuló el cuerpo de {@code responder()} (no hace nada) y de
 * {@code abandonar()} (no contesta nada), se recompiló y se corrió sólo esta clase con
 * {@code JUnitCore} (bajo {@code timeout}, porque con los dos métodos anulados los hilos de medir de
 * las pruebas afectadas se quedan bloqueados para siempre en su {@code ArrayBlockingQueue.take()} —
 * JUnitCore los mata igual al llamar a {@code System.exit()} al terminar). Capturado tal cual:</p>
 * <pre>
 * JUnit version 4.13.2
 * ...E.E.E.E.E
 * Time: 51,231
 * There were 5 failures:
 * 1) abandonarConPreguntaPendienteTerminaMedirYNoCondenaAlSiguiente(...EstadoMedidaTest)
 * java.lang.AssertionError: abandonar() no liberó el hilo de medir
 *     at ...EstadoMedidaTest.abandonarConPreguntaPendienteTerminaMedirYNoCondenaAlSiguiente(EstadoMedidaTest.java:236)
 * 2) trasResponderNoQuedaPreguntaPendienteYElOyenteFueAvisado(...EstadoMedidaTest)
 * java.lang.AssertionError
 *     at ...EstadoMedidaTest.trasResponderNoQuedaPreguntaPendienteYElOyenteFueAvisado(EstadoMedidaTest.java:317)
 * 3) disparoAnuladoDejaPreguntaAnuladoPendienteYSaltarAnulaLaSerie(...EstadoMedidaTest)
 * java.lang.AssertionError
 *     at ...EstadoMedidaTest.disparoAnuladoDejaPreguntaAnuladoPendienteYSaltarAnulaLaSerie(EstadoMedidaTest.java:125)
 * 4) responderSinPreguntaPendienteSeDescartaYNoContestaLaSiguiente(...EstadoMedidaTest)
 * java.lang.AssertionError
 *     at ...EstadoMedidaTest.responderSinPreguntaPendienteSeDescartaYNoContestaLaSiguiente(EstadoMedidaTest.java:291)
 * 5) serieConCeroDejaPreguntaCeroPendienteYSaltarGuardaLaFila(...EstadoMedidaTest)
 * java.lang.AssertionError: el hilo de medir no terminó tras responder
 *     at ...EstadoMedidaTest.serieConCeroDejaPreguntaCeroPendienteYSaltarGuardaLaFila(EstadoMedidaTest.java:94)
 * Tests run: 7,  Failures: 5
 * </pre>
 * <p>— 5 de 7 en rojo por comportamiento real (los pendientes nunca se contestan, los hilos de medir
 * de esas cinco pruebas nunca terminan dentro del plazo); {@code faseEsMidiendoMientrasMideYLibreAlTerminar}
 * (T3) y {@code excepcionEnMedirDejaErrorPendienteYFaseLibre} (T4) siguen en verde, correcto: ninguna
 * de las dos depende de {@code responder()}/{@code abandonar()}. Revertidos los dos métodos a su
 * implementación real, las 7 vuelven a verde (ver el recuento de la suite completa en el README).</p>
 */
public class EstadoMedidaTest {

    private static final long TIMEOUT_ESPERA_MS = 5000;
    private static final long TIMEOUT_JOIN_MS = 10000;

    private EquipoSimuladoDisparos sim;
    private ParametrosRitmo params;
    private RegistroTramas log;
    private Diario diario;
    private File ficheroDiario;
    private SesionMedicion sesion;

    @Before
    public void preparar() throws IOException {
        sim = new EquipoSimuladoDisparos();
        params = new ParametrosRitmo();
        log = new RegistroTramas();
        ficheroDiario = File.createTempFile("diario-estadomedida", ".txt");
        ficheroDiario.deleteOnExit();
        diario = new Diario(ficheroDiario);
        sesion = new SesionMedicion(sim, params, log, diario);
        sesion.registrarEquipo("MAC-T", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
    }

    @After
    public void limpiar() {
        ficheroDiario.delete();
    }

    /** Espera activa (polling corto) a que {@code medida} llegue a {@code fase}, como haría una
     *  Activity que pregunta desde fuera del hilo de medir — falla con un mensaje claro si se agota
     *  el plazo, en vez de colgarse. */
    private static void esperarFase(EstadoMedida medida, EstadoMedida.Fase fase, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            if (medida.fase() == fase) {
                return;
            }
            Thread.sleep(5);
        }
        throw new AssertionError("timeout esperando fase " + fase + "; fase actual: " + medida.fase());
    }

    /** T1: una serie con un 0 deja la pregunta CERO pendiente, visible desde otro hilo; "Saltar" cierra
     *  con la fila de la SPEC (media=0, válido=NO, motivo saturado_o_negativo), entregada una sola vez. */
    @Test
    public void serieConCeroDejaPreguntaCeroPendienteYSaltarGuardaLaFila() throws InterruptedException {
        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        EstadoMedida medida = new EstadoMedida();

        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesion, "rojo", "2026-09-21T10:00:00-05:00", "", "", "sin_posicion"),
                "medir-rojo-t1");
        hiloMedir.start();

        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);
        assertEquals(EstadoMedida.TipoPregunta.CERO, medida.preguntaPendiente());

        medida.responder(PreguntaOperador.Decision.SALTAR);
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse("el hilo de medir no terminó tras responder", hiloMedir.isAlive());

        EstadoMedida.Resultado resultado = medida.recogerResultadoPendiente();
        assertNotNull(resultado);
        FilaMedida fila = resultado.fila();
        assertNotNull(fila);
        assertEquals(0, fila.media);
        assertFalse(fila.valido);
        assertEquals("saturado_o_negativo", fila.motivo);

        assertNull("el resultado se entrega una sola vez", medida.recogerResultadoPendiente());
    }

    /** T2: un disparo anulado deja la pregunta ANULADO pendiente; "Saltar" anula la serie entera (sin
     *  fila, {@code recogerResultadoPendiente()} entrega un {@link EstadoMedida.Resultado} con
     *  {@code fila() == null}) y no deja nada en el diario. */
    @Test
    public void disparoAnuladoDejaPreguntaAnuladoPendienteYSaltarAnulaLaSerie() throws InterruptedException {
        sim.programarSinRespuesta();
        EstadoMedida medida = new EstadoMedida();

        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesion, "verde", "2026-09-21T10:00:00-05:00", "", "", "sin_posicion"),
                "medir-verde-t2");
        hiloMedir.start();

        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);
        assertEquals(EstadoMedida.TipoPregunta.ANULADO, medida.preguntaPendiente());

        medida.responder(PreguntaOperador.Decision.SALTAR);
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse(hiloMedir.isAlive());

        EstadoMedida.Resultado resultado = medida.recogerResultadoPendiente();
        assertNotNull(resultado);
        assertNull("serie anulada: sin fila", resultado.fila());
        assertTrue("una serie anulada no deja fila en el diario", sesion.filas().isEmpty());
    }

    /** T3: la fase es MIDIENDO desde que arranca {@code ejecutar()} hasta que publica, y LIBRE después
     *  — confirmado con concurrencia real (un {@link FuenteBytes} que se detiene a mitad del primer
     *  {@code leer()} hasta que el hilo de prueba lo confirma), no por suposición de temporización. */
    @Test
    public void faseEsMidiendoMientrasMideYLibreAlTerminar() throws InterruptedException {
        params.lecturasPorColor(1);
        sim.programarValor(50, 100);
        java.util.concurrent.CountDownLatch primerLeer = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch puedeContinuar = new java.util.concurrent.CountDownLatch(1);
        FuenteBytes controlada = new FuenteBytes() {
            @Override
            public long ahoraMs() {
                return sim.ahoraMs();
            }

            @Override
            public void enviarByte(int b) {
                sim.enviarByte(b);
            }

            @Override
            public Integer leer(long limiteMs) {
                primerLeer.countDown();
                try {
                    puedeContinuar.await(TIMEOUT_ESPERA_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sim.leer(limiteMs);
            }
        };
        SesionMedicion sesionControlada = new SesionMedicion(controlada, params, log, diario);
        sesionControlada.registrarEquipo("MAC-T3", "#V,3.6#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        EstadoMedida medida = new EstadoMedida();
        assertEquals(EstadoMedida.Fase.LIBRE, medida.fase());

        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesionControlada, "azul", "x", "", "", "sin_posicion"), "medir-azul-t3");
        hiloMedir.start();

        assertTrue("nunca llamó a leer()", primerLeer.await(TIMEOUT_ESPERA_MS, TimeUnit.MILLISECONDS));
        assertEquals(EstadoMedida.Fase.MIDIENDO, medida.fase());

        puedeContinuar.countDown();
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse(hiloMedir.isAlive());
        assertEquals(EstadoMedida.Fase.LIBRE, medida.fase());
    }

    /** T4: una excepción dentro de {@code sesion.medir()} (B-1 de la 0.3.0: antes la capturaba el hilo
     *  de {@code MedirActivity}) queda como error pendiente, de un solo uso, con la fase de vuelta a
     *  LIBRE — sin arnés de hilos: {@code ejecutar()} no bloquea en este caso. */
    @Test
    public void excepcionEnMedirDejaErrorPendienteYFaseLibre() {
        FuenteBytes fuenteQueFalla = new FuenteBytes() {
            @Override
            public long ahoraMs() {
                return 0;
            }

            @Override
            public void enviarByte(int b) {
                // nada: el fallo está en leer().
            }

            @Override
            public Integer leer(long limiteMs) {
                throw new RuntimeException("fallo simulado T4");
            }
        };
        SesionMedicion sesionQueFalla = new SesionMedicion(fuenteQueFalla, params, log, diario);
        sesionQueFalla.registrarEquipo("MAC-T4", "#V,3.6#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        EstadoMedida medida = new EstadoMedida();

        medida.ejecutar(sesionQueFalla, "marron", "x", "", "", "sin_posicion");

        assertEquals(EstadoMedida.Fase.LIBRE, medida.fase());
        assertEquals("fallo simulado T4", medida.recogerErrorPendiente());
        assertNull("un error no deja un resultado pendiente", medida.recogerResultadoPendiente());
        assertNull("el error se entrega una sola vez", medida.recogerErrorPendiente());
    }

    /** T5: {@code abandonar()} con una pregunta pendiente contesta SALTAR y deja terminar
     *  {@code sesion.medir()} (suelta el monitor de {@code SesionMedicion.java:100}) — y un
     *  {@code medir()} POSTERIOR, desde otro hilo, no queda condenado a auto-saltar para siempre: si
     *  vuelve a traer un cero, vuelve a preguntar y a bloquear normalmente (confirma que
     *  {@code ejecutar()} resetea el abandono de la medida anterior). Las dos partes, en menos de 10 s. */
    @Test
    public void abandonarConPreguntaPendienteTerminaMedirYNoCondenaAlSiguiente() throws InterruptedException, IOException {
        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        EstadoMedida medida = new EstadoMedida();

        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesion, "rojo", "x", "", "", "sin_posicion"), "medir-rojo-t5");
        long t0 = System.currentTimeMillis();
        hiloMedir.start();
        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);

        medida.abandonar();
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse("abandonar() no liberó el hilo de medir", hiloMedir.isAlive());
        long elapsedAbandono = System.currentTimeMillis() - t0;
        assertTrue("abandonar() tardó " + elapsedAbandono + " ms", elapsedAbandono < TIMEOUT_JOIN_MS);

        // Medir posterior, desde otro hilo, sobre un equipo distinto: si vuelve a bloquear en una
        // pregunta normal (no auto-SALTAR), abandonar() no dejó la bandera pegada para siempre.
        EquipoSimuladoDisparos sim2 = new EquipoSimuladoDisparos();
        sim2.programarValor(50, 0);
        ParametrosRitmo params2 = new ParametrosRitmo();
        params2.lecturasPorColor(1);
        File ficheroDiario2 = File.createTempFile("diario-estadomedida-t5b", ".txt");
        ficheroDiario2.deleteOnExit();
        Diario diario2 = new Diario(ficheroDiario2);
        SesionMedicion sesion2 = new SesionMedicion(sim2, params2, new RegistroTramas(), diario2);
        sesion2.registrarEquipo("MAC-T5b", "#V,3.6#", "SLV-003", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));

        long t1 = System.currentTimeMillis();
        Thread hiloMedir2 = new Thread(
                () -> medida.ejecutar(sesion2, "azul", "x", "", "", "sin_posicion"), "medir-azul-t5");
        hiloMedir2.start();
        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);
        assertEquals("el medir posterior no volvió a preguntar de verdad: quedó auto-saltando",
                EstadoMedida.TipoPregunta.CERO, medida.preguntaPendiente());
        medida.responder(PreguntaOperador.Decision.SALTAR);
        hiloMedir2.join(TIMEOUT_JOIN_MS);
        assertFalse(hiloMedir2.isAlive());
        long elapsedSiguiente = System.currentTimeMillis() - t1;
        assertTrue("el medir posterior tardó " + elapsedSiguiente + " ms (>= 10 s)", elapsedSiguiente < TIMEOUT_JOIN_MS);

        ficheroDiario2.delete();
    }

    /** T6: {@code responder()} sin pregunta pendiente se descarta — no queda guardada para contestar
     *  la pregunta SIGUIENTE que haga el hilo de medir. */
    @Test
    public void responderSinPreguntaPendienteSeDescartaYNoContestaLaSiguiente() throws InterruptedException {
        EstadoMedida medida = new EstadoMedida();

        medida.responder(PreguntaOperador.Decision.REPETIR); // sin pregunta pendiente: no debe colarse.

        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesion, "amarillo", "x", "", "", "sin_posicion"), "medir-amarillo-t6");
        hiloMedir.start();
        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);

        // Si la respuesta de arriba se hubiera colado, la pregunta ya estaría contestada: sigue pendiente.
        Thread.sleep(150);
        assertEquals("una respuesta sin pregunta pendiente contestó la pregunta siguiente",
                EstadoMedida.TipoPregunta.CERO, medida.preguntaPendiente());

        medida.responder(PreguntaOperador.Decision.SALTAR); // esta sí contesta de verdad.
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse(hiloMedir.isAlive());
    }

    /** T7: tras {@code responder()}, la pregunta pendiente desaparece y el oyente recibió, al menos,
     *  el aviso del cambio de fase (a PREGUNTANDO al preguntar, y de nuevo al terminar). */
    @Test
    public void trasResponderNoQuedaPreguntaPendienteYElOyenteFueAvisado() throws InterruptedException {
        int[] avisos = { 0 };
        EstadoMedida medida = new EstadoMedida();
        medida.registrarOyente(() -> avisos[0]++);

        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        Thread hiloMedir = new Thread(
                () -> medida.ejecutar(sesion, "anaranjado", "x", "", "", "sin_posicion"), "medir-anaranjado-t7");
        hiloMedir.start();
        esperarFase(medida, EstadoMedida.Fase.PREGUNTANDO, TIMEOUT_ESPERA_MS);
        long limiteAviso = System.currentTimeMillis() + TIMEOUT_ESPERA_MS;
        while (avisos[0] < 1 && System.currentTimeMillis() < limiteAviso) {
            Thread.sleep(5);
        }
        int avisosAlPreguntar = avisos[0];
        assertTrue("el oyente no fue avisado al quedar pendiente la pregunta", avisosAlPreguntar >= 1);

        medida.responder(PreguntaOperador.Decision.SALTAR);
        hiloMedir.join(TIMEOUT_JOIN_MS);
        assertFalse(hiloMedir.isAlive());

        assertNull(medida.preguntaPendiente());
        assertTrue("el oyente no fue avisado al terminar la medida", avisos[0] > avisosAlPreguntar);
    }
}
