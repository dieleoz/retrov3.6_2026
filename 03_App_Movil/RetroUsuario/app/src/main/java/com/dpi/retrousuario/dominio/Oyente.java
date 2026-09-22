package com.dpi.retrousuario.dominio;

/**
 * A quién avisar cuando hay algo nuevo que pintar (extraído de {@code EstadoDeteccion.Oyente} sobre
 * 0.3.5, FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): {@link EstadoMedida} necesita el mismo mecanismo y no tiene sentido
 * duplicar la interfaz) — sin Android (ni {@code Handler} ni Activity), para que
 * {@link EstadoDeteccion}/{@link EstadoMedida} sigan probándose en la JVM. Quien implemente esto (la
 * Activity) es responsable de saltar al hilo principal (con {@code Handler(Looper.getMainLooper())})
 * antes de tocar ninguna vista: {@code avisar()} se llama en el mismo hilo que publicó.
 */
public interface Oyente {
    void avisar();
}
