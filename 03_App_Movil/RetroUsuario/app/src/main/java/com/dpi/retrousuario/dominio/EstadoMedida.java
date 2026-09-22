package com.dpi.retrousuario.dominio;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * FABLE-USR, revisor Fable opción A sobre RetroUsuario 0.3.5 (SPEC-App-Usuario-V3.6.md:617-625,
 * "Operaciones que sobreviven a la pantalla"): la operación "medir" vivía
 * repartida en {@code MedirActivity} (hilo lanzado en {@code :174-193}, cola local
 * {@code ArrayBlockingQueue} alimentada por un diálogo sobre {@code this} en {@code :73-87}, y
 * {@code runOnUiThread} sobre {@code this} en {@code :182}/{@code :188}) — el mismo defecto de diseño
 * que C1/C2 de {@link EstadoDeteccion} sobre la 0.3.3/0.3.4, pero sin corregir todavía: un giro de
 * pantalla a mitad de la pregunta "Repetir o Saltar" pierde el diálogo del sistema y deja el HILO DE
 * FONDO que mide bloqueado para siempre en el {@code ArrayBlockingQueue.take()} de la Activity vieja
 * (documentado, sin arnés, en el Javadoc de la 0.3.5); y la Activity nueva nace con los botones
 * habilitados ({@code onResume :134-138} no llamaba a {@code establecerControlesEnCurso(true)}),
 * porque nada en {@code SesionHolder} sabía que había una medida en curso.
 *
 * <p>Esta clase, dominio puro (sin Android), es la dueña de la operación en su lugar — mismo patrón
 * que {@link EstadoDeteccion}: {@link #fase()} distingue {@link Fase#LIBRE}, {@link Fase#MIDIENDO} y
 * {@link Fase#PREGUNTANDO}; implementa {@link PreguntaOperador} para que {@link SerieDisparos} la use
 * sin saber que hay una Activity detrás: {@link #preguntarCero()}/{@link #preguntarDisparoAnulado()}
 * dejan la pregunta pendiente (con su {@link TipoPregunta}), avisan al {@link Oyente} registrado y
 * bloquean en su propia {@link ArrayBlockingQueue} de capacidad 1 hasta que {@link #responder} la
 * alimente; {@link #responder} viene de CUALQUIER Activity viva (no necesariamente la que lanzó el
 * hilo de medir) y, sin pregunta pendiente, se descarta — no queda guardada para contestar la
 * siguiente pregunta que llegue después (evita que un doble toque, o una respuesta tardía tras un
 * giro, conteste algo que el operador no vio). {@link #ejecutar} llama a
 * {@code sesion.medir(..., this)} y publica el resultado (una fila, {@code null} si el operador saltó
 * tras un disparo anulado, o el mensaje de una excepción) como pendiente de un solo uso — la fase baja
 * a {@link Fase#LIBRE} ANTES de avisar, mismo orden que {@link EstadoDeteccion#publicar}. {@link
 * #abandonar()} (llamado desde {@code SesionHolder.limpiar()}, antes {@code MainActivity.java:219-221}
 * cerraba el socket con una medida en vuelo) contesta {@link Decision#ABANDONAR} a la pregunta
 * pendiente y a las que vengan después EN ESA MISMA MEDIDA, para que {@code sesion.medir()} termine de
 * verdad y suelte el monitor de {@code SesionMedicion.java:100} (sin eso, un {@code synchronized}
 * colgado bloquearía también la medida siguiente, aunque fuera sobre un equipo nuevo). El oyente se
 * registra en {@code onResume()} y se retira en {@code onPause()}, igual que {@link EstadoDeteccion}.</p>
 *
 * <p><b>arq C1 (REVISIONES-Apps-V3.6.md, entrada 0.3.6).</b> Hasta esta corrección {@link
 * #abandonar()} contestaba {@link Decision#SALTAR}, la misma decisión que un "Saltar" real del
 * operador — {@link SerieDisparos#medir()} no podía distinguir uno de otro, así que abandonar con la
 * pregunta CERO abierta guardaba una fila con {@code media = 0} (`SerieDisparos.java:98-99` de esa
 * entrega), contra la SPEC (§4 bis: "serie anulada, sin fila"). {@link Decision#ABANDONAR} es una
 * tercera decisión que sólo emite esta clase (ningún diálogo de operador la ofrece): {@code
 * SerieDisparos} la trata como "sin fila" ante un cero, igual que ya trataba cualquier decisión
 * distinta de REPETIR ante un disparo anulado.</p>
 */
public final class EstadoMedida implements PreguntaOperador {

    public enum Fase {
        LIBRE, MIDIENDO, PREGUNTANDO
    }

    public enum TipoPregunta {
        CERO, ANULADO
    }

    /** Resultado de una medida terminada (C2, mismo patrón que {@link EstadoDeteccion.Resultado}):
     *  {@link #fila} puede ser {@code null} de verdad (el operador saltó tras un disparo anulado, la
     *  serie se anuló entera) — por eso el "no hay nada pendiente" lo distingue la propia referencia a
     *  este objeto, no el valor de {@link #fila}. */
    public static final class Resultado {
        private final FilaMedida fila;

        private Resultado(FilaMedida fila) {
            this.fila = fila;
        }

        public FilaMedida fila() {
            return fila;
        }
    }

    private Fase fase = Fase.LIBRE;
    private TipoPregunta tipoPreguntaPendiente;
    private ArrayBlockingQueue<Decision> colaRespuesta;
    private boolean abandonado = false;
    private Resultado resultadoPendiente;
    private String errorPendiente;
    private Oyente oyente;

    public synchronized Fase fase() {
        return fase;
    }

    /** {@code null} si no hay pregunta pendiente (fase distinta de {@link Fase#PREGUNTANDO}); si la
     *  hay, cuál de las dos es, para que la Activity re-muestre el diálogo correcto tras un giro. */
    public synchronized TipoPregunta preguntaPendiente() {
        return fase == Fase.PREGUNTANDO ? tipoPreguntaPendiente : null;
    }

    /** {@link SerieDisparos} llama a esto (vía {@link PreguntaOperador}) desde el hilo de fondo de
     *  medir, nunca desde la Activity. */
    @Override
    public Decision preguntarCero() {
        return preguntar(TipoPregunta.CERO);
    }

    @Override
    public Decision preguntarDisparoAnulado() {
        return preguntar(TipoPregunta.ANULADO);
    }

    private Decision preguntar(TipoPregunta tipo) {
        ArrayBlockingQueue<Decision> q;
        synchronized (this) {
            if (abandonado) {
                return Decision.ABANDONAR; // abandonar() ya se llamó para esta medida: nunca vuelve a bloquear
                                            // (arq C1, 0.3.6: no es un "Saltar" del operador, SerieDisparos lo distingue).
            }
            q = new ArrayBlockingQueue<>(1);
            colaRespuesta = q;
            tipoPreguntaPendiente = tipo;
            fase = Fase.PREGUNTANDO;
        }
        avisar();
        Decision d;
        try {
            d = q.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            d = Decision.SALTAR; // defensivo: nunca deja el hilo de fondo colgado si lo interrumpen.
        }
        synchronized (this) {
            if (colaRespuesta == q) {
                colaRespuesta = null;
                tipoPreguntaPendiente = null;
            }
            fase = Fase.MIDIENDO;
        }
        return d;
    }

    /** Contesta la pregunta pendiente, desde CUALQUIER Activity viva (no necesariamente la que lanzó
     *  el hilo de medir: un giro de pantalla reemplaza la Activity, no la pregunta). Sin pregunta
     *  pendiente en el momento de la llamada, se descarta: no queda guardada para la siguiente
     *  pregunta que {@link SerieDisparos} haga después. */
    public void responder(Decision d) {
        ArrayBlockingQueue<Decision> q;
        synchronized (this) {
            if (fase != Fase.PREGUNTANDO || colaRespuesta == null) {
                return;
            }
            q = colaRespuesta;
        }
        q.offer(d);
    }

    /** arq Bajo (REVISIONES-Apps-V3.6.md, entrada 0.3.6): pone la fase en {@link Fase#MIDIENDO} de
     *  inmediato, desde el hilo de la PANTALLA (nunca el de fondo) — mismo patrón que {@link
     *  EstadoDeteccion#iniciar()}. Se llama ANTES de lanzar el hilo "medir-" + color, para que no quede
     *  una ventana en {@link Fase#LIBRE} entre la pulsación del botón y que ese hilo llegue a {@link
     *  #ejecutar} (antes, esa ventana existía: {@code MedirActivity.medir()} deshabilitaba los
     *  controles y arrancaba el hilo, pero {@link #fase()} sólo pasaba a MIDIENDO cuando el hilo nuevo
     *  ya estaba corriendo y llamaba a {@link #ejecutar}). {@link #ejecutar} repite el mismo
     *  {@code synchronized} por si se llama sin pasar antes por aquí (pruebas JVM directas, sin
     *  Activity): idempotente. */
    public synchronized void marcarMidiendo() {
        fase = Fase.MIDIENDO;
        abandonado = false;
        resultadoPendiente = null;
        errorPendiente = null;
    }

    /** Mide una serie (llamado desde el hilo "medir-" + color, nunca desde la Activity) y publica el
     *  resultado como pendiente de un solo uso: {@link #recogerResultadoPendiente()} (fila, o
     *  {@code null} si la serie se anuló) o {@link #recogerErrorPendiente()} si {@code sesion.medir()}
     *  lanzó una excepción (B-1 de la 0.3.0, mismo arnés que tenía {@code MedirActivity} antes de esta
     *  entrega). Reinicia {@link #abandonado} al empezar: {@link #abandonar()} sólo afecta a la medida
     *  que estaba en vuelo cuando se llamó, no a la siguiente. */
    public void ejecutar(SesionMedicion sesion, String colorFondo, String fechaHoraIso, String latitud,
            String longitud, String gpsEstado) {
        marcarMidiendo();
        try {
            FilaMedida fila = sesion.medir(colorFondo, fechaHoraIso, latitud, longitud, gpsEstado, this);
            synchronized (this) {
                fase = Fase.LIBRE;
                resultadoPendiente = new Resultado(fila);
            }
        } catch (Exception e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            synchronized (this) {
                fase = Fase.LIBRE;
                errorPendiente = mensaje;
            }
        }
        avisar();
    }

    /** {@code null} si no hay resultado pendiente; si lo hay, se entrega UNA sola vez (mismo patrón
     *  que {@link EstadoDeteccion#recogerResultadoPendiente()}). */
    public synchronized Resultado recogerResultadoPendiente() {
        Resultado r = resultadoPendiente;
        resultadoPendiente = null;
        return r;
    }

    public synchronized String recogerErrorPendiente() {
        String e = errorPendiente;
        errorPendiente = null;
        return e;
    }

    /** {@code SesionHolder.limpiar()} llama a esto (cambio de equipo, o salir de verdad): contesta
     *  SALTAR a la pregunta pendiente, si la hay, para que {@code sesion.medir()} deje de bloquear en
     *  su {@code synchronized} — y marca {@link #abandonado} para que cualquier pregunta SIGUIENTE de
     *  ESA MISMA medida (un "Saltar" tras un cero no impide que la serie vuelva a preguntar en el
     *  siguiente intento) también se conteste sola, sin esperar a un operador que ya no va a llegar
     *  (la Activity puede estar cerrándose). {@link #ejecutar} borra esta bandera al empezar la
     *  siguiente medida: abandonar una no condena a las que vengan después sobre un equipo nuevo. */
    public void abandonar() {
        ArrayBlockingQueue<Decision> q;
        synchronized (this) {
            abandonado = true;
            q = colaRespuesta;
        }
        if (q != null) {
            q.offer(Decision.ABANDONAR); // arq C1, 0.3.6: no un "Saltar" del operador (ver preguntar()).
        }
    }

    /** La Activity viva se registra en su {@code onResume()} y se retira en su {@code onPause()} —
     *  mismo patrón que {@link EstadoDeteccion#registrarOyente}. */
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
