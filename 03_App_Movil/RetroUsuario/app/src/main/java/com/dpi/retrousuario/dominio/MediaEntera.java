package com.dpi.retrousuario.dominio;

import java.util.List;

/**
 * Media entera de una serie de disparos, **redondeo estándar, mitad hacia arriba** (RF-USR-06,
 * distinto del truncado hacia cero del Δ%, M-7: aquí no hay umbral que truncar contra cero). En
 * aritmética entera exacta (sin `double`, sin error de redondeo binario en el `,5`): con
 * {@code suma} y {@code n} no negativos, {@code round(suma/n) = suma/n + 1} si el resto duplicado
 * llega o supera a {@code n}, si no {@code suma/n} (división entera).
 */
final class MediaEntera {

    private MediaEntera() {
    }

    static int deLista(List<Integer> valores) {
        long suma = 0;
        for (int v : valores) {
            suma += v;
        }
        return redondear(suma, valores.size());
    }

    static int redondear(long suma, int n) {
        long cociente = suma / n;
        long resto = suma % n;
        return (int) (resto * 2 >= n ? cociente + 1 : cociente);
    }
}
