package com.example.ecolim.models;

public class Residuo {

    // Categorías principales según norma ambiental
    public static final String CATEGORIA_PELIGROSO    = "Peligroso";
    public static final String CATEGORIA_NO_PELIGROSO = "No Peligroso";
    public static final String CATEGORIA_ESPECIAL     = "Especial";

    // Tipos predefinidos más comunes
    public static final String[] TIPOS = {
            "Residuos Orgánicos",
            "Papel y Cartón",
            "Plásticos",
            "Metales",
            "Vidrio",
            "Residuos Peligrosos",
            "Residuos Electrónicos (RAEE)",
            "Residuos Químicos",
            "Residuos Biológicos",
            "Residuos de Construcción",
            "Otros"
    };

    // Unidades de medida
    public static final String[] UNIDADES = {
            "Kilogramos (kg)",
            "Litros (L)",
            "Toneladas (t)",
            "Unidades"
    };

    private long   id;
    private String nombre;
    private String categoria;    // Peligroso / No Peligroso / Especial
    private String descripcion;
    private String colorHex;     // Color para identificación visual en UI
    private String icono;        // Nombre del ícono drawable
    private boolean activo;

    // ── Constructor vacío ────────────────────────────────
    public Residuo() {}

    // ── Constructor completo ─────────────────────────────
    public Residuo(String nombre, String categoria,
                   String descripcion, String colorHex, String icono) {
        this.nombre      = nombre;
        this.categoria   = categoria;
        this.descripcion = descripcion;
        this.colorHex    = colorHex;
        this.icono       = icono;
        this.activo      = true;
    }

    // ── Getters ──────────────────────────────────────────
    public long   getId()          { return id; }
    public String getNombre()      { return nombre; }
    public String getCategoria()   { return categoria; }
    public String getDescripcion() { return descripcion; }
    public String getColorHex()    { return colorHex; }
    public String getIcono()       { return icono; }
    public boolean isActivo()      { return activo; }

    // ── Setters ──────────────────────────────────────────
    public void setId(long v)          { this.id          = v; }
    public void setNombre(String v)    { this.nombre      = v; }
    public void setCategoria(String v) { this.categoria   = v; }
    public void setDescripcion(String v){ this.descripcion = v; }
    public void setColorHex(String v)  { this.colorHex    = v; }
    public void setIcono(String v)     { this.icono       = v; }
    public void setActivo(boolean v)   { this.activo      = v; }

    @Override
    public String toString() { return nombre; }
}