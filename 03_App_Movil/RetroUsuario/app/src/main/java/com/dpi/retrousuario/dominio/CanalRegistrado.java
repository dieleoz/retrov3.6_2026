package com.dpi.retrousuario.dominio;

/**
 * Decora un {@link Canal} para que cada `#...#` (detección RF-USR-01, sonda RF-USR-02) quede en el
 * mismo {@link RegistroTramas} que los disparos `::<n>` (RF-USR-15 bis): sin esto, `tramas.log` no
 * llevaría la detección ni la sonda, y la comparación de T-USR-19a(c) (proyección TX del registro
 * contra lo que recibió el simulador) no cuadraría.
 *
 * <p><b>M2, corrige un borrador anterior:</b> esta clase existía y la usaban las pruebas
 * (T-USR-19a), pero la capa Android (MainActivity) llamaba a {@link DetectorEquipo#detectar} y a
 * {@link Sonda362#sondear} directamente sobre el enlace, sin pasar por aquí: en producción
 * `tramas.log` no llevaba ni la detección ni la sonda. Pública para que MainActivity pueda montar
 * exactamente el mismo cableado que usan las pruebas: {@code new CanalRegistrado(enlace, log, enlace)}.</p>
 */
public final class CanalRegistrado implements Canal {

    private final Canal delegado;
    private final RegistroTramas log;
    private final FuenteBytes reloj;

    public CanalRegistrado(Canal delegado, RegistroTramas log, FuenteBytes reloj) {
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
