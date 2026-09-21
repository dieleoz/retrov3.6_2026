package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Condiciones C1/C2 (arquitecto-iot sobre RetroUsuario 0.3.3, MainActivity.java:192-194 y :94-97/211
 * de esa entrega — ver el Javadoc de {@link EstadoDeteccion}). Dos casos, tal como los pide la
 * condición: "salida durante conexión → detectando=false" (C1) y "resultado publicado tras destruir
 * la Activity → la nueva lo muestra" (C2, simulado aquí sin Activity: publicar y luego recoger desde
 * "otra" pregunta es indistinguible, en dominio puro, de que lo pregunte una Activity distinta).
 *
 * <p>{@link EstadoDeteccion} es una clase nueva de esta entrega (0.3.4): no existía en la 0.3.3, así
 * que copiar este fichero solo contra ese commit daría "cannot find symbol" — eso no demuestra nada
 * del comportamiento. La demostración de rojo real es la del método
 * {@code salirDuranteConexionDejaDetectandoEnFalso} de abajo: corrida ANTES de que {@code salir()}
 * existiera (con el cuerpo de {@link EstadoDeteccion#salir()} vacío, sin tocar {@code detectando}, tal
 * como estaba el camino de MainActivity.java:192-194 antes de esta entrega — la línea que C1 dice que
 * faltaba), JUnitCore dio:</p>
 *
 * <pre>
 * JUnit version 4.13.2
 * ....E.
 * Time: 0,012
 * There was 1 failure:
 * 1) salirDuranteConexionDejaDetectandoEnFalso(com.dpi.retrousuario.dominio.EstadoDeteccionTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertFalse(Assert.java:65)
 *     at org.junit.Assert.assertFalse(Assert.java:75)
 *     at com.dpi.retrousuario.dominio.EstadoDeteccionTest.salirDuranteConexionDejaDetectandoEnFalso(EstadoDeteccionTest.java:73)
 *
 * FAILURES!!!
 * Tests run: 5,  Failures: 1
 * </pre>
 *
 * <p>— capturado tal cual con JUnitCore contra esta misma clase (README "Tests JVM"), rojo por
 * comportamiento (una aserción real que falló con {@code salir()} vacío, no un símbolo que faltara),
 * corregida de vuelta a la implementación real (que sí pone {@code detectando = false}) en
 * {@link EstadoDeteccion}; con ella, verde (134 tests en la suite completa).</p>
 */
public class EstadoDeteccionTest {

    /** C1: antes de conectar, no hay ninguna detección en curso. */
    @Test
    public void alCrearloNoHayDeteccionEnCurso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        assertFalse(estado.detectando());
        assertNull(estado.recogerResultadoPendiente());
    }

    @Test
    public void iniciarPoneDetectandoEnCurso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        assertTrue(estado.detectando());
    }

    /** C1 (MainActivity.java:192-194): Atrás a mitad de "Conectando" (la Activity ya terminó antes
     *  de que el hilo llegara a registrar el enlace) sale del hilo SIN publicar ningún resultado —
     *  {@link EstadoDeteccion#salir()} es el único camino de ese "return" temprano, y tiene que dejar
     *  {@code detectando() == false}, o la app se queda en "Conectando" para siempre (el bug real).
     */
    @Test
    public void salirDuranteConexionDejaDetectandoEnFalso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();

        estado.salir();

        assertFalse(estado.detectando());
    }

    /** {@link EstadoDeteccion#salir()} sin publicar no deja ningún resultado pendiente (no repinta
     *  nada: es justo el caso de "cerrar sin llegar a saber qué contestó el equipo"). */
    @Test
    public void salirSinPublicarNoDejaResultadoPendiente() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();

        estado.salir();

        assertNull(estado.recogerResultadoPendiente());
    }

    /** C2 (MainActivity.java:94-97/211): el hilo de fondo termina con un resultado DESPUÉS de que la
     *  Activity que lo lanzó ya se destruyó (giro de pantalla) — antes se pintaba con
     *  {@code runOnUiThread} sobre esa instancia vieja y se perdía; ahora se publica aquí, y una
     *  Activity que pregunte DESPUÉS (la nueva, recreada) lo recibe intacto, y sólo una vez. */
    @Test
    public void resultadoPublicadoTrasDestruirLaActivityLoMuestraLaNueva() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        Canal canalDeLaConexion = (trama, plazoMs) -> null; // dominio puro: no hace falta un canal real.
        DeteccionYSonda.Resultado resultadoDeLaSonda = DeteccionYSonda.ejecutar(
                canalDeLaConexion, new ParametrosRitmo());

        // El hilo de fondo publica el resultado DESPUÉS de que, en el escenario real, la Activity que
        // lo lanzó ya no existe (esta prueba no instancia ninguna Activity: lo relevante es que
        // publicar() no depende de ninguna).
        estado.publicar(canalDeLaConexion, resultadoDeLaSonda);

        assertFalse(estado.detectando());
        EstadoDeteccion.Resultado pendiente = estado.recogerResultadoPendiente();
        assertSame(resultadoDeLaSonda, pendiente.deteccion());
        assertSame(canalDeLaConexion, pendiente.canal());
        // C2, "UNA vez": una segunda Activity que pregunte después de la primera no ve nada (no
        // repinta el mismo resultado dos veces, ni uno viejo de una detección anterior).
        assertNull(estado.recogerResultadoPendiente());
    }
}
