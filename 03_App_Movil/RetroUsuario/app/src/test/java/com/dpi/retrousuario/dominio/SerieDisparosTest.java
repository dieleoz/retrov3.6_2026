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
 * de la serie), TDD-V3.6.md §8, **reescritas contra RF-USR-04 r7 (REPETIR-PREGUNTA, DECISIONES nota
 * 18, SPEC-App-Usuario-V3.6.md:167-197)**: la 0.3.4 repetía sola (una vez, automático, la serie con
 * cero) y anulaba la serie sola tras un tope fijo de 2 reintentos por disparo anulado; r7 lo prohíbe
 * — "la app nunca repite sola" — y decide el operador, sin límite, contestando
 * {@link PreguntaOperadorFalsa}. Contra el {@code SerieDisparos.medir()} de la 0.3.4 real (constructor
 * de 6 argumentos, sin {@link PreguntaOperador}) estas pruebas ni compilan ("cannot find symbol": no
 * existía el 7º parámetro) — eso no demuestra nada del comportamiento. La demostración de rojo real:
 * se restauró el auto-repetir de la 0.3.4 dentro de {@code SerieDisparos} de ESTA entrega (ignorando
 * a propósito la respuesta de {@code pregunta}: {@code medir()} repetía la serie con cero UNA vez sin
 * preguntar, y {@code medirUnDisparo()} volvía al tope fijo {@code intento <= 2}), se recompiló y se
 * corrió con {@code JUnitCore} sólo esta clase — capturado tal cual:
 * <pre>
 * JUnit version 4.13.2
 * ..E..E....E.E.E.E.
 * Time: 0,203
 * There were 6 failures:
 * 1) disparoAnuladoConSaltarAnulaLaSerieEntera(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError: expected:&lt;1&gt; but was:&lt;0&gt;
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.disparoAnuladoConSaltarAnulaLaSerieEntera(SerieDisparosTest.java:195)
 * 2) ceroEnLaPrimeraSerieSinCeroEnLaRepeticionGuardaLaRepeticion(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError: expected:&lt;1&gt; but was:&lt;0&gt;
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.ceroEnLaPrimeraSerieSinCeroEnLaRepeticionGuardaLaRepeticion(SerieDisparosTest.java:150)
 * 3) unCeroSoloSeMuestraComoValorSaturadoONegativo(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.unCeroSoloSeMuestraComoValorSaturadoONegativo(SerieDisparosTest.java:101)
 * 4) cincoPersisteEnLaRepeticionGuardaUnaSolaFilaConMediaCero(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError: expected:&lt;2&gt; but was:&lt;0&gt;
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.cincoPersisteEnLaRepeticionGuardaUnaSolaFilaConMediaCero(SerieDisparosTest.java:129)
 * 5) disparoAnuladoPorPlazoSeRepiteYLaSerieSeGuarda(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError: expected:&lt;1&gt; but was:&lt;0&gt;
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.disparoAnuladoPorPlazoSeRepiteYLaSerieSeGuarda(SerieDisparosTest.java:181)
 * 6) disparoAnuladoSinLimiteDeRepeticionesSiElOperadorSigueRepitiendo(com.dpi.retrousuario.dominio.SerieDisparosTest)
 * java.lang.AssertionError
 *     at com.dpi.retrousuario.dominio.SerieDisparosTest.disparoAnuladoSinLimiteDeRepeticionesSiElOperadorSigueRepitiendo(SerieDisparosTest.java:213)
 * Tests run: 12,  Failures: 6
 * </pre>
 * — 6 de 12 en rojo, todas por comportamiento real (contadores de "veces preguntado" en 0 porque el
 * código viejo nunca llama a {@code pregunta}, y una aserción de "n=1" que ya no se cumple con el
 * tope fijo). Revertido el cambio de vuelta a la implementación de esta entrega (que sí pregunta y sí
 * respeta "Repetir"/"Saltar"), las 12 vuelven a verde.
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

    private SerieDisparos.Resultado medir(char codigo, PreguntaOperador pregunta) {
        return new SerieDisparos(sim, new EmisorRitmo(), params, log, diario, codigo, pregunta).medir();
    }

    /** (a) 3 por defecto (DECISIONES nota 11a), media 110, minimo 100, n=3, valido=SI. Sin cero ni
     *  disparo anulado: nunca se llega a preguntar nada (r7, "la app nunca repite sola" no aplica
     *  porque no hay nada que repetir). */
    @Test
    public void tresDisparosPorDefectoConMediaYMinimo() {
        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(3, params.lecturasPorColor());
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
        assertEquals(110, r.media);
        assertEquals(100, r.minimo);
        assertTrue(r.valido);
        assertEquals("", r.motivo);
        assertEquals(0, pregunta.vecesPreguntadoCero);
        assertEquals(0, pregunta.vecesPreguntadoAnulado);
    }

    /** (b) r7: un cero aislado (lecturasPorColor = 1) pregunta UNA vez ("La lectura dio 0..."); con
     *  "Saltar" (el operador decide no repetir) se guarda la fila con ese mismo 0, sin ocultarlo ni
     *  ponerlo a null — nunca se lee un segundo disparo (0.3.4 sí lo hacía, automático). */
    @Test
    public void unCeroSoloSeMuestraComoValorSaturadoONegativo() {
        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.cero(PreguntaOperador.Decision.SALTAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(0), r.lecturas);
        assertFalse(r.valido);
        assertEquals("saturado_o_negativo", r.motivo);
        assertEquals(0, r.media);
        assertEquals(1, pregunta.vecesPreguntadoCero);
    }

    /** (c) r7: primera serie 0,0,330 -> pregunta -> "Repetir"; repetición 105,0,115 (cero persiste)
     *  -> pregunta otra vez -> "Saltar": una sola fila, la de la repetición, media=0 (SPEC :171-186,
     *  igual que exigía H-A3 con el auto-repetir de 0.3.4, pero ahora decidido por el operador). */
    @Test
    public void cincoPersisteEnLaRepeticionGuardaUnaSolaFilaConMediaCero() {
        sim.programarValor(50, 0);
        sim.programarValor(50, 0);
        sim.programarValor(50, 330);
        sim.programarValor(50, 105);
        sim.programarValor(50, 0);
        sim.programarValor(50, 115);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.cero(
                PreguntaOperador.Decision.REPETIR, PreguntaOperador.Decision.SALTAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(105, 0, 115), r.lecturas); // solo la ultima serie (RF-USR-04).
        assertEquals(0, r.media); // "se guarda la fila con media = 0" (RF-USR-04), no la media aritmetica.
        assertEquals(0, r.minimo);
        assertFalse(r.valido);
        assertEquals("saturado_o_negativo", r.motivo);
        assertEquals(2, pregunta.vecesPreguntadoCero);
    }

    /** (d) r7: primera serie 0,105,110 -> pregunta -> "Repetir"; repetición 100,108,112 (sin ceros):
     *  no se pregunta una segunda vez (ya no hay cero), se guarda la repetición, media = round(320/3)
     *  = 107. */
    @Test
    public void ceroEnLaPrimeraSerieSinCeroEnLaRepeticionGuardaLaRepeticion() {
        sim.programarValor(50, 0);
        sim.programarValor(50, 105);
        sim.programarValor(50, 110);
        sim.programarValor(50, 100);
        sim.programarValor(50, 108);
        sim.programarValor(50, 112);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.cero(PreguntaOperador.Decision.REPETIR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 108, 112), r.lecturas);
        assertEquals(107, r.media);
        assertTrue(r.valido);
        assertEquals("", r.motivo);
        assertEquals(1, pregunta.vecesPreguntadoCero);
    }

    /** (e) n=2, lecturas 100 y 101: media = round(100,5) = 101, mitad hacia arriba (no al par mas cercano). */
    @Test
    public void mediaConFraccionExactaRedondeaMitadHaciaArriba() {
        params.lecturasPorColor(2);
        sim.programarValor(50, 100);
        sim.programarValor(50, 101);
        SerieDisparos.Resultado r = medir('4', PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
        assertTrue(r.guardada);
        assertEquals(101, r.media);
        assertTrue(r.valido);
    }

    /** T-USR-06b(a) corregido por r7: el disparo 2 se anula por plazo -> pregunta ("El equipo no
     *  respondió a este disparo. ¿Repetir o saltar?") -> "Repetir" -> vuelve a disparar y sale 110;
     *  la serie se guarda con n=3, sin tope fijo de reintentos (antes, 0.3.4 lo hacía sola). */
    @Test
    public void disparoAnuladoPorPlazoSeRepiteYLaSerieSeGuarda() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta(); // disparo 2: no responde dentro del plazo -> anulado.
        sim.programarValor(50, 110); // tras "Repetir", nuevo intento del disparo 2.
        sim.programarValor(50, 120);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.anulado(PreguntaOperador.Decision.REPETIR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
        assertEquals(110, r.media);
        assertEquals(100, r.minimo);
        assertTrue(r.valido);
        assertEquals(1, pregunta.vecesPreguntadoAnulado);
    }

    /** T-USR-06b(b) corregido por r7: el disparo 2 se anula por plazo -> pregunta -> "Saltar" anula
     *  LA SERIE ENTERA de ese color de inmediato (SPEC :190-192, "serie anulada: el equipo no
     *  respondió"), sin ningún tope de reintentos — 0.3.4 exigía 2 reintentos automáticos antes de
     *  anular; r7 lo quita, el operador decide en el primer anulado si quiere seguir o no. */
    @Test
    public void disparoAnuladoConSaltarAnulaLaSerieEntera() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta();
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.anulado(PreguntaOperador.Decision.SALTAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertFalse(r.guardada);
        assertEquals(1, pregunta.vecesPreguntadoAnulado);
    }

    /** r7, "sin límite de repeticiones: decide el operador" (DECISIONES nota 18): cuatro disparos
     *  anulados seguidos, todos contestados "Repetir", terminan midiendo igual — nada en el dominio
     *  fija un tope (0.3.4 SÍ lo hacía: anulaba tras 2 reintentos aunque el operador quisiera seguir). */
    @Test
    public void disparoAnuladoSinLimiteDeRepeticionesSiElOperadorSigueRepitiendo() {
        params.lecturasPorColor(1);
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        sim.programarSinRespuesta();
        sim.programarValor(50, 100); // el quinto intento, tras 4 "Repetir", por fin responde.
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.anulado(
                PreguntaOperador.Decision.REPETIR, PreguntaOperador.Decision.REPETIR,
                PreguntaOperador.Decision.REPETIR, PreguntaOperador.Decision.REPETIR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100), r.lecturas);
        assertEquals(4, pregunta.vecesPreguntadoAnulado);
    }

    /** T-USR-06b(c) corregido por r7: el disparo anulado es el PRIMERO de la serie -> pregunta ->
     *  "Repetir" -> mismo resultado que (a). */
    @Test
    public void disparoAnuladoAlPrincipioSeRepiteIgual() {
        sim.programarSinRespuesta();
        sim.programarValor(50, 100); // tras "Repetir", nuevo intento del disparo 1.
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        SerieDisparos.Resultado r = medir('4', PreguntaOperadorFalsa.anulado(PreguntaOperador.Decision.REPETIR));
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
    }

    /** A1 (ALTO): un cambio de `lecturasPorColor` desde Ajustes a mitad de esta serie no la afecta;
     *  n se fija al empezar la serie (3, el valor de antes del cambio), y la serie se guarda con
     *  esas 3 lecturas, no con las 6 del valor nuevo. */
    @Test
    public void unCambioDeLecturasPorColorAMitadDeLaSerieNoLaAfecta() {
        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        FuenteBytes fuenteQueCambiaAjustesAMitad = new FuenteBytes() {
            private int enviosHechos = 0;

            @Override
            public long ahoraMs() {
                return sim.ahoraMs();
            }

            @Override
            public void enviarByte(int b) {
                sim.enviarByte(b);
                enviosHechos++;
                if (enviosHechos == 2) {
                    params.lecturasPorColor(6); // "Ajustes" cambiando n a mitad de esta serie.
                }
            }

            @Override
            public Integer leer(long limiteMs) {
                return sim.leer(limiteMs);
            }
        };
        SerieDisparos.Resultado r = new SerieDisparos(fuenteQueCambiaAjustesAMitad, new EmisorRitmo(), params, log,
                diario, '4', PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR)).medir();
        assertTrue(r.guardada);
        assertEquals(Arrays.asList(100, 110, 120), r.lecturas);
        assertEquals(6, params.lecturasPorColor()); // el cambio sí queda para la SIGUIENTE medida.
    }

    /** Prueba de robustez de este trabajo: una serie anulada (r7, "Saltar" tras el primer disparo sin
     *  respuesta) no deja ninguna fila FILA en el diario. */
    @Test
    public void unaSerieAnuladaNoDejaFilaEnElDiario() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta();
        medir('4', PreguntaOperadorFalsa.anulado(PreguntaOperador.Decision.SALTAR));
        assertTrue(diario.filasGuardadas().isEmpty());
    }

    /** RF-USR-04 r7, texto literal ("serie anulada: el equipo no respondió", sin "a N intentos": ya
     *  no hay un tope fijo que citar). */
    @Test
    public void mensajeDeSerieAnuladaSinToqueDeIntentos() {
        assertEquals("serie anulada: el equipo no respondió", SerieDisparos.MENSAJE_SERIE_ANULADA);
    }

    /** arq C1 (REVISIONES-Apps-V3.6.md, entrada 0.3.6): {@link PreguntaOperador.Decision#ABANDONAR}
     *  ante la pregunta CERO anula la serie entera (sin fila, {@code r.guardada == false}) — a
     *  diferencia de {@link PreguntaOperador.Decision#SALTAR}, que SÍ guarda la fila con
     *  {@code media = 0} ({@link #unCeroSoloSeMuestraComoValorSaturadoONegativo}, arriba). Antes de
     *  esta corrección {@code SerieDisparos} sólo distinguía REPETIR de "cualquier otra cosa" y trataba
     *  ABANDONAR igual que SALTAR: {@code r.guardada} salía {@code true} y esta aserción fallaba. */
    @Test
    public void abandonarAntePreguntaCeroAnulaLaSerieSinFila() {
        params.lecturasPorColor(1);
        sim.programarValor(50, 0);
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.cero(PreguntaOperador.Decision.ABANDONAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertFalse("ABANDONAR ante un cero no debe guardar fila (SPEC §4 bis: sin fila)", r.guardada);
        assertEquals(1, pregunta.vecesPreguntadoCero);
    }

    /** arq C1: {@link PreguntaOperador.Decision#ABANDONAR} ante un disparo anulado se comporta igual
     *  que SALTAR — serie entera anulada, sin fila (RF-USR-16 ya no distingue el motivo). */
    @Test
    public void abandonarAntePreguntaDisparoAnuladoAnulaLaSerieIgualQueSaltar() {
        sim.programarValor(50, 100);
        sim.programarSinRespuesta();
        PreguntaOperadorFalsa pregunta = PreguntaOperadorFalsa.anulado(PreguntaOperador.Decision.ABANDONAR);
        SerieDisparos.Resultado r = medir('4', pregunta);
        assertFalse(r.guardada);
        assertEquals(1, pregunta.vecesPreguntadoAnulado);
    }
}
