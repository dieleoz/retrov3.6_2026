package com.dpi.retrov36;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Construccion y lectura de tramas segun PROTOCOLO-V3.6.md. Java puro.
 *
 * Regla de la app: ninguna trama con '@' salvo la sonda literal de deteccion
 * de V4 (SONDA_V4). En el V4.1 una trama con '@' y sin LEERV deja el equipo
 * sin Bluetooth.
 */
public final class Tramas {

    private Tramas() { }

    /** Revision 1.1 del contrato (§4 bis, O-01): 96 bytes. La 1.0 decia 48. */
    public static final int MAX_TRAMA = 96;
    public static final String SONDA_V4 = "@LEERV,BLA,1@";

    public enum Tipo {
        /** "::n" sin terminador: completa tras un silencio. */
        MEDIDA,
        /** ":n:" (bateria, orden 9). */
        BATERIA,
        /** "#...#". */
        ADMIN,
        /** "@LEERV,...@" (V4). */
        LEERV
    }

    private static final Pattern P_MEDIDA = Pattern.compile("::(\\d+)");
    private static final Pattern P_BATERIA = Pattern.compile(":([^:]{1,8}):");
    private static final Pattern P_ADMIN = Pattern.compile("#([^#]+)#");
    /**
     * RF-APP-U05 (SPEC-App-Unica-V36-V46.md): solo "@LEERV,<entero>@". El eco de la propia sonda
     * ("@LEERV,BLA,1@") y "@LEERV,127,45@" no son respuesta.
     */
    private static final Pattern P_LEERV = Pattern.compile("@LEERV,-?\\d{1,6}@");

    /**
     * 'e' solo a un equipo identificado como V3.6. En SLV-002 (V3 2020 sin 'e')
     * el equipo dejo de responder tras 'e' (18-sep-2026; hipotesis sin confirmar).
     */
    public static boolean peticionPermitida(String p, boolean esV36) {
        if ("e".equals(p) && !esV36) {
            return false;
        }
        return peticionPermitida(p);
    }

    /** true si la peticion puede enviarse: sin '@', salvo la sonda de V4. */
    public static boolean peticionPermitida(String p) {
        return p.indexOf('@') < 0 || SONDA_V4.equals(p);
    }

    /**
     * Devuelve la trama completa contenida en lo recibido, o null.
     *
     * @param silencio true si ya ha pasado el silencio que da por terminada una
     *                 respuesta sin terminador ("::n").
     */
    public static String extraer(Tipo t, String rx, boolean silencio) {
        Matcher m;
        switch (t) {
            case MEDIDA:
                m = P_MEDIDA.matcher(rx);
                if (!m.find()) {
                    return null;
                }
                // Sin terminador: los digitos pueden seguir llegando.
                if (!silencio && m.end() == rx.length()) {
                    return null;
                }
                return m.group();
            case BATERIA:
                m = P_BATERIA.matcher(rx);
                return m.find() ? m.group() : null;
            case ADMIN:
                m = P_ADMIN.matcher(rx);
                return m.find() ? m.group() : null;
            case LEERV:
                m = P_LEERV.matcher(rx);
                return m.find() ? m.group() : null;
            default:
                return null;
        }
    }

    /** "::123" -> 123; null si no es una respuesta de medida. */
    public static Integer valorMedida(String trama) {
        if (trama == null) {
            return null;
        }
        Matcher m = P_MEDIDA.matcher(trama);
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Campos de "#A,B,C#" -> {"A","B","C"}; null si no es trama admin. */
    public static String[] campos(String trama) {
        if (trama == null) {
            return null;
        }
        Matcher m = P_ADMIN.matcher(trama);
        if (!m.find()) {
            return null;
        }
        return m.group(1).split(",", -1);
    }

    public static boolean esOk(String trama) {
        String[] c = campos(trama);
        return c != null && c.length == 1 && "OK".equals(c[0]);
    }

    /** Motivo de "#ERR,motivo#", o null si no es un error. */
    public static String motivoError(String trama) {
        String[] c = campos(trama);
        if (c == null || c.length < 1 || !"ERR".equals(c[0])) {
            return null;
        }
        return c.length > 1 ? c[1] : "";
    }

    public static final class InfoVersion {
        public final String version;
        public final String fecha;
        /** "CAL" o "DEF". */
        public final String marca;
        /**
         * Mascara de la revision 1.1 (O-03): bit i = codigo CODIGOS[i] ajustado,
         * bit 12 = temperatura. -1 si la trama no la trae (contrato 1.0).
         */
        public final int mascara;

        InfoVersion(String version, String fecha, String marca, int mascara) {
            this.version = version;
            this.fecha = fecha;
            this.marca = marca;
            this.mascara = mascara;
        }

        public boolean tieneMascara() {
            return mascara >= 0;
        }

        public boolean codigoAjustado(int indice) {
            return mascara >= 0 && ((mascara >> indice) & 1) != 0;
        }

        public boolean temperaturaAjustada() {
            return mascara >= 0 && ((mascara >> 12) & 1) != 0;
        }
    }

    /**
     * "#V,3.6,2026-09-20,CAL,0003#" (revision 1.1) o "#V,3.6,2026-09-20,CAL#"
     * (1.0). null si no cuadra con ninguna de las dos.
     */
    public static InfoVersion parsearVersion(String trama) {
        String[] c = campos(trama);
        if (c == null || (c.length != 4 && c.length != 5) || !"V".equals(c[0])) {
            return null;
        }
        int m = -1;
        if (c.length == 5) {
            try {
                m = Integer.parseInt(c[4].trim(), 16);
            } catch (NumberFormatException e) {
                return null;
            }
            if (m < 0 || m > 0xFFFF) {
                return null;
            }
        }
        return new InfoVersion(c[1], c[2], c[3], m);
    }

    /** "#E,k,R#" -> R; null si no cuadra o k no coincide. */
    public static Integer parsearE(String trama, char k) {
        String[] c = campos(trama);
        if (c == null || c.length != 3 || !"E".equals(c[0]) || !String.valueOf(k).equals(c[1])) {
            return null;
        }
        try {
            return Integer.parseInt(c[2].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Coeficiente en el formato de la revision 1.1 (O-02): %.8E, 9 cifras. */
    public static String coeficiente(double v) {
        return String.format(Locale.US, "%.8E", v);
    }

    /** "#G,k,c3,c2,c1,c0#" con %.8E (lo que el acta cita como curva certificada). */
    public static String tramaG(char k, Ecuacion e) {
        return "#G," + k + "," + coeficiente(e.c3) + "," + coeficiente(e.c2) + "," + coeficiente(e.c1) + ","
                + coeficiente(e.c0) + "#";
    }

    /** "#G,k,c3,c2,c1,c0#" -> ecuacion; null si no cuadra o k no coincide. */
    public static Ecuacion parsearG(String trama, char k) {
        String[] c = campos(trama);
        if (c == null || c.length != 6 || !"G".equals(c[0]) || !String.valueOf(k).equals(c[1])) {
            return null;
        }
        try {
            return new Ecuacion(num(c[2]), num(c[3]), num(c[4]), num(c[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "#GT,x2,x1,x0#" -> {x2,x1,x0}; null si no cuadra. */
    public static double[] parsearGT(String trama) {
        String[] c = campos(trama);
        if (c == null || c.length != 4 || !"GT".equals(c[0])) {
            return null;
        }
        try {
            return new double[]{num(c[1]), num(c[2]), num(c[3])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double num(String s) {
        double v = Double.parseDouble(s.trim());
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new NumberFormatException("no finito: " + s);
        }
        return v;
    }

    /** Limites de X_0 del factor de temperatura que la app acepta (condicion C4). */
    public static final double X0_MIN = 0.5;
    public static final double X0_MAX = 1.5;

    /**
     * "#ST,x2,x1,x0#". null si X_0 esta fuera de [0,5; 1,5] o algun valor no
     * es finito. La app no ofrece hoy escribir la temperatura; la trama existe
     * para que cualquier uso futuro pase por estos limites.
     */
    public static String tramaST(double x2, double x1, double x0) {
        if (Double.isNaN(x0) || x0 < X0_MIN || x0 > X0_MAX
                || Double.isNaN(x1) || Double.isInfinite(x1) || Double.isNaN(x2) || Double.isInfinite(x2)) {
            return null;
        }
        String t = "#ST," + coeficiente(x2) + "," + coeficiente(x1) + "," + coeficiente(x0) + "#";
        return t.length() <= MAX_TRAMA ? t : null;
    }

    /** Registro de la pantalla STONE: "#K,n,hex1;hex2;...#". */
    public static final class RegistroK {
        /** Tramas vistas desde el arranque, segun el firmware. */
        public final long total;
        /** De la mas antigua a la mas reciente, en bytes. */
        public final List<byte[]> tramas;

        RegistroK(long total, List<byte[]> tramas) {
            this.total = total;
            this.tramas = tramas;
        }
    }

    /**
     * Admite los bytes en hex seguidos ("A55A06") o separados por espacios.
     * Una trama hex ilegible hace fallar el conjunto (null): es preferible a
     * mostrar un codigo de boton inventado.
     */
    public static RegistroK parsearK(String trama) {
        String[] c = campos(trama);
        if (c == null || c.length < 2 || !"K".equals(c[0])) {
            return null;
        }
        long total;
        try {
            total = Long.parseLong(c[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        List<byte[]> lista = new ArrayList<>();
        if (c.length > 2) {
            // Por si el firmware separase algo con ',' dentro de la lista.
            StringBuilder sb = new StringBuilder(c[2]);
            for (int i = 3; i < c.length; i++) {
                sb.append(';').append(c[i]);
            }
            for (String h : sb.toString().split(";")) {
                if (h.trim().isEmpty()) {
                    continue;
                }
                try {
                    lista.add(Hex.parsear(h));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return new RegistroK(total, lista);
    }

    /** Posicion del codigo de boton en la trama STONE: bufferPantalla[8]. */
    public static final int POS_CODIGO_STONE = 8;

    /**
     * Numero con como mucho 'cifras' significativas, en la forma mas corta de
     * las dos que acepta el firmware (cientifica o decimal simple), sin ceros
     * sobrantes. Ej.: -8.6541E-05, 0.6196681, -303.
     */
    public static String numero(double v, int cifras) {
        if (v == 0) {
            return "0";
        }
        BigDecimal b = new BigDecimal(v).round(new MathContext(cifras)).stripTrailingZeros();
        String plano = b.toPlainString();
        String mant = String.format(Locale.US, "%." + (cifras - 1) + "E", b.doubleValue());
        int e = mant.indexOf('E');
        String m = mant.substring(0, e);
        String exp = mant.substring(e);
        if (m.indexOf('.') >= 0) {
            m = m.replaceAll("0+$", "");
            if (m.endsWith(".")) {
                m = m.substring(0, m.length() - 1);
            }
        }
        String cient = m + exp;
        return plano.length() <= cient.length() ? plano : cient;
    }

    public static final class TramaS {
        public final String texto;
        /** Los coeficientes tal como quedan tras formatear: lo que se escribe. */
        public final Ecuacion enviada;

        TramaS(String texto, Ecuacion enviada) {
            this.texto = texto;
            this.enviada = enviada;
        }
    }

    /**
     * "#S,k,c3,c2,c1,c0#" con 9 cifras (%.8E), revision 1.1. Ocupa como mucho
     * 5 + 4*15 + 3 + 1 = 69 bytes, dentro del limite de 96. null si no cabe.
     */
    public static TramaS tramaS(char k, Ecuacion e) {
        String a = coeficiente(e.c3);
        String b = coeficiente(e.c2);
        String c = coeficiente(e.c1);
        String d = coeficiente(e.c0);
        String t = "#S," + k + "," + a + "," + b + "," + c + "," + d + "#";
        if (t.length() > MAX_TRAMA) {
            return null;
        }
        return new TramaS(t, new Ecuacion(Double.parseDouble(a), Double.parseDouble(b),
                Double.parseDouble(c), Double.parseDouble(d)));
    }
}
