package com.dpi.retrousuario.dominio;

/**
 * Decora un {@link Canal} para que cada `#...#` (detección RF-USR-01, sonda RF-USR-02) quede en el
 * mismo {@link RegistroTramas} que los disparos `::<n>` (RF-USR-15 bis): sin esto, `tramas.log` no
 * llevaría la detección ni la sonda, y la comparación de T-USR-19a(c) (proyección TX del registro
 * contra lo que recibió el simulador) no cuadraría.
 */
final class CanalRegistrado implements Canal {

    private final Canal delegado;
    private final RegistroTramas log;
    private final FuenteBytes reloj;

    CanalRegistrado(Canal delegado, RegistroTramas log, FuenteBytes reloj) {
        this.delegado = delegado;
        this.log = log;
        this.reloj = reloj;
    }

    @Override
    public String enviar(String trama, long plazoMs) {
        log.tx(reloj.ahoraMs(), trama);
        String respuesta = delegado.enviar(trama, plazoMs);
        if (respuesta != null) {
            log.rx(reloj.ahoraMs(), respuesta);
        }
        return respuesta;
    }
}
