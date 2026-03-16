package com.example.ecolim.fragments;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistorialFragment extends Fragment {

    private EcolimDbHelper   db;
    private long             usuarioId;
    private List<Registro>   listaCompleta;
    private List<Registro>   listaFiltrada;
    private HistorialAdapter adapter;

    private RecyclerView         recycler;
    private TextView             txtContador, txtEmpty;
    private TextInputEditText    edtBuscar, edtFiltroFecha;
    private AutoCompleteTextView ddFiltroTipo;
    private MaterialButton       btnLimpiarFiltros;

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
        edtBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                aplicarFiltros();
            }
        });

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
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        String[] tipos = {"Todos", "Residuos Orgánicos", "Papel y Cartón",
                "Plásticos", "Metales", "Vidrio", "Residuos Peligrosos",
                "Residuos Electrónicos (RAEE)", "Residuos Químicos",
                "Residuos Biológicos", "Residuos de Construcción", "Otros"};
        ddFiltroTipo.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, tipos));
        ddFiltroTipo.setOnItemClickListener((p, v, pos, id) -> aplicarFiltros());

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
            boolean matchTipo  = tipo.isEmpty() || tipo.equals("Todos")
                    || r.getTipoResiduo().equals(tipo);

            if (matchBuscar && matchFecha && matchTipo) listaFiltrada.add(r);
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

    // ── Detalle completo con foto ─────────────────────────────────────────────
    private void mostrarDetalle(Registro r) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_detalle_registro, null);

        // Foto
        ImageView imgFoto   = dialogView.findViewById(R.id.img_detalle_foto);
        View      cardFoto  = dialogView.findViewById(R.id.card_detalle_foto);

        String fotoUri = r.getFotoUri();
        if (fotoUri != null && !fotoUri.isEmpty()) {
            cardFoto.setVisibility(View.VISIBLE);
            try {
                imgFoto.setImageURI(Uri.parse(fotoUri));
            } catch (Exception e) {
                imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            cardFoto.setVisibility(View.GONE);
        }

        // Textos del detalle
        setText(dialogView, R.id.txt_det_tipo,        r.getTipoResiduo());
        setText(dialogView, R.id.txt_det_categoria,   r.getCategoriaResiduo());
        setText(dialogView, R.id.txt_det_cantidad,    r.getCantidad() + " " + r.getUnidad());
        setText(dialogView, R.id.txt_det_ubicacion,   r.getUbicacion());
        setText(dialogView, R.id.txt_det_fecha,       r.getFecha() + "  " + r.getHora());
        setText(dialogView, R.id.txt_det_usuario,     r.getNombreUsuario());
        setText(dialogView, R.id.txt_det_estado,      r.getEstado());
        setText(dialogView, R.id.txt_det_sync,
                r.isSincronizado() ? "Sincronizado ✅" : "Pendiente ⏳");
        setText(dialogView, R.id.txt_det_obs,
                (r.getObservaciones() != null && !r.getObservaciones().isEmpty())
                        ? r.getObservaciones() : "Sin observaciones");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Registro #" + r.getId())
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void setText(View parent, int id, String texto) {
        TextView tv = parent.findViewById(id);
        if (tv != null) tv.setText(texto);
    }

    private void confirmarEliminar(Registro r) {
        new MaterialAlertDialogBuilder(requireContext())
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

    // ══════════════════════════════════════════════════════════════════════════
    //  ADAPTER
    // ══════════════════════════════════════════════════════════════════════════
    static class HistorialAdapter
            extends RecyclerView.Adapter<HistorialAdapter.VH> {

        interface OnDetalle  { void onClick(Registro r); }
        interface OnEliminar { void onClick(Registro r); }

        private final List<Registro> lista;
        private final OnDetalle      onDetalle;
        private final OnEliminar     onEliminar;

        HistorialAdapter(List<Registro> lista,
                         OnDetalle onDetalle, OnEliminar onEliminar) {
            this.lista     = lista;
            this.onDetalle = onDetalle;
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
            h.txtSync.setText(r.isSincronizado() ? "✅ Sincronizado" : "⏳ Pendiente");
            h.txtSync.setTextColor(r.isSincronizado() ? 0xFF2E7D32 : 0xFFE65100);

            // ── Foto en miniatura ────────────────────────────────────────────
            String fotoUri = r.getFotoUri();
            if (fotoUri != null && !fotoUri.isEmpty()) {
                h.imgMiniatura.setVisibility(View.VISIBLE);
                try {
                    h.imgMiniatura.setImageURI(null);
                    h.imgMiniatura.setImageURI(Uri.parse(fotoUri));
                } catch (Exception e) {
                    h.imgMiniatura.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else {
                // Sin foto — mostrar ícono gris
                h.imgMiniatura.setVisibility(View.VISIBLE);
                h.imgMiniatura.setImageResource(android.R.drawable.ic_menu_camera);
                h.imgMiniatura.setAlpha(0.3f);
            }

            // ── Color barra lateral ──────────────────────────────────────────
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
            ImageView      imgMiniatura;
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
                imgMiniatura = v.findViewById(R.id.img_miniatura_foto);  // ← NUEVO
                btnEliminar  = v.findViewById(R.id.btn_eliminar);
            }
        }
    }
}