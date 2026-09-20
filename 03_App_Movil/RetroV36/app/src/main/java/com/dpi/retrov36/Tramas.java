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
    /**
     * Sonda de deteccion del V4 (RF-APP-U04, C-U01 cerrada por la revision P2 §4 y el encargo de la RTV 1.0):
     * "@LEERV,BLA,2@", otros papeles. Lee el 'error' oculto del V4.1 pero no lo escribe
     * (V4.1:Aplicacion.c:297-298 solo en la rama de tipo 1). Deja la pantalla en BLANCO tipo 2 (H-06).
     */
    public static final String SONDA_V4 = "@LEERV,BLA,2@";
    /**
     * Espera de la sonda y de toda @LEERV (RF-APP-U04): el V4.1 espera 1 s antes de medir.
     *
     * RTV 1.0.0-rc6: medido en campo el 19-sep con SLV-028 (V4 original), "@LEERV,BLA,2@" tarda 3,1-3,3 s de
     * punta a punta. Con los 5000 ms de la rc5 el margen era de 1,7 s (un 55 %): basta para una lectura suelta,
     * pero en una tanda larga una sola peticion que se pase del plazo se cuenta como timeout y corta la tanda.
     * Se sube a 8000 ms, que deja mas del doble de margen sobre lo medido. Subirlo NO cuesta tiempo cuando el
     * equipo responde: el plazo se cierra con la trama, no esperando (Cliente.pedir:141-153). Solo se paga con
     * un equipo mudo, y ahi ya no hay tanda que salvar.
     */
    public static final long TIMEOUT_LEERV_MS = 8000;
    /** Los 6 colores de @LEERV, en el orden de las claves 1-6 (PROTOCOLO-V4.6 §4.2). */
    public static final String[] COLORES_V4 = {"BLA", "AMA", "VER", "ROJ", "AZU", "NAR"};
    private static final Pattern P_LEERV_PETICION = Pattern.compile("@LEERV,(BLA|AMA|VER|ROJ|AZU|NAR),([12])@");
    private static final Pattern P_LEERV_VALOR = Pattern.compile("@LEERV,(-?\\d{1,6})@");

    /** true si p es, byte a byte, una de las 12 peticiones @LEERV validas (T-U10). */
    public static boolean esLeervValida(String p) {
        return p != null && P_LEERV_PETICION.matcher(p).matches();
    }

    /** "@LEERV,<COLOR>,<tipo>@" para la clave k (PROTOCOLO-V4.6 §4.2: 1-6 tipo 2, 7, 8, a-d tipo 1). */
    public static String tramaLeerv(char k) {
        int i = Fabrica.indice(k);
        if (i < 0) {
            return null;
        }
        return "@LEERV," + COLORES_V4[i % 6] + "," + (i < 6 ? 2 : 1) + "@";
    }

    /** Clave de una peticion @LEERV valida (inversa de tramaLeerv); 0 si no lo es. */
    public static char claveDeLeerv(String p) {
        Matcher m = p == null ? null : P_LEERV_PETICION.matcher(p);
        if (m == null || !m.matches()) {
            return 0;
        }
        int c = java.util.Arrays.asList(COLORES_V4).indexOf(m.group(1));
        return Fabrica.CODIGOS["2".equals(m.group(2)) ? c : 6 + c];
    }

    /** true si la clave k mide con tipo 1 en @LEERV. */
    public static boolean esTipo1(char k) {
        return Fabrica.indice(k) >= 6;
    }

    /** "@LEERV,127@" -> 127 (RF-APP-U05: solo "@LEERV,<entero>@"); null si no cuadra. */
    public static Integer valorLeerv(String trama) {
        if (trama == null) {
            return null;
        }
        Matcher m = P_LEERV_VALOR.matcher(trama);
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * "#X,k,x#" o "#X,k,x,TO,a#" (PROTOCOLO-V4.6 §4.4) -> x; null si no cuadra o k no coincide.
     *
     * RTV 1.0.0-rc3: se admiten LAS DOS formas a proposito. El candidato del firmware emite tres campos
     * (V4.6:Calibracion.c:774-781, verificado) y el borrador ya especifica cinco, con la temperatura optica y
     * el ajuste aplicado dentro de la propia respuesta (RF-FW-B13, PROTOCOLO-V4.6-BORRADOR.md:215,251). La app
     * tiene que hablar con los dos: con el de hoy, para medir el banco esta noche, y con el de manana sin
     * tener que reprogramarla.
     */
    public static Double parsearX(String trama, char k) {
        XCompleta x = parsearXCompleta(trama, k);
        return x == null ? null : x.x;
    }

    /** Respuesta de "#X": la x y, si el firmware la trae, la temperatura optica y el ajuste aplicado. */
    public static final class XCompleta {
        public final double x;
        /** Temperatura optica del propio disparo; null si el firmware no la manda (forma de 3 campos). */
        public final Double temperaturaOptica;
        /** Ajuste por temperatura ya aplicado, en cuentas (a = x_corregida - x_cruda); null si no viene. */
        public final Double ajuste;

        XCompleta(double x, Double to, Double a) {
            this.x = x;
            this.temperaturaOptica = to;
            this.ajuste = a;
        }

        /** true si esta respuesta ya trae la temperatura: entonces no hace falta pedir "#T#" por el disparo. */
        public boolean traeTemperatura() {
            return temperaturaOptica != null;
        }
    }

    public static XCompleta parsearXCompleta(String trama, char k) {
        String[] c = campos(trama);
        if (c == null || (c.length != 3 && c.length != 5) || !"X".equals(c[0])
                || !String.valueOf(k).equals(c[1])) {
            return null;
        }
        try {
            double x = num(c[2]);
            if (c.length == 3) {
                return new XCompleta(x, null, null);
            }
            // RF-FW-B13: el cuarto campo es TO y el quinto el ajuste aplicado, en cuentas.
            return new XCompleta(x, numONull(c[3]), numONull(c[4]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * RTV 1.0.0-rc3: temperaturas de "#T,<TO>,<TC>#" (V4.6). TO es la optica y TC la de circuito.
     *
     * Cada una puede venir ausente o ilegible por separado, y entonces vale null CON SU MOTIVO: no se
     * sustituye por 0. Un 0 en esta columna es una cifra que alguien ajustaria manana como si fuese una
     * medida; un hueco con motivo es un dato.
     */
    public static final class Temperatura {
        /** Temperatura optica, en las unidades del firmware; null si no vino o no se pudo leer. */
        public final Double optica;
        /** Temperatura de circuito; null si no vino o no se pudo leer. */
        public final Double circuito;
        /** "" si las dos se leyeron; si no, por que falta la que falta. */
        public final String motivo;
        /** Estado del sensor optico: "OK", "DESC" (RF-FW-B13) o "SIN". Tercera columna del diario. */
        public final String estado;

        Temperatura(Double optica, Double circuito, String motivo, String estado) {
            this.optica = optica;
            this.circuito = circuito;
            this.motivo = motivo == null ? "" : motivo;
            this.estado = estado;
        }

        public boolean hayOptica() {
            return optica != null;
        }
    }

    /** Motivo cuando el valor viene pero no es un numero finito. */
    public static final String T_ILEGIBLE = "el firmware devolvió un valor que no es un número";
    /** Motivo cuando el campo no viene en la respuesta. */
    public static final String T_AUSENTE = "la respuesta #T no trae ese campo";
    /** Estado "DESC" de RF-FW-B13: lectura cruda >= 70 C, que el firmware fuerza a TO = 0. */
    public static final String T_DESC = "DESC";
    /**
     * Motivo cuando el firmware dice DESC. El 0 que manda NO es una temperatura: es el centinela de
     * V4.6:Temp_Optica.c:63-66. Por eso la columna va vacia y no con ese 0.
     */
    public static final String T_MOTIVO_DESC = "sensor óptico desconectado o lectura cruda >= 70 C (estado DESC): "
            + "el firmware fuerza TO = 0 y ese 0 no es una temperatura";
    /**
     * En el candidato del firmware la temperatura de circuito vale SIEMPRE 0: se emite ap.fTempCircuit
     * (V4.6:Calibracion.c:986) y el unico sitio que la asigna (V4.6:Aplicacion.c:216) esta dentro de
     * ST_TEMPCIRC_AP, un estado al que no se llega desde ningun cambiarEstado(). Verificado dos veces.
     */
    public static final String T_CIRCUITO_ESTRUCTURAL =
            "en el candidato del firmware la temperatura de circuito es siempre 0 (estado ST_TEMPCIRC_AP "
                    + "inalcanzable, V4.6:Aplicacion.c:205-216): no se ajusta con ella";

    /**
     * "#T,&lt;Tcirc&gt;,&lt;TO&gt;#" o "#T,&lt;Tcirc&gt;,&lt;TO&gt;,&lt;estado&gt;#" -> temperaturas.
     * null si no es una respuesta #T en absoluto.
     *
     * ORDEN: la de CIRCUITO va PRIMERO y la OPTICA SEGUNDA. Se comprueba en el fuente del firmware
     * (V4.6:Calibracion.c:986 enviarNumero(ap.fTempCircuit), :988 enviarNumero(opt.fTempOpt)) y en el
     * contrato (PROTOCOLO-V4.6-BORRADOR.md:215,251). La rc3 lo tuvo invertido un rato: con la V4.6 real eso
     * habria metido el 0 estructural del circuito en la columna optica, que es la que se ajusta. Una prueba
     * con el simulador simetrico ("#T,25.0,25.0#") no lo habria visto nunca; por eso el simulador ya no es
     * simetrico.
     *
     * El tercer campo es de RF-FW-B13 y el candidato todavia no lo manda: se admiten las dos formas.
     */
    public static Temperatura parsearT(String trama) {
        String[] c = campos(trama);
        if (c == null || c.length < 3 || c.length > 4 || !"T".equals(c[0])) {
            return null;
        }
        Double tc = numONull(c[1]);
        Double to = numONull(c[2]);
        String estado = c.length == 4 ? c[3].trim() : "";
        StringBuilder m = new StringBuilder();
        String est = to == null ? "SIN" : "OK";
        if (T_DESC.equalsIgnoreCase(estado)) {
            // El 0 del centinela no entra en la columna: vacia, con motivo, y marcada DESC. Sin esta marca un
            // sensor muerto se lee igual que un dia fresco, y un ajuste hecho asi saldria plano.
            to = null;
            est = T_DESC;
            m.append("TO: ").append(T_MOTIVO_DESC);
        } else if (to == null) {
            m.append("TO: ").append(T_ILEGIBLE);
        }
        if (tc == null) {
            m.append(m.length() > 0 ? "; " : "").append("TC: ").append(T_ILEGIBLE);
        } else if (tc == 0.0) {
            m.append(m.length() > 0 ? "; " : "").append("TC: ").append(T_CIRCUITO_ESTRUCTURAL);
        }
        return new Temperatura(to, tc, m.toString(), est);
    }

    /** El numero del campo, o null si no es finito o no se puede leer (nunca 0 por defecto). */
    private static Double numONull(String s) {
        try {
            return num(s);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    /** "#GB,n#" (PROTOCOLO-V4.6 §4.4) -> n; null si no cuadra. La unidad esta por fijar en la V4.6. */
    public static Integer parsearGB(String trama) {
        String[] c = campos(trama);
        if (c == null || c.length != 2 || !"GB".equals(c[0])) {
            return null;
        }
        try {
            return Integer.parseInt(c[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
