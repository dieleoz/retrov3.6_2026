package com.dpi.retrov36;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * RF-COV-17 — el "un solo botón" de la app de calibrar "RTV Calibra" (BuildConfig.CORTO). Orquesta las
 * acciones PUBLICAS de {@link FlujoCalibracion} (calibrar, persistencia, aceptar) en el orden de
 * {@link FlujoCalibracion#ORDEN_SESION}, sin preguntar acta a acta: si la re-medida es conforme, la acepta
 * sola, para poder seguir con la siguiente.
 *
 * MODULARIDAD (21-sep-2026, rules/modularidad.md: fichero 500 líneas, función 60/100): sacado de
 * {@code FlujoCalibracion.calibrarAutomatico}/{@code calibrarAutomaticoInterno}/{@code persistirYAceptarAuto}
 * (que ya rondaban las 2100 líneas del fichero), como su propio commit y con la suite en verde, salida
 * idéntica — sin ningún cambio de comportamiento: es un traslado, no un arreglo. El acta, el canal y los
 * campos de sesión (sesionTodo, encendidoReciente, scEnUltimoAceptar...) los sigue guardando
 * {@link FlujoCalibracion}: esta clase solo los toca a través de los métodos de paquete que esa clase
 * expone para esto (operador, iniciarSesionAutomatica/terminarSesionAutomatica, marcarAceptadoReciente,
 * aceptadoVigente, fila, descartarActaVacia).
 */
final class CalibracionAutomatica {

    /** RF-COV-17: nota fija de la conformidad en el camino automático (la app de calibrar no pide nota). */
    private static final String NOTA_AUTOMATICA = "Calibración automática (RTV Calibra, un solo botón, RF-COV-17)";

    private final FlujoCalibracion flujo;

    CalibracionAutomatica(FlujoCalibracion flujo) {
        this.flujo = flujo;
    }

    String calibrarAutomatico(String nombre) throws IOException, InterruptedException {
        flujo.iniciarSesionAutomatica();
        try {
            return calibrarAutomaticoInterno(nombre);
        } finally {
            flujo.terminarSesionAutomatica();
        }
    }

    private String calibrarAutomaticoInterno(String nombre) throws IOException, InterruptedException {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Escriba su nombre antes de pulsar Calibrar.";
        }
        String pv = flujo.motivoPrevias();
        if (pv != null) {
            return "No se calibra: " + pv;
        }
        String vacia = flujo.descartarActaVacia("al retomar");
        if (!vacia.isEmpty()) {
            flujo.operador().progreso(vacia.trim());
        }
        StringBuilder ok = new StringBuilder();
        StringBuilder no = new StringBuilder();
        if (flujo.acta() != null) {
            Acta acta = flujo.acta();
            if (acta.rechazoPendiente() != null) {
                return "Hay un rechazo pendiente (" + acta.rechazoPendiente() + "): vuelva a pulsar Calibrar para "
                        + "reintentar, o resuélvalo con el PIN de administrador. No se calibra.";
            }
            // Termina primero lo que hubiera a medias de una sesión anterior (igual que "Calibrar todo").
            char k0 = acta.codigos().isEmpty() ? acta.escribiendo() : acta.codigos().get(0).k;
            String r = flujo.calibrar(new HashSet<Character>(), "", "");
            if (flujo.acta() != null && flujo.acta().motivoNoAceptable(false) == null) {
                String a = persistirYAceptarAuto(k0);
                if (a == null) {
                    ok.append(Fabrica.nombre(k0)).append("; ");
                } else {
                    no.append(Fabrica.nombre(k0)).append(": ").append(a).append("; ");
                }
            } else if (flujo.acta() != null) {
                no.append(Fabrica.nombre(k0)).append(": ").append(r).append("; ");
            }
        }
        Set<Character> aceptados = new HashSet<>();
        for (char k : FlujoCalibracion.ORDEN_SESION) {
            if (flujo.aceptadoVigente(k)) {
                aceptados.add(k);
            }
        }
        for (char k : FlujoCalibracion.ORDEN_SESION) {
            if (aceptados.contains(k)) {
                continue;
            }
            TablaCalibracion.Fila f = flujo.fila(k);
            FlujoCalibracion.Plan p = flujo.plan(k, null, true);
            if (!p.escribible()) {
                no.append(Fabrica.nombre(k)).append(": ").append(p.motivoNo).append("; ");
                continue;
            }
            if (f.requiereAceptado != 0 && !aceptados.contains(f.requiereAceptado)) {
                no.append(Fabrica.nombre(k)).append(": depende de que quede aceptado el código ")
                        .append(Fabrica.nombre(f.requiereAceptado)).append("; ");
                continue;
            }
            flujo.operador().progreso("Calibrando " + Fabrica.nombre(k) + "...");
            Set<Character> uno = new HashSet<>();
            uno.add(k);
            String r = flujo.calibrar(uno, nombre, NOTA_AUTOMATICA);
            if (flujo.acta() == null || flujo.acta().motivoNoAceptable(false) != null) {
                no.append(Fabrica.nombre(k)).append(": ").append(r).append("; ");
                continue;
            }
            String a = persistirYAceptarAuto(k);
            if (a != null) {
                no.append(Fabrica.nombre(k)).append(": ").append(a).append("; ");
                continue;
            }
            aceptados.add(k);
            ok.append(Fabrica.nombre(k)).append("; ");
        }
        if (ok.length() == 0 && no.length() == 0) {
            return "Nada que calibrar: todos los códigos ya tienen acta ACEPTADA vigente.";
        }
        return "Calibrado: " + (ok.length() == 0 ? "ninguno" : ok.toString().trim()) + ". No calibrado: "
                + (no.length() == 0 ? "ninguno" : no.toString().trim());
    }

    /**
     * Como {@code persistirConfirmarYAceptar} de FlujoCalibracion, pero SIN preguntar "¿Acepta esta acta?"
     * (RF-COV-17: el camino automático no para a confirmar cada acta una por una; si la re-medida es
     * conforme, se acepta sola).
     */
    private String persistirYAceptarAuto(char k) throws IOException, InterruptedException {
        String p = flujo.persistencia();
        if (!p.startsWith("Persistencia OK")) {
            return p;
        }
        String a = flujo.aceptar();
        if (a.startsWith("Acta ACEPTADA")) {
            flujo.marcarAceptadoReciente(k);
            return null;
        }
        return a;
    }
}
