package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-06 (disparos, 3 por defecto, regla del cero) y T-USR-06b (disparo anulado por plazo dentro
 * de la serie), TDD-V3.6.md §8. Fuente del esperado citada en cada caso.
 */
public class SerieDisparosTest {

    private EquipoSimuladoDisparos sim;
    private ParametrosRitmo params;
    private RegistroTramas log;
    private Diario diario;
    private File ficheroDiario;

    @Before
    public void preparar() throws IOException {
        sim = new EquipoSimuladoDisparos();
        params = new ParametrosRitmo();
        log = new RegistroTramas();
        ficheroDiario = File.createTempFile("diario", ".txt");
        ficheroDiario.deleteOnExit();
        diario = new Diario(ficheroDiario);
    }

    @After
    public void limpiar() {
        ficheroDiario.delete();
    }

    private SerieDisparos.Resultado medir(char codigo) {
        return new SerieDisparos(sim, new EmisorRitmo(), params, log, diario, codigo).medir();
    }

    /** (a) 3 por defecto (DECISIONES nota 11a), media 110, minimo 100, n=3, valido=SI. */
    @Test
    public void tresDisparosPorDefectoConMediaYMinimo() {
        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(3, params.lecturasPorColor());
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
        assertEquals(110, r.media);
        assertEquals(100, r.minimo);
        assertTrue(r.valido);
        assertEquals("", r.motivo);
    }

    /** (b) un cero aislado (lecturasPorColor = 1) no se oculta ni se pone a null. */
    @Test
    public void unCeroSoloSeMuestraComoValorSaturadoONegativo() {
        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        sim.programarValor(50, 0); // repite una vez (unico disparo de la serie es 0): sigue siendo 0.
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(0), r.lecturas);
        assertFalse(r.valido);
        assertEquals("saturado_o_negativo", r.motivo);
        assertEquals(0, r.media);
    }

    /** (c) primera serie 0,0,330; repeticion 105,0,115 (cero persiste): una sola fila, la de la repeticion, media=0. */
    @Test
    public void cincoPersisteEnLaRepeticionGuardaUnaSolaFilaConMediaCero() {
        sim.programarValor(50, 0);
        sim.programarValor(50, 0);
        sim.programarValor(50, 330);
        sim.programarValor(50, 105);
        sim.programarValor(50, 0);
        sim.programarValor(50, 115);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(105, 0, 115), r.lecturas); // solo la segunda serie (RF-USR-04).
        assertEquals(0, r.media); // "se guarda la fila con media = 0" (RF-USR-04), no la media aritmetica.
        assertEquals(0, r.minimo);
        assertFalse(r.valido);
        assertEquals("saturado_o_negativo", r.motivo);
    }

    /** (d) primera serie 0,105,110; repeticion 100,108,112 (sin ceros): se guarda la repeticion, media = round(320/3) = 107. */
    @Test
    public void ceroEnLaPrimeraSerieSinCeroEnLaRepeticionGuardaLaRepeticion() {
        sim.programarValor(50, 0);
        sim.programarValor(50, 105);
        sim.programarValor(50, 110);
        sim.programarValor(50, 100);
        sim.programarValor(50, 108);
        sim.programarValor(50, 112);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 108, 112), r.lecturas);
        assertEquals(107, r.media);
        assertTrue(r.valido);
        assertEquals("", r.motivo);
    }

    /** (e) n=2, lecturas 100 y 101: media = round(100,5) = 101, mitad hacia arriba (no al par mas cercano). */
    @Test
    public void mediaConFraccionExactaRedondeaMitadHaciaArriba() {
        params.lecturasPorColor(2);
        sim.programarValor(50, 100);
        sim.programarValor(50, 101);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(101, r.media);
        assertTrue(r.valido);
    }

    /** T-USR-06b(a): el disparo 2 se anula por plazo, se repite una vez y sale 110; la serie se guarda con n=3. */
    @Test
    public void disparoAnuladoPorPlazoSeRepiteYLaSerieSeGuarda() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta(); // disparo 2: no responde dentro del plazo -> anulado.
        sim.programarValor(50, 110); // repeticion del disparo 2.
        sim.programarValor(50, 120);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
        assertEquals(110, r.media);
        assertEquals(100, r.minimo);
        assertTrue(r.valido);
    }

    /** T-USR-06b(b): 2 repeticiones tambien anuladas -> la serie entera se anula, sin fila. */
    @Test
    public void disparoAnuladoDosVecesAnulaLaSerieEntera() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        SerieDisparos.Resultado r = medir('4');
        assertFalse(r.guardada);
    }

    /** T-USR-06b(c): el disparo anulado es el PRIMERO de la serie: mismo resultado que (a). */
    @Test
    public void disparoAnuladoAlPrincipioSeRepiteIgual() {
        sim.programarSinRespuesta();
        sim.programarValor(50, 100); // repeticion del disparo 1.
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        SerieDisparos.Resultado r = medir('4');
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
    }

    /** Prueba de robustez de este trabajo: una serie anulada no deja ninguna fila FILA en el diario. */
    @Test
    public void unaSerieAnuladaNoDejaFilaEnElDiario() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        medir('4');
        assertTrue(diario.filasGuardadas().isEmpty());
    }
}
