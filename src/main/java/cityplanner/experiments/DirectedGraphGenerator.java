package cityplanner.experiments;

import cityplanner.graph.WeightedGraph;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class DirectedGraphGenerator {
    public static final int MIN_WEIGHT = 0;
    public static final int MAX_WEIGHT = 100;

    private DirectedGraphGenerator() {
    }

    public static WeightedGraph generateDag(int vertexCount, int edgeCount, long seed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Vertex count must not be negative.");
        }
        long maxEdges = (long) vertexCount * (vertexCount - 1) / 2;
        if (edgeCount < 0 || edgeCount > maxEdges) {
            throw new IllegalArgumentException("A DAG edge count must be between zero and V * (V - 1) / 2.");
        }

        Random random = new Random(seed);
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        int[] order = shuffledOrder(vertexCount, random);
        int edgesNeeded = edgeCount;
        long remainingPairs = maxEdges;
        for (int from = 0; from < vertexCount && edgesNeeded > 0; from++) {
            for (int to = from + 1; to < vertexCount && edgesNeeded > 0; to++) {
                // Only forward edges in this vertex order can be selected.
                if (random.nextLong(remainingPairs) < edgesNeeded) {
                    addWeightedEdge(graph, order[from], order[to], random);
                    edgesNeeded--;
                }
                remainingPairs--;
            }
        }
        return graph;
    }

    public static WeightedGraph generateCyclic(int vertexCount, int edgeCount, long seed) {
        if (vertexCount < 2) {
            throw new IllegalArgumentException("A loop-free directed cycle needs at least two vertices.");
        }
        long maxEdges = (long) vertexCount * (vertexCount - 1);
        if (edgeCount < 2 || edgeCount > maxEdges) {
            throw new IllegalArgumentException("A cyclic edge count must be between two and V * (V - 1).");
        }

        Random random = new Random(seed);
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        int[] order = shuffledOrder(vertexCount, random);
        int cycleSize = Math.min(3, Math.min(vertexCount, edgeCount));
        Set<Long> cycleEdges = new HashSet<>();
        for (int index = 0; index < cycleSize; index++) {
            int from = order[index];
            int to = order[(index + 1) % cycleSize];
            cycleEdges.add(edgeKey(from, to));
            addWeightedEdge(graph, from, to, random);
        }

        int edgesNeeded = edgeCount - cycleSize;
        long remainingPairs = maxEdges - cycleSize;
        for (int from = 0; from < vertexCount && edgesNeeded > 0; from++) {
            for (int to = 0; to < vertexCount && edgesNeeded > 0; to++) {
                if (from == to || cycleEdges.contains(edgeKey(from, to))) {
                    continue;
                }
                // Select this pair with probability edgesNeeded / remainingPairs.
                if (random.nextLong(remainingPairs) < edgesNeeded) {
                    addWeightedEdge(graph, from, to, random);
                    edgesNeeded--;
                }
                remainingPairs--;
            }
        }
        return graph;
    }

    private static int[] shuffledOrder(int vertexCount, Random random) {
        int[] order = new int[vertexCount];
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            order[vertex] = vertex;
        }
        for (int index = order.length - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            int saved = order[index];
            order[index] = order[other];
            order[other] = saved;
        }
        return order;
    }

    private static void addWeightedEdge(WeightedGraph graph, int from, int to, Random random) {
        graph.addEdge(from, to, MIN_WEIGHT + random.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1));
    }

    private static long edgeKey(int from, int to) {
        return ((long) from << 32) | to;
    }
}
