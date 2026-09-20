package com.dpi.retrov36;

import java.util.Locale;

/**
 * Familia de firmware de una campaña: la escala en la que están sus números. Java puro (RTV 1.0.0-rc6).
 *
 * Por qué existe. Hasta la rc5, mezclar dos familias en una campaña sólo se AVISABA
 * (ImportadorCampana.compararFirmware), mientras que el equipo y la MAC bloqueaban duro
 * (ImportadorCampana:93-95). Y eso no es simétrico: el equipo y la MAC identifican el aparato, pero la
 * familia dice en qué escala están las x, y **la MAC no cambia al grabar** —es del módulo Bluetooth, no del
 * equipo—, así que un ZIP de SLV-003-2026 de ANTES de grabarle la V4.6 pasa la guarda de equipo tan campante.
 *
 * Lo que se mezclaría no es comparable:
 *
 * - **V4 original** no da x en absoluto (ProtocoloV4Original.daX:69-71): sólo R entera por "@LEERV".
 * - **V3.6** da x con 'e' o invirtiendo el '6' (Protocolo:46).
 * - **V4.6** da x con "#X,&lt;clave&gt;#", y es **otra magnitud**, no la de la V3.6
 *   (BancoCola.Tipo.REPRESENTATIVO_V46:44-49, "sin x_esperada: la x de la V4.6 es otra magnitud, §4.3").
 *
 * Un campana.csv con las dos dentro no se puede separar después: el ZIP no lleva ningún manifiesto que diga
 * de qué familia es. De ahí el evento FAMILIA en el diario, que viaja igual que el evento COLA.
 *
 * El nombre de familia que se guarda es el de {@link Protocolo.Firmware} ("F2020", "F36", "V4_ORIGINAL",
 * "F46"), no el texto de presentación: los textos cambian de una versión a otra y no son contrato.
 */
public final class Familia {

    private Familia() { }

    /** Familia desconocida: no se bloquea por ella (de la ignorancia no se concluye nada). */
    public static final String DESCONOCIDA = "";

    /**
     * Familia de un texto de firmware de los que se guardan en el diario. Los produce
     * Sesion.firmware() (:246-262): "V3.6 &lt;fecha&gt; …", "V4.6 &lt;fecha&gt; …" o, por defecto, el nombre del
     * protocolo — ProtocoloV36.nombre() "V3.6", ProtocoloV46.nombre() "V4.6", ProtocoloV2020.nombre()
     * "V3 2020 (sin e)" y ProtocoloV4Original.nombre() "V4 original (…)".
     *
     * Devuelve {@link #DESCONOCIDA} cuando el texto no identifica ninguna ("sin detectar", "desconocido" o
     * vacío): eso NO bloquea, sólo deja de afirmar.
     */
    public static String de(String firmware) {
        if (firmware == null) {
            return DESCONOCIDA;
        }
        String t = firmware.trim().toUpperCase(Locale.ROOT);
        if (t.isEmpty()) {
            return DESCONOCIDA;
        }
        // El orden importa: "V4 ORIGINAL" antes que cualquier prefijo "V4".
        if (t.startsWith("V4 ORIGINAL")) {
            return Protocolo.Firmware.V4_ORIGINAL.name();
        }
        if (t.startsWith("V4.6")) {
            return Protocolo.Firmware.F46.name();
        }
        if (t.startsWith("V3.6")) {
            return Protocolo.Firmware.F36.name();
        }
        if (t.startsWith("V3 2020")) {
            return Protocolo.Firmware.F2020.name();
        }
        // Un diario ya escrito puede traer el nombre de la constante tal cual (evento FAMILIA).
        for (Protocolo.Firmware f : Protocolo.Firmware.values()) {
            if (f != Protocolo.Firmware.DESCONOCIDO && f.name().equals(t)) {
                return f.name();
            }
        }
        return DESCONOCIDA;
    }

    /** Nombre corto para el operador. */
    public static String texto(String familia) {
        for (Protocolo.Firmware f : Protocolo.Firmware.values()) {
            if (f.name().equals(familia)) {
                return f.texto;
            }
        }
        return "sin identificar";
    }

    /**
     * true si dos familias pueden convivir en una campaña. Una familia desconocida convive con cualquiera:
     * bloquear por no saber dejaría fuera los diarios anteriores a la rc6, que no llevan FAMILIA.
     */
    public static boolean compatibles(String a, String b) {
        return DESCONOCIDA.equals(a) || DESCONOCIDA.equals(b) || a.equals(b);
    }

    /**
     * Por qué no se pueden mezclar, para decírselo al operador. Sólo se llama cuando ya se sabe que no son
     * compatibles.
     */
    public static String porQueNoSeMezclan(String aqui, String viene) {
        return "esta campaña es de un equipo con firmware " + texto(aqui) + " y el ZIP trae medidas de "
                + texto(viene) + ". No es el mismo equipo lógico aunque la MAC coincida: la MAC es del módulo "
                + "Bluetooth y no cambia al grabar. Las x de una familia y las de la otra no son la misma "
                + "magnitud, y un campana.csv con las dos dentro ya no se puede separar. Abra una campaña nueva "
                + "para el firmware de ahora (Campaña, Avanzado, \"Nueva campaña\"); la anterior se archiva, no "
                + "se borra.";
    }
}
