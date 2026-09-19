package com.dpi.retrov36;

/**
 * Las 12 ecuaciones de fabrica, copiadas de
 * 01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c:3-42,
 * y la correspondencia codigo -> color (ecuacionesColoresApp, :137-193).
 *
 * "OTROS PAPELES" de la pantalla usa las intensas (1-6); "PAPEL TIPO I" las
 * opacas (7, 8, a-d). Los patrones P1-P31 (IV, IX, XI) calibran las intensas.
 */
public final class Fabrica {

    private Fabrica() { }

    /** Los 12 codigos de medida, en el orden del contrato. */
    public static final char[] CODIGOS = {'1', '2', '3', '4', '5', '6', '7', '8', 'a', 'b', 'c', 'd'};

    public static int indice(char k) {
        for (int i = 0; i < CODIGOS.length; i++) {
            if (CODIGOS[i] == k) {
                return i;
            }
        }
        return -1;
    }

    public static boolean esCodigo(char k) {
        return indice(k) >= 0;
    }

    /** Color en minusculas, tal como lo usa el CSV de patrones. */
    public static String color(char k) {
        switch (k) {
            case '1': case '7': return "blanco";
            case '2': case '8': return "amarillo";
            case '3': case 'a': return "verde";
            case '4': case 'b': return "rojo";
            case '5': case 'c': return "azul";
            case '6': case 'd': return "naranja";
            default: return "?";
        }
    }

    public static boolean esIntensa(char k) {
        return k >= '1' && k <= '6';
    }

    public static String nombre(char k) {
        return color(k) + (esIntensa(k) ? " intenso" : " opaco");
    }

    /** Codigo intenso de un color del CSV, o 0 si no hay. */
    public static char codigoIntenso(String color) {
        for (char k : CODIGOS) {
            if (esIntensa(k) && color(k).equalsIgnoreCase(color.trim())) {
                return k;
            }
        }
        return 0;
    }

    public static Ecuacion ecuacion(char k) {
        switch (k) {
            case '1': // blancoIntenso, :6-8
                return new Ecuacion(0, -0.000086541, 0.619668128, -303);
            case '2': // amarilloIntenso, :3-5
                return new Ecuacion(-0.000000073, 0.000282508, 0.124075350, -130);
            case '3': // verdeIntenso, :18-20
            case 'a': // verdeOpaco, :40-42 (identica)
                return new Ecuacion(0.000000163, -0.000756695, 1.213142724, -442);
            case '4': // rojoIntenso, :12-14
                return new Ecuacion(0.000000147, -0.000668624, 1.082394050, -393);
            case '5': // azulIntenso, :15-17
            case 'c': // azulOpaco, :37-39 (identica)
                return new Ecuacion(0.000000026, -0.000018402, 0.102152670, -49);
            case '6': // naranjaIntenso, :9-11
            case 'd': // naranjaOpaco, :31-33 (identica)
                return new Ecuacion(0, 0.000090731, 0.001064329, -21);
            case '7': // blancoOpaco, :28-30
                return new Ecuacion(0, 0.000087404, 0.013617682, -27);
            case '8': // amarilloOpaco, :25-27
                return new Ecuacion(0.000000086, -0.000299026, 0.446572329, -161);
            case 'b': // rojoOpaco, :34-36
                return new Ecuacion(0, 0.000113098, -0.076130418, 10);
            default:
                throw new IllegalArgumentException("codigo sin ecuacion: " + k);
        }
    }

    /**
     * Significado del byte que el firmware usa como codigo de la pantalla STONE
     * (bufferPantalla[8], ecuacionesCalibracion.c:73-130). Referencia: el
     * proyecto STONE archivado (KeyCodeReturn): OTROS PAPELES = 0x01-0x06,
     * TIPO I = 0x07-0x0D. La pantalla real puede no coincidir: para eso se mapea.
     */
    public static String describirCodigoStone(int b) {
        if (b >= 0x01 && b <= 0x08) {
            char k = (char) ('0' + b);
            return String.format("0x%02X = %s (%s)", b, nombre(k), esIntensa(k) ? "OTROS PAPELES" : "PAPEL TIPO I");
        }
        if (b >= 0x0A && b <= 0x0D) {
            char k = (char) ('a' + (b - 0x0A));
            return String.format("0x%02X = %s (PAPEL TIPO I)", b, nombre(k));
        }
        if (b == 0x09) {
            return "0x09 = sin rama en el firmware (no aplica ecuacion)";
        }
        if (b == 0x0E) {
            return "0x0E = sin ecuacion (PRUEBA ADC)";
        }
        return String.format("0x%02X = fuera de 0x01-0x0E: no es un codigo de ecuacion", b);
    }
}
