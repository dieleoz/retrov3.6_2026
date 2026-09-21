package com.dpi.retrousuario.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Ritmo del enlace (RF-APP-01, `SPEC-V3.6.md:441-446`, citado por RF-USR-16): antes de cada envío,
 * espera **1500 ms desde el envío anterior Y 600 ms desde el último byte recibido, lo que se cumpla
 * más tarde**; tras un disparo anulado (plazo vencido o doble `::`), una **cuarentena** de `Q` ms en
 * la que se descarta todo lo recibido y no se envía nada, y sólo entonces la pausa normal (no se
 * solapan, RF-USR-16).
 *
 * <p>Toda espera se hace llamando a {@link FuenteBytes#leer(long)} hasta el instante límite, nunca
 * con `Thread.sleep`: en producción esa llamada bloquea de verdad; en pruebas, un doble de
 * {@link FuenteBytes} avanza un reloj simulado.</p>
 */
final class EmisorRitmo {

    private long tUltimoEnvio = Long.MIN_VALUE / 2;
    private long tUltimoByte = Long.MIN_VALUE / 2;

    /** Espera lo que exige RF-APP-01 y envía {@code b}; devuelve el instante de envío. */
    long enviarConPausa(FuenteBytes fuente, ParametrosRitmo params, int b) {
        long minimo = Math.max(tUltimoEnvio + params.pausaEnvioMs(), tUltimoByte + params.pausaSilencioMs());
        esperarHasta(fuente, minimo);
        fuente.enviarByte(b);
        tUltimoEnvio = fuente.ahoraMs();
        return tUltimoEnvio;
    }

    /**
     * Cuarentena de {@code params.cuarentenaMs()} desde {@code tInicio}: descarta todo lo recibido,
     * sin asignarlo a ningún disparo (RF-USR-16). No envía nada mientras dura.
     */
    List<ResultadoDisparo.ByteRecibido> cuarentena(FuenteBytes fuente, ParametrosRitmo params, long tInicio) {
        long limite = tInicio + params.cuarentenaMs();
        List<ResultadoDisparo.ByteRecibido> descartados = new ArrayList<>();
        while (fuente.ahoraMs() < limite) {
            Integer b = fuente.leer(limite);
            if (b == null) {
                break;
            }
            long t = fuente.ahoraMs();
            tUltimoByte = t; // cuenta para la pausa de 600 ms posterior, aunque el byte se descarte.
            descartados.add(new ResultadoDisparo.ByteRecibido(t - tInicio, b));
        }
        return descartados;
    }

    /** Registra el resultado de un disparo (no anulado) para el ritmo: actualiza el último byte recibido. */
    void registrarDisparo(ResultadoDisparo r) {
        if (!r.recibidos().isEmpty()) {
            tUltimoByte = r.tFin();
        }
    }

    private void esperarHasta(FuenteBytes fuente, long limite) {
        while (fuente.ahoraMs() < limite) {
            Integer b = fuente.leer(limite);
            if (b == null) {
                break;
            }
            tUltimoByte = fuente.ahoraMs(); // byte inesperado en la pausa: no se asigna a nada, sólo mueve el ritmo.
        }
    }

    long tUltimoByte() {
        return tUltimoByte;
    }
}
