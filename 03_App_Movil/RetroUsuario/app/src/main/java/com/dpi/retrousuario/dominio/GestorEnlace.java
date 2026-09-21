package com.dpi.retrousuario.dominio;

/**
 * Condición C1 (arquitecto-iot sobre 0.3.1, MainActivity.java:147-154 de 766e6f2): fuga de socket.
 * Tras NO_COMPATIBLE, ACTUALIZAR_FIRMWARE o reintentos agotados, el enlace quedaba abierto (en el
 * campo de la Activity o en {@code SesionHolder}) y elegir otro equipo, o el MISMO, no lo cerraba —
 * abría un segundo socket RFCOMM en vez de reutilizar o cerrar el primero.
 *
 * Esta clase sólo decide, en dominio puro (sin Bluetooth, sin Android), qué hacer con un enlace
 * anterior antes de conectar a un equipo: <b>se reutiliza sólo si la MAC coincide y el enlace
 * anterior sigue vivo</b>; en cualquier otro caso (MAC distinta, o el anterior ya no está vivo) se
 * cierra antes de abrir uno nuevo. Quien tiene la referencia real al socket ({@code MainActivity})
 * es quien cierra y conecta; esta clase sólo entrega la decisión.
 */
public final class GestorEnlace {

    public enum Decision {
        /** No había ningún enlace anterior: conectar sin cerrar nada. */
        CONECTAR_NUEVO,
        /** Había un enlace anterior pero no es reutilizable (MAC distinta, o ya no está vivo): se
         *  cierra antes de conectar el nuevo. */
        CERRAR_Y_CONECTAR,
        /** El enlace anterior es del MISMO equipo y sigue vivo: se reutiliza tal cual, sin conectar
         *  ni cerrar nada (giro de pantalla, o "mismo equipo" tras NO_COMPATIBLE/reintentos agotados). */
        REUTILIZAR
    }

    private GestorEnlace() {
    }

    /**
     * @param macEnlaceAnterior MAC del enlace ya abierto (campo de la Activity, o
     *                          {@code SesionHolder}), o {@code null} si no hay ninguno.
     * @param enlaceAnteriorVivo si ese enlace anterior sigue conectado (el socket, no la sesión);
     *                           ignorado si {@code macEnlaceAnterior} es {@code null}.
     * @param macDestino MAC del equipo que el operador acaba de elegir.
     */
    public static Decision decidir(String macEnlaceAnterior, boolean enlaceAnteriorVivo, String macDestino) {
        if (macEnlaceAnterior == null) {
            return Decision.CONECTAR_NUEVO;
        }
        if (enlaceAnteriorVivo && macEnlaceAnterior.equals(macDestino)) {
            return Decision.REUTILIZAR;
        }
        return Decision.CERRAR_Y_CONECTAR;
    }

    /**
     * Arq ALTO, segunda parte (sobre 0.3.2): un enlace REUTILIZADO puede estar muerto sin que
     * {@code EnlaceBluetooth.vivo()} lo supiera todavía — la caída se detecta AL LEER o AL ENVIAR,
     * no antes de intentarlo ({@link EstadoEnlace}) — así que un primer silencio a "#V#" sobre un
     * REUTILIZADO no basta para concluir "no compatible": puede ser el equipo real diciendo que no
     * es 3.6.x, o puede ser un socket que ya no escucha nadie. Antes de concluir "no compatible" se
     * cierra ese enlace y se reintenta con una conexión nueva.
     *
     * <p>Sólo dispara con silencio real (nada recibido dentro del plazo,
     * {@link DetectorEquipo.ResultadoDeteccion#sinRespuesta()}): una trama que SÍ llegó pero no es
     * compatible (otro firmware, "#ERR#"...) es una respuesta de verdad, no un enlace muerto — ese
     * caso concluye "no compatible" a la primera, con o sin reutilización.</p>
     *
     * <p><b>"UNA vez":</b> tras el reintento la conexión ya no es reutilizada (es nueva), así que
     * una segunda llamada con {@code sobreEnlaceReutilizado = false} siempre da {@code false} — no
     * hace falta un contador aparte, lo impone el propio parámetro.</p>
     *
     * @param sobreEnlaceReutilizado true si la detección se hizo con {@link Decision#REUTILIZAR}.
     * @param sinRespuesta {@link DetectorEquipo.ResultadoDeteccion#sinRespuesta()} de esa detección.
     */
    public static boolean debeReintentarConexionNueva(boolean sobreEnlaceReutilizado, boolean sinRespuesta) {
        return sobreEnlaceReutilizado && sinRespuesta;
    }
}
