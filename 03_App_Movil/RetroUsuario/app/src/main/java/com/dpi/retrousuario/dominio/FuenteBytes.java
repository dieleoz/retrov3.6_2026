package com.dpi.retrousuario.dominio;

/**
 * Enlace visto byte a byte, con reloj propio (RF-USR-16, segunda parte del incremento 1). A
 * diferencia de {@link Canal} (una trama "#...#" completa, plazo único), esta interfaz es la que
 * necesita el protocolo de disparo {@code ::<n>}: sin terminador, con plazo, silencio, pausa de
 * ritmo y cuarentena, todo medido en el reloj que devuelve {@link #ahoraMs()} — el mismo reloj que
 * usan {@link #leer(long)} para decidir cuándo vencer.
 *
 * <p>Toda espera del dominio (silencio de {@code ::<n>}, plazo de un disparo, cuarentena, pausa de
 * ritmo) pasa por {@link #leer(long)}: en producción bloquea de verdad hasta el instante dado; en
 * las pruebas, un doble de esta interfaz avanza un reloj simulado sin ningún {@code Thread.sleep}
 * real (encargo: "reloj inyectable, nada de sleep real en pruebas").</p>
 */
public interface FuenteBytes {

    /** Instante actual, en el mismo reloj que {@link #leer(long)}. */
    long ahoraMs();

    /**
     * Envía un único byte (el código de color '1'-'6', RF-USR-03) y marca el instante de envío
     * para el ritmo (RF-USR-16, "1500 ms desde el envío anterior").
     */
    void enviarByte(int b);

    /**
     * Espera hasta que llegue un byte o hasta {@code limiteMs} (instante absoluto, no duración).
     *
     * @return el byte recibido (0-255), o {@code null} si se alcanzó {@code limiteMs} sin nada.
     */
    Integer leer(long limiteMs);
}
