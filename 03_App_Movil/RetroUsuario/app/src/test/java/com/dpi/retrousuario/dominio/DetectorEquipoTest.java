package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-01 (TDD-V3.6.md:1794-1805): detección sólo V3.6, nunca 'e', nunca otro firmware.
 * Fuente del esperado: PROTOCOLO-V3.6.md:42 (formato de "#V#"); SPEC-App-Usuario-V3.6.md r4,
 * RF-USR-01; decisión USR-ALCANCE. Las aserciones de {@code sinRespuesta()} son arq ALTO
 * (`05_Documentacion/REVISIONES-Apps-V3.6.md:34-35`).
 */
public class DetectorEquipoTest {

    /** (a) EquipoSimulado responde #V,3.6,2026-09-21,CAL,0000# a #V#: queda conectado, y la
     *  secuencia enviada es EXACTAMENTE #V#, nada más. */
    @Test
    public void unEquipoV36RespondeYSoloSeEnviaVMayuscula() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#V#", "#V,3.6,2026-09-21,CAL,0000#");

        DetectorEquipo.ResultadoDeteccion r = DetectorEquipo.detectar(equipo);

        assertEquals(DetectorEquipo.Resultado.COMPATIBLE, r.resultado());
        assertNotNull(r.respuestaV());
        assertEquals("3.6", r.respuestaV().version());
        assertEquals("2026-09-21", r.respuestaV().fechaCompilacion());
        assertEquals(RespuestaV.EstadoAjuste.CAL, r.respuestaV().estadoAjuste());
        assertFalse("arq ALTO: compatible siempre es una respuesta de verdad, nunca silencio", r.sinRespuesta());
        // Fuente: PROTOCOLO-V3.6.md:42 y RF-USR-01 ("se envía únicamente #V#").
        assertEquals(Arrays.asList("#V#"), equipo.tramasRecibidas());
        assertSinBytesProhibidos(equipo);
    }

    /** (b) simulador que no reconoce #V# (silencio: ni F-2020 ni V4, construido para esta ficha). */
    @Test
    public void unEquipoQueNoReconoceVEsNoCompatibleYSoloSeEnvioV() {
        EquipoSimulado equipo = new EquipoSimulado(); // #V# sin configurar: silencio puro.

        DetectorEquipo.ResultadoDeteccion r = DetectorEquipo.detectar(equipo);

        assertEquals(DetectorEquipo.Resultado.NO_COMPATIBLE, r.resultado());
        // arq ALTO: silencio real (nada llegó) -- distinto de "llegó algo pero no es compatible" (c),
        // abajo. Es el único caso que GestorEnlace.debeReintentarConexionNueva puede convertir en un
        // reintento con conexión nueva, si la detección se hizo sobre un enlace REUTILIZADO.
        assertTrue(r.sinRespuesta());
        assertEquals(Arrays.asList("#V#"), equipo.tramasRecibidas());
        assertSinBytesProhibidos(equipo);
    }

    /** (c) simulador que responde #V,4.0,...# (V4, no V3.6): no compatible, y sólo se envió #V#. */
    @Test
    public void unEquipoV4EsNoCompatibleYSoloSeEnvioV() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#V#", "#V,4.0,2026-01-01,CAL,0000#");

        DetectorEquipo.ResultadoDeteccion r = DetectorEquipo.detectar(equipo);

        assertEquals(DetectorEquipo.Resultado.NO_COMPATIBLE, r.resultado());
        // arq ALTO: SÍ llegó una trama (de un V4 real) -- no es silencio, así que no dispara el
        // reintento de GestorEnlace.debeReintentarConexionNueva aunque el enlace fuera reutilizado.
        assertFalse(r.sinRespuesta());
        assertEquals(Arrays.asList("#V#"), equipo.tramasRecibidas());
        assertSinBytesProhibidos(equipo);
    }

    /** Vista en rojo (CLAUDE.md §7): un detector que además probara "9" tras un rechazo fallaría
     *  esta aserción; se deja como comprobación explícita de la lista blanca, no sólo del resultado. */
    private static void assertSinBytesProhibidos(EquipoSimulado equipo) {
        for (String t : equipo.tramasRecibidas()) {
            if (t.contains("9") || t.contains("6") || t.contains("e") || t.contains("@LEERV")) {
                throw new AssertionError("byte de efecto desconocido enviado: " + t);
            }
        }
    }
}
