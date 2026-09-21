package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * QA-3 (SPEC-V3.6.md:441-446, RF-USR-16 :344): 150 ms de pausa también entre #V# y el primer #GN#,
 * no sólo entre #GN# y #GC# (eso ya lo prueba {@code Sonda362ExigirTest.sePausan150MsEntreGnYGc}).
 * Marcas de tiempo capturadas con el mismo patrón que M5 (Sonda362): un consumidor de pausa que
 * anota en vez de dormir, para no gastar 150 ms reales en la JVM por prueba.
 */
public class DeteccionYSondaTest {

    private final List<Long> pausas = new ArrayList<>();

    private DeteccionYSonda.Resultado ejecutar(Canal canal, ParametrosRitmo params) {
        return DeteccionYSonda.ejecutar(canal, params, pausas::add);
    }

    /** Vista en rojo contra 32c785d (condición QA-3): esa versión llamaba a DetectorEquipo.detectar y a
     *  Sonda362.sondear por separado, sin ninguna pausa entre los dos; esta prueba fallaría con
     *  pausas = [150] (sólo la de Sonda362 entre GN y GC) en vez de [150, 150]. */
    @Test
    public void pausa150MsEntreVYElPrimerGn() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#V#", "#V,3.6,2026-09-19,CAL,0000#")
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");

        DeteccionYSonda.Resultado r = ejecutar(equipo, new ParametrosRitmo());

        assertEquals(DetectorEquipo.Resultado.COMPATIBLE, r.deteccion().resultado());
        assertEquals(Sonda362.Resultado.OK, r.sonda().resultado());
        assertEquals(Arrays.asList(150L, 150L), pausas); // 1: entre #V# y #GN#; 2: entre #GN# y #GC# (M5).
        assertEquals(Arrays.asList("#V#", "#GN#", "#GC#"), equipo.tramasRecibidas());
    }

    /** Si el equipo no es compatible, ni se sondea ni se pausa: nunca se envía #GN#/#GC# tras un rechazo. */
    @Test
    public void sinCompatibilidadNoSondeaNiPausa() {
        EquipoSimulado equipo = new EquipoSimulado().responde("#V#", "#V,4.0,2026-09-19,CAL,0000#");

        DeteccionYSonda.Resultado r = ejecutar(equipo, new ParametrosRitmo());

        assertEquals(DetectorEquipo.Resultado.NO_COMPATIBLE, r.deteccion().resultado());
        assertTrue(pausas.isEmpty());
        assertEquals(Arrays.asList("#V#"), equipo.tramasRecibidas());
    }
}
