package com.dpi.retrousuario.dominio;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Orquesta "Medir y exportar" (RF-USR-04, RF-USR-06, RF-USR-15 bis) sobre un equipo ya detectado
 * (RF-USR-01/02, fuera de esta clase): mide un color, persiste cada disparo y cada fila cerrada
 * ({@link Diario}), y exporta el ZIP. Cero tecleo (RF-USR-05/06): todos los datos de fila que no
 * salen del equipo (fecha-hora, GPS) los captura el llamante (la Activity) y se los pasa a
 * {@link #medir}, esta clase no llama a ningún reloj ni GPS del sistema (dominio Java puro).
 */
public final class SesionMedicion {

    private final FuenteBytes fuente;
    private final ParametrosRitmo params;
    private final RegistroTramas log;
    private final Diario diario;
    private final EmisorRitmo ritmo = new EmisorRitmo();

    private boolean conectado = false;
    private String mac = "";
    private String firmwareV = "";
    private String serieEquipo = "";
    private String serieOrigen = "ninguna";
    private String fechaCalibracion = "";
    private String vencimiento = "";
    private String estadoCalibracion = "DEF";

    public SesionMedicion(FuenteBytes fuente, ParametrosRitmo params, RegistroTramas log, Diario diario) {
        this.fuente = fuente;
        this.params = params;
        this.log = log;
        this.diario = diario;
    }

    public ParametrosRitmo parametros() {
        return params;
    }

    /**
     * Datos del equipo ya sondeado (RF-USR-01/02), para que cada fila lleve L-23: serie, MAC,
     * vencimiento. {@code fechaGCTexto}: "NONE" si no se pudo leer o si estadoAjuste es DEF.
     */
    public void registrarEquipo(String mac, String firmwareVCompleta, String serieEquipo, boolean serieLeida,
            RespuestaV.EstadoAjuste estadoAjuste, String fechaGCTexto, boolean gcRespondioAlgo, FechaISO hoy) {
        this.conectado = true;
        this.mac = mac;
        this.firmwareV = firmwareVCompleta;
        this.serieEquipo = serieLeida ? serieEquipo : "";
        this.serieOrigen = serieLeida ? "leida" : "ninguna";
        calcularEstadoCalibracion(estadoAjuste, fechaGCTexto, gcRespondioAlgo, hoy);
    }

    private void calcularEstadoCalibracion(RespuestaV.EstadoAjuste estadoAjuste, String fechaGCTexto,
            boolean gcRespondioAlgo, FechaISO hoy) {
        if (estadoAjuste == RespuestaV.EstadoAjuste.DEF) {
            estadoCalibracion = "DEF";
            fechaCalibracion = "";
            vencimiento = "";
            return;
        }
        if (!gcRespondioAlgo) {
            estadoCalibracion = "sin_fecha"; // RF-USR-01: CAL pero #GC# no contestó nada.
            fechaCalibracion = "";
            vencimiento = "";
            return;
        }
        EstadoCalibracion e = EstadoCalibracion.calcular(estadoAjuste, fechaGCTexto, hoy);
        if (e.estado() == EstadoCalibracion.Estado.SIN_REGISTRAR) {
            estadoCalibracion = "sin_fecha";
            fechaCalibracion = "";
            vencimiento = "";
        } else {
            estadoCalibracion = e.estado() == EstadoCalibracion.Estado.VENCIDA ? "vencida" : "CAL";
            fechaCalibracion = fechaGCTexto;
            vencimiento = e.vencimiento().toString();
        }
    }

    /**
     * Mide una serie completa del color dado (RF-USR-04) y, si se guarda, la persiste y la devuelve;
     * {@code null} si la serie se anuló (RF-USR-16: no produce fila, T-USR-06b).
     */
    public FilaMedida medir(String colorFondo, String fechaHoraIso, String latitud, String longitud,
            String gpsEstado) {
        Character codigo = MapaColor.byteParaColor(colorFondo);
        if (codigo == null) {
            throw new IllegalArgumentException("Color no medible: " + colorFondo);
        }
        SerieDisparos serie = new SerieDisparos(fuente, ritmo, params, log, diario, codigo);
        SerieDisparos.Resultado r = serie.medir();
        if (!r.guardada) {
            return null;
        }
        FilaMedida fila = new FilaMedida(fechaHoraIso, latitud, longitud, gpsEstado, colorFondo, codigo,
                r.lecturas.size(), r.lecturas, r.media, r.minimo, r.valido, r.motivo, serieEquipo, serieOrigen,
                mac, firmwareV, fechaCalibracion, vencimiento, estadoCalibracion);
        diario.registrarFila(fila);
        return fila;
    }

    /** Todas las filas guardadas hasta ahora (fuente de verdad: el diario, RF-USR-06/T-USR-25/27). */
    public List<FilaMedida> filas() {
        return diario.filasGuardadas();
    }

    /**
     * Exporta el ZIP a {@code carpeta} (RF-USR-15 bis); no borra nada. Nombre saneado con colisión
     * resuelta en la misma carpeta (nuevo r6, C4).
     */
    public File exportar(File carpeta, Date instante, TimeZone zona) {
        List<FilaMedida> filas = filas();
        String segmento = segmentoSerieParaNombre(filas);
        String base = NombreZip.base(segmento, instante, zona);
        String nombre = NombreZip.resolverColision(base, carpeta);
        File destino = new File(carpeta, nombre);
        ExportadorZip.escribir(destino, filas, log);
        return destino;
    }

    private String segmentoSerieParaNombre(List<FilaMedida> filas) {
        if (!conectado) {
            return "VARIOS"; // RF-USR-15 bis: exportar sin haber conectado ningun equipo en la sesion.
        }
        Set<String> series = new LinkedHashSet<>();
        for (FilaMedida f : filas) {
            if (!f.serieEquipo.isEmpty()) {
                series.add(f.serieEquipo);
            }
        }
        if (series.size() > 1) {
            return "VARIOS";
        }
        if (series.size() == 1) {
            return NombreZip.sanear(series.iterator().next());
        }
        return NombreZip.sanear(serieEquipo); // equipo actual, sin filas todavia o sin serie leida.
    }

    /** Sólo para pruebas/inspección: proyección de las tramas TX (T-USR-19a). */
    public List<String> tramasTxParaPrueba() {
        return new ArrayList<>(log.lineasTx());
    }
}
