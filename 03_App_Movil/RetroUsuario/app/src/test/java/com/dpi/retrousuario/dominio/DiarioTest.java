package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-25 — persistencia entre reinicios del proceso, cada disparo al diario, y serie incompleta
 * descartada (TDD-V3.6.md §8, RF-USR-06, nuevo r6, C5). "Muerte del proceso" simulada construyendo
 * una {@link Diario} nueva sobre el mismo fichero, sin arrastrar ningún estado de la instancia
 * anterior (la propia ficha lo exige: la fuente de verdad de la instancia nueva es sólo el fichero).
 */
public class DiarioTest {

    private File fichero;

    @Before
    public void preparar() throws IOException {
        fichero = File.createTempFile("diario-usr25", ".txt");
        fichero.deleteOnExit();
    }

    @After
    public void limpiar() {
        fichero.delete();
    }

    private static FilaMedida filaDePrueba(String color) {
        return new FilaMedida("2026-09-21T10:00:00-05:00", "4,609712", "-74,081753", "con_posicion", color,
                '4', 3, Arrays.asList(100, 110, 120), 110, 100, true, "", "SLV-002", "leida",
                "00:21:13:05:19:3B", "#V,3.6,2026-09-19,CAL,0003#", "2026-09-19", "2027-09-19", "CAL");
    }

    /** (a) 3 series completas persistidas antes de "morir": tras reabrir, las 3 sobreviven intactas. */
    @Test
    public void tresSeriesCompletasSobrevivenAlReabrir() {
        Diario diario = new Diario(fichero);
        for (String color : new String[] { "rojo", "azul", "verde" }) {
            long id = diario.nuevoIntento();
            diario.registrarDisparo(id, 0, 100);
            diario.registrarDisparo(id, 1, 110);
            diario.registrarDisparo(id, 2, 120);
            diario.registrarFila(filaDePrueba(color));
        }

        Diario diarioReabierto = new Diario(fichero); // "muerte del proceso": instancia nueva, mismo fichero.
        List<FilaMedida> filas = diarioReabierto.filasGuardadas();
        assertEquals(3, filas.size());
        assertEquals("rojo", filas.get(0).color);
        assertEquals("azul", filas.get(1).color);
        assertEquals("verde", filas.get(2).color);
    }

    /** (b) una tercera serie con sólo 2 de 3 disparos (muere a mitad): se descarta al reabrir, sin fila a medias. */
    @Test
    public void serieIncompletaAlMorirElProcesoSeDescartaAlReabrir() {
        Diario diario = new Diario(fichero);
        long id1 = diario.nuevoIntento();
        diario.registrarDisparo(id1, 0, 100);
        diario.registrarDisparo(id1, 1, 110);
        diario.registrarDisparo(id1, 2, 120);
        diario.registrarFila(filaDePrueba("rojo"));

        long id2 = diario.nuevoIntento();
        diario.registrarDisparo(id2, 0, 200);
        diario.registrarDisparo(id2, 1, 210);
        diario.registrarFila(filaDePrueba("azul"));

        long id3 = diario.nuevoIntento(); // tercera serie: sólo 2 de 3 disparos, el proceso "muere" aquí.
        diario.registrarDisparo(id3, 0, 300);
        diario.registrarDisparo(id3, 1, 310);
        // sin registrarFila para id3: la serie no llegó a cerrar.

        Diario diarioReabierto = new Diario(fichero);
        List<FilaMedida> filas = diarioReabierto.filasGuardadas();
        assertEquals(2, filas.size());
        assertEquals("rojo", filas.get(0).color);
        assertEquals("azul", filas.get(1).color);
    }

    /** B1: una línea FILA cortada a mitad de escritura (el proceso murió mientras la escribía, la
     *  serie propia de este caso ya cerró la parte "buena" antes) no revienta la lectura del resto
     *  del fichero: se ignora y queda en lineasCortadasIgnoradas(), sin contar como fila guardada. */
    @Test
    public void unaFilaCortadaSeIgnoraYQuedaAnotadaSinReventarElResto() throws IOException {
        Diario diario = new Diario(fichero);
        long id1 = diario.nuevoIntento();
        diario.registrarDisparo(id1, 0, 100);
        diario.registrarDisparo(id1, 1, 110);
        diario.registrarDisparo(id1, 2, 120);
        diario.registrarFila(filaDePrueba("rojo")); // fila completa, antes de la que se corta.

        // Simula el proceso muriendo a mitad de escribir la linea FILA de "azul": sólo llegaron a
        // disco los primeros campos, cortados en medio de "media" (c[9]), sin los que siguen.
        String lineaCortada = "FILA\u00012026-09-21T10:00:00-05:00\u00014,609712\u0001-74,081753"
                + "\u0001con_posicion\u0001azul\u00014\u00013\u0001100|110|120\u000111";
        try (FileWriter w = new FileWriter(fichero, true)) {
            w.write(lineaCortada);
            w.write('\n');
        }

        long id3 = diario.nuevoIntento();
        diario.registrarDisparo(id3, 0, 200);
        diario.registrarDisparo(id3, 1, 210);
        diario.registrarDisparo(id3, 2, 220);
        diario.registrarFila(filaDePrueba("verde")); // fila completa, después de la cortada.

        Diario diarioReabierto = new Diario(fichero);
        List<FilaMedida> filas = diarioReabierto.filasGuardadas();
        assertEquals(2, filas.size()); // "azul" (cortada) no cuenta; "rojo" y "verde" sí.
        assertEquals("rojo", filas.get(0).color);
        assertEquals("verde", filas.get(1).color);
        List<String> ignoradas = diarioReabierto.lineasCortadasIgnoradas();
        assertEquals(1, ignoradas.size());
        assertTrue(ignoradas.get(0).startsWith("FILA"));
    }
}
