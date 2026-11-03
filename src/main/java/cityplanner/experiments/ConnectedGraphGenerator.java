package cityplanner.experiments;

import cityplanner.graph.WeightedGraph;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class ConnectedGraphGenerator {
    public static final int MIN_WEIGHT = 0;
    public static final int MAX_WEIGHT = 100;

    private ConnectedGraphGenerator() {
    }

    public static WeightedGraph generate(int vertexCount, int edgeCount, long seed) {
        if (vertexCount < 1) {
            throw new IllegalArgumentException("A connected graph must have at least one vertex.");
        }
        long maxEdges = (long) vertexCount * (vertexCount - 1) / 2;
        if (edgeCount < vertexCount - 1 || edgeCount > maxEdges) {
            throw new IllegalArgumentException("Edge count must be between V - 1 and V * (V - 1) / 2.");
        }

        Random random = new Random(seed);
        WeightedGraph graph = new WeightedGraph(vertexCount, false);
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

        Set<Long> treeEdges = new HashSet<>();
        for (int index = 1; index < order.length; index++) {
            int from = order[index];
            int to = order[random.nextInt(index)];
            treeEdges.add(edgeKey(from, to));
            graph.addEdge(from, to, MIN_WEIGHT + random.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1));
        }

        int edgesNeeded = edgeCount - (vertexCount - 1);
        long remainingPairs = maxEdges - (vertexCount - 1L);
        for (int from = 0; from < vertexCount && edgesNeeded > 0; from++) {
            for (int to = from + 1; to < vertexCount && edgesNeeded > 0; to++) {
                if (treeEdges.contains(edgeKey(from, to))) {
                    continue;
                }
                // Select this pair with probability edgesNeeded / remainingPairs.
                if (random.nextLong(remainingPairs) < edgesNeeded) {
                    graph.addEdge(from, to, MIN_WEIGHT + random.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1));
                    edgesNeeded--;
                }
                remainingPairs--;
            }
        }
        return graph;
    }

    private static long edgeKey(int from, int to) {
        int first = Math.min(from, to);
        int second = Math.max(from, to);
        return ((long) first << 32) | second;
    }
}
