package com.example.ecolim.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Usuario;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private EcolimDbHelper db;
    private long           usuarioId;
    private String         fechaHoy;

    private TextView       txtSaludo, txtFecha;
    private TextView       txtTotalRegistros, txtTotalKg, txtPendientes;
    private TextView       txtNombreUsuario, txtCargo;
    private PieChart       pieChart;
    private BarChart       barChart;
    private TextView       txtPctPeligroso, txtPctNoPeligroso, txtPctEspecial;
    private View           barPeligroso, barNoPeligroso, barEspecial;
    private ViewGroup      layoutUltimos;
    private MaterialButton btnNuevoRegistro, btnVerHistorial;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null
                ? getArguments().getLong("usuario_id", -1) : -1;
        fechaHoy  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        vincularVistas(view);
        cargarDatos();
        return view;
    }

    private void vincularVistas(View v) {
        txtSaludo         = v.findViewById(R.id.txt_saludo);
        txtFecha          = v.findViewById(R.id.txt_fecha);
        txtTotalRegistros = v.findViewById(R.id.txt_total_registros);
        txtTotalKg        = v.findViewById(R.id.txt_total_kg);
        txtPendientes     = v.findViewById(R.id.txt_pendientes);
        txtNombreUsuario  = v.findViewById(R.id.txt_nombre_usuario);
        txtCargo          = v.findViewById(R.id.txt_cargo);
        pieChart          = v.findViewById(R.id.pie_chart);
        barChart          = v.findViewById(R.id.bar_chart);
        txtPctPeligroso   = v.findViewById(R.id.txt_pct_peligroso);
        txtPctNoPeligroso = v.findViewById(R.id.txt_pct_no_peligroso);
        txtPctEspecial    = v.findViewById(R.id.txt_pct_especial);
        barPeligroso      = v.findViewById(R.id.bar_pct_peligroso);
        barNoPeligroso    = v.findViewById(R.id.bar_pct_no_peligroso);
        barEspecial       = v.findViewById(R.id.bar_pct_especial);
        layoutUltimos     = v.findViewById(R.id.layout_ultimos);
        btnNuevoRegistro  = v.findViewById(R.id.btn_nuevo_registro);
        btnVerHistorial   = v.findViewById(R.id.btn_ver_historial);
    }

    private void cargarDatos() {
        
        String fechaLegible = new SimpleDateFormat(
                "EEEE, dd 'de' MMMM yyyy", new Locale("es", "PE"))
                .format(new Date());
        txtFecha.setText(
                fechaLegible.substring(0, 1).toUpperCase()
                        + fechaLegible.substring(1));

        // Datos del usuario
        Usuario u = db.obtenerUsuarioPorId(usuarioId);
        if (u != null) {
            int hora = Integer.parseInt(
                    new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
            String saludo = hora < 12 ? "Buenos días"
                    : hora < 18 ? "Buenas tardes" : "Buenas noches";
            txtSaludo.setText(saludo + ", " + u.getNombre() + " 👋");
            txtNombreUsuario.setText(u.getNombre() + " " + u.getApellido());
            txtCargo.setText(u.getCargo());
        }

        // KPIs del día
        int    totalHoy   = db.contarRegistrosHoy(fechaHoy);
        double kgHoy      = db.sumarCantidadPorFecha(fechaHoy);
        int    pendientes = db.obtenerRegistrosPendientesSync().size();

        txtTotalRegistros.setText(String.valueOf(totalHoy));
        txtTotalKg.setText(String.format(Locale.getDefault(), "%.1f kg", kgHoy));
        txtPendientes.setText(String.valueOf(pendientes));

        // Datos del mes para gráficos
        String inicioMes = new SimpleDateFormat("yyyy-MM-01", Locale.getDefault())
                .format(new Date());
        List<Registro> regsMes = db.obtenerRegistrosPorRango(inicioMes, fechaHoy);

        construirPieChart(regsMes);
        construirBarChart(regsMes);
        cargarUltimosRegistros();
        configurarBotones();
    }

    private void construirPieChart(List<Registro> registros) {
        int peligrosos = 0, noPeligrosos = 0, especiales = 0;
        for (Registro r : registros) {
            switch (r.getCategoriaResiduo()) {
                case "Peligroso":    peligrosos++;   break;
                case "Especial":     especiales++;   break;
                default:             noPeligrosos++; break;
            }
        }
        int total = peligrosos + noPeligrosos + especiales;

        float pctPel = total > 0 ? peligrosos   * 100f / total : 0f;
        float pctNP  = total > 0 ? noPeligrosos * 100f / total : 0f;
        float pctEsp = total > 0 ? especiales   * 100f / total : 0f;

        txtPctPeligroso.setText(String.format(Locale.getDefault(),   "%.0f%%", pctPel));
        txtPctNoPeligroso.setText(String.format(Locale.getDefault(), "%.0f%%", pctNP));
        txtPctEspecial.setText(String.format(Locale.getDefault(),    "%.0f%%", pctEsp));

        setBarWidth(barPeligroso,   pctPel);
        setBarWidth(barNoPeligroso, pctNP);
        setBarWidth(barEspecial,    pctEsp);

        List<PieEntry> entries = new ArrayList<>();
        if (peligrosos   > 0) entries.add(new PieEntry(peligrosos,   "Peligroso"));
        if (noPeligrosos > 0) entries.add(new PieEntry(noPeligrosos, "No Peligroso"));
        if (especiales   > 0) entries.add(new PieEntry(especiales,   "Especial"));
        if (entries.isEmpty()) entries.add(new PieEntry(1f,           "Sin datos"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#F44336"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#9C27B0"));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        // ─── CORRECCIÓN: setDrawHoleEnabled (no setHoleEnabled) ───
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);       // ← CORRECTO para MPAndroidChart v3.1.0
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Residuos\ndel mes");
        pieChart.setCenterTextSize(13f);
        pieChart.setCenterTextColor(Color.parseColor("#1B5E20"));
        pieChart.setRotationAngle(270f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1000);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#333333"));
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

        pieChart.invalidate();
    }

    private void construirBarChart(List<Registro> registros) {
        Map<String, Float> porTipo = new HashMap<>();
        for (Registro r : registros)
            porTipo.merge(r.getTipoResiduo(), (float) r.getCantidad(), Float::sum);

        List<Map.Entry<String, Float>> sorted = new ArrayList<>(porTipo.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        int max5 = Math.min(5, sorted.size());

        List<BarEntry> entries = new ArrayList<>();
        List<String>   labels  = new ArrayList<>();
        for (int i = 0; i < max5; i++) {
            entries.add(new BarEntry(i, sorted.get(i).getValue()));
            String n = sorted.get(i).getKey();
            labels.add(n.length() > 10 ? n.substring(0, 9) + "…" : n);
        }
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0f));
            labels.add("Sin datos");
        }

        BarDataSet dataSet = new BarDataSet(entries, "kg");
        dataSet.setColors(
                Color.parseColor("#2E7D32"),
                Color.parseColor("#43A047"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#A5D6A7"),
                Color.parseColor("#C8E6C9"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(9f);
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setLabelCount(max5);

        barChart.getAxisLeft().setTextColor(Color.parseColor("#555555"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate();
    }

    private void cargarUltimosRegistros() {
        List<Registro> ultimos = db.obtenerRegistrosPorFecha(fechaHoy);
        layoutUltimos.removeAllViews();

        if (ultimos.isEmpty()) {
            TextView vacio = new TextView(requireContext());
            vacio.setText("No hay registros hoy.\n¡Empieza tu primera recolección!");
            vacio.setTextColor(Color.parseColor("#9E9E9E"));
            vacio.setTextSize(13f);
            vacio.setPadding(0, 8, 0, 8);
            layoutUltimos.addView(vacio);
            return;
        }

        int max = Math.min(3, ultimos.size());
        for (int i = 0; i < max; i++) {
            Registro r    = ultimos.get(i);
            View     item = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_registro_mini, layoutUltimos, false);

            ((TextView) item.findViewById(R.id.txt_tipo))
                    .setText(r.getTipoResiduo());
            ((TextView) item.findViewById(R.id.txt_cantidad))
                    .setText(String.format(Locale.getDefault(),
                            "%.1f %s", r.getCantidad(), r.getUnidad()));
            ((TextView) item.findViewById(R.id.txt_ubicacion))
                    .setText(r.getUbicacion());
            ((TextView) item.findViewById(R.id.txt_hora))
                    .setText(r.getHora());

            item.findViewById(R.id.view_color_bar)
                    .setBackgroundColor(colorCategoria(r.getCategoriaResiduo()));

            layoutUltimos.addView(item);
        }
    }

    private void configurarBotones() {
        btnNuevoRegistro.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new RegistroFragment())
                        .commit());

        btnVerHistorial.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HistorialFragment())
                        .commit());
    }

    private int colorCategoria(String cat) {
        if (cat == null) return Color.parseColor("#9E9E9E");
        switch (cat) {
            case "Peligroso": return Color.parseColor("#F44336");
            case "Especial":  return Color.parseColor("#9C27B0");
            default:          return Color.parseColor("#4CAF50");
        }
    }

    private void setBarWidth(View bar, float pct) {
        bar.post(() -> {
            ViewGroup parent   = (ViewGroup) bar.getParent();
            int       totalW   = parent.getWidth();
            int       newWidth = pct > 0 ? (int) (totalW * pct / 100f) : 12;
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.width = newWidth;
            bar.setLayoutParams(lp);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarDatos();
    }
}