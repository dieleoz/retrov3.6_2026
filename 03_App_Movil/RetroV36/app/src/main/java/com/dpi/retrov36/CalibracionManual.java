package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * RF-COV-22 (arquitecto-iot a Cov_3.6.4_calibrar, modularidad rules/modularidad.md: función 100 líneas de
 * techo): el cuerpo de lo que era {@code FlujoCalibracion.calibrarInterno} (110 líneas en un único
 * método), partido en {@link #validarYPreparar} y {@link #escribirYVerificar} — validar la selección y
 * abrir/preparar el acta, y después escribir cada código y resolver lo que quedaba a medias. Mismos
 * pasos, mismo orden, mismos textos que antes del corte: sin cambio de comportamiento (la suite es la
 * prueba de salida idéntica).
 *
 * Como {@link CalibracionAutomatica}: vive en su propio fichero para no hacer crecer
 * {@code FlujoCalibracion.java}, llamando a los métodos de paquete que esa clase ya tenía (p. ej.
 * {@code operador()}, {@code plan}, {@code fila}) o que se han abierto para esto (mismo paquete: sin
 * getters de relleno, solo quitar {@code private} donde hacía falta).
 */
final class CalibracionManual {

    private final FlujoCalibracion flujo;

    CalibracionManual(FlujoCalibracion flujo) {
        this.flujo = flujo;
    }

    String calibrarInterno(Set<Character> seleccion, String nombre, String nota) throws IOException, InterruptedException {
        Set<Character> sel = seleccion == null ? new HashSet<>() : new HashSet<>(seleccion);
        Map<Character, FlujoCalibracion.Plan> planes = new LinkedHashMap<>();
        String previo = validarYPreparar(sel, planes, nombre, nota);
        return previo != null ? previo : escribirYVerificar(planes, nombre, nota);
    }

    /**
     * Guardas de la calibracion, el plan de cada codigo seleccionado y, si hace falta, abre el acta.
     * Devuelve el texto de parada si algo no procede; null si se sigue a {@link #escribirYVerificar}.
     */
    private String validarYPreparar(Set<Character> sel, Map<Character, FlujoCalibracion.Plan> planes, String nombre,
            String nota) throws IOException, InterruptedException {
        String pv = flujo.motivoPrevias();
        if (pv != null) {
            return "No se calibra: " + pv;
        }
        Acta acta = flujo.acta();
        if (acta != null && acta.rechazoPendiente() != null) {
            // P14-06: con un rechazo pendiente no se continua la calibracion.
            return "Hay un rechazo pendiente (" + acta.rechazoPendiente() + "): vuelva a pulsar Rechazar, o ciérrela sin "
                    + "restaurar, firmándola con el PIN de administrador. No se calibra.";
        }
        for (char k : sel) {
            Acta vig = flujo.aceptadaVigente(k);
            if (vig != null && flujo.serieAnuladaDe(vig, k) == null && (acta == null || acta.codigo(k) == null)) {
                return "El " + Fabrica.elCodigo(k, flujo.corto()) + " ya tiene un acta ACEPTADA vigente: no se vuelve a escribir.";
            }
        }
        for (char k : Fabrica.CODIGOS) {
            if (!sel.contains(k)) {
                continue;
            }
            FlujoCalibracion.Plan p = flujo.plan(k, null);
            if (!p.escribible()) {
                return "El " + Fabrica.elCodigo(k, flujo.corto()) + " no se escribe: " + p.motivoNo;
            }
            String mc = flujo.motivoCodigo(k);
            if (mc != null) {
                return mc;
            }
            planes.put(k, p);
        }
        for (FlujoCalibracion.Plan p : planes.values()) {
            if (p.fila.solo && (planes.size() > 1 || (acta != null && !acta.codigos().isEmpty() && acta.codigo(p.k) == null))) {
                return Fabrica.elCodigoCap(p.k, flujo.corto()) + " va solo, en un acta propia (P12 §6.3): desmarque los "
                        + "demás o acabe el acta en curso.";  // H-3 (Cov364, RF-COV-17): nunca el número en corto.
            }
        }
        if (acta != null) {
            for (Acta.Codigo c : acta.codigos()) {
                TablaCalibracion.Fila fc = flujo.fila(c.k);
                if (fc != null && fc.solo && !planes.isEmpty() && !planes.containsKey(c.k)) {
                    return "El acta en curso es del " + Fabrica.elCodigo(c.k, flujo.corto()) + ", que va solo: acéptela o "
                            + "recházela antes.";
                }
            }
        }
        if (!planes.isEmpty() && (nombre == null || nombre.trim().isEmpty() || nota == null || nota.trim().isEmpty())) {
            return "Escriba el nombre y la nota de la conformidad.";
        }
        String e = flujo.entrar();
        if (e != null) {
            return e;
        }
        if (flujo.acta() == null) {
            flujo.abrirActa();
        }
        flujo.completarDatos();
        return null;
    }

    /** Conformidad, T-C41, batería, lo que el acta tenía a medias y la escritura de cada plan. */
    private String escribirYVerificar(Map<Character, FlujoCalibracion.Plan> planes, String nombre, String nota)
            throws IOException, InterruptedException {
        Acta acta = flujo.acta();
        String conformidad = "";
        if (!planes.isEmpty()) {
            conformidad = nombre.trim() + ". " + nota.trim();
            for (FlujoCalibracion.Plan p : planes.values()) {
                // QA-3613-08 / RF-COV-20 (H-2): dispensa y quien la decidio; en corto, siempre RF-COV-12, nunca RF-CAL-18.
                acta.conformidad(nombre.trim() + ": código " + p.k + ". " + nota.trim() + (p.fila.dispensa.isEmpty() ? ""
                        : " | dispensa " + p.fila.dispensa + ": "
                                + (flujo.corto() ? "RF-COV-12 (VERIF-5-10)" : p.fila.origen)));
            }
        }
        // T-C41: los heredados, antes de tocar nada.
        flujo.escribiendoAhora.clear();
        flujo.escribiendoAhora.addAll(planes.keySet());
        String t41 = flujo.heredados();
        if (t41 != null) {
            return t41;
        }
        flujo.operador().progreso("Batería antes de la calibración...");
        Bateria.Lectura b = flujo.bateria();
        if (b.bloqueaEscrituras) {
            return b.texto + " No se escribe nada.";
        }
        if (acta.escribiendo() != 0) {
            String r = flujo.resolverCorte();
            if (r != null) {
                return r;
            }
        }
        // Lo que el acta tiene a medias: restauraciones sin verificar, dos NO CONFORME, re-medidas.
        for (Acta.Codigo c : new ArrayList<>(acta.codigos())) {
            if (c.resuelto()) {
                continue;
            }
            String r = c.restauracionFallida != null ? flujo.restaurarCodigo(c.k, "restauración pendiente")
                    : c.debeRestaurarse() ? flujo.restaurarCodigo(c.k, flujo.corto() ? "re-medida fuera de ±10 %"
                            : "dos re-medidas NO CONFORME")
                    : flujo.remedida(c.k);
            if (r != null) {
                return r;
            }
        }
        for (FlujoCalibracion.Plan p : planes.values()) {
            String r = flujo.escribir(p, FlujoCalibracion.conformidad(p, "Conformidad de " + conformidad));
            if (r != null) {
                return r;
            }
            r = flujo.remedida(p.k);
            if (r != null) {
                return r;
            }
        }
        return acta.motivoNoAceptable(false) == null
                ? "Códigos escritos y verificados. Ahora: persistencia (apagar y encender) y después aceptar."
                : "Sin cambios pendientes: " + acta.motivoNoAceptable(false);
    }
}
