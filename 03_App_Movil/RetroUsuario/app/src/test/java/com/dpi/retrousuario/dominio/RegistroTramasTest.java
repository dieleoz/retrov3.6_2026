package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * M2: `tramas.log` persistido en disco, acumulado entre instancias (Activity recreada, o proceso
 * relanzado) hasta exportar — mismo principio que {@link DiarioTest} para `medidas.csv`/T-USR-25,
 * extendido aquí al registro de tramas (corrige un borrador anterior que sólo lo llevaba en memoria).
 */
public class RegistroTramasTest {

    private File fichero;

    @Before
    public void preparar() throws IOException {
        fichero = File.createTempFile("tramas-m2", ".log");
        fichero.delete(); // el constructor de RegistroTramas escribe la cabecera si no existe.
    }

    @After
    public void limpiar() {
        fichero.delete();
    }

    /** Una segunda instancia sobre el MISMO fichero ve lo que escribió la primera, y sigue acumulando. */
    @Test
    public void unaSegundaInstanciaSobreElMismoFicheroAcumulaLoDeLaPrimera() {
        RegistroTramas primera = new RegistroTramas(fichero);
        primera.tx(0, "#V#");
        primera.rx(10, "#V,3.6,2026-09-19,CAL,0000#");

        RegistroTramas segunda = new RegistroTramas(fichero); // "Activity recreada" / "proceso relanzado".
        segunda.tx(20, "#GN#");

        List<String> lineas = segunda.lineas();
        // cabecera (2 lineas) + tx #V# + rx #V,... + tx #GN# = 5, sin duplicar la cabecera.
        assertEquals(5, lineas.size());
        assertTrue(lineas.get(2).contains("TX") && lineas.get(2).contains(RegistroTramas.hexDe("#V#")));
        assertTrue(lineas.get(4).contains("TX") && lineas.get(4).contains(RegistroTramas.hexDe("#GN#")));
    }

    /** La cabecera no se duplica si ya existe (fichero no vacío al reabrir). */
    @Test
    public void laCabeceraNoSeDuplicaAlReabrir() {
        new RegistroTramas(fichero);
        new RegistroTramas(fichero);
        new RegistroTramas(fichero);

        List<String> lineas = new RegistroTramas(fichero).lineas();
        assertEquals(2, lineas.size()); // sólo las 2 líneas de cabecera, una vez.
    }

    /** El constructor sin fichero sigue en memoria pura (pruebas del dominio, sin tocar disco). */
    @Test
    public void sinFicheroQuedaSoloEnMemoria() {
        RegistroTramas log = new RegistroTramas();
        log.tx(0, "#V#");
        assertEquals(3, log.lineas().size());
        assertTrue(!fichero.exists());
    }
}
