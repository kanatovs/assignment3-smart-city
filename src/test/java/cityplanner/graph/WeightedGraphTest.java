package cityplanner.graph;

import java.util.List;

public final class WeightedGraphTest {
    private WeightedGraphTest() {
    }

    public static void main(String[] args) {
        emptyGraphHasNoEdges();
        singleVertexCanBeIsolated();
        directedEdgesKeepTheirDirection();
        undirectedEdgesHaveReverseAdjacency();
        parallelEdgesAndRepeatedWeightsArePreserved();
        selfLoopsAreStoredOnce();
        weightsCanBeZeroOrNegative();
        invalidVertexCountsAreRejected();
        invalidVerticesDoNotChangeTheGraph();
        edgeViewsAreReadOnly();
        edgeViewsReflectNewEdges();
        System.out.println("WeightedGraphTest passed.");
    }

    private static void emptyGraphHasNoEdges() {
        WeightedGraph graph = new WeightedGraph(0, false);

        check(graph.vertexCount() == 0, "An empty graph must have no vertices.");
        check(graph.edgeCount() == 0, "An empty graph must have no edges.");
        check(graph.edges().isEmpty(), "The edge list must be empty.");
        expectIllegalArgument(() -> graph.edgesFrom(0));
        expectIllegalArgument(() -> graph.addEdge(0, 0, 1));
    }

    private static void singleVertexCanBeIsolated() {
        WeightedGraph graph = new WeightedGraph(1, true);

        check(graph.vertexCount() == 1, "The isolated vertex must be counted.");
        check(graph.isDirected(), "The directed flag must be preserved.");
        check(graph.edgeCount() == 0, "An isolated vertex must not create edges.");
        check(graph.edgesFrom(0).isEmpty(), "An isolated vertex must have no outgoing edges.");
    }

    private static void directedEdgesKeepTheirDirection() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 7);

        check(graph.edges().equals(List.of(new Edge(0, 1, 7))), "The logical edge is incorrect.");
        check(graph.edgesFrom(0).equals(List.of(new Edge(0, 1, 7))), "The outgoing edge is missing.");
        check(graph.edgesFrom(1).isEmpty(), "A directed edge must not create a reverse edge.");
        check(graph.edgesFrom(2).isEmpty(), "The isolated vertex must remain isolated.");
        check(graph.edgeCount() == 1, "A directed edge must count once.");
    }

    private static void undirectedEdgesHaveReverseAdjacency() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 2, 7);
        graph.addEdge(2, 3, 5);

        check(!graph.isDirected(), "The graph must be undirected.");
        check(graph.vertexCount() == 4, "Adding edges must not change the vertex count.");
        check(graph.edgeCount() == 2, "Reverse adjacency must not increase the logical edge count.");
        check(graph.edges().equals(List.of(new Edge(0, 2, 7), new Edge(2, 3, 5))),
                "Logical edges must appear once in insertion order.");
        check(graph.edgesFrom(0).equals(List.of(new Edge(0, 2, 7))), "The forward edge is missing.");
        check(graph.edgesFrom(2).equals(List.of(new Edge(2, 0, 7), new Edge(2, 3, 5))),
                "Reverse edges must start at their adjacency vertex.");
        check(graph.edgesFrom(3).equals(List.of(new Edge(3, 2, 5))), "The reverse edge is missing.");
        check(graph.edgesFrom(1).isEmpty(), "An unconnected vertex must have an empty adjacency list.");
    }

    private static void parallelEdgesAndRepeatedWeightsArePreserved() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 5);

        check(graph.edgeCount() == 4, "Each parallel edge must be counted.");
        check(graph.edges().equals(List.of(new Edge(0, 1, 5), new Edge(0, 1, 5),
                new Edge(0, 1, 2), new Edge(1, 2, 5))), "Parallel edges must not be overwritten.");
        check(graph.edgesFrom(0).equals(List.of(new Edge(0, 1, 5), new Edge(0, 1, 5),
                new Edge(0, 1, 2))), "All parallel outgoing edges must be preserved.");
        check(graph.edgesFrom(1).equals(List.of(new Edge(1, 0, 5), new Edge(1, 0, 5),
                new Edge(1, 0, 2), new Edge(1, 2, 5))), "All reverse parallel edges must be preserved.");
    }

    private static void selfLoopsAreStoredOnce() {
        for (boolean directed : new boolean[] {false, true}) {
            WeightedGraph graph = new WeightedGraph(1, directed);
            graph.addEdge(0, 0, 0);

            check(graph.edgeCount() == 1, "A self-loop must count once.");
            check(graph.edges().equals(List.of(new Edge(0, 0, 0))), "The logical self-loop is incorrect.");
            check(graph.edgesFrom(0).equals(List.of(new Edge(0, 0, 0))),
                    "A self-loop must appear once in adjacency.");
        }
    }

    private static void weightsCanBeZeroOrNegative() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 2, -3);
        graph.addEdge(2, 0, Integer.MAX_VALUE);
        graph.addEdge(2, 1, Integer.MIN_VALUE);

        check(graph.edges().equals(List.of(new Edge(0, 1, 0), new Edge(1, 2, -3),
                new Edge(2, 0, Integer.MAX_VALUE), new Edge(2, 1, Integer.MIN_VALUE))),
                "The graph must preserve the full range of integer weights.");
    }

    private static void invalidVertexCountsAreRejected() {
        expectIllegalArgument(() -> new WeightedGraph(-1, false));
        expectIllegalArgument(() -> new WeightedGraph(Integer.MIN_VALUE, true));
    }

    private static void invalidVerticesDoNotChangeTheGraph() {
        for (boolean directed : new boolean[] {false, true}) {
            WeightedGraph graph = new WeightedGraph(2, directed);
            graph.addEdge(0, 1, 4);
            List<Edge> originalEdges = List.copyOf(graph.edges());
            List<Edge> originalFromZero = List.copyOf(graph.edgesFrom(0));
            List<Edge> originalFromOne = List.copyOf(graph.edgesFrom(1));

            expectIllegalArgument(() -> graph.addEdge(-1, 0, 2));
            expectIllegalArgument(() -> graph.addEdge(0, -1, 2));
            expectIllegalArgument(() -> graph.addEdge(2, 0, 2));
            expectIllegalArgument(() -> graph.addEdge(0, 2, 2));
            expectIllegalArgument(() -> graph.edgesFrom(-1));
            expectIllegalArgument(() -> graph.edgesFrom(2));

            check(graph.edgeCount() == 1, "An invalid add must not change the edge count.");
            check(graph.edges().equals(originalEdges), "An invalid add must not change logical edges.");
            check(graph.edgesFrom(0).equals(originalFromZero), "An invalid add must not change adjacency.");
            check(graph.edgesFrom(1).equals(originalFromOne), "An invalid add must not change reverse adjacency.");
        }
    }

    private static void edgeViewsAreReadOnly() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 3);

        expectUnsupportedOperation(() -> graph.edges().add(new Edge(1, 0, 4)));
        expectUnsupportedOperation(() -> graph.edges().clear());
        expectUnsupportedOperation(() -> graph.edgesFrom(0).remove(0));
        expectUnsupportedOperation(() -> graph.edgesFrom(1).set(0, new Edge(1, 0, 4)));
        check(graph.edgeCount() == 1, "Mutation through a view must not change the graph.");
        check(graph.edgesFrom(0).equals(List.of(new Edge(0, 1, 3))), "The forward edge must be unchanged.");
        check(graph.edgesFrom(1).equals(List.of(new Edge(1, 0, 3))), "The reverse edge must be unchanged.");
    }

    private static void edgeViewsReflectNewEdges() {
        WeightedGraph graph = new WeightedGraph(3, true);
        List<Edge> edgeView = graph.edges();
        List<Edge> outgoingView = graph.edgesFrom(0);
        graph.addEdge(0, 1, 6);
        graph.addEdge(0, 2, 2);

        List<Edge> expectedEdges = List.of(new Edge(0, 1, 6), new Edge(0, 2, 2));
        check(edgeView.equals(expectedEdges), "An existing edge view must reflect later additions.");
        check(outgoingView.equals(expectedEdges), "An existing adjacency view must reflect later additions.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static void expectUnsupportedOperation(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected UnsupportedOperationException.");
    }
}
