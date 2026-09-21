package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * QA-7: una posición de más de 2 min o con precisión peor que 50 m no cuenta como "con posición".
 * Vista en rojo contra 32c785d (condición QA-7): {@code EstadoGps} no existía; {@code UbicacionGps}
 * devolvía la primera posición conocida sin mirar edad ni precisión, así que una posición de hace 10
 * minutos o con 500 m de margen de error se reportaba igual que una fresca y precisa.
 */
public class EstadoGpsTest {

    @Test
    public void sinPosicionEsSinPosicion() {
        assertEquals(EstadoGps.Resultado.SIN_POSICION, EstadoGps.evaluar(false, 0, 0f));
    }

    @Test
    public void posicionFrescaYPrecisaEsConPosicion() {
        assertEquals(EstadoGps.Resultado.CON_POSICION, EstadoGps.evaluar(true, 0, 10f));
        assertEquals(EstadoGps.Resultado.CON_POSICION, EstadoGps.evaluar(true, EstadoGps.EDAD_MAXIMA_MS, 50f));
    }

    /** Más de 2 min (120000 ms): posición antigua, aunque la precisión sea buena. */
    @Test
    public void masDeDosMinutosEsPosicionAntigua() {
        assertEquals(EstadoGps.Resultado.POSICION_ANTIGUA,
                EstadoGps.evaluar(true, EstadoGps.EDAD_MAXIMA_MS + 1, 5f));
    }

    /** Precisión peor que 50 m (más metros de margen de error): se trata como si no hubiera posición. */
    @Test
    public void precisionPeorQue50mEsSinPosicion() {
        assertEquals(EstadoGps.Resultado.SIN_POSICION,
                EstadoGps.evaluar(true, 0, EstadoGps.PRECISION_MAXIMA_M + 0.1f));
    }

    /** Precisión desconocida (NaN, el sistema no la reporta): no se descarta por eso. */
    @Test
    public void precisionDesconocidaNoDescarta() {
        assertEquals(EstadoGps.Resultado.CON_POSICION, EstadoGps.evaluar(true, 0, Float.NaN));
    }

    @Test
    public void textoDeCadaResultado() {
        assertEquals("con_posicion", EstadoGps.texto(EstadoGps.Resultado.CON_POSICION));
        assertEquals("posicion_antigua", EstadoGps.texto(EstadoGps.Resultado.POSICION_ANTIGUA));
        assertEquals("sin_posicion", EstadoGps.texto(EstadoGps.Resultado.SIN_POSICION));
    }
}
