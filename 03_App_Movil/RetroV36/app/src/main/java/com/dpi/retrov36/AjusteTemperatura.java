package com.dpi.retrov36;

import java.util.Locale;

/**
 * RTV 1.0.0-rc3: SITIO PREPARADO para el ajuste del termino de temperatura de la V4.6.
 *
 * <b>El ajuste NO se hace todavia, y esta clase no lo hace.</b> Aqui solo vive la guarda que decide si los
 * datos permiten ajustarlo, porque esa decision hay que tomarla antes de escribir nada en el equipo y no
 * despues. La rc3 trae la columna de temperatura del diario, que es lo que permite acumular el dato; el
 * ajuste entra cuando haya SPEC cerrada y el umbral medido.
 *
 * <b>El modelo</b> (PROTOCOLO-V4.6-BORRADOR.md, RF-FW-B10/B11): {@code V_corregida = F(TO)*V + S(TO)}, con
 * la temperatura OPTICA, aditivo sobre V e igual para todos los colores. De fabrica F = 1 y S son las dos
 * rectas del equipo de Nordeste.
 *
 * <b>Por que hace falta la guarda.</b> F y S entran multiplicando y sumando la misma V. Si todas las medidas
 * se tomaron practicamente a la misma TO, el sistema es degenerado: infinitas parejas (F, S) explican los
 * mismos datos igual de bien, y una regresion devuelve una cualquiera con toda la apariencia de un buen
 * ajuste. No es que el ajuste salga "con mas error": es que el numero que sale no significa nada. Por eso
 * la respuesta correcta no es ajustar con menos confianza, sino <b>negarse a ajustar y dejar los de
 * fabrica, diciendolo en el acta</b>.
 *
 * <b>El umbral no esta en este fichero a proposito.</b> {@link #RECORRIDO_TO_MINIMO} vale NaN, y con NaN
 * {@link #evaluar} siempre responde que no se ajusta, con el motivo. El numero lo esta midiendo quien tiene
 * los datos reales delante; ponerlo aqui "por poner" seria exactamente la clase de cifra sin respaldo que
 * este repositorio no admite. El dia que exista, se escribe aqui con su documento y su linea, y esta clase
 * empieza a dejar pasar el ajuste sin tocar nada mas.
 */
public final class AjusteTemperatura {

    private AjusteTemperatura() { }

    /**
     * Recorrido minimo de TO, en grados, para que F y S se puedan separar.
     *
     * <b>NaN a proposito: el numero no esta medido.</b> Mientras valga NaN, {@link #evaluar} nunca autoriza
     * el ajuste. No lo rellene sin el documento que lo sostenga.
     */
    public static final double RECORRIDO_TO_MINIMO = Double.NaN;

    /** Lo que la app hace con el termino de temperatura, y por que. */
    public static final class Decision {
        /** true solo si los datos permiten separar F de S. Hoy, nunca. */
        public final boolean ajustar;
        /** Recorrido de TO observado, en grados; NaN si no hay temperaturas. */
        public final double recorrido;
        /** Cuantas medidas traian temperatura. */
        public final int conTemperatura;
        /** Cuantas medidas NO traian temperatura. */
        public final int sinTemperatura;
        /** El texto que va al acta: dice que se hace y por que. */
        public final String motivo;

        Decision(boolean ajustar, double recorrido, int con, int sin, String motivo) {
            this.ajustar = ajustar;
            this.recorrido = recorrido;
            this.conTemperatura = con;
            this.sinTemperatura = sin;
            this.motivo = motivo;
        }
    }

    /** Lo que se escribe en el acta cuando no se ajusta: el equipo se queda con los de fabrica. */
    public static final String SE_QUEDA_FABRICA =
            "No se ajusta el término de temperatura: el equipo conserva los coeficientes de fábrica.";

    /**
     * Decide si con estas temperaturas se puede ajustar el termino. Java puro: no toca el equipo.
     *
     * @param temperaturasOpticas una por medida; null en las medidas que no traian temperatura (esas no se
     *                            inventan ni se rellenan con 0: se cuentan aparte y se dicen).
     */
    public static Decision evaluar(Double[] temperaturasOpticas) {
        int con = 0;
        int sin = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        if (temperaturasOpticas != null) {
            for (Double t : temperaturasOpticas) {
                if (t == null || t.isNaN() || t.isInfinite()) {
                    sin++;
                    continue;
                }
                con++;
                min = Math.min(min, t);
                max = Math.max(max, t);
            }
        }
        double recorrido = con == 0 ? Double.NaN : max - min;
        if (con == 0) {
            return new Decision(false, Double.NaN, con, sin, SE_QUEDA_FABRICA
                    + " Ninguna de las " + sin + " medidas trae temperatura, así que no hay con qué separar el "
                    + "factor de la suma.");
        }
        if (Double.isNaN(RECORRIDO_TO_MINIMO)) {
            return new Decision(false, recorrido, con, sin, String.format(Locale.US, "%s El recorrido de TO de "
                    + "estos datos es %.2f grados (%d medidas con temperatura, %d sin), pero el recorrido mínimo "
                    + "que permite separar F(TO) de S(TO) todavía NO ESTÁ MEDIDO: mientras no lo esté, la app no "
                    + "ajusta. Ajustar con un recorrido insuficiente daría coeficientes con apariencia de buenos "
                    + "y sin fundamento.", SE_QUEDA_FABRICA, recorrido, con, sin));
        }
        if (recorrido < RECORRIDO_TO_MINIMO) {
            return new Decision(false, recorrido, con, sin, String.format(Locale.US, "%s El recorrido de TO es "
                    + "%.2f grados y hacen falta al menos %.2f para separar F(TO) de S(TO).",
                    SE_QUEDA_FABRICA, recorrido, RECORRIDO_TO_MINIMO));
        }
        return new Decision(true, recorrido, con, sin, String.format(Locale.US,
                "Recorrido de TO de %.2f grados sobre %d medidas: suficiente para separar F(TO) de S(TO).",
                recorrido, con));
    }
}
