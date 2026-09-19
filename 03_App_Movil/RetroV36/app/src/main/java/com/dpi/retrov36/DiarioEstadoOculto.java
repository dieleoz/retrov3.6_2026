package com.dpi.retrov36;

/**
 * Diario del estado oculto del V4 original (RTV 1.0, RF-APP-U08). Java puro.
 *
 * En el V4.1 toda medida de tipo 1 hace error = R - 163 (V4.1:Aplicacion.c:270-271 desde la pantalla,
 * :297-298 por Bluetooth), y ese error entra en la siguiente medida de otros papeles
 * (V4.1:Ecuaciones.c:20,28,38,48,57,66): el resultado depende del orden de los disparos. Se anota en cada
 * medida de otros papeles la ultima de tipo 1 que envio la app en esta conexion, o "estado previo
 * desconocido". Una de tipo 1 hecha desde la pantalla tambien lo cambia sin que la app lo sepa.
 */
public final class DiarioEstadoOculto {

    public static final String DESCONOCIDO = "estado previo desconocido";

    private String ultimaTipo1;
    private int desconocidas;

    /** Al conectar: nada se sabe. */
    public synchronized void reiniciar() {
        ultimaTipo1 = null;
        desconocidas = 0;
    }

    /**
     * Anota una medida hecha por la app y devuelve la nota para ella: en otros papeles, la previa; en tipo 1,
     * "escribe error".
     */
    public synchronized String anotar(char k, Integer valor, boolean sonda) {
        String trama = Tramas.tramaLeerv(k);
        String clave = trama == null ? String.valueOf(k) : trama.substring(7, trama.length() - 1);
        if (Tramas.esTipo1(k)) {
            ultimaTipo1 = clave + " = " + (valor == null ? "?" : valor) + (sonda ? " (sonda)" : "");
            return "tipo 1: reescribe el error oculto del V4.1";
        }
        String nota = ultimaTipo1 == null ? DESCONOCIDO : "previa: " + ultimaTipo1;
        if (ultimaTipo1 == null) {
            desconocidas++;
        }
        return nota + (sonda ? " (sonda; deja la pantalla en BLANCO, otros papeles)" : "");
    }

    /** Aviso para el resumen: cuantas medidas de otros papeles tienen el estado previo desconocido. */
    public synchronized String aviso() {
        return desconocidas == 0 ? "" : "ATENCIÓN: " + desconocidas + " medidas de otros papeles con " + DESCONOCIDO
                + " (el resultado del V4 depende de la última medida de tipo 1).";
    }
}
