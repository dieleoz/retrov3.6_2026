package com.dpi.retrov36;

/**
 * Acumulador de lo recibido para UNA peticion en vuelo. Java puro: el reloj
 * lo pone quien llama, para poder probar tramas partidas en varios read().
 *
 * - "::n" no lleva terminador (ecuacionesCalibracion.c:60-63): se da por
 *   cerrada tras SILENCIO_MS sin bytes despues del ultimo.
 * - "#...#", ":n:" y "@LEERV,...@" se cierran por su delimitador.
 * - Acotado: pasado MAX_BYTES se deja de acumular y se marca desbordado.
 */
public final class Receptor {

    public static final long SILENCIO_MS = 180;
    public static final int MAX_BYTES = 512;

    public enum Desenlace { VALIDA, TIMEOUT, INESPERADA }

    private final StringBuilder buf = new StringBuilder();
    private long ultimoByteMs = -1;
    private boolean desbordado;

    public synchronized void vaciar() {
        buf.setLength(0);
        ultimoByteMs = -1;
        desbordado = false;
    }

    public synchronized void agregar(long ahoraMs, byte[] datos) {
        for (byte b : datos) {
            if (buf.length() >= MAX_BYTES) {
                desbordado = true;
                break;
            }
            buf.append((char) (b & 0xFF));
        }
        ultimoByteMs = ahoraMs;
    }

    public synchronized String texto() {
        return buf.toString();
    }

    public synchronized boolean vacio() {
        return buf.length() == 0;
    }

    public synchronized boolean desbordado() {
        return desbordado;
    }

    /** -1 si aun no llego nada. */
    public synchronized long ultimoByteMs() {
        return ultimoByteMs;
    }

    /** Trama completa del tipo pedido, o null si aun no la hay. */
    public synchronized String completa(Tramas.Tipo tipo, long ahoraMs) {
        boolean silencio = ultimoByteMs >= 0 && ahoraMs - ultimoByteMs >= SILENCIO_MS;
        return Tramas.extraer(tipo, buf.toString(), silencio);
    }

    /**
     * Clasifica el final de la espera: VALIDA si hay trama; INESPERADA si
     * llegaron bytes que no la forman; TIMEOUT si no llego nada.
     */
    public synchronized Desenlace desenlace(Tramas.Tipo tipo, long ahoraMs) {
        if (completa(tipo, ahoraMs) != null) {
            return Desenlace.VALIDA;
        }
        return buf.length() == 0 ? Desenlace.TIMEOUT : Desenlace.INESPERADA;
    }
}
