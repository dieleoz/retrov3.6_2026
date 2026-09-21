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
 * <p>Esta clase, dominio puro (sin Android, sin {@code SesionHolder} — la usa por composición), junta
 * las dos banderas para poder probarlas en la JVM sin Activity: {@link #iniciar()} arranca una
 * detección; CUALQUIER camino de salida del hilo de fondo (éxito, error, o Atrás a mitad) llama a
 * {@link #salir()} o {@link #publicar}, que SIEMPRE dejan {@link #detectando()} en {@code false} —
 * "todo camino de salida" es justo lo que C1 pedía; y el resultado publicado con {@link #publicar}
 * lo recoge, UNA SOLA VEZ, quien pregunte después con {@link #recogerResultadoPendiente()} — la
 * Activity que esté viva en ESE momento (su propio {@code onResume()}), no necesariamente la que iba
 * a pintarlo cuando terminó el hilo.</p>
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

    private boolean detectando = false;
    private Resultado resultadoPendiente;

    /** Arranca una detección: todavía no hay resultado que pintar. */
    public synchronized void iniciar() {
        detectando = true;
        resultadoPendiente = null;
    }

    /** C1: cualquier salida del hilo de detección que NO termina en un resultado que pintar (Atrás a
     *  mitad de "Conectando", o una excepción) también da la detección por terminada. */
    public synchronized void salir() {
        detectando = false;
    }

    /** C2: termina la detección Y publica el resultado para que lo recoja, UNA vez, la Activity que
     *  esté viva cuando pregunte — no la que lanzó el hilo. */
    public synchronized void publicar(Canal canal, DeteccionYSonda.Resultado deteccion) {
        resultadoPendiente = new Resultado(canal, deteccion);
        detectando = false;
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
}
