package com.dpi.retrousuario.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RF-USR-04: mide {@code lecturasPorColor} disparos de un color, con las dos reglas de repetición
 * (que no se confunden, T-USR-06/T-USR-06b):
 * <ul>
 * <li>Un disparo **anulado** por plazo o por doble `::` (RF-USR-16) se repite hasta
 * {@link ParametrosRitmo#maxRepeticionesDisparoAnulado()} veces; si todas se anulan, **la serie
 * entera se anula** (ningún valor cuenta como "0").</li>
 * <li>Si la serie completa trae algún **cero**, se repite **la serie entera una vez**; si la
 * repetición también trae un cero, se guarda esa segunda serie con {@code valido = NO} y
 * {@code motivo = "saturado_o_negativo"} — con {@code media = 0} (no la media aritmética: RF-USR-04,
 * "se guarda la fila con media = 0"), {@code minimo} el mínimo real de esa serie (siempre 0 si hay
 * un cero). La primera serie descartada no entra en el resultado: sólo queda en {@code log}.</li>
 * </ul>
 */
final class SerieDisparos {

    static final String MENSAJE_SERIE_ANULADA = "serie anulada: el equipo no respondió a 3 intentos";

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

    /**
     * @param diario persistencia por disparo (RF-USR-06, C5): cada disparo válido de **cualquier**
     *               intento (incluida una primera serie que luego se descarte por traer un cero) se
     *               anota al recibirlo, con un {@code intentoId} propio por intento; sólo el intento
     *               que llega a cerrarse produce, aparte, la línea {@code FILA} (en
     *               {@link SesionMedicion}), así que un intento descartado no sobrevive al reabrir
     *               aunque sus disparos sueltos queden en el fichero.
     */
    SerieDisparos(FuenteBytes fuente, EmisorRitmo ritmo, ParametrosRitmo params, RegistroTramas log,
            Diario diario, int codigoByte) {
        this.fuente = fuente;
        this.ritmo = ritmo;
        this.params = params;
        this.log = log;
        this.diario = diario;
        this.codigoByte = codigoByte;
    }

    Resultado medir() {
        List<Integer> intento1 = intentarSerieCompleta();
        if (intento1 == null) {
            return Resultado.anulada();
        }
        if (!intento1.contains(0)) {
            return Resultado.de(intento1, true);
        }
        List<Integer> intento2 = intentarSerieCompleta();
        if (intento2 == null) {
            return Resultado.anulada();
        }
        return Resultado.de(intento2, !intento2.contains(0));
    }

    /** null si algún disparo agotó sus reintentos por anulación (RF-USR-16): la serie entera se anula. */
    private List<Integer> intentarSerieCompleta() {
        long intentoId = diario.nuevoIntento();
        List<Integer> valores = new ArrayList<>();
        for (int i = 0; i < params.lecturasPorColor(); i++) {
            Integer v = intentarUnDisparo();
            if (v == null) {
                return null;
            }
            diario.registrarDisparo(intentoId, i, v); // RF-USR-06, C5: al recibirlo, antes de media/minimo.
            valores.add(v);
        }
        return valores;
    }

    private Integer intentarUnDisparo() {
        for (int intento = 0; intento <= params.maxRepeticionesDisparoAnulado(); intento++) {
            long tEnvio = ritmo.enviarConPausa(fuente, params, codigoByte);
            log.tx(tEnvio, String.valueOf((char) codigoByte));
            ResultadoDisparo r = LectorDisparo.leer(fuente, params, tEnvio);
            if (!r.anulado()) {
                registrarRecepcion(tEnvio, r);
                ritmo.registrarDisparo(r);
                return r.valor();
            }
            anotarAnulacionYCuarentena(r);
        }
        return null;
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
