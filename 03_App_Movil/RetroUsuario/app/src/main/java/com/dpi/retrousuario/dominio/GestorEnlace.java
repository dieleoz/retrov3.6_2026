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
}
