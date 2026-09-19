package com.dpi.retrov36;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * RTV 1.0.0-rc4: el texto de la firma del acta, aseverado CONTRA EL REQUISITO y no contra la salida.
 *
 * El requisito es una frase dictada por Diego, literal:
 * <pre>Firmado por "&lt;nombre&gt;", ITVIAL SAS, &lt;AAAA-MM-DD&gt;</pre>
 * y el acta es un documento que ve el cliente. De ahi que lo que se asevera aqui no sea "que contenga la
 * frase" -la rc3 la contenia y estaba mal- sino que <b>la frase se lea una sola vez y tal como se dicto</b>:
 * con mayuscula inicial y sin que ninguna otra parte del texto vuelva a anunciarla.
 *
 * En la rc3 (8b0fc9d) {@code firma()} ya devolvia la frase entera y los cuatro puntos de incrustacion le
 * anteponian "firmado por "/"liberado por ", asi que el acta decia <i>"firmado por Firmado por ..."</i>.
 * Estas comprobaciones fallan contra aquella version; su salida en rojo esta en el mensaje del commit.
 *
 * Nada de esto se ha probado contra un equipo fisico ni en un telefono.
 */
public class FirmaActaTest {

    /** Veces que aparece una subcadena, sin distinguir mayusculas. */
    static int veces(String texto, String aguja) {
        String t = texto.toLowerCase(Locale.ROOT);
        String a = aguja.toLowerCase(Locale.ROOT);
        int n = 0;
        for (int i = t.indexOf(a); i >= 0; i = t.indexOf(a, i + 1)) {
            n++;
        }
        return n;
    }

    /** Anuncios que, puestos DELANTE de la frase dictada, la repiten. */
    private static final String[] ANUNCIOS = {"firmado por ", "firmada por ", "liberado por ", "liberada por ",
            "la firma de ", "firma: ", "por "};

    /**
     * La regla del requisito: la frase dictada aparece UNA vez y NO va precedida de otro anuncio de la
     * firma. Se mira lo que hay justo delante, no cuantas veces sale "firmado por" en todo el documento: un
     * acta es un diario largo y puede nombrar la firma en otras lineas con todo derecho. Lo que no puede es
     * anunciarla dos veces seguidas.
     *
     * La version anterior de esta regla contaba ocurrencias en el documento entero y daba falsos positivos;
     * queda escrito para no volver a escribirla asi.
     */
    static void firmaSinAnuncioRepetido(String texto, String firmaEsperada) {
        assertTrue("no lleva la frase dictada: " + texto, texto.contains(firmaEsperada));
        assertEquals("la frase dictada tiene que aparecer una sola vez: " + texto,
                1, veces(texto, firmaEsperada));
        String antes = texto.substring(0, texto.indexOf(firmaEsperada)).toLowerCase(Locale.ROOT);
        for (String a : ANUNCIOS) {
            assertFalse("anuncio duplicado: la frase dictada va precedida de \"" + a.trim() + "\" en: "
                    + texto, antes.endsWith(a));
        }
    }

    /** Alias historico: la liberacion se comprueba con la misma regla. */
    static void anunciaLaFirmaUnaSolaVez(String texto, String firmaEsperada) {
        firmaSinAnuncioRepetido(texto, firmaEsperada);
    }

    static void anunciaLaLiberacionUnaSolaVez(String texto, String firmaEsperada) {
        firmaSinAnuncioRepetido(texto, firmaEsperada);
    }

    private static String firmaDe(String nombre, String fecha) {
        // la del codigo, no una copia: si alguien cambia el formato, estas pruebas lo siguen
        return FlujoCalibracion.firmaDe(nombre, fecha);
    }

    // ------------------------------------------------------------------ la forma de la frase

    /** F-01: la frase es exactamente la que dicto Diego. */
    @Test
    public void laFraseEsLaQueDictoDiego() {
        assertEquals("ITVIAL SAS", FlujoCalibracion.EMPRESA);
        String f = firmaDe("Ana Ruiz", "2026-09-19");
        assertEquals("Firmado por \"Ana Ruiz\", ITVIAL SAS, 2026-09-19", f);
        // mayuscula inicial: el acta la lee como frase, no como continuacion de otra
        assertTrue(f.startsWith("Firmado por \""));
        assertFalse("minuscula: seria continuacion de un anuncio previo", f.startsWith("firmado"));
    }

    /**
     * F-02: la fecha es la del dia, AAAA-MM-DD, y sale de Reloj.hoy() -el MISMO del que sale la fecha que se
     * graba en el equipo con #SC (FlujoCalibracion:1476)-, no de recortar ahoraIso(). Asi la firma del acta y
     * la fecha de calibracion del equipo no pueden discrepar. La rc3 usaba un recorte propio.
     */
    @Test
    public void laFechaSaleDelMismoRelojQueLaDeCalibracion() {
        FlujoCalibracion.Reloj r = new FlujoCalibracion.Reloj() {
            @Override
            public String ahoraIso() {
                return "2026-09-19T21:14:07";
            }

            @Override
            public String hoy() {
                return "2026-09-19";
            }
        };
        String f = FlujoCalibracion.firmaDe("Ana Ruiz", r.hoy());
        assertEquals("Firmado por \"Ana Ruiz\", ITVIAL SAS, 2026-09-19", f);
        assertEquals(10, r.hoy().length());
        assertTrue(r.hoy(), r.hoy().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
    }

    // ------------------------------------------------------------------ el anuncio, una sola vez

    /**
     * F-03 (la que cazo el defecto de la rc3): el acta no puede anunciar la firma dos veces. En la rc3 esto
     * daba "RECHAZADA SIN RESTAURAR, firmado por Firmado por \"Ana Ruiz\", ...".
     */
    @Test
    public void elActaNoAnunciaLaFirmaDosVeces() {
        String f = firmaDe("Ana Ruiz", "2026-09-19");
        // las dos formas que la rc3 producia, para que la regla se vea en rojo si alguien las repone
        firmaSinAnuncioRepetido("RECHAZADA SIN RESTAURAR. " + f + ". Motivo: el #F no entra | codigos: 8", f);
        firmaSinAnuncioRepetido("Acta cerrada SIN RESTAURAR. " + f + ". Codigos en estado desconocido: 8", f);
    }

    /** F-04: y la liberacion tampoco. Las formas correctas no anteponen NINGUN anuncio a la frase. */
    @Test
    public void laLiberacionNoAnunciaLaFirmaDosVeces() {
        String f = firmaDe("Luis Gomez", "2026-09-19");
        firmaSinAnuncioRepetido("LIBERADA. " + f + ". Motivo: curva del 8 comprobada con #G", f);
        firmaSinAnuncioRepetido("Equipo liberado. " + f + ". La nueva acta tomara como curva anterior la "
                + "que lea con #G.", f);
    }

    /**
     * F-05: la regla caza las CUATRO formas que la rc3 producia. Si alguien repone cualquiera de ellas,
     * vuelve a ponerse en rojo.
     */
    @Test
    public void laReglaCazaLasCuatroFormasDeLaRc3() {
        String f = firmaDe("Ana Ruiz", "2026-09-19");
        String[] rc3 = {
                "RECHAZADA SIN RESTAURAR, firmado por " + f + ": el #F no entra",
                "Acta cerrada SIN RESTAURAR (firmado por " + f + "). Codigos: 8",
                "liberado por " + f + ": curva comprobada",
                "Liberado por " + f + ". La nueva acta...",
        };
        for (String t : rc3) {
            try {
                firmaSinAnuncioRepetido(t, f);
                org.junit.Assert.fail("la regla tenia que rechazar: " + t);
            } catch (AssertionError esperado) {
                assertTrue(esperado.getMessage(), esperado.getMessage().contains("anuncio duplicado"));
            }
        }
    }
}
