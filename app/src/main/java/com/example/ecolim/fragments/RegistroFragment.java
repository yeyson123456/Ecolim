package com.example.ecolim.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.ecolim.AlertaMLService;
import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Residuo;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistroFragment extends Fragment {

    private EcolimDbHelper db;
    private long           usuarioId;
    private List<Residuo>  listaResiduos;
    private Residuo        residuoSeleccionado;

    // ── Foto ──────────────────────────────────────────────────────────────────
    private ImageView imgFotoResiduo;
    private Uri       uriFotoTemporal;
    private String    rutaFotoGuardada = "";

    // ── Campos ────────────────────────────────────────────────────────────────
    private TextInputLayout      layoutTipo, layoutCategoria, layoutCantidad,
            layoutUnidad, layoutUbicacion, layoutFecha, layoutHora;
    private AutoCompleteTextView ddTipoResiduo, ddUnidad, ddUbicacion;
    private TextInputEditText    edtCantidad, edtFecha, edtHora,
            edtObservaciones, edtCategoria;
    private MaterialButton       btnGuardar, btnLimpiar, btnFoto;

    private static final String[] ZONAS = {
            "Área de Producción", "Área Administrativa", "Almacén General",
            "Almacén de Químicos", "Comedor", "Baños / Servicios Higiénicos",
            "Estacionamiento", "Área de Carga y Descarga", "Laboratorio",
            "Sala de Reuniones", "Pasillo Principal", "Otra zona"
    };

    // ══════════════════════════════════════════════════════════════════════════
    //  LAUNCHERS (registrar antes de onCreateView)
    // ══════════════════════════════════════════════════════════════════════════

    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), exito -> {
                if (exito && uriFotoTemporal != null) {
                    rutaFotoGuardada = uriFotoTemporal.toString();
                    mostrarFoto(uriFotoTemporal);
                    toast("Foto capturada");
                }
            });

    private final ActivityResultLauncher<String> launcherGaleria =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    rutaFotoGuardada = uri.toString();
                    mostrarFoto(uri);
                    toast("Foto seleccionada");
                }
            });

    private final ActivityResultLauncher<String> launcherPermiso =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) abrirCamara();
                else toast("Permiso de cámara denegado");
            });

    // ══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);
        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null
                ? getArguments().getLong("usuario_id", -1) : -1;
        iniciarVistas(view);
        configurarDropdowns();
        configurarFechaHora();
        configurarBotones();
        setFechaHoraActual();
        return view;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VISTAS
    // ══════════════════════════════════════════════════════════════════════════

    private void iniciarVistas(View v) {
        layoutTipo       = v.findViewById(R.id.layout_tipo_residuo);
        layoutCategoria  = v.findViewById(R.id.layout_categoria);
        layoutCantidad   = v.findViewById(R.id.layout_cantidad);
        layoutUnidad     = v.findViewById(R.id.layout_unidad);
        layoutUbicacion  = v.findViewById(R.id.layout_ubicacion);
        layoutFecha      = v.findViewById(R.id.layout_fecha);
        layoutHora       = v.findViewById(R.id.layout_hora);
        ddTipoResiduo    = v.findViewById(R.id.dd_tipo_residuo);
        ddUnidad         = v.findViewById(R.id.dd_unidad);
        ddUbicacion      = v.findViewById(R.id.dd_ubicacion);
        edtCantidad      = v.findViewById(R.id.edt_cantidad);
        edtFecha         = v.findViewById(R.id.edt_fecha);
        edtHora          = v.findViewById(R.id.edt_hora);
        edtObservaciones = v.findViewById(R.id.edt_observaciones);
        edtCategoria     = v.findViewById(R.id.edt_categoria);
        btnGuardar       = v.findViewById(R.id.btn_guardar);
        btnLimpiar       = v.findViewById(R.id.btn_limpiar);
        imgFotoResiduo   = v.findViewById(R.id.img_foto_residuo);
        btnFoto          = v.findViewById(R.id.btn_foto);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DROPDOWNS
    // ══════════════════════════════════════════════════════════════════════════

    private void configurarDropdowns() {
        listaResiduos = db.obtenerTodosResiduos();
        List<String> nombres = new ArrayList<>();
        for (Residuo r : listaResiduos) nombres.add(r.getNombre());

        ddTipoResiduo.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, nombres));

        ddTipoResiduo.setOnItemClickListener((parent, view, pos, id) -> {
            residuoSeleccionado = listaResiduos.get(pos);
            edtCategoria.setText(residuoSeleccionado.getCategoria());
            int color;
            switch (residuoSeleccionado.getCategoria()) {
                case "Peligroso": color = 0xFFFFFDE7; break;
                case "Especial":  color = 0xFFF3E5F5; break;
                default:          color = 0xFFE8F5E9; break;
            }
            layoutCategoria.setBackgroundColor(color);
        });

        String[] unidades = {"Kilogramos (kg)", "Litros (L)", "Toneladas (t)", "Unidades"};
        ddUnidad.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, unidades));

        ddUbicacion.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, ZONAS));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FECHA / HORA
    // ══════════════════════════════════════════════════════════════════════════

    private void configurarFechaHora() {
        edtFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (p, y, m, d) -> edtFecha.setText(
                            String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        edtHora.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(),
                    (p, h, min) -> edtHora.setText(
                            String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });
    }

    private void setFechaHoraActual() {
        edtFecha.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        edtHora.setText(new SimpleDateFormat("HH:mm",      Locale.getDefault()).format(new Date()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOTONES
    // ══════════════════════════════════════════════════════════════════════════

    private void configurarBotones() {
        btnGuardar.setOnClickListener(v -> guardarRegistro());
        btnLimpiar.setOnClickListener(v -> limpiarFormulario());
        btnFoto.setOnClickListener(v -> mostrarDialogoFoto());
    }

    private void mostrarDialogoFoto() {
        String[] opciones = rutaFotoGuardada.isEmpty()
                ? new String[]{"Tomar foto", "Seleccionar de galería"}
                : new String[]{"Tomar foto", "Seleccionar de galería", "Eliminar foto"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Foto del residuo")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) pedirPermisoCamara();
                    else if (which == 1) launcherGaleria.launch("image/*");
                    else eliminarFoto();
                }).show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CÁMARA — manejo correcto de FileProvider
    // ══════════════════════════════════════════════════════════════════════════

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            launcherPermiso.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File archivo = crearArchivoFoto();
            // URI segura a través de FileProvider (evita el crash de FileUriExposedException)
            uriFotoTemporal = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo);
            launcherCamara.launch(uriFotoTemporal);
        } catch (IOException e) {
            toast("Error al preparar la cámara: " + e.getMessage());
        }
    }

    private File crearArchivoFoto() throws IOException {
        String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File   dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir != null && !dir.exists()) dir.mkdirs();
        return File.createTempFile("ECOLIM_" + ts, ".jpg", dir);
    }

    private void mostrarFoto(Uri uri) {
        try {
            imgFotoResiduo.setImageURI(null);   // limpiar caché antes
            imgFotoResiduo.setImageURI(uri);
            btnFoto.setText("Cambiar foto");
        } catch (Exception e) {
            toast("No se pudo mostrar la imagen");
        }
    }

    private void eliminarFoto() {
        rutaFotoGuardada = "";
        uriFotoTemporal  = null;
        imgFotoResiduo.setImageResource(android.R.drawable.ic_menu_camera);
        btnFoto.setText("Agregar foto");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARDAR REGISTRO
    // ══════════════════════════════════════════════════════════════════════════

    private void guardarRegistro() {
        if (residuoSeleccionado == null) {
            layoutTipo.setError("Seleccione el tipo de residuo"); return;
        }
        layoutTipo.setError(null);

        String cantidadStr = txt(edtCantidad);
        if (cantidadStr.isEmpty()) {
            layoutCantidad.setError("Ingrese la cantidad");
            edtCantidad.requestFocus(); return;
        }
        layoutCantidad.setError(null);

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            layoutCantidad.setError("Cantidad inválida"); return;
        }

        String unidad = ddUnidad.getText().toString().trim();
        if (unidad.isEmpty()) { layoutUnidad.setError("Seleccione la unidad"); return; }
        layoutUnidad.setError(null);

        String ubicacion = ddUbicacion.getText().toString().trim();
        if (ubicacion.isEmpty()) { layoutUbicacion.setError("Seleccione la ubicación"); return; }
        layoutUbicacion.setError(null);

        String fecha = txt(edtFecha);
        String hora  = txt(edtHora);
        if (fecha.isEmpty() || hora.isEmpty()) { toast("Indique fecha y hora"); return; }

        Usuario u      = db.obtenerUsuarioPorId(usuarioId);
        String nombreU = u != null ? u.getNombreCompleto() : "Desconocido";

        Registro registro = new Registro(
                usuarioId, nombreU,
                residuoSeleccionado.getId(),
                residuoSeleccionado.getNombre(),
                residuoSeleccionado.getCategoria(),
                cantidad, unidad, ubicacion,
                txt(edtObservaciones),
                fecha, hora,
                rutaFotoGuardada   // URI de la foto para mostrar en Historial/Reportes
        );

        long id = db.insertarRegistro(registro);

        if (id != -1) {
            registro.setId(id);
            // ✅ Análisis ML — detecta exceso/peligro → alerta WhatsApp automática
            AlertaMLService.get(requireContext()).analizar(registro);
            toast("Registro guardado exitosamente");
            limpiarFormulario();
        } else {
            toast("Error al guardar. Intente nuevamente.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIMPIAR
    // ══════════════════════════════════════════════════════════════════════════

    private void limpiarFormulario() {
        ddTipoResiduo.setText("");
        ddUnidad.setText("");
        ddUbicacion.setText("");
        edtCantidad.setText("");
        edtObservaciones.setText("");
        edtCategoria.setText("");
        residuoSeleccionado = null;
        layoutCategoria.setBackgroundColor(0x00000000);
        setFechaHoraActual();
        layoutTipo.setError(null);
        layoutCantidad.setError(null);
        layoutUnidad.setError(null);
        layoutUbicacion.setError(null);
        eliminarFoto();
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}