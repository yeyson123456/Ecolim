package com.example.ecolim.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.ecolim.AlertaMLService; // ← NUEVO
import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Residuo;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistroFragment extends Fragment {

    private EcolimDbHelper    db;
    private long              usuarioId;
    private List<Residuo>     listaResiduos;
    private Residuo           residuoSeleccionado;

    // Campos del formulario
    private TextInputLayout       layoutTipo, layoutCategoria, layoutCantidad,
            layoutUnidad, layoutUbicacion, layoutFecha, layoutHora;
    private AutoCompleteTextView  ddTipoResiduo, ddUnidad, ddUbicacion;
    private TextInputEditText     edtCantidad, edtFecha, edtHora, edtObservaciones;
    private TextInputEditText     edtCategoria;
    private MaterialButton        btnGuardar, btnLimpiar;

    // Zonas de trabajo predefinidas
    private static final String[] ZONAS = {
            "Área de Producción",
            "Área Administrativa",
            "Almacén General",
            "Almacén de Químicos",
            "Comedor",
            "Baños / Servicios Higiénicos",
            "Estacionamiento",
            "Área de Carga y Descarga",
            "Laboratorio",
            "Sala de Reuniones",
            "Pasillo Principal",
            "Otra zona"
    };

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
                case "Peligroso":    color = 0xFFFFFDE7; break;
                case "Especial":     color = 0xFFF3E5F5; break;
                default:             color = 0xFFE8F5E9; break;
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
                    cal.get(Calendar.DAY_OF_MONTH))
                    .show();
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
                    true)
                    .show();
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
    }

    private void guardarRegistro() {
        // ── Validaciones ─────────────────────────────────────────────────────
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

        // ── Crear registro ───────────────────────────────────────────────────
        Usuario u      = db.obtenerUsuarioPorId(usuarioId);
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
                ""
        );

        // ── Guardar en base de datos ─────────────────────────────────────────
        long id = db.insertarRegistro(registro);

        if (id != -1) {
            registro.setId(id); // asignar el id generado al registro

            // ✅ ANÁLISIS ML — detecta exceso o peligrosidad y alerta WhatsApp
            AlertaMLService.get(requireContext()).analizar(registro);

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