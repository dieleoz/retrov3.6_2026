package com.dpi.retrov36;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A4B-FILTRO (decisión de Diego, nota 21 de DECISIONES; cierra el bucle declarado en Cov_3.6.7_calibrar,
 * 05_Documentacion/REVISIONES-Apps-V3.6.md) — RF-COV-21: en la app corta ("RTV Calibra", BuildConfig.CORTO)
 * el operador NUNCA ve el número de código. En vez de perseguir cada sitio que arma un texto con el
 * número (3.6.6 y 3.6.7 lo intentaron dos veces y las dos se quedaron cortas: motivoNoEscribir alcanzable
 * por CalibracionManual.validarYPreparar seguía crudo en la 3.6.7, REVISIONES :25-26), TODO texto que
 * llega a una vista o diálogo del operador en corto pasa por el ÚNICO filtro de esta clase, en la frontera
 * de la app (CalibrarActivity), antes de setText/AlertDialog/Toast.
 *
 * Clase NUEVA a propósito: FlujoCalibracion.java no crece (rules/modularidad.md, ya en 2003 líneas) y esto
 * no es lógica de calibración, es presentación en la frontera de pantalla.
 */
final class TextoOperador {

    private TextoOperador() { }

    /**
     * RF-COV-21: sustituye cualquier forma del número de código — "código k", "del k", "el k", "#S,k",
     * "#G,k", "#F,k#", "#E,k" — por el nombre de color de {@link Fabrica#elCodigo(char, boolean)} (corto).
     * Los códigos válidos son los de {@link Fabrica#CODIGOS} (1-8, a-d); no se toca ningún otro texto. No
     * se arregla texto a texto en cada sitio que lo genera: un único filtro, aquí (decisión A4B-FILTRO).
     */
    private static final Pattern FORMAS = Pattern.compile(
            "(?:[Cc]ódigo\\s+|\\b[Dd]el\\s+|\\b[Ee]l\\s+|#S,\\s*|#G,\\s*|#E,\\s*)([1-8a-d])\\b"
                    + "|#F,\\s*([1-8a-d])#");

    /**
     * corto=false: no toca nada (la app de campo sigue igual, RF-COV-17 la deja tal cual). Función pura:
     * no toca el acta guardada ni el ZIP de soporte (documentos técnicos que sí llevan el código, RF-COV-21).
     */
    static String limpiar(boolean corto, String texto) {
        if (!corto || texto == null || texto.isEmpty()) {
            return texto;
        }
        Matcher m = FORMAS.matcher(texto);
        StringBuffer salida = new StringBuffer();
        while (m.find()) {
            char k = (m.group(1) != null ? m.group(1) : m.group(2)).charAt(0);
            m.appendReplacement(salida, Matcher.quoteReplacement(Fabrica.elCodigo(k, true)));
        }
        m.appendTail(salida);
        return salida.toString();
    }

    /**
     * RF-COV-21: en corto el acta (documento técnico, RF-COV-11/P11-M1) queda OCULTA en pantalla: null.
     * En la app de campo, el mismo texto que pintaba CalibrarActivity antes de esta tarea (sin cambio de
     * comportamiento ahí). Función pura para poder probar la ocultación sin instanciar la Activity
     * (Android no se ejecuta en la JVM de estas pruebas).
     */
    static String actaParaPantalla(boolean corto, Acta a, String motivoNoAceptar) {
        if (corto) {
            return null;
        }
        return a == null ? "Sin acta en curso." : a.texto() + "\nPara aceptar: "
                + (motivoNoAceptar == null ? "listo (la verificación final se hace al pulsar)" : motivoNoAceptar);
    }
}
