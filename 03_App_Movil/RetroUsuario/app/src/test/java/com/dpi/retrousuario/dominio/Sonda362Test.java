package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-01b (TDD-V3.6.md:1807-1820): exige firmware 3.6.2, y un silencio no es lo mismo que
 * "#ERR,FORMATO#". Fuente del esperado: PROTOCOLO-V3.6.md:51-54 (#GN#/#GC# son de 3.6.2,
 * #ERR,FORMATO# es la respuesta genérica del firmware a un campo que no reconoce,
 * calibracion_v36.c:805-807) y :70-72 (formato de motivo); SPEC-App-Usuario-V3.6.md r4 RF-USR-01
 * (M-3) y §3 (parser).
 */
public class Sonda362Test {

    /** (b) #GN,SLV-002# y #GC,2026-09-19#: OK, con serie y fecha leídas. */
    @Test
    public void unEquipo362RespondeGnYGcYLaSondaEsOk() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");

        Sonda362.ResultadoSonda r = Sonda362.sondear(equipo);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertTrue(r.serieLeida());
        assertEquals("SLV-002", r.serie());
        assertEquals("leida", r.origenSerie());
        assertTrue(r.fechaRegistrada());
        assertEquals("2026-09-19", r.fechaCalibracion());
    }

    /** (c) un firmware "3.6"/"3.6.1" responde #ERR,FORMATO# a las dos: ACTUALIZAR_FIRMWARE. */
    @Test
    public void unFirmwareSinGnNiGcRespondeErrFormatoYPideActualizar() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#ERR,FORMATO#")
                .responde("#GC#", "#ERR,FORMATO#");

        Sonda362.ResultadoSonda r = Sonda362.sondear(equipo);

        assertEquals(Sonda362.Resultado.ACTUALIZAR_FIRMWARE, r.resultado());
    }

    /** (d) silencio puro (sin #GN,...# ni #ERR,...#): no concluye "actualice", ofrece reintentar. */
    @Test
    public void unSilencioPuroNoConcluyeActualizarSinoReintentar() {
        EquipoSimulado equipo = new EquipoSimulado(); // #GN# y #GC# sin configurar: silencio.

        Sonda362.ResultadoSonda r = Sonda362.sondear(equipo);

        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r.resultado());
    }

    /** Distinto de (d): "#GN,NONE#" SÍ es una sonda OK (equipo 3.6.2 sin serie grabada, RF-USR-06);
     *  no se confunde con silencio ni con "actualice el firmware" (T-USR-07(b), fuera de este
     *  incremento salvo por esta distinción, que sí es parte de RF-USR-01). */
    @Test
    public void gnNoneEsOkSinSerieNoSilencioNiActualizar() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#GN,NONE#")
                .responde("#GC#", "#GC,NONE#");

        Sonda362.ResultadoSonda r = Sonda362.sondear(equipo);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertFalse(r.serieLeida());
        assertEquals("", r.serie());
        assertEquals("ninguna", r.origenSerie());
        assertFalse(r.fechaRegistrada());
    }

    /** Vista en rojo (CLAUDE.md §7): contra un borrador que sólo mirase si la respuesta es null para
     *  decidir "actualice el firmware" (confundiendo silencio con ERR,FORMATO), este caso fallaría:
     *  el borrador diría ACTUALIZAR_FIRMWARE donde el esperado es SIN_RESPUESTA_REINTENTAR. Se deja
     *  como comentario porque el borrador nunca se escribió como clase aparte: la implementación
     *  actual de Sonda362.clasificar ya distingue null (SILENCIO) de "#ERR,FORMATO#" explícito. */
    @Test
    public void unMotivoDeErrorDistintoDeFormatoNoPideActualizar() {
        // Un "#ERR,PIN#" o "#ERR,BLOQUEADO#" no debería darse aquí (esta app no envía #L#), pero el
        // parser no debe fallar si llega: se trata como "sin respuesta útil" (SPEC §3), no como
        // "actualice el firmware".
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#ERR,PIN#")
                .responde("#GC#", "#GC,2026-09-19#");

        Sonda362.ResultadoSonda r = Sonda362.sondear(equipo);

        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r.resultado());
    }
}
