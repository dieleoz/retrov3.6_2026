package com.dpi.retrov36;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * V4.6 simulado con el contrato de PROTOCOLO-V4.6-BORRADOR.md §4 (tabla unica de la revision P2). El firmware
 * no existe: esto emula el contrato, no un equipo. Se apoya en {@link EquipoSimulado} (la 3.6.2 fiel) para
 * todo lo "#...#" que la V4.6 adopta tal cual (#L, #G, #S, #E, #F, #SN, #GN, #SC, #GC, ...), y cambia:
 * <ul>
 * <li>#V# responde "#V,4.6,...#" (5 campos, mascara en el orden 1-8, a-d).</li>
 * <li>La medida es "@LEERV,<COLOR>,<1|2>@" -> "@LEERV,<n>@/n/r" + 0x00, con la clave de §4.2 y la curva de la
 *     clave (la del simulador base). Los bytes sueltos (1-8, a-d, 9, 6, e) no se adoptan: callan.</li>
 * <li>"#X,k#" -> "#X,k,<x>#" con x en %.8E (§4.4; x en float, §4.3). No toca error ni la pantalla.</li>
 * <li>"#GB#" -> "#GB,<n>#" (unidad por fijar). "#T#" -> "#T,25.0,25.0#".</li>
 * </ul>
 * Lo que la 3.6.2 hace distinto y aqui no se cambia: los limites de #S (x 600-4300) siguen siendo los de la
 * 3.6.2, porque los de la V4.6 estan por fijar (L-19).
 */
final class EquipoSimuladoV46 implements Canal {

    final EquipoSimulado base;
    /** Todas las tramas, tambien las @LEERV y las propias de la V4.6. */
    final List<String> recibidas = new ArrayList<>();

    EquipoSimuladoV46(EquipoSimulado base) {
        this.base = base;
    }

    /** V4.6 con la EEPROM en blanco: DEF, #GN,NONE#, #GC,NONE# (RF-APP-U13). */
    static EquipoSimuladoV46 enBlanco() {
        return new EquipoSimuladoV46(new EquipoSimulado());
    }

    int cuantas(String prefijo) {
        int n = 0;
        for (String r : recibidas) {
            if (r.startsWith(prefijo)) {
                n++;
            }
        }
        return n;
    }

    private static Cliente.Respuesta valida(String p, String t) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.VALIDA, t, t, 40);
    }

    private static Cliente.Respuesta timeout(String p) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.TIMEOUT, null, "", -1);
    }

    @Override
    public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) throws IOException {
        if (!base.conectado) {
            throw new IOException("sin enlace (simulado)");
        }
        recibidas.add(t);
        if (Tramas.esLeervValida(t)) {
            char k = Tramas.claveDeLeerv(t);
            String r = base.responder(String.valueOf(k));
            Integer n = Tramas.valorMedida(r);
            String bruto = "@LEERV," + (n == null ? 0 : n) + "@/n/r\0";
            Receptor rx = new Receptor();
            rx.agregar(1600, bruto.getBytes(StandardCharsets.ISO_8859_1));
            String trama = rx.completa(tipo, 1600 + Receptor.SILENCIO_MS);
            return new Cliente.Respuesta(t, trama != null ? Receptor.Desenlace.VALIDA : Receptor.Desenlace.INESPERADA,
                    trama, rx.texto(), 1600);
        }
        if (t.indexOf('@') >= 0) {
            throw new AssertionError("trama con '@' que no es @LEERV: " + t);
        }
        if (!t.startsWith("#")) {
            return timeout(t);       // la V4.6 no adopta los bytes sueltos (RF-FW-34)
        }
        if (t.equals("#V#")) {
            Cliente.Respuesta r = base.pedir(t, tipo, timeoutMs);
            return r.valida() ? valida(t, r.trama.replace("#V,3.6,", "#V,4.6,")) : r;
        }
        if (t.startsWith("#X,") && t.length() == 5 && t.endsWith("#") && Fabrica.esCodigo(t.charAt(3))) {
            return valida(t, "#X," + t.charAt(3) + "," + String.format(Locale.US, "%.8E", base.x) + "#");
        }
        if (t.equals("#GB#")) {
            return valida(t, "#GB," + base.bateriaN + "#");
        }
        if (t.equals("#T#")) {
            return valida(t, "#T,25.0,25.0#");
        }
        return base.pedir(t, tipo, timeoutMs);
    }
}
