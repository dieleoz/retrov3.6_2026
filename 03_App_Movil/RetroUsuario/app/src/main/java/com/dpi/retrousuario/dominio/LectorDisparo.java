package com.dpi.retrousuario.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee la respuesta de un disparo `::<n>`, sin terminador, dada por completa por **silencio**
 * (RF-USR-16). Patrón `::(\d+)` — misma forma que `rtv-1.0:...Tramas.java:266`
 * ({@code P_MEDIDA = Pattern.compile("::(\\d+)")}), reescrito aquí porque esa clase entera no se
 * reutiliza (SPEC §3).
 *
 * <p>Ficha de trama partida (SPEC RF-USR-16): {@code ::1} llega, no hay silencio todavía —el
 * temporizador de silencio se reinicia con cada byte—, y sigue esperando hasta que llega {@code 23},
 * completando {@code ::123} y confirmando el silencio: entonces se lee 123.</p>
 *
 * <p>Doble `::` (dos respuestas completas en la misma ventana, antes de poder separarlas): el búfer
 * acumulado no es una única `::<n>` limpia (le sobran caracteres, o hay más de una coincidencia del
 * patrón) → se anula como {@link ResultadoDisparo.Tipo#ANULADO_DOBLE}, igual que un plazo vencido
 * (RF-USR-04: no se adivina cuál de las dos es la buena).</p>
 */
final class LectorDisparo {

    private static final Pattern UN_DISPARO = Pattern.compile("^::(\\d+)$");
    private static final Pattern CUALQUIER_DISPARO = Pattern.compile("::(\\d+)");

    private LectorDisparo() {
    }

    /** Envía nada (el envío lo hace el llamante): sólo espera y clasifica la respuesta de un disparo ya enviado. */
    static ResultadoDisparo leer(FuenteBytes fuente, ParametrosRitmo params, long tEnvio) {
        long limiteAbsoluto = tEnvio + params.plazoDisparoMs();
        StringBuilder buffer = new StringBuilder();
        List<ResultadoDisparo.ByteRecibido> recibidos = new ArrayList<>();
        long tUltimoByte = -1;

        while (true) {
            long limiteSilencio = tUltimoByte < 0
                    ? limiteAbsoluto
                    : Math.min(limiteAbsoluto, tUltimoByte + params.silencioMs());
            Integer b = fuente.leer(limiteSilencio);
            if (b == null) {
                break; // silencio confirmado (si ya habia bytes) o plazo total vencido sin nada.
            }
            tUltimoByte = fuente.ahoraMs();
            buffer.append((char) b.intValue());
            recibidos.add(new ResultadoDisparo.ByteRecibido(tUltimoByte - tEnvio, b));
        }

        long tFin = fuente.ahoraMs();
        if (buffer.length() == 0) {
            return new ResultadoDisparo(ResultadoDisparo.Tipo.ANULADO_PLAZO, 0, recibidos, tFin);
        }
        Integer valor = valorUnico(buffer.toString());
        if (valor != null) {
            return new ResultadoDisparo(ResultadoDisparo.Tipo.VALOR, valor, recibidos, tFin);
        }
        return new ResultadoDisparo(ResultadoDisparo.Tipo.ANULADO_DOBLE, 0, recibidos, tFin);
    }

    /** null si el búfer no es EXACTAMENTE una única "::<digitos>" (garantiza que no hay una segunda pegada). */
    private static Integer valorUnico(String buffer) {
        Matcher exacto = UN_DISPARO.matcher(buffer);
        if (!exacto.matches()) {
            return null;
        }
        Matcher cualquiera = CUALQUIER_DISPARO.matcher(buffer);
        int coincidencias = 0;
        while (cualquiera.find()) {
            coincidencias++;
        }
        if (coincidencias != 1) {
            return null;
        }
        try {
            return Integer.parseInt(exacto.group(1));
        } catch (NumberFormatException overflow) {
            return null;
        }
    }
}
