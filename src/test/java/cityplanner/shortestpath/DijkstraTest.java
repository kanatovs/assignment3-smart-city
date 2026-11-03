package cityplanner.shortestpath;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class DijkstraTest {
    private DijkstraTest() {
    }

    public static void main(String[] args) {
        knownDirectedDistancesAndPathsAreCorrect();
        nonzeroSourceRespectsEdgeDirection();
        undirectedPathsUseBothOrientations();
        disconnectedVerticesHaveNoPath();
        isolatedSourceKeepsOtherVerticesUnreachable();
        singleVertexHasZeroDistance();
        selfLoopsDoNotChangeShortestPaths();
        zeroWeightCyclesDoNotCreatePredecessorCycles();
        equalDistancesAllowAnyOptimalPath();
        parallelEdgesUseTheCheapestConnection();
        maximumWeightsAndLongDistancesAreSupported();
        obsoleteQueueEntriesDoNotRescanEdges();
        countersIncludeValidationOfUnreachableEdges();
        countersResetForValidCalls();
        invalidSourceAndNullGraphAreRejected();
        negativeWeightsAreRejectedEverywhere();
        invalidInputPreservesPreviousCounters();
        invalidTargetVerticesAreRejected();
        inputAndEarlierResultsArePreserved();
        returnedPathsAreImmutable();
        resultCopiesInternalArrays();
        allSmallDirectedGraphsMatchDistanceOracle();
        allSmallUndirectedGraphsMatchDistanceOracle();
        generatedMultigraphsMatchDistanceOracle();
        longChainReconstructsWithoutRecursion();
        System.out.println("DijkstraTest passed.");
    }

    private static void knownDirectedDistancesAndPathsAreCorrect() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 2, 9);
        graph.addEdge(0, 5, 14);
        graph.addEdge(1, 2, 10);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 11);
        graph.addEdge(2, 5, 2);
        graph.addEdge(3, 4, 6);
        graph.addEdge(5, 4, 9);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, 7, 9, 20, 20, 11});
        check(result.pathTo(4).equals(List.of(0, 2, 5, 4)), "The known shortest path to vertex four is unique.");
        verifyCounters(graph, algorithm, result);
    }

    private static void nonzeroSourceRespectsEdgeDirection() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 4);
        graph.addEdge(1, 3, 2);

        verifyResult(graph, new Dijkstra().findShortestPaths(graph, 2), 2,
                new long[] {DijkstraResult.UNREACHABLE, 4, 0, 6});
    }

    private static void undirectedPathsUseBothOrientations() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 3, 4);
        graph.addEdge(3, 0, 20);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 3);

        verifyResult(graph, result, 3, new long[] {9, 7, 4, 0});
        check(result.pathTo(0).equals(List.of(3, 2, 1, 0)), "Undirected adjacency must allow reverse travel.");
        verifyCounters(graph, algorithm, result);
    }

    private static void disconnectedVerticesHaveNoPath() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(2, 3, 7);
        graph.addEdge(3, 2, 1);

        verifyResult(graph, new Dijkstra().findShortestPaths(graph, 0), 0,
                new long[] {0, 3, DijkstraResult.UNREACHABLE, DijkstraResult.UNREACHABLE,
                    DijkstraResult.UNREACHABLE});
    }

    private static void isolatedSourceKeepsOtherVerticesUnreachable() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 2);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 2);

        verifyResult(graph, result, 2, new long[] {DijkstraResult.UNREACHABLE, DijkstraResult.UNREACHABLE, 0});
        verifyCounters(graph, algorithm, result);
        check(algorithm.successfulRelaxations() == 0, "An isolated source cannot relax another vertex.");
        check(algorithm.queueOffers() == 1 && algorithm.queuePolls() == 1,
                "The source must enter and leave the queue even when isolated.");
    }

    private static void singleVertexHasZeroDistance() {
        for (boolean directed : new boolean[] {true, false}) {
            WeightedGraph graph = new WeightedGraph(1, directed);
            Dijkstra algorithm = new Dijkstra();
            DijkstraResult result = algorithm.findShortestPaths(graph, 0);

            verifyResult(graph, result, 0, new long[] {0});
            check(result.pathTo(0).equals(List.of(0)), "The source path must include the source itself.");
            verifyCounters(graph, algorithm, result);
            check(algorithm.queueComparisons() == 0, "A one-entry queue requires no weight comparisons.");
        }
    }

    private static void selfLoopsDoNotChangeShortestPaths() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 0, 0);
        graph.addEdge(0, 0, Integer.MAX_VALUE);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 1, 0);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, 5});
        verifyCounters(graph, algorithm, result);
        check(algorithm.successfulRelaxations() == 1, "Loops must not replace a shortest path by a cycle.");
    }

    private static void zeroWeightCyclesDoNotCreatePredecessorCycles() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 0, 0);
        graph.addEdge(2, 1, 0);
        graph.addEdge(0, 3, 0);
        graph.addEdge(3, 4, 2);
        graph.addEdge(2, 4, 2);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, 0, 0, 0, 2});
        verifyCounters(graph, algorithm, result);
    }

    private static void equalDistancesAllowAnyOptimalPath() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 2);

        verifyResult(graph, new Dijkstra().findShortestPaths(graph, 0), 0, new long[] {0, 1, 1, 3});
    }

    private static void parallelEdgesUseTheCheapestConnection() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 20);
        graph.addEdge(0, 1, 3);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 9);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, 3, 5});
        verifyCounters(graph, algorithm, result);
    }

    private static void maximumWeightsAndLongDistancesAreSupported() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        graph.addEdge(2, 3, Integer.MAX_VALUE);
        graph.addEdge(1, 4, Integer.MAX_VALUE - 1);
        graph.addEdge(4, 3, Integer.MAX_VALUE);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, Integer.MAX_VALUE, 2L * Integer.MAX_VALUE,
            3L * Integer.MAX_VALUE - 1, 2L * Integer.MAX_VALUE - 1});
        check(result.isReachable(3), "A distance greater than the integer range must remain reachable.");
        verifyCounters(graph, algorithm, result);
    }

    private static void obsoleteQueueEntriesDoNotRescanEdges() {
        WeightedGraph graph = staleEntryGraph();
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyResult(graph, result, 0, new long[] {0, 2, 1, 3});
        verifyCounters(graph, algorithm, result);
        check(algorithm.processingEdgeExaminations() == 5, "Stale entries must not rescan outgoing edges.");
        check(algorithm.successfulRelaxations() == 5, "The graph has five strict distance improvements.");
        check(algorithm.queueOffers() == 6 && algorithm.queuePolls() == 6,
                "Queue operations must include the source and two obsolete entries.");
        check(algorithm.queueComparisons() > 0, "Heap distance comparisons must be recorded.");
    }

    private static void countersIncludeValidationOfUnreachableEdges() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(2, 3, 8);
        graph.addEdge(3, 2, 0);
        graph.addEdge(4, 4, 5);
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);

        verifyCounters(graph, algorithm, result);
        check(algorithm.validationEdgeExaminations() == 4, "Validation must inspect every logical edge.");
        check(algorithm.processingEdgeExaminations() == 1, "Only reachable adjacency is processed.");
        check(algorithm.edgeExaminations() == 5, "Total edge examinations must include validation.");
        check(algorithm.successfulRelaxations() == 1, "Only the source's neighbor can be improved.");
        check(algorithm.queueComparisons() == 0, "This graph never has two queue entries at once.");
    }

    private static void countersResetForValidCalls() {
        Dijkstra algorithm = new Dijkstra();
        algorithm.findShortestPaths(staleEntryGraph(), 0);
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(1, 0, 7);
        DijkstraResult result = algorithm.findShortestPaths(graph, 1);

        verifyResult(graph, result, 1, new long[] {7, 0});
        verifyCounters(graph, algorithm, result);
        check(algorithm.successfulRelaxations() == 1 && algorithm.queueComparisons() == 0,
                "A new call must reset improvements and heap comparison counts.");
        WeightedGraph single = new WeightedGraph(1, true);
        DijkstraResult singleResult = algorithm.findShortestPaths(single, 0);
        verifyCounters(single, algorithm, singleResult);
        check(algorithm.edgeExaminations() == 0 && algorithm.successfulRelaxations() == 0,
                "An edgeless valid call must not retain earlier edge counters.");
    }

    private static void invalidSourceAndNullGraphAreRejected() {
        Dijkstra algorithm = new Dijkstra();
        WeightedGraph graph = new WeightedGraph(3, true);

        expectIllegalArgument(() -> algorithm.findShortestPaths(graph, -1));
        expectIllegalArgument(() -> algorithm.findShortestPaths(graph, 3));
        expectIllegalArgument(() -> algorithm.findShortestPaths(new WeightedGraph(0, true), 0));
        expectIllegalArgument(() -> algorithm.findShortestPaths(new WeightedGraph(0, false), -1));
        expectNullPointer(() -> algorithm.findShortestPaths(null, 0));
    }

    private static void negativeWeightsAreRejectedEverywhere() {
        for (boolean directed : new boolean[] {true, false}) {
            WeightedGraph reachable = new WeightedGraph(2, directed);
            reachable.addEdge(0, 1, -1);
            expectIllegalArgument(() -> new Dijkstra().findShortestPaths(reachable, 0));

            WeightedGraph unreachable = new WeightedGraph(4, directed);
            unreachable.addEdge(0, 1, 3);
            unreachable.addEdge(2, 3, Integer.MIN_VALUE);
            expectIllegalArgument(() -> new Dijkstra().findShortestPaths(unreachable, 0));

            WeightedGraph loop = new WeightedGraph(2, directed);
            loop.addEdge(1, 1, -7);
            expectIllegalArgument(() -> new Dijkstra().findShortestPaths(loop, 0));
        }
    }

    private static void invalidInputPreservesPreviousCounters() {
        Dijkstra algorithm = new Dijkstra();
        algorithm.findShortestPaths(staleEntryGraph(), 0);
        long[] previous = counters(algorithm);
        WeightedGraph negative = new WeightedGraph(4, true);
        negative.addEdge(0, 1, 3);
        negative.addEdge(2, 3, -1);

        expectIllegalArgument(() -> algorithm.findShortestPaths(negative, 0));
        check(Arrays.equals(previous, counters(algorithm)), "Negative input must preserve all prior counters.");
        expectIllegalArgument(() -> algorithm.findShortestPaths(new WeightedGraph(1, true), 1));
        check(Arrays.equals(previous, counters(algorithm)), "An invalid source must preserve all prior counters.");
        expectNullPointer(() -> algorithm.findShortestPaths(null, 0));
        check(Arrays.equals(previous, counters(algorithm)), "Null input must preserve all prior counters.");
    }

    private static void invalidTargetVerticesAreRejected() {
        DijkstraResult result = new Dijkstra().findShortestPaths(new WeightedGraph(2, true), 0);
        for (int vertex : new int[] {-1, 2}) {
            expectIllegalArgument(() -> result.distanceTo(vertex));
            expectIllegalArgument(() -> result.isReachable(vertex));
            expectIllegalArgument(() -> result.pathTo(vertex));
        }
    }

    private static void inputAndEarlierResultsArePreserved() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 5);
        graph.addEdge(0, 2, 20);
        List<Edge> originalEdges = List.copyOf(graph.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);
        List<Integer> originalPath = result.pathTo(2);

        check(graph.edges().equals(originalEdges), "Shortest-path search must preserve logical edge order.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(originalAdjacency.get(vertex)), "Input adjacency must be preserved.");
        }
        graph.addEdge(0, 2, 1);
        check(algorithm.findShortestPaths(graph, 0).distanceTo(2) == 1, "A later run must use the changed graph.");
        check(result.distanceTo(2) == 9 && result.pathTo(2).equals(List.of(0, 1, 2)),
                "Earlier distances and predecessors must be detached from later calls.");
        check(originalPath.equals(List.of(0, 1, 2)), "Earlier returned paths must remain unchanged.");
    }

    private static void returnedPathsAreImmutable() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 2);
        DijkstraResult result = new Dijkstra().findShortestPaths(graph, 0);

        expectUnsupportedOperation(() -> result.pathTo(1).clear());
        expectUnsupportedOperation(() -> result.pathTo(1).add(2));
        expectUnsupportedOperation(() -> result.pathTo(0).set(0, 2));
        expectUnsupportedOperation(() -> result.pathTo(2).add(0));
    }

    private static void resultCopiesInternalArrays() {
        long[] distances = {0, 5};
        int[] predecessors = {-1, 0};
        DijkstraResult result = new DijkstraResult(0, distances, predecessors);
        distances[1] = 100;
        predecessors[1] = -1;

        check(result.distanceTo(1) == 5, "The result must copy its distance array.");
        check(result.pathTo(1).equals(List.of(0, 1)), "The result must copy its predecessor array.");
    }

    private static void allSmallDirectedGraphsMatchDistanceOracle() {
        for (int mask = 0; mask < 512; mask++) {
            WeightedGraph graph = new WeightedGraph(3, true);
            for (int bit = 0; bit < 9; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    graph.addEdge(bit / 3, bit % 3, (3 * bit + mask) % 6);
                }
            }
            verifyAgainstOracle(graph);
        }
    }

    private static void allSmallUndirectedGraphsMatchDistanceOracle() {
        int[][] pairs = {{0, 0}, {0, 1}, {0, 2}, {1, 1}, {1, 2}, {2, 2}};
        for (int mask = 0; mask < 64; mask++) {
            WeightedGraph graph = new WeightedGraph(3, false);
            for (int bit = 0; bit < pairs.length; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    graph.addEdge(pairs[bit][0], pairs[bit][1], (bit + mask) % 5);
                }
            }
            verifyAgainstOracle(graph);
        }
    }

    private static void generatedMultigraphsMatchDistanceOracle() {
        Random random = new Random(41008);
        for (int example = 0; example < 60; example++) {
            int count = 2 + random.nextInt(6);
            WeightedGraph graph = new WeightedGraph(count, example % 2 == 0);
            int edges = random.nextInt(20);
            for (int edge = 0; edge < edges; edge++) {
                graph.addEdge(random.nextInt(count), random.nextInt(count), random.nextInt(11));
            }
            verifyAgainstOracle(graph);
        }
    }

    private static void longChainReconstructsWithoutRecursion() {
        int count = 20000;
        WeightedGraph graph = new WeightedGraph(count, true);
        for (int vertex = 1; vertex < count; vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        Dijkstra algorithm = new Dijkstra();
        DijkstraResult result = algorithm.findShortestPaths(graph, 0);
        List<Integer> path = result.pathTo(count - 1);

        check(result.distanceTo(count - 1) == count - 1, "The chain distance must equal its edge count.");
        check(path.size() == count, "The chain path must contain every vertex.");
        for (int vertex = 0; vertex < count; vertex++) {
            check(path.get(vertex) == vertex, "The unique chain path must follow consecutive vertices.");
        }
        verifyCounters(graph, algorithm, result);
    }

    private static WeightedGraph staleEntryGraph() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 1);
        graph.addEdge(0, 3, 100);
        graph.addEdge(2, 1, 1);
        graph.addEdge(1, 3, 1);
        return graph;
    }

    private static void verifyAgainstOracle(WeightedGraph graph) {
        long[][] expected = shortestDistances(graph);
        Dijkstra algorithm = new Dijkstra();
        for (int source = 0; source < graph.vertexCount(); source++) {
            DijkstraResult result = algorithm.findShortestPaths(graph, source);
            verifyResult(graph, result, source, expected[source]);
            verifyCounters(graph, algorithm, result);
        }
    }

    private static long[][] shortestDistances(WeightedGraph graph) {
        int count = graph.vertexCount();
        long[][] distances = new long[count][count];
        for (int from = 0; from < count; from++) {
            Arrays.fill(distances[from], DijkstraResult.UNREACHABLE);
            distances[from][from] = 0;
            for (Edge edge : graph.edgesFrom(from)) {
                distances[from][edge.to()] = Math.min(distances[from][edge.to()], edge.weight());
            }
        }
        for (int intermediate = 0; intermediate < count; intermediate++) {
            for (int from = 0; from < count; from++) {
                for (int to = 0; to < count; to++) {
                    if (distances[from][intermediate] != DijkstraResult.UNREACHABLE
                            && distances[intermediate][to] != DijkstraResult.UNREACHABLE) {
                        long candidate = Math.addExact(distances[from][intermediate], distances[intermediate][to]);
                        distances[from][to] = Math.min(distances[from][to], candidate);
                    }
                }
            }
        }
        return distances;
    }

    private static void verifyResult(WeightedGraph graph, DijkstraResult result, int source, long[] expected) {
        check(result.source() == source, "The result must remember its source.");
        check(result.vertexCount() == graph.vertexCount(), "The result must retain all graph vertices.");
        for (int target = 0; target < graph.vertexCount(); target++) {
            check(result.distanceTo(target) == expected[target], "A shortest distance differs for vertex " + target + ".");
            check(result.isReachable(target) == (expected[target] != DijkstraResult.UNREACHABLE),
                    "Reachability must agree with the distance sentinel.");
            verifyPath(graph, result, target);
        }
    }

    private static void verifyPath(WeightedGraph graph, DijkstraResult result, int target) {
        List<Integer> path = result.pathTo(target);
        if (!result.isReachable(target)) {
            check(path.isEmpty(), "An unreachable vertex must have no path.");
            return;
        }
        check(!path.isEmpty(), "A reachable vertex must have a path.");
        check(path.get(0) == result.source(), "A path must start at its source.");
        check(path.get(path.size() - 1) == target, "A path must end at its target.");
        boolean[] seen = new boolean[graph.vertexCount()];
        long cost = 0;
        for (int index = 0; index < path.size(); index++) {
            int vertex = path.get(index);
            check(vertex >= 0 && vertex < graph.vertexCount(), "Path vertices must be in range.");
            check(!seen[vertex], "A reconstructed path must not contain a predecessor cycle.");
            seen[vertex] = true;
            if (index > 0) {
                long weight = DijkstraResult.UNREACHABLE;
                for (Edge edge : graph.edgesFrom(path.get(index - 1))) {
                    if (edge.to() == vertex) {
                        weight = Math.min(weight, edge.weight());
                    }
                }
                check(weight != DijkstraResult.UNREACHABLE, "Every path step must be an actual directed adjacency.");
                cost = Math.addExact(cost, weight);
            }
        }
        check(cost == result.distanceTo(target), "Actual path edges must sum to the reported shortest distance.");
    }

    private static void verifyCounters(WeightedGraph graph, Dijkstra algorithm, DijkstraResult result) {
        long reachableAdjacency = 0;
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (result.isReachable(vertex)) {
                reachableAdjacency += graph.edgesFrom(vertex).size();
            }
        }
        check(algorithm.validationEdgeExaminations() == graph.edgeCount(), "Validation must scan all logical edges.");
        check(algorithm.processingEdgeExaminations() == reachableAdjacency,
                "Reachable adjacency must be processed once, without stale-entry rescans.");
        check(algorithm.edgeExaminations() == graph.edgeCount() + reachableAdjacency,
                "Total examinations must include validation and processing.");
        check(algorithm.relaxationAttempts() == reachableAdjacency, "Each processed edge must attempt relaxation.");
        check(algorithm.queueOffers() == algorithm.successfulRelaxations() + 1,
                "The source and each strict improvement must create one queue entry.");
        check(algorithm.queuePolls() == algorithm.queueOffers(), "Every offered entry must eventually be polled.");
    }

    private static long[] counters(Dijkstra algorithm) {
        return new long[] {algorithm.validationEdgeExaminations(), algorithm.processingEdgeExaminations(),
            algorithm.edgeExaminations(), algorithm.relaxationAttempts(), algorithm.successfulRelaxations(),
            algorithm.queueOffers(), algorithm.queuePolls(), algorithm.queueComparisons()};
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
