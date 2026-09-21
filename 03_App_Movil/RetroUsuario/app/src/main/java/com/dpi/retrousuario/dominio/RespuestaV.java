package com.dpi.retrousuario.dominio;

/**
 * Respuesta a "#V#": {@code #V,3.6,<AAAA-MM-DD>,CAL|DEF,<mascara %04X>#}.
 *
 * Fuente: {@code calibracion_v36.c:635-642} —
 * {@code responder("#V," FW_VERSION_STR ","); enviarFecha(); responder(x ? ",CAL," : ",DEF,");
 * sprintf(num, "%04X", x); sendUartStr(num); UART1_Write('#');} — con {@code FW_VERSION_STR}
 * = {@code "3.6"} ({@code calibracion_v36.h:16}) y {@code enviarFecha()} en {@code :458-474}
 * ({@code __DATE__}, fecha de COMPILACIÓN, no la de hoy). La mascara es el valor de
 * {@code mascaraAjustes()} (bit i = codigo i ajustado, bit 12 = temperatura); esta app sólo usa si
 * es 0 (equivale a DEF, "x ? CAL : DEF" en el propio firmware) o distinto de 0 (CAL), no descompone
 * los bits todavia.
 */
public final class RespuestaV {

    /** DEF: usa los coeficientes de fabrica; CAL: hay al menos un ajuste en EEPROM. */
    public enum EstadoAjuste { CAL, DEF }

    private final String version;
    private final String fechaCompilacion;
    private final EstadoAjuste estadoAjuste;
    private final String mascaraHex;

    private RespuestaV(String version, String fechaCompilacion, EstadoAjuste estadoAjuste, String mascaraHex) {
        this.version = version;
        this.fechaCompilacion = fechaCompilacion;
        this.estadoAjuste = estadoAjuste;
        this.mascaraHex = mascaraHex;
    }

    /**
     * @param respuesta cruda ya reconocida como "empieza por #V,3.6," (RF-USR-01); si no tiene
     *                   los 5 campos esperados, devuelve null (trama rara: se trata como silencio).
     */
    public static RespuestaV analizar(String respuesta) {
        RespuestaTrama t = RespuestaTrama.analizar(respuesta);
        if (t.esInvalida() || t.esError() || t.numeroCampos() != 5 || !"V".equals(t.campo(0))) {
            return null;
        }
        String version = t.campo(1);
        String fecha = t.campo(2);
        String calDef = t.campo(3);
        String mascara = t.campo(4);
        EstadoAjuste estado;
        if ("CAL".equals(calDef)) {
            estado = EstadoAjuste.CAL;
        } else if ("DEF".equals(calDef)) {
            estado = EstadoAjuste.DEF;
        } else {
            return null;
        }
        return new RespuestaV(version, fecha, estado, mascara);
    }

    /** "3.6" siempre en esta linea (3.6, 3.6.1 y 3.6.2 responden el mismo texto de version, PROTOCOLO-V3.6.md §4 ter). */
    public String version() {
        return version;
    }

    /** AAAA-MM-DD de __DATE__: fecha de compilacion del firmware, no la de hoy. */
    public String fechaCompilacion() {
        return fechaCompilacion;
    }

    public EstadoAjuste estadoAjuste() {
        return estadoAjuste;
    }

    public String mascaraHex() {
        return mascaraHex;
    }

    /** true si empieza por "#V,3.6," (RF-USR-01): unica condicion de compatibilidad de esta app. */
    public static boolean esCompatible(String respuestaCruda) {
        return respuestaCruda != null && respuestaCruda.startsWith("#V,3.6,");
    }
}
