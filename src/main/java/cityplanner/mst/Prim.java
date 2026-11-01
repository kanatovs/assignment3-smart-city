package cityplanner.mst;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

public final class Prim {
    private long edgeExaminations;
    private long queueOffers;
    private long queuePolls;
    private long queueComparisons;

    public MstResult findMinimumSpanningForest(WeightedGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph.");
        }

        edgeExaminations = 0;
        queueOffers = 0;
        queuePolls = 0;
        queueComparisons = 0;

        boolean[] visited = new boolean[graph.vertexCount()];
        List<Edge> selectedEdges = new ArrayList<>();
        PriorityQueue<Edge> candidates = new PriorityQueue<>((first, second) -> {
            queueComparisons++;
            return Integer.compare(first.weight(), second.weight());
        });
        long totalWeight = 0;
        int componentCount = 0;

        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            componentCount++;
            visit(graph, start, visited, candidates);
            while (!candidates.isEmpty()) {
                Edge edge = candidates.poll();
                queuePolls++;
                if (visited[edge.to()]) {
                    continue;
                }

                selectedEdges.add(edge);
                totalWeight += edge.weight();
                visit(graph, edge.to(), visited, candidates);
            }
        }

        return new MstResult(selectedEdges, totalWeight, componentCount);
    }

    private void visit(WeightedGraph graph, int vertex, boolean[] visited, PriorityQueue<Edge> candidates) {
        visited[vertex] = true;
        for (Edge edge : graph.edgesFrom(vertex)) {
            edgeExaminations++;
            if (!visited[edge.to()]) {
                candidates.offer(edge);
                queueOffers++;
            }
        }
    }

    public long edgeExaminations() {
        return edgeExaminations;
    }

    public long queueOffers() {
        return queueOffers;
    }

    public long queuePolls() {
        return queuePolls;
    }

    public long queueComparisons() {
        return queueComparisons;
    }
}
