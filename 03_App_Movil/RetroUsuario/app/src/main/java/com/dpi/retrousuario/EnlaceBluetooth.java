package com.dpi.retrousuario;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.dpi.retrousuario.dominio.Canal;
import com.dpi.retrousuario.dominio.FuenteBytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Enlace SPP mínimo para esta app, reescrito de
 * {@code rtv-1.0:03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/EnlaceSerie.java} (308
 * líneas), **sin** {@code Registro.*} (SPEC-App-Usuario-V3.6.md §3: "se tocan EnlaceSerie.java (308
 * líneas, sin Registro.*, 9 llamadas :188-272)"). Implementa {@link Canal} (tramas "#...#",
 * RF-USR-01/02) **y** {@link FuenteBytes} (disparos {@code ::<n>}, RF-USR-04/16, segunda parte de
 * este incremento): las dos comparten el mismo socket y la misma cola de bytes recibidos.
 *
 * "El búfer de recepción se vacía antes de cada envío" (RF-USR-16): se aplica en los dos, para no
 * arrastrar un resto de una trama o un disparo anterior.
 */
final class EnlaceBluetooth implements Canal, FuenteBytes {

    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int LIMITE_TRAMA = 100; // PROTOCOLO-V3.6.md §4 bis O-01: buffer de 100.

    private final BluetoothSocket socket;
    private final OutputStream salida;
    private final LinkedBlockingQueue<Integer> bytesRecibidos = new LinkedBlockingQueue<>();
    private final long tApertura = System.currentTimeMillis(); // M2: t_ms de tramas.log es desde aqui.
    private final String macConectada;

    private EnlaceBluetooth(BluetoothSocket socket, OutputStream salida, InputStream entrada, String macConectada) {
        this.socket = socket;
        this.salida = salida;
        this.macConectada = macConectada;
        Thread lector = new Thread(() -> leerSinParar(entrada), "lector-spp-usuario");
        lector.setDaemon(true);
        lector.start();
    }

    /** Conecta de forma síncrona; llamar fuera del hilo de interfaz. */
    static EnlaceBluetooth conectar(BluetoothDevice dispositivo) throws IOException {
        BluetoothSocket s;
        try {
            s = dispositivo.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
            s.connect();
        } catch (IOException | SecurityException primerIntento) {
            s = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP);
            s.connect();
        }
        return new EnlaceBluetooth(s, s.getOutputStream(), s.getInputStream(), dispositivo.getAddress());
    }

    /** A2: la MAC del dispositivo con el que este enlace REALMENTE conectó (no la del último elegido
     *  en la lista, que puede no coincidir si la conexión falló a mitad). */
    String macConectada() {
        return macConectada;
    }

    private void leerSinParar(InputStream entrada) {
        byte[] b = new byte[256];
        try {
            int n;
            while ((n = entrada.read(b)) >= 0) {
                for (int i = 0; i < n; i++) {
                    bytesRecibidos.add(b[i] & 0xFF);
                }
            }
        } catch (IOException cerrado) {
            // El socket se cerro (desconexion voluntaria o perdida de enlace): el hilo termina solo.
        }
    }

    @Override
    public String enviar(String trama, long plazoMs) {
        bytesRecibidos.clear(); // RF-USR-16: vaciar antes de cada envio.
        try {
            salida.write(trama.getBytes(StandardCharsets.US_ASCII));
            salida.flush();
        } catch (IOException e) {
            return null;
        }
        return esperarTrama(plazoMs);
    }

    /** Acumula bytes hasta formar "#...#" completa, o hasta que venza el plazo: entonces null. */
    private String esperarTrama(long plazoMs) {
        StringBuilder acumulado = new StringBuilder();
        long limite = System.currentTimeMillis() + plazoMs;
        boolean abierta = false;
        while (System.currentTimeMillis() < limite) {
            long restante = limite - System.currentTimeMillis();
            Integer b;
            try {
                b = bytesRecibidos.poll(Math.max(1, restante), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (b == null) {
                continue; // vuelve a comprobar el plazo.
            }
            char c = (char) b.intValue();
            if (!abierta) {
                if (c != '#') {
                    continue; // descarta bytes sueltos antes del primer '#', como el firmware con "#...#".
                }
                abierta = true;
                acumulado.append(c);
                continue;
            }
            acumulado.append(c);
            if (c == '#') {
                return acumulado.toString(); // segundo '#': trama completa.
            }
            if (acumulado.length() >= LIMITE_TRAMA) {
                return null; // trama mas larga que el limite del protocolo: se descarta (silencio).
            }
        }
        return null;
    }

    // --- FuenteBytes: disparos "::<n>" (RF-USR-04/16). Comparte socket y cola con Canal, arriba. ---

    /** M2: relativo a la apertura del enlace (no epoch absoluto), para que t_ms de tramas.log sea legible. */
    @Override
    public long ahoraMs() {
        return System.currentTimeMillis() - tApertura;
    }

    @Override
    public void enviarByte(int b) {
        bytesRecibidos.clear(); // RF-USR-16: vaciar antes de cada envio.
        try {
            salida.write(b);
            salida.flush();
        } catch (IOException e) {
            // Sin conexion: el disparo se trata como "sin respuesta" (plazo vencido), igual que un timeout.
        }
    }

    @Override
    public Integer leer(long limiteMs) {
        long restante = limiteMs - System.currentTimeMillis();
        if (restante <= 0) {
            return null;
        }
        try {
            return bytesRecibidos.poll(restante, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    void desconectar() {
        try {
            socket.close();
        } catch (IOException e) {
            // Cerrar un socket ya roto lanza; no cambia nada.
        }
    }
}
