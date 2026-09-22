/*
 * Tarea A4B-FILTRO (encargo del 21-sep-2026, contra HEAD e9d6e65 de rtv-1.0-cierre, Cov_3.6.7_calibrar).
 * Diego declaró bucle sobre RF-COV-17/21 tras dos correcciones punto a punto (3.6.6, 3.6.7:
 * REVISIONES-Apps-V3.6.md) y pidió CAMBIAR DE ENFOQUE (DECISIONES nota 21): un único filtro en la
 * frontera de la app corta ("RTV Calibra"), no más arreglos texto a texto. SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md, RF-COV-21 ("único filtro").
 *
 * Estas pruebas son PURAS (java.util.regex y Fabrica; sin Android, sin EquipoSimulado): cubren las siete
 * formas que RF-COV-21 nombra ("código k", "del k", "el k", "#S,k", "#G,k", "#F,k#", "#E,k") para los
 * 12 códigos de Fabrica.CODIGOS, y la ocultación del acta en corto (TextoOperador.actaParaPantalla).
 * El valor esperado de cada sustitución es Fabrica.elCodigo(k, true) / Fabrica.nombreCorto(k): la propia
 * fuente de RF-COV-21 ("el nombre del color que ya da Fabrica.elCodigo(k, true)"), nunca inventado
 * (CLAUDE.md del repositorio, §7).
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextoOperadorTest {

    // ============================================================ limpiar(): las siete formas de RF-COV-21

    @Test
    public void formaCodigoMinusculaYMayuscula() {
        for (char k : Fabrica.CODIGOS) {
            String esperado = Fabrica.elCodigo(k, true);
            assertEquals("código " + k + " no se escribe: motivo",
                    esperado + " no se escribe: motivo", TextoOperador.limpiar(true, "código " + k + " no se escribe: motivo"));
            assertEquals("Código " + k + " (mayúscula, inicio de frase)",
                    esperado + " ya tiene un acta ACEPTADA",
                    TextoOperador.limpiar(true, "Código " + k + " ya tiene un acta ACEPTADA"));
        }
    }

    @Test
    public void formaDelYElConCodigo() {
        for (char k : Fabrica.CODIGOS) {
            String esperado = Fabrica.elCodigo(k, true);
            assertEquals("la restauración " + esperado + " no se ha verificado",
                    TextoOperador.limpiar(true, "la restauración del " + k + " no se ha verificado"));
            assertEquals("acéptela: " + esperado + " va solo",
                    TextoOperador.limpiar(true, "acéptela: el " + k + " va solo"));
        }
    }

    @Test
    public void formasDeTrama() {
        for (char k : Fabrica.CODIGOS) {
            String esperado = Fabrica.elCodigo(k, true);
            assertEquals("escritura: " + esperado + " -> ERR",
                    TextoOperador.limpiar(true, "escritura: #S," + k + " -> ERR"));
            assertEquals("relectura " + esperado + " distinta",
                    TextoOperador.limpiar(true, "relectura #G," + k + " distinta"));
            assertEquals("restaurar (" + esperado + ")",
                    TextoOperador.limpiar(true, "restaurar (#F," + k + "#)"));
            assertEquals(esperado + " coincide",
                    TextoOperador.limpiar(true, "#E," + k + " coincide"));
        }
    }

    /** Ninguna forma cruda sobrevive al filtro, para ningún código de Fabrica.CODIGOS. */
    @Test
    public void ningunaFormaCrudaSobrevive() {
        for (char k : Fabrica.CODIGOS) {
            for (String cruda : new String[] {
                    "código " + k, "del " + k + " restaurado", "el " + k + " va solo",
                    "#S," + k, "#G," + k, "#F," + k + "#", "#E," + k}) {
                String limpio = TextoOperador.limpiar(true, cruda);
                assertFalse("EN ROJO: '" + cruda + "' -> '" + limpio + "' sigue con el número",
                        limpio.contains("código " + k) || limpio.matches(".*[#,]" + k + "\\b.*"));
                assertTrue("'" + limpio + "' no lleva el nombre de color de " + k,
                        limpio.contains(Fabrica.elCodigo(k, true)));
            }
        }
    }

    /** No toca texto que no menciona ningún código: no hay "el"/"del" sueltos que no encajen en la forma. */
    @Test
    public void noTocaTextoSinCodigo() {
        String t = "Conectado con SLV-002. Cola del banco md5 abc123. Batería antes de la calibración...";
        assertEquals(t, TextoOperador.limpiar(true, t));
    }

    @Test
    public void nullYVacioNoRompen() {
        assertEquals(null, TextoOperador.limpiar(true, null));
        assertEquals("", TextoOperador.limpiar(true, ""));
    }

    /** corto=false: la app de campo no cambia (RF-COV-17 la deja tal cual, "código k" sigue). */
    @Test
    public void corteFalsoEsNoOp() {
        for (char k : Fabrica.CODIGOS) {
            String crudo = "código " + k + " no se escribe";
            assertEquals(crudo, TextoOperador.limpiar(false, crudo));
        }
    }

    /** Caso real: el motivo de Acta.motivoNoEscribir para un código restaurado tras dos re-medidas (C2
     *  del arquitecto, REVISIONES-Apps-V3.6.md: "motivoNoEscribir alcanzable en corto"). */
    @Test
    public void motivoDeRestauradoSinNumero() {
        for (char k : Fabrica.CODIGOS) {
            String crudo = "el código " + k + " se restauró tras dos re-medidas no conformes: no se reescribe en esta acta";
            String limpio = TextoOperador.limpiar(true, crudo);
            assertFalse("EN ROJO: sigue con \"código " + k + "\": " + limpio, limpio.contains("código " + k));
            assertTrue(limpio.contains(Fabrica.elCodigo(k, true)));
            assertTrue(limpio.endsWith("no se reescribe en esta acta"));
        }
    }

    // ============================================================ actaParaPantalla(): RF-COV-21, acta oculta

    @Test
    public void actaOcultaEnCortoAunqueTengaContenido() {
        Acta a = new Acta("SLV-002", "00:21:13:05:19:3B", "3.6.8", 5, 20, 60, "2026-09-21T10:00:00-0500");
        assertNull("EN ROJO: el acta no puede llegar a pantalla en corto", TextoOperador.actaParaPantalla(true, a, "falta algo"));
        assertNull("EN ROJO: tampoco sin acta en curso", TextoOperador.actaParaPantalla(true, null, null));
    }

    @Test
    public void actaVisibleEnCampoComoAntes() {
        Acta a = new Acta("SLV-002", "00:21:13:05:19:3B", "3.6.8", 5, 20, 60, "2026-09-21T10:00:00-0500");
        assertEquals(a.texto() + "\nPara aceptar: listo (la verificación final se hace al pulsar)",
                TextoOperador.actaParaPantalla(false, a, null));
        assertEquals(a.texto() + "\nPara aceptar: falta algo",
                TextoOperador.actaParaPantalla(false, a, "falta algo"));
        assertEquals("Sin acta en curso.", TextoOperador.actaParaPantalla(false, null, null));
    }
}
