package com.dpi.retrov36;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

/**
 * "Botones de pantalla" (solo V3.6): ingenieria inversa de la pantalla STONE.
 * #KC# vacia el registro del firmware; el operador pulsa un boton en el
 * equipo; #K# devuelve las ultimas tramas STONE. La app resalta el byte 8
 * (bufferPantalla[8]) y guarda la pareja boton -> trama.
 */
public class BotonesActivity extends Base {

    private TextView txtTramas;
    private TextView txtEstado;
    private EditText edBoton;
    private Button btnVaciar;
    private Button btnLeer;
    private Button btnGuardar;
    private volatile boolean ocupado;
    /** Trama mas reciente de la ultima lectura, en hex; null si no hay. */
    private String ultimaHex;
    private String ultimoCodigo;
    /** Total de tramas que dijo el firmware en la lectura anterior. */
    private long totalAnterior = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("1) Pulse \"Vaciar (#KC#)\".  2) Pulse UN botón en la pantalla del equipo.  "
                + "3) Pulse \"Leer (#K#)\".  4) Escriba qué botón pulsó y pulse Guardar.\n"
                + "Se resalta el byte 8 de cada trama (bufferPantalla[8]): es el código que usa el firmware. "
                + "Referencia archivada: OTROS PAPELES = 0x01-0x06, PAPEL TIPO I = 0x07-0x0D.");
        btnVaciar = boton("Vaciar (#KC#)", v -> vaciar());
        btnLeer = boton("Leer (#K#)", v -> leer());
        fila(btnVaciar, btnLeer);
        txtEstado = texto("");
        txtTramas = texto("");
        txtTramas.setTypeface(Typeface.MONOSPACE);
        edBoton = campo("Botón pulsado (p. ej. OTROS PAPELES > barra de color, 2º toque)",
                InputType.TYPE_CLASS_TEXT);
        btnGuardar = boton("Guardar pareja botón -> trama", v -> guardar());
        boton("Compartir registro y datos", v -> compartirTodo());
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnLeer == null) {
            return;
        }
        boolean v36 = Sesion.get().version == Sesion.Version.V36;
        boolean con = EnlaceSerie.instancia().estaConectado();
        btnVaciar.setEnabled(v36 && con && !ocupado);
        btnLeer.setEnabled(v36 && con && !ocupado);
        btnGuardar.setEnabled(ultimaHex != null);
        if (!v36 && !ocupado) {
            txtEstado.setText("Sólo con firmware V3.6 (órdenes #K# y #KC#). Firmware detectado: "
                    + Sesion.get().firmware());
        }
    }

    private void vaciar() {
        ocupado = true;
        refrescar();
        Cliente.instancia().ejecutar(() -> {
            String t;
            try {
                Cliente.Respuesta r = Cliente.instancia().pedir("#KC#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                t = Tramas.esOk(r.trama) ? "Registro vaciado. Pulse ahora un botón en el equipo y después Leer."
                        : "#KC# -> " + r.describir();
            } catch (IOException | InterruptedException e) {
                t = "Error: " + EnlaceSerie.descripcion(e);
            }
            final String fin = t;
            enUi(() -> {
                ocupado = false;
                ultimaHex = null;
                totalAnterior = 0;
                txtTramas.setText("");
                txtEstado.setText(fin);
                refrescar();
            });
        });
    }

    private void leer() {
        ocupado = true;
        refrescar();
        Cliente.instancia().ejecutar(() -> {
            Cliente.Respuesta r = null;
            String err = null;
            try {
                r = Cliente.instancia().pedir("#K#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            } catch (IOException | InterruptedException e) {
                err = "Error: " + EnlaceSerie.descripcion(e);
            }
            final Cliente.Respuesta rr = r;
            final String e2 = err;
            enUi(() -> {
                ocupado = false;
                if (e2 != null) {
                    txtEstado.setText(e2);
                } else {
                    pintar(rr);
                }
                refrescar();
            });
        });
    }

    private void pintar(Cliente.Respuesta r) {
        Tramas.RegistroK k = r.valida() ? Tramas.parsearK(r.trama) : null;
        if (k == null) {
            txtEstado.setText("#K# -> " + r.describir() + (r.valida() ? " (no cuadra con el contrato)" : ""));
            ultimaHex = null;
            return;
        }
        long nuevas = totalAnterior >= 0 ? k.total - totalAnterior : -1;
        txtEstado.setText("Tramas vistas desde el arranque: " + k.total
                + (nuevas >= 0 ? "; nuevas desde la lectura anterior: " + nuevas : "")
                + "; en la respuesta: " + k.tramas.size() + " (de la más antigua a la más reciente)");
        totalAnterior = k.total;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        List<byte[]> l = k.tramas;
        ultimaHex = null;
        for (int i = 0; i < l.size(); i++) {
            byte[] t = l.get(i);
            for (int j = 0; j < t.length; j++) {
                String h = String.format("%02X", t[j] & 0xFF);
                int ini = sb.length();
                sb.append(h);
                if (j == Tramas.POS_CODIGO_STONE) {
                    sb.setSpan(new BackgroundColorSpan(Color.YELLOW), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new StyleSpan(Typeface.BOLD), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.append(' ');
            }
            String codigo = t.length > Tramas.POS_CODIGO_STONE
                    ? Fabrica.describirCodigoStone(t[Tramas.POS_CODIGO_STONE] & 0xFF)
                    : "trama de " + t.length + " bytes: no llega al byte 8";
            sb.append("\n   byte 8: ").append(codigo).append(i == l.size() - 1 ? "   <- la más reciente" : "").append("\n");
            if (i == l.size() - 1) {
                ultimaHex = Hex.hex(t);
                ultimoCodigo = codigo;
            }
        }
        if (l.isEmpty()) {
            sb.append("Sin tramas: la pantalla no ha enviado nada desde el último vaciado.");
        }
        txtTramas.setText(sb);
        Registro.nota("#K#: " + r.trama);
    }

    private void guardar() {
        String boton = edBoton.getText().toString().trim();
        if (boton.isEmpty()) {
            alerta("Botón", "Escriba qué botón pulsó en la pantalla del equipo.");
            return;
        }
        if (ultimaHex == null) {
            return;
        }
        try {
            Sesion.get().agregarBoton(this, boton, ultimaHex, ultimoCodigo);
            Registro.nota("boton \"" + boton + "\" -> trama " + ultimaHex + " (" + ultimoCodigo + ")");
            aviso("Guardado: " + boton + " -> " + ultimoCodigo);
            edBoton.setText("");
        } catch (IOException e) {
            alerta("No se pudo guardar", EnlaceSerie.descripcion(e));
        }
    }
}
