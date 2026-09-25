package com.shipovskijkorp.scythes.mod.ability;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached integer offsets for spherical block scans. The returned arrays contain
 * packed x/y/z triples and must be treated as immutable.
 */
public final class ScytheSphereOffsets {
    private static final Map<Long, int[]> CACHE = new HashMap<>();

    private ScytheSphereOffsets() {}

    public static synchronized int[] forRadius(double radius) {
        if (!(radius >= 0.0D) || !Double.isFinite(radius)) return new int[0];
        long key = Double.doubleToLongBits(radius);
        return CACHE.computeIfAbsent(key, ignored -> build(radius));
    }

    private static int[] build(double radius) {
        int ceil = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        int count = 0;
        for (int x = -ceil; x <= ceil; x++) {
            for (int y = -ceil; y <= ceil; y++) {
                for (int z = -ceil; z <= ceil; z++) {
                    if ((double) x * x + (double) y * y + (double) z * z <= radiusSquared) count++;
                }
            }
        }

        int[] offsets = new int[count * 3];
        int index = 0;
        for (int x = -ceil; x <= ceil; x++) {
            for (int y = -ceil; y <= ceil; y++) {
                for (int z = -ceil; z <= ceil; z++) {
                    if ((double) x * x + (double) y * y + (double) z * z > radiusSquared) continue;
                    offsets[index++] = x;
                    offsets[index++] = y;
                    offsets[index++] = z;
                }
            }
        }
        return offsets;
    }
}
