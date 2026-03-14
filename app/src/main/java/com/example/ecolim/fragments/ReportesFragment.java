package com.example.ecolim.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReportesFragment extends Fragment {

    private TextInputEditText    edtFechaInicio, edtFechaFin;
    private AutoCompleteTextView ddTipoFiltro;
    private MaterialButton       btnGenerar, btnExportarExcel, btnExportarPdf, btnLimpiar;
    private LinearLayout         layoutResultados;
    private View                 cardResultados;
    private TextView             txtReporteTotal, txtReporteKg,
            txtReportePeligrosos, txtReporteNoPeligrosos, txtReporteEspeciales;

    private EcolimDbHelper       db;
    private long                 usuarioId;
    private final List<Registro> lista   = new ArrayList<>();
    private String               periodo = "";

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    ok -> { if (ok) generarExcel(); else toast("Permiso necesario"); });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reportes, container, false);
        db        = EcolimDbHelper.getInstance(requireContext());
        usuarioId = getArguments() != null ? getArguments().getLong("usuario_id", -1) : -1;
        bind(v); initFiltros(); initBotones(); setMesActual();
        return v;
    }

    private void bind(View v) {
        edtFechaInicio         = v.findViewById(R.id.edt_fecha_inicio);
        edtFechaFin            = v.findViewById(R.id.edt_fecha_fin);
        ddTipoFiltro           = v.findViewById(R.id.dd_tipo_filtro);
        btnGenerar             = v.findViewById(R.id.btn_generar);
        btnExportarExcel       = v.findViewById(R.id.btn_exportar_excel);
        btnExportarPdf         = v.findViewById(R.id.btn_exportar_pdf);
        btnLimpiar             = v.findViewById(R.id.btn_limpiar_reporte);
        layoutResultados       = v.findViewById(R.id.layout_resultados);
        cardResultados         = v.findViewById(R.id.card_resultados);
        txtReporteTotal        = v.findViewById(R.id.txt_reporte_total);
        txtReporteKg           = v.findViewById(R.id.txt_reporte_kg);
        txtReportePeligrosos   = v.findViewById(R.id.txt_reporte_peligrosos);
        txtReporteNoPeligrosos = v.findViewById(R.id.txt_reporte_no_peligrosos);
        txtReporteEspeciales   = v.findViewById(R.id.txt_reporte_especiales);
    }

    private void initFiltros() {
        List<String> items = new ArrayList<>();
        items.add("Todos los tipos");
        db.obtenerTodosResiduos().forEach(r -> items.add(r.getNombre()));
        ddTipoFiltro.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, items));
        ddTipoFiltro.setText("Todos los tipos", false);
        edtFechaInicio.setOnClickListener(x -> picker(true));
        edtFechaFin.setOnClickListener(x -> picker(false));
    }

    private void setMesActual() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH)+1,
                d = c.get(Calendar.DAY_OF_MONTH);
        edtFechaInicio.setText(fmt("%d-%02d-01", y, m));
        edtFechaFin.setText(fmt("%d-%02d-%02d", y, m, d));
    }

    private void initBotones() {
        btnGenerar.setOnClickListener(x -> generarReporte());
        btnLimpiar.setOnClickListener(x -> {
            setMesActual();
            ddTipoFiltro.setText("Todos los tipos", false);
            cardResultados.setVisibility(View.GONE);
            lista.clear();
        });
        btnExportarExcel.setOnClickListener(x -> pedirPermisoExcel());
        btnExportarPdf.setOnClickListener(x -> generarPdf());
    }

    // ── Reporte en pantalla ───────────────────────────────────────────────
    private void generarReporte() {
        String ini  = edt(edtFechaInicio), fin = edt(edtFechaFin);
        String tipo = ddTipoFiltro.getText().toString().trim();
        if (ini.isEmpty() || fin.isEmpty()) { toast("Seleccione el período"); return; }
        lista.clear();
        for (Registro r : db.obtenerRegistrosPorRango(ini, fin))
            if (tipo.equals("Todos los tipos") || tipo.equals(r.getTipoResiduo()))
                lista.add(r);
        periodo = ini + " al " + fin;
        if (lista.isEmpty()) { toast("Sin registros en el período"); cardResultados.setVisibility(View.GONE); return; }
        mostrar();
        cardResultados.setVisibility(View.VISIBLE);
    }

    private void mostrar() {
        double kg = kg(); long pel = cat("Peligroso"), np = cat("No Peligroso"), esp = cat("Especial");
        txtReporteTotal.setText(String.valueOf(lista.size()));
        txtReporteKg.setText(fmt("%.1f kg", kg));
        txtReportePeligrosos.setText(String.valueOf(pel));
        txtReporteNoPeligrosos.setText(String.valueOf(np));
        txtReporteEspeciales.setText(String.valueOf(esp));
        layoutResultados.removeAllViews();
        for (Map.Entry<String, Double> e : porTipo()) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_reporte_fila, layoutResultados, false);
            ((TextView) row.findViewById(R.id.txt_fila_tipo)).setText(e.getKey());
            ((TextView) row.findViewById(R.id.txt_fila_cantidad)).setText(
                    fmt("%.1f kg  (%.0f%%)", e.getValue(), e.getValue() / kg * 100));
            layoutResultados.addView(row);
        }
    }
//REPORTES EXCEL

    private void pedirPermisoExcel() {
        if (lista.isEmpty()) { toast("Primero genera el reporte"); return; }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        generarExcel();
    }

    private void generarExcel() {
        try {
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            String nombre = "ECOLIM_" + sdf("yyyyMMdd_HHmmss") + ".xlsx";
            File file = new File(dir, nombre);

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
                // Estructura mínima de un XLSX válido
                zipEntry(zos, "[Content_Types].xml",   contentTypes());
                zipEntry(zos, "_rels/.rels",            relsRoot());
                zipEntry(zos, "xl/workbook.xml",        workbook());
                zipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels());
                zipEntry(zos, "xl/styles.xml",          styles());
                zipEntry(zos, "xl/sharedStrings.xml",   sharedStrings());
                zipEntry(zos, "xl/worksheets/sheet1.xml", hoja1Xml());
                zipEntry(zos, "xl/worksheets/sheet2.xml", hoja2Xml());
                zipEntry(zos, "xl/worksheets/sheet3.xml", hoja3Xml());
            }

            toast("✔ Excel generado: " + nombre);
            open(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) {
            toast("Error Excel: " + e.getMessage());
        }
    }

    private void zipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    // ── Partes del XLSX ───────────────────────────────────────────────────
    private String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"
                + "</Types>";
    }

    private String relsRoot() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"Resumen\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Detalle\" sheetId=\"2\" r:id=\"rId2\"/>"
                + "<sheet name=\"Por Categoria\" sheetId=\"3\" r:id=\"rId3\"/>"
                + "</sheets></workbook>";
    }

    private String workbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
                + "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "<Relationship Id=\"rId5\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>"
                + "</Relationships>";
    }

    private String styles() {
        // Estilos: 0=normal, 1=header verde, 2=celda par, 3=celda impar, 4=total
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"3\">"
                + "<font><sz val=\"10\"/><name val=\"Arial\"/></font>"
                + "<font><sz val=\"11\"/><b/><name val=\"Arial\"/><color rgb=\"FFFFFFFF\"/></font>"
                + "<font><sz val=\"11\"/><b/><name val=\"Arial\"/><color rgb=\"FF2E7D32\"/></font>"
                + "</fonts>"
                + "<fills count=\"5\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1B5E20\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8F5E9\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFC8E6C9\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/></border>"
                + "<border><left style=\"thin\"><color rgb=\"FFCCCCCC\"/></left>"
                + "<right style=\"thin\"><color rgb=\"FFCCCCCC\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFCCCCCC\"/></top>"
                + "<bottom style=\"thin\"><color rgb=\"FFCCCCCC\"/></bottom></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"5\">"
                // 0: normal con borde
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"><alignment wrapText=\"0\"/></xf>"
                // 1: header (fondo verde oscuro, texto blanco bold)
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\"><alignment horizontal=\"center\"/></xf>"
                // 2: fila par (fondo verde claro)
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\"/>"
                // 3: fila impar (blanco)
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"/>"
                // 4: total (verde medio, texto verde bold)
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"4\" borderId=\"1\" xfId=\"0\"><alignment horizontal=\"center\"/></xf>"
                + "</cellXfs>"
                + "</styleSheet>";
    }

    // Tabla global de strings compartidos (todas las hojas la usan)
    private final List<String> ss = new ArrayList<>();

    private int si(String s) {
        String safe = s == null ? "" : s
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
        if (!ss.contains(safe)) ss.add(safe);
        return ss.indexOf(safe);
    }

    private String sharedStrings() {
        // Forzar que todos los strings ya estén indexados llamando las hojas primero
        ss.clear();
        hoja1Xml(); hoja2Xml(); hoja3Xml(); // este método también llena ss
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                .append(ss.size()).append("\" uniqueCount=\"").append(ss.size()).append("\">");
        for (String s : ss) sb.append("<si><t xml:space=\"preserve\">").append(s).append("</t></si>");
        sb.append("</sst>");
        return sb.toString();
    }

    // ── Hoja 1: Resumen ───────────────────────────────────────────────────
    private String hoja1Xml() {
        double kg = kg();
        long pel = cat("Peligroso"), np = cat("No Peligroso"), esp = cat("Especial");
        StringBuilder sb = new StringBuilder();
        sb.append(wsHeader());
        // Anchos de columna
        sb.append("<cols><col min=\"1\" max=\"1\" width=\"4\"/><col min=\"2\" max=\"2\" width=\"30\"/>"
                + "<col min=\"3\" max=\"3\" width=\"18\"/><col min=\"4\" max=\"4\" width=\"16\"/></cols>");
        sb.append("<sheetData>");

        // Título
        sb.append(rowStr(1, 40, new Object[][]{
                {2, "REPORTE ECOLIM S.A.C. — " + periodo, 1}
        }));
        // Sub-título
        sb.append(rowStr(2, 18, new Object[][]{
                {2, "Generado: " + sdf("dd/MM/yyyy HH:mm"), 0}
        }));
        sb.append(rowStr(3, 8, new Object[][]{})); // espacio

        // Headers KPIs
        sb.append(rowStr(4, 22, new Object[][]{
                {2, "INDICADOR", 1}, {3, "VALOR", 1}, {4, "UNIDAD", 1}
        }));

        Object[][] kpis = {
                {"Total Registros", String.valueOf(lista.size()), "unidades"},
                {"Total Recolectado", fmt("%.2f", kg), "kg"},
                {"Residuos Peligrosos", String.valueOf(pel), "registros"},
                {"Residuos No Peligrosos", String.valueOf(np), "registros"},
                {"Residuos Especiales", String.valueOf(esp), "registros"},
                {"Promedio por registro", fmt("%.2f", kg / Math.max(1, lista.size())), "kg/reg"},
        };
        for (int i = 0; i < kpis.length; i++) {
            int st = (i % 2 == 0) ? 2 : 3;
            sb.append(rowStr(5+i, 20, new Object[][]{
                    {2, kpis[i][0], st}, {3, kpis[i][1], st}, {4, kpis[i][2], st}
            }));
        }

        sb.append(rowStr(12, 8, new Object[][]{}));

        // Tabla por tipo
        sb.append(rowStr(13, 22, new Object[][]{
                {2, "TIPO DE RESIDUO", 1}, {3, "TOTAL (kg)", 1}, {4, "% DEL TOTAL", 1}
        }));
        List<Map.Entry<String, Double>> pt = porTipo();
        for (int i = 0; i < pt.size(); i++) {
            int st = (i % 2 == 0) ? 2 : 3;
            double v = pt.get(i).getValue();
            sb.append(rowStr(14+i, 20, new Object[][]{
                    {2, pt.get(i).getKey(), st},
                    {3, fmt("%.2f", v), st},
                    {4, fmt("%.1f%%", v / kg * 100), st}
            }));
        }
        int tr = 14 + pt.size();
        sb.append(rowStr(tr, 22, new Object[][]{{2, "TOTAL", 4}, {3, fmt("%.2f", kg), 4}, {4, "100.0%", 4}}));

        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    // ── Hoja 2: Detalle ───────────────────────────────────────────────────
    private String hoja2Xml() {
        StringBuilder sb = new StringBuilder();
        sb.append(wsHeader());
        sb.append("<cols>"
                + "<col min=\"1\" max=\"1\" width=\"6\"/><col min=\"2\" max=\"2\" width=\"22\"/>"
                + "<col min=\"3\" max=\"3\" width=\"20\"/><col min=\"4\" max=\"4\" width=\"14\"/>"
                + "<col min=\"5\" max=\"5\" width=\"12\"/><col min=\"6\" max=\"6\" width=\"10\"/>"
                + "<col min=\"7\" max=\"7\" width=\"20\"/><col min=\"8\" max=\"8\" width=\"12\"/>"
                + "<col min=\"9\" max=\"9\" width=\"10\"/><col min=\"10\" max=\"10\" width=\"12\"/>"
                + "</cols>");
        sb.append("<sheetData>");

        sb.append(rowStr(1, 28, new Object[][]{{1, "DETALLE REGISTROS — " + periodo, 1}}));
        sb.append(rowStr(2, 8, new Object[][]{}));
        sb.append(rowStr(3, 22, new Object[][]{
                {1,"#",1},{2,"Trabajador",1},{3,"Tipo Residuo",1},{4,"Categoría",1},
                {5,"Kg",1},{6,"Unidad",1},{7,"Zona",1},{8,"Fecha",1},{9,"Hora",1},{10,"Estado",1}
        }));

        double kgSum = 0;
        for (int i = 0; i < lista.size(); i++) {
            Registro r = lista.get(i);
            kgSum += r.getCantidad();
            int st = (i % 2 == 0) ? 2 : 3;
            sb.append(rowStr(4+i, 18, new Object[][]{
                    {1, String.valueOf(r.getId()), st},
                    {2, r.getNombreUsuario(), st},
                    {3, r.getTipoResiduo(), st},
                    {4, r.getCategoriaResiduo(), st},
                    {5, fmt("%.2f", r.getCantidad()), st},
                    {6, r.getUnidad(), st},
                    {7, r.getUbicacion(), st},
                    {8, r.getFecha(), st},
                    {9, r.getHora(), st},
                    {10, r.getEstado(), st}
            }));
        }
        int tr = 4 + lista.size();
        sb.append(rowStr(tr, 22, new Object[][]{
                {1,"TOTAL",4},{5,fmt("%.2f",kgSum),4}
        }));
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    // ── Hoja 3: Por categoría ─────────────────────────────────────────────
    private String hoja3Xml() {
        StringBuilder sb = new StringBuilder();
        sb.append(wsHeader());
        sb.append("<cols><col min=\"1\" max=\"1\" width=\"4\"/><col min=\"2\" max=\"2\" width=\"28\"/>"
                + "<col min=\"3\" max=\"3\" width=\"16\"/><col min=\"4\" max=\"4\" width=\"14\"/></cols>");
        sb.append("<sheetData>");

        String[] cats = {"Peligroso", "No Peligroso", "Especial"};
        int ri = 1;
        for (String cat : cats) {
            List<Registro> sub = new ArrayList<>();
            for (Registro r : lista) if (cat.equals(r.getCategoriaResiduo())) sub.add(r);
            if (sub.isEmpty()) continue;
            double kgC = 0; for (Registro r : sub) kgC += r.getCantidad();

            sb.append(rowStr(ri++, 26, new Object[][]{{2, cat.toUpperCase(), 1}}));
            sb.append(rowStr(ri++, 22, new Object[][]{{2,"TIPO",1},{3,"kg",1},{4,"% grupo",1}}));
            Map<String, Double> pt = new LinkedHashMap<>();
            for (Registro r : sub) pt.merge(r.getTipoResiduo(), r.getCantidad(), Double::sum);
            int idx = 0;
            for (Map.Entry<String, Double> e : pt.entrySet()) {
                int st = (idx++ % 2 == 0) ? 2 : 3;
                sb.append(rowStr(ri++, 18, new Object[][]{
                        {2, e.getKey(), st},
                        {3, fmt("%.2f", e.getValue()), st},
                        {4, fmt("%.1f%%", e.getValue() / kgC * 100), st}
                }));
            }
            sb.append(rowStr(ri++, 22, new Object[][]{{2,"SUBTOTAL",4},{3,fmt("%.2f",kgC),4}}));
            ri++;
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    // ── Helpers XML XLSX ─────────────────────────────────────────────────
    private String wsHeader() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">";
    }

    private String rowStr(int rowNum, int height, Object[][] cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("<row r=\"").append(rowNum).append("\" ht=\"").append(height).append("\" customHeight=\"1\">");
        for (Object[] cell : cells) {
            int    col = (int) cell[0];
            String val = cell[1] == null ? "" : cell[1].toString();
            int    sty = (int) cell[2];
            String ref = colLetter(col) + rowNum;
            int    idx = si(val);
            sb.append("<c r=\"").append(ref).append("\" t=\"s\" s=\"").append(sty)
                    .append("\"><v>").append(idx).append("</v></c>");
        }
        sb.append("</row>");
        return sb.toString();
    }

    private String colLetter(int col) {
        String[] letters = {"","A","B","C","D","E","F","G","H","I","J","K"};
        return col < letters.length ? letters[col] : "Z";
    }

    private void generarPdf() {
        if (lista.isEmpty()) { toast("Primero genera el reporte"); return; }
        PdfDocument pdf = new PdfDocument();
        final int W = 595, H = 842;
        PdfDocument.Page pg = pdf.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        Canvas c = pg.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pb = new Paint(Paint.ANTI_ALIAS_FLAG);
        pb.setTypeface(Typeface.DEFAULT_BOLD);

        // Header
        p.setColor(Color.parseColor("#1B5E20")); c.drawRect(0, 0, W, 65, p);
        pb.setColor(Color.WHITE); pb.setTextSize(17); c.drawText("ECOLIM S.A.C.", 20, 30, pb);
        p.setColor(Color.parseColor("#A5D6A7")); p.setTextSize(9);
        c.drawText("Gestión Integral de Residuos Sólidos", 20, 50, p);
        p.setColor(Color.WHITE); p.setTextSize(8);
        c.drawText(sdf("dd/MM/yyyy HH:mm"), W - 135, 30, p);
        c.drawText("Pág. 1", W - 60, 50, p);
        p.setColor(Color.parseColor("#4CAF50")); c.drawRect(0, 65, W, 69, p);

        // Título
        pb.setColor(Color.parseColor("#1B5E20")); pb.setTextSize(13);
        c.drawText("REPORTE DE RECOLECCIÓN DE RESIDUOS", 20, 96, pb);
        p.setColor(Color.parseColor("#555555")); p.setTextSize(9);
        c.drawText("Período: " + periodo, 20, 113, p);
        p.setColor(Color.parseColor("#CCCCCC")); c.drawLine(20, 121, W - 20, 121, p);

        // KPIs
        double kg = kg(); int tot = lista.size();
        long pel = cat("Peligroso"), np = cat("No Peligroso"), esp = cat("Especial");
        pb.setColor(Color.parseColor("#2E7D32")); pb.setTextSize(11);
        c.drawText("INDICADORES CLAVE", 20, 143, pb);
        String[][] kpis = {
                {"Total Registros", String.valueOf(tot)},
                {"Total Recolectado", fmt("%.1f kg", kg)},
                {"Peligrosos", pel + " (" + fmt("%.0f%%", pct(pel, tot)) + ")"},
                {"No Peligrosos", np + " (" + fmt("%.0f%%", pct(np, tot)) + ")"},
                {"Especiales", esp + " (" + fmt("%.0f%%", pct(esp, tot)) + ")"},
        };
        int ky = 160;
        for (int i = 0; i < kpis.length; i++) {
            p.setColor(i % 2 == 0 ? Color.WHITE : Color.parseColor("#F9FBE7"));
            c.drawRect(20, ky - 11, W - 20, ky + 9, p);
            p.setColor(Color.parseColor("#444444")); p.setTextSize(9);
            c.drawText(kpis[i][0], 26, ky, p);
            pb.setColor(Color.parseColor("#2E7D32")); pb.setTextSize(9);
            c.drawText(kpis[i][1], W - 175, ky, pb);
            ky += 22;
        }
        p.setColor(Color.parseColor("#DDDDDD")); c.drawLine(20, ky + 2, W - 20, ky + 2, p); ky += 20;

        // Tabla por tipo
        pb.setColor(Color.parseColor("#2E7D32")); pb.setTextSize(11);
        c.drawText("DESGLOSE POR TIPO", 20, ky, pb); ky += 16;
        p.setColor(Color.parseColor("#2E7D32")); c.drawRect(20, ky - 11, W - 20, ky + 8, p);
        pb.setColor(Color.WHITE); pb.setTextSize(8.5f);
        c.drawText("Tipo de Residuo", 26, ky, pb);
        c.drawText("kg", W - 165, ky, pb);
        c.drawText("% Total", W - 90, ky, pb);
        ky += 18;
        int idx = 0;
        for (Map.Entry<String, Double> e : porTipo()) {
            if (ky > H - 120) break;
            p.setColor(idx % 2 == 0 ? Color.WHITE : Color.parseColor("#F9FBE7"));
            c.drawRect(20, ky - 11, W - 20, ky + 8, p);
            p.setColor(Color.parseColor("#333333")); p.setTextSize(8.5f);
            c.drawText(e.getKey(), 26, ky, p);
            pb.setColor(Color.parseColor("#2E7D32")); pb.setTextSize(8.5f);
            c.drawText(fmt("%.1f", e.getValue()), W - 165, ky, pb);
            c.drawText(fmt("%.1f%%", e.getValue() / kg * 100), W - 90, ky, pb);
            ky += 18; idx++;
        }

        // Firmas
        int fy = H - 120;
        p.setColor(Color.parseColor("#AAAAAA"));
        c.drawLine(30, fy, 220, fy, p); c.drawLine(W - 220, fy, W - 30, fy, p);
        p.setColor(Color.parseColor("#555555")); p.setTextSize(8);
        c.drawText("Responsable de Recolección", 34, fy + 13, p);
        c.drawText("Supervisor ECOLIM S.A.C.", W - 215, fy + 13, p);

        // Footer
        p.setColor(Color.parseColor("#2E7D32")); c.drawRect(0, H - 32, W, H, p);
        p.setColor(Color.WHITE); p.setTextSize(7.5f);
        c.drawText("ECOLIM S.A.C.  |  NTP 900.058  |  " + sdf("dd/MM/yyyy"), 18, H - 11, p);

        pdf.finishPage(pg);
        try {
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            String nombre = "ECOLIM_" + sdf("yyyyMMdd_HHmmss") + ".pdf";
            File file = new File(dir, nombre);
            try (FileOutputStream fos = new FileOutputStream(file)) { pdf.writeTo(fos); }
            pdf.close();
            toast("✔ PDF generado: " + nombre);
            open(file, "application/pdf");
        } catch (IOException e) { pdf.close(); toast("Error PDF: " + e.getMessage()); }
    }

    // ── Utilidades ───────────────────────────────────────────────────────
    private double kg() { double s = 0; for (Registro r : lista) s += r.getCantidad(); return s; }
    private long cat(String c) { long n = 0; for (Registro r : lista) if (c.equals(r.getCategoriaResiduo())) n++; return n; }
    private double pct(long v, int t) { return t == 0 ? 0 : v * 100.0 / t; }
    private List<Map.Entry<String, Double>> porTipo() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Registro r : lista) m.merge(r.getTipoResiduo(), r.getCantidad(), Double::sum);
        List<Map.Entry<String, Double>> l = new ArrayList<>(m.entrySet());
        l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return l;
    }
    private String sdf(String p) { return new SimpleDateFormat(p, Locale.getDefault()).format(new Date()); }
    private String fmt(String f, Object... a) { return String.format(Locale.getDefault(), f, a); }
    private String edt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
    private void toast(String m) { Toast.makeText(requireContext(), m, Toast.LENGTH_LONG).show(); }
    private void picker(boolean ini) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
            String f = fmt("%d-%02d-%02d", y, m + 1, d);
            if (ini) edtFechaInicio.setText(f); else edtFechaFin.setText(f);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void open(File f, String mime) {
        try {
            Uri u = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", f);
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_VIEW).setDataAndType(u, mime)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Abrir con…"));
        } catch (Exception e) { toast("Archivo guardado en Documentos"); }
    }
}