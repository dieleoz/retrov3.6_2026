package com.dpi.retrousuario.dominio;

/**
 * Arq ALTO (sobre 0.3.2, {@code EnlaceBluetooth.vivo()}, `com.dpi.retrousuario.EnlaceBluetooth.java:69-71`
 * de 385fc92): antes, {@code vivo()} delegaba entero en {@code socket.isConnected()}, que Android sólo
 * pone a {@code false} cuando la app llama {@code close()} — si el equipo se cae a medio camino, el
 * hilo lector traga la {@code IOException} (`EnlaceBluetooth.java:82-84` de 385fc92) y el socket queda
 * "conectado" para siempre a ojos de Android, aunque no haya nadie al otro lado: {@link GestorEnlace}
 * decidía REUTILIZAR sobre un socket muerto, y la detección volvía a dar "equipo no compatible" en
 * bucle sin que el operador pudiera salir de ahí más que cerrando y reabriendo la app.
 *
 * Este estado se separa del socket real para poder probarlo en la JVM sin {@code BluetoothSocket}
 * (capa Android, sin arnés de pruebas en este árbol): {@code EnlaceBluetooth} sólo delega aquí —
 * {@link #marcarCaido()} en el catch de su hilo lector, al salir de su bucle de lectura (fin de
 * flujo sin excepción, EOF), y si {@code enviar()}/{@code enviarByte()} fallan al escribir — y
 * calcula {@code vivo()} como {@link #vivo(boolean)} con el {@code socket.isConnected()} real.
 *
 * "Caído" no se puede quitar: una vez puesto, el enlace no vuelve a considerarse vivo aunque el
 * socket siga reportando conectado (no hay reconexión automática en este incremento, RF-USR-01).
 */
public final class EstadoEnlace {

    private volatile boolean caido = false;

    /** Hilo lector (catch de la IOException, o fin normal del bucle), o un envío fallido. */
    public void marcarCaido() {
        caido = true;
    }

    public boolean caido() {
        return caido;
    }

    /**
     * @param socketConectado {@code socket.isConnected()} de Android (o su equivalente simulado en
     *                        una prueba).
     * @return {@code false} si ya se marcó {@link #marcarCaido()}, aunque el socket diga que sigue
     *         conectado; si no, el valor de {@code socketConectado} tal cual.
     */
    public boolean vivo(boolean socketConectado) {
        return !caido && socketConectado;
    }
}
