package cityplanner.connectivity;

import java.util.List;

public record TopologicalSortResult(List<Integer> order, boolean hasCycle) {
    public TopologicalSortResult {
        order = List.copyOf(order);
        if (hasCycle && !order.isEmpty()) {
            throw new IllegalArgumentException("A cyclic graph cannot have a topological order.");
        }
    }
}
