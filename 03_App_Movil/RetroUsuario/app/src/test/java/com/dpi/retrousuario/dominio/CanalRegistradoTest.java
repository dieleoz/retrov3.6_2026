package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * B-3 (condición QA-8 sobre 32c785d): {@code tramas.log} deja línea cuando una trama "#...#" no
 * llega (silencio/timeout), y marca el arranque de una sesión Bluetooth nueva con la MAC. Antes de
 * esta ficha, un {@code enviar} que devolvía {@code null} dejaba un TX sin ningún RX ni comentario:
 * el registro no distinguía "no llegó nada" de "faltó anotarlo".
 */
public class CanalRegistradoTest {

    /** Silencio: TX se anota igual, pero además una línea de comentario "SIN_RESPUESTA <trama>". */
    @Test
    public void unaTramaSinRespuestaDejaComentarioEnElRegistro() {
        RegistroTramas log = new RegistroTramas();
        // #GN# sin configurar en EquipoSimulado: null. EquipoSimuladoDisparos sólo aporta el reloj (FuenteBytes).
        CanalRegistrado canal = new CanalRegistrado(new EquipoSimulado(), log, new EquipoSimuladoDisparos());

        String r = canal.enviar("#GN#", 2000);

        assertNull(r);
        List<String> lineas = log.lineas();
        boolean hayTx = false;
        boolean hayComentario = false;
        for (String l : lineas) {
            if (l.contains("TX") && l.contains(RegistroTramas.hexDe("#GN#"))) {
                hayTx = true;
            }
            if (l.startsWith("#") && l.contains("SIN_RESPUESTA") && l.contains("#GN#")) {
                hayComentario = true;
            }
        }
        assertTrue("falta el TX de #GN#: " + lineas, hayTx);
        assertTrue("falta el comentario SIN_RESPUESTA: " + lineas, hayComentario);
    }

    /** Una respuesta normal no deja el comentario de silencio, sólo TX + RX (comportamiento previo, sin cambios). */
    @Test
    public void unaRespuestaNormalNoDejaComentarioDeSilencio() {
        RegistroTramas log = new RegistroTramas();
        CanalRegistrado canal = new CanalRegistrado(
                new EquipoSimulado().responde("#GN#", "#GN,SLV-002#"), log, new EquipoSimuladoDisparos());

        canal.enviar("#GN#", 2000);

        for (String l : log.lineas()) {
            assertTrue(!l.contains("SIN_RESPUESTA"));
        }
    }

    /** B-3: sesionNueva(mac) deja un comentario con la MAC, distinto de TX/RX. */
    @Test
    public void sesionNuevaDejaComentarioConLaMac() {
        RegistroTramas log = new RegistroTramas();
        CanalRegistrado canal = new CanalRegistrado(new EquipoSimulado(), log, new EquipoSimuladoDisparos());

        canal.sesionNueva("00:21:13:05:19:3B");

        boolean encontrada = false;
        for (String l : log.lineas()) {
            if (l.startsWith("#") && l.contains("SESION_NUEVA") && l.contains("00:21:13:05:19:3B")) {
                encontrada = true;
            }
        }
        assertTrue("falta el comentario SESION_NUEVA con la MAC: " + log.lineas(), encontrada);
    }
}
