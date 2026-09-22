package com.dpi.retrousuario.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RF-USR-04 r7 (REPETIR-PREGUNTA, DECISIONES nota 18): mide {@code lecturasPorColor} disparos de un
 * color. **La app nunca repite sola**: ante cualquiera de las dos situaciones de abajo, se bloquea
 * en {@link PreguntaOperador} hasta que el operador conteste, sin límite de repeticiones (T-USR-06/
 * T-USR-06b, corregidos por r7 — antes había un tope fijo de reintentos automáticos; ya no):
 * <ul>
 * <li>Un disparo **anulado** por plazo o por doble `::` (RF-USR-16): {@link PreguntaOperador#preguntarDisparoAnulado()}.
 * **Repetir** vuelve a disparar ESE disparo; **Saltar** anula la serie entera de ese color (ningún
 * valor cuenta como "0", {@code guardada = false}).</li>
 * <li>Si la serie completa trae algún **cero**: {@link PreguntaOperador#preguntarCero()}. **Repetir**
 * mide una serie nueva, juzgada igual (si también trae un cero, se pregunta otra vez); **Saltar**
 * guarda la ÚLTIMA serie medida con {@code valido = NO} y {@code motivo = "saturado_o_negativo"} —
 * con {@code media = 0} (no la media aritmética: RF-USR-04, "se guarda la fila con media = 0"),
 * {@code minimo} el mínimo real de esa serie (siempre 0 si hay un cero). Las series descartadas por
 * "Repetir" no entran en el resultado: sólo quedan en {@code log}/{@code diario} (RF-USR-15 bis).</li>
 * </ul>
 */
final class SerieDisparos {

    /** RF-USR-04 r7: texto literal que pinta la app al anular la serie por "El equipo no respondió a
     *  este disparo. ¿Repetir o saltar?" → Saltar (SPEC-App-Usuario-V3.6.md:192). */
    static final String MENSAJE_SERIE_ANULADA = "serie anulada: el equipo no respondió";

    static final class Resultado {
        final boolean guardada;
        final List<Integer> lecturas;
        final int media;
        final int minimo;
        final boolean valido;
        final String motivo;

        private Resultado(boolean guardada, List<Integer> lecturas, int media, int minimo, boolean valido, String motivo) {
            this.guardada = guardada;
            this.lecturas = lecturas;
            this.media = media;
            this.minimo = minimo;
            this.valido = valido;
            this.motivo = motivo;
        }

        static Resultado anulada() {
            return new Resultado(false, Collections.emptyList(), 0, 0, false, "");
        }

        static Resultado de(List<Integer> lecturas, boolean valido) {
            int media = valido ? MediaEntera.deLista(lecturas) : 0;
            int minimo = Collections.min(lecturas);
            String motivo = valido ? "" : "saturado_o_negativo";
            return new Resultado(true, lecturas, media, minimo, valido, motivo);
        }
    }

    private final FuenteBytes fuente;
    private final EmisorRitmo ritmo;
    private final ParametrosRitmo params;
    private final RegistroTramas log;
    private final Diario diario;
    private final int codigoByte;
    private final PreguntaOperador pregunta;

    /**
     * @param diario persistencia por disparo (RF-USR-06, C5): cada disparo válido de **cualquier**
     *               intento (incluida una serie que luego se descarte por "Repetir" tras un cero) se
     *               anota al recibirlo, con un {@code intentoId} propio por intento; sólo el intento
     *               que llega a cerrarse produce, aparte, la línea {@code FILA} (en
     *               {@link SesionMedicion}), así que un intento descartado no sobrevive al reabrir
     *               aunque sus disparos sueltos queden en el fichero.
     * @param pregunta r7: a quién preguntar "Repetir o Saltar" — nunca decide este dominio solo.
     */
    SerieDisparos(FuenteBytes fuente, EmisorRitmo ritmo, ParametrosRitmo params, RegistroTramas log,
            Diario diario, int codigoByte, PreguntaOperador pregunta) {
        this.fuente = fuente;
        this.ritmo = ritmo;
        this.params = params;
        this.log = log;
        this.diario = diario;
        this.codigoByte = codigoByte;
        this.pregunta = pregunta;
    }

    Resultado medir() {
        // A1: n se fija UNA vez, al empezar esta serie (que puede llevar varios intentos, uno por
        // cada "Repetir" del operador tras un cero); un cambio de lecturasPorColor desde Ajustes a
        // mitad de esta serie no la afecta, sólo se aplica a la siguiente medida que se pida.
        int n = params.lecturasPorColor();
        List<Integer> serie = medirSerieCompleta(n);
        if (serie == null) {
            return Resultado.anulada();
        }
        // r7: nunca se repite sola. Mientras la serie traiga un 0, se pregunta; sin límite.
        while (serie.contains(0)) {
            if (pregunta.preguntarCero() == PreguntaOperador.Decision.SALTAR) {
                return Resultado.de(serie, false); // última serie medida, media=0, motivo saturado_o_negativo.
            }
            List<Integer> nueva = medirSerieCompleta(n);
            if (nueva == null) {
                return Resultado.anulada();
            }
            serie = nueva;
        }
        return Resultado.de(serie, true);
    }

    /** null si el operador saltó tras un disparo anulado (RF-USR-16): la serie entera se anula. */
    private List<Integer> medirSerieCompleta(int n) {
        long intentoId = diario.nuevoIntento();
        List<Integer> valores = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Integer v = medirUnDisparo();
            if (v == null) {
                return null;
            }
            diario.registrarDisparo(intentoId, i, v); // RF-USR-06, C5: al recibirlo, antes de media/minimo.
            valores.add(v);
        }
        return valores;
    }

    /** r7: un disparo anulado por plazo o doble `::` se pregunta de inmediato, sin reintentos
     *  automáticos ni tope fijo — "Repetir" vuelve a este mismo bucle, "Saltar" devuelve null. */
    private Integer medirUnDisparo() {
        while (true) {
            long tEnvio = ritmo.enviarConPausa(fuente, params, codigoByte);
            log.tx(tEnvio, String.valueOf((char) codigoByte));
            ResultadoDisparo r = LectorDisparo.leer(fuente, params, tEnvio);
            if (!r.anulado()) {
                registrarRecepcion(tEnvio, r);
                ritmo.registrarDisparo(r);
                return r.valor();
            }
            anotarAnulacionYCuarentena(r);
            if (pregunta.preguntarDisparoAnulado() == PreguntaOperador.Decision.SALTAR) {
                return null;
            }
        }
    }

    private void registrarRecepcion(long tEnvio, ResultadoDisparo r) {
        if (r.recibidos().isEmpty()) {
            return;
        }
        StringBuilder texto = new StringBuilder();
        long tUltimo = tEnvio;
        for (ResultadoDisparo.ByteRecibido b : r.recibidos()) {
            texto.append((char) b.valor);
            tUltimo = tEnvio + b.tMsRelativo;
        }
        log.rx(tUltimo, texto.toString());
    }

    private void anotarAnulacionYCuarentena(ResultadoDisparo r) {
        String causa = r.tipo() == ResultadoDisparo.Tipo.ANULADO_PLAZO
                ? "ANULADO disparo sin respuesta, plazo " + params.plazoDisparoMs() + " ms"
                : "ANULADO doble disparo (dos respuestas sin separar)";
        log.comentario(r.tFin(), causa);
        long tInicioCuarentena = r.tFin();
        List<ResultadoDisparo.ByteRecibido> descartados = ritmo.cuarentena(fuente, params, tInicioCuarentena);
        for (ResultadoDisparo.ByteRecibido b : descartados) {
            log.comentario(tInicioCuarentena + b.tMsRelativo,
                    "DESCARTADO_EN_CUARENTENA " + String.format("%02X", b.valor));
        }
    }
}
