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
 * Precisión de este trabajo, sin ficha propia en TDD-V3.6.md §8 que la fije con casos mixtos: si
 * #GN# y #GC# no coinciden (uno ERR,FORMATO y el otro silencio, o uno ERR,FORMATO y el otro OK), el
 * ERR,FORMATO explícito manda sobre la ambigüedad del otro campo: ACTUALIZAR_FIRMWARE. Las tres
 * fichas de T-USR-01b (T-USR-01b (b)-(d)) sólo prueban los casos uniformes (los dos OK, los dos ERR,
 * los dos mudos); esta regla de desempate no está medida contra un caso mixto todavía.
 */
public final class Sonda362 {

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

    public static ResultadoSonda sondear(Canal canal) {
        String crudaGN = canal.enviar("#GN#", PLAZO_MS);
        String crudaGC = canal.enviar("#GC#", PLAZO_MS);
        RespuestaTrama tGN = RespuestaTrama.analizar(crudaGN);
        RespuestaTrama tGC = RespuestaTrama.analizar(crudaGC);
        Clasificacion cGN = clasificar(crudaGN, tGN, "GN");
        Clasificacion cGC = clasificar(crudaGC, tGC, "GC");

        if (cGN == Clasificacion.ERR_FORMATO || cGC == Clasificacion.ERR_FORMATO) {
            return new ResultadoSonda(Resultado.ACTUALIZAR_FIRMWARE, "", false, "", false);
        }
        if (cGN != Clasificacion.OK || cGC != Clasificacion.OK) {
            return new ResultadoSonda(Resultado.SIN_RESPUESTA_REINTENTAR, "", false, "", false);
        }
        boolean serieLeida = !"NONE".equals(tGN.campo(1));
        String serie = serieLeida ? tGN.campo(1) : "";
        boolean fechaRegistrada = !"NONE".equals(tGC.campo(1));
        String fecha = fechaRegistrada ? tGC.campo(1) : "";
        return new ResultadoSonda(Resultado.OK, serie, serieLeida, fecha, fechaRegistrada);
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
