package com.mapbuilder.mapbuilder.core.map;

/**
 * Enum representing the types of Points of Interest in the world.
 * Each type represents a distinct category for world-building rules and rendering.
 * This enum is designed to be expandable for future POI types.
 */
public enum POIType {
    // Kingdom & Settlement Types
    CITY,           // Kingdom capitals and major settlements
    VILLAGE,        // Small settlements and towns
    CASTLE,         // Fortified strongholds and castles
    TAVERN,         // Taverns, inns, and gathering places
    
    // Dungeon & Danger Types
    DUNGEON,        // Underground complexes and monster lairs
    CAVE,           // Natural caves and rock formations
    RUIN,           // Abandoned structures and ancient ruins
    
    // Natural & Special Landmarks
    LANDMARK,       // Notable natural or constructed features
    TOWER,          // Towers and tall structures
    SHRINE          // Religious sites and sacred places
}
