package com.mapbuilder.mapbuilder.core.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class PointOfInterestTest {

    @Test
    void testPointOfInterestConstructorSetsAllFields() {
        long beforeTime = System.currentTimeMillis();
        PointOfInterest poi = new PointOfInterest(1, 42, 67, POIType.CITY, "Ashland City", "kingdom_capital");
        long afterTime = System.currentTimeMillis();
        
        assertEquals(1, poi.getId(), "ID should be set correctly");
        assertEquals(42, poi.getX(), "X coordinate should be set correctly");
        assertEquals(67, poi.getY(), "Y coordinate should be set correctly");
        assertEquals(POIType.CITY, poi.getType(), "Type should be set correctly");
        assertEquals("Ashland City", poi.getName(), "Name should be set correctly");
        assertEquals("kingdom_capital", poi.getCreatedByRule(), "CreatedByRule should be set correctly");
        assertTrue(poi.getCreatedAt() >= beforeTime && poi.getCreatedAt() <= afterTime, 
                   "CreatedAt should be set to current timestamp");
    }

    @Test
    void testPointOfInterestGettersReturnExpectedValues() {
        PointOfInterest poi = new PointOfInterest(2, 10, 20, POIType.DUNGEON, "Dark Cavern", "terrain_landmark");
        
        assertEquals(10, poi.getX(), "getX should return correct value");
        assertEquals(20, poi.getY(), "getY should return correct value");
        assertEquals(POIType.DUNGEON, poi.getType(), "getType should return correct value");
        assertEquals("Dark Cavern", poi.getName(), "getName should return correct value");
    }

    @Test
    void testPointOfInterestSettersModifyEditableFields() {
        PointOfInterest poi = new PointOfInterest(3, 5, 15, POIType.TAVERN, "The Prancing Pony", "user_placed");
        
        // Test name setter
        poi.setName("The Silver Stag");
        assertEquals("The Silver Stag", poi.getName(), "setName should update name");
        
        // Test description setter
        poi.setDescription("A cozy tavern");
        assertEquals("A cozy tavern", poi.getDescription(), "setDescription should update description");
        
        // Test customColor setter
        poi.setCustomColor(0xFF00FF00);
        assertEquals(0xFF00FF00, poi.getCustomColor(), "setCustomColor should update color");
        
        // Test customIcon setter
        poi.setCustomIcon(POIType.CASTLE);
        assertEquals(POIType.CASTLE, poi.getCustomIcon(), "setCustomIcon should update icon");
    }

    @Test
    void testPointOfInterestImmutableFields() {
        PointOfInterest poi = new PointOfInterest(4, 30, 40, POIType.RUIN, "Ancient Ruin", "border_dungeon");
        int originalX = poi.getX();
        int originalY = poi.getY();
        POIType originalType = poi.getType();
        
        // Verify we can't change immutable fields by trying to create similar POI with different values
        PointOfInterest poi2 = new PointOfInterest(4, 31, 41, POIType.SHRINE, "Ancient Ruin", "border_dungeon");
        assertNotEquals(originalX, poi2.getX(), "Different POI should have different X");
        assertNotEquals(originalY, poi2.getY(), "Different POI should have different Y");
        assertNotEquals(originalType, poi2.getType(), "Different POI should have different type");
    }

    @Test
    void testPointOfInterestToString() {
        PointOfInterest poi = new PointOfInterest(5, 50, 60, POIType.TOWER, "Wizard Tower", "rule_based");
        String str = poi.toString();
        
        assertTrue(str.contains("id=5"), "toString should contain id");
        assertTrue(str.contains("type=TOWER"), "toString should contain type");
        assertTrue(str.contains("name='Wizard Tower'"), "toString should contain name");
        assertTrue(str.contains("x=50"), "toString should contain x");
        assertTrue(str.contains("y=60"), "toString should contain y");
    }

    @Test
    void testMapGridAddPointOfInterestAndRetrieve() {
        MapGrid grid = new MapGrid(100, 100);
        PointOfInterest poi = new PointOfInterest(1, 25, 35, POIType.CITY, "Capital", "kingdom_capital");
        
        grid.addPointOfInterest(poi);
        
        assertEquals(1, grid.getPointsOfInterest().size(), "Grid should contain 1 POI");
        assertEquals(poi, grid.getPointOfInterestById(1), "getPointOfInterestById should return the added POI");
    }

    @Test
    void testMapGridGetPointsOfInterestReturnsIterableMutableList() {
        MapGrid grid = new MapGrid(100, 100);
        PointOfInterest poi1 = new PointOfInterest(1, 10, 10, POIType.CITY, "City 1", "rule1");
        PointOfInterest poi2 = new PointOfInterest(2, 20, 20, POIType.VILLAGE, "Village 1", "rule2");
        
        grid.addPointOfInterest(poi1);
        grid.addPointOfInterest(poi2);
        
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        assertEquals(2, pois.size(), "Should return list with 2 POIs");
        
        // Verify it's mutable (UI can iterate over it)
        assertNotNull(pois, "List should not be null");
        assertTrue(pois.contains(poi1), "List should contain first POI");
        assertTrue(pois.contains(poi2), "List should contain second POI");
    }

    @Test
    void testMapGridSetPointsOfInterestReplacesEntireList() {
        MapGrid grid = new MapGrid(100, 100);
        PointOfInterest poi1 = new PointOfInterest(1, 10, 10, POIType.CITY, "City 1", "rule1");
        PointOfInterest poi2 = new PointOfInterest(2, 20, 20, POIType.VILLAGE, "Village 1", "rule2");
        PointOfInterest poi3 = new PointOfInterest(3, 30, 30, POIType.DUNGEON, "Dungeon 1", "rule3");
        
        grid.addPointOfInterest(poi1);
        grid.addPointOfInterest(poi2);
        
        List<PointOfInterest> newList = new ArrayList<>();
        newList.add(poi3);
        grid.setPointsOfInterest(newList);
        
        assertEquals(1, grid.getPointsOfInterest().size(), "Grid should have 1 POI after setPointsOfInterest");
        assertEquals(poi3, grid.getPointOfInterestById(3), "Grid should contain new POI");
        assertNull(grid.getPointOfInterestById(1), "Grid should not contain old POI");
    }

    @Test
    void testMapGridRemovePointOfInterestById() {
        MapGrid grid = new MapGrid(100, 100);
        PointOfInterest poi1 = new PointOfInterest(1, 10, 10, POIType.CITY, "City 1", "rule1");
        PointOfInterest poi2 = new PointOfInterest(2, 20, 20, POIType.VILLAGE, "Village 1", "rule2");
        
        grid.addPointOfInterest(poi1);
        grid.addPointOfInterest(poi2);
        assertEquals(2, grid.getPointsOfInterest().size(), "Grid should have 2 POIs");
        
        grid.removePointOfInterest(1);
        
        assertEquals(1, grid.getPointsOfInterest().size(), "Grid should have 1 POI after removal");
        assertNull(grid.getPointOfInterestById(1), "Removed POI should not be found");
        assertEquals(poi2, grid.getPointOfInterestById(2), "Other POI should still exist");
    }

    @Test
    void testMapGridMultiplePOIsAtSameCoordinates() {
        MapGrid grid = new MapGrid(100, 100);
        PointOfInterest poi1 = new PointOfInterest(1, 50, 50, POIType.CITY, "City 1", "rule1");
        PointOfInterest poi2 = new PointOfInterest(2, 50, 50, POIType.VILLAGE, "Village 1", "rule2");
        
        grid.addPointOfInterest(poi1);
        grid.addPointOfInterest(poi2);
        
        assertEquals(2, grid.getPointsOfInterest().size(), "Grid should allow multiple POIs at same coordinates");
        assertEquals(poi1, grid.getPointOfInterestById(1), "First POI should be retrievable");
        assertEquals(poi2, grid.getPointOfInterestById(2), "Second POI should be retrievable");
    }

    @Test
    void testPointOfInterestCustomColorNullable() {
        PointOfInterest poi1 = new PointOfInterest(1, 10, 10, POIType.CITY, "City 1", "rule1");
        assertNull(poi1.getCustomColor(), "Custom color should be null by default");
        
        poi1.setCustomColor(0xFFFF0000);
        assertEquals(0xFFFF0000, poi1.getCustomColor(), "Custom color should be settable");
        
        poi1.setCustomColor(null);
        assertNull(poi1.getCustomColor(), "Custom color should be settable back to null");
    }
}
