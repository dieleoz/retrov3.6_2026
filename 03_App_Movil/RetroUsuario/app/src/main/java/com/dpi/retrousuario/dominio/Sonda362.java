package com.dpi.retrousuario.dominio;

/**
 * RF-USR-01 (M-3): tras confirmar "#V,3.6,", exige firmware 3.6.2 probando "#GN#" y "#GC#" (ninguna
 * mide, PROTOCOLO-V3.6.md:52,54). Firmware 3.6/3.6.1, que no tienen esas órdenes, caen en el "else"
 * de {@code adminProcesarTrama} y responden "#ERR,FORMATO#" ({@code calibracion_v36.c:805-807}).
 *
 * Regla de silencio (RF-USR-01, corrige lo que decía la r1 de esta ficha en r2/r3 de la SPEC): un
 * timeout NO es lo mismo que un "#ERR,FORMATO#" explícito. Sólo el ERR explícito dispara "actualice
 * el firmware"; el silencio ofrece reintentar la sonda, porque puede ser el enlace, no la versión.
 * Un "#ERR,<otro motivo>#" (PIN, BLOQUEADO, EEPROM: no deberían darse aquí, esta app no hace #L#) se
 * trata igual que el silencio (SPEC §3: "sin respuesta útil"), no como "actualice el firmware".
 *
 * Caso mixto (T-USR-01c(c), TDD-V3.6.md r6): si #GN# y #GC# no coinciden, dos reglas, en este orden:
 * (1) un ERR,FORMATO explícito en cualquiera de las dos manda sobre la ambigüedad del otro campo:
 * ACTUALIZAR_FIRMWARE con {@code exigir_362 = true} (degradado si {@code false}, igual que el caso
 * uniforme). (2) si ninguna de las dos dio ERR,FORMATO pero SÍ contestó una de ellas (la otra en
 * silencio o "sin respuesta útil" — un ERR,&lt;motivo≠FORMATO&gt;, T-USR-01c(c)), la sonda mide con lo
 * que sí contestó (p. ej. la fecha de #GC# aunque #GN# no diera nada), como en la rama degradada de
 * {@code exigir_362 = false} — esto pasa **con cualquier valor de exigir_362**, porque no es la falta
 * de firmware 3.6.2 lo que impide medir, es que una de las dos SÍ lo confirmó. Sólo cuando NINGUNA de
 * las dos contesta nada útil se aplica la puerta de {@code exigir_362} (SIN_RESPUESTA_REINTENTAR si es
 * {@code true}). Antes de esta corrección (r6, condición QA-2), un GN mudo/sin-respuesta-útil con GC
 * contestando perdía la fecha de GC sin motivo bajo {@code exigir_362 = true} (daba
 * SIN_RESPUESTA_REINTENTAR con la fecha descartada; ver {@code Sonda362Test}, T-USR-01c(c)).
 */
public final class Sonda362 {

    /** @deprecated usar {@link ParametrosRitmo#plazoTramaMs()}; se mantiene como valor de conveniencia. */
    @Deprecated
    public static final long PLAZO_MS = DetectorEquipo.PLAZO_MS;

    public enum Resultado { OK, ACTUALIZAR_FIRMWARE, SIN_RESPUESTA_REINTENTAR }

    public static final class ResultadoSonda {
        private final Resultado resultado;
        private final String serie;
        private final boolean serieLeida;
        private final String fechaCalibracion;
        private final boolean fechaRegistrada;

        private ResultadoSonda(Resultado resultado, String serie, boolean serieLeida,
                String fechaCalibracion, boolean fechaRegistrada) {
            this.resultado = resultado;
            this.serie = serie;
            this.serieLeida = serieLeida;
            this.fechaCalibracion = fechaCalibracion;
            this.fechaRegistrada = fechaRegistrada;
        }

        public Resultado resultado() {
            return resultado;
        }

        /** Serie leída, o "" si {@link #serieLeida()} es false. No confundir con SERIE-USR (incremento 2). */
        public String serie() {
            return serie;
        }

        /** false si el equipo respondió "#GN,NONE#" (3.6.2 sin serie grabada en EEPROM, RF-USR-06). */
        public boolean serieLeida() {
            return serieLeida;
        }

        /** "leida" o "ninguna" (columna serie_origen de RF-USR-06). */
        public String origenSerie() {
            return serieLeida ? "leida" : "ninguna";
        }

        public String fechaCalibracion() {
            return fechaCalibracion;
        }

        /** false si "#GC,NONE#" (EEPROM en blanco, CRC mala o fecha fuera de rango, PROTOCOLO-V3.6.md:52). */
        public boolean fechaRegistrada() {
            return fechaRegistrada;
        }
    }

    private enum Clasificacion { OK, ERR_FORMATO, SIN_RESPUESTA_UTIL, SILENCIO }

    private Sonda362() {
    }

    /** Compatibilidad: exigir_362 = true (por defecto) y plazo/pacing por defecto. */
    public static ResultadoSonda sondear(Canal canal) {
        return sondear(canal, new ParametrosRitmo());
    }

    /**
     * M-3/exigir_362 (RF-USR-01, T-USR-01c): con {@code params.exigir362() == true}, un
     * "#ERR,FORMATO#" en #GN# o #GC# pide actualizar firmware y no se mide, y si ninguna de las dos
     * contesta nada útil (tras los reintentos) se ofrece reintentar, sin medir. Con
     * {@code params.exigir362() == false}, la app mide igual sea cual sea la respuesta de #GN#/#GC#
     * (incluido "#ERR,FORMATO#" o silencio): el resultado se da como {@link Resultado#OK} con lo que
     * sí se pudo leer, y {@code serieLeida}/{@code fechaRegistrada} en false donde no se pudo (la
     * fila queda con {@code serie_origen = "ninguna"} y {@code estado_calibracion = "sin_fecha"} si
     * corresponde, decidido en {@link SesionMedicion}, no aquí).
     *
     * <p>M5: 150 ms de pausa entre el envío de #GN# y el de #GC# (equipo ya identificado,
     * SPEC-V3.6.md:441-446), y el plazo de cada trama es {@code params.plazoTramaMs()} (2000 ms por
     * defecto), no el plazo de un disparo.</p>
     */
    public static ResultadoSonda sondear(Canal canal, ParametrosRitmo params) {
        return sondear(canal, params, Sonda362::pausaReal);
    }

    /** Visible para pruebas (evita un {@code Thread.sleep} real de 150 ms en la JVM, M5). */
    static ResultadoSonda sondear(Canal canal, ParametrosRitmo params, java.util.function.LongConsumer pausa) {
        String crudaGN = enviarConReintentos(canal, "#GN#", params);
        pausa.accept(150L); // M5: 150 ms entre dos tramas "#", equipo ya identificado.
        String crudaGC = enviarConReintentos(canal, "#GC#", params);
        RespuestaTrama tGN = RespuestaTrama.analizar(crudaGN);
        RespuestaTrama tGC = RespuestaTrama.analizar(crudaGC);
        Clasificacion cGN = clasificar(crudaGN, tGN, "GN");
        Clasificacion cGC = clasificar(crudaGC, tGC, "GC");

        if (cGN == Clasificacion.OK && cGC == Clasificacion.OK) {
            return resultadoOk(tGN, tGC);
        }
        if (cGN == Clasificacion.ERR_FORMATO || cGC == Clasificacion.ERR_FORMATO) {
            if (!params.exigir362()) {
                return resultadoOkDegradado(cGN, tGN, cGC, tGC);
            }
            return new ResultadoSonda(Resultado.ACTUALIZAR_FIRMWARE, "", false, "", false);
        }
        // Ni ERR,FORMATO en ninguna, ni las dos OK: si alguna SÍ contestó (caso mixto, T-USR-01c(c)),
        // se mide con lo que contestó, sea cual sea exigir_362 (esa puerta es sólo para "ninguna dio
        // nada útil"). Si ninguna contestó nada útil, exigir_362 decide igual que antes.
        if (cGN == Clasificacion.OK || cGC == Clasificacion.OK) {
            return resultadoOkDegradado(cGN, tGN, cGC, tGC);
        }
        if (!params.exigir362()) {
            return resultadoOkDegradado(cGN, tGN, cGC, tGC);
        }
        return new ResultadoSonda(Resultado.SIN_RESPUESTA_REINTENTAR, "", false, "", false);
    }

    private static ResultadoSonda resultadoOk(RespuestaTrama tGN, RespuestaTrama tGC) {
        boolean serieLeida = !"NONE".equals(tGN.campo(1));
        String serie = serieLeida ? tGN.campo(1) : "";
        boolean fechaRegistrada = !"NONE".equals(tGC.campo(1));
        String fecha = fechaRegistrada ? tGC.campo(1) : "";
        return new ResultadoSonda(Resultado.OK, serie, serieLeida, fecha, fechaRegistrada);
    }

    /** exigir_362 = false: mide igual con lo que se pudo leer; lo que falte queda vacío/no leído. */
    private static ResultadoSonda resultadoOkDegradado(Clasificacion cGN, RespuestaTrama tGN,
            Clasificacion cGC, RespuestaTrama tGC) {
        boolean serieLeida = cGN == Clasificacion.OK && !"NONE".equals(tGN.campo(1));
        String serie = serieLeida ? tGN.campo(1) : "";
        boolean fechaRegistrada = cGC == Clasificacion.OK && !"NONE".equals(tGC.campo(1));
        String fecha = fechaRegistrada ? tGC.campo(1) : "";
        return new ResultadoSonda(Resultado.OK, serie, serieLeida, fecha, fechaRegistrada);
    }

    /** RF-USR-01: si #GN#/#GC# no contestan nada, reintenta hasta {@code maxReintentosSonda} veces
     *  (2 por defecto, 3 intentos en total); una respuesta (incluido un "#ERR,...#") no se reintenta. */
    private static String enviarConReintentos(Canal canal, String trama, ParametrosRitmo params) {
        String r = canal.enviar(trama, params.plazoTramaMs());
        for (int intento = 0; r == null && intento < params.maxReintentosSonda(); intento++) {
            r = canal.enviar(trama, params.plazoTramaMs());
        }
        return r;
    }

    private static void pausaReal(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Clasificacion clasificar(String cruda, RespuestaTrama t, String campoEsperado) {
        if (cruda == null) {
            return Clasificacion.SILENCIO;
        }
        if (t.esInvalida()) {
            return Clasificacion.SIN_RESPUESTA_UTIL;
        }
        if (t.esError()) {
            return t.esErrorFormato() ? Clasificacion.ERR_FORMATO : Clasificacion.SIN_RESPUESTA_UTIL;
        }
        if (!campoEsperado.equals(t.campo(0)) || t.numeroCampos() != 2) {
            return Clasificacion.SIN_RESPUESTA_UTIL;
        }
        return Clasificacion.OK;
    }
}
