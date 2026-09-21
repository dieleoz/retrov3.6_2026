package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A1 (ALTO): una sola medida en vuelo. Dos hilos llamando a {@link SesionMedicion#medir} a la vez
 * sobre la MISMA sesión (mismo {@code fuente}/{@code ritmo}, como pasaría si la interfaz no
 * deshabilitara los botones y el operador pulsara dos colores casi a la vez) no deben entrelazar sus
 * disparos: cada serie tiene que salir completa y sin mezclar valores de la otra.
 *
 * <p>Para exponer la carrera de forma fiable (sin depender de que el scheduler del SO golpee la
 * ventana exacta), el {@link FuenteBytes} de prueba usada por los dos hilos añade una pausa real muy
 * corta dentro de {@code enviarByte}/{@code leer}, ensanchando la ventana en la que un
 * entrelazado sin cerrojo sería visible.</p>
 */
public class SesionMedicionConcurrenciaTest {

    private File ficheroDiario;

    @Before
    public void preparar() throws IOException {
        ficheroDiario = File.createTempFile("diario-concurrencia", ".txt");
        ficheroDiario.deleteOnExit();
    }

    @After
    public void limpiar() {
        ficheroDiario.delete();
    }

    /** Doble de {@link FuenteBytes} que cede el hilo (pausa real corta) en cada operación, para que
     *  dos hilos sin cerrojo tengan una ventana real donde entrelazarse. */
    private static final class FuenteLenta implements FuenteBytes {
        private final EquipoSimuladoDisparos delegado;

        FuenteLenta(EquipoSimuladoDisparos delegado) {
            this.delegado = delegado;
        }

        @Override
        public long ahoraMs() {
            return delegado.ahoraMs();
        }

        @Override
        public void enviarByte(int b) {
            ceder();
            delegado.enviarByte(b);
            ceder();
        }

        @Override
        public Integer leer(long limiteMs) {
            ceder();
            return delegado.leer(limiteMs);
        }

        private static void ceder() {
            try {
                Thread.sleep(0, 200000); // 0,2 ms: ensancha la ventana sin alargar la prueba en serio.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** T-USR-A1: dos medir() concurrentes (colores y valores distinguibles) no mezclan lecturas entre sí. */
    @Test
    public void dosMedirConcurrentesNoEntrelazanSusDisparos() throws InterruptedException {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        // Serie "rojo": tres 10; serie "azul": tres 20 -- valores distinguibles para detectar mezcla.
        for (int i = 0; i < 3; i++) {
            sim.programarValor(1, 10);
        }
        for (int i = 0; i < 3; i++) {
            sim.programarValor(1, 20);
        }
        FuenteLenta fuente = new FuenteLenta(sim);
        SesionMedicion sesion = new SesionMedicion(fuente, new ParametrosRitmo(), new RegistroTramas(),
                new Diario(ficheroDiario));
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));

        CountDownLatch salida = new CountDownLatch(2);
        Thread hiloRojo = new Thread(() -> {
            sesion.medir("rojo", "x", "", "", "sin_posicion");
            salida.countDown();
        }, "medir-rojo");
        Thread hiloAzul = new Thread(() -> {
            sesion.medir("azul", "x", "", "", "sin_posicion");
            salida.countDown();
        }, "medir-azul");
        hiloRojo.start();
        hiloAzul.start();
        assertTrue("las dos medidas no terminaron a tiempo", salida.await(10, TimeUnit.SECONDS));

        List<FilaMedida> filas = sesion.filas();
        assertEquals(2, filas.size());
        // El cerrojo (A1) garantiza que las dos series no se entrelazan, pero NO fija cuál de los dos
        // hilos gana la carrera por el monitor: por eso esta prueba no asume que "rojo" se queda con
        // los 10 y "azul" con los 20, sólo que CADA fila es homogénea (las tres lecturas del mismo
        // valor, 10 o 20) y que entre las dos filas aparecen los dos valores, una vez cada uno.
        int conTodoDiez = 0;
        int conTodoVeinte = 0;
        for (FilaMedida f : filas) {
            assertEquals(3, f.lecturas.size());
            if (Arrays.asList(10, 10, 10).equals(f.lecturas)) {
                conTodoDiez++;
            } else if (Arrays.asList(20, 20, 20).equals(f.lecturas)) {
                conTodoVeinte++;
            } else {
                throw new AssertionError("fila de color " + f.color + " con lecturas mezcladas: " + f.lecturas);
            }
        }
        assertEquals(1, conTodoDiez);
        assertEquals(1, conTodoVeinte);
    }
}
