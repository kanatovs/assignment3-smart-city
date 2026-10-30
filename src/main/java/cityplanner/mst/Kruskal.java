package cityplanner.mst;

import cityplanner.dsu.DisjointSetUnion;
import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Kruskal {
    private long sortingComparisons;
    private long edgeExaminations;
    private long findCalls;
    private long unionCalls;
    private long parentTraversals;

    public MstResult findMinimumSpanningForest(WeightedGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected graph.");
        }

        sortingComparisons = 0;
        edgeExaminations = 0;
        findCalls = 0;
        unionCalls = 0;
        parentTraversals = 0;

        List<Edge> sortedEdges = new ArrayList<>(graph.edges());
        sortedEdges.sort((first, second) -> {
            sortingComparisons++;
            return Integer.compare(first.weight(), second.weight());
        });

        DisjointSetUnion groups = new DisjointSetUnion(graph.vertexCount());
        List<Edge> selectedEdges = new ArrayList<>();
        long totalWeight = 0;
        for (Edge edge : sortedEdges) {
            if (groups.componentCount() <= 1) {
                break;
            }
            edgeExaminations++;
            if (groups.union(edge.from(), edge.to())) {
                selectedEdges.add(edge);
                totalWeight += edge.weight();
            }
        }

        findCalls = groups.findCalls();
        unionCalls = groups.unionCalls();
        parentTraversals = groups.parentTraversals();
        return new MstResult(selectedEdges, totalWeight, groups.componentCount());
    }

    public long sortingComparisons() {
        return sortingComparisons;
    }

    public long edgeExaminations() {
        return edgeExaminations;
    }

    public long findCalls() {
        return findCalls;
    }

    public long unionCalls() {
        return unionCalls;
    }

    public long parentTraversals() {
        return parentTraversals;
    }
}
