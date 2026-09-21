package com.dpi.retrousuario.dominio;

/**
 * Enlace con el equipo, visto desde el dominio: enviar una trama y esperar la respuesta hasta un
 * plazo. Sin dependencia de Android, para poder probar el dominio en la JVM contra un equipo
 * simulado (CLAUDE.md §8, receta R-JVM de TDD-V3.6.md §8).
 *
 * La capa Bluetooth real (com.dpi.retrousuario.EnlaceBluetooth) implementa esta interfaz.
 */
public interface Canal {

    /**
     * Envia {@code trama} tal cual (sin bytes de mas) y espera una respuesta completa
     * ({@code "#...#"}) hasta {@code plazoMs}.
     *
     * @return la respuesta cruda, con sus dos '#', o {@code null} si no llego nada completo dentro
     *         del plazo (silencio: PROTOCOLO-V3.6.md no distingue enlace caido de firmware mudo).
     */
    String enviar(String trama, long plazoMs);
}
