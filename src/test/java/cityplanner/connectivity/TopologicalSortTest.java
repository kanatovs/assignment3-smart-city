package cityplanner.connectivity;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;

public final class TopologicalSortTest {
    private TopologicalSortTest() {
    }

    public static void main(String[] args) {
        knownDagRespectsEveryDependency();
        disconnectedDagIncludesEveryComponent();
        isolatedVerticesAppearOnce();
        emptyGraphHasAValidEmptyOrder();
        singleVertexHasAValidOrder();
        directedCycleHasNoOrder();
        cycleInLaterComponentIsDetected();
        acyclicPrefixIsNotReturnedAsACompleteOrder();
        selfLoopIsACycle();
        parallelEdgesAndArbitraryWeightsAreSupported();
        inputAndEarlierResultsArePreserved();
        resultCopiesAndProtectsItsOrder();
        countersDescribeBothPhases();
        countersResetForValidCalls();
        invalidInputPreservesPreviousCounters();
        allSmallDirectedGraphsMatchPermutationOracle();
        longChainDoesNotRequireRecursion();
        System.out.println("TopologicalSortTest passed.");
    }

    private static void knownDagRespectsEveryDependency() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(5, 2, 1);
        graph.addEdge(5, 0, 1);
        graph.addEdge(4, 0, 1);
        graph.addEdge(4, 1, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 1, 1);

        verifyOrder(graph, new TopologicalSort().sort(graph));
    }

    private static void disconnectedDagIncludesEveryComponent() {
        WeightedGraph graph = new WeightedGraph(7, true);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 4);
        graph.addEdge(4, 5, 5);

        verifyOrder(graph, new TopologicalSort().sort(graph));
    }

    private static void isolatedVerticesAppearOnce() {
        WeightedGraph graph = new WeightedGraph(4, true);
        verifyOrder(graph, new TopologicalSort().sort(graph));
    }

    private static void emptyGraphHasAValidEmptyOrder() {
        WeightedGraph graph = new WeightedGraph(0, true);
        TopologicalSort algorithm = new TopologicalSort();
        TopologicalSortResult result = algorithm.sort(graph);

        verifyOrder(graph, result);
        check(result.order().isEmpty(), "The empty graph must have an empty valid order.");
        check(algorithm.edgeExaminations() == 0, "The empty graph has no edges to examine.");
    }

    private static void singleVertexHasAValidOrder() {
        WeightedGraph graph = new WeightedGraph(1, true);
        TopologicalSortResult result = new TopologicalSort().sort(graph);

        check(result.order().equals(List.of(0)), "The single vertex must be included.");
        verifyOrder(graph, result);
    }

    private static void directedCycleHasNoOrder() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        verifyCycle(new TopologicalSort().sort(graph));
    }

    private static void cycleInLaterComponentIsDetected() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);

        verifyCycle(new TopologicalSort().sort(graph));
    }

    private static void acyclicPrefixIsNotReturnedAsACompleteOrder() {
        WeightedGraph graph = prefixAndCycle();
        TopologicalSort algorithm = new TopologicalSort();
        TopologicalSortResult result = algorithm.sort(graph);

        verifyCycle(result);
        check(algorithm.processingEdgeExaminations() == 2,
                "The acyclic prefix must be processed before the cycle blocks progress.");
    }

    private static void selfLoopIsACycle() {
        WeightedGraph graph = new WeightedGraph(1, true);
        graph.addEdge(0, 0, -100);
        TopologicalSort algorithm = new TopologicalSort();

        verifyCycle(algorithm.sort(graph));
        check(algorithm.indegreeEdgeExaminations() == 1, "The self-loop must contribute to indegree.");
        check(algorithm.processingEdgeExaminations() == 0, "A self-loop prevents its vertex from being processed.");
    }

    private static void parallelEdgesAndArbitraryWeightsAreSupported() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, Integer.MIN_VALUE);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, 0);
        graph.addEdge(0, 2, -7);
        TopologicalSort algorithm = new TopologicalSort();
        TopologicalSortResult result = algorithm.sort(graph);

        check(result.order().equals(List.of(0, 1, 2)), "Parallel edges must not enqueue a vertex prematurely.");
        verifyOrder(graph, result);
        check(algorithm.edgeExaminations() == 8, "Parallel edges must be counted separately in both phases.");
    }

    private static void inputAndEarlierResultsArePreserved() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 9);
        graph.addEdge(1, 2, 4);
        List<Edge> originalEdges = List.copyOf(graph.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }
        TopologicalSort algorithm = new TopologicalSort();
        TopologicalSortResult result = algorithm.sort(graph);

        check(graph.edges().equals(originalEdges), "Sorting must preserve input edges and their order.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(originalAdjacency.get(vertex)), "Sorting must preserve adjacency.");
        }
        graph.addEdge(2, 0, 1);
        verifyCycle(algorithm.sort(graph));
        check(!result.hasCycle() && result.order().equals(List.of(0, 1, 2)),
                "Graph changes and later calls must not alter an earlier result.");
    }

    private static void resultCopiesAndProtectsItsOrder() {
        List<Integer> mutableOrder = new ArrayList<>(List.of(0, 1, 2));
        TopologicalSortResult result = new TopologicalSortResult(mutableOrder, false);
        mutableOrder.clear();

        check(result.order().equals(List.of(0, 1, 2)), "The result must hold a detached snapshot.");
        expectUnsupportedOperation(() -> result.order().clear());
        expectUnsupportedOperation(() -> result.order().add(3));
        expectIllegalArgument(() -> new TopologicalSortResult(List.of(0), true));
        expectNullPointer(() -> new TopologicalSortResult(null, false));
        List<Integer> nullVertex = new ArrayList<>();
        nullVertex.add(null);
        expectNullPointer(() -> new TopologicalSortResult(nullVertex, false));
        verifyCycle(new TopologicalSortResult(List.of(), true));
    }

    private static void countersDescribeBothPhases() {
        WeightedGraph dag = new WeightedGraph(4, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 1, 2);
        dag.addEdge(1, 2, 3);
        dag.addEdge(0, 2, 4);
        TopologicalSort algorithm = new TopologicalSort();
        verifyOrder(dag, algorithm.sort(dag));

        check(algorithm.indegreeEdgeExaminations() == 4, "Indegree construction must scan all four edges.");
        check(algorithm.processingEdgeExaminations() == 4, "A DAG must process all four edges.");
        check(algorithm.edgeExaminations() == 8, "The total must include both phases.");

        verifyCycle(algorithm.sort(prefixAndCycle()));
        check(algorithm.indegreeEdgeExaminations() == 4, "Even a cyclic graph must have every indegree counted.");
        check(algorithm.processingEdgeExaminations() == 2, "Only processed vertices contribute outgoing scans.");
        check(algorithm.edgeExaminations() == 6, "A cycle can stop processing before all edges are scanned twice.");
    }

    private static void countersResetForValidCalls() {
        TopologicalSort algorithm = new TopologicalSort();
        verifyCycle(algorithm.sort(prefixAndCycle()));

        WeightedGraph dag = new WeightedGraph(2, true);
        dag.addEdge(1, 0, 7);
        verifyOrder(dag, algorithm.sort(dag));
        check(algorithm.indegreeEdgeExaminations() == 1 && algorithm.processingEdgeExaminations() == 1,
                "A valid call must use fresh counters and indegree state.");
        algorithm.sort(new WeightedGraph(0, true));
        check(algorithm.indegreeEdgeExaminations() == 0 && algorithm.processingEdgeExaminations() == 0
                && algorithm.edgeExaminations() == 0, "An empty valid call must reset every counter.");
    }

    private static void invalidInputPreservesPreviousCounters() {
        TopologicalSort algorithm = new TopologicalSort();
        algorithm.sort(prefixAndCycle());
        long indegreeExaminations = algorithm.indegreeEdgeExaminations();
        long processingExaminations = algorithm.processingEdgeExaminations();
        long totalExaminations = algorithm.edgeExaminations();

        expectIllegalArgument(() -> algorithm.sort(new WeightedGraph(2, false)));
        expectIllegalArgument(() -> algorithm.sort(new WeightedGraph(0, false)));
        expectNullPointer(() -> algorithm.sort(null));
        check(algorithm.indegreeEdgeExaminations() == indegreeExaminations
                && algorithm.processingEdgeExaminations() == processingExaminations
                && algorithm.edgeExaminations() == totalExaminations,
                "Rejected input must preserve the previous valid call's counters.");
    }

    private static void allSmallDirectedGraphsMatchPermutationOracle() {
        for (int mask = 0; mask < 512; mask++) {
            WeightedGraph graph = new WeightedGraph(3, true);
            for (int bit = 0; bit < 9; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    graph.addEdge(bit / 3, bit % 3, bit - 4);
                }
            }
            boolean hasOrder = hasValidPermutation(graph);
            TopologicalSort algorithm = new TopologicalSort();
            TopologicalSortResult result = algorithm.sort(graph);
            check(result.hasCycle() == !hasOrder, "Cycle status disagrees with the permutation oracle: " + mask);
            if (hasOrder) {
                verifyOrder(graph, result);
                check(algorithm.processingEdgeExaminations() == graph.edgeCount(),
                        "Every DAG edge must be processed: " + mask);
            } else {
                verifyCycle(result);
            }
            check(algorithm.indegreeEdgeExaminations() == graph.edgeCount(),
                    "Indegree construction must scan every edge: " + mask);
            check(algorithm.processingEdgeExaminations() <= graph.edgeCount(),
                    "Outgoing edges must not be processed more than once: " + mask);
            check(algorithm.edgeExaminations()
                    == algorithm.indegreeEdgeExaminations() + algorithm.processingEdgeExaminations(),
                    "The total counter must equal its phases: " + mask);
        }
    }

    private static void longChainDoesNotRequireRecursion() {
        int vertexCount = 20000;
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        for (int vertex = 1; vertex < vertexCount; vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        TopologicalSort algorithm = new TopologicalSort();
        TopologicalSortResult result = algorithm.sort(graph);

        verifyOrder(graph, result);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            check(result.order().get(vertex) == vertex, "A chain has only one valid ordering.");
        }
        check(algorithm.edgeExaminations() == 2L * (vertexCount - 1), "Every chain edge must be examined twice.");
    }

    private static WeightedGraph prefixAndCycle() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);
        return graph;
    }

    private static boolean hasValidPermutation(WeightedGraph graph) {
        int[][] permutations = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
        };
        for (int[] permutation : permutations) {
            int[] position = new int[3];
            for (int index = 0; index < permutation.length; index++) {
                position[permutation[index]] = index;
            }
            boolean valid = true;
            for (Edge edge : graph.edges()) {
                if (position[edge.from()] >= position[edge.to()]) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return true;
            }
        }
        return false;
    }

    private static void verifyOrder(WeightedGraph graph, TopologicalSortResult result) {
        check(!result.hasCycle(), "A DAG must not be reported as cyclic.");
        check(result.order().size() == graph.vertexCount(), "The order must contain every vertex.");
        boolean[] seen = new boolean[graph.vertexCount()];
        int[] position = new int[graph.vertexCount()];
        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);
            check(vertex >= 0 && vertex < graph.vertexCount(), "The order contains an invalid vertex.");
            check(!seen[vertex], "Every vertex must appear exactly once.");
            seen[vertex] = true;
            position[vertex] = index;
        }
        for (Edge edge : graph.edges()) {
            check(position[edge.from()] < position[edge.to()], "Every dependency must point forward in the order.");
        }
    }

    private static void verifyCycle(TopologicalSortResult result) {
        check(result.hasCycle(), "A directed cycle must prevent a topological order.");
        check(result.order().isEmpty(), "A partial order must not be presented as a complete topological order.");
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
