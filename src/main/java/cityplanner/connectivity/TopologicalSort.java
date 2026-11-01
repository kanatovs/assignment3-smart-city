package cityplanner.connectivity;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TopologicalSort {
    private long indegreeEdgeExaminations;
    private long processingEdgeExaminations;

    public TopologicalSortResult sort(WeightedGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological sorting requires a directed graph.");
        }

        indegreeEdgeExaminations = 0;
        processingEdgeExaminations = 0;

        int[] indegree = new int[graph.vertexCount()];
        for (Edge edge : graph.edges()) {
            indegree[edge.to()]++;
            indegreeEdgeExaminations++;
        }

        ArrayDeque<Integer> ready = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                ready.addLast(vertex);
            }
        }

        List<Integer> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int vertex = ready.removeFirst();
            order.add(vertex);
            for (Edge edge : graph.edgesFrom(vertex)) {
                processingEdgeExaminations++;
                indegree[edge.to()]--;
                if (indegree[edge.to()] == 0) {
                    ready.addLast(edge.to());
                }
            }
        }

        boolean hasCycle = order.size() != graph.vertexCount();
        return new TopologicalSortResult(hasCycle ? List.of() : order, hasCycle);
    }

    public long indegreeEdgeExaminations() {
        return indegreeEdgeExaminations;
    }

    public long processingEdgeExaminations() {
        return processingEdgeExaminations;
    }

    public long edgeExaminations() {
        return indegreeEdgeExaminations + processingEdgeExaminations;
    }
}
