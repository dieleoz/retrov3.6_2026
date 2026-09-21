package com.dpi.retrousuario;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dpi.retrousuario.dominio.Canal;
import com.dpi.retrousuario.dominio.CanalRegistrado;
import com.dpi.retrousuario.dominio.DeteccionYSonda;
import com.dpi.retrousuario.dominio.DetectorEquipo;
import com.dpi.retrousuario.dominio.Diario;
import com.dpi.retrousuario.dominio.EstadoCalibracion;
import com.dpi.retrousuario.dominio.EstadoDeteccion;
import com.dpi.retrousuario.dominio.EstrategiaExportacion;
import com.dpi.retrousuario.dominio.FechaISO;
import com.dpi.retrousuario.dominio.GestorEnlace;
import com.dpi.retrousuario.dominio.RegistroTramas;
import com.dpi.retrousuario.dominio.RespuestaV;
import com.dpi.retrousuario.dominio.SesionMedicion;
import com.dpi.retrousuario.dominio.Sonda362;
import com.dpi.retrousuario.dominio.SondaReintentable;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Pantallas 1 y 2 (SPEC-App-Usuario-V3.6.md §1): aviso antes de conectar, lista de equipos
 * emparejados, detección (RF-USR-01) y estado de calibración (RF-USR-02). Al terminar la sonda crea
 * la {@link SesionMedicion} (con lo ya leído: serie, MAC, calibración) y ofrece "Medir" hacia
 * {@link MedirActivity}. **Sin pantalla 3 propia** ("elegir modo", SPEC §1): en el incremento 1 el
 * único modo entregado es "Medir y exportar" (RF-USR-05), así que no hay nada que elegir todavía; la
 * pantalla se añade cuando exista una segunda opción real ("Señal a señal", incremento 2).
 *
 * Esta Activity sólo pinta y llama al dominio (CLAUDE.md, rules/modularidad.md "Capas"): toda la
 * decisión de RF-USR-01/02 vive en {@code dominio.*}, probado en la JVM.
 */
public final class MainActivity extends AppCompatActivity {

    private static final int PETICION_PERMISO_EXPORTAR = 200;

    private TextView tvEstado;
    private ListView lvEquipos;
    private Button btnReintentar;
    private Button btnContinuar;
    private Button btnExportar;

    /** M2: UN registro y UN diario para toda la vida del proceso (fichero, no memoria): así una
     *  reconexión o una Activity recreada no pierde lo acumulado hasta que se exporte. */
    private RegistroTramas log;
    private Diario diario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvEstado = findViewById(R.id.tvEstado);
        lvEquipos = findViewById(R.id.lvEquipos);
        btnReintentar = findViewById(R.id.btnReintentar);
        btnReintentar.setOnClickListener(v -> reintentarSonda());
        btnContinuar = findViewById(R.id.btnContinuar);
        btnContinuar.setOnClickListener(v -> startActivity(new Intent(this, MedirActivity.class)));
        btnExportar = findViewById(R.id.btnExportarPrincipal);
        btnExportar.setOnClickListener(v -> exportarDesdePrincipal());

        SesionHolder.cargarAjustesPersistidos(getApplicationContext()); // B3.
        log = new RegistroTramas(new File(getFilesDir(), "tramas.log"));
        diario = new Diario(new File(getFilesDir(), "diario_medidas.txt"));

        mostrarAvisoPrevio();
    }

    /**
     * C2 (sobre 0.3.3, corrige el comentario falso de esta misma Activity a :94-97 de esa entrega):
     * {@code onResume()} se llama SIEMPRE que esta Activity queda visible — tras {@code onCreate}
     * (giro de pantalla, o arranque normal) y también si el proceso simplemente vuelve al primer
     * plano — así que es el único sitio que hace falta para repintar desde {@link SesionHolder}, no
     * {@code onCreate} a secas.
     */
    @Override
    protected void onResume() {
        super.onResume();
        restaurarInterfaz();
    }

    /**
     * M-1 (arq, sobre 0.3.2) + C1/C2 (arq, sobre 0.3.3): {@code detectando} y el resultado de una
     * detección terminada viven en {@link SesionHolder} (dominio {@code EstadoDeteccion}), no en un
     * campo de esta Activity (se perdía al recrearla, giro de pantalla) ni en el {@code
     * runOnUiThread} de la Activity que lanzó el hilo de fondo (C2: pintaba sobre una instancia ya
     * destruida si hubo un giro mientras la conexión seguía en curso, y la Activity nueva no se
     * enteraba de nada — {@code MainActivity.hiloConectarYDetectar}). Esta Activity, viva, pinta
     * aquí lo que {@link SesionHolder} ya sabe: si todavía hay una detección en curso, "Conectando";
     * si no, y hay un resultado pendiente (publicado por el hilo de fondo, con o sin esta MISMA
     * instancia viva en ese momento), lo recoge UNA vez y lo pinta.
     */
    private void restaurarInterfaz() {
        lvEquipos.setEnabled(!SesionHolder.detectando());
        btnReintentar.setVisibility(SesionHolder.canalSondaEnCurso() != null ? View.VISIBLE : View.GONE);
        btnContinuar.setVisibility(SesionHolder.sesion() != null ? View.VISIBLE : View.GONE);
        if (SesionHolder.detectando()) {
            tvEstado.setText(R.string.conectando);
            return;
        }
        EstadoDeteccion.Resultado pendiente = SesionHolder.recogerResultadoDeteccionPendiente();
        if (pendiente != null) {
            mostrarResultadoDeteccion(pendiente.canal(), pendiente.deteccion());
        }
    }

    /** RF-USR-01: el aviso tiene que estar visible antes de cualquier trama (T-USR-02). M-1: sólo
     *  sale una vez por proceso (SesionHolder), no otra vez tras cada giro de pantalla. */
    private void mostrarAvisoPrevio() {
        if (SesionHolder.avisoAceptado()) {
            poblarListaEquipos();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.aviso_previo)
                .setCancelable(false)
                .setPositiveButton(R.string.entendido, (dialog, which) -> {
                    SesionHolder.marcarAvisoAceptado();
                    poblarListaEquipos();
                })
                .show();
    }

    private void poblarListaEquipos() {
        BluetoothAdapter adaptador = BluetoothAdapter.getDefaultAdapter();
        List<BluetoothDevice> emparejados = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        if (adaptador != null) {
            try {
                for (BluetoothDevice d : adaptador.getBondedDevices()) {
                    emparejados.add(d);
                    etiquetas.add(d.getName() + " (" + d.getAddress() + ")");
                }
            } catch (SecurityException e) {
                tvEstado.setText("Sin permiso para leer los equipos emparejados");
                return;
            }
        }
        lvEquipos.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas));
        lvEquipos.setOnItemClickListener((parent, view, position, id) -> conectarYDetectar(emparejados.get(position)));
    }

    /**
     * A2 (ALTO) + M-1 (giro, condición QA-5): la guarda de cambio de equipo va sobre
     * {@link SesionHolder#enlace()}, NO sobre un campo de esta Activity (M-1: un campo de Activity se
     * pierde al recrearla, giro de pantalla). La decisión de reutilizar, cerrar o conectar de cero es
     * de {@link GestorEnlace} (arq C1, dominio puro, con prueba JVM): antes de esta condición, la
     * guarda sólo miraba {@link SesionHolder#enlace()}, que sólo se rellenaba si la sonda tenía éxito
     * — tras NO_COMPATIBLE, ACTUALIZAR_FIRMWARE o reintentos agotados el enlace quedaba abierto pero
     * invisible para esta guarda, y elegir otro equipo (o el mismo) abría un SEGUNDO socket RFCOMM
     * sin cerrar el primero. Ahora {@link SesionHolder#registrarEnlaceAbierto} guarda el enlace en
     * cuanto se conecta, ANTES de saber si la sonda tendrá éxito, así que {@link SesionHolder#enlace()}
     * es siempre la única fuente de verdad.
     */
    private void conectarYDetectar(BluetoothDevice dispositivo) {
        if (dispositivo == null || SesionHolder.detectando()) {
            return; // M7: una sola detección a la vez; una segunda pulsación mientras hay una en curso se ignora.
        }
        EnlaceBluetooth previo = SesionHolder.enlace();
        String macPrevio = previo != null ? previo.macConectada() : null;
        boolean previoVivo = previo != null && previo.vivo();
        GestorEnlace.Decision decision = GestorEnlace.decidir(macPrevio, previoVivo, dispositivo.getAddress());
        boolean reutilizar = decision == GestorEnlace.Decision.REUTILIZAR;
        if (!reutilizar) {
            if (previo != null) {
                previo.desconectar(); // CERRAR_Y_CONECTAR: MAC distinta, o el anterior ya no está vivo.
            }
            SesionHolder.limpiar(); // A2: la sesión, el enlace y el estado de "Reintente" anteriores dejan de estar disponibles.
        }
        SesionHolder.marcarDetectando(true); // M-1: en SesionHolder, sobrevive a un giro a mitad de detección.
        prepararControlesConectando();
        new Thread(() -> hiloConectarYDetectar(dispositivo, reutilizar), "deteccion-usuario").start();
    }

    private void prepararControlesConectando() {
        btnReintentar.setVisibility(View.GONE);
        btnContinuar.setVisibility(View.GONE);
        lvEquipos.setEnabled(false);
        tvEstado.setText(R.string.conectando);
    }

    /**
     * C1 (sobre 0.3.3, MainActivity.java:192-194 de esa entrega): TODOS los caminos de salida de
     * este hilo terminan la detección con {@code SesionHolder.marcarDetectando(false)} — antes, el
     * {@code return} temprano de más abajo (Atrás a mitad de "Conectando", con la Activity ya
     * terminando de verdad) no lo hacía, y la app se quedaba en "Conectando" para siempre. El
     * {@code finally} lo garantiza sin repetir la llamada en cada rama; la única excepción real es la
     * recursión de la línea de abajo (reintento con conexión nueva), que sigue "detectando" hasta que
     * ESA vuelta también pase por este mismo {@code finally} — un solo booleano, sin ventana entre
     * medias.
     */
    private void hiloConectarYDetectar(BluetoothDevice dispositivo, boolean reutilizar) {
        try {
            EnlaceBluetooth enlaceLocal = reutilizar ? SesionHolder.enlace() : EnlaceBluetooth.conectar(dispositivo);
            if (!reutilizar) {
                // arq B-3 (sobre 0.3.2): si la Activity ya terminó de verdad mientras connect()
                // estaba en curso (botón Atrás a mitad de la conexión), este socket recién abierto
                // no tiene ya ninguna pantalla a la que volver — se cierra aquí en vez de registrarlo
                // en SesionHolder como un enlace huérfano que nadie va a liberar (isFinishing() NO es
                // true en un giro de pantalla, sólo en una salida real: MainActivity.onDestroy).
                if (isFinishing()) {
                    enlaceLocal.desconectar();
                    return; // C1: el finally, abajo, termina la detección — no un "return" mudo.
                }
                SesionHolder.registrarEnlaceAbierto(enlaceLocal); // C1: única fuente de verdad, desde antes de sondear.
            }
            CanalRegistrado canalRegistrado = new CanalRegistrado(enlaceLocal, log, enlaceLocal); // M2.
            if (!reutilizar) {
                canalRegistrado.sesionNueva(enlaceLocal.macConectada()); // B-3: marca la sesion nueva con la MAC.
            }
            // QA-3: detección ("#V#") y primera sonda ("#GN#"/"#GC#") con la pausa de 150 ms entre las dos.
            DeteccionYSonda.Resultado resultado = DeteccionYSonda.ejecutar(canalRegistrado, SesionHolder.parametros());
            if (debeReintentarConexionNueva(reutilizar, resultado)) {
                // arq ALTO, segunda parte: el REUTILIZADO puede estar muerto sin que vivo() lo supiera
                // todavía — se cierra y se reintenta con conexión nueva antes de concluir "no compatible".
                enlaceLocal.desconectar();
                // Bajos (sobre 0.3.3): SesionHolder todavía apunta al enlace (y, si lo hubiera, a la
                // sesión) que acabamos de cerrar arriba — sin este limpiar(), MedirActivity/AjustesActivity
                // verían por un instante una sesión viva sobre un socket ya cerrado si preguntaran entre
                // este desconectar() y que la vuelta de abajo registre el enlace nuevo.
                SesionHolder.limpiar();
                // C1, limpiar() también termina la detección (defensivo, para quien llame a limpiar()
                // desde fuera de este hilo) — aquí SIGUE en curso, la vuelta de abajo apenas empieza:
                // se repone antes de recursar, para no dejar un hueco con "Conectando" a false a mitad
                // de un reintento (habilitaría la lista y una segunda pulsación durante la reconexión).
                SesionHolder.marcarDetectando(true);
                hiloConectarYDetectar(dispositivo, false); // UNA vez: la vuelta siguiente ya no es "reutilizado".
                return;
            }
            // C2 (sobre 0.3.3, corrige MainActivity.java:94-97/211 de esa entrega): el resultado se
            // PUBLICA en SesionHolder — no se pinta con un runOnUiThread que capture canalRegistrado/
            // resultado sobre "this" — porque "this" puede ser una Activity ya destruida (giro de
            // pantalla a mitad de la conexión/sonda): esa Activity vieja pintaba sobre sí misma, sin
            // que nadie más se enterara. Cualquier Activity viva (la misma, o una recreada) lo recoge y
            // lo pinta desde SesionHolder en su propio onResume() → restaurarInterfaz().
            SesionHolder.publicarResultadoDeteccion(canalRegistrado, resultado);
            runOnUiThread(this::restaurarInterfaz);
        } catch (Exception e) {
            String mensaje = e.getMessage();
            runOnUiThread(() -> {
                tvEstado.setText("No se pudo conectar: " + mensaje);
                lvEquipos.setEnabled(true);
            });
        } finally {
            SesionHolder.marcarDetectando(false); // C1: TODO camino de salida de este hilo termina "Conectando".
        }
    }

    /** Arq ALTO, segunda parte (sobre 0.3.2): decisión en dominio puro,
     *  {@link GestorEnlace#debeReintentarConexionNueva}, sobre el resultado de esta detección. */
    private static boolean debeReintentarConexionNueva(boolean reutilizar, DeteccionYSonda.Resultado resultado) {
        boolean noCompatible = resultado.deteccion().resultado() == DetectorEquipo.Resultado.NO_COMPATIBLE;
        return noCompatible && GestorEnlace.debeReintentarConexionNueva(reutilizar, resultado.deteccion().sinRespuesta());
    }

    /** C2: pinta un resultado ya publicado en {@link SesionHolder} — se llama sólo desde
     *  {@link #restaurarInterfaz()}, que ya comprobó que la detección terminó ({@code detectando() ==
     *  false}, puesto por el {@code finally} de {@link #hiloConectarYDetectar}); no repite esa
     *  llamada aquí. */
    private void mostrarResultadoDeteccion(Canal canalRegistrado, DeteccionYSonda.Resultado resultado) {
        if (resultado.deteccion().resultado() == DetectorEquipo.Resultado.NO_COMPATIBLE) {
            tvEstado.setText(R.string.equipo_no_compatible);
            lvEquipos.setEnabled(true); // A2: tras "no compatible", elegir otro (o el mismo, C1) sin reiniciar.
            return;
        }
        // QA-1: la sonda dentro de DeteccionYSonda YA fue el primer intento ("la sonda original").
        // B-1: el estado de "Reintente" vive en SesionHolder (sobrevive a un giro), no en un campo de esta Activity.
        SondaReintentable reintentos = new SondaReintentable(SesionHolder.parametros(), 1);
        SesionHolder.registrarSondaEnCurso(canalRegistrado, reintentos, resultado.deteccion().respuestaV());
        mostrarResultadoSonda(resultado.sonda());
    }

    /**
     * QA-1 (RF-USR-01, T-USR-01b(d)(e)): el reintento lo dispara EL OPERADOR (este método sólo se
     * llama desde el click de {@link #btnReintentar}), nunca la app sola; reutiliza el canal de
     * {@link SesionHolder#canalSondaEnCurso()} para no reenviar "#V#".
     */
    private void reintentarSonda() {
        Canal canal = SesionHolder.canalSondaEnCurso();
        SondaReintentable reintentos = SesionHolder.reintentosSondaEnCurso();
        if (SesionHolder.detectando() || canal == null || reintentos == null) {
            return;
        }
        SesionHolder.marcarDetectando(true);
        btnReintentar.setVisibility(View.GONE);
        lvEquipos.setEnabled(false);
        tvEstado.setText(R.string.conectando);
        new Thread(() -> {
            Sonda362.ResultadoSonda sonda = reintentos.intentar(canal);
            SesionHolder.marcarDetectando(false);
            runOnUiThread(() -> mostrarResultadoSonda(sonda));
        }, "reintento-sonda-usuario").start();
    }

    private void mostrarResultadoSonda(Sonda362.ResultadoSonda sonda) {
        lvEquipos.setEnabled(true);
        switch (sonda.resultado()) {
            case ACTUALIZAR_FIRMWARE:
                tvEstado.setText(R.string.actualice_firmware);
                return;
            case SIN_RESPUESTA_REINTENTAR:
                mostrarSinRespuesta();
                return;
            default:
                mostrarEstadoCalibracion(sonda);
        }
    }

    /** QA-1: ofrece reintentar sólo mientras queden reintentos (hasta 2); agotados, con exigir_362 =
     *  true, mensaje distinto y ningún botón (no se ofrece un tercero). arq B-4 (SPEC :115-122): con
     *  exigir_362 = false, agotados los reintentos se mide igual, degradado ({@link Sonda362#vacio()}),
     *  en vez de dejar al operador sin salida — Sonda362.sondear ya ofrece los reintentos con
     *  cualquier valor de exigir_362, esta rama sólo decide qué pasa cuando se agotan. */
    private void mostrarSinRespuesta() {
        SondaReintentable reintentos = SesionHolder.reintentosSondaEnCurso();
        if (reintentos != null && reintentos.puedeReintentar()) {
            tvEstado.setText(R.string.sin_respuesta_reintente);
            btnReintentar.setVisibility(View.VISIBLE);
            return;
        }
        if (!SesionHolder.parametros().exigir362()) {
            mostrarEstadoCalibracion(Sonda362.vacio());
            return;
        }
        tvEstado.setText(R.string.sin_datos_calibracion_serie);
        btnReintentar.setVisibility(View.GONE);
    }

    private void mostrarEstadoCalibracion(Sonda362.ResultadoSonda sonda) {
        FechaISO hoy = hoyDelDispositivo();
        RespuestaV respuestaVActual = SesionHolder.respuestaVEnCurso();
        String fechaGC = sonda.fechaRegistrada() ? sonda.fechaCalibracion() : "NONE";
        EstadoCalibracion estado = EstadoCalibracion.calcular(respuestaVActual.estadoAjuste(), fechaGC, hoy);
        String serie = sonda.serieLeida() ? sonda.serie() : "SIN SERIE";
        String texto = "Serie: " + serie + "\n" + (estado.texto().isEmpty() ? "Calibración vigente" : estado.texto());
        tvEstado.setText(texto);

        // RF-USR-04, RF-USR-05, RF-USR-06: la sesion de medir se crea aqui, una vez, con lo que ya sondeo esta pantalla.
        EnlaceBluetooth enlaceActual = SesionHolder.enlace();
        SesionMedicion sesion = new SesionMedicion(enlaceActual, SesionHolder.parametros(), log, diario);
        // A2: la MAC sale del dispositivo REALMENTE conectado (el socket), no sólo del último elegido en la lista.
        String mac = enlaceActual != null ? enlaceActual.macConectada() : "";
        sesion.registrarEquipo(mac, respuestaVActual.crudo(), sonda.serie(), sonda.serieLeida(), // D-1/M1.
                respuestaVActual.estadoAjuste(), fechaGC, sonda.fechaRegistrada(), hoy);
        SesionHolder.establecer(sesion, enlaceActual);
        SesionHolder.limpiarSondaEnCurso(); // B-1: ya hay sesión, el estado de "Reintente" deja de hacer falta.
        btnContinuar.setVisibility(View.VISIBLE);
    }

    private static FechaISO hoyDelDispositivo() {
        Calendar c = Calendar.getInstance();
        return FechaISO.de(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * M-2 (condición QA-6): "Exportar" desde la pantalla principal, sin equipo conectado en ESTA
     * sesión: si {@link SesionHolder} ya tiene una sesión viva (equipo conectado ahora), se usa esa
     * (con su serie); si no, se arma una sesión SÓLO para exportar sobre {@link #log}/{@link #diario}
     * ya acumulados (medidas de una conexión anterior en este mismo proceso) — sin equipo, el nombre
     * del ZIP sale "VARIOS" o "SIN_SERIE" (SPEC :281-283), nunca bloquea.
     */
    private void exportarDesdePrincipal() {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            sesion = new SesionMedicion(SesionHolder.enlace(), SesionHolder.parametros(), log, diario);
        }
        if (EstrategiaExportacion.paraApi(Build.VERSION.SDK_INT) == EstrategiaExportacion.Via.ARCHIVO_DIRECTO
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, PETICION_PERMISO_EXPORTAR);
            tvEstado.setText(R.string.exportar_permiso_necesario);
            return;
        }
        SesionMedicion sesionFinal = sesion;
        btnExportar.setEnabled(false);
        new Thread(() -> {
            try {
                String destinoTexto = exportarSegunApi(sesionFinal);
                runOnUiThread(() -> {
                    tvEstado.setText(getString(R.string.medir_exportado, destinoTexto));
                    btnExportar.setEnabled(true);
                });
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> {
                    tvEstado.setText(getString(R.string.exportar_error, mensaje));
                    btnExportar.setEnabled(true);
                });
            }
        }, "exportar-principal").start();
    }

    private String exportarSegunApi(SesionMedicion sesion) throws java.io.IOException {
        if (EstrategiaExportacion.paraApi(Build.VERSION.SDK_INT) == EstrategiaExportacion.Via.MEDIA_STORE) {
            android.net.Uri uri = ExportadorAndroid.exportarMediaStore(getApplicationContext(), sesion, new Date());
            return uri.toString();
        }
        File carpeta = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "RetroUsuario");
        File zip = sesion.exportar(carpeta, new Date(), TimeZone.getDefault());
        return zip.getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PETICION_PERMISO_EXPORTAR) {
            boolean concedido = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            tvEstado.setText(concedido ? R.string.exportar_permiso_concedido : R.string.exportar_permiso_denegado);
        }
    }

    /**
     * M-2 (arq, sobre 0.3.2, corrige M7/B-1 de la 0.3.1): {@code MainActivity} es la Activity RAÍZ
     * de esta app (lanzador único, sin `singleTask` ni tareas separadas) — cuando el operador la
     * cierra de verdad (botón Atrás desde aquí, o "Cerrar" del sistema), eso es salir de la app, y
     * el equipo se libera para que otro teléfono pueda conectar: se cierra el enlace y se limpia
     * {@link SesionHolder} (sesión incluida), aunque hubiera una sesión de medida en curso — README
     * "salir con Atrás libera el equipo".
     *
     * <p>{@link #isFinishing()} distingue esto de las otras dos formas de pasar por
     * {@code onDestroy} sin que sea una salida real: {@link #isChangingConfigurations()} (giro de
     * pantalla: {@link SesionHolder} sobrevive tal cual, ni se toca) y que el sistema recicle esta
     * instancia por memoria sin que el usuario haya pedido salir ({@code isFinishing() == false} en
     * ese caso también). El fallo de 766e6f2 que motivó M7/B-1: comparaba contra un campo local que
     * SÍ se había rellenado antes de terminar la sonda, así que un giro A MITAD de la detección
     * cerraba el socket que el hilo de fondo seguía usando; con el enlace viviendo siempre en
     * {@link SesionHolder} (nunca en un campo propio, desde {@link #conectarYDetectar}) esa carrera
     * ya no puede pasar.</p>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isFinishing() || isChangingConfigurations()) {
            return; // giro de pantalla, o destrucción sin terminar de verdad (el sistema recupera memoria).
        }
        EnlaceBluetooth actual = SesionHolder.enlace();
        if (actual != null) {
            actual.desconectar();
        }
        SesionHolder.limpiar(); // M-2: libera el equipo aunque hubiera sesión viva (MedirActivity no sobrevive a esto).
    }
}
