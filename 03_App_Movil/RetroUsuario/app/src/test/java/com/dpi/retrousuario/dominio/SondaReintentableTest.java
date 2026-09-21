package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-01b(d)(e) (TDD-V3.6.md :1826-1834, SPEC r6 RF-USR-01): el reintento de la sonda es
 * operador-driven, acotado a 2, y no reenvía "#V#". Condición QA-1: esta clase no existía en
 * 32c785d; {@link Sonda362} por sí solo no distingue "sonda original" de "reintento n" ni cuenta
 * cuántos se han ofrecido, así que el caso (e) (agotar los 2 reintentos) no tenía dónde vivir en el
 * dominio antes de esta ficha — se hubiera tenido que probar contra MainActivity, que no tiene arnés
 * JVM (README, "Tests JVM").
 */
public class SondaReintentableTest {

    /** T-USR-01b(d): la sonda original, sola, sobre un equipo en silencio puro: SIN_RESPUESTA_REINTENTAR
     *  y todavía se puede ofrecer reintentar (1 intento hecho, máximo 2 reintentos). */
    @Test
    public void tSusr01bD_sondaOriginalEnSilencioOfreceReintentar() {
        EquipoSimulado equipo = new EquipoSimulado(); // #GN#/#GC# sin configurar: silencio puro.
        SondaReintentable sonda = new SondaReintentable(new ParametrosRitmo());

        Sonda362.ResultadoSonda r = sonda.intentar(equipo);

        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r.resultado());
        assertEquals(1, sonda.intentosHechos());
        assertTrue(sonda.puedeReintentar());
    }

    /** T-USR-01b(e): "el operador pulsa reintentar 2 veces y el simulador sigue sin contestar nada
     *  las 2 veces". Tras el segundo reintento (3er intento en total: la sonda original + 2
     *  reintentos), la sonda se da por agotada: no se ofrece un tercer reintento.
     *
     *  <p>Vista en rojo contra 32c785d (condición QA-1): esa versión no tenía {@link SondaReintentable}
     *  ni forma de contar reintentos en el dominio (el botón de MainActivity no llevaba cuenta y
     *  reconectaba entero en cada click, reenviando "#V#"); esta clase no compilaba/no existía, así
     *  que esta prueba fallaba por "cannot find symbol" antes de esta ficha.</p> */
    @Test
    public void tSusr01bE_dosReintentosAgotadosNoOfrecenUnTercero() {
        EquipoSimulado equipo = new EquipoSimulado(); // silencio puro las 3 veces.
        ParametrosRitmo params = new ParametrosRitmo();
        assertEquals(2, params.maxReintentosSonda());
        SondaReintentable sonda = new SondaReintentable(params);

        Sonda362.ResultadoSonda r1 = sonda.intentar(equipo); // la sonda original.
        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r1.resultado());
        assertTrue(sonda.puedeReintentar()); // 1 intento hecho: se puede ofrecer el 1er reintento.

        Sonda362.ResultadoSonda r2 = sonda.intentar(equipo); // 1er reintento (operador).
        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r2.resultado());
        assertTrue(sonda.puedeReintentar()); // 2 intentos hechos: todavia se puede ofrecer el 2o.

        Sonda362.ResultadoSonda r3 = sonda.intentar(equipo); // 2o reintento (operador): se agota.
        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r3.resultado());
        assertEquals(3, sonda.intentosHechos()); // sonda original + 2 reintentos (SPEC r6 RF-USR-01).
        assertFalse(sonda.puedeReintentar()); // no se ofrece un tercer reintento.
    }

    /** Constructor con {@code intentosYaHechos}: seedea el contador con la sonda original que ya
     *  hizo {@link DeteccionYSonda} junto con la detección de "#V#", para que los dos compartan el
     *  mismo límite de 3 intentos en total. */
    @Test
    public void constructorConIntentosYaHechosSeedeaElContador() {
        SondaReintentable sonda = new SondaReintentable(new ParametrosRitmo(), 1);
        assertEquals(1, sonda.intentosHechos());
        assertTrue(sonda.puedeReintentar()); // 1 <= 2: todavia caben 2 reintentos mas.

        EquipoSimulado equipo = new EquipoSimulado();
        sonda.intentar(equipo); // 2o intento en total (1er reintento).
        sonda.intentar(equipo); // 3er intento en total (2o reintento): se agota.
        assertEquals(3, sonda.intentosHechos());
        assertFalse(sonda.puedeReintentar());
    }

    /** El reintento no reenvía "#V#": SondaReintentable sólo llama a Sonda362.sondear (#GN#/#GC#)
     *  sobre el canal que se le pasa, nunca a DetectorEquipo.detectar. */
    @Test
    public void elReintentoNoReenviaV() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");
        SondaReintentable sonda = new SondaReintentable(new ParametrosRitmo());

        sonda.intentar(equipo);
        sonda.intentar(equipo);

        assertFalse(equipo.tramasRecibidas().contains("#V#"));
    }
}
