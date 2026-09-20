package com.dpi.retrov36;

import java.io.IOException;

/**
 * Qué sirve para qué, de los ficheros que saca la app. Java puro (RTV 1.0.0-rc6, D-2).
 *
 * Hay dos ZIP y no son intercambiables:
 *
 * - **ligero** (`campana_<serie>_<sello>.zip`, Campanas.exportar): incremental, sólo lo nuevo desde la
 *   exportación anterior, con `indice.sha256`. **No lleva `campana.csv`** (Campanas.java:579-580, que sólo lo
 *   añade con `completo = true`), así que **no se puede calcular con él** ni se puede importar en otro teléfono
 *   (CampanaActivity:754-757 lo rechaza por incremental). Es para enviar y archivar la sesión.
 * - **soporte** (`soporte_<serie>_<sello>.zip`, Campanas.exportarSoporte): todo entero, con `campana.csv` y las
 *   tramas. Es el que sirve para calcular coeficientes y el que se importa.
 *
 * Hasta la rc5 el que salía solo al terminar el banco era el **ligero**, y el bueno había que pedirlo a mano
 * (BancoActivity:80). La rc5 lo puso por escrito; el 19-sep, de noche, se comprobó en campo que decirlo no basta.
 *
 * Esta clase no decide qué ficheros lleva cada ZIP —eso es Campanas.piezas— : decide **cómo se reconoce** uno
 * que sirve, para poder comprobarlo sobre un ZIP real sin teléfono delante.
 */
public final class Entregables {

    private Entregables() { }

    /** El fichero sin el cual no se calculan coeficientes (Campanas.java:580). */
    public static final String CSV = "campana.csv";

    public static final String AVISO_LIGERO = "Este es el ZIP LIGERO: sirve para enviar y archivar, NO para calcular "
            + "coeficientes (no lleva " + CSV + ").";

    public static final String AVISO_SOPORTE = "Este es el ZIP DE SOPORTE: es el que sirve para calcular "
            + "coeficientes y el que se importa en otro teléfono.";

    /**
     * true si con este ZIP se pueden calcular coeficientes: lleva {@link #CSV}. Se mira el contenido, no el
     * nombre, porque el nombre lo pone quien reenvía el fichero.
     */
    public static boolean sirveParaCoeficientes(byte[] zip) throws IOException {
        return ImportadorCampana.esZip(zip) && ImportadorCampana.csvDeZip(zip) != null;
    }

    /** Lo que hay que decirle al operador de un ZIP ya hecho. */
    public static String veredicto(byte[] zip) throws IOException {
        return sirveParaCoeficientes(zip) ? AVISO_SOPORTE : AVISO_LIGERO;
    }
}
