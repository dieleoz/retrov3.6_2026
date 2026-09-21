package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * T-USR-03, T-USR-03b y T-USR-22 (TDD-V3.6.md:1832-1847, 2183-2190): estado de calibración,
 * vencimiento con la regla del 29-feb, y "DEF manda sobre #GC#".
 */
public class EstadoCalibracionTest {

    /** T-USR-03(a): #GC,2025-09-18# con reloj de prueba en 2026-09-19 → EQUIPO CON CALIBRACIÓN
     *  VENCIDA. Fuente: PROTOCOLO-V3.6.md:34,42; SPEC-REG:567-569 (RF-REG-02 a 04, PA-01). */
    @Test
    public void fechaDeHaceMasDeUnAnioEstaVencida() {
        EstadoCalibracion e = EstadoCalibracion.calcular(RespuestaV.EstadoAjuste.CAL, "2025-09-18",
                FechaISO.de(2026, 9, 19));

        assertEquals(EstadoCalibracion.Estado.VENCIDA, e.estado());
        assertEquals(EstadoCalibracion.TEXTO_VENCIDA, e.texto());
        assertEquals(FechaISO.de(2026, 9, 18), e.vencimiento());
    }

    /** T-USR-03(b): #GC,NONE# → "SIN CALIBRACIÓN" (mismo texto que DEF, aunque la causa sea otra). */
    @Test
    public void gcNoneEsSinCalibracion() {
        EstadoCalibracion e = EstadoCalibracion.calcular(RespuestaV.EstadoAjuste.CAL, "NONE",
                FechaISO.de(2026, 9, 19));

        assertEquals(EstadoCalibracion.Estado.SIN_REGISTRAR, e.estado());
        assertEquals(EstadoCalibracion.TEXTO_SIN_CALIBRACION, e.texto());
    }

    /** T-USR-03(c): #GC# de hace 30 días → vigente, sin aviso. */
    @Test
    public void fechaDeHace30DiasEstaVigenteSinAviso() {
        EstadoCalibracion e = EstadoCalibracion.calcular(RespuestaV.EstadoAjuste.CAL, "2026-08-20",
                FechaISO.de(2026, 9, 19));

        assertEquals(EstadoCalibracion.Estado.VIGENTE, e.estado());
        assertEquals("", e.texto());
    }

    /** T-USR-03b (PA-02, B-4): #GC,2024-02-29# (bisiesto) con reloj en 2025-03-01 → vencimiento
     *  calculado = 2025-02-28 (no 2025-03-01), y a esa fecha ya está vencida.
     *  Fuente: SPEC-Registro-Indicador-Interventoria.md:567 (RF-REG-02, "29-feb → 28-feb del año
     *  siguiente") y :609 (PA-02). */
    @Test
    public void veintinueveDeFebreroVenceElVeintiochoDelAnioSiguiente() {
        EstadoCalibracion e = EstadoCalibracion.calcular(RespuestaV.EstadoAjuste.CAL, "2024-02-29",
                FechaISO.de(2025, 3, 1));

        assertEquals(FechaISO.de(2025, 2, 28), e.vencimiento());
        assertEquals(EstadoCalibracion.Estado.VENCIDA, e.estado());
        assertEquals(EstadoCalibracion.TEXTO_VENCIDA, e.texto());
    }

    /** T-USR-22: "#V#" con DEF manda sobre "#GC#", aunque ésta traiga fecha.
     *  Fuente: calibracion_v36.c:635-642 (campo CAL/DEF de "#V#"); PROTOCOLO-V3.6.md:51
     *  ("#F y #FT no la tocan": #SC/#GC# puede seguir con fecha aunque la calibración esté de
     *  fábrica). */
    @Test
    public void defMandaSobreGcAunqueTengaFecha() {
        EstadoCalibracion e = EstadoCalibracion.calcular(RespuestaV.EstadoAjuste.DEF, "2025-01-01",
                FechaISO.de(2026, 9, 19));

        assertEquals(EstadoCalibracion.Estado.SIN_CALIBRACION_DEF, e.estado());
        assertEquals(EstadoCalibracion.TEXTO_SIN_CALIBRACION, e.texto());
    }

    /** Vista en rojo (CLAUDE.md §7): contra una fórmula que hiciera "fecha.plusDays(365)" en vez de
     *  la regla PA-02, este caso fallaría (2025-02-28 + 365 días = 2026-02-28, no lo que se pide
     *  aquí, pero además el propio 2024-02-29 + 365 días da 2025-02-28 por casualidad en este caso
     *  concreto: la diferencia real se ve con un año NO bisiesto de origen, donde +365 días SÍ
     *  coincide con "mismo mes y día", y con el propio 29-feb si se sumaran 366 días en vez de
     *  resolver el mes/día inválido). Se deja la prueba de bisiesto de FechaISO aparte, más directa. */
    @Test
    public void anioBisiestoSeCalculaConLaReglaDelCalendarioGregoriano() {
        // 2024, 2000 y 1600 son bisiestos (%4 y, si %100, tambien %400); 1900 y 2100 no.
        assertEquals(true, FechaISO.esBisiestoParaPrueba(2024));
        assertEquals(true, FechaISO.esBisiestoParaPrueba(2000));
        assertEquals(false, FechaISO.esBisiestoParaPrueba(1900));
        assertEquals(false, FechaISO.esBisiestoParaPrueba(2025));
    }
}
