package com.dpi.retrov36;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enlace SPP para diagnostico. Basado en EnlaceBluetooth de RetroCotejoV1,
 * con dos diferencias deliberadas:
 *
 * 1. NO hay protocolo: se envian bytes crudos y se reciben bytes crudos. No se
 *    espera ninguna trama ni ningun terminador; el firmware de 2020 responde
 *    "::numero" sin terminador.
 *
 * 2. Un hilo lector permanente. Todo lo que llegue, se haya pedido o no, se
 *    registra con su marca de tiempo. Nada se vacia ni se descarta: en un
 *    diagnostico, un byte inesperado es precisamente un dato.
 *
 * La marca de tiempo de RX es la de retorno de read(): incluye la latencia de
 * la pila Bluetooth del movil, que no se puede separar desde aqui.
 */
public final class EnlaceSerie {

    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final EnlaceSerie INSTANCIA = new EnlaceSerie();

    public interface OyenteRx {
        /** Llamado desde el hilo lector, NO desde el de interfaz. */
        void rx(long tMs, byte[] datos);
    }

    public interface OyenteEstado {
        /** Llamado en el hilo de interfaz. */
        void estado(boolean conectado, String texto);
    }

    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<OyenteRx> oyentesRx = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OyenteEstado> oyentesEstado = new CopyOnWriteArrayList<>();
    private final Object cerrojoEscritura = new Object();

    private volatile BluetoothSocket socket;
    private volatile OutputStream salida;
    private volatile boolean conectando;
    private volatile String nombre = "";
    private volatile String mac = "";
    private volatile String textoEstado = "Sin conexión";
    /** SystemClock.elapsedRealtime() del ultimo write() terminado; 0 si ninguno. */
    private volatile long ultimoEnvio;

    private EnlaceSerie() { }

    public static EnlaceSerie instancia() {
        return INSTANCIA;
    }

    public static BluetoothAdapter adaptador() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public void agregarOyenteRx(OyenteRx o) {
        oyentesRx.addIfAbsent(o);
    }

    public void quitarOyenteRx(OyenteRx o) {
        oyentesRx.remove(o);
    }

    public void agregarOyenteEstado(OyenteEstado o) {
        oyentesEstado.addIfAbsent(o);
    }

    public void quitarOyenteEstado(OyenteEstado o) {
        oyentesEstado.remove(o);
    }

    /** Conectado solo si connect() termino sin excepcion y el socket sigue abierto. */
    public boolean estaConectado() {
        BluetoothSocket s = socket;
        return s != null && s.isConnected() && salida != null;
    }

    /** Milisegundos desde el ultimo envio; Long.MAX_VALUE si no hubo ninguno. */
    public long msDesdeUltimoEnvio() {
        long u = ultimoEnvio;
        return u == 0 ? Long.MAX_VALUE : android.os.SystemClock.elapsedRealtime() - u;
    }

    public boolean estaConectando() {
        return conectando;
    }

    public String getTextoEstado() {
        return textoEstado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMac() {
        return mac;
    }

    private void publicarEstado(final boolean conectado, final String texto) {
        textoEstado = texto;
        hiloPrincipal.post(new Runnable() {
            @Override
            public void run() {
                for (OyenteEstado o : oyentesEstado) {
                    o.estado(conectado, texto);
                }
            }
        });
    }

    /**
     * Conecta en un hilo propio. Primero con socket inseguro (como la app de
     * cotejo); si falla, reintenta con socket seguro. Ambos motivos se muestran
     * si fallan los dos.
     */
    public void conectar(final Context ctx, final BluetoothDevice d) {
        if (conectando) {
            return;
        }
        conectando = true;
        final Context app = ctx.getApplicationContext();
        cerrar();
        final String n = nombreDe(d);
        final String m = d.getAddress();
        publicarEstado(false, "Conectando con " + n + " (" + m + ")...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothAdapter a = adaptador();
                    if (a != null && a.isDiscovering()) {
                        a.cancelDiscovery();
                    }
                } catch (SecurityException e) {
                    // Sin permiso para cancelar el descubrimiento: se sigue igual.
                }
                String motivo1;
                String motivo2;
                BluetoothSocket s = null;
                try {
                    s = d.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                    s.connect();
                    motivo1 = null;
                } catch (IOException | SecurityException e) {
                    motivo1 = descripcion(e);
                    cerrarSocket(s);
                    s = null;
                }
                motivo2 = null;
                if (s == null) {
                    try {
                        s = d.createRfcommSocketToServiceRecord(UUID_SPP);
                        s.connect();
                    } catch (IOException | SecurityException e) {
                        motivo2 = descripcion(e);
                        cerrarSocket(s);
                        s = null;
                    }
                }
                if (s == null) {
                    conectando = false;
                    publicarEstado(false, "Fallo al conectar con " + n + " (" + m + ").\n"
                            + "Socket inseguro: " + motivo1 + "\n"
                            + "Socket seguro: " + motivo2);
                    return;
                }
                try {
                    OutputStream out = s.getOutputStream();
                    InputStream in = s.getInputStream();
                    nombre = n;
                    mac = m;
                    Registro.abrir(app, n, m);
                    Registro.nota("conectado con " + n + " " + m
                            + (motivo1 == null ? " (socket inseguro)" : " (socket seguro; inseguro fallo: " + motivo1 + ")"));
                    socket = s;
                    salida = out;
                    arrancarLector(s, in);
                    conectando = false;
                    publicarEstado(true, "Conectado con " + n + " (" + m + ")");
                } catch (IOException e) {
                    cerrarSocket(s);
                    conectando = false;
                    publicarEstado(false, "Conectó, pero no se pudieron abrir los flujos: "
                            + descripcion(e));
                }
            }
        }, "conexion-spp").start();
    }

    private void arrancarLector(final BluetoothSocket s, final InputStream in) {
        Thread lector = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[512];
                try {
                    while (true) {
                        int n = in.read(buffer);
                        if (n < 0) {
                            throw new IOException("fin de flujo");
                        }
                        if (n == 0) {
                            continue;
                        }
                        long t = Registro.ahora();
                        byte[] copia = new byte[n];
                        System.arraycopy(buffer, 0, copia, 0, n);
                        Registro.rx(t, copia);
                        for (OyenteRx o : oyentesRx) {
                            o.rx(t, copia);
                        }
                    }
                } catch (IOException e) {
                    // Si el socket ya no es el vigente, el cierre fue voluntario.
                    if (socket == s) {
                        Registro.nota("conexion perdida: " + descripcion(e));
                        cerrar();
                        publicarEstado(false, "Conexión perdida: " + descripcion(e));
                    }
                }
            }
        }, "lector-spp");
        lector.setDaemon(true);
        lector.start();
    }

    /**
     * Envia bytes. Bloquea hasta que write()+flush() retornan; llamar fuera
     * del hilo de interfaz.
     *
     * @return t_ms del registro tomado justo antes de escribir.
     */
    public long enviar(byte[] datos) throws IOException {
        synchronized (cerrojoEscritura) {
            OutputStream out = salida;
            if (out == null || !estaConectado()) {
                throw new IOException("no hay conexión");
            }
            long t = Registro.ahora();
            try {
                if (datos.length > 32) {
                    int offset = 0;
                    while (offset < datos.length) {
                        int bloque = Math.min(24, datos.length - offset);
                        out.write(datos, offset, bloque);
                        out.flush();
                        offset += bloque;
                        if (offset < datos.length) {
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } else {
                    out.write(datos);
                    out.flush();
                }
            } catch (IOException e) {
                Registro.nota("fallo al enviar " + Hex.hex(Hex.ocultarPin(datos)) + ": " + descripcion(e));
                cerrar();
                publicarEstado(false, "Conexión perdida al enviar: " + descripcion(e));
                throw e;
            }
            ultimoEnvio = android.os.SystemClock.elapsedRealtime();
            Registro.tx(t, datos);
            return t;
        }
    }

    public void desconectar() {
        if (socket != null) {
            Registro.nota("desconexion pedida por el operador");
        }
        cerrar();
        publicarEstado(false, "Sin conexión (desconectado por el operador)");
    }

    private void cerrar() {
        BluetoothSocket s = socket;
        socket = null;
        salida = null;
        cerrarSocket(s);
    }

    private static void cerrarSocket(BluetoothSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                // Cerrar un socket ya roto lanza; no cambia nada.
            }
        }
    }

    public static String nombreDe(BluetoothDevice d) {
        try {
            String n = d.getName();
            return (n == null || n.isEmpty()) ? "(sin nombre)" : n;
        } catch (SecurityException e) {
            return "(sin permiso para leer el nombre)";
        }
    }

    public static String descripcion(Exception e) {
        String m = e.getMessage();
        return e.getClass().getSimpleName() + ((m == null || m.isEmpty()) ? "" : ": " + m);
    }
}
