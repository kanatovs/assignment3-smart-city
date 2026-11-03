package cityplanner.mst;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;

public final class PrimTest {
    private PrimTest() {
    }

    public static void main(String[] args) {
        connectedGraphHasKnownMinimumWeight();
        disconnectedGraphCoversEveryComponent();
        isolatedVerticesRemainSeparate();
        emptyGraphIsNotConnected();
        singleVertexIgnoresLoops();
        negativeZeroAndExtremeWeightsAreSupported();
        totalWeightUsesLong();
        loopsAndParallelEdgesDoNotCreateCycles();
        equalWeightsAllowDifferentMinimumTrees();
        reversedInputEdgesAreSupported();
        inputAndEarlierResultsArePreserved();
        obsoleteQueueEntriesAreSkipped();
        countersResetForRepeatedValidCalls();
        invalidInputPreservesPreviousCounters();
        System.out.println("PrimTest passed.");
    }

    private static void connectedGraphHasKnownMinimumWeight() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 6);
        graph.addEdge(0, 3, 5);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 4);
        Prim algorithm = new Prim();

        MstResult result = algorithm.findMinimumSpanningForest(graph);
        check(result.totalWeight() == 19, "The known MST must cost 19.");
        check(result.isConnected(), "The known graph must be connected.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void disconnectedGraphCoversEveryComponent() {
        WeightedGraph graph = new WeightedGraph(6, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 1);
        graph.addEdge(0, 2, 9);
        graph.addEdge(3, 4, -2);
        Prim algorithm = new Prim();

        MstResult result = algorithm.findMinimumSpanningForest(graph);
        check(result.totalWeight() == 3, "The forest must include the minimum tree in both nontrivial components.");
        check(result.componentCount() == 3, "The isolated vertex must be included as a component.");
        check(!result.isConnected(), "A disconnected forest must not be labeled as an MST.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void isolatedVerticesRemainSeparate() {
        WeightedGraph graph = new WeightedGraph(4, false);
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.componentCount() == 4, "Every isolated vertex must count as a component.");
        check(result.totalWeight() == 0, "An edgeless forest must cost zero.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void emptyGraphIsNotConnected() {
        WeightedGraph graph = new WeightedGraph(0, false);
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.componentCount() == 0, "The empty graph must have zero components.");
        check(!result.isConnected(), "The empty graph must follow the shared disconnected convention.");
        check(result.totalWeight() == 0, "The empty graph must cost zero.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void singleVertexIgnoresLoops() {
        WeightedGraph graph = new WeightedGraph(1, false);
        graph.addEdge(0, 0, -7);
        graph.addEdge(0, 0, Integer.MAX_VALUE);
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.isConnected(), "A single vertex is connected without selected edges.");
        check(result.totalWeight() == 0, "Even negative loops must not enter the tree.");
        check(algorithm.edgeExaminations() == 2, "Both loop adjacency entries must be inspected.");
        check(algorithm.queueComparisons() == 0, "Loops must not enter the queue.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void negativeZeroAndExtremeWeightsAreSupported() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, -4);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 3, 3);
        graph.addEdge(0, 3, 10);
        graph.addEdge(0, 2, 2);
        MstResult result = new Prim().findMinimumSpanningForest(graph);
        check(result.totalWeight() == -1, "Negative and zero edges must be handled normally.");
        verifyForest(graph, result);

        WeightedGraph extremes = new WeightedGraph(3, false);
        extremes.addEdge(0, 1, Integer.MIN_VALUE);
        extremes.addEdge(1, 2, Integer.MAX_VALUE);
        extremes.addEdge(0, 2, 0);
        MstResult extremeResult = new Prim().findMinimumSpanningForest(extremes);
        check(extremeResult.totalWeight() == Integer.MIN_VALUE, "Queue weight comparison must not overflow.");
        verifyForest(extremes, extremeResult);
    }

    private static void totalWeightUsesLong() {
        for (int weight : new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            WeightedGraph graph = new WeightedGraph(3, false);
            graph.addEdge(0, 1, weight);
            graph.addEdge(1, 2, weight);
            MstResult result = new Prim().findMinimumSpanningForest(graph);

            check(result.totalWeight() == 2L * weight, "A total outside the integer range must be preserved.");
            verifyForest(graph, result);
        }
    }

    private static void loopsAndParallelEdgesDoNotCreateCycles() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(3, 3, -50);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(0, 2, 4);
        graph.addEdge(2, 3, 1);
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.totalWeight() == 6, "Loops and duplicate queue entries must not lower the valid tree cost.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void equalWeightsAllowDifferentMinimumTrees() {
        WeightedGraph graph = new WeightedGraph(4, false);
        for (int first = 3; first >= 0; first--) {
            for (int second = first - 1; second >= 0; second--) {
                graph.addEdge(first, second, 5);
            }
        }
        MstResult primResult = new Prim().findMinimumSpanningForest(graph);
        MstResult kruskalResult = new Kruskal().findMinimumSpanningForest(graph);

        check(primResult.totalWeight() == 15, "Every minimum tree must contain three weight-five edges.");
        check(primResult.totalWeight() == kruskalResult.totalWeight(), "Both methods must agree on minimum cost.");
        check(primResult.componentCount() == kruskalResult.componentCount(), "Both methods must agree on connectivity.");
        verifyForest(graph, primResult);
        verifyForest(graph, kruskalResult);
    }

    private static void reversedInputEdgesAreSupported() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(1, 0, 4);
        graph.addEdge(2, 1, 5);
        MstResult result = new Prim().findMinimumSpanningForest(graph);

        check(result.totalWeight() == 9, "The undirected tree must be traversable in reverse input orientation.");
        verifyForest(graph, result);
    }

    private static void inputAndEarlierResultsArePreserved() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(1, 2, 5);
        graph.addEdge(0, 2, 20);
        graph.addEdge(0, 1, 4);
        List<Edge> originalEdges = List.copyOf(graph.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);
        List<Edge> originalSelection = List.copyOf(result.edges());

        check(graph.edges().equals(originalEdges), "Prim must not change input edges or their order.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(originalAdjacency.get(vertex)), "Prim must preserve adjacency.");
        }
        expectUnsupportedOperation(() -> result.edges().clear());
        expectUnsupportedOperation(() -> result.edges().add(new Edge(0, 2, 1)));
        graph.addEdge(0, 2, -100);
        check(algorithm.findMinimumSpanningForest(graph).totalWeight() == -96, "The updated graph must have a cheaper tree.");
        check(result.edges().equals(originalSelection) && result.totalWeight() == 9,
                "A later graph change or run must not alter an earlier result.");
    }

    private static void obsoleteQueueEntriesAreSkipped() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 1, -9);
        Prim algorithm = new Prim();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.totalWeight() == 3 && result.edges().size() == 2, "The stale edge must not create a cycle.");
        check(algorithm.edgeExaminations() == 7, "Adjacency scans must count both road directions and one loop.");
        check(algorithm.queueOffers() == 3, "Every non-loop logical edge must be queued once.");
        check(algorithm.queuePolls() == 3, "Polling must include the obsolete edge.");
        check(algorithm.queueComparisons() > 0, "Heap comparisons must be recorded.");
        verifyForest(graph, result);
        verifyQueueCounts(graph, algorithm);
    }

    private static void countersResetForRepeatedValidCalls() {
        Prim algorithm = new Prim();
        WeightedGraph first = new WeightedGraph(3, false);
        first.addEdge(0, 1, 2);
        first.addEdge(0, 2, 8);
        first.addEdge(1, 2, 1);
        algorithm.findMinimumSpanningForest(first);

        WeightedGraph second = new WeightedGraph(2, false);
        second.addEdge(1, 0, 7);
        MstResult result = algorithm.findMinimumSpanningForest(second);
        check(result.isConnected() && result.totalWeight() == 7, "Each run must use fresh visited state.");
        verifyQueueCounts(second, algorithm);
        check(algorithm.queueComparisons() == 0, "A one-edge queue must not retain old comparison counts.");

        algorithm.findMinimumSpanningForest(new WeightedGraph(0, false));
        check(algorithm.edgeExaminations() == 0 && algorithm.queueOffers() == 0
                && algorithm.queuePolls() == 0 && algorithm.queueComparisons() == 0,
                "An empty valid call must reset all counters.");
    }

    private static void invalidInputPreservesPreviousCounters() {
        Prim algorithm = new Prim();
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 1);
        algorithm.findMinimumSpanningForest(graph);
        long examinations = algorithm.edgeExaminations();
        long offers = algorithm.queueOffers();
        long polls = algorithm.queuePolls();
        long comparisons = algorithm.queueComparisons();

        expectIllegalArgument(() -> algorithm.findMinimumSpanningForest(new WeightedGraph(2, true)));
        expectNullPointer(() -> algorithm.findMinimumSpanningForest(null));
        check(algorithm.edgeExaminations() == examinations && algorithm.queueOffers() == offers
                && algorithm.queuePolls() == polls && algorithm.queueComparisons() == comparisons,
                "Rejected input must preserve the last valid run's counters.");
    }

    private static void verifyQueueCounts(WeightedGraph graph, Prim algorithm) {
        long loops = 0;
        for (Edge edge : graph.edges()) {
            if (edge.from() == edge.to()) {
                loops++;
            }
        }
        check(algorithm.edgeExaminations() == 2L * graph.edgeCount() - loops,
                "Every adjacency entry must be examined exactly once.");
        check(algorithm.queueOffers() == graph.edgeCount() - loops, "Every non-loop edge must be offered once.");
        check(algorithm.queuePolls() == algorithm.queueOffers(), "Every offered edge must eventually be polled.");
    }

    private static void verifyForest(WeightedGraph graph, MstResult result) {
        int[] originalGroups = separateGroups(graph.vertexCount());
        for (Edge edge : graph.edges()) {
            merge(originalGroups, edge.from(), edge.to());
        }
        int[] selectedGroups = separateGroups(graph.vertexCount());
        List<Edge> availableEdges = new ArrayList<>(graph.edges());
        long weight = 0;
        for (Edge edge : result.edges()) {
            boolean present = availableEdges.remove(edge);
            if (!present) {
                present = availableEdges.remove(new Edge(edge.to(), edge.from(), edge.weight()));
            }
            check(present, "Selected edges must come from the undirected input with matching multiplicity.");
            check(merge(selectedGroups, edge.from(), edge.to()), "Selected edges must not contain cycles.");
            weight += edge.weight();
        }
        for (int first = 0; first < graph.vertexCount(); first++) {
            for (int second = 0; second < graph.vertexCount(); second++) {
                check((originalGroups[first] == originalGroups[second])
                        == (selectedGroups[first] == selectedGroups[second]),
                        "The forest must preserve every original component.");
            }
        }
        boolean[] seen = new boolean[graph.vertexCount()];
        int components = 0;
        for (int group : originalGroups) {
            if (!seen[group]) {
                seen[group] = true;
                components++;
            }
        }
        check(result.edges().size() == graph.vertexCount() - components, "The forest edge count is incorrect.");
        check(result.componentCount() == components, "The forest component count is incorrect.");
        check(result.isConnected() == (components == 1), "The shared connectivity convention must be preserved.");
        check(result.totalWeight() == weight, "The total must equal the selected edge weights.");
    }

    private static int[] separateGroups(int vertices) {
        int[] groups = new int[vertices];
        for (int vertex = 0; vertex < vertices; vertex++) {
            groups[vertex] = vertex;
        }
        return groups;
    }

    private static boolean merge(int[] groups, int first, int second) {
        int from = groups[second];
        int to = groups[first];
        if (from == to) {
            return false;
        }
        for (int vertex = 0; vertex < groups.length; vertex++) {
            if (groups[vertex] == from) {
                groups[vertex] = to;
            }
        }
        return true;
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

    private static void expectNullPointer(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Expected NullPointerException.");
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
