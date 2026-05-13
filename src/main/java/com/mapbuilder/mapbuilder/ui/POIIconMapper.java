package com.mapbuilder.mapbuilder.ui;

import com.mapbuilder.mapbuilder.core.map.POIType;

/**
 * Utility class for mapping POI types to sprite sheet coordinates and default colors.
 * This class handles the visual representation of Points of Interest through sprite indexing
 * and color assignment.
 */
public class POIIconMapper {

    // Sprite sheet dimensions (512x512 with 32x32 icons = 16x16 grid)
    private static final int SPRITE_SHEET_SIZE = 512;
    private static final int ICON_SIZE = 32;
    private static final int ICONS_PER_ROW = SPRITE_SHEET_SIZE / ICON_SIZE; // 16

    /**
     * Gets the sprite sheet coordinates (x, y) in pixels for a given POI type.
     * Coordinates are based on a 512x512 sprite sheet with 16x16 grid of 32x32 icons.
     *
     * @param type The POI type
     * @return Array [spriteX, spriteY] indicating the pixel coordinates in the sprite sheet
     */
    public static int[] getSpriteCoordinates(POIType type) {
        int index = getTypeIndex(type);
        int row = index / ICONS_PER_ROW;
        int col = index % ICONS_PER_ROW;
        int spriteX = col * ICON_SIZE;
        int spriteY = row * ICON_SIZE;
        return new int[]{spriteX, spriteY};
    }

    /**
     * Gets the default ARGB color for a POI type.
     * Used for rendering the colored circle background and for UI list indicators.
     *
     * @param type The POI type
     * @return ARGB color value
     */
    public static int getDefaultColor(POIType type) {
        switch (type) {
            case CITY:
                return 0xFF4CAF50; // Green
            case DUNGEON:
                return 0xFF3F51B5; // Dark blue
            case RUIN:
                return 0xFF795548; // Brown
            case VILLAGE:
                return 0xFFFFC107; // Amber
            case CASTLE:
                return 0xFF9C27B0; // Purple
            case CAVE:
                return 0xFF424242; // Dark grey
            default:
                return 0xFF9E9E9E; // Grey (fallback)
        }
    }

    /**
     * Gets the visual display name for a POI type (for UI labels).
     *
     * @param type The POI type
     * @return Human-readable name
     */
    public static String getDisplayName(POIType type) {
        switch (type) {
            case CITY:
                return "City";
            case VILLAGE:
                return "Village";
            case CASTLE:
                return "Castle";
            case DUNGEON:
                return "Dungeon";
            case CAVE:
                return "Cave";
            case RUIN:
                return "Ruin";
            default:
                return "Unknown";
        }
    }

    /**
     * Internal: Maps POI type to sprite sheet index.
     * Index determines grid position: index = row * 16 + col
     *
     * @param type The POI type
     * @return Index in sprite sheet grid
     */
    private static int getTypeIndex(POIType type) {
        switch (type) {
            case CITY:       return 0;
            case VILLAGE:    return 1;
            case CASTLE:     return 2;
            case DUNGEON:    return 3;
            case CAVE:       return 4;
            case RUIN:       return 5;
            default:
                // For any future types, cycle through remaining indices
                return 6;
        }
    }
}
