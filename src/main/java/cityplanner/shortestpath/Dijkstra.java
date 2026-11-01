package cityplanner.shortestpath;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.Arrays;
import java.util.Objects;
import java.util.PriorityQueue;

public final class Dijkstra {
    private long validationEdgeExaminations;
    private long processingEdgeExaminations;
    private long successfulRelaxations;
    private long queueOffers;
    private long queuePolls;
    private long queueComparisons;

    public DijkstraResult findShortestPaths(WeightedGraph graph, int source) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (source < 0 || source >= graph.vertexCount()) {
            throw new IllegalArgumentException("Source is outside the graph: " + source);
        }
        long validatedEdges = 0;
        for (Edge edge : graph.edges()) {
            if (edge.weight() < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights.");
            }
            validatedEdges++;
        }

        validationEdgeExaminations = validatedEdges;
        processingEdgeExaminations = 0;
        successfulRelaxations = 0;
        queueOffers = 0;
        queuePolls = 0;
        queueComparisons = 0;

        long[] distances = new long[graph.vertexCount()];
        Arrays.fill(distances, DijkstraResult.UNREACHABLE);
        int[] predecessors = new int[graph.vertexCount()];
        Arrays.fill(predecessors, -1);
        distances[source] = 0;
        PriorityQueue<QueueEntry> pending = new PriorityQueue<>((first, second) -> {
            queueComparisons++;
            return Long.compare(first.distance(), second.distance());
        });
        pending.offer(new QueueEntry(source, 0));
        queueOffers++;

        while (!pending.isEmpty()) {
            QueueEntry current = pending.poll();
            queuePolls++;
            if (current.distance() != distances[current.vertex()]) {
                continue;
            }
            for (Edge edge : graph.edgesFrom(current.vertex())) {
                processingEdgeExaminations++;
                long candidate = Math.addExact(current.distance(), (long) edge.weight());
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    predecessors[edge.to()] = current.vertex();
                    successfulRelaxations++;
                    pending.offer(new QueueEntry(edge.to(), candidate));
                    queueOffers++;
                }
            }
        }

        return new DijkstraResult(source, distances, predecessors);
    }

    public long validationEdgeExaminations() {
        return validationEdgeExaminations;
    }

    public long processingEdgeExaminations() {
        return processingEdgeExaminations;
    }

    public long edgeExaminations() {
        return validationEdgeExaminations + processingEdgeExaminations;
    }

    public long relaxationAttempts() {
        return processingEdgeExaminations;
    }

    public long successfulRelaxations() {
        return successfulRelaxations;
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

    private record QueueEntry(int vertex, long distance) {
    }
}
