package com.dpi.retrousuario.dominio;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Doble de prueba de {@link PreguntaOperador} para RF-USR-04 r7 (REPETIR-PREGUNTA): en vez de un
 * operador real tocando un diálogo, cada pregunta consume la siguiente respuesta programada, en el
 * orden exacto en que {@link SerieDisparos} la pida. Si se agota la cola programada, la prueba falla
 * con un mensaje claro (no se cuelga, no inventa una respuesta) — así una prueba que programa de
 * menos avisa de que {@code SerieDisparos} preguntó más veces de las esperadas.
 */
final class PreguntaOperadorFalsa implements PreguntaOperador {

    private final Deque<Decision> respuestasCero;
    private final Deque<Decision> respuestasAnulado;
    private final Decision constante;
    int vecesPreguntadoCero = 0;
    int vecesPreguntadoAnulado = 0;

    private PreguntaOperadorFalsa(Deque<Decision> cero, Deque<Decision> anulado, Decision constante) {
        this.respuestasCero = cero;
        this.respuestasAnulado = anulado;
        this.constante = constante;
    }

    /** Contesta SIEMPRE lo mismo, a cualquiera de las dos preguntas (para las pruebas a las que no
     *  les importa esta rama, o que no esperan que se llegue a preguntar). */
    static PreguntaOperadorFalsa siempre(Decision d) {
        return new PreguntaOperadorFalsa(null, null, d);
    }

    /** Programa las respuestas de {@link #preguntarCero()}, en orden; {@link #preguntarDisparoAnulado()}
     *  no se espera en esta prueba (revienta si se llama). */
    static PreguntaOperadorFalsa cero(Decision... respuestas) {
        return new PreguntaOperadorFalsa(new ArrayDeque<>(Arrays.asList(respuestas)), new ArrayDeque<>(), null);
    }

    /** Programa las respuestas de {@link #preguntarDisparoAnulado()}, en orden; {@link #preguntarCero()}
     *  no se espera en esta prueba (revienta si se llama). */
    static PreguntaOperadorFalsa anulado(Decision... respuestas) {
        return new PreguntaOperadorFalsa(new ArrayDeque<>(), new ArrayDeque<>(Arrays.asList(respuestas)), null);
    }

    @Override
    public Decision preguntarCero() {
        vecesPreguntadoCero++;
        if (constante != null) {
            return constante;
        }
        if (respuestasCero.isEmpty()) {
            throw new IllegalStateException("preguntarCero() llamado sin respuesta programada");
        }
        return respuestasCero.poll();
    }

    @Override
    public Decision preguntarDisparoAnulado() {
        vecesPreguntadoAnulado++;
        if (constante != null) {
            return constante;
        }
        if (respuestasAnulado.isEmpty()) {
            throw new IllegalStateException("preguntarDisparoAnulado() llamado sin respuesta programada");
        }
        return respuestasAnulado.poll();
    }
}
