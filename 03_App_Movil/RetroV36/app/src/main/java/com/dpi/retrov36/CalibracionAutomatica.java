package com.dpi.retrov36;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * RF-COV-17 — el "un solo botón" de la app de calibrar "RTV Calibra" (BuildConfig.CORTO). Orquesta las
 * acciones PUBLICAS de {@link FlujoCalibracion} (calibrar, persistencia, aceptar, rechazar) en el orden de
 * {@link FlujoCalibracion#ORDEN_SESION}, sin preguntar acta a acta: si la re-medida es conforme, la acepta
 * sola, para poder seguir con la siguiente.
 *
 * MODULARIDAD (21-sep-2026, rules/modularidad.md: fichero 500 líneas, función 60/100): sacado de
 * {@code FlujoCalibracion.calibrarAutomatico}/{@code calibrarAutomaticoInterno}/{@code persistirYAceptarAuto}
 * (que ya rondaban las 2100 líneas del fichero), como su propio commit y con la suite en verde, salida
 * idéntica. El acta, el canal y los campos de sesión (sesionTodo, encendidoReciente, scEnUltimoAceptar...)
 * los sigue guardando {@link FlujoCalibracion}: esta clase solo los toca a través de los métodos de paquete
 * que esa clase expone para esto (operador, iniciarSesionAutomatica/terminarSesionAutomatica,
 * marcarAceptadoReciente, aceptadoVigente, fila, descartarActaVacia, salirModoAdministrador).
 *
 * Cov363 (revisiones arquitecto-iot y qa-istqb de Cov_3.6.2_calibrar), arreglos 4, 5 y 6:
 * - MEDIO (4): un código que quedó escrito y verificado pero sin aceptar (persistencia o #SC fallaron) no
 *   se cuenta como "No calibrado: <motivo>" sin más: se dice que queda escrito sin aceptar y qué hacer. Un
 *   acta que ya no puede quedar aceptada (todo resuelto, nada conforme: p. ej. un código restaurado tras
 *   dos re-medidas) se rechaza sola, para que el siguiente "Calibrar" no tropiece con
 *   {@code Acta.motivoNoEscribir} ("no se reescribe en esta acta", Acta.java:403-404).
 * - BAJO (5, RF-COV-17 "nunca el código"): los nombres del resumen son los de {@link Fabrica#nombreCorto}
 *   ("amarillo, lámina tipo I"), no "amarillo opaco".
 * - BAJO (6): al terminar, #Q# cierra el modo administrador.
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
            flujo.salirModoAdministrador();   // BAJO (QA, arreglo 6): #Q# cierra el modo administrador
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
        Acta acta = flujo.acta();
        if (acta != null) {
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
                    ok.append(Fabrica.nombreCorto(k0)).append("; ");
                } else {
                    anotarNoCalibrado(no, k0, a);
                }
            } else if (flujo.acta() != null) {
                anotarNoCalibrado(no, k0, r);
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
                no.append(Fabrica.nombreCorto(k)).append(": ").append(p.motivoNo).append("; ");
                continue;
            }
            if (f.requiereAceptado != 0 && !aceptados.contains(f.requiereAceptado)) {
                no.append(Fabrica.nombreCorto(k)).append(": depende de que quede aceptado ")
                        .append(Fabrica.nombreCorto(f.requiereAceptado)).append("; ");
                continue;
            }
            flujo.operador().progreso("Calibrando " + Fabrica.nombreCorto(k) + "...");
            Set<Character> uno = new HashSet<>();
            uno.add(k);
            String r = flujo.calibrar(uno, nombre, NOTA_AUTOMATICA);
            if (flujo.acta() == null || flujo.acta().motivoNoAceptable(false) != null) {
                anotarNoCalibrado(no, k, r);
                continue;
            }
            String a = persistirYAceptarAuto(k);
            if (a != null) {
                anotarNoCalibrado(no, k, a);
                continue;
            }
            aceptados.add(k);
            ok.append(Fabrica.nombreCorto(k)).append("; ");
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

    /**
     * MEDIO (Cov363, arreglo 4): clasifica lo que queda tras un intento fallido de escribir/persistir/
     * aceptar el código k, en vez de anotar sin más "No calibrado: <motivo>":
     * - si el código quedó CONFORME pero el acta no llegó a aceptarse (persistencia o #SC fallaron), lo
     *   dice y da los dos caminos (terminar o restaurar);
     * - si el acta ya no puede quedar aceptada (todo resuelto — conforme o restaurado — y nada conforme:
     *   p. ej. un código que se restauró tras dos re-medidas), se rechaza sola, porque si no la siguiente
     *   pulsación de "Calibrar" tropieza en bucle con Acta.motivoNoEscribir ("no se reescribe en esta
     *   acta", Acta.java:403-404) sin que RF-COV-17 le dé al operador un botón "Rechazar" que pulsar.
     */
    private void anotarNoCalibrado(StringBuilder no, char k, String resultado) throws IOException, InterruptedException {
        Acta acta = flujo.acta();
        no.append(Fabrica.nombreCorto(k)).append(": ");
        if (acta == null) {
            no.append(resultado).append("; ");
            return;
        }
        Acta.Codigo c = acta.codigo(k);
        if (c != null && c.conforme()) {
            no.append("queda escrito sin aceptar: pulse Calibrar para terminar o Rechazar para restaurar (")
                    .append(resultado).append("); ");
            return;
        }
        if (todoResueltoSinConforme(acta)) {
            String rech = flujo.rechazar("RF-COV-17: acta sin ningún código conforme, cerrada sola para no "
                    + "bloquear la siguiente pulsación de Calibrar (arreglo Cov363-4)");
            no.append(resultado).append("; acta rechazada sola para no bloquear el siguiente Calibrar: ")
                    .append(rech).append("; ");
            return;
        }
        no.append(resultado).append("; ");
    }

    /**
     * BAJO B-2 (Cov363, arreglo 5, QA): true si el resumen de {@link #calibrarAutomatico} dice que ALGO
     * quedó calibrado ("Calibrado: " sin ser "Calibrado: ninguno."). Java puro, de paquete, para que
     * CalibrarActivity lo use sin repetir la regla (antes miraba solo si el texto contenía "falta"/"Falta",
     * y eso disparaba "No se puede calibrar" aunque la propia calibración fuera correcta, por el motivo de
     * OTRO código bloqueado).
     */
    static boolean huboExito(String resumen) {
        return resumen != null && resumen.startsWith("Calibrado: ") && !resumen.startsWith("Calibrado: ninguno.");
    }

    /** true si todos los códigos del acta están resueltos (conforme o restaurado) y ninguno es conforme. */
    private static boolean todoResueltoSinConforme(Acta acta) {
        boolean alguno = false;
        for (Acta.Codigo c : acta.codigos()) {
            if (!c.resuelto()) {
                return false;
            }
            alguno |= c.conforme();
        }
        return !alguno;
    }
}
