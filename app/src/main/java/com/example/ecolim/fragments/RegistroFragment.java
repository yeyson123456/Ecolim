package com.example.ecolim.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Residuo;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
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

    // Foto
    private ImageView imgFotoResiduo;
    private Uri       fotoUri;
    private String    rutaFotoActual = "";

    // Campos del formulario
    private TextInputLayout      layoutTipo, layoutCategoria, layoutCantidad,
            layoutUnidad, layoutUbicacion, layoutFecha, layoutHora;
    private AutoCompleteTextView ddTipoResiduo, ddUnidad, ddUbicacion;
    private TextInputEditText    edtCantidad, edtFecha, edtHora,
            edtObservaciones, edtCategoria;
    private MaterialButton       btnGuardar, btnLimpiar, btnCamara, btnGaleria;

    private static final String[] ZONAS = {
            "Área de Producción", "Área Administrativa", "Almacén General",
            "Almacén de Químicos", "Comedor", "Baños / Servicios Higiénicos",
            "Estacionamiento", "Área de Carga y Descarga", "Laboratorio",
            "Sala de Reuniones", "Pasillo Principal", "Otra zona"
    };

    // Launcher para cámara
    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), exito -> {
                if (exito && fotoUri != null) {
                    imgFotoResiduo.setImageURI(fotoUri);
                    rutaFotoActual = fotoUri.toString();
                }
            });

    // Launcher para galería
    private final ActivityResultLauncher<String> launcherGaleria =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imgFotoResiduo.setImageURI(uri);
                    rutaFotoActual = uri.toString();
                    fotoUri = uri;
                }
            });

    // Launcher para permiso de cámara
    private final ActivityResultLauncher<String> launcherPermisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) abrirCamara();
                else Toast.makeText(requireContext(),
                        "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            });

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

        // Foto
        imgFotoResiduo   = v.findViewById(R.id.img_foto_residuo);
        btnCamara        = v.findViewById(R.id.btn_camara);
        btnGaleria       = v.findViewById(R.id.btn_galeria);
    }

    private void configurarDropdowns() {
        listaResiduos = db.obtenerTodosResiduos();
        List<String> nombres = new ArrayList<>();
        for (Residuo r : listaResiduos) nombres.add(r.getNombre());

        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombres);
        ddTipoResiduo.setAdapter(adapterTipo);

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

    private void configurarFechaHora() {
        edtFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (picker, year, month, day) -> {
                        String fecha = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, day);
                        edtFecha.setText(fecha);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        edtHora.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(),
                    (picker, hour, minute) -> {
                        String hora = String.format(Locale.getDefault(),
                                "%02d:%02d", hour, minute);
                        edtHora.setText(hora);
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true).show();
        });
    }

    private void setFechaHoraActual() {
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora  = new SimpleDateFormat("HH:mm",      Locale.getDefault()).format(new Date());
        edtFecha.setText(fecha);
        edtHora.setText(hora);
    }

    private void configurarBotones() {
        btnGuardar.setOnClickListener(v -> guardarRegistro());
        btnLimpiar.setOnClickListener(v -> limpiarFormulario());

        btnCamara.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                launcherPermisoCamara.launch(android.Manifest.permission.CAMERA);
            }
        });

        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));
    }

    private void abrirCamara() {
        try {
            File foto = crearArchivoFoto();
            fotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    foto);
            launcherCamara.launch(fotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String nombre = "ECOLIM_" + timestamp;
        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File foto = File.createTempFile(nombre, ".jpg", dir);
        rutaFotoActual = foto.getAbsolutePath();
        return foto;
    }

    private void guardarRegistro() {
        if (residuoSeleccionado == null) {
            layoutTipo.setError("Seleccione el tipo de residuo");
            return;
        }
        layoutTipo.setError(null);

        String cantidadStr = txt(edtCantidad);
        if (cantidadStr.isEmpty()) {
            layoutCantidad.setError("Ingrese la cantidad");
            edtCantidad.requestFocus();
            return;
        }
        layoutCantidad.setError(null);

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            layoutCantidad.setError("Cantidad inválida");
            return;
        }

        String unidad = ddUnidad.getText().toString().trim();
        if (unidad.isEmpty()) {
            layoutUnidad.setError("Seleccione la unidad");
            return;
        }
        layoutUnidad.setError(null);

        String ubicacion = ddUbicacion.getText().toString().trim();
        if (ubicacion.isEmpty()) {
            layoutUbicacion.setError("Seleccione o escriba la ubicación");
            return;
        }
        layoutUbicacion.setError(null);

        String fecha = txt(edtFecha);
        String hora  = txt(edtHora);
        if (fecha.isEmpty() || hora.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Indique fecha y hora", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario u = db.obtenerUsuarioPorId(usuarioId);
        String nombreU = u != null ? u.getNombreCompleto() : "Desconocido";

        Registro registro = new Registro(
                usuarioId,
                nombreU,
                residuoSeleccionado.getId(),
                residuoSeleccionado.getNombre(),
                residuoSeleccionado.getCategoria(),
                cantidad,
                unidad,
                ubicacion,
                txt(edtObservaciones),
                fecha,
                hora,
                rutaFotoActual  // ← aquí se guarda la ruta de la foto
        );

        long id = db.insertarRegistro(registro);

        if (id != -1) {
            Toast.makeText(requireContext(),
                    "Registro guardado exitosamente", Toast.LENGTH_SHORT).show();
            limpiarFormulario();
        } else {
            Toast.makeText(requireContext(),
                    "Error al guardar. Intente nuevamente.", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarFormulario() {
        ddTipoResiduo.setText("");
        ddUnidad.setText("");
        ddUbicacion.setText("");
        edtCantidad.setText("");
        edtObservaciones.setText("");
        edtCategoria.setText("");
        residuoSeleccionado = null;
        rutaFotoActual = "";
        fotoUri = null;
        imgFotoResiduo.setImageResource(android.R.drawable.ic_menu_camera);
        layoutCategoria.setBackgroundColor(0x00000000);
        setFechaHoraActual();
        layoutTipo.setError(null);
        layoutCantidad.setError(null);
        layoutUnidad.setError(null);
        layoutUbicacion.setError(null);
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}