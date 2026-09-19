package com.dpi.retrov36;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V4 original simulado (RTV 1.0, TDD-V3.6 §7: el simulador V4.1 debe reproducir el bloqueo por '@' sin LEERV,
 * el 0x00 tras "/n/r", el segundo de espera antes de medir y el error arrastrado; si no, la prueba no vale).
 *
 * Lo que emula, con su origen en el fuente V4.1 (D:\IT\P_RetroVertical_V4.6\01_Firmware\base_v4.1_repositorio\):
 * <ul>
 * <li>Solo atiende "@LEERV,<COLOR>,<1|2>@" (Serial.c:86-105). Una trama sin '@' vuelve a reposo sin responder
 *     (:101-104): "#V#", "9", "6", "e" callan. Una '@' sin LEERV deja ST_INIT_ANAT sin salida (:87-100) y el
 *     equipo no responde a nada hasta apagarlo (WDTE = OFF).</li>
 * <li>Respuesta "@LEERV,<entero>@" + "/n/r" literal + 0x00 (Serial.c:31-40 recorre x &lt;= strlen;
 *     Aplicacion.c:312). Al encender, una vez, el saludo y "OFFSET= ..." (Aplicacion.c:94-99).</li>
 * <li>Un segundo de espera antes de medir (Serial.c:182-188) mas la medida: {@link #retardoMs}.</li>
 * <li>Tipo 1: devuelve V = x/10 truncado y hace error = R - 163 (Aplicacion.c:297-298). Otros papeles:
 *     R = (V - error - a)/b + k, recortado a 0 y truncado (Ecuaciones.c, Aplicacion.c:312). Los a, b, k de
 *     este simulador son inventados (no son los del V4.1): la prueba mira el arrastre de error, no el valor.</li>
 * </ul>
 */
final class EquipoSimuladoV4 implements Canal {

    static final String SALUDO = "***** RETROREFLECTOMETRO VERTICAL *****\r\nOFFSET= 12.30\r\n\r\n";
    private static final Pattern P = Pattern.compile("@LEERV,(BLA|AMA|VER|ROJ|AZU|NAR),([12])@");

    final List<String> recibidas = new ArrayList<>();
    /** Cuentas de ADC (media) sobre la superficie colocada. */
    double x = 2000;
    /** error oculto (Aplicacion.c:27). */
    double error;
    /** Del envio al primer byte de la respuesta. */
    long retardoMs = 1600;
    boolean bloqueado;
    boolean conectado = true;
    private boolean saludoPendiente = true;
    /** Lo ultimo que se envio de verdad, bytes incluidos (para mirar el 0x00 y el "/n/r"). */
    String ultimoBruto = "";

    void apagarYEncender() {
        bloqueado = false;
        error = 0;
        saludoPendiente = true;
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

    /** R entero que daria el firmware (y efecto sobre error). */
    int medir(String color, int tipo) {
        double v = x / 10.0;
        double r;
        if (tipo == 1) {
            r = v;
            error = r - 163;
        } else {
            int c = java.util.Arrays.asList(Tramas.COLORES_V4).indexOf(color);
            double a = 20 + 5 * c;
            double b = 0.3 + 0.05 * c;
            r = (v - error - a) / b + 10;
        }
        if (r <= 0) {
            r = 0;
        }
        return (int) r;
    }

    @Override
    public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) throws IOException {
        if (!conectado) {
            throw new IOException("sin enlace (simulado)");
        }
        recibidas.add(t);
        if (bloqueado) {
            return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
        }
        if (t.indexOf('@') >= 0 && !t.startsWith("@LEERV")) {
            bloqueado = true;           // ST_INIT_ANAT sin salida (V4.1:Serial.c:87-100)
            return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
        }
        Matcher m = P.matcher(t);
        if (!m.matches()) {
            if (t.startsWith("@LEERV")) {
                bloqueado = true;       // extraerColor desde un puntero nulo: sin salida conocida
            }
            return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
        }
        int r = medir(m.group(1), Integer.parseInt(m.group(2)));
        String bruto = (saludoPendiente ? SALUDO : "") + "@LEERV," + r + "@/n/r\0";
        saludoPendiente = false;
        ultimoBruto = bruto;
        if (retardoMs > timeoutMs) {
            // Llega tarde: el cliente ya lo dio por perdido; la pausa y el vaciado lo tiran antes de la siguiente.
            return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
        }
        // Lo que hace Cliente: acumula en el Receptor y extrae con el patron estricto.
        Receptor rx = new Receptor();
        rx.agregar(retardoMs, bruto.getBytes(StandardCharsets.ISO_8859_1));
        String trama = rx.completa(tipo, retardoMs + Receptor.SILENCIO_MS);
        return new Cliente.Respuesta(t, trama != null ? Receptor.Desenlace.VALIDA : Receptor.Desenlace.INESPERADA,
                trama, rx.texto(), retardoMs);
    }
}
