package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * D-1/M1 (ALTO QA, revisión P16 de RetroUsuario 0.2.0): {@code RespuestaV} tiene que conservar el
 * texto crudo de "#V#" tal como llegó, sin que nadie (MainActivity incluido) lo reconstruya a mano.
 * Esta prueba pasa por el MISMO método de dominio, {@link RespuestaV#crudo()}, que usa la capa
 * Android (MainActivity, tras este arreglo) para rellenar {@code firmware_v} — así una prueba verde
 * aquí certifica también lo que hace la Activity, no sólo el dominio.
 */
public class RespuestaVTest {

    /** #V# de 5 campos (con máscara), el formato que manda hoy el firmware (calibracion_v36.c:635-642). */
    @Test
    public void crudoConservaElTextoOriginalDeCincoCampos() {
        String original = "#V,3.6,2026-09-19,CAL,0003#";
        RespuestaV v = RespuestaV.analizar(original);
        assertNotNull(v);
        assertEquals(original, v.crudo());
    }

    /** #V# de 4 campos (sin máscara), el formato documentado en PROTOCOLO-V3.6.md:42. */
    @Test
    public void crudoConservaElTextoOriginalDeCuatroCampos() {
        String original = "#V,3.6,2026-09-19,DEF#";
        RespuestaV v = RespuestaV.analizar(original);
        assertNotNull(v);
        assertEquals(original, v.crudo());
    }

    /** Vista en rojo (CLAUDE.md §7): un borrador que reconstruyera el texto a mano, como hacía
     *  MainActivity antes de este arreglo ("#V," + version + "," + fecha + "," + calDef + "#"),
     *  perdería el quinto campo (la máscara) del #V# real: el original de 5 campos NO sería igual a
     *  la reconstrucción de 4. Esta ficha, ejecutada contra ese borrador, fallaría exactamente así:
     *  "#V,3.6,2026-09-19,CAL,0003#" (original) contra "#V,3.6,2026-09-19,CAL#" (reconstruido, sin
     *  máscara). Con {@link RespuestaV#crudo()} en su lugar, no hay reconstrucción que pueda perder
     *  ningún campo: es el texto que llegó, verbatim.
     */
    @Test
    public void reconstruirAManoPerderiaLaMascaraDeUnV36ConCincoCampos() {
        String original = "#V,3.6,2026-09-19,CAL,0003#";
        RespuestaV v = RespuestaV.analizar(original);
        assertNotNull(v);
        String reconstruidoAMano = "#V," + v.version() + "," + v.fechaCompilacion() + ","
                + (v.estadoAjuste() == RespuestaV.EstadoAjuste.CAL ? "CAL" : "DEF") + "#";
        assertEquals("#V,3.6,2026-09-19,CAL#", reconstruidoAMano); // pierde ",0003" (D-1, el defecto original).
        assertEquals(original, v.crudo()); // crudo() no lo pierde: es el texto exacto recibido.
    }
}
