package com.dpi.retrousuario.dominio;

/**
 * RF-USR-04 r7 (REPETIR-PREGUNTA, DECISIONES nota 18, sustituye a "se repite una vez" de H-A3):
 * **la app nunca repite sola**. Ante una serie con algún `0` o un disparo anulado por plazo o por
 * doble `::` (RF-USR-16), {@link SerieDisparos} se bloquea en esta llamada hasta que el operador
 * conteste "Repetir" o "Saltar" — dominio puro, sin Android: quien mida (la Activity) implementa
 * esto con un diálogo y bloquea el hilo de fondo hasta que el operador lo cierre.
 *
 * <p>Sin límite de repeticiones (DECISIONES nota 18): decide el operador, no un contador fijo — por
 * eso esta interfaz no lleva ningún parámetro de "intento número N", sólo la pregunta.</p>
 */
public interface PreguntaOperador {

    enum Decision {
        REPETIR,
        SALTAR
    }

    /**
     * La serie completa ({@code lecturasPorColor} disparos) trajo algún `0`. **Repetir** mide una
     * serie nueva, que se juzga igual (si también trae un `0`, se pregunta otra vez); **Saltar**
     * guarda la fila con {@code media = 0}, {@code válido = NO} y {@code motivo =
     * "saturado_o_negativo"}, con las lecturas de la última serie medida.
     */
    Decision preguntarCero();

    /**
     * Un disparo de la serie se anuló por plazo o por doble `::` (RF-USR-16). **Repetir** vuelve a
     * disparar ese mismo disparo; **Saltar** anula la serie entera de ese color (sin fila).
     */
    Decision preguntarDisparoAnulado();
}
