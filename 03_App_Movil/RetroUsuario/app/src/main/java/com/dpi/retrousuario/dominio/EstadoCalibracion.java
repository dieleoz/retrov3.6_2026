package com.dpi.retrousuario.dominio;

/**
 * RF-USR-02: estado de calibración, visible siempre, con el formato real de "#V#".
 *
 * "El cuarto campo de #V,3.6,&lt;fecha de compilación&gt;,CAL|DEF,&lt;máscara %04X&gt;# decide: DEF
 * → 'sin calibración', AUNQUE #GC# devuelva una fecha (#F no borra #SC, PROTOCOLO-V3.6.md:51); CAL →
 * se lee #GC#/#GN#; vencimiento = fecha + 1 año [PA-02]; si NONE, vencida o sin registrar, avisa y
 * marca, no bloquea." (SPEC-App-Usuario-V3.6.md r4, RF-USR-02). T-USR-03(b) fija el mismo texto,
 * "SIN CALIBRACIÓN", tanto para DEF como para CAL con #GC,NONE#: el operador ve el mismo aviso, sea
 * cual sea la causa; el estado interno ({@link Estado}) sí distingue las dos, por si la pantalla
 * quiere decir "de fábrica" frente a "fecha no grabada" más adelante.
 *
 * Nunca bloquea la medida (RF-USR-02): esta clase sólo calcula el texto y el estado, no impide nada.
 */
public final class EstadoCalibracion {

    public enum Estado { SIN_CALIBRACION_DEF, SIN_REGISTRAR, VIGENTE, VENCIDA }

    public static final String TEXTO_SIN_CALIBRACION = "SIN CALIBRACIÓN";
    public static final String TEXTO_VENCIDA = "EQUIPO CON CALIBRACIÓN VENCIDA";

    private final Estado estado;
    private final FechaISO vencimiento;

    private EstadoCalibracion(Estado estado, FechaISO vencimiento) {
        this.estado = estado;
        this.vencimiento = vencimiento;
    }

    public Estado estado() {
        return estado;
    }

    /** Fecha de vencimiento (fecha de #GC# + 1 año, PA-02), o null si no hay fecha con la que calcularlo. */
    public FechaISO vencimiento() {
        return vencimiento;
    }

    /** Texto para la pantalla; "" si está vigente (RF-USR-02: sin aviso cuando no hace falta). */
    public String texto() {
        switch (estado) {
            case SIN_CALIBRACION_DEF:
            case SIN_REGISTRAR:
                return TEXTO_SIN_CALIBRACION;
            case VENCIDA:
                return TEXTO_VENCIDA;
            case VIGENTE:
            default:
                return "";
        }
    }

    /**
     * @param estadoAjuste  campo CAL/DEF de "#V#" (calibracion_v36.c:635-642).
     * @param fechaGCTexto  fecha cruda de "#GC#" ("AAAA-MM-DD" o "NONE"); ignorada si estadoAjuste es DEF.
     * @param hoy           fecha de referencia para decidir vencida/vigente (reloj de prueba en los tests).
     */
    public static EstadoCalibracion calcular(RespuestaV.EstadoAjuste estadoAjuste, String fechaGCTexto, FechaISO hoy) {
        if (estadoAjuste == RespuestaV.EstadoAjuste.DEF) {
            return new EstadoCalibracion(Estado.SIN_CALIBRACION_DEF, null);
        }
        FechaISO fecha = "NONE".equals(fechaGCTexto) ? null : FechaISO.deTexto(fechaGCTexto);
        if (fecha == null) {
            return new EstadoCalibracion(Estado.SIN_REGISTRAR, null);
        }
        FechaISO vencimiento = fecha.masUnAnio();
        Estado estado = hoy.esPosteriorA(vencimiento) ? Estado.VENCIDA : Estado.VIGENTE;
        return new EstadoCalibracion(estado, vencimiento);
    }
}
