package cityplanner.mst;

import cityplanner.graph.Edge;
import java.util.List;

public record MstResult(List<Edge> edges, long totalWeight, int componentCount) {
    public MstResult {
        edges = List.copyOf(edges);
        if (componentCount < 0) {
            throw new IllegalArgumentException("Component count must not be negative.");
        }
    }

    public boolean isConnected() {
        return componentCount == 1;
    }
}
