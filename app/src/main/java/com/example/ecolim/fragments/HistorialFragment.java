package com.example.ecolim.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistorialFragment extends Fragment {

    private EcolimDbHelper      db;
    private long                usuarioId;
    private List<Registro>      listaCompleta;
    private List<Registro>      listaFiltrada;
    private HistorialAdapter    adapter;

    private RecyclerView        recycler;
    private TextView            txtContador, txtEmpty;
    private TextInputEditText   edtBuscar, edtFiltroFecha;
    private AutoCompleteTextView ddFiltroTipo;
    private MaterialButton      btnLimpiarFiltros;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null
                ? getArguments().getLong("usuario_id", -1) : -1;

        iniciarVistas(view);
        configurarRecycler();
        configurarFiltros();
        cargarRegistros();

        return view;
    }

    private void iniciarVistas(View v) {
        recycler          = v.findViewById(R.id.recycler_historial);
        txtContador       = v.findViewById(R.id.txt_contador);
        txtEmpty          = v.findViewById(R.id.txt_empty);
        edtBuscar         = v.findViewById(R.id.edt_buscar);
        edtFiltroFecha    = v.findViewById(R.id.edt_filtro_fecha);
        ddFiltroTipo      = v.findViewById(R.id.dd_filtro_tipo);
        btnLimpiarFiltros = v.findViewById(R.id.btn_limpiar_filtros);
    }

    private void configurarRecycler() {
        listaFiltrada = new ArrayList<>();
        adapter = new HistorialAdapter(listaFiltrada,
                this::mostrarDetalle,
                this::confirmarEliminar);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    private void configurarFiltros() {
        // Búsqueda en tiempo real
        edtBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                aplicarFiltros();
            }
        });

        // Filtro por fecha con DatePicker
        edtFiltroFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (picker, year, month, day) -> {
                        String fecha = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, day);
                        edtFiltroFecha.setText(fecha);
                        aplicarFiltros();
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        // Filtro por tipo
        String[] tipos = {"Todos", "Residuos Orgánicos", "Papel y Cartón",
                "Plásticos", "Metales", "Vidrio", "Residuos Peligrosos",
                "Residuos Electrónicos (RAEE)", "Residuos Químicos",
                "Residuos Biológicos", "Residuos de Construcción", "Otros"};
        ddFiltroTipo.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, tipos));
        ddFiltroTipo.setOnItemClickListener((p, v, pos, id) -> aplicarFiltros());

        // Limpiar filtros
        btnLimpiarFiltros.setOnClickListener(v -> {
            edtBuscar.setText("");
            edtFiltroFecha.setText("");
            ddFiltroTipo.setText("");
            cargarRegistros();
        });
    }

    private void cargarRegistros() {
        listaCompleta = db.obtenerTodosRegistros();
        listaFiltrada.clear();
        listaFiltrada.addAll(listaCompleta);
        actualizarUI();
    }

    private void aplicarFiltros() {
        String buscar = edtBuscar.getText() != null
                ? edtBuscar.getText().toString().toLowerCase().trim() : "";
        String fecha  = edtFiltroFecha.getText() != null
                ? edtFiltroFecha.getText().toString().trim() : "";
        String tipo   = ddFiltroTipo.getText().toString().trim();

        listaFiltrada.clear();

        for (Registro r : listaCompleta) {
            boolean matchBuscar = buscar.isEmpty()
                    || r.getTipoResiduo().toLowerCase().contains(buscar)
                    || r.getUbicacion().toLowerCase().contains(buscar)
                    || r.getNombreUsuario().toLowerCase().contains(buscar);

            boolean matchFecha = fecha.isEmpty() || r.getFecha().equals(fecha);

            boolean matchTipo = tipo.isEmpty() || tipo.equals("Todos")
                    || r.getTipoResiduo().equals(tipo);

            if (matchBuscar && matchFecha && matchTipo) {
                listaFiltrada.add(r);
            }
        }

        actualizarUI();
    }

    private void actualizarUI() {
        adapter.notifyDataSetChanged();
        int n = listaFiltrada.size();
        txtContador.setText(n + (n == 1 ? " registro" : " registros"));
        txtEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        recycler.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    }

    private void mostrarDetalle(Registro r) {
        String detalle =
                "📋 DETALLE DEL REGISTRO\n\n" +
                        "Tipo:        " + r.getTipoResiduo() + "\n" +
                        "Categoría:   " + r.getCategoriaResiduo() + "\n" +
                        "Cantidad:    " + r.getCantidad() + " " + r.getUnidad() + "\n" +
                        "Ubicación:   " + r.getUbicacion() + "\n" +
                        "Fecha:       " + r.getFecha() + "\n" +
                        "Hora:        " + r.getHora() + "\n" +
                        "Registrado:  " + r.getNombreUsuario() + "\n" +
                        "Estado:      " + r.getEstado() + "\n" +
                        "Sincronizado: " + (r.isSincronizado() ? "Sí ✅" : "No ⏳") + "\n\n" +
                        "Observaciones:\n" + (r.getObservaciones() != null &&
                        !r.getObservaciones().isEmpty() ? r.getObservaciones() : "Sin observaciones");

        new AlertDialog.Builder(requireContext())
                .setTitle("Registro #" + r.getId())
                .setMessage(detalle)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void confirmarEliminar(Registro r) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar registro")
                .setMessage("¿Eliminar el registro de " + r.getTipoResiduo() +
                        " del " + r.getFecha() + "?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    db.eliminarRegistro(r.getId());
                    Toast.makeText(requireContext(),
                            "Registro eliminado", Toast.LENGTH_SHORT).show();
                    cargarRegistros();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    static class HistorialAdapter
            extends RecyclerView.Adapter<HistorialAdapter.VH> {

        interface OnDetalle  { void onClick(Registro r); }
        interface OnEliminar { void onClick(Registro r); }

        private final List<Registro> lista;
        private final OnDetalle      onDetalle;
        private final OnEliminar     onEliminar;

        HistorialAdapter(List<Registro> lista,
                         OnDetalle onDetalle, OnEliminar onEliminar) {
            this.lista      = lista;
            this.onDetalle  = onDetalle;
            this.onEliminar = onEliminar;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_registro, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Registro r = lista.get(pos);

            h.txtTipo.setText(r.getTipoResiduo());
            h.txtCategoria.setText(r.getCategoriaResiduo());
            h.txtCantidad.setText(r.getCantidad() + " " + r.getUnidad());
            h.txtUbicacion.setText(r.getUbicacion());
            h.txtFechaHora.setText(r.getFecha() + "  " + r.getHora());
            h.txtUsuario.setText(r.getNombreUsuario());

            // Indicador de sincronización
            h.txtSync.setText(r.isSincronizado() ? "✅ Sincronizado" : "⏳ Pendiente");
            h.txtSync.setTextColor(r.isSincronizado() ? 0xFF2E7D32 : 0xFFE65100);

            // Color de barra lateral según categoría
            int color;
            switch (r.getCategoriaResiduo()) {
                case "Peligroso": color = 0xFFF44336; break;
                case "Especial":  color = 0xFF9C27B0; break;
                default:          color = 0xFF4CAF50; break;
            }
            h.colorBar.setBackgroundColor(color);

            h.itemView.setOnClickListener(v -> onDetalle.onClick(r));
            h.btnEliminar.setOnClickListener(v -> onEliminar.onClick(r));
        }

        @Override public int getItemCount() { return lista.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView       txtTipo, txtCategoria, txtCantidad,
                    txtUbicacion, txtFechaHora, txtUsuario, txtSync;
            View           colorBar;
            MaterialButton btnEliminar;

            VH(View v) {
                super(v);
                txtTipo      = v.findViewById(R.id.txt_tipo);
                txtCategoria = v.findViewById(R.id.txt_categoria);
                txtCantidad  = v.findViewById(R.id.txt_cantidad);
                txtUbicacion = v.findViewById(R.id.txt_ubicacion);
                txtFechaHora = v.findViewById(R.id.txt_fecha_hora);
                txtUsuario   = v.findViewById(R.id.txt_usuario);
                txtSync      = v.findViewById(R.id.txt_sync);
                colorBar     = v.findViewById(R.id.view_color_bar);
                btnEliminar  = v.findViewById(R.id.btn_eliminar);
            }
        }
    }
}