package cityplanner.experiments;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import cityplanner.mst.Kruskal;
import cityplanner.mst.MstResult;
import cityplanner.mst.Prim;
import cityplanner.shortestpath.Dijkstra;
import cityplanner.shortestpath.DijkstraResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConnectedGraphGeneratorTest {
    private static final int[][] REQUIRED_SIZES = {{10, 30}, {100, 500}, {1000, 5000}};
    private static final long[] REQUIRED_SEEDS = {41100L, 41101L, 41102L};

    private ConnectedGraphGeneratorTest() {
    }

    public static void main(String[] args) {
        requiredSizesHaveValidStructure();
        requiredCasesAreReproducible();
        singleVertexNeedsNoEdges();
        twoVerticesHaveOneConnection();
        treeOnlyGraphsAreConnected();
        completeGraphsContainAllPairs();
        denseGraphsHaveExactSizes();
        weightRangeMatchesTheDocumentedPolicy();
        allLongSeedsAreAccepted();
        callsDoNotShareRandomState();
        generatedGraphsAreIndependent();
        invalidVertexCountsAreRejected();
        impossibleEdgeCountsAreRejected();
        largeVertexCapacityUsesLongArithmetic();
        requiredCasesWorkWithExistingAlgorithms();
        smallCasesCoverEveryValidEdgeCount();
        System.out.println("ConnectedGraphGeneratorTest passed.");
    }

    private static void requiredSizesHaveValidStructure() {
        for (int example = 0; example < REQUIRED_SIZES.length; example++) {
            verifyStructure(requiredGraph(example), REQUIRED_SIZES[example][0], REQUIRED_SIZES[example][1]);
        }
    }

    private static void requiredCasesAreReproducible() {
        for (int example = 0; example < REQUIRED_SIZES.length; example++) {
            verifySameGraph(requiredGraph(example), requiredGraph(example));
        }
    }

    private static void singleVertexNeedsNoEdges() {
        WeightedGraph graph = ConnectedGraphGenerator.generate(1, 0, 7);
        verifyStructure(graph, 1, 0);
        check(graph.edgesFrom(0).isEmpty(), "A single vertex must not acquire a self-loop.");
    }

    private static void twoVerticesHaveOneConnection() {
        WeightedGraph graph = ConnectedGraphGenerator.generate(2, 1, 9);
        verifyStructure(graph, 2, 1);
        check(graph.edgesFrom(0).size() == 1 && graph.edgesFrom(1).size() == 1,
                "The only connection must be usable in both directions.");
    }

    private static void treeOnlyGraphsAreConnected() {
        for (int count : new int[] {3, 10, 1000}) {
            verifyStructure(ConnectedGraphGenerator.generate(count, count - 1, 41010), count, count - 1);
        }
    }

    private static void completeGraphsContainAllPairs() {
        for (int count : new int[] {3, 10, 25}) {
            int edgeCount = count * (count - 1) / 2;
            WeightedGraph graph = ConnectedGraphGenerator.generate(count, edgeCount, 29);
            verifyStructure(graph, count, edgeCount);
            for (int vertex = 0; vertex < count; vertex++) {
                check(graph.edgesFrom(vertex).size() == count - 1,
                        "A complete simple graph must connect each vertex to every other vertex.");
            }
        }
    }

    private static void denseGraphsHaveExactSizes() {
        for (int missing : new int[] {1, 5, 20}) {
            int count = 30;
            int edgeCount = count * (count - 1) / 2 - missing;
            verifyStructure(ConnectedGraphGenerator.generate(count, edgeCount, 31 + missing), count, edgeCount);
        }
    }

    private static void weightRangeMatchesTheDocumentedPolicy() {
        check(ConnectedGraphGenerator.MIN_WEIGHT == 0 && ConnectedGraphGenerator.MAX_WEIGHT == 100,
                "The documented experiment weight range must be zero through one hundred.");
        WeightedGraph graph = requiredGraph(2);
        Set<Integer> weights = new HashSet<>();
        for (Edge edge : graph.edges()) {
            check(edge.weight() >= 0 && edge.weight() <= 100, "All generated weights must be Dijkstra-compatible.");
            weights.add(edge.weight());
        }
        check(weights.size() < graph.edgeCount(), "Repeated weights must be allowed without dropping distinct edges.");
    }

    private static void allLongSeedsAreAccepted() {
        for (long seed : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 123456789101112L}) {
            WeightedGraph first = ConnectedGraphGenerator.generate(12, 30, seed);
            verifyStructure(first, 12, 30);
            verifySameGraph(first, ConnectedGraphGenerator.generate(12, 30, seed));
        }
    }

    private static void callsDoNotShareRandomState() {
        WeightedGraph first = requiredGraph(1);
        ConnectedGraphGenerator.generate(10, 40, -17);
        ConnectedGraphGenerator.generate(1, 0, Long.MIN_VALUE);
        ConnectedGraphGenerator.generate(1000, 999, 53);

        verifySameGraph(first, requiredGraph(1));
    }

    private static void generatedGraphsAreIndependent() {
        WeightedGraph first = requiredGraph(0);
        WeightedGraph second = requiredGraph(0);
        List<Edge> originalEdges = List.copyOf(second.edges());
        List<List<Edge>> originalAdjacency = new ArrayList<>();
        for (int vertex = 0; vertex < second.vertexCount(); vertex++) {
            originalAdjacency.add(List.copyOf(second.edgesFrom(vertex)));
        }

        check(first != second, "Each invocation must return a separate graph instance.");
        first.addEdge(0, 0, -1);
        check(first.edgeCount() == 31, "The caller must be able to use the graph's normal mutation API.");
        check(second.edges().equals(originalEdges), "Mutating one graph must not change another graph's edge list.");
        for (int vertex = 0; vertex < second.vertexCount(); vertex++) {
            check(second.edgesFrom(vertex).equals(originalAdjacency.get(vertex)),
                    "Mutating one graph must not change another graph's adjacency.");
        }
        verifySameGraph(second, requiredGraph(0));
    }

    private static void invalidVertexCountsAreRejected() {
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(0, 0, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(-1, 0, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(Integer.MIN_VALUE, 0, 1));
    }

    private static void impossibleEdgeCountsAreRejected() {
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(1, -1, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(2, 0, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(10, 8, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(1, 1, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(2, 2, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(10, 46, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(3, Integer.MAX_VALUE, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(Integer.MAX_VALUE, 0, 1));
        expectIllegalArgument(() -> ConnectedGraphGenerator.generate(Integer.MAX_VALUE, Integer.MAX_VALUE - 2, 1));
    }

    private static void largeVertexCapacityUsesLongArithmetic() {
        int count = 50000;
        verifyStructure(ConnectedGraphGenerator.generate(count, count - 1, 71), count, count - 1);
    }

    private static void requiredCasesWorkWithExistingAlgorithms() {
        for (int example = 0; example < REQUIRED_SIZES.length; example++) {
            WeightedGraph graph = requiredGraph(example);
            List<Edge> originalEdges = List.copyOf(graph.edges());
            MstResult kruskal = new Kruskal().findMinimumSpanningForest(graph);
            MstResult prim = new Prim().findMinimumSpanningForest(graph);
            check(kruskal.isConnected() && prim.isConnected(), "Both MST algorithms must accept every generated case.");
            check(kruskal.edges().size() == graph.vertexCount() - 1
                    && prim.edges().size() == graph.vertexCount() - 1, "Both results must span every generated vertex.");
            check(kruskal.totalWeight() == prim.totalWeight(), "Both MST algorithms must agree on the generated input.");

            DijkstraResult routes = new Dijkstra().findShortestPaths(graph, 0);
            check(routes.source() == 0 && routes.distanceTo(0) == 0, "The documented shortest-path source must be valid.");
            for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                check(routes.isReachable(vertex), "Every generated vertex must be reachable from source zero.");
            }
            for (int target : new int[] {0, graph.vertexCount() / 2, graph.vertexCount() - 1}) {
                verifyPath(graph, routes, target);
            }
            check(graph.edges().equals(originalEdges), "Algorithms must leave the generated input available for reuse.");
        }
    }

    private static void smallCasesCoverEveryValidEdgeCount() {
        for (int count = 2; count <= 12; count++) {
            int maximum = count * (count - 1) / 2;
            for (int edges = count - 1; edges <= maximum; edges++) {
                for (long seed : new long[] {0, -19, Long.MAX_VALUE}) {
                    verifyStructure(ConnectedGraphGenerator.generate(count, edges, seed), count, edges);
                }
            }
        }
    }

    private static WeightedGraph requiredGraph(int example) {
        return ConnectedGraphGenerator.generate(REQUIRED_SIZES[example][0], REQUIRED_SIZES[example][1],
                REQUIRED_SEEDS[example]);
    }

    private static void verifySameGraph(WeightedGraph first, WeightedGraph second) {
        check(first.vertexCount() == second.vertexCount() && first.edgeCount() == second.edgeCount()
                && first.isDirected() == second.isDirected(), "Regeneration must preserve graph metadata.");
        check(first.edges().equals(second.edges()), "The same seed must reproduce weights, orientation, and edge order.");
        for (int vertex = 0; vertex < first.vertexCount(); vertex++) {
            check(first.edgesFrom(vertex).equals(second.edgesFrom(vertex)), "Regeneration must preserve adjacency order.");
        }
    }

    private static void verifyStructure(WeightedGraph graph, int vertexCount, int edgeCount) {
        check(graph.vertexCount() == vertexCount && graph.edgeCount() == edgeCount,
                "The generated graph must have exactly the requested size.");
        check(!graph.isDirected(), "The generator must return an undirected graph.");
        Set<Long> pairs = new HashSet<>();
        Set<Edge> expectedAdjacency = new HashSet<>();
        for (Edge edge : graph.edges()) {
            check(edge.from() >= 0 && edge.from() < vertexCount && edge.to() >= 0 && edge.to() < vertexCount,
                    "Every endpoint must belong to the generated graph.");
            check(edge.from() != edge.to(), "Self-loops must not be generated.");
            int low = Math.min(edge.from(), edge.to());
            int high = Math.max(edge.from(), edge.to());
            check(pairs.add((long) low * vertexCount + high), "An undirected pair must not be generated twice.");
            check(edge.weight() >= ConnectedGraphGenerator.MIN_WEIGHT
                    && edge.weight() <= ConnectedGraphGenerator.MAX_WEIGHT, "Generated weights must stay within bounds.");
            expectedAdjacency.add(edge);
            expectedAdjacency.add(new Edge(edge.to(), edge.from(), edge.weight()));
        }
        long adjacencyCount = 0;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            for (Edge edge : graph.edgesFrom(vertex)) {
                check(edge.from() == vertex, "Adjacency must use the vertex being enumerated as its source.");
                check(expectedAdjacency.remove(edge), "Adjacency must contain each weighted orientation exactly once.");
                adjacencyCount++;
            }
        }
        check(adjacencyCount == 2L * edgeCount && expectedAdjacency.isEmpty(),
                "Each logical undirected edge must produce exactly two matching adjacency entries.");
        verifyConnected(graph);
    }

    private static void verifyConnected(WeightedGraph graph) {
        boolean[] reached = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        reached[0] = true;
        pending.addLast(0);
        int count = 0;
        while (!pending.isEmpty()) {
            int vertex = pending.removeFirst();
            count++;
            for (Edge edge : graph.edgesFrom(vertex)) {
                if (!reached[edge.to()]) {
                    reached[edge.to()] = true;
                    pending.addLast(edge.to());
                }
            }
        }
        check(count == graph.vertexCount(), "Independent BFS must reach every generated vertex.");
    }

    private static void verifyPath(WeightedGraph graph, DijkstraResult routes, int target) {
        List<Integer> path = routes.pathTo(target);
        check(!path.isEmpty() && path.get(0) == 0 && path.get(path.size() - 1) == target,
                "A generated route must connect the documented source to its target.");
        Set<Integer> visited = new HashSet<>();
        long cost = 0;
        for (int index = 0; index < path.size(); index++) {
            int vertex = path.get(index);
            check(visited.add(vertex), "A generated route must not contain a predecessor cycle.");
            if (index == 0) {
                continue;
            }
            int previous = path.get(index - 1);
            Edge step = graph.edgesFrom(previous).stream().filter(edge -> edge.to() == vertex).findFirst()
                    .orElseThrow(() -> new AssertionError("A route must follow an actual generated edge."));
            cost += step.weight();
        }
        check(cost == routes.distanceTo(target), "The route cost must equal its reported shortest distance.");
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
}
