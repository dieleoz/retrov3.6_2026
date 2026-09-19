package com.dpi.retrov36;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Deteccion del firmware al conectar (RTV 1.0; RF-APP-U01 a U04 de SPEC-App-Unica-V36-V46.md, con C-U01 y
 * C-U02 como las cierra la revision P2 §4). Java puro: habla con el equipo por un {@link Canal}.
 *
 * Orden, y solo este: #V# -> 9 -> 6 -> sonda "@LEERV,BLA,2@" (espera de 5 s). Nunca 'e', nunca una '@' que
 * no sea la sonda, nunca otro byte suelto que 9 y 6, a un equipo sin identificar. Con un V3.6 o una V4.6 se
 * leen ademas #GC# y, si es la variante con serie, #GN#.
 */
public final class Deteccion {

    private Deteccion() { }

    /** Timeouts de la deteccion (los de Pruebas en la 3.6.16). */
    public static final long TIMEOUT_V_MS = Ops.TIMEOUT_ADMIN_MS + 500;

    /** Las unicas tramas que salen antes de identificar el firmware (T-U06). */
    public static final List<String> PERMITIDAS = Arrays.asList("#V#", "#GC#", "#GN#", "9", "6", Tramas.SONDA_V4);

    public static final String SIN_RESPUESTA = "Sin respuesta válida. Puede ser un firmware que esta app no conoce, "
            + "o el módulo Bluetooth del equipo a otra velocidad que el equipo (la app no puede verlo).";
    public static final String CONTRADICE = "El equipo no responde como su perfil";

    public static final String MARCA_DECLARADA = " (serie declarada, no leída del equipo)";

    /**
     * RF-APP-U11: marca de la serie cuando no sale de #GN# (V3.6.2 y V4.6 con serie grabada, sin marca; V3.6.0/1,
     * V3 2020 y V4 original, con marca).
     */
    public static String marcaSerie(Protocolo p, Calibracion.Variante variante, String serieEquipo) {
        boolean conGN = p != null && p.administra()
                && (p.firmware() == Protocolo.Firmware.F46 || variante == Calibracion.Variante.V362);
        return conGN && serieEquipo != null && !Calibracion.NONE.equals(serieEquipo) ? "" : MARCA_DECLARADA;
    }

    public static boolean permitida(String t) {
        return PERMITIDAS.contains(t);
    }

    public static final class Resultado {
        /** Protocolo con el que operar; null si no se opera (desconocido o contradice el perfil). */
        public Protocolo protocolo;
        public Protocolo.Firmware firmware = Protocolo.Firmware.DESCONOCIDO;
        public Tramas.InfoVersion version;
        public Calibracion.Variante variante = Calibracion.Variante.DESCONOCIDA;
        /** #GN# (null si no se leyo). */
        public String serieEquipo;
        /** #GC# (null si no se leyo). */
        public String fechaCalibracion;
        /** Huella para el perfil aprendido. */
        public String huella = "";
        public boolean perfilCaducado;
        public boolean contradicePerfil;
        public final StringBuilder detalle = new StringBuilder();

        public boolean opera() {
            return protocolo != null;
        }
    }

    /**
     * @param firmado   perfil firmado de equipos.csv para esta MAC, o null.
     * @param aprendido lo aprendido de esta MAC en la ultima deteccion, o null.
     * @param completa  true si el operador pide la deteccion completa aunque el perfil sea SOLO_SONDA (tras
     *                  regrabar el equipo): entonces se envia #V#.
     */
    public static Resultado detectar(Canal c, PerfilesEquipo.Perfil firmado, PerfilesEquipo.Aprendido aprendido,
                                     boolean completa) throws IOException, InterruptedException {
        Resultado r = new Resultado();
        boolean soloSonda = firmado != null && firmado.soloSonda() && !completa
                && !(aprendido != null && aprendido.firmware == Protocolo.Firmware.F46);
        if (firmado != null && firmado.soloSonda() && !soloSonda) {
            r.perfilCaducado = true;
            r.detalle.append("Perfil SOLO_SONDA de equipos.csv no aplicado: ").append(completa
                    ? "el operador pidió la detección completa" : "la última detección vio una V4.6").append('\n');
        }
        if (soloSonda) {
            r.detalle.append("Perfil de equipos.csv (").append(firmado.documento).append("): sólo la sonda.\n");
            if (sonda(c, r)) {
                r.protocolo = new ProtocoloV4Original(firmado.variante);
                r.firmware = Protocolo.Firmware.V4_ORIGINAL;
                r.huella = "SONDA";
            } else {
                r.contradicePerfil = true;
                r.detalle.append(CONTRADICE).append(": no respondió a la sonda.\n").append(SIN_RESPUESTA);
            }
            return r;
        }
        detectarCompleta(c, r, firmado);
        confirmar(r, firmado, aprendido);
        return r;
    }

    private static void detectarCompleta(Canal c, Resultado r, PerfilesEquipo.Perfil firmado)
            throws IOException, InterruptedException {
        Cliente.Respuesta rv = c.pedir("#V#", Tramas.Tipo.ADMIN, TIMEOUT_V_MS);
        r.detalle.append("#V# -> ").append(rv.describir()).append('\n');
        Tramas.InfoVersion iv = rv.valida() ? Tramas.parsearVersion(rv.trama) : null;
        if (iv != null) {
            r.version = iv;
            r.huella = "V," + iv.version + "," + iv.fecha;
            if ("3.6".equals(iv.version) || "4.6".equals(iv.version)) {
                boolean v46 = "4.6".equals(iv.version);
                identidad(c, r, v46);
                r.firmware = v46 ? Protocolo.Firmware.F46 : Protocolo.Firmware.F36;
                r.protocolo = v46 ? new ProtocoloV46() : new ProtocoloV36();
                r.detalle.append(String.format(Locale.US, "Versión %s, fecha %s, marca %s%s", iv.version, iv.fecha,
                        iv.marca, iv.tieneMascara() ? String.format(Locale.US, ", máscara %04X", iv.mascara) : ""));
                return;
            }
            // C-U03: cualquier otra version (4.6A, 4.6B, ...) es desconocida y no se envia nada mas.
            r.detalle.append("Responde a #V# con versión ").append(iv.version).append(": no es la del contrato. "
                    + "No se envía nada más.");
            return;
        }
        Cliente.Respuesta r9 = c.pedir("9", Tramas.Tipo.BATERIA, Ops.TIMEOUT_MEDIDA_MS);
        r.detalle.append("9 -> ").append(r9.describir()).append('\n');
        if (r9.valida()) {
            r.firmware = Protocolo.Firmware.F2020;
            r.protocolo = new ProtocoloV2020();
            r.huella = "9";
            r.detalle.append("V3 de 2020 (responde :n: a 9). Se medirá con 6 invertido; nunca con e.");
            return;
        }
        Cliente.Respuesta r6 = c.pedir("6", Tramas.Tipo.MEDIDA, Ops.TIMEOUT_MEDIDA_MS);
        r.detalle.append("6 -> ").append(r6.describir()).append('\n');
        if (r6.valida()) {
            r.firmware = Protocolo.Firmware.F2020;
            r.protocolo = new ProtocoloV2020();
            r.huella = "6";
            r.detalle.append("V3 de 2020 (responde ::n a 6). Se medirá con 6 invertido; nunca con e.");
            return;
        }
        if (sonda(c, r)) {
            r.firmware = Protocolo.Firmware.V4_ORIGINAL;
            r.protocolo = new ProtocoloV4Original(firmado == null ? "" : firmado.variante);
            r.huella = "SONDA";
            r.detalle.append("V4 original (protocolo @LEERV). ").append(ProtocoloV4Original.MOTIVO).append('.');
            return;
        }
        r.detalle.append(SIN_RESPUESTA);
    }

    /** La sonda, con su espera de 5 s. true si respondio "@LEERV,<entero>@". */
    private static boolean sonda(Canal c, Resultado r) throws IOException, InterruptedException {
        Cliente.Respuesta r4 = c.pedir(Tramas.SONDA_V4, Tramas.Tipo.LEERV, Tramas.TIMEOUT_LEERV_MS);
        r.detalle.append(Tramas.SONDA_V4).append(" -> ").append(r4.describir()).append('\n');
        boolean ok = r4.valida() && Tramas.valorLeerv(r4.trama) != null;
        if (ok) {
            r.detalle.append("La sonda deja la pantalla del equipo en BLANCO, otros papeles (H-06).\n");
        }
        return ok;
    }

    /** #GC# y, si la variante tiene serie, #GN#. Solo tras identificar V3.6 o V4.6. */
    private static void identidad(Canal c, Resultado r, boolean v46) throws IOException, InterruptedException {
        Cliente.Respuesta rgc = c.pedir("#GC#", Tramas.Tipo.ADMIN, Ops.TIMEOUT_ADMIN_MS);
        r.variante = Calibracion.variante(rgc.trama);
        r.detalle.append("#GC# -> ").append(rgc.describir()).append('\n');
        if (r.variante == Calibracion.Variante.V362 || v46) {
            r.fechaCalibracion = rgc.valida() ? Calibracion.fechaDe(rgc.trama) : null;
            Cliente.Respuesta rgn = c.pedir("#GN#", Tramas.Tipo.ADMIN, Ops.TIMEOUT_ADMIN_MS);
            r.serieEquipo = rgn.valida() ? Calibracion.serieDe(rgn.trama) : null;
            r.detalle.append("#GN# -> ").append(rgn.describir()).append('\n');
        }
    }

    /** Coteja con el perfil firmado (confirma o contradice; caduca con caduca_con_v) y con lo aprendido. */
    private static void confirmar(Resultado r, PerfilesEquipo.Perfil firmado, PerfilesEquipo.Aprendido aprendido) {
        if (aprendido != null && !r.huella.isEmpty() && !aprendido.huella.equals(r.huella)) {
            r.perfilCaducado = true;
            r.detalle.append("\nPerfil aprendido caducado: antes ").append(aprendido.huella).append(" (")
                    .append(aprendido.fecha).append("), ahora ").append(r.huella).append('.');
        }
        if (firmado == null || firmado.soloSonda() || r.firmware == Protocolo.Firmware.DESCONOCIDO) {
            return;
        }
        if (firmado.esperado() == r.firmware) {
            r.detalle.append("\nConfirma el perfil de equipos.csv (").append(firmado.perfil).append(").");
            return;
        }
        if (r.version != null && !firmado.caducaConV.isEmpty() && firmado.caducaConV.equals(r.version.version)) {
            r.perfilCaducado = true;
            r.detalle.append("\nPerfil de equipos.csv (").append(firmado.perfil).append(") caducado: el equipo "
                    + "responde #V# con ").append(r.version.version).append('.');
            return;
        }
        r.contradicePerfil = true;
        r.protocolo = null;
        r.detalle.append('\n').append(CONTRADICE).append(" (").append(firmado.perfil).append(", responde como ")
                .append(r.firmware.texto).append("): no se opera. Avise a Diego.");
    }
}
