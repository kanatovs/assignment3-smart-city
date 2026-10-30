package cityplanner.mst;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class KruskalTest {
    private KruskalTest() {
    }

    public static void main(String[] args) {
        connectedGraphHasKnownMinimumWeight();
        disconnectedGraphReturnsACompleteForest();
        isolatedVerticesRemainSeparate();
        emptyGraphIsNotConnected();
        singleVertexNeedsNoEdges();
        cyclesLoopsAndParallelEdgesAreHandled();
        multipleMinimumTreesAreAccepted();
        negativeZeroAndExtremeWeightsAreSupported();
        totalWeightUsesLong();
        directedAndNullInputsAreRejected();
        inputOrderIsPreserved();
        resultsAreReadOnlySnapshots();
        countersDescribeOnlyTheLatestValidRun();
        smallGraphsMatchExhaustiveSearch();
        System.out.println("KruskalTest passed.");
    }

    private static void connectedGraphHasKnownMinimumWeight() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 6);
        graph.addEdge(0, 3, 5);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 4);

        MstResult result = new Kruskal().findMinimumSpanningForest(graph);
        check(result.totalWeight() == 19, "The known MST must cost 19.");
        check(result.isConnected(), "The known graph must have a spanning tree.");
        verifyForest(graph, result);
    }

    private static void disconnectedGraphReturnsACompleteForest() {
        WeightedGraph graph = new WeightedGraph(6, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 1);
        graph.addEdge(0, 2, 9);
        graph.addEdge(3, 4, -2);
        Kruskal algorithm = new Kruskal();

        MstResult result = algorithm.findMinimumSpanningForest(graph);
        check(result.totalWeight() == 3, "The two trees must have total cost 3.");
        check(result.componentCount() == 3, "The isolated vertex must be counted as a component.");
        check(!result.isConnected(), "A forest must not be reported as a connected MST.");
        check(algorithm.edgeExaminations() == graph.edgeCount(), "Disconnected input needs a full edge scan.");
        verifyForest(graph, result);
    }

    private static void isolatedVerticesRemainSeparate() {
        WeightedGraph graph = new WeightedGraph(4, false);
        MstResult result = new Kruskal().findMinimumSpanningForest(graph);

        check(result.componentCount() == 4, "All isolated vertices must remain separate.");
        check(result.totalWeight() == 0, "An edgeless forest must cost zero.");
        verifyForest(graph, result);
    }

    private static void emptyGraphIsNotConnected() {
        WeightedGraph graph = new WeightedGraph(0, false);
        MstResult result = new Kruskal().findMinimumSpanningForest(graph);

        check(!result.isConnected(), "The empty graph must follow the documented disconnected convention.");
        check(result.componentCount() == 0, "The empty graph must have zero components.");
        check(result.totalWeight() == 0, "The empty graph must cost zero.");
        verifyForest(graph, result);
    }

    private static void singleVertexNeedsNoEdges() {
        WeightedGraph graph = new WeightedGraph(1, false);
        graph.addEdge(0, 0, -7);
        graph.addEdge(0, 0, Integer.MAX_VALUE);
        Kruskal algorithm = new Kruskal();

        MstResult result = algorithm.findMinimumSpanningForest(graph);
        check(result.isConnected(), "A single vertex is already connected.");
        check(result.totalWeight() == 0, "Self-loops must not change the empty tree's cost.");
        check(algorithm.edgeExaminations() == 0, "An already connected vertex needs no union attempts.");
        verifyForest(graph, result);
    }

    private static void cyclesLoopsAndParallelEdgesAreHandled() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(3, 3, -50);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(0, 2, 4);
        graph.addEdge(2, 3, 1);

        MstResult result = new Kruskal().findMinimumSpanningForest(graph);
        check(result.totalWeight() == 6, "Loops and redundant edges must not reduce the tree cost.");
        verifyForest(graph, result);
    }

    private static void multipleMinimumTreesAreAccepted() {
        WeightedGraph graph = new WeightedGraph(4, false);
        for (int first = 0; first < 4; first++) {
            for (int second = first + 1; second < 4; second++) {
                graph.addEdge(first, second, 5);
            }
        }

        MstResult result = new Kruskal().findMinimumSpanningForest(graph);
        check(result.totalWeight() == 15, "Any valid minimum tree must have three edges of weight 5.");
        verifyForest(graph, result);
    }

    private static void negativeZeroAndExtremeWeightsAreSupported() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, -4);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 3, 3);
        graph.addEdge(0, 3, 10);
        graph.addEdge(0, 2, 2);
        MstResult result = new Kruskal().findMinimumSpanningForest(graph);
        check(result.totalWeight() == -1, "Zero and negative edges must be accepted.");
        verifyForest(graph, result);

        WeightedGraph extremes = new WeightedGraph(3, false);
        extremes.addEdge(0, 1, Integer.MIN_VALUE);
        extremes.addEdge(1, 2, Integer.MAX_VALUE);
        extremes.addEdge(0, 2, 0);
        MstResult extremeResult = new Kruskal().findMinimumSpanningForest(extremes);
        check(extremeResult.totalWeight() == Integer.MIN_VALUE, "Weight comparison must not overflow.");
        verifyForest(extremes, extremeResult);
    }

    private static void totalWeightUsesLong() {
        for (int weight : new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            WeightedGraph graph = new WeightedGraph(3, false);
            graph.addEdge(0, 1, weight);
            graph.addEdge(1, 2, weight);

            MstResult result = new Kruskal().findMinimumSpanningForest(graph);
            check(result.totalWeight() == 2L * weight, "A total outside the integer range must be preserved.");
            verifyForest(graph, result);
        }
    }

    private static void directedAndNullInputsAreRejected() {
        Kruskal algorithm = new Kruskal();
        WeightedGraph directed = new WeightedGraph(2, true);
        directed.addEdge(0, 1, 1);
        expectIllegalArgument(() -> algorithm.findMinimumSpanningForest(directed));
        try {
            algorithm.findMinimumSpanningForest(null);
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Null input must be rejected.");
    }

    private static void inputOrderIsPreserved() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(1, 2, 9);
        graph.addEdge(2, 0, -1);
        graph.addEdge(0, 1, 3);
        List<Edge> originalEdges = List.copyOf(graph.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }

        new Kruskal().findMinimumSpanningForest(graph);
        check(graph.edges().equals(originalEdges), "Sorting must not change the input edge order.");
        check(graph.edgeCount() == originalEdges.size(), "The input edge count must be preserved.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(originalAdjacency.get(vertex)), "Adjacency must be preserved.");
        }
    }

    private static void resultsAreReadOnlySnapshots() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 5);
        Kruskal algorithm = new Kruskal();
        MstResult result = algorithm.findMinimumSpanningForest(graph);
        List<Edge> originalSelection = List.copyOf(result.edges());
        expectUnsupportedOperation(() -> result.edges().clear());
        expectUnsupportedOperation(() -> result.edges().add(new Edge(0, 2, 1)));

        List<Edge> mutableEdges = new ArrayList<>(result.edges());
        MstResult copiedResult = new MstResult(mutableEdges, result.totalWeight(), result.componentCount());
        mutableEdges.clear();
        check(copiedResult.edges().equals(originalSelection), "The result must defensively copy its edge list.");
        graph.addEdge(0, 2, -100);
        check(algorithm.findMinimumSpanningForest(graph).totalWeight() == -96, "The updated graph has a cheaper tree.");
        check(result.edges().equals(originalSelection) && result.totalWeight() == 9,
                "Later graph changes and algorithm runs must not modify an earlier result.");
        expectIllegalArgument(() -> new MstResult(List.of(), 0, -1));
    }

    private static void countersDescribeOnlyTheLatestValidRun() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 0, -1);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);
        graph.addEdge(2, 3, 4);
        graph.addEdge(0, 3, 50);
        Kruskal algorithm = new Kruskal();
        MstResult result = algorithm.findMinimumSpanningForest(graph);

        check(result.totalWeight() == 7, "The counter example must return the known tree.");
        check(algorithm.sortingComparisons() > 0, "Sorting comparisons must be recorded.");
        check(algorithm.edgeExaminations() == 5, "Processing must stop before the final expensive edge.");
        check(algorithm.unionCalls() == 5, "Each examined edge must cause one union attempt.");
        check(algorithm.findCalls() == 10, "Each union must perform two finds.");
        check(algorithm.parentTraversals() == 6, "DSU parent traversal counts must be preserved.");

        WeightedGraph smaller = new WeightedGraph(2, false);
        smaller.addEdge(0, 1, 8);
        algorithm.findMinimumSpanningForest(smaller);
        check(algorithm.edgeExaminations() == 1 && algorithm.unionCalls() == 1,
                "Counters must reset when the same instance is reused.");
        check(algorithm.findCalls() == 2 && algorithm.parentTraversals() == 0,
                "A new call must use a fresh DSU.");
        check(algorithm.sortingComparisons() == 0, "A single edge needs no sorting comparisons.");
        expectIllegalArgument(() -> algorithm.findMinimumSpanningForest(new WeightedGraph(1, true)));
        check(algorithm.edgeExaminations() == 1 && algorithm.findCalls() == 2,
                "Rejected input must preserve the previous run's counters.");

        algorithm.findMinimumSpanningForest(new WeightedGraph(0, false));
        check(algorithm.sortingComparisons() == 0 && algorithm.edgeExaminations() == 0
                && algorithm.unionCalls() == 0 && algorithm.findCalls() == 0
                && algorithm.parentTraversals() == 0, "An empty run must reset all counters.");
    }

    private static void smallGraphsMatchExhaustiveSearch() {
        for (int mask = 0; mask < 64; mask++) {
            WeightedGraph graph = new WeightedGraph(4, false);
            int pair = 0;
            for (int first = 0; first < 4; first++) {
                for (int second = first + 1; second < 4; second++) {
                    if ((mask & (1 << pair)) != 0) {
                        graph.addEdge(first, second, pair - 2);
                    }
                    pair++;
                }
            }
            checkAgainstExhaustiveSearch(graph, "topology " + mask);
        }

        Random random = new Random(41004L);
        for (int example = 0; example < 80; example++) {
            int vertices = random.nextInt(5);
            WeightedGraph graph = new WeightedGraph(vertices, false);
            int edges = vertices == 0 ? 0 : random.nextInt(9);
            for (int edge = 0; edge < edges; edge++) {
                graph.addEdge(random.nextInt(vertices), random.nextInt(vertices), random.nextInt(15) - 7);
            }
            checkAgainstExhaustiveSearch(graph, "generated graph " + example);
        }
    }

    private static void checkAgainstExhaustiveSearch(WeightedGraph graph, String example) {
        MstResult result = new Kruskal().findMinimumSpanningForest(graph);
        verifyForest(graph, result);
        check(result.totalWeight() == minimumForestWeight(graph), "Minimum cost mismatch for " + example + ".");
    }

    private static void verifyForest(WeightedGraph graph, MstResult result) {
        int[] originalGroups = graphGroups(graph);
        int[] selectedGroups = separateGroups(graph.vertexCount());
        List<Edge> availableEdges = new ArrayList<>(graph.edges());
        long weight = 0;
        for (Edge edge : result.edges()) {
            check(availableEdges.remove(edge), "Every selected edge must come from the input.");
            check(merge(selectedGroups, edge.from(), edge.to()), "Selected edges must not contain a cycle.");
            weight += edge.weight();
        }
        int components = componentCount(originalGroups);
        check(samePartition(originalGroups, selectedGroups), "The forest must span every original component.");
        check(result.edges().size() == graph.vertexCount() - components, "The forest edge count is incorrect.");
        check(result.componentCount() == components, "The result component count is incorrect.");
        check(result.isConnected() == (components == 1), "The connectivity flag is incorrect.");
        check(result.totalWeight() == weight, "The reported cost must equal the selected edge weights.");
    }

    private static long minimumForestWeight(WeightedGraph graph) {
        List<Edge> edges = graph.edges();
        check(edges.size() <= 8, "Exhaustive search is limited to small test graphs.");
        int[] originalGroups = graphGroups(graph);
        int neededEdges = graph.vertexCount() - componentCount(originalGroups);
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << edges.size()); mask++) {
            if (Integer.bitCount(mask) != neededEdges) {
                continue;
            }
            int[] groups = separateGroups(graph.vertexCount());
            long weight = 0;
            boolean acyclic = true;
            for (int index = 0; index < edges.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    Edge edge = edges.get(index);
                    if (!merge(groups, edge.from(), edge.to())) {
                        acyclic = false;
                        break;
                    }
                    weight += edge.weight();
                }
            }
            if (acyclic && samePartition(originalGroups, groups)) {
                best = Math.min(best, weight);
            }
        }
        check(best != Long.MAX_VALUE, "Exhaustive search must find a spanning forest.");
        return best;
    }

    private static int[] graphGroups(WeightedGraph graph) {
        int[] groups = separateGroups(graph.vertexCount());
        for (Edge edge : graph.edges()) {
            merge(groups, edge.from(), edge.to());
        }
        return groups;
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

    private static int componentCount(int[] groups) {
        boolean[] seen = new boolean[groups.length];
        int count = 0;
        for (int group : groups) {
            if (!seen[group]) {
                seen[group] = true;
                count++;
            }
        }
        return count;
    }

    private static boolean samePartition(int[] first, int[] second) {
        for (int from = 0; from < first.length; from++) {
            for (int to = 0; to < first.length; to++) {
                if ((first[from] == first[to]) != (second[from] == second[to])) {
                    return false;
                }
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

    private static void expectUnsupportedOperation(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected UnsupportedOperationException.");
    }
}
