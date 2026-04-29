package com.mapbuilder.mapbuilder.core.map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LodGridCache {

    public static final class Key {
        public final int wx0, wy0, wx1, wy1;
        public final LodLevel lod;

        public Key(int wx0, int wy0, int wx1, int wy1, LodLevel lod) {
            this.wx0 = wx0; this.wy0 = wy0;
            this.wx1 = wx1; this.wy1 = wy1;
            this.lod = lod;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key k)) return false;
            return wx0 == k.wx0 && wy0 == k.wy0 && wx1 == k.wx1 && wy1 == k.wy1 && lod == k.lod;
        }

        @Override
        public int hashCode() {
            return Objects.hash(wx0, wy0, wx1, wy1, lod);
        }
    }

    private static final int MAX_ENTRIES = 6;

    private final LinkedHashMap<Key, MapGrid> map = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, MapGrid> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public MapGrid get(Key key) {
        return map.get(key);
    }

    public void put(Key key, MapGrid grid) {
        map.put(key, grid);
    }

    public void invalidateAll() {
        map.clear();
    }
}
