package com.dpi.retrov36;

import java.io.IOException;

/**
 * RF-COV-18/19 (Cov364, H-1 ALTO de la revision arquitecto-iot a Cov_3.6.3_calibrar): lectura, escritura
 * y devolucion de la fecha de calibracion (#GC#/#SC), sacado de {@link FlujoCalibracion#aceptar} y
 * {@link FlujoCalibracion#rechazar} (rules/modularidad.md: 500 lineas por fichero, 100 por funcion).
 *
 * Antes de este arreglo, si #GC# no respondia (o respondia algo que no era una fecha ni NONE) ANTES de
 * enviar #SC, {@code aceptar()} trataba ese fallo de lectura igual que un NONE legitimo (linea "antes ==
 * null ? Calibracion.NONE : antes", FlujoCalibracion.java:1578 en ed7e604) y enviaba #SC de todas formas.
 * Si luego se rechazaba esa acta, {@code rechazar()} devolvia "#SC,NONE#" — borrando una fecha que el
 * equipo SI tenia, solo porque la app nunca llego a leerla. RF-COV-18 exige lo contrario: sin fecha
 * anterior leida (una fecha o NONE), no se envia #SC.
 */
final class FechaCalibracion {

    private FechaCalibracion() { }

    /** Resultado de {@link #grabar}: si quedo bien, si se llego a intentar un #SC (para encendidoReciente
     *  y scEnUltimoAceptar) y, si no quedo bien, el motivo (sin cerrar el acta: "escrito sin aceptar"). */
    static final class Resultado {
        final boolean ok;
        final boolean escribioSc;
        final String motivo;

        private Resultado(boolean ok, boolean escribioSc, String motivo) {
            this.ok = ok;
            this.escribioSc = escribioSc;
            this.motivo = motivo;
        }
    }

    /**
     * RF-COV-18: lee #GC# antes de grabar `hoy`. Sin una fecha anterior leida (una fecha valida o NONE),
     * NO se envia #SC: el codigo queda "escrito sin aceptar" (el acta no se cierra) y el motivo es
     * literal, para reintentar. Con fecha anterior leida, se anota ANTES de #SC (RF-COV-19 la necesita
     * para poder devolverla) y solo se escribe si hoy es distinta.
     */
    static Resultado grabar(Ops ops, Acta acta, String hoy) throws IOException, InterruptedException {
        Cliente.Respuesta g0 = ops.pedir("#GC#");
        String antes = g0.valida() ? Calibracion.fechaDe(g0.trama) : null;
        if (antes == null) {
            acta.dato("#GC# antes de #SC", g0.describir());
            return new Resultado(false, false, "no se pudo leer la fecha del equipo (#GC# -> " + g0.describir()
                    + "); reintente. El código queda escrito sin aceptar.");
        }
        if (hoy.equals(antes)) {
            acta.dato("#SC", "no se reescribe: el equipo ya tiene la fecha de hoy (" + g0.describir() + ")");
            return new Resultado(true, false, null);
        }
        // La fecha anterior queda anotada ANTES de enviar #SC (RF-COV-19: rechazar() la necesita para
        // devolverla, y nunca puede devolver una que no se leyo).
        acta.dato("#SC emitido, fecha anterior", antes);
        Cliente.Respuesta r = ops.escribir("#SC," + hoy + "#");
        Cliente.Respuesta g = ops.pedir("#GC#");
        String leida = g.valida() ? Calibracion.fechaDe(g.trama) : null;
        if (!Tramas.esOk(r.trama) || !hoy.equals(leida)) {
            acta.dato("#SC", r.describir());
            acta.dato("#GC#", g.describir());
            return new Resultado(false, true, "#SC no quedó grabada (#SC -> " + r.describir() + ", #GC# -> "
                    + g.describir() + "): el acta NO se cierra. Reintente.");
        }
        return new Resultado(true, true, null);
    }

    /**
     * RF-COV-19: si esta acta llegó a emitir #SC (dato "#SC emitido, fecha anterior" anotado por
     * {@link #grabar}), devuelve exactamente esa fecha con #SC,<fecha>#) (o #SC,NONE# si lo leído fue
     * NONE) y relee #GC#. Si no hay fecha anterior leída, no envía nada (nunca borra una fecha que no se
     * leyó) y devuelve "".
     */
    static String devolver(Ops ops, Acta acta) throws IOException, InterruptedException {
        String antesSC = acta.dato("#SC emitido, fecha anterior");
        if (antesSC == null) {
            return "";
        }
        Cliente.Respuesta rSc = ops.escribir("#SC," + antesSC + "#");
        Cliente.Respuesta gSc = ops.pedir("#GC#");
        String leidaSc = gSc.valida() ? Calibracion.fechaDe(gSc.trama) : null;
        boolean okSc = Tramas.esOk(rSc.trama) && antesSC.equals(leidaSc);
        acta.dato("#SC al rechazar", "#SC," + antesSC + "# -> " + rSc.describir() + "; #GC# -> "
                + gSc.describir() + (okSc ? " (fecha anterior restaurada)" : " (NO SE PUDO RESTAURAR LA FECHA)"));
        return "fecha #SC devuelta a " + antesSC + (okSc ? "; " : " SIN VERIFICAR; ");
    }
}
