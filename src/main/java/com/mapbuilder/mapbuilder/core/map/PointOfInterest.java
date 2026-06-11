package com.mapbuilder.mapbuilder.core.map;

/**
 * Represents a Point of Interest (POI) on the map.
 * The identity fields {@code id}, {@code createdAt}, and {@code createdByRule}
 * are immutable after construction. All other fields are editable to support
 * manual POI management (rename, retype, move, recolor, re-icon).
 *
 * This class is designed to be serialization-friendly with no circular references.
 */
public class PointOfInterest {
    // Immutable identity fields (set at construction)
    private final int id;
    private final long createdAt;
    private final String createdByRule;

    // Editable fields
    private int x;
    private int y;
    private POIType type;
    private String name;
    private String description;
    private Integer customColor;      // ARGB as Integer, nullable
    private POIType customIcon;       // POIType or null for default
    
    /**
     * Constructor for PointOfInterest with required fields.
     * Timestamp is auto-set to current time if not provided.
     * 
     * @param id Unique identifier for this POI
     * @param x X coordinate on the map
     * @param y Y coordinate on the map
     * @param type POIType enum value
     * @param name Display name for this POI
     * @param createdByRule Metadata string indicating creation source
     */
    public PointOfInterest(int id, int x, int y, POIType type, String name, String createdByRule) {
        this(id, x, y, type, name, createdByRule, System.currentTimeMillis());
    }
    
    /**
     * Full constructor with explicit timestamp.
     * 
     * @param id Unique identifier for this POI
     * @param x X coordinate on the map
     * @param y Y coordinate on the map
     * @param type POIType enum value
     * @param name Display name for this POI
     * @param createdByRule Metadata string indicating creation source
     * @param createdAt Timestamp in milliseconds
     */
    public PointOfInterest(int id, int x, int y, POIType type, String name, String createdByRule, long createdAt) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.name = name;
        this.createdByRule = createdByRule;
        this.createdAt = createdAt;
        this.description = "";
        this.customColor = null;
        this.customIcon = null;
    }
    
    // Immutable getters
    
    /**
     * Get the unique identifier for this POI.
     */
    public int getId() {
        return id;
    }
    
    /**
     * Get the X coordinate of this POI on the map.
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X coordinate of this POI on the map. Used when moving a POI.
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y coordinate of this POI on the map.
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y coordinate of this POI on the map. Used when moving a POI.
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the POIType of this POI.
     */
    public POIType getType() {
        return type;
    }

    /**
     * Set the POIType of this POI. Changing the type updates the default
     * icon sprite and color used during rendering.
     */
    public void setType(POIType type) {
        this.type = type;
    }
    
    /**
     * Get the creation timestamp in milliseconds.
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get the metadata string indicating how this POI was created.
     */
    public String getCreatedByRule() {
        return createdByRule;
    }
    
    // Editable getters and setters
    
    /**
     * Get the display name of this POI.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the display name of this POI.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the description of this POI.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description of this POI.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the custom color (ARGB as Integer), or null for type default.
     */
    public Integer getCustomColor() {
        return customColor;
    }
    
    /**
     * Set the custom color (ARGB as Integer), or null for type default.
     */
    public void setCustomColor(Integer customColor) {
        this.customColor = customColor;
    }
    
    /**
     * Get the custom icon (POIType override), or null for type default.
     */
    public POIType getCustomIcon() {
        return customIcon;
    }
    
    /**
     * Set the custom icon (POIType override), or null for type default.
     */
    public void setCustomIcon(POIType customIcon) {
        this.customIcon = customIcon;
    }
    
    /**
     * Return a human-readable string representation of this POI.
     */
    @Override
    public String toString() {
        return "POI{" +
                "id=" + id +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
