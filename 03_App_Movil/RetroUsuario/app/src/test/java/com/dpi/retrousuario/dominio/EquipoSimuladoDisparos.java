package com.dpi.retrousuario.dominio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doble de pruebas del enlace, byte a byte, con reloj propio (receta R-JVM de TDD-V3.6.md §8: "reloj
 * inyectable, nada de sleep real"). Implementa {@link Canal} (para `#V#`/`#GN#`/`#GC#`, RF-USR-01/02,
 * sin cambios de esta parte) **y** {@link FuenteBytes} (para `::<n>`, RF-USR-04/16), para que T-USR-19a
 * pueda comprobar la lista blanca de RF-USR-15 sobre UN solo registro de lo recibido.
 *
 * <p>Cada llamada a {@link #enviarByte} consume el siguiente "programa" (una respuesta programada con
 * {@link #programarValor}, {@link #programarTexto}, {@link #programarFragmentos} o
 * {@link #programarSinRespuesta}): una lista de fragmentos de texto, cada uno con su instante
 * (relativo al envío). Los fragmentos se entregan carácter a carácter cuando su instante cae dentro
 * del límite que pide {@link #leer(long)} — sin importar si quien pregunta es {@link LectorDisparo}
 * (esperando su disparo) o {@link EmisorRitmo#cuarentena} (descartando todo): así una respuesta
 * programada bien dentro de la cuarentena se entrega igual, para las fichas de T-USR-24.</p>
 */
final class EquipoSimuladoDisparos implements Canal, FuenteBytes {

    private static final class Programa {
        final List<Object[]> fragmentos = new ArrayList<>(); // {offsetMs (Long), texto (String)}
        boolean sinRespuesta;
    }

    private final Map<String, String> respuestasTrama = new HashMap<>();
    private final List<String> tramasRecibidas = new ArrayList<>();
    private final Deque<Programa> programas = new ArrayDeque<>();
    private final List<long[]> colaBytes = new ArrayList<>(); // {tAbsoluto, byte}
    private final List<long[]> deltasEnvio = new ArrayList<>(); // {desdeEnvioAnterior, desdeUltimoByte}

    private long ahora = 0;
    private long tUltimoEnvio = Long.MIN_VALUE / 2;
    private long tUltimoByte = Long.MIN_VALUE / 2;

    EquipoSimuladoDisparos responde(String trama, String respuesta) {
        respuestasTrama.put(trama, respuesta);
        return this;
    }

    void programarValor(long offsetMs, int valor) {
        programarTexto(offsetMs, "::" + valor);
    }

    void programarTexto(long offsetMs, String texto) {
        Programa p = new Programa();
        p.fragmentos.add(new Object[] { offsetMs, texto });
        programas.add(p);
    }

    /** Varios trozos en una sola respuesta programada: offset1, texto1, offset2, texto2, ... */
    void programarFragmentos(Object... paresOffsetTexto) {
        Programa p = new Programa();
        for (int i = 0; i < paresOffsetTexto.length; i += 2) {
            p.fragmentos.add(new Object[] { ((Number) paresOffsetTexto[i]).longValue(), paresOffsetTexto[i + 1] });
        }
        programas.add(p);
    }

    void programarSinRespuesta() {
        Programa p = new Programa();
        p.sinRespuesta = true;
        programas.add(p);
    }

    @Override
    public String enviar(String trama, long plazoMs) {
        tramasRecibidas.add(trama);
        return respuestasTrama.get(trama);
    }

    @Override
    public long ahoraMs() {
        return ahora;
    }

    @Override
    public void enviarByte(int b) {
        tramasRecibidas.add(String.valueOf((char) b));
        deltasEnvio.add(new long[] { ahora - tUltimoEnvio, ahora - tUltimoByte });
        tUltimoEnvio = ahora;
        colaBytes.clear();
        Programa p = programas.isEmpty() ? sinRespuestaPorDefecto() : programas.poll();
        if (p.sinRespuesta) {
            return;
        }
        for (Object[] frag : p.fragmentos) {
            long tAbs = ahora + (Long) frag[0];
            String texto = (String) frag[1];
            for (int i = 0; i < texto.length(); i++) {
                colaBytes.add(new long[] { tAbs, texto.charAt(i) });
            }
        }
        colaBytes.sort(Comparator.comparingLong(a -> a[0]));
    }

    @Override
    public Integer leer(long limiteMs) {
        if (!colaBytes.isEmpty() && colaBytes.get(0)[0] <= limiteMs) {
            long[] ev = colaBytes.remove(0);
            ahora = Math.max(ahora, ev[0]);
            tUltimoByte = ahora;
            return (int) ev[1];
        }
        ahora = Math.max(ahora, limiteMs);
        return null;
    }

    private static Programa sinRespuestaPorDefecto() {
        Programa p = new Programa();
        p.sinRespuesta = true;
        return p;
    }

    List<String> tramasRecibidas() {
        return Collections.unmodifiableList(tramasRecibidas);
    }

    /** Un par {desde el envío anterior, desde el último byte recibido} por cada {@link #enviarByte}, en orden (T-USR-26). */
    List<long[]> deltasDeRitmo() {
        return Collections.unmodifiableList(deltasEnvio);
    }
}
