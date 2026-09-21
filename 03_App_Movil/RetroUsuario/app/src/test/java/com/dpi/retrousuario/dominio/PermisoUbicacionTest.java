package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Condición C2 (arquitecto-iot sobre 0.3.1): la decisión de pedir {@code ACCESS_FINE_LOCATION} al
 * entrar a medir, probada en la JVM. Vista en rojo contra 766e6f2: la clase no existe en ese commit.
 */
public class PermisoUbicacionTest {

    @Test
    public void sinConcederYSinPedirAntesTocaPedirlo() {
        assertTrue(PermisoUbicacion.debePedirse(false, false));
    }

    @Test
    public void yaConcedidoNoHaceFaltaPedirlo() {
        assertFalse(PermisoUbicacion.debePedirse(true, false));
    }

    @Test
    public void noConcedidoPeroYaPedidoEnEsteProcesoNoInsiste() {
        assertFalse(PermisoUbicacion.debePedirse(false, true));
    }

    @Test
    public void concedidoYYaPedidoNoHaceFaltaPedirlo() {
        assertFalse(PermisoUbicacion.debePedirse(true, true));
    }
}
