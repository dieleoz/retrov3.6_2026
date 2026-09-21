package com.dpi.retrousuario.dominio;

import java.util.Collections;
import java.util.List;

/**
 * Resultado de {@link LectorDisparo#leer(FuenteBytes, ParametrosRitmo)}: un valor `::<n>` leído, o
 * una anulación por plazo vencido o por doble `::` (RF-USR-16, RF-USR-04).
 */
public final class ResultadoDisparo {

    public enum Tipo { VALOR, ANULADO_PLAZO, ANULADO_DOBLE }

    /** Un byte recibido, con su instante relativo al envío del disparo (para `tramas.log`). */
    public static final class ByteRecibido {
        public final long tMsRelativo;
        public final int valor;

        ByteRecibido(long tMsRelativo, int valor) {
            this.tMsRelativo = tMsRelativo;
            this.valor = valor;
        }
    }

    private final Tipo tipo;
    private final int valor;
    private final List<ByteRecibido> recibidos;
    private final long tFin;

    ResultadoDisparo(Tipo tipo, int valor, List<ByteRecibido> recibidos, long tFin) {
        this.tipo = tipo;
        this.valor = valor;
        this.recibidos = Collections.unmodifiableList(recibidos);
        this.tFin = tFin;
    }

    public Tipo tipo() {
        return tipo;
    }

    /** Sólo válido si {@link #tipo()} es {@link Tipo#VALOR}. */
    public int valor() {
        return valor;
    }

    /** Bytes recibidos durante la espera de este disparo (vacío si no llegó nada). */
    public List<ByteRecibido> recibidos() {
        return recibidos;
    }

    /** Instante (reloj de la fuente) en el que terminó la espera de este disparo: inicio de la cuarentena si se anula. */
    public long tFin() {
        return tFin;
    }

    public boolean anulado() {
        return tipo != Tipo.VALOR;
    }
}
