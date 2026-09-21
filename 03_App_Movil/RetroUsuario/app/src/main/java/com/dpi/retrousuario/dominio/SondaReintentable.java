package com.dpi.retrousuario.dominio;

/**
 * RF-USR-01, T-USR-01b(d)(e): el reintento de la sonda (#GN#/#GC#) tras "sin respuesta útil" lo
 * dispara **el operador** (nunca la app sola, "no reintenta sola en silencio"), hasta 2 veces —
 * {@code params.maxReintentosSonda()} — contando la sonda original ("3 intentos en total, la sonda
 * original más 2 reintentos", SPEC-App-Usuario-V3.6.md r6 RF-USR-01). Cada intento (el original y
 * cada reintento) es una llamada aparte a {@link #intentar(Canal)}: nunca reenvía "#V#" (eso ya lo
 * confirmó la detección, antes de construir esta clase), sólo repite #GN#/#GC# sobre el MISMO canal
 * ya conectado.
 *
 * <p>Condición QA-1 sobre 32c785d: antes de esta clase, el botón "Reintentar" de MainActivity volvía
 * a llamar a {@code conectarYDetectar} entero (reenviando "#V#") y no llevaba cuenta de cuántas veces
 * se había reintentado ya: se podía reintentar indefinidamente, y nunca se distinguía el mensaje de
 * "reintente" del de "sin datos de calibración/serie" tras agotar los reintentos.</p>
 */
public final class SondaReintentable {

    private final ParametrosRitmo params;
    private int intentosHechos = 0;

    public SondaReintentable(ParametrosRitmo params) {
        this(params, 0);
    }

    /**
     * @param intentosYaHechos cuántos intentos ya se hicieron FUERA de esta instancia (p. ej. la
     *                         sonda original, hecha junto con la detección de "#V#" en
     *                         {@link DeteccionYSonda}, antes de construir esta clase para ofrecer
     *                         los reintentos siguientes).
     */
    public SondaReintentable(ParametrosRitmo params, int intentosYaHechos) {
        this.params = params;
        this.intentosHechos = intentosYaHechos;
    }

    /** Intentos ya hechos (1 tras la sonda original, 2 tras el primer reintento, ...). */
    public int intentosHechos() {
        return intentosHechos;
    }

    /**
     * Un intento: la sonda original la primera vez que se llama, un reintento operador-driven las
     * siguientes. Nunca se llama sola: quien orquesta (la Activity) sólo la invoca de nuevo tras una
     * acción explícita del operador.
     */
    public Sonda362.ResultadoSonda intentar(Canal canal) {
        Sonda362.ResultadoSonda r = Sonda362.sondear(canal, params);
        intentosHechos++;
        return r;
    }

    /**
     * true si, tras un {@link Sonda362.Resultado#SIN_RESPUESTA_REINTENTAR}, todavía se puede ofrecer
     * otro intento sin pasar de {@code maxReintentosSonda()} reintentos (T-USR-01b: "no se ofrece un
     * tercer reintento" tras agotarlos).
     */
    public boolean puedeReintentar() {
        return intentosHechos <= params.maxReintentosSonda();
    }
}
