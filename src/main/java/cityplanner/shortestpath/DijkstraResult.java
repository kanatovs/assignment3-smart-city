package cityplanner.shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DijkstraResult {
    public static final long UNREACHABLE = Long.MAX_VALUE;

    private final int source;
    private final long[] distances;
    private final int[] predecessors;

    DijkstraResult(int source, long[] distances, int[] predecessors) {
        this.source = source;
        this.distances = distances.clone();
        this.predecessors = predecessors.clone();
    }

    public int source() {
        return source;
    }

    public int vertexCount() {
        return distances.length;
    }

    public long distanceTo(int vertex) {
        validateVertex(vertex);
        return distances[vertex];
    }

    public boolean isReachable(int vertex) {
        return distanceTo(vertex) != UNREACHABLE;
    }

    public List<Integer> pathTo(int vertex) {
        if (!isReachable(vertex)) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        for (int current = vertex; current != -1; current = predecessors[current]) {
            path.add(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IllegalArgumentException("Vertex is outside the result: " + vertex);
        }
    }
}
