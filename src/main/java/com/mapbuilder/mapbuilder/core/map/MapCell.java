package com.mapbuilder.mapbuilder.core.map;

public class MapCell {
    private int x;
    private int y;
    private double elevation;
    private double temperature;
    private double rainfall;
    private Biome biome;
    private int mixedColorARGB;
    private boolean isRiver;
    private boolean isLake;
    private Kingdom kingdom;

    public MapCell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getRainfall() {
        return rainfall;
    }

    public void setRainfall(double rainfall) {
        this.rainfall = rainfall;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    public int getMixedColorARGB() {
        return mixedColorARGB;
    }

    public void setMixedColorARGB(int mixedColorARGB) {
        this.mixedColorARGB = mixedColorARGB;
    }

    public boolean isRiver() {
        return isRiver;
    }

    public void setRiver(boolean isRiver) {
        this.isRiver = isRiver;
    }

    public boolean isLake() {
        return isLake;
    }

    public void setLake(boolean isLake) {
        this.isLake = isLake;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public void setKingdom(Kingdom kingdom) {
        this.kingdom = kingdom;
    }
}
