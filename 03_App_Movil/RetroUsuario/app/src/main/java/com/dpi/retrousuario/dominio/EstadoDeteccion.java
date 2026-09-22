package com.dpi.retrousuario.dominio;

/**
 * Condiciones C1/C2 (arquitecto-iot sobre RetroUsuario 0.3.3, MainActivity.java:192-194 y :94-97/211
 * de esa entrega): la máquina de "hay una detección en curso" y "qué resultado hay que pintar"
 * vivía repartida entre un booleano de {@code SesionHolder} que un camino de salida (Atrás durante
 * "Conectando") olvidaba poner a {@code false} (C1: la app quedaba en "Conectando" para siempre) y
 * una llamada a {@code runOnUiThread} de LA ACTIVITY QUE LANZÓ el hilo, que pinta sobre una instancia
 * ya destruida si hubo un giro de pantalla mientras la conexión seguía en curso (C2: el resultado se
 * pierde, la Activity nueva no se entera).
 *
 * <p><b>C2 sobre 0.3.4 (REVISIONES-Apps-V3.6.md, entrada 0.3.4):</b> publicar aquí NO bastaba —
 * {@code MainActivity.hiloConectarYDetectar} seguía llamando, además, a
 * {@code runOnUiThread(this::restaurarInterfaz)}, que corre sobre la Activity vieja tras un giro
 * (consumiendo el resultado de un solo uso sin que la Activity nueva lo vea) y, si el hilo principal
 * lo ejecutaba antes que el {@code finally} de más abajo, pintaba "Conectando" sin recoger nada. El
 * camino de error tampoco publicaba: pintaba sobre {@code this} directamente. Ahora {@link #publicar}
 * y {@link #publicarError} dejan {@link #detectando()} en {@code false} ANTES de avisar a nadie
 * (dentro del mismo bloque {@code synchronized}), y el aviso va por un {@link Oyente} —registrado en
 * {@code onResume()} y retirado en {@code onPause()}, nunca capturado en la pila del hilo de fondo—
 * en vez de un {@code runOnUiThread} sobre una Activity que puede estar muerta. Sin oyente vivo (caso
 * normal si el hilo termina entre {@code onPause} y el siguiente {@code onResume}), el resultado no
 * se pierde: sigue pendiente y la próxima Activity que pregunte en su propio {@code onResume()} lo
 * recoge igual — el oyente es sólo para repintar SIN esperar a un {@code onResume} nuevo.</p>
 *
 * <p>Esta clase, dominio puro (sin Android, sin {@code SesionHolder} — la usa por composición), junta
 * las banderas para poder probarlas en la JVM sin Activity: {@link #iniciar()} arranca una
 * detección; CUALQUIER camino de salida del hilo de fondo (éxito, error, o Atrás a mitad) llama a
 * {@link #salir()}, {@link #publicar} o {@link #publicarError}, que SIEMPRE dejan {@link #detectando()}
 * en {@code false} — "todo camino de salida" es justo lo que C1 pedía; y el resultado publicado lo
 * recoge, UNA SOLA VEZ, quien pregunte después con {@link #recogerResultadoPendiente()} /
 * {@link #recogerErrorPendiente()} / {@link #recogerSondaPendiente()} — la Activity que esté viva en
 * ESE momento, no necesariamente la que iba a pintarlo cuando terminó el hilo.</p>
 */
public final class EstadoDeteccion {

    /** Lo que hace falta para repintar el resultado de una detección (C2): el canal ya conectado (y
     *  registrado, con el log de tramas) y lo que contestó. Ninguno de los dos es de Android. */
    public static final class Resultado {
        private final Canal canal;
        private final DeteccionYSonda.Resultado deteccion;

        public Resultado(Canal canal, DeteccionYSonda.Resultado deteccion) {
            this.canal = canal;
            this.deteccion = deteccion;
        }

        public Canal canal() {
            return canal;
        }

        public DeteccionYSonda.Resultado deteccion() {
            return deteccion;
        }
    }

    /** C2 sobre 0.3.4: a quién avisar cuando hay algo nuevo que pintar — sin Android (ni Handler ni
     *  Activity), para que esta clase se siga probando en la JVM. Quien implemente esto (MainActivity)
     *  es responsable de saltar al hilo principal (con {@code Handler(Looper.getMainLooper())}) antes
     *  de tocar ninguna vista: {@link #avisar()} llama a esto en el mismo hilo que publicó. */
    public interface Oyente {
        void avisar();
    }

    private boolean detectando = false;
    private Resultado resultadoPendiente;
    private String errorPendiente;
    private Sonda362.ResultadoSonda sondaPendiente;
    private Oyente oyente;

    /** Arranca una detección: todavía no hay resultado que pintar. */
    public synchronized void iniciar() {
        detectando = true;
        resultadoPendiente = null;
        errorPendiente = null;
    }

    /** C1: cualquier salida del hilo de detección que NO termina en un resultado que pintar (Atrás a
     *  mitad de "Conectando") también da la detección por terminada — sin publicar nada, así que no
     *  avisa al oyente (no hay nada nuevo que repintar). */
    public synchronized void salir() {
        detectando = false;
    }

    /** C2: termina la detección Y publica el resultado para que lo recoja, UNA vez, la Activity que
     *  esté viva cuando pregunte — no la que lanzó el hilo. {@code detectando} baja ANTES de avisar
     *  (mismo bloque {@code synchronized}); el aviso al oyente vivo va DESPUÉS, fuera del bloque. */
    public void publicar(Canal canal, DeteccionYSonda.Resultado deteccion) {
        synchronized (this) {
            resultadoPendiente = new Resultado(canal, deteccion);
            detectando = false;
        }
        avisar();
    }

    /** C2 sobre 0.3.4: el camino de error (antes pintaba "No se pudo conectar: " + mensaje sobre
     *  {@code this} desde el hilo de fondo, sin publicar nada) también pasa por aquí. */
    public void publicarError(String mensaje) {
        synchronized (this) {
            errorPendiente = mensaje;
            detectando = false;
        }
        avisar();
    }

    /** C2 sobre 0.3.4, mismo arreglo para {@code reintentarSonda} (anotado por el arquitecto,
     *  REVISIONES 0.3.4): el resultado de un reintento de sonda también se publica aquí en vez de
     *  pintarse con {@code runOnUiThread} sobre {@code this}. */
    public void publicarSonda(Sonda362.ResultadoSonda sonda) {
        synchronized (this) {
            sondaPendiente = sonda;
            detectando = false;
        }
        avisar();
    }

    public synchronized boolean detectando() {
        return detectando;
    }

    /** C2: se entrega UNA sola vez — una segunda Activity que pregunte después de que la primera ya
     *  lo recogió no lo repinta encima (ni ve un resultado viejo de una detección anterior). */
    public synchronized Resultado recogerResultadoPendiente() {
        Resultado r = resultadoPendiente;
        resultadoPendiente = null;
        return r;
    }

    /** Igual que {@link #recogerResultadoPendiente()} pero para el camino de error. */
    public synchronized String recogerErrorPendiente() {
        String e = errorPendiente;
        errorPendiente = null;
        return e;
    }

    /** Igual que {@link #recogerResultadoPendiente()} pero para el resultado de {@code reintentarSonda}. */
    public synchronized Sonda362.ResultadoSonda recogerSondaPendiente() {
        Sonda362.ResultadoSonda s = sondaPendiente;
        sondaPendiente = null;
        return s;
    }

    /** C2: la Activity viva se registra en su {@code onResume()} y se retira en su {@code onPause()}
     *  — nunca queda más de un oyente (una Activity nueva reemplaza al de la vieja). */
    public synchronized void registrarOyente(Oyente o) {
        oyente = o;
    }

    public synchronized void quitarOyente(Oyente o) {
        if (oyente == o) {
            oyente = null;
        }
    }

    private void avisar() {
        Oyente o;
        synchronized (this) {
            o = oyente;
        }
        if (o != null) {
            o.avisar();
        }
    }
}
