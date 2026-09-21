package com.dpi.retrousuario.dominio;

/**
 * Parámetros configurables de RF-USR-04/RF-USR-16, con sus valores por defecto (todos "a fijar en
 * T-B06", provisionales, `SPEC-V3.6.md:441-457`). Mutable a propósito: la pantalla de Ajustes
 * (RF-USR-04, pantalla 5) cambia {@code lecturasPorColor} en caliente, sin volver a medir.
 */
public final class ParametrosRitmo {

    /** Silencio de fin de `::<n>` sin terminador: 180 ms (`SPEC-V3.6.md:453-457`, RF-APP-02). */
    private long silencioMs = 180;

    /** Plazo máximo de espera por disparo `::<n>`: 2500 ms (`SPEC-V3.6.md:453-456`, RF-APP-02). */
    private long plazoDisparoMs = 2500;

    /** Plazo de una trama `#...#` (cierra en el `#`, no por silencio): 2000 ms (`SPEC-V3.6.md:455`). */
    private long plazoTramaMs = 2000;

    /** Pausa mínima desde el envío anterior: 1500 ms (`SPEC-V3.6.md:441-442`, RF-APP-01). */
    private long pausaEnvioMs = 1500;

    /** Pausa mínima desde el último byte recibido: 600 ms (`SPEC-V3.6.md:441-442`, RF-APP-01). */
    private long pausaSilencioMs = 600;

    /** Cuarentena tras un disparo anulado (plazo o doble `::`): `Q >=` plazo, 2500 ms por defecto (RF-USR-16). */
    private long cuarentenaMs = 2500;

    /** RF-USR-04: 3 disparos por color por defecto (DECISIONES nota 11a, "leído 3 veces promedio"). */
    private int lecturasPorColor = 3;

    /** RF-USR-04: un disparo anulado se repite hasta 2 veces antes de anular la serie entera. */
    private int maxRepeticionesDisparoAnulado = 2;

    /** RF-USR-01: `#GN#`/`#GC#` se reintentan hasta 2 veces si no contestan nada. */
    private int maxReintentosSonda = 2;

    /** RF-USR-01, M-3: exigir firmware 3.6.2 (▸ propuesta pendiente de Diego, por defecto true). */
    private boolean exigir362 = true;

    public long silencioMs() {
        return silencioMs;
    }

    public void silencioMs(long v) {
        silencioMs = v;
    }

    public long plazoDisparoMs() {
        return plazoDisparoMs;
    }

    public void plazoDisparoMs(long v) {
        plazoDisparoMs = v;
    }

    public long plazoTramaMs() {
        return plazoTramaMs;
    }

    public void plazoTramaMs(long v) {
        plazoTramaMs = v;
    }

    public long pausaEnvioMs() {
        return pausaEnvioMs;
    }

    public void pausaEnvioMs(long v) {
        pausaEnvioMs = v;
    }

    public long pausaSilencioMs() {
        return pausaSilencioMs;
    }

    public void pausaSilencioMs(long v) {
        pausaSilencioMs = v;
    }

    public long cuarentenaMs() {
        return cuarentenaMs;
    }

    public void cuarentenaMs(long v) {
        cuarentenaMs = v;
    }

    public int lecturasPorColor() {
        return lecturasPorColor;
    }

    /** RF-USR-04, pantalla 5: se fija sólo desde Ajustes, nunca desde la pantalla de medir (T-USR-28). */
    public void lecturasPorColor(int v) {
        if (v < 1) {
            throw new IllegalArgumentException("lecturasPorColor debe ser >= 1");
        }
        lecturasPorColor = v;
    }

    public int maxRepeticionesDisparoAnulado() {
        return maxRepeticionesDisparoAnulado;
    }

    public int maxReintentosSonda() {
        return maxReintentosSonda;
    }

    public boolean exigir362() {
        return exigir362;
    }

    public void exigir362(boolean v) {
        exigir362 = v;
    }
}
