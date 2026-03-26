package com.example.ecolim.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapaFragment — Mapa de calor de zonas de recolección.
 *
 * Muestra en el mapa cada zona donde se registraron residuos,
 * con un marcador de color según el nivel de acumulación:
 *   🟢 Verde   → menos de 50 kg  (normal)
 *   🟡 Naranja → 50 a 150 kg     (precaución)
 *   🔴 Rojo    → más de 150 kg   (alerta)
 *
 * Al tocar un marcador abre un BottomSheet con el resumen de esa zona.
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/fragments/MapaFragment.java
 */
public class MapaFragment extends Fragment {

    private MapView            mapView;
    private EcolimDbHelper     db;
    private long               usuarioId;
    private MyLocationNewOverlay locationOverlay;
    private TextView           txtZonasContador;
    private MaterialButton     btnMiUbicacion;
    private ChipGroup          chipGroup;

    // Coordenadas por defecto — Lima, Perú
    private static final double LAT_DEFAULT = -12.0464;
    private static final double LON_DEFAULT = -77.0428;
    private static final int    ZOOM_DEFAULT = 14;

    // Filtro activo
    private String filtroActivo = "Todas";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Configurar OSMDroid
        Configuration.getInstance().setUserAgentValue(
                requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null
                ? getArguments().getLong("usuario_id", -1) : -1;

        mapView         = view.findViewById(R.id.map_view);
        txtZonasContador = view.findViewById(R.id.txt_zonas_contador);
        btnMiUbicacion  = view.findViewById(R.id.btn_mi_ubicacion);
        chipGroup       = view.findViewById(R.id.chip_group_filtros);

        configurarMapa();
        configurarFiltros();
        configurarBotones();
        cargarMarcadores("Todas");

        return view;
    }

    // ── Configurar el mapa ────────────────────────────────────────────────────
    private void configurarMapa() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        IMapController controller = mapView.getController();
        controller.setZoom((double) ZOOM_DEFAULT);
        controller.setCenter(new GeoPoint(LAT_DEFAULT, LON_DEFAULT));

        // Overlay de mi ubicación
        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);
    }

    // ── Filtros por categoría ─────────────────────────────────────────────────
    private void configurarFiltros() {
        String[] filtros = {"Todas", "Normal", "Precaución", "Alerta"};
        for (String filtro : filtros) {
            Chip chip = new Chip(requireContext());
            chip.setText(filtro);
            chip.setCheckable(true);
            chip.setChecked(filtro.equals("Todas"));
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setTextColor(Color.parseColor("#2E7D32"));
            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    filtroActivo = filtro;
                    cargarMarcadores(filtro);
                }
            });
            chipGroup.addView(chip);
        }
    }

    // ── Botón mi ubicación ────────────────────────────────────────────────────
    private void configurarBotones() {
        btnMiUbicacion.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                GeoPoint miPos = locationOverlay.getMyLocation();
                if (miPos != null) {
                    mapView.getController().animateTo(miPos);
                    mapView.getController().setZoom(16.0);
                } else {
                    locationOverlay.runOnFirstFix(() ->
                            requireActivity().runOnUiThread(() -> {
                                GeoPoint pos = locationOverlay.getMyLocation();
                                if (pos != null) mapView.getController().animateTo(pos);
                            }));
                }
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 200);
            }
        });
    }

    // ── Cargar marcadores desde la BD ─────────────────────────────────────────
    private void cargarMarcadores(String filtro) {
        // Limpiar marcadores anteriores (mantener el overlay de ubicación)
        mapView.getOverlays().clear();
        mapView.getOverlays().add(locationOverlay);

        List<Registro> registros = db.obtenerTodosRegistros();

        // Agrupar por zona
        Map<String, List<Registro>> porZona = new HashMap<>();
        for (Registro r : registros) {
            String zona = r.getUbicacion();
            if (!porZona.containsKey(zona)) porZona.put(zona, new ArrayList<>());
            porZona.get(zona).add(r);
        }

        int contador = 0;

        // Coordenadas predefinidas para cada zona
        Map<String, GeoPoint> coordenadas = coordenadasZonas();

        for (Map.Entry<String, List<Registro>> entry : porZona.entrySet()) {
            String         zona   = entry.getKey();
            List<Registro> regs   = entry.getValue();

            double totalKg = 0;
            int    peligrosos = 0;
            for (Registro r : regs) {
                totalKg += r.getCantidad();
                if ("Peligroso".equals(r.getCategoriaResiduo())) peligrosos++;
            }

            // Determinar nivel
            String nivel;
            if (totalKg > 150 || peligrosos > 0) nivel = "Alerta";
            else if (totalKg > 50)                nivel = "Precaución";
            else                                   nivel = "Normal";

            // Aplicar filtro
            if (!filtro.equals("Todas") && !nivel.equals(filtro)) continue;

            // Obtener coordenadas de la zona
            GeoPoint punto = coordenadas.containsKey(zona)
                    ? coordenadas.get(zona)
                    : generarCoordenadaAleatoria(zona);

            // Crear marcador
            Marker marcador = new Marker(mapView);
            marcador.setPosition(punto);
            marcador.setTitle(zona);
            marcador.setSnippet(String.format("%.1f kg | %d registros | %d peligrosos",
                    totalKg, regs.size(), peligrosos));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // Color según nivel
            Drawable icono = colorMarcador(nivel);
            if (icono != null) marcador.setIcon(icono);

            // Al tocar → BottomSheet con detalle
            final String   zonaNombre  = zona;
            final double   totalFinal  = totalKg;
            final int      regsFinal   = regs.size();
            final int      pelFinal    = peligrosos;
            final String   nivelFinal  = nivel;
            marcador.setOnMarkerClickListener((m, map) -> {
                mostrarDetalleZona(zonaNombre, totalFinal, regsFinal, pelFinal, nivelFinal);
                return true;
            });

            mapView.getOverlays().add(marcador);
            contador++;
        }

        mapView.invalidate();
        txtZonasContador.setText(contador + " zona" + (contador != 1 ? "s" : ""));
    }

    // ── BottomSheet de detalle de zona ────────────────────────────────────────
    private void mostrarDetalleZona(String zona, double totalKg,
                                    int registros, int peligrosos, String nivel) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_zona, null);

        // Color del nivel
        int color;
        switch (nivel) {
            case "Alerta":    color = Color.parseColor("#F44336"); break;
            case "Precaución":color = Color.parseColor("#FF9800"); break;
            default:          color = Color.parseColor("#4CAF50"); break;
        }

        TextView txtZona     = sheet.findViewById(R.id.txt_bs_zona);
        TextView txtNivel    = sheet.findViewById(R.id.txt_bs_nivel);
        TextView txtKg       = sheet.findViewById(R.id.txt_bs_kg);
        TextView txtRegs     = sheet.findViewById(R.id.txt_bs_registros);
        TextView txtPel      = sheet.findViewById(R.id.txt_bs_peligrosos);
        View     indicador   = sheet.findViewById(R.id.view_bs_indicador);

        txtZona.setText(zona);
        txtNivel.setText(nivel);
        txtNivel.setTextColor(color);
        txtKg.setText(String.format("%.1f kg acumulados", totalKg));
        txtRegs.setText(registros + " registro" + (registros != 1 ? "s" : ""));
        txtPel.setText(peligrosos + " peligroso" + (peligrosos != 1 ? "s" : ""));
        indicador.setBackgroundColor(color);

        dialog.setContentView(sheet);
        dialog.show();
    }

    // ── Coordenadas predefinidas para las zonas ECOLIM ───────────────────────
    private Map<String, GeoPoint> coordenadasZonas() {
        Map<String, GeoPoint> map = new HashMap<>();
        // Distribuidas alrededor del centro de Lima
        map.put("Área de Producción",          new GeoPoint(-12.0420, -77.0280));
        map.put("Área Administrativa",          new GeoPoint(-12.0435, -77.0320));
        map.put("Almacén General",              new GeoPoint(-12.0450, -77.0350));
        map.put("Almacén de Químicos",          new GeoPoint(-12.0465, -77.0310));
        map.put("Comedor",                      new GeoPoint(-12.0480, -77.0290));
        map.put("Baños / Servicios Higiénicos", new GeoPoint(-12.0490, -77.0330));
        map.put("Estacionamiento",              new GeoPoint(-12.0445, -77.0400));
        map.put("Área de Carga y Descarga",     new GeoPoint(-12.0460, -77.0380));
        map.put("Laboratorio",                  new GeoPoint(-12.0425, -77.0360));
        map.put("Sala de Reuniones",            new GeoPoint(-12.0415, -77.0340));
        map.put("Pasillo Principal",            new GeoPoint(-12.0440, -77.0300));
        map.put("Otra zona",                    new GeoPoint(-12.0470, -77.0360));
        return map;
    }

    // Genera coordenada basada en el hash de la zona (para zonas no predefinidas)
    private GeoPoint generarCoordenadaAleatoria(String zona) {
        int hash = zona.hashCode();
        double lat = LAT_DEFAULT + (hash % 100) * 0.0002;
        double lon = LON_DEFAULT + (hash % 137) * 0.0002;
        return new GeoPoint(lat, lon);
    }

    // ── Color del marcador según nivel ────────────────────────────────────────
    private Drawable colorMarcador(String nivel) {
        try {
            int color;
            switch (nivel) {
                case "Alerta":     color = Color.parseColor("#F44336"); break;
                case "Precaución": color = Color.parseColor("#FF9800"); break;
                default:           color = Color.parseColor("#4CAF50"); break;
            }
            // Crear un drawable circular coloreado
            android.graphics.drawable.GradientDrawable circle =
                    new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(color);
            circle.setStroke(4, Color.WHITE);
            circle.setSize(48, 48);
            return circle;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onResume() {
        super.onResume();
        mapView.onResume();
        cargarMarcadores(filtroActivo);
    }

    @Override public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
    }
}
