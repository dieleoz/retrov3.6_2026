package com.dpi.retrousuario.dominio;

/**
 * Une la detección ("#V#", {@link DetectorEquipo}) y la primera sonda ("#GN#"/"#GC#",
 * {@link Sonda362}) con la pausa de 150 ms entre dos tramas "#...#" que exige
 * {@code SPEC-V3.6.md:441-446} (RF-USR-16 :344) **una vez el equipo ya se identificó como V3.6** —
 * no sólo entre #GN# y #GC# (eso ya lo hacía {@link Sonda362}), también entre #V# y el primer #GN#.
 *
 * <p>Condición QA-3 sobre 32c785d: MainActivity llamaba a {@code DetectorEquipo.detectar} y, en un
 * hilo aparte, a {@code Sonda362.sondear} sin ninguna pausa entre los dos: la trama #GN# podía salir
 * inmediatamente después de la respuesta a #V#, sin los 150 ms.</p>
 */
public final class DeteccionYSonda {

    public static final class Resultado {
        private final DetectorEquipo.ResultadoDeteccion deteccion;
        private final Sonda362.ResultadoSonda sonda;

        private Resultado(DetectorEquipo.ResultadoDeteccion deteccion, Sonda362.ResultadoSonda sonda) {
            this.deteccion = deteccion;
            this.sonda = sonda;
        }

        public DetectorEquipo.ResultadoDeteccion deteccion() {
            return deteccion;
        }

        /** null si {@link #deteccion()} es {@link DetectorEquipo.Resultado#NO_COMPATIBLE} (nunca se llegó a sondear). */
        public Sonda362.ResultadoSonda sonda() {
            return sonda;
        }
    }

    private DeteccionYSonda() {
    }

    public static Resultado ejecutar(Canal canal, ParametrosRitmo params) {
        return ejecutar(canal, params, DeteccionYSonda::pausaReal);
    }

    /** Visible para pruebas (evita un {@code Thread.sleep} real de 150 ms en la JVM, mismo patrón que {@link Sonda362}). */
    static Resultado ejecutar(Canal canal, ParametrosRitmo params, java.util.function.LongConsumer pausa) {
        DetectorEquipo.ResultadoDeteccion d = DetectorEquipo.detectar(canal, params);
        if (d.resultado() == DetectorEquipo.Resultado.NO_COMPATIBLE) {
            return new Resultado(d, null);
        }
        pausa.accept(150L); // RF-USR-16 :344 / SPEC-V3.6.md:441-446: 150 ms entre #V# y el primer #GN#.
        Sonda362.ResultadoSonda s = Sonda362.sondear(canal, params, pausa);
        return new Resultado(d, s);
    }

    private static void pausaReal(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
