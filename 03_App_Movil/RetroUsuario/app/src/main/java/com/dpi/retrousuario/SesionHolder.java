package com.dpi.retrousuario;

import android.content.Context;
import android.content.SharedPreferences;

import com.dpi.retrousuario.dominio.Canal;
import com.dpi.retrousuario.dominio.DeteccionYSonda;
import com.dpi.retrousuario.dominio.EstadoDeteccion;
import com.dpi.retrousuario.dominio.EstadoMedida;
import com.dpi.retrousuario.dominio.Oyente;
import com.dpi.retrousuario.dominio.ParametrosRitmo;
import com.dpi.retrousuario.dominio.RespuestaV;
import com.dpi.retrousuario.dominio.SesionMedicion;
import com.dpi.retrousuario.dominio.SondaReintentable;
import com.dpi.retrousuario.dominio.Sonda362;

/**
 * Punto único de acceso a la sesión de medición en curso (RF-USR-04, RF-USR-05, RF-USR-06): {@link MainActivity} la
 * crea tras la sonda (RF-USR-01/02); {@link MedirActivity} y {@link AjustesActivity} la leen. Vive
 * mientras el proceso vive (un solo equipo conectado a la vez, sin hilos concurrentes que la
 * comparta); lo que sobrevive a la muerte del proceso es el diario en disco, no este objeto
 * (RF-USR-06, C5, T-USR-25).
 *
 * {@link #parametros()} es el **mismo** objeto para toda la app: cambiar `lecturasPorColor` en
 * {@link AjustesActivity} se ve de inmediato en la siguiente serie que mida {@link MedirActivity}
 * (T-USR-28), sin pasar por la pantalla de medir.
 *
 * <p>Condiciones arq B-1/C1 sobre 0.3.1: el enlace Bluetooth se registra aquí **desde que se abre el
 * socket** ({@link #registrarEnlaceAbierto}), no sólo cuando la sonda tiene éxito — así
 * {@link #enlace()} es la única fuente de verdad, sobrevive a un giro de pantalla y
 * {@code MainActivity} nunca duplica esa referencia en un campo propio (la duplicación era la causa
 * de la fuga de socket de C1: un campo de Activity se pierde al recrearla, así que la guarda de
 * "cambio de equipo" comparaba contra un {@code SesionHolder} que no sabía nada del enlace todavía
 * abierto). El estado de "Reintente" (canal y contador de la sonda, B-1) vive aquí por el mismo
 * motivo: un campo de Activity no sobrevive a un giro mientras se espera al operador.</p>
 */
final class SesionHolder {

    private static final String PREFS = "retrousuario_ajustes";
    private static final String CLAVE_LECTURAS_POR_COLOR = "lecturasPorColor";

    private static final ParametrosRitmo PARAMETROS = new ParametrosRitmo();
    private static SesionMedicion sesion;
    private static EnlaceBluetooth enlace;
    /** M-1 (giro): el aviso previo (T-USR-02) se acepta una sola vez por proceso, no por Activity. */
    private static boolean avisoAceptado = false;
    /** C2: el permiso de ubicación se pide una sola vez por proceso (mismo patrón que el aviso previo). */
    private static boolean permisoUbicacionPedido = false;
    /** C1/C2 (arq, sobre 0.3.3): "una detección en curso" y "qué resultado hay que pintar" ya no son
     *  un campo de {@code MainActivity} (se pierden al recrearla, giro de pantalla) ni una llamada a
     *  {@code runOnUiThread} de la Activity que lanzó el hilo (pinta sobre una instancia ya destruida
     *  — C2) — viven en {@link EstadoDeteccion}, dominio puro con pruebas JVM propias
     *  ({@code EstadoDeteccionTest}): cualquier Activity viva pregunta {@link #detectando()} y
     *  {@link #recogerResultadoDeteccionPendiente()} en su propio {@code onResume()}. */
    private static final EstadoDeteccion ESTADO_DETECCION = new EstadoDeteccion();

    /** FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625) (revisor Fable, opción A, sobre 0.3.5): "hay una medida en curso"/"qué
     *  pregunta hay que repintar" tampoco vivía aquí (vivía repartida en {@code MedirActivity}, ver su
     *  Javadoc) — mismo patrón que {@link #ESTADO_DETECCION}: dominio puro, con pruebas JVM propias
     *  ({@code EstadoMedidaTest}), UNA instancia para todo el proceso (una sola medida en vuelo a la
     *  vez, como ya garantizaba el cerrojo de {@code SesionMedicion#medir}). */
    private static final EstadoMedida ESTADO_MEDIDA = new EstadoMedida();

    /** B-1: canal (envuelve el mismo {@link #enlace}), reintentos hechos y `#V#` ya confirmado de la
     *  sonda EN CURSO — mientras se espera a que el operador pulse "Reintentar" o continúe. Se limpia
     *  al establecer una sesión (ya no hace falta) o al cambiar de equipo ({@link #limpiar()}). */
    private static Canal canalSondaEnCurso;
    private static SondaReintentable reintentosSondaEnCurso;
    private static RespuestaV respuestaVEnCurso;

    private SesionHolder() {
    }

    /** M-1: true si el operador ya aceptó el aviso previo en este proceso (no vuelve a salir tras un giro). */
    static boolean avisoAceptado() {
        return avisoAceptado;
    }

    static void marcarAvisoAceptado() {
        avisoAceptado = true;
    }

    /** C2: true si esta app ya pidió {@code ACCESS_FINE_LOCATION} en este proceso (no se repite en
     *  cada entrada a "Medir" ni en cada giro; ver {@code dominio.PermisoUbicacion}). */
    static boolean permisoUbicacionPedido() {
        return permisoUbicacionPedido;
    }

    static void marcarPermisoUbicacionPedido() {
        permisoUbicacionPedido = true;
    }

    /** M-1: true mientras hay una detección (conexión + "#V#" + primera sonda) en curso en un hilo
     *  de fondo, con o sin la Activity que la lanzó todavía viva. */
    static boolean detectando() {
        return ESTADO_DETECCION.detectando();
    }

    /** C1: {@code true} arranca la detección (nada que pintar todavía); {@code false} la da por
     *  terminada SIN resultado — el camino de "Atrás a mitad de Conectando"
     *  ({@code MainActivity.hiloConectarYDetectar}) y el `catch` de una excepción llaman a esto, no a
     *  {@link #publicarResultadoDeteccion}. */
    static void marcarDetectando(boolean valor) {
        if (valor) {
            ESTADO_DETECCION.iniciar();
        } else {
            ESTADO_DETECCION.salir();
        }
    }

    /** C2: publica el resultado de una detección que SÍ terminó de sondear, para que lo repinte la
     *  Activity viva en su {@code onResume()} — nunca la que lanzó el hilo directamente. También
     *  termina la detección ({@link EstadoDeteccion#publicar} pone {@code detectando = false}) y
     *  avisa al oyente registrado, si hay uno vivo en ese momento. */
    static void publicarResultadoDeteccion(Canal canal, DeteccionYSonda.Resultado resultado) {
        ESTADO_DETECCION.publicar(canal, resultado);
    }

    /** C2 sobre 0.3.4: publica un error de conexión/detección (antes se pintaba sobre {@code this}
     *  desde el hilo de fondo, sin publicar). */
    static void publicarErrorDeteccion(String mensaje) {
        ESTADO_DETECCION.publicarError(mensaje);
    }

    /** C2 sobre 0.3.4: mismo mecanismo para el resultado de {@code reintentarSonda} (anotado por el
     *  arquitecto, REVISIONES 0.3.4). */
    static void publicarSondaPendiente(Sonda362.ResultadoSonda sonda) {
        ESTADO_DETECCION.publicarSonda(sonda);
    }

    /** C2: recoge, UNA vez, el resultado publicado por {@link #publicarResultadoDeteccion} — o
     *  {@code null} si no hay ninguno pendiente (no hubo detección, ya se recogió, o terminó sin
     *  resultado con {@link #marcarDetectando}({@code false})). */
    static EstadoDeteccion.Resultado recogerResultadoDeteccionPendiente() {
        return ESTADO_DETECCION.recogerResultadoPendiente();
    }

    /** Igual que {@link #recogerResultadoDeteccionPendiente()} pero para el error publicado con
     *  {@link #publicarErrorDeteccion}. */
    static String recogerErrorDeteccionPendiente() {
        return ESTADO_DETECCION.recogerErrorPendiente();
    }

    /** Igual que {@link #recogerResultadoDeteccionPendiente()} pero para {@link #publicarSondaPendiente}. */
    static Sonda362.ResultadoSonda recogerSondaPendiente() {
        return ESTADO_DETECCION.recogerSondaPendiente();
    }

    /** C2: la Activity viva se registra aquí en su {@code onResume()} y se retira en su {@code
     *  onPause()} — ver el Javadoc de {@link Oyente}. */
    static void registrarOyenteDeteccion(Oyente oyente) {
        ESTADO_DETECCION.registrarOyente(oyente);
    }

    static void quitarOyenteDeteccion(Oyente oyente) {
        ESTADO_DETECCION.quitarOyente(oyente);
    }

    /** FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): la única instancia de {@link EstadoMedida} del proceso — {@code
     *  MedirActivity} llama a {@link EstadoMedida#ejecutar}/{@link EstadoMedida#responder} sobre ella,
     *  nunca crea la suya (una Activity recreada por un giro no es dueña de la medida, el dominio sí). */
    static EstadoMedida medida() {
        return ESTADO_MEDIDA;
    }

    static ParametrosRitmo parametros() {
        return PARAMETROS;
    }

    /**
     * A2 (ALTO) + C1: invalida la sesión, el enlace y el estado de sonda en curso anteriores (al
     * cambiar de equipo, o cuando el enlace anterior ya no está vivo — {@link
     * com.dpi.retrousuario.dominio.GestorEnlace}). No cierra el socket aquí (eso lo hace quien tenga
     * la referencia real, MainActivity, antes de llamar a esto): esto sólo quita la referencia
     * compartida, para que {@link MedirActivity}/{@link AjustesActivity} dejen de ver la sesión vieja
     * de inmediato, y para no dejar en {@link #canalSondaEnCurso} un canal que envuelve un enlace ya
     * cerrado (B-1). FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625) (sobre 0.3.5): antes esto cerraba el socket con una medida
     * todavía en vuelo (el hilo de medir se quedaba bloqueado para siempre en el enlace ya cerrado) —
     * {@link EstadoMedida#abandonar()} contesta SALTAR a cualquier pregunta pendiente para que {@code
     * sesion.medir()} termine y suelte su cerrojo antes de que esta sesión deje de ser la actual.
     */
    static void limpiar() {
        sesion = null;
        enlace = null;
        ESTADO_DETECCION.salir(); // C1: defensivo, no queda "Conectando" colgado sobre un enlace que ya no existe.
        ESTADO_MEDIDA.abandonar(); // FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): no deja una medida en vuelo bloqueada para siempre.
        limpiarSondaEnCurso();
    }

    /** C1: registra el enlace en cuanto se abre el socket, ANTES de saber si la sonda tendrá éxito —
     *  así es la única referencia (no hay un campo paralelo en MainActivity) y sobrevive a un giro de
     *  pantalla aunque la sonda todavía no haya terminado. */
    static void registrarEnlaceAbierto(EnlaceBluetooth e) {
        enlace = e;
    }

    /** B-1: guarda el estado de "Reintente" (canal, contador de intentos y el `#V#` ya confirmado)
     *  para que sobreviva a un giro mientras se espera al operador. */
    static void registrarSondaEnCurso(Canal canal, SondaReintentable reintentos, RespuestaV respuestaV) {
        canalSondaEnCurso = canal;
        reintentosSondaEnCurso = reintentos;
        respuestaVEnCurso = respuestaV;
    }

    /** B-1: ya hay sesión (o se canceló la conexión): el estado de "Reintente" deja de hacer falta. */
    static void limpiarSondaEnCurso() {
        canalSondaEnCurso = null;
        reintentosSondaEnCurso = null;
        respuestaVEnCurso = null;
    }

    static Canal canalSondaEnCurso() {
        return canalSondaEnCurso;
    }

    static SondaReintentable reintentosSondaEnCurso() {
        return reintentosSondaEnCurso;
    }

    static RespuestaV respuestaVEnCurso() {
        return respuestaVEnCurso;
    }

    static void establecer(SesionMedicion s, EnlaceBluetooth e) {
        sesion = s;
        enlace = e;
    }

    static SesionMedicion sesion() {
        return sesion;
    }

    static EnlaceBluetooth enlace() {
        return enlace;
    }

    /** B3: `lecturasPorColor` sobrevive a la muerte del proceso (SharedPreferences), no sólo a que
     *  la Activity se recree. Se llama una vez, al arrancar (MainActivity.onCreate). */
    static void cargarAjustesPersistidos(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int valor = prefs.getInt(CLAVE_LECTURAS_POR_COLOR, PARAMETROS.lecturasPorColor());
        if (valor >= 1) {
            PARAMETROS.lecturasPorColor(valor);
        }
    }

    /** B3: guarda `lecturasPorColor` (AjustesActivity, tras validar el valor). */
    static void guardarLecturasPorColor(Context contexto, int valor) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(CLAVE_LECTURAS_POR_COLOR, valor)
                .apply();
    }
}
