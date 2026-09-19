package com.dpi.retrov36;

import android.os.SystemClock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Peticion-respuesta sobre EnlaceSerie, con UNA sola peticion en vuelo.
 *
 * - Todas las operaciones con el equipo corren en un unico hilo de trabajo
 *   (ejecutar()); nunca en el de interfaz.
 * - Antes de cada peticion se respeta la pausa minima del equipo (1500 ms
 *   desde el envio anterior y 600 ms desde el ultimo byte recibido) y se
 *   vacia la entrada.
 * - Cada peticion acaba en uno de tres desenlaces: VALIDA, TIMEOUT o
 *   INESPERADA. La trama literal va siempre al registro.
 * - Ninguna peticion con '@' salvo la sonda de V4 (Tramas.peticionPermitida).
 */
public final class Cliente implements EnlaceSerie.OyenteRx, Canal {

    public static final long PAUSA_ENTRE_ENVIOS_MS = 1500;
    public static final long PAUSA_TRAS_RX_MS = 600;
    /** Limite de respuesta de una medida (prueba 3: menos de 2,5 s). */
    public static final long TIMEOUT_MEDIDA_MS = 2500;
    public static final long TIMEOUT_ADMIN_MS = 2000;
    /**
     * Entre dos tramas '#' seguidas con un V3.6 ya detectado basta una pausa
     * corta: segun el contrato (§3 y O-05) una trama '#' nunca dispara medida.
     */
    public static final long PAUSA_ENTRE_ALMOHADILLAS_MS = 150;

    private static final Charset LATIN1 = Charset.forName("ISO-8859-1");
    private static final Cliente INSTANCIA = new Cliente();

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final Receptor receptor = new Receptor();
    private final Object aviso = new Object();
    private volatile long ultimoRx;
    private volatile boolean ultimaFueAlmohadilla;
    /** Timeouts seguidos (sin un solo byte recibido). Se pone a 0 al recibir algo. */
    private volatile int timeoutsSeguidos;
    private volatile boolean consejoAnotado;
    /** A partir de cuantos timeouts seguidos se aconseja apagar y encender. */
    public static final int TIMEOUTS_PARA_CONSEJO = 3;
    public static final String CONSEJO_MUDO = "El equipo no responde a nada ("
            + TIMEOUTS_PARA_CONSEJO + " o más peticiones seguidas sin respuesta). Apáguelo y enciéndalo, "
            + "espere unos segundos y vuelva a conectar.";

    private Cliente() {
        EnlaceSerie.instancia().agregarOyenteRx(this);
    }

    public static Cliente instancia() {
        return INSTANCIA;
    }

    public void ejecutar(Runnable r) {
        hilo.execute(r);
    }

    @Override
    public void rx(long tMs, byte[] datos) {
        long ahora = SystemClock.elapsedRealtime();
        receptor.agregar(ahora, datos);
        ultimoRx = ahora;
        synchronized (aviso) {
            aviso.notifyAll();
        }
    }

    public static final class Respuesta {
        public final String peticion;
        public final Receptor.Desenlace desenlace;
        /** Trama reconocida (null si no VALIDA). */
        public final String trama;
        /** Todo lo recibido, tal cual. */
        public final String bruto;
        /** Del envio al ultimo byte; -1 si no llego nada. */
        public final long ms;

        Respuesta(String peticion, Receptor.Desenlace desenlace, String trama, String bruto, long ms) {
            this.peticion = peticion;
            this.desenlace = desenlace;
            this.trama = trama;
            this.bruto = bruto;
            this.ms = ms;
        }

        public boolean valida() {
            return desenlace == Receptor.Desenlace.VALIDA;
        }

        public String describir() {
            switch (desenlace) {
                case VALIDA:
                    return trama + " en " + ms + " ms";
                case TIMEOUT:
                    return "sin respuesta (timeout)";
                default:
                    // ISO-8859-1 sin pasar por Cliente: Respuesta se usa tambien en la JVM (3.6.13).
                    return "respuesta inesperada: \"" + Hex.ascii(bruto.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
                            + "\" [" + Hex.hex(bruto.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) + "]";
            }
        }
    }

    /**
     * Envia una peticion y espera su respuesta. Llamar SOLO desde ejecutar().
     *
     * @throws IOException si no hay enlace o se pierde durante la espera.
     */
    @Override
    public Respuesta pedir(String peticion, Tramas.Tipo tipo, long timeoutMs)
            throws IOException, InterruptedException {
        if (!Tramas.peticionPermitida(peticion, Sesion.get().version == Sesion.Version.V36)) {
            throw new IllegalArgumentException("peticion no permitida: " + peticion
                    + " ('@' fuera de la sonda V4, o 'e' a un equipo que no es V3.6)");
        }
        EnlaceSerie enlace = EnlaceSerie.instancia();
        boolean almohadilla = peticion.startsWith("#");
        boolean corta = almohadilla && ultimaFueAlmohadilla
                && Sesion.get().version == Sesion.Version.V36;
        esperarPausa(enlace, corta);
        ultimaFueAlmohadilla = almohadilla;
        receptor.vaciar();
        long t0 = SystemClock.elapsedRealtime();
        enlace.enviar(peticion.getBytes(LATIN1));
        // Se espera el timeout mas el silencio que cierra "::n".
        long limite = t0 + timeoutMs + Receptor.SILENCIO_MS;
        String trama = null;
        while (true) {
            long ahora = SystemClock.elapsedRealtime();
            trama = receptor.completa(tipo, ahora);
            if (trama != null || ahora >= limite) {
                break;
            }
            if (!enlace.estaConectado()) {
                throw new IOException("enlace perdido esperando la respuesta a " + peticion);
            }
            synchronized (aviso) {
                aviso.wait(Math.min(40, Math.max(1, limite - ahora)));
            }
        }
        long ult = receptor.ultimoByteMs();
        long ms = ult < 0 ? -1 : ult - t0;
        String bruto = receptor.texto();
        Receptor.Desenlace d;
        if (trama != null && ms <= timeoutMs) {
            d = Receptor.Desenlace.VALIDA;
        } else if (trama != null) {
            // Completa, pero fuera de plazo: se trata como timeout y se dice.
            d = Receptor.Desenlace.TIMEOUT;
            trama = null;
        } else {
            d = receptor.desenlace(tipo, SystemClock.elapsedRealtime());
            if (d == Receptor.Desenlace.VALIDA) {
                d = Receptor.Desenlace.INESPERADA; // no deberia ocurrir
            }
        }
        Respuesta r = new Respuesta(peticion, d, trama, bruto, ms);
        if (almohadilla && d == Receptor.Desenlace.VALIDA) {
            Sesion.get().ultimaAlmohadillaMs = SystemClock.elapsedRealtime();
        }
        Registro.nota("peticion " + peticion + " -> " + r.describir()
                + (receptor.desbordado() ? " (entrada desbordada, recortada)" : ""));
        if (d == Receptor.Desenlace.TIMEOUT && bruto.isEmpty()) {
            timeoutsSeguidos++;
            if (timeoutsSeguidos >= TIMEOUTS_PARA_CONSEJO && !consejoAnotado) {
                consejoAnotado = true;
                Registro.nota("AVISO: " + timeoutsSeguidos + " timeouts seguidos. " + CONSEJO_MUDO);
            }
        } else {
            timeoutsSeguidos = 0;
            consejoAnotado = false;
        }
        return r;
    }

    public int timeoutsSeguidos() {
        return timeoutsSeguidos;
    }

    /** "" si el equipo responde; el consejo de apagar y encender si lleva varios timeouts seguidos. */
    public String consejoSiMudo() {
        return timeoutsSeguidos >= TIMEOUTS_PARA_CONSEJO ? "\n" + CONSEJO_MUDO : "";
    }

    /** Al conectar de nuevo, la cuenta empieza de cero. */
    public void reiniciarCuenta() {
        timeoutsSeguidos = 0;
        consejoAnotado = false;
    }

    private void esperarPausa(EnlaceSerie enlace, boolean corta) throws InterruptedException, IOException {
        long pausaEnvio = corta ? PAUSA_ENTRE_ALMOHADILLAS_MS : PAUSA_ENTRE_ENVIOS_MS;
        long pausaRx = corta ? 0 : PAUSA_TRAS_RX_MS;
        while (true) {
            if (!enlace.estaConectado()) {
                throw new IOException("no hay conexión");
            }
            long desdeEnvio = enlace.msDesdeUltimoEnvio();
            long u = ultimoRx;
            long desdeRx = u == 0 ? Long.MAX_VALUE : SystemClock.elapsedRealtime() - u;
            long falta = Math.max(pausaEnvio - desdeEnvio, pausaRx - desdeRx);
            if (falta <= 0) {
                return;
            }
            Thread.sleep(Math.min(falta, 100)); // hilo de trabajo, nunca el de interfaz
        }
    }
}
