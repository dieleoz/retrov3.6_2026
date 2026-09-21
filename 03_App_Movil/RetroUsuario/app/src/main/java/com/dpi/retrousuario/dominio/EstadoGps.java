package com.dpi.retrousuario.dominio;

/**
 * M-8 (▸ propuesta de este trabajo, sin cita de Diego, condición QA-7): una posición GPS vieja o
 * imprecisa no sirve más que no tener ninguna. RF-USR-15: esto nunca bloquea la medida, sólo decide
 * qué van a decir {@code latitud}/{@code longitud}/{@code gps_estado} en la fila. Lógica pura (sin
 * Android): {@link com.dpi.retrousuario.UbicacionGps} sólo lee el {@code Location} del sistema y le
 * pasa la edad y la precisión a esta clase.
 */
public final class EstadoGps {

    /** Antigüedad máxima de una posición para usarla: 2 minutos (▸ propuesta, sin cita de Diego). */
    public static final long EDAD_MAXIMA_MS = 2 * 60 * 1000L;

    /** Precisión mínima exigida: 50 m (▸ propuesta, sin cita de Diego). */
    public static final float PRECISION_MAXIMA_M = 50f;

    /** Valor de {@code gps_estado} en la fila (RF-USR-06/RF-USR-15 bis). */
    public enum Resultado { CON_POSICION, POSICION_ANTIGUA, SIN_POSICION }

    private EstadoGps() {
    }

    /**
     * @param hayPosicion false si el sistema no dio ninguna última posición conocida.
     * @param edadMs      edad de la posición en ms ({@code ahora - location.getTime()}); ignorado si
     *                    {@code hayPosicion} es false.
     * @param precisionM  precisión en metros ({@code location.getAccuracy()}), o {@code Float.NaN}
     *                    si el sistema no la reporta (no se descarta por precisión desconocida).
     */
    public static Resultado evaluar(boolean hayPosicion, long edadMs, float precisionM) {
        if (!hayPosicion) {
            return Resultado.SIN_POSICION;
        }
        if (edadMs > EDAD_MAXIMA_MS) {
            return Resultado.POSICION_ANTIGUA; // se descarta, pero se distingue de "nunca hubo".
        }
        if (!Float.isNaN(precisionM) && precisionM > PRECISION_MAXIMA_M) {
            return Resultado.SIN_POSICION; // imprecisa: tan inútil como no tener ninguna.
        }
        return Resultado.CON_POSICION;
    }

    /** Texto de columna {@code gps_estado} (RF-USR-06/RF-USR-15 bis). */
    public static String texto(Resultado r) {
        switch (r) {
            case CON_POSICION:
                return "con_posicion";
            case POSICION_ANTIGUA:
                return "posicion_antigua";
            default:
                return "sin_posicion";
        }
    }
}
