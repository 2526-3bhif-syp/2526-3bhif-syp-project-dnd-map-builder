package com.mapbuilder.mapbuilder.core.map;

public class Kingdom {
    private int id;
    private int colorARGB;
    private MapCell capital;

    public Kingdom(int id, int colorARGB, MapCell capital) {
        this.id = id;
        this.colorARGB = colorARGB;
        this.capital = capital;
    }

    public int getId() { return id; }
    public int getColorARGB() { return colorARGB; }
    public MapCell getCapital() { return capital; }
}
