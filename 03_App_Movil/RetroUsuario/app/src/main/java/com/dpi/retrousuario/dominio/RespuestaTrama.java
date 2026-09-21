package com.dpi.retrousuario.dominio;

/**
 * Parser de una trama de administracion "#campo,campo,...#" (SPEC-App-Usuario-V3.6.md §3,
 * PROTOCOLO-V3.6.md:34-55). Sólo entiende lo que esta app necesita: {@code #V,...#},
 * {@code #GN,...#}, {@code #GC,...#} y {@code #ERR,<motivo>#} genérico. No entiende
 * {@code "::<n>"} (RF-USR-16, fuera de este incremento) ni ninguna trama de administrador
 * (RF-USR-15: esta app no envia {@code #L#}, no hay nada que desbloquear).
 *
 * "El parser no debe fallar si llega otro motivo [que no sea FORMATO]: lo registra ... y trata la
 * sonda como 'sin respuesta util', igual que un timeout" (SPEC §3). Por eso este parser nunca lanza
 * una excepción con una entrada rara: cualquier cosa que no case con el formato esperado se
 * devuelve como {@link #esInvalida()}, y quien llama decide qué hacer (Sonda362 la trata como
 * silencio).
 */
public final class RespuestaTrama {

    private final boolean invalida;
    private final boolean esError;
    private final String motivoError;
    private final String[] campos;

    private RespuestaTrama(boolean invalida, boolean esError, String motivoError, String[] campos) {
        this.invalida = invalida;
        this.esError = esError;
        this.motivoError = motivoError;
        this.campos = campos;
    }

    /**
     * Analiza una respuesta cruda. {@code cruda == null} (silencio del enlace, timeout de
     * {@link Canal#enviar}) NO es una trama invalida: se representa aparte, sin llamar a este
     * metodo; quien orquesta la sonda (Sonda362) distingue "no llegó nada" de "llegó basura".
     */
    public static RespuestaTrama analizar(String cruda) {
        if (cruda == null || cruda.length() < 2 || cruda.charAt(0) != '#'
                || cruda.charAt(cruda.length() - 1) != '#') {
            return new RespuestaTrama(true, false, null, null);
        }
        String cuerpo = cruda.substring(1, cruda.length() - 1);
        String[] campos = cuerpo.split(",", -1);
        if (campos.length == 0 || campos[0].isEmpty()) {
            return new RespuestaTrama(true, false, null, null);
        }
        if ("ERR".equals(campos[0])) {
            // motivo: texto corto sin comas (PROTOCOLO-V3.6.md:70-72): PIN, BLOQUEADO, FORMATO, EEPROM.
            String motivo = campos.length >= 2 ? campos[1] : "";
            return new RespuestaTrama(false, true, motivo, campos);
        }
        return new RespuestaTrama(false, false, null, campos);
    }

    /** true si la trama no tenia forma de "#...#", o no se pudo trocear en campos. */
    public boolean esInvalida() {
        return invalida;
    }

    /** true si es "#ERR,<motivo>#". */
    public boolean esError() {
        return esError;
    }

    /** "FORMATO", "PIN", "BLOQUEADO", "EEPROM" o cualquier otro texto corto; null si no es ERR. */
    public String motivoError() {
        return motivoError;
    }

    /** true si es "#ERR,FORMATO#" exactamente: la única señal explícita de "actualice el firmware" (RF-USR-01). */
    public boolean esErrorFormato() {
        return esError && "FORMATO".equals(motivoError);
    }

    /** Campo en la posicion {@code i} (0 = la palabra tras el primer '#', p. ej. "V", "GN", "GC"). */
    public String campo(int i) {
        if (invalida || campos == null || i < 0 || i >= campos.length) {
            return null;
        }
        return campos[i];
    }

    /** Numero de campos separados por ','. */
    public int numeroCampos() {
        return campos == null ? 0 : campos.length;
    }
}
