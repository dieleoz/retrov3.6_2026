package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * T-USR-26 — ritmo: el simulador borra lo recibido 80 ms tras `::<n>`, y el disparo siguiente no
 * sale antes de la mayor de dos pausas: 1500 ms desde el envío anterior y 600 ms desde el último
 * byte recibido (TDD-V3.6.md §8, RF-USR-16, `SPEC-V3.6.md:441-446`).
 */
public class RitmoDePacingTest {

    private File fichero;

    @Before
    public void preparar() throws IOException {
        fichero = File.createTempFile("diario-usr26", ".txt");
        fichero.deleteOnExit();
    }

    @After
    public void limpiar() {
        fichero.delete();
    }

    @Test
    public void ningunEnvioSaleAntesDeLaPausaMayorDeLasDos() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        ParametrosRitmo params = new ParametrosRitmo();
        RegistroTramas log = new RegistroTramas();
        Diario diario = new Diario(fichero);
        // Cada disparo responde casi al instante (50 ms), como el firmware borrando el búfer 80 ms
        // después: si la app respetara sólo esos 80 ms, la ficha lo detectaría (Bajos, más abajo).
        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);

        new SerieDisparos(sim, new EmisorRitmo(), params, log, diario, '4').medir();

        for (long[] delta : sim.deltasDeRitmo()) {
            long desdeEnvioAnterior = delta[0];
            long desdeUltimoByte = delta[1];
            // El primer envio no tiene envio/byte anterior real (referencia muy en el pasado): se
            // cumple trivialmente. Los siguientes tienen que respetar las dos pausas.
            if (desdeEnvioAnterior < 1_000_000) { // descarta el "envio anterior" ficticio del primero.
                assertTrue("menos de 1500 ms desde el envio anterior: " + desdeEnvioAnterior,
                        desdeEnvioAnterior >= params.pausaEnvioMs());
                assertTrue("menos de 600 ms desde el ultimo byte: " + desdeUltimoByte,
                        desdeUltimoByte >= params.pausaSilencioMs());
                assertTrue("dentro de los 80 ms que el firmware borra", desdeUltimoByte >= 80);
            }
        }
    }
}
