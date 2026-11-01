package cityplanner.connectivity;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KosarajuTest {
    private KosarajuTest() {
    }

    public static void main(String[] args) {
        knownComponentsKeepOneWayConnectionsSeparate();
        oneWayChainDoesNotMergeVertices();
        directedCycleFormsOneComponent();
        dagHasOneComponentPerVertex();
        disconnectedCyclesRemainSeparate();
        isolatedVerticesAreIncluded();
        emptyGraphHasNoComponents();
        singleVertexFormsOneComponent();
        selfLoopsDoNotJoinSeparateVertices();
        parallelEdgesAndArbitraryWeightsAreSupported();
        branchingDfsUsesFinishingOrder();
        inputAndEarlierResultsArePreserved();
        resultCopiesAndProtectsBothListLevels();
        invalidResultArgumentsAreRejected();
        countersDescribeAllThreePhases();
        countersResetForValidCalls();
        invalidInputPreservesPreviousCounters();
        allSmallDirectedGraphsMatchReachabilityOracle();
        longChainDoesNotRequireRecursion();
        longCycleDoesNotRequireRecursion();
        System.out.println("KosarajuTest passed.");
    }

    private static void knownComponentsKeepOneWayConnectionsSeparate() {
        WeightedGraph graph = new WeightedGraph(8, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        graph.addEdge(4, 5, 1);
        graph.addEdge(5, 5, 1);
        graph.addEdge(6, 7, 1);

        verifyGroups(8, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0, 1, 2}, {3, 4}, {5}, {6}, {7}});
    }

    private static void oneWayChainDoesNotMergeVertices() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);

        verifyGroups(4, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0}, {1}, {2}, {3}});
    }

    private static void directedCycleFormsOneComponent() {
        WeightedGraph graph = new WeightedGraph(5, true);
        for (int vertex = 0; vertex < 5; vertex++) {
            graph.addEdge(vertex, (vertex + 1) % 5, 1);
        }
        graph.addEdge(0, 3, 2);
        graph.addEdge(2, 4, 2);

        verifyGroups(5, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0, 1, 2, 3, 4}});
    }

    private static void dagHasOneComponentPerVertex() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(5, 2, 1);
        graph.addEdge(5, 0, 1);
        graph.addEdge(4, 0, 1);
        graph.addEdge(4, 1, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 1, 1);

        verifyGroups(6, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0}, {1}, {2}, {3}, {4}, {5}});
    }

    private static void disconnectedCyclesRemainSeparate() {
        WeightedGraph graph = new WeightedGraph(7, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 2, 1);

        verifyGroups(7, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0, 1}, {2, 3, 4}, {5}, {6}});
    }

    private static void isolatedVerticesAreIncluded() {
        WeightedGraph graph = new WeightedGraph(4, true);
        verifyGroups(4, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0}, {1}, {2}, {3}});
    }

    private static void emptyGraphHasNoComponents() {
        Kosaraju algorithm = new Kosaraju();
        SccResult result = algorithm.findStronglyConnectedComponents(new WeightedGraph(0, true));

        verifyGroups(0, result, new int[][] {});
        check(result.components().isEmpty(), "An empty graph must have no components.");
        verifyCounters(algorithm, 0);
    }

    private static void singleVertexFormsOneComponent() {
        verifyGroups(1, new Kosaraju().findStronglyConnectedComponents(new WeightedGraph(1, true)),
                new int[][] {{0}});
    }

    private static void selfLoopsDoNotJoinSeparateVertices() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 0, -7);
        graph.addEdge(0, 0, 9);
        graph.addEdge(1, 1, 0);
        graph.addEdge(2, 2, 5);
        Kosaraju algorithm = new Kosaraju();

        verifyGroups(3, algorithm.findStronglyConnectedComponents(graph), new int[][] {{0}, {1}, {2}});
        verifyCounters(algorithm, graph.edgeCount());
    }

    private static void parallelEdgesAndArbitraryWeightsAreSupported() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, Integer.MIN_VALUE);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 0, 0);
        graph.addEdge(1, 2, -2);
        graph.addEdge(2, 3, 4);
        graph.addEdge(2, 3, 4);
        graph.addEdge(3, 2, -8);
        graph.addEdge(4, 4, 0);
        Kosaraju algorithm = new Kosaraju();

        verifyGroups(5, algorithm.findStronglyConnectedComponents(graph), new int[][] {{0, 1}, {2, 3}, {4}});
        verifyCounters(algorithm, graph.edgeCount());
    }

    private static void branchingDfsUsesFinishingOrder() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(3, 0, 1);
        graph.addEdge(2, 4, 1);
        graph.addEdge(4, 2, 1);

        verifyGroups(5, new Kosaraju().findStronglyConnectedComponents(graph),
                new int[][] {{0, 1, 3}, {2, 4}});
    }

    private static void inputAndEarlierResultsArePreserved() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 0, 7);
        graph.addEdge(1, 2, 2);
        List<Edge> originalEdges = List.copyOf(graph.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }
        Kosaraju algorithm = new Kosaraju();
        SccResult result = algorithm.findStronglyConnectedComponents(graph);
        verifyGroups(3, result, new int[][] {{0, 1}, {2}});

        check(graph.edges().equals(originalEdges), "Component search must preserve input edges and their order.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(originalAdjacency.get(vertex)), "Input adjacency must be preserved.");
        }
        graph.addEdge(2, 0, -3);
        verifyGroups(3, algorithm.findStronglyConnectedComponents(graph), new int[][] {{0, 1, 2}});
        verifyGroups(3, result, new int[][] {{0, 1}, {2}});
    }

    private static void resultCopiesAndProtectsBothListLevels() {
        List<Integer> first = new ArrayList<>(List.of(0, 1));
        List<Integer> second = new ArrayList<>(List.of(2));
        List<List<Integer>> mutableComponents = new ArrayList<>();
        mutableComponents.add(first);
        mutableComponents.add(second);
        SccResult result = new SccResult(mutableComponents);
        first.clear();
        second.add(3);
        mutableComponents.clear();

        verifyGroups(3, result, new int[][] {{0, 1}, {2}});
        expectUnsupportedOperation(() -> result.components().clear());
        expectUnsupportedOperation(() -> result.components().add(List.of(3)));
        expectUnsupportedOperation(() -> result.components().get(0).clear());
        expectUnsupportedOperation(() -> result.components().get(0).add(3));
        expectUnsupportedOperation(() -> result.components().get(0).set(0, 9));
        check(new SccResult(List.of()).componentCount() == 0, "An empty outer component list must be valid.");
    }

    private static void invalidResultArgumentsAreRejected() {
        expectIllegalArgument(() -> new SccResult(List.of(List.of())));
        expectNullPointer(() -> new SccResult(null));
        List<List<Integer>> nullComponent = new ArrayList<>();
        nullComponent.add(null);
        expectNullPointer(() -> new SccResult(nullComponent));
        List<Integer> nullVertex = new ArrayList<>();
        nullVertex.add(null);
        expectNullPointer(() -> new SccResult(List.of(nullVertex)));
    }

    private static void countersDescribeAllThreePhases() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 0, 3);
        graph.addEdge(2, 2, 4);
        graph.addEdge(1, 3, 5);
        Kosaraju algorithm = new Kosaraju();

        verifyGroups(4, algorithm.findStronglyConnectedComponents(graph), new int[][] {{0, 1}, {2}, {3}});
        verifyCounters(algorithm, 5);
    }

    private static void countersResetForValidCalls() {
        Kosaraju algorithm = new Kosaraju();
        WeightedGraph cycle = new WeightedGraph(3, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);
        algorithm.findStronglyConnectedComponents(cycle);

        WeightedGraph chain = new WeightedGraph(2, true);
        chain.addEdge(1, 0, 7);
        verifyGroups(2, algorithm.findStronglyConnectedComponents(chain), new int[][] {{0}, {1}});
        verifyCounters(algorithm, 1);
        verifyGroups(0, algorithm.findStronglyConnectedComponents(new WeightedGraph(0, true)), new int[][] {});
        verifyCounters(algorithm, 0);
    }

    private static void invalidInputPreservesPreviousCounters() {
        Kosaraju algorithm = new Kosaraju();
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 1);
        algorithm.findStronglyConnectedComponents(graph);

        expectIllegalArgument(() -> algorithm.findStronglyConnectedComponents(new WeightedGraph(2, false)));
        expectIllegalArgument(() -> algorithm.findStronglyConnectedComponents(new WeightedGraph(0, false)));
        expectNullPointer(() -> algorithm.findStronglyConnectedComponents(null));
        verifyCounters(algorithm, 1);
    }

    private static void allSmallDirectedGraphsMatchReachabilityOracle() {
        for (int mask = 0; mask < 512; mask++) {
            WeightedGraph graph = new WeightedGraph(3, true);
            for (int bit = 0; bit < 9; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    graph.addEdge(bit / 3, bit % 3, bit - 4);
                }
            }
            boolean[][] reachable = reachability(graph);
            Kosaraju algorithm = new Kosaraju();
            SccResult result = algorithm.findStronglyConnectedComponents(graph);
            int[] groups = componentLabels(3, result);
            for (int first = 0; first < 3; first++) {
                for (int second = 0; second < 3; second++) {
                    check((groups[first] == groups[second]) == (reachable[first][second] && reachable[second][first]),
                            "Component membership disagrees with mutual reachability: " + mask);
                }
            }
            verifyCounters(algorithm, graph.edgeCount());
        }
    }

    private static void longChainDoesNotRequireRecursion() {
        int vertexCount = 20000;
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        for (int vertex = 1; vertex < vertexCount; vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        Kosaraju algorithm = new Kosaraju();
        SccResult result = algorithm.findStronglyConnectedComponents(graph);

        componentLabels(vertexCount, result);
        check(result.componentCount() == vertexCount, "Every chain vertex must be a separate component.");
        for (List<Integer> component : result.components()) {
            check(component.size() == 1, "A one-way chain has only singleton components.");
        }
        verifyCounters(algorithm, vertexCount - 1);
    }

    private static void longCycleDoesNotRequireRecursion() {
        int vertexCount = 20000;
        WeightedGraph graph = new WeightedGraph(vertexCount, true);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            graph.addEdge(vertex, (vertex + 1) % vertexCount, 1);
        }
        Kosaraju algorithm = new Kosaraju();
        SccResult result = algorithm.findStronglyConnectedComponents(graph);

        componentLabels(vertexCount, result);
        check(result.componentCount() == 1, "Every vertex of a directed cycle must share a component.");
        check(result.components().get(0).size() == vertexCount, "The cycle component must contain every vertex.");
        verifyCounters(algorithm, vertexCount);
    }

    private static boolean[][] reachability(WeightedGraph graph) {
        int count = graph.vertexCount();
        boolean[][] reachable = new boolean[count][count];
        for (int vertex = 0; vertex < count; vertex++) {
            reachable[vertex][vertex] = true;
        }
        for (Edge edge : graph.edges()) {
            reachable[edge.from()][edge.to()] = true;
        }
        for (int intermediate = 0; intermediate < count; intermediate++) {
            for (int from = 0; from < count; from++) {
                for (int to = 0; to < count; to++) {
                    reachable[from][to] |= reachable[from][intermediate] && reachable[intermediate][to];
                }
            }
        }
        return reachable;
    }

    private static void verifyGroups(int vertexCount, SccResult result, int[][] expected) {
        check(result.componentCount() == expected.length, "The component count differs from the expected partition.");
        int[] actualLabels = componentLabels(vertexCount, result);
        int[] expectedLabels = new int[vertexCount];
        Arrays.fill(expectedLabels, -1);
        for (int group = 0; group < expected.length; group++) {
            for (int vertex : expected[group]) {
                check(vertex >= 0 && vertex < vertexCount && expectedLabels[vertex] == -1,
                        "The expected partition must contain valid unique vertices.");
                expectedLabels[vertex] = group;
            }
        }
        for (int first = 0; first < vertexCount; first++) {
            check(expectedLabels[first] != -1, "The expected partition must cover every vertex.");
            for (int second = 0; second < vertexCount; second++) {
                check((actualLabels[first] == actualLabels[second]) == (expectedLabels[first] == expectedLabels[second]),
                        "The result must match expected groups regardless of list order.");
            }
        }
    }

    private static int[] componentLabels(int vertexCount, SccResult result) {
        check(result.componentCount() == result.components().size(), "The component count must match the list size.");
        int[] labels = new int[vertexCount];
        Arrays.fill(labels, -1);
        for (int group = 0; group < result.components().size(); group++) {
            List<Integer> component = result.components().get(group);
            check(!component.isEmpty(), "Returned components must not be empty.");
            for (int vertex : component) {
                check(vertex >= 0 && vertex < vertexCount, "A returned vertex is outside the input graph.");
                check(labels[vertex] == -1, "Every vertex must belong to exactly one component.");
                labels[vertex] = group;
            }
        }
        for (int label : labels) {
            check(label != -1, "Every input vertex must be covered by a component.");
        }
        return labels;
    }

    private static void verifyCounters(Kosaraju algorithm, int edgeCount) {
        check(algorithm.firstPassEdgeExaminations() == edgeCount, "The first DFS pass must examine every edge.");
        check(algorithm.reversalEdgeExaminations() == edgeCount, "Graph reversal must examine every edge.");
        check(algorithm.secondPassEdgeExaminations() == edgeCount, "The second DFS pass must examine every edge.");
        check(algorithm.edgeExaminations() == 3L * edgeCount, "The total must include all three phases.");
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
