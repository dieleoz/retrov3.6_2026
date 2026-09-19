package com.dpi.retrov36;

/**
 * Protocolo de un firmware (RTV 1.0, RF-APP-U06 de SPEC-App-Unica-V36-V46.md; ESTUDIO-Tecnologia §6). Java puro.
 *
 * Sustituye al booleano "es V3.6": ninguna pantalla pregunta por la version, pregunta al protocolo que puede
 * hacer. Cada firmware es una implementacion: {@link ProtocoloV2020}, {@link ProtocoloV36},
 * {@link ProtocoloV4Original} y {@link ProtocoloV46}. Lo que es dato (dominio de x, cola, coherencia en DEF,
 * bateria) viene del CSV de firmwares ({@link PerfilFirmware}); lo que es sintaxis de tramas, de aqui.
 */
public interface Protocolo {

    /** Los firmwares de SPEC-App-Unica §1 que la app sabe tratar. */
    enum Firmware {
        F2020("V3 2020 (sin e)"),
        F36("V3.6"),
        V4_ORIGINAL("V4 original (F-40/F-41), sin identificar la versión exacta"),
        F46("V4.6"),
        DESCONOCIDO("desconocido");

        public final String texto;

        Firmware(String t) {
            texto = t;
        }
    }

    Firmware firmware();

    /** Texto para cabeceras y registros. */
    String nombre();

    /** true si la trama puede enviarse a este firmware (lista cerrada; RF-APP-U06, T-U10). */
    boolean permitida(String trama);

    /** Trama que mide R con la clave k; null si la clave no existe en este firmware. */
    String tramaMedida(char k);

    Tramas.Tipo tipoMedida();

    long timeoutMedidaMs();

    /** R de la respuesta de medida; null si no cuadra. */
    Integer valorMedida(String trama);

    /** true si el firmware da x, directa o por inversion (V3 2020). Sin x no hay banco ni pruebas 3-6. */
    boolean daX();

    /** Trama que lee x (en bruto) para la clave k; null si no hay trama directa. */
    String tramaX(char k);

    Tramas.Tipo tipoX();

    /** x de la respuesta; null si no cuadra. */
    Double valorX(String trama, char k);

    /**
     * RTV 1.0.0-rc3 (decision de Diego: el diario anota la temperatura de cada disparo). Trama que lee la
     * temperatura, o null si este firmware no tiene ninguna que devuelva la LECTURA del sensor.
     *
     * Hoy solo la V4.6, con "#T#". La V3.6 tiene #GT#, #ST y #FT, pero las tres leen o escriben los
     * COEFICIENTES del factor de temperatura, no el sensor (PROTOCOLO-V3.6.md:48-50, verificado: cero
     * apariciones de #T# en ese documento). Del V4 original no sale por @LEERV. Donde no se puede leer, la
     * columna va VACIA con su motivo ({@link #motivoSinTemperatura()}): nunca 0, y nunca omitida.
     */
    String tramaTemperatura();

    Tramas.Tipo tipoTemperatura();

    /** Temperaturas de la respuesta; null si no cuadra. */
    Tramas.Temperatura valorTemperatura(String trama);

    /** Por que este firmware no da la temperatura; "" si la da. */
    String motivoSinTemperatura();

    /** Trama de bateria; null si el firmware no la tiene. */
    String tramaBateria();

    Tramas.Tipo tipoBateria();

    /** n de la respuesta de bateria; null si no cuadra. */
    Integer valorBateria(String trama);

    /** true si habla el contrato de administracion "#...#" (V3.6 rev. 1.1, y la V4.6 que lo adopta). */
    boolean administra();

    /** true si se puede calibrar ("Calibrar este equipo", Avanzado). */
    boolean calibra();

    /** Por que no se calibra; "" si se calibra. */
    String motivoNoCalibra();

    /** true si se puede medir el banco (hay x). */
    boolean mideBanco();

    /** Por que no se mide el banco; "" si se mide. */
    String motivoNoBanco();

    /** Etiqueta de una medida de la clave k (RF-APP-U07: el tipo 1 del V4 original no es retrorreflexion). */
    String etiquetaMedida(char k);

    /** Perfil de datos del firmware (CSV). */
    PerfilFirmware perfil();
}
