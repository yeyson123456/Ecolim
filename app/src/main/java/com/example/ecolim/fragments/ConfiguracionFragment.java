package com.example.ecolim.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.ThemeManager;
import com.example.ecolim.activities.LoginActivity;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConfiguracionFragment extends Fragment {

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final String PREFS         = "ecolim_prefs";
    private static final String FOTO_FILENAME = "perfil_foto.jpg";

    // ── URLs ──────────────────────────────────────────────────────────────────
    private static final String URL_TERMINOS   = "https://jheyson.42web.io/Ecolim.com/terminos.html";
    private static final String URL_PRIVACIDAD = "https://jheyson.42web.io/Ecolim.com/Politica.html";
    private static final String URL_SOPORTE    = "https://jheyson.42web.io/Ecolim.com/";

    // ── Estado interno ────────────────────────────────────────────────────────
    private SharedPreferences prefs;
    private ThemeManager      tm;
    private EcolimDbHelper    db;
    private long              usuarioId;

    // ── Vistas: Perfil ────────────────────────────────────────────────────────
    private ImageView      imgAvatar;
    private TextView       txtAvatarIniciales, txtPerfilNombre,
            txtPerfilCargo, txtPerfilEmail, txtPerfilRol;
    private View           rowEditarPerfil, rowCambiarPassword, rowPinAcceso;

    // ── Vistas: Apariencia ────────────────────────────────────────────────────
    private SwitchMaterial swModoOscuro, swAltoContraste;
    private TextView       txtModoOscuroEstado;
    private View           rowColorTema, rowTamanoTexto, rowIdioma;

    // ── Vistas: Notificaciones ────────────────────────────────────────────────
    private SwitchMaterial swNotificaciones, swNotifPeligrosos,
            swRecordatorio, swVibracion;
    private TextView       txtHoraRecordatorio;
    private View           rowSonidoNotif;

    // ── Vistas: Seguridad ─────────────────────────────────────────────────────
    private SwitchMaterial swBiometria;
    private View           rowConfigurarPin, rowSesionAuto, rowRegistroActividad;

    // ── Vistas: Datos ─────────────────────────────────────────────────────────
    private TextView                 txtStorageUsado, txtUltimoBackup;
    private LinearProgressIndicator  progressStorage;
    private SwitchMaterial           swAutoBackup;
    private View                     rowLimpiarCache, rowExportarBd, rowRestaurarBd;

    // ── Vistas: Sync ──────────────────────────────────────────────────────────
    private View           indicatorSync;
    private TextView       txtSyncEstado, txtSyncUltima;
    private MaterialButton btnSyncAhora;
    private SwitchMaterial swSoloWifi;
    private View           rowFrecuenciaSync, rowServidorUrl;

    // ── Vistas: Accesibilidad ─────────────────────────────────────────────────
    private SeekBar        seekbarFontSize;
    private TextView       txtFontSizeLabel;
    private SwitchMaterial swReducirAnimaciones, swTalkBack;

    // ── Vistas: Acerca de ─────────────────────────────────────────────────────
    private View     rowVersion, rowNovedades, rowTerminos,
            rowPrivacidad, rowLicencias, rowSoporte, rowCalificar;
    private TextView txtFooterVersion;

    // ── Vistas: Zona peligro ──────────────────────────────────────────────────
    private View rowCerrarSesion, rowEliminarCuenta;

    // ── Launchers ─────────────────────────────────────────────────────────────
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onFotoSeleccionada);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) abrirGaleria();
                    else toast("Permiso denegado para acceder a fotos");
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_configuracion, container, false);

        prefs     = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        tm        = ThemeManager.get(requireContext());
        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null
                ? getArguments().getLong("usuario_id", -1) : -1;

        bindVistas(v);
        cargarDatosUsuario();
        cargarPreferencias();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configurarListeners();
    }

    private void bindVistas(View v) {
        imgAvatar           = v.findViewById(R.id.img_avatar);
        txtAvatarIniciales  = v.findViewById(R.id.txt_avatar_iniciales);
        txtPerfilNombre     = v.findViewById(R.id.txt_perfil_nombre);
        txtPerfilCargo      = v.findViewById(R.id.txt_perfil_cargo);
        txtPerfilEmail      = v.findViewById(R.id.txt_perfil_email);
        txtPerfilRol        = v.findViewById(R.id.txt_perfil_rol);
        rowEditarPerfil     = v.findViewById(R.id.row_editar_perfil);
        rowCambiarPassword  = v.findViewById(R.id.row_cambiar_password);
        rowPinAcceso        = v.findViewById(R.id.row_pin_acceso);

        swModoOscuro        = v.findViewById(R.id.switch_modo_oscuro);
        swAltoContraste     = v.findViewById(R.id.switch_alto_contraste);
        txtModoOscuroEstado = v.findViewById(R.id.txt_modo_oscuro_estado);
        rowColorTema        = v.findViewById(R.id.row_color_tema);
        rowTamanoTexto      = v.findViewById(R.id.row_tamano_texto);
        rowIdioma           = v.findViewById(R.id.row_idioma);

        swNotificaciones    = v.findViewById(R.id.switch_notificaciones);
        swNotifPeligrosos   = v.findViewById(R.id.switch_notif_peligrosos);
        swRecordatorio      = v.findViewById(R.id.switch_recordatorio);
        swVibracion         = v.findViewById(R.id.switch_vibracion);
        txtHoraRecordatorio = v.findViewById(R.id.txt_hora_recordatorio);
        rowSonidoNotif      = v.findViewById(R.id.row_sonido_notif);

        swBiometria         = v.findViewById(R.id.switch_biometria);
        rowConfigurarPin    = v.findViewById(R.id.row_configurar_pin);
        rowSesionAuto       = v.findViewById(R.id.row_sesion_auto);
        rowRegistroActividad= v.findViewById(R.id.row_registro_actividad);

        txtStorageUsado     = v.findViewById(R.id.txt_storage_usado);
        txtUltimoBackup     = v.findViewById(R.id.txt_ultimo_backup);
        progressStorage     = v.findViewById(R.id.progress_storage);
        swAutoBackup        = v.findViewById(R.id.switch_auto_backup);
        rowLimpiarCache     = v.findViewById(R.id.row_limpiar_cache);
        rowExportarBd       = v.findViewById(R.id.row_exportar_bd);
        rowRestaurarBd      = v.findViewById(R.id.row_restaurar_bd);

        indicatorSync       = v.findViewById(R.id.indicator_sync);
        txtSyncEstado       = v.findViewById(R.id.txt_sync_estado);
        txtSyncUltima       = v.findViewById(R.id.txt_sync_ultima);
        btnSyncAhora        = v.findViewById(R.id.btn_sync_ahora);
        swSoloWifi          = v.findViewById(R.id.switch_solo_wifi);
        rowFrecuenciaSync   = v.findViewById(R.id.row_frecuencia_sync);
        rowServidorUrl      = v.findViewById(R.id.row_servidor_url);

        seekbarFontSize        = v.findViewById(R.id.seekbar_font_size);
        txtFontSizeLabel       = v.findViewById(R.id.txt_font_size_label);
        swReducirAnimaciones   = v.findViewById(R.id.switch_reducir_animaciones);
        swTalkBack             = v.findViewById(R.id.switch_talkback);

        rowVersion    = v.findViewById(R.id.row_version);
        rowNovedades  = v.findViewById(R.id.row_novedades);
        rowTerminos   = v.findViewById(R.id.row_terminos);
        rowPrivacidad = v.findViewById(R.id.row_privacidad);
        rowLicencias  = v.findViewById(R.id.row_licencias);
        rowSoporte    = v.findViewById(R.id.row_soporte);
        rowCalificar  = v.findViewById(R.id.row_calificar);
        txtFooterVersion = v.findViewById(R.id.txt_footer_version);

        rowCerrarSesion  = v.findViewById(R.id.row_cerrar_sesion);
        rowEliminarCuenta= v.findViewById(R.id.row_eliminar_cuenta);

        // Textos iniciales de las filas
        setRow(rowEditarPerfil,       "Editar perfil",                 "Nombre, cargo, email");
        setRow(rowCambiarPassword,    "Cambiar contraseña",            "Actualizar credenciales");
        setRow(rowPinAcceso,          "PIN de acceso rápido",          pinLabel());
        setRow(rowColorTema,          "Color del tema",                temaActual());
        setRow(rowTamanoTexto,        "Tamaño de texto",               fontLabel(prefs.getInt("font_size", 2)));
        setRow(rowIdioma,             "Idioma",                        "Español (Perú)");
        setRow(rowSonidoNotif,        "Sonido de notificaciones",      sonidoActual());
        setRow(rowConfigurarPin,      "Configurar PIN",                pinLabel());
        setRow(rowSesionAuto,         "Cierre de sesión automático",   sesionAutoLabel());
        setRow(rowRegistroActividad,  "Registro de actividad",         "Ver historial de accesos");
        setRow(rowLimpiarCache,       "Limpiar caché",                 "Liberar espacio temporal");
        setRow(rowExportarBd,         "Exportar base de datos",        "Guardar copia .db");
        setRow(rowRestaurarBd,        "Restaurar copia de seguridad",  "Desde archivo .db");
        setRow(rowFrecuenciaSync,     "Frecuencia de sync",            frecuenciaLabel());
        setRow(rowServidorUrl,        "URL del servidor",              prefs.getString("servidor_url", "No configurado"));
        setRow(rowVersion,            "Versión",                       "1.0.0 (Build 1)");
        setRow(rowNovedades,          "¿Qué hay de nuevo?",            "Ver últimos cambios");
        setRow(rowTerminos,           "Términos de uso",               null);
        setRow(rowPrivacidad,         "Política de privacidad",        null);
        setRow(rowLicencias,          "Licencias open source",         null);
        setRow(rowSoporte,            "Contactar soporte",             "ecolim.pe");
        setRow(rowCalificar,          "Calificar en Play Store",       null);
        txtFooterVersion.setText("ECOLIM v1.0.0  •  Android " + Build.VERSION.RELEASE
                + "  •  " + Build.MANUFACTURER);
    }

    private void cargarDatosUsuario() {
        Usuario u = db.obtenerUsuarioPorId(usuarioId);
        if (u == null) return;

        txtPerfilNombre.setText(nvl(u.getNombre(), "") + " " + nvl(u.getApellido(), ""));
        txtPerfilCargo.setText(nvl(u.getCargo(), "Sin cargo"));
        txtPerfilEmail.setText(nvl(u.getEmail(), "Sin email"));
        if (txtPerfilRol != null)
            txtPerfilRol.setText(nvl(u.getRol(), nvl(u.getCargo(), "Operario")));
        txtAvatarIniciales.setText(inicial(u.getNombre()) + inicial(u.getApellido()));

        File fotoFile = new File(requireContext().getFilesDir(), FOTO_FILENAME);
        if (fotoFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(fotoFile.getAbsolutePath());
            if (bmp != null) {
                imgAvatar.setImageBitmap(circleCrop(bmp));
                txtAvatarIniciales.setVisibility(View.INVISIBLE);
            }
        } else {
            txtAvatarIniciales.setVisibility(View.VISIBLE);
        }
        calcularStorage();
    }

    private void cargarPreferencias() {
        boolean dark = tm.isModoOscuro();
        swModoOscuro.setChecked(dark);
        txtModoOscuroEstado.setText(dark ? "Activado" : "Desactivado");

        swAltoContraste.setChecked(tm.isAltoContraste());

        swNotificaciones.setChecked(prefs.getBoolean("notificaciones", true));
        swNotifPeligrosos.setChecked(prefs.getBoolean("notif_peligrosos", true));
        swRecordatorio.setChecked(prefs.getBoolean("recordatorio", false));
        swVibracion.setChecked(prefs.getBoolean("vibracion", true));
        txtHoraRecordatorio.setText(prefs.getString("hora_recordatorio", "08:00 AM"));

        swBiometria.setChecked(prefs.getBoolean("biometria", false));
        swAutoBackup.setChecked(prefs.getBoolean("auto_backup", false));
        String ub = prefs.getString("ultimo_backup", null);
        txtUltimoBackup.setText(ub != null ? "Último: " + ub : "Último: nunca");

        swSoloWifi.setChecked(prefs.getBoolean("sync_solo_wifi", true));

        int fs = prefs.getInt("font_size", 2);
        seekbarFontSize.setProgress(fs);
        txtFontSizeLabel.setText(fontLabel(fs));

        swReducirAnimaciones.setChecked(prefs.getBoolean("reducir_animaciones", false));
        swTalkBack.setChecked(prefs.getBoolean("talkback", false));
    }

    private void configurarListeners() {

        // ── Foto de perfil ────────────────────────────────────────────────
        View fab = requireView().findViewById(R.id.fab_cambiar_foto);
        if (fab != null) fab.setOnClickListener(x -> pedirPermisoFoto());
        if (imgAvatar != null) imgAvatar.setOnClickListener(x -> pedirPermisoFoto());

        // ── Perfil ────────────────────────────────────────────────────────
        click(rowEditarPerfil,    this::dialogEditarPerfil);
        click(rowCambiarPassword, this::dialogCambiarPassword);
        click(rowPinAcceso,       this::dialogConfigurarPin);

        swModoOscuro.setOnCheckedChangeListener((btn, on) -> {
            if (!btn.isPressed()) return;
            txtModoOscuroEstado.setText(on ? "Activado" : "Desactivado");
            tm.setModoOscuro(on);
            requireActivity().recreate();
        });

        swAltoContraste.setOnCheckedChangeListener((btn, on) -> {
            if (!btn.isPressed()) return;
            tm.setAltoContraste(on);
            requireActivity().recreate();
        });

        click(rowColorTema,    this::dialogColorTema);
        click(rowTamanoTexto,  this::dialogTamanoTexto);

        seekbarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user) {
                    txtFontSizeLabel.setText(fontLabel(p));
                    setRow(rowTamanoTexto, "Tamaño de texto", fontLabel(p));
                }
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                prefs.edit().putInt("font_size", sb.getProgress()).apply();
                requireActivity().recreate();
            }
        });

        click(rowIdioma, this::dialogIdioma);

        // ── Notificaciones ────────────────────────────────────────────────
        swNotificaciones.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("notificaciones", on).apply();
            swNotifPeligrosos.setEnabled(on);
            swRecordatorio.setEnabled(on);
            swVibracion.setEnabled(on);
        });
        swNotifPeligrosos.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("notif_peligrosos", on).apply());
        swRecordatorio.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("recordatorio", on).apply();
            if (on) dialogHoraRecordatorio();
        });
        swVibracion.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("vibracion", on).apply());
        click(rowSonidoNotif, this::dialogSonido);

        // ── Seguridad ─────────────────────────────────────────────────────
        swBiometria.setOnCheckedChangeListener((btn, on) -> {
            if (on) verificarBiometria();
            else {
                prefs.edit().putBoolean("biometria", false).apply();
                toast("Biometría desactivada");
            }
        });
        click(rowConfigurarPin,      this::dialogConfigurarPin);
        click(rowSesionAuto,         this::dialogSesionAuto);
        click(rowRegistroActividad,  this::mostrarRegistroActividad);

        // ── Datos ─────────────────────────────────────────────────────────
        swAutoBackup.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("auto_backup", on).apply());
        click(rowLimpiarCache, this::dialogLimpiarCache);
        click(rowExportarBd,   this::exportarBd);
        click(rowRestaurarBd,  this::dialogRestaurar);

        // ── Sync ──────────────────────────────────────────────────────────
        btnSyncAhora.setOnClickListener(x -> sincronizarAhora());
        swSoloWifi.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("sync_solo_wifi", on).apply());
        click(rowFrecuenciaSync, this::dialogFrecuenciaSync);
        click(rowServidorUrl,    this::dialogServidorUrl);

        // ── Accesibilidad extra ───────────────────────────────────────────
        swReducirAnimaciones.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("reducir_animaciones", on).apply());
        swTalkBack.setOnCheckedChangeListener((btn, on) ->
                prefs.edit().putBoolean("talkback", on).apply());

        // ── Acerca de ─────────────────────────────────────────────────────
        click(rowVersion,    this::dialogVersionInfo);
        click(rowNovedades,  this::dialogNovedades);
        // ✅ URLs corregidas
        click(rowTerminos,   () -> abrirUrl(URL_TERMINOS));
        click(rowPrivacidad, () -> abrirUrl(URL_PRIVACIDAD));
        click(rowLicencias,  this::dialogLicencias);
        click(rowSoporte,    () -> abrirUrl(URL_SOPORTE));
        click(rowCalificar,  () -> toast("Disponible al publicar en Play Store"));

        // ── Zona peligro ──────────────────────────────────────────────────
        click(rowCerrarSesion,    this::dialogCerrarSesion);
        click(rowEliminarCuenta,  this::dialogEliminarCuenta);
    }

    private void dialogColorTema() {
        String[] opciones = {
                "🟢  Verde ECOLIM (predeterminado)",
                "🔵  Azul corporativo",
                "🟠  Naranja energético",
                "⚫  Gris profesional"
        };
        int selActual = tm.getColorTema();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Color del tema")
                .setSingleChoiceItems(opciones, selActual, (dialog, which) -> {
                    tm.setColorTema(which);
                    setRow(rowColorTema, "Color del tema", opciones[which].substring(4));
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void dialogTamanoTexto() {
        String[] labels = {"Muy pequeño (75%)", "Pequeño (87%)", "Normal (100%)",
                "Grande (115%)", "Muy grande (130%)"};
        int sel = prefs.getInt("font_size", 2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tamaño de texto")
                .setSingleChoiceItems(labels, sel, (d, which) -> {
                    prefs.edit().putInt("font_size", which).apply();
                    seekbarFontSize.setProgress(which);
                    txtFontSizeLabel.setText(fontLabel(which));
                    setRow(rowTamanoTexto, "Tamaño de texto", fontLabel(which));
                    d.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void pedirPermisoFoto() {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permiso)
                == PackageManager.PERMISSION_GRANTED) {
            mostrarOpcionesFoto();
        } else {
            permissionLauncher.launch(permiso);
        }
    }

    private void mostrarOpcionesFoto() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Foto de perfil")
                .setItems(new String[]{"Seleccionar de galería", "Tomar foto",
                        "Eliminar foto actual"}, (d, which) -> {
                    if      (which == 0) abrirGaleria();
                    else if (which == 1) abrirCamara();
                    else                 eliminarFoto();
                }).show();
    }

    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        galleryLauncher.launch(i);
    }

    private void abrirCamara() {
        galleryLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void onFotoSeleccionada(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        try {
            Bitmap bitmap;
            if (result.getData().getData() == null && result.getData().getExtras() != null) {
                bitmap = (Bitmap) result.getData().getExtras().get("data");
            } else {
                Uri uri = result.getData().getData();
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
            }
            if (bitmap == null) { toast("No se pudo cargar la imagen"); return; }
            bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
            File fotoFile = new File(requireContext().getFilesDir(), FOTO_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(fotoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            imgAvatar.setImageBitmap(circleCrop(bitmap));
            txtAvatarIniciales.setVisibility(View.INVISIBLE);
            prefs.edit().putBoolean("tiene_foto", true).apply();
            toast("✔ Foto de perfil actualizada");
        } catch (Exception e) {
            toast("Error al guardar foto: " + e.getMessage());
        }
    }

    private void eliminarFoto() {
        File f = new File(requireContext().getFilesDir(), FOTO_FILENAME);
        if (f.exists()) f.delete();
        imgAvatar.setImageResource(R.drawable.bg_avatar_circle);
        txtAvatarIniciales.setVisibility(View.VISIBLE);
        prefs.edit().putBoolean("tiene_foto", false).apply();
        toast("Foto eliminada");
    }

    private Bitmap circleCrop(Bitmap src) {
        int size   = Math.min(src.getWidth(), src.getHeight());
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(out);
        Paint  p   = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;
        c.drawBitmap(src, new Rect(x, y, x + size, y + size),
                new Rect(0, 0, size, size), p);
        return out;
    }

    private void dialogEditarPerfil() {
        Usuario u = db.obtenerUsuarioPorId(usuarioId);
        if (u == null) return;
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_editar_perfil, null);
        TextInputEditText edtN = form.findViewById(R.id.edt_dialog_nombre);
        TextInputEditText edtA = form.findViewById(R.id.edt_dialog_apellido);
        TextInputEditText edtC = form.findViewById(R.id.edt_dialog_cargo);
        TextInputEditText edtE = form.findViewById(R.id.edt_dialog_email);
        edtN.setText(u.getNombre());
        edtA.setText(u.getApellido());
        edtC.setText(u.getCargo());
        edtE.setText(u.getEmail());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar perfil").setView(form)
                .setPositiveButton("Guardar", (d, w) -> {
                    u.setNombre(str(edtN));
                    u.setApellido(str(edtA));
                    u.setCargo(str(edtC));
                    u.setEmail(str(edtE));
                    db.actualizarUsuario(u);
                    cargarDatosUsuario();
                    toast("✔ Perfil actualizado");
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogCambiarPassword() {
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_cambiar_password, null);
        TextInputEditText edtActual  = form.findViewById(R.id.edt_password_actual);
        TextInputEditText edtNueva   = form.findViewById(R.id.edt_password_nueva);
        TextInputEditText edtConfirm = form.findViewById(R.id.edt_password_confirmar);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar contraseña").setView(form)
                .setPositiveButton("Cambiar", (d, w) -> {
                    String actual  = str(edtActual);
                    String nueva   = str(edtNueva);
                    String confirm = str(edtConfirm);
                    if (nueva.length() < 6) { toast("Mínimo 6 caracteres"); return; }
                    if (!nueva.equals(confirm)) { toast("Las contraseñas no coinciden"); return; }
                    Usuario u = db.obtenerUsuarioPorId(usuarioId);
                    if (u != null && actual.equals(u.getPassword())) {
                        u.setPassword(nueva);
                        db.actualizarUsuario(u);
                        toast("✔ Contraseña actualizada");
                    } else toast("Contraseña actual incorrecta");
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogConfigurarPin() {
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pin, null);
        TextInputEditText edtPin1 = form.findViewById(R.id.edt_pin_nuevo);
        TextInputEditText edtPin2 = form.findViewById(R.id.edt_pin_confirmar);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("PIN de acceso rápido")
                .setMessage("Ingresa 4 dígitos numéricos")
                .setView(form)
                .setPositiveButton("Guardar PIN", (d, w) -> {
                    String p1 = str(edtPin1), p2 = str(edtPin2);
                    if (!p1.matches("\\d{4}")) { toast("Debe ser exactamente 4 dígitos"); return; }
                    if (!p1.equals(p2)) { toast("Los PINs no coinciden"); return; }
                    prefs.edit().putString("pin", p1).apply();
                    setRow(rowPinAcceso, "PIN de acceso rápido", "Configurado ✔");
                    setRow(rowConfigurarPin, "Configurar PIN", "PIN activo ✔");
                    toast("✔ PIN guardado");
                })
                .setNeutralButton("Eliminar PIN", (d, w) -> {
                    prefs.edit().remove("pin").apply();
                    setRow(rowPinAcceso, "PIN de acceso rápido", "No configurado");
                    setRow(rowConfigurarPin, "Configurar PIN", "Sin PIN");
                    toast("PIN eliminado");
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogIdioma() {
        String[] idiomas = {"Español (Perú)", "Español (España)", "Inglés (US)", "Português"};
        int sel = prefs.getInt("idioma", 0);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Idioma")
                .setSingleChoiceItems(idiomas, sel, (d, which) -> {
                    prefs.edit().putInt("idioma", which).apply();
                    toast("Idioma: " + idiomas[which] + "\nReinicia la app para aplicar");
                    d.dismiss();
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void dialogHoraRecordatorio() {
        new TimePickerDialog(requireContext(), (tp, h, m) -> {
            String hora = String.format(Locale.getDefault(),
                    "%02d:%02d %s", h > 12 ? h - 12 : h == 0 ? 12 : h, m, h >= 12 ? "PM" : "AM");
            txtHoraRecordatorio.setText(hora);
            prefs.edit().putString("hora_recordatorio", hora).apply();
            toast("✔ Recordatorio a las " + hora);
        }, 8, 0, false).show();
    }

    private void dialogSonido() {
        String[] sonidos = {"Predeterminado", "Campana suave", "Alerta", "Silencio"};
        int sel = prefs.getInt("sonido_notif", 0);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sonido de notificaciones")
                .setSingleChoiceItems(sonidos, sel, (d, which) -> {
                    prefs.edit().putInt("sonido_notif", which).apply();
                    setRow(rowSonidoNotif, "Sonido de notificaciones", sonidos[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void verificarBiometria() {
        BiometricManager bm = BiometricManager.from(requireContext());
        int result = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (result == BiometricManager.BIOMETRIC_SUCCESS) {
            prefs.edit().putBoolean("biometria", true).apply();
            toast("✔ Biometría activada");
        } else {
            swBiometria.setChecked(false);
            toast(result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    ? "No hay huellas registradas. Ve a Ajustes del sistema."
                    : "Este dispositivo no es compatible con biometría.");
        }
    }

    private void dialogSesionAuto() {
        String[] opts = {"5 minutos", "15 minutos", "30 minutos", "1 hora", "Nunca"};
        int sel = prefs.getInt("sesion_auto", 2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cierre automático de sesión")
                .setSingleChoiceItems(opts, sel, (d, which) -> {
                    prefs.edit().putInt("sesion_auto", which).apply();
                    setRow(rowSesionAuto, "Cierre de sesión automático",
                            which == 4 ? "Nunca" : "Después de " + opts[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void mostrarRegistroActividad() {
        String log = "• Última sesión: " + prefs.getString("hora_login", sdf("dd/MM/yyyy HH:mm")) + "\n"
                + "• Usuario ID: " + usuarioId + "\n"
                + "• Apertura: " + sdf("dd/MM/yyyy HH:mm") + "\n"
                + prefs.getString("log_actividad", "• Sin actividad adicional registrada");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Registro de actividad")
                .setMessage(log)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Limpiar registro", (d, w) -> {
                    prefs.edit().remove("log_actividad").apply();
                    toast("Registro limpiado");
                }).show();
    }

    private void dialogLimpiarCache() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Limpiar caché")
                .setMessage("Se eliminarán archivos temporales. Los datos no se borrarán.")
                .setPositiveButton("Limpiar", (d, w) -> {
                    try {
                        borrarDir(requireContext().getCacheDir());
                        calcularStorage();
                        toast("✔ Caché eliminada");
                    } catch (Exception e) {
                        toast("Error: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void exportarBd() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Exportar base de datos")
                .setMessage("Se guardará una copia en la carpeta Documentos.")
                .setPositiveButton("Exportar", (d, w) -> {
                    try {
                        File src  = requireContext().getDatabasePath("ecolim.db");
                        File dir  = requireContext().getExternalFilesDir(
                                android.os.Environment.DIRECTORY_DOCUMENTS);
                        if (dir != null && !dir.exists()) dir.mkdirs();
                        File dst  = new File(dir, "ECOLIM_" + sdf("yyyyMMdd_HHmmss") + ".db");
                        copiar(src, dst);
                        String hoy = sdf("dd/MM/yyyy HH:mm");
                        prefs.edit().putString("ultimo_backup", hoy).apply();
                        txtUltimoBackup.setText("Último: " + hoy);
                        toast("✔ BD exportada a " + dst.getName());
                    } catch (Exception e) {
                        toast("Error: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogRestaurar() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠ Restaurar copia")
                .setMessage("Esto REEMPLAZARÁ todos los datos actuales. ¿Continuar?")
                .setPositiveButton("Restaurar", (d, w) ->
                        toast("Selecciona el archivo .db desde el explorador"))
                .setNegativeButton("Cancelar", null).show();
    }

    private void sincronizarAhora() {
        btnSyncAhora.setEnabled(false);
        txtSyncEstado.setText("Sincronizando…");
        btnSyncAhora.postDelayed(() -> {
            String ahora = sdf("dd/MM/yyyy HH:mm");
            txtSyncEstado.setText("Sincronizado");
            txtSyncUltima.setText("Última sync: " + ahora);
            prefs.edit().putString("ultima_sync", ahora).apply();
            btnSyncAhora.setEnabled(true);
            toast("✔ Sincronización completada");
        }, 2000);
    }

    private void dialogFrecuenciaSync() {
        String[] opts = {"Manual", "Cada 5 min", "Cada 15 min", "Cada 30 min", "Cada hora"};
        int sel = prefs.getInt("frecuencia_sync", 2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Frecuencia de sincronización")
                .setSingleChoiceItems(opts, sel, (d, which) -> {
                    prefs.edit().putInt("frecuencia_sync", which).apply();
                    setRow(rowFrecuenciaSync, "Frecuencia de sync", opts[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void dialogServidorUrl() {
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_servidor_url, null);
        TextInputEditText edt = form.findViewById(R.id.edt_servidor_url);
        edt.setText(prefs.getString("servidor_url", ""));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("URL del servidor API")
                .setMessage("https://jheyson.42web.io/Ecolim.com/")
                .setView(form)
                .setPositiveButton("Guardar", (d, w) -> {
                    String url = str(edt).trim();
                    if (url.isEmpty()
                            || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        toast("URL inválida. Debe empezar con http:// o https://");
                        return;
                    }
                    prefs.edit().putString("servidor_url", url).apply();
                    setRow(rowServidorUrl, "URL del servidor", url);
                    toast("✔ Servidor guardado");
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogVersionInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Información de versión")
                .setMessage("ECOLIM v1.0.0 (Build 1)\n\n"
                        + "Gestión de Residuos Sólidos — NTP 900.058\n\n"
                        + "Android: " + Build.VERSION.RELEASE
                        + " (API " + Build.VERSION.SDK_INT + ")\n"
                        + "Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL
                        + "\n\n© 2025 ECOLIM S.A.C.")
                .setPositiveButton("Cerrar", null).show();
    }

    private void dialogNovedades() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Novedades v1.0.0")
                .setMessage("• Dashboard con gráficos MPAndroidChart\n"
                        + "• Exportación Excel XLSX nativo y PDF\n"
                        + "• 11 tipos de residuos NTP 900.058\n"
                        + "• Sistema de temas dinámico (4 colores)\n"
                        + "• Modo oscuro real y alto contraste\n"
                        + "• Tamaño de fuente ajustable globalmente\n"
                        + "• Seguridad biométrica\n"
                        + "• Historial con filtros avanzados")
                .setPositiveButton("Cerrar", null).show();
    }

    private void dialogLicencias() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Licencias open source")
                .setMessage("• Material Components for Android — Apache 2.0\n"
                        + "• AndroidX Libraries — Apache 2.0\n"
                        + "• MPAndroidChart v3.1.0 — Apache 2.0\n"
                        + "• AndroidX Biometric — Apache 2.0")
                .setPositiveButton("Cerrar", null).show();
    }

    private void abrirUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            toast("No se pudo abrir el enlace: " + url);
        }
    }

    private void dialogCerrarSesion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Cerrar sesión", (d, w) -> {
                    prefs.edit()
                            .remove("usuario_id")
                            .remove("sesion_activa")
                            .remove("hora_login")
                            .apply();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogEliminarCuenta() {
        View form = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_eliminar_cuenta, null);
        TextInputEditText edtC = form.findViewById(R.id.edt_confirmar_eliminar);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠ Eliminar cuenta")
                .setMessage("Escribe ELIMINAR para confirmar. Esta acción es IRREVERSIBLE.")
                .setView(form)
                .setPositiveButton("Eliminar definitivamente", (d, w) -> {
                    if ("ELIMINAR".equals(str(edtC))) {
                        db.eliminarUsuario(usuarioId);
                        prefs.edit().clear().apply();
                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else toast("Texto incorrecto. Escribe exactamente: ELIMINAR");
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────
    private void setRow(View row, String titulo, String subtitulo) {
        if (row == null) return;
        TextView t = row.findViewById(R.id.txt_row_titulo);
        TextView s = row.findViewById(R.id.txt_row_subtitulo);
        if (t != null) t.setText(titulo);
        if (s != null) {
            if (subtitulo != null && !subtitulo.isEmpty()) {
                s.setText(subtitulo);
                s.setVisibility(View.VISIBLE);
            } else {
                s.setVisibility(View.GONE);
            }
        }
    }

    private void click(View v, Runnable r) {
        if (v != null) v.setOnClickListener(x -> r.run());
    }

    private String str(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String nvl(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private String inicial(String s) {
        return (s == null || s.isEmpty()) ? "?" : String.valueOf(s.charAt(0)).toUpperCase();
    }

    private String sdf(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    private String pinLabel() {
        return prefs.getString("pin", "").isEmpty() ? "No configurado" : "Configurado ✔";
    }

    private String fontLabel(int n) {
        String[] labels = {"Muy pequeño", "Pequeño", "Normal", "Grande", "Muy grande"};
        return (n >= 0 && n < labels.length) ? labels[n] : "Normal";
    }

    private String temaActual() {
        String[] temas = {"Verde ECOLIM", "Azul corporativo",
                "Naranja energético", "Gris profesional"};
        int i = tm.getColorTema();
        return (i >= 0 && i < temas.length) ? temas[i] : "Verde ECOLIM";
    }

    private String sonidoActual() {
        String[] s = {"Predeterminado", "Campana suave", "Alerta", "Silencio"};
        int i = prefs.getInt("sonido_notif", 0);
        return (i >= 0 && i < s.length) ? s[i] : "Predeterminado";
    }

    private String frecuenciaLabel() {
        String[] f = {"Manual", "Cada 5 min", "Cada 15 min", "Cada 30 min", "Cada hora"};
        int i = prefs.getInt("frecuencia_sync", 2);
        return (i >= 0 && i < f.length) ? f[i] : "Cada 15 min";
    }

    private String sesionAutoLabel() {
        String[] s = {"5 minutos", "15 minutos", "30 minutos", "1 hora", "Nunca"};
        int i = prefs.getInt("sesion_auto", 2);
        return (i == 4) ? "Nunca" : "Después de " + ((i >= 0 && i < s.length) ? s[i] : "30 minutos");
    }

    private void calcularStorage() {
        try {
            long dbSize  = requireContext().getDatabasePath("ecolim.db").length();
            long cache   = tamDir(requireContext().getCacheDir());
            long total   = dbSize + cache;
            txtStorageUsado.setText(fmtSize(total));
            if (progressStorage != null)
                progressStorage.setProgress((int) Math.max(1, Math.min(total / (1024 * 512), 100)));
        } catch (Exception ignored) { }
    }

    private long tamDir(File d) {
        if (d == null) return 0;
        long size = 0;
        File[] files = d.listFiles();
        if (files != null)
            for (File f : files)
                size += f.isDirectory() ? tamDir(f) : f.length();
        return size;
    }

    private void borrarDir(File d) {
        if (d == null) return;
        File[] files = d.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) borrarDir(f);
                else f.delete();
            }
    }

    private void copiar(File src, File dst) throws Exception {
        try (InputStream  in  = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private String fmtSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0);
    }
}