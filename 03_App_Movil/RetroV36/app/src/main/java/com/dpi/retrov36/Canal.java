package com.dpi.retrov36;

import java.io.IOException;

/**
 * Peticion-respuesta con el equipo (3.6.13). En la app lo implementa {@link Cliente} (Bluetooth);
 * en las pruebas JVM, un equipo simulado que emula el protocolo de la 3.6.2. Todo el flujo
 * "Calibrar este equipo" ({@link FlujoCalibracion}) habla con el equipo solo a traves de aqui.
 */
public interface Canal {

    /**
     * Envia una trama y espera su respuesta.
     * @throws IOException si no hay enlace o se pierde durante la espera (corte).
     */
    Cliente.Respuesta pedir(String trama, Tramas.Tipo tipo, long timeoutMs) throws IOException, InterruptedException;
}
