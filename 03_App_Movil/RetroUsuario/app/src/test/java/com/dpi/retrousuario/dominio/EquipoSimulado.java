package com.dpi.retrousuario.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doble de pruebas de un equipo V3.6 (o de otro firmware, para los casos de rechazo), para las
 * pruebas de este dominio en la JVM (receta R-JVM, TDD-V3.6.md §8; CLAUDE.md §7: "EquipoSimulado es
 * una fuente más, no la verdad" — cada respuesta configurada en las pruebas lleva su cita al fuente
 * del firmware o al protocolo en el propio test, no aquí).
 *
 * Registra cada trama que RECIBE (no lo que la app "dice" haber enviado, mismo principio que
 * RF-USR-15 bis/T-USR-19), para poder afirmar la lista blanca de RF-USR-01: "la secuencia enviada es
 * EXACTAMENTE #V#, nada más".
 */
final class EquipoSimulado implements Canal {

    private final List<String> tramasRecibidas = new ArrayList<>();
    private final Map<String, String> respuestas = new HashMap<>();

    /** Configura la respuesta a una trama exacta; null (o no llamar a este metodo) = silencio. */
    EquipoSimulado responde(String trama, String respuesta) {
        respuestas.put(trama, respuesta);
        return this;
    }

    @Override
    public String enviar(String trama, long plazoMs) {
        tramasRecibidas.add(trama);
        return respuestas.get(trama);
    }

    List<String> tramasRecibidas() {
        return Collections.unmodifiableList(tramasRecibidas);
    }
}
