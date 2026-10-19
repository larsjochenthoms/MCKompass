package de.kompass4mc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/** Small cardinal paths on one safe, already loaded ground plane. */
final class WanderRoutes {
    record Cell(int x, int z) {}

    private WanderRoutes() {}

    static List<List<Cell>> find(int homeX, int homeZ, int startX, int startZ, int radius,
                                BiPredicate<Integer, Integer> safe) {
        Cell start = new Cell(startX, startZ);
        if (!inside(start, homeX, homeZ, radius) || !safe.test(startX, startZ)) return List.of();
        Map<Cell, Cell> previous = new LinkedHashMap<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        previous.put(start, start);
        queue.add(start);
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            for (int[] direction : directions) {
                Cell next = new Cell(current.x() + direction[0], current.z() + direction[1]);
                if (!previous.containsKey(next) && inside(next, homeX, homeZ, radius)
                        && safe.test(next.x(), next.z())) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
            }
        }
        List<List<Cell>> routes = new ArrayList<>();
        for (Cell destination : previous.keySet()) {
            List<Cell> reversed = new ArrayList<>();
            for (Cell cell = destination; !cell.equals(start); cell = previous.get(cell)) reversed.add(cell);
            // Short strolls, with a real pause between them; never circle an entire obstacle course.
            if (reversed.size() < 2 || reversed.size() > 8) continue;
            routes.add(List.copyOf(reversed.reversed()));
        }
        return List.copyOf(routes);
    }

    private static boolean inside(Cell cell, int homeX, int homeZ, int radius) {
        long dx = (long) cell.x() - homeX;
        long dz = (long) cell.z() - homeZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
