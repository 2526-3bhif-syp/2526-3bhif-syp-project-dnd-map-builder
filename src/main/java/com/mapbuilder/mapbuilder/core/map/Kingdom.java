package com.mapbuilder.mapbuilder.core.map;

public class Kingdom {
    private int id;
    private int colorARGB;
    private MapCell capital;
    private String name;

    public Kingdom(int id, int colorARGB, MapCell capital) {
        this.id = id;
        this.colorARGB = colorARGB;
        this.capital = capital;
        this.name = "Province " + (id + 1);
    }

    public int getId()                       { return id; }
    public int getColorARGB()               { return colorARGB; }
    public void setColorARGB(int colorARGB) { this.colorARGB = colorARGB; }
    public MapCell getCapital()              { return capital; }
    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }
}
