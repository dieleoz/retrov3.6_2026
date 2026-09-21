package com.dpi.retrousuario.dominio;

/**
 * RF-USR-01, primera parte: detección sólo V3.6, con aviso previo (que pinta la interfaz, no este
 * dominio) y **un solo byte enviado**: {@code #V#}. Nunca {@code 9}, {@code 6}, {@code e} ni
 * {@code @LEERV...} — a diferencia de la app de empresa (RTV10, README "Pruebas del equipo"), que sí
 * los envía para distinguir V3 de 2020. Esta app los descarta directamente como "no compatible": el
 * alcance es sólo equipos V3.6 (USR-ALCANCE, SPEC §0).
 *
 * Fuente: PROTOCOLO-V3.6.md:42 (formato de "#V#"); SPEC-App-Usuario-V3.6.md r4, RF-USR-01; en un V3
 * de 2020 el primer byte de cualquier trama dispara la luz —{@code gui.c:294-296} de
 * {@code 01_Firmware/base_2020_d089f962/}: {@code activador = !BOTON_INICIO_GetValue(); if
 * (activador || bufferIndex > 0) { ... adquiere ... }}, y el '#' de "#V#" ya deja
 * {@code bufferIndex > 0}—, por eso el aviso previo de la pantalla (RF-USR-01) tiene que estar
 * visible ANTES de este envío (T-USR-02, verificado por lectura, sin código propio que probar aquí).
 */
public final class DetectorEquipo {

    /** M5, corrige un valor anterior: plazo de una trama "#...#" (`plazoTramaMs`, SPEC-V3.6.md:455),
     *  2000 ms — distinto del plazo de 2500 ms de un disparo "::&lt;n&gt;" (RF-USR-16), que es otro
     *  parámetro. Se mantiene como valor por defecto de conveniencia; {@link #detectar(Canal,
     *  ParametrosRitmo)} usa siempre {@link ParametrosRitmo#plazoTramaMs()}. */
    public static final long PLAZO_MS = 2000;

    public enum Resultado { COMPATIBLE, NO_COMPATIBLE }

    public static final class ResultadoDeteccion {
        private final Resultado resultado;
        private final RespuestaV respuestaV;
        private final boolean sinRespuesta;

        private ResultadoDeteccion(Resultado resultado, RespuestaV respuestaV, boolean sinRespuesta) {
            this.resultado = resultado;
            this.respuestaV = respuestaV;
            this.sinRespuesta = sinRespuesta;
        }

        public Resultado resultado() {
            return resultado;
        }

        /** No nulo sólo si {@link #resultado()} es {@link Resultado#COMPATIBLE}. */
        public RespuestaV respuestaV() {
            return respuestaV;
        }

        /** Arq ALTO (sobre 0.3.2): true si "#V#" no trajo NADA dentro del plazo (silencio: el canal
         *  devolvió null) — a diferencia de una trama que sí llegó pero no es compatible (otro
         *  firmware, "#ERR#"...), que es una respuesta de verdad. Siempre false si
         *  {@link #resultado()} es {@link Resultado#COMPATIBLE}. Sólo el silencio sobre un enlace
         *  REUTILIZADO puede ser, en realidad, un socket muerto ({@link GestorEnlace#debeReintentarConexionNueva}). */
        public boolean sinRespuesta() {
            return sinRespuesta;
        }
    }

    private DetectorEquipo() {
    }

    /**
     * Envía **únicamente** "#V#" y decide. Silencio (canal.enviar devuelve null), una trama que no
     * empieza por "#V,3.6," (otro firmware, otra version, "#ERR,...#", basura) o una que sí empieza
     * así pero no tiene 4 o 5 campos (D-3, corrige un comentario desactualizado: {@link RespuestaV}
     * acepta las dos formas, r6 C3) — en cualquiera de esos casos: NO_COMPATIBLE, sin enviar ningún
     * otro byte (RF-USR-01: "nunca 9, 6, e, @LEERV...").
     */
    public static ResultadoDeteccion detectar(Canal canal) {
        return detectar(canal, new ParametrosRitmo());
    }

    /** M5: plazo de la trama "#V#" con el {@code plazoTramaMs} de {@code params} (2000 ms por defecto). */
    public static ResultadoDeteccion detectar(Canal canal, ParametrosRitmo params) {
        String respuesta = canal.enviar("#V#", params.plazoTramaMs());
        if (!RespuestaV.esCompatible(respuesta)) {
            return new ResultadoDeteccion(Resultado.NO_COMPATIBLE, null, respuesta == null);
        }
        RespuestaV v = RespuestaV.analizar(respuesta);
        if (v == null) {
            return new ResultadoDeteccion(Resultado.NO_COMPATIBLE, null, false);
        }
        return new ResultadoDeteccion(Resultado.COMPATIBLE, v, false);
    }
}
