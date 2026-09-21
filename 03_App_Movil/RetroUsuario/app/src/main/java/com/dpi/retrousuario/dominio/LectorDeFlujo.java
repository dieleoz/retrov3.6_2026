package com.dpi.retrousuario.dominio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;

/**
 * QA sobre RetroUsuario 0.3.3: el bucle de lectura del hilo lector de {@code EnlaceBluetooth} (antes
 * {@code EnlaceBluetooth.leerSinParar}, sólo en la capa Android) no tenía ningún arnés de pruebas en
 * este árbol — quitar la llamada a
 * {@link EstadoEnlace#marcarCaido()} del {@code catch} seguía dejando las 126 pruebas JVM en verde.
 * Se saca aquí, dominio puro ({@link InputStream}/{@link Queue}, sin {@code BluetoothSocket}), para
 * poder romperlo en la JVM con un flujo doble.
 *
 * <p>Comportamiento, igual que el bucle que reemplaza: acumula cada byte leído (como entero sin
 * signo, 0-255) en {@code destino} mientras el flujo entregue bytes ({@code read()} &gt;= 0); una
 * {@link IOException} O el fin de flujo sin excepción ({@code read()} == -1, EOF: el otro lado pudo
 * cerrar limpio en vez de romper) marcan el enlace caído en {@code estado} y terminan el bucle.</p>
 */
public final class LectorDeFlujo {

    private LectorDeFlujo() {
    }

    /**
     * Bloquea leyendo de {@code entrada} hasta que se cae (excepción o EOF); llamar desde un hilo
     * de fondo dedicado ({@code EnlaceBluetooth} lo hace desde su hilo "lector-spp-usuario").
     *
     * @param entrada  el flujo del socket (o su doble, en una prueba).
     * @param destino  cola donde se acumulan los bytes recibidos, como enteros 0-255.
     * @param estado   se marca {@link EstadoEnlace#marcarCaido() caído} al terminar el bucle, por
     *                 cualquiera de las dos causas.
     */
    public static void leer(InputStream entrada, Queue<Integer> destino, EstadoEnlace estado) {
        byte[] b = new byte[256];
        try {
            int n;
            while ((n = entrada.read(b)) >= 0) {
                for (int i = 0; i < n; i++) {
                    destino.add(b[i] & 0xFF);
                }
            }
            // Fin de flujo sin excepcion (EOF) tambien es el enlace caido, no solo la IOException de
            // abajo: el otro lado pudo cerrar limpio en vez de romper.
            estado.marcarCaido();
        } catch (IOException cerrado) {
            // El socket se cerro (desconexion voluntaria o perdida de enlace): el hilo termina solo.
            estado.marcarCaido();
        }
    }
}
