package cityplanner.experiments;

import cityplanner.connectivity.Kosaraju;
import cityplanner.connectivity.SccResult;
import cityplanner.connectivity.TopologicalSort;
import cityplanner.connectivity.TopologicalSortResult;
import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import cityplanner.shortestpath.Dijkstra;
import cityplanner.shortestpath.DijkstraResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DirectedGraphGeneratorTest {
    private static final int[][] REQUIRED_SIZES = {{10, 30}, {100, 500}, {1000, 5000}};
    private static final long[] DAG_SEEDS = {41200L, 41201L, 41202L};
    private static final long[] CYCLIC_SEEDS = {41300L, 41301L, 41302L};

    private DirectedGraphGeneratorTest() {
    }

    public static void main(String[] args) {
        requiredDagSizesHaveValidStructure();
        requiredCyclicSizesHaveValidStructure();
        requiredInputsAreReproducible();
        emptyAndSingleVertexDagsHaveNoEdges();
        minimalCyclesHaveKnownComponents();
        triangleLeavesOtherVerticesIsolated();
        completeDagsHaveAValidTotalOrder();
        completeCyclicGraphsContainAllOrderedPairs();
        denseInputsHaveExactSizes();
        zeroEdgeDagsKeepOtherVerticesUnreachable();
        weightsMatchTheDocumentedPolicy();
        allLongSeedsAreAccepted();
        callsDoNotShareRandomState();
        generatedGraphsAreIndependent();
        invalidDagSizesAreRejected();
        invalidCyclicSizesAreRejected();
        largeCapacitiesUseLongArithmetic();
        requiredCasesWorkWithExistingAlgorithms();
        smallDagCasesMatchIndependentOracles();
        smallCyclicCasesMatchIndependentOracles();
        System.out.println("DirectedGraphGeneratorTest passed.");
    }

    private static void requiredDagSizesHaveValidStructure() {
        for (int example = 0; example < REQUIRED_SIZES.length; example++) {
            WeightedGraph graph = requiredGraph(false, example);
            verifyStructure(graph, REQUIRED_SIZES[example][0], REQUIRED_SIZES[example][1]);
            verifyOrder(graph, new TopologicalSort().sort(graph));
        }
    }

    private static void requiredCyclicSizesHaveValidStructure() {
        for (int example = 0; example < REQUIRED_SIZES.length; example++) {
            WeightedGraph graph = requiredGraph(true, example);
            verifyStructure(graph, REQUIRED_SIZES[example][0], REQUIRED_SIZES[example][1]);
            verifyCycle(new TopologicalSort().sort(graph));
        }
    }

    private static void requiredInputsAreReproducible() {
        for (boolean cyclic : new boolean[] {false, true}) {
            for (int example = 0; example < REQUIRED_SIZES.length; example++) {
                verifySameGraph(requiredGraph(cyclic, example), requiredGraph(cyclic, example));
            }
        }
    }

    private static void emptyAndSingleVertexDagsHaveNoEdges() {
        for (int count : new int[] {0, 1}) {
            WeightedGraph graph = DirectedGraphGenerator.generateDag(count, 0, 3);
            verifyStructure(graph, count, 0);
            verifyOrder(graph, new TopologicalSort().sort(graph));
            check(new Kosaraju().findStronglyConnectedComponents(graph).componentCount() == count,
                    "An edgeless DAG must have one component per vertex.");
        }
    }

    private static void minimalCyclesHaveKnownComponents() {
        for (int count : new int[] {2, 3}) {
            WeightedGraph graph = DirectedGraphGenerator.generateCyclic(count, count, 7);
            verifyStructure(graph, count, count);
            verifyCycle(new TopologicalSort().sort(graph));
            SccResult result = new Kosaraju().findStronglyConnectedComponents(graph);
            int[] labels = componentLabels(graph.vertexCount(), result);
            check(result.componentCount() == 1, "The minimal full cycle must contain every vertex in one SCC.");
            for (int vertex = 1; vertex < count; vertex++) {
                check(labels[vertex] == labels[0], "Every vertex of the known cycle must be mutually reachable.");
            }
        }
    }

    private static void triangleLeavesOtherVerticesIsolated() {
        WeightedGraph graph = DirectedGraphGenerator.generateCyclic(5, 3, 19);
        verifyStructure(graph, 5, 3);
        Set<Integer> triangle = new HashSet<>();
        for (Edge edge : graph.edges()) {
            triangle.add(edge.from());
            triangle.add(edge.to());
        }
        check(triangle.size() == 3, "The three reserved edges must form a triangle on three vertices.");
        SccResult result = new Kosaraju().findStronglyConnectedComponents(graph);
        componentLabels(5, result);
        Set<Set<Integer>> expected = new HashSet<>();
        expected.add(triangle);
        for (int vertex = 0; vertex < 5; vertex++) {
            if (!triangle.contains(vertex)) {
                expected.add(Set.of(vertex));
                check(graph.edgesFrom(vertex).isEmpty(), "Vertices outside the reserved triangle must stay isolated.");
            }
        }
        Set<Set<Integer>> actual = new HashSet<>();
        for (List<Integer> component : result.components()) {
            actual.add(new HashSet<>(component));
        }
        check(actual.equals(expected), "The known triangle and isolated vertices must have separate SCC memberships.");
    }

    private static void completeDagsHaveAValidTotalOrder() {
        for (int count : new int[] {2, 5, 15}) {
            int edges = count * (count - 1) / 2;
            WeightedGraph graph = DirectedGraphGenerator.generateDag(count, edges, 23);
            verifyStructure(graph, count, edges);
            verifyOrder(graph, new TopologicalSort().sort(graph));
        }
    }

    private static void completeCyclicGraphsContainAllOrderedPairs() {
        for (int count : new int[] {2, 5, 15}) {
            int edges = count * (count - 1);
            WeightedGraph graph = DirectedGraphGenerator.generateCyclic(count, edges, 29);
            verifyStructure(graph, count, edges);
            for (int vertex = 0; vertex < count; vertex++) {
                check(graph.edgesFrom(vertex).size() == count - 1,
                        "A complete directed graph must contain every distinct target, including reciprocal connections.");
            }
            check(new Kosaraju().findStronglyConnectedComponents(graph).componentCount() == 1,
                    "A complete directed graph must be strongly connected.");
        }
    }

    private static void denseInputsHaveExactSizes() {
        int count = 20;
        for (int missing : new int[] {1, 5, 20}) {
            int dagEdges = count * (count - 1) / 2 - missing;
            int cyclicEdges = count * (count - 1) - missing;
            verifyStructure(DirectedGraphGenerator.generateDag(count, dagEdges, missing), count, dagEdges);
            verifyStructure(DirectedGraphGenerator.generateCyclic(count, cyclicEdges, missing), count, cyclicEdges);
        }
    }

    private static void zeroEdgeDagsKeepOtherVerticesUnreachable() {
        WeightedGraph graph = DirectedGraphGenerator.generateDag(5, 0, 31);
        DijkstraResult result = new Dijkstra().findShortestPaths(graph, 0);
        check(result.distanceTo(0) == 0 && result.pathTo(0).equals(List.of(0)), "The source must remain reachable.");
        for (int target = 1; target < graph.vertexCount(); target++) {
            check(!result.isReachable(target) && result.distanceTo(target) == DijkstraResult.UNREACHABLE
                    && result.pathTo(target).isEmpty(), "An edgeless directed scenario must preserve unreachable targets.");
        }
    }

    private static void weightsMatchTheDocumentedPolicy() {
        check(DirectedGraphGenerator.MIN_WEIGHT == 0 && DirectedGraphGenerator.MAX_WEIGHT == 100,
                "The directed experiment weight range must be zero through one hundred.");
        for (boolean cyclic : new boolean[] {false, true}) {
            WeightedGraph graph = requiredGraph(cyclic, 2);
            Set<Integer> weights = new HashSet<>();
            for (Edge edge : graph.edges()) {
                check(edge.weight() >= 0 && edge.weight() <= 100, "Weights must be valid for Dijkstra.");
                weights.add(edge.weight());
            }
            check(weights.size() < graph.edgeCount(), "Repeated weights must not remove distinct directed edges.");
        }
    }

    private static void allLongSeedsAreAccepted() {
        for (long seed : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 123456789101112L}) {
            for (boolean cyclic : new boolean[] {false, true}) {
                WeightedGraph first = generate(cyclic, 8, 15, seed);
                verifyStructure(first, 8, 15);
                verifySameGraph(first, generate(cyclic, 8, 15, seed));
            }
        }
    }

    private static void callsDoNotShareRandomState() {
        WeightedGraph dag = requiredGraph(false, 0);
        WeightedGraph cyclic = requiredGraph(true, 0);
        DirectedGraphGenerator.generateDag(5, 0, -17);
        DirectedGraphGenerator.generateCyclic(5, 20, Long.MIN_VALUE);
        DirectedGraphGenerator.generateDag(1, 0, 41);
        verifySameGraph(dag, requiredGraph(false, 0));
        verifySameGraph(cyclic, requiredGraph(true, 0));
    }

    private static void generatedGraphsAreIndependent() {
        for (boolean cyclic : new boolean[] {false, true}) {
            WeightedGraph first = requiredGraph(cyclic, 0);
            WeightedGraph second = requiredGraph(cyclic, 0);
            List<Edge> originalEdges = List.copyOf(second.edges());
            List<List<Edge>> originalAdjacency = new ArrayList<>();
            for (int vertex = 0; vertex < second.vertexCount(); vertex++) {
                originalAdjacency.add(List.copyOf(second.edgesFrom(vertex)));
            }
            check(first != second, "Every call must return an independent mutable graph.");
            first.addEdge(0, 0, -1);
            check(second.edges().equals(originalEdges), "Mutating one result must not change another edge list.");
            for (int vertex = 0; vertex < second.vertexCount(); vertex++) {
                check(second.edgesFrom(vertex).equals(originalAdjacency.get(vertex)),
                        "Mutating one result must not change another adjacency list.");
            }
            verifySameGraph(second, requiredGraph(cyclic, 0));
        }
    }

    private static void invalidDagSizesAreRejected() {
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(-1, 0, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(Integer.MIN_VALUE, 0, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(0, -1, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(0, 1, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(1, 1, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(2, 2, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(3, 4, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(3, Integer.MAX_VALUE, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateDag(Integer.MAX_VALUE, -1, 1));
    }

    private static void invalidCyclicSizesAreRejected() {
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(-1, 2, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(Integer.MIN_VALUE, 2, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(0, 0, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(1, 1, 1));
        for (int edges : new int[] {-1, 0, 1, 3}) {
            expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(2, edges, 1));
        }
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(3, 7, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(3, Integer.MAX_VALUE, 1));
        expectIllegalArgument(() -> DirectedGraphGenerator.generateCyclic(Integer.MAX_VALUE, 1, 1));
    }

    private static void largeCapacitiesUseLongArithmetic() {
        int count = 50000;
        verifyStructure(DirectedGraphGenerator.generateDag(count, 0, 43), count, 0);
        verifyStructure(DirectedGraphGenerator.generateCyclic(count, 2, 47), count, 2);
    }

    private static void requiredCasesWorkWithExistingAlgorithms() {
        for (boolean cyclic : new boolean[] {false, true}) {
            for (int example = 0; example < REQUIRED_SIZES.length; example++) {
                WeightedGraph graph = requiredGraph(cyclic, example);
                List<Edge> originalEdges = List.copyOf(graph.edges());
                TopologicalSortResult order = new TopologicalSort().sort(graph);
                if (cyclic) {
                    verifyCycle(order);
                } else {
                    verifyOrder(graph, order);
                }
                SccResult components = new Kosaraju().findStronglyConnectedComponents(graph);
                componentLabels(graph.vertexCount(), components);
                if (cyclic) {
                    check(components.components().stream().anyMatch(group -> group.size() >= 2),
                            "A cyclic scenario must have a nontrivial strongly connected component.");
                } else {
                    check(components.componentCount() == graph.vertexCount(), "A DAG must have only singleton SCCs.");
                }
                DijkstraResult routes = new Dijkstra().findShortestPaths(graph, 0);
                boolean[] reached = reachableFrom(graph, 0);
                check(routes.source() == 0 && routes.distanceTo(0) == 0, "The documented route source must be zero.");
                for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                    check(routes.isReachable(vertex) == reached[vertex], "Directed reachability must match independent BFS.");
                }
                for (int target : new int[] {0, graph.vertexCount() / 2, graph.vertexCount() - 1}) {
                    verifyPath(graph, routes, target);
                }
                check(graph.edges().equals(originalEdges), "Algorithms must preserve input edges for subsequent runs.");
            }
        }
    }

    private static void smallDagCasesMatchIndependentOracles() {
        for (int count = 0; count <= 7; count++) {
            for (int edges = 0; edges <= count * (count - 1) / 2; edges++) {
                for (long seed : new long[] {0, -19, Long.MAX_VALUE}) {
                    verifySmallCase(DirectedGraphGenerator.generateDag(count, edges, seed), count, edges, false);
                }
            }
        }
    }

    private static void smallCyclicCasesMatchIndependentOracles() {
        for (int count = 2; count <= 6; count++) {
            for (int edges = 2; edges <= count * (count - 1); edges++) {
                for (long seed : new long[] {0, -19, Long.MAX_VALUE}) {
                    verifySmallCase(DirectedGraphGenerator.generateCyclic(count, edges, seed), count, edges, true);
                }
            }
        }
    }

    private static void verifySmallCase(WeightedGraph graph, int vertexCount, int edgeCount, boolean cyclic) {
        verifyStructure(graph, vertexCount, edgeCount);
        boolean[][] reachable = reachability(graph);
        boolean hasCycle = false;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            hasCycle |= reachable[vertex][vertex];
        }
        check(hasCycle == cyclic, "Independent transitive closure must confirm the scenario's cycle policy.");
        TopologicalSortResult order = new TopologicalSort().sort(graph);
        if (cyclic) {
            verifyCycle(order);
        } else {
            verifyOrder(graph, order);
        }
        int[] groups = componentLabels(vertexCount, new Kosaraju().findStronglyConnectedComponents(graph));
        for (int first = 0; first < vertexCount; first++) {
            for (int second = 0; second < vertexCount; second++) {
                boolean mutual = first == second || (reachable[first][second] && reachable[second][first]);
                check((groups[first] == groups[second]) == mutual, "SCC membership must match mutual reachability.");
            }
        }
        long[][] distances = shortestDistances(graph);
        Dijkstra algorithm = new Dijkstra();
        for (int source = 0; source < vertexCount; source++) {
            DijkstraResult result = algorithm.findShortestPaths(graph, source);
            for (int target = 0; target < vertexCount; target++) {
                check(result.distanceTo(target) == distances[source][target],
                        "Generated shortest distances must match the independent Floyd-Warshall oracle.");
                verifyPath(graph, result, target);
            }
        }
    }

    private static WeightedGraph requiredGraph(boolean cyclic, int example) {
        return generate(cyclic, REQUIRED_SIZES[example][0], REQUIRED_SIZES[example][1],
                cyclic ? CYCLIC_SEEDS[example] : DAG_SEEDS[example]);
    }

    private static WeightedGraph generate(boolean cyclic, int vertexCount, int edgeCount, long seed) {
        return cyclic ? DirectedGraphGenerator.generateCyclic(vertexCount, edgeCount, seed)
                : DirectedGraphGenerator.generateDag(vertexCount, edgeCount, seed);
    }

    private static void verifyStructure(WeightedGraph graph, int vertexCount, int edgeCount) {
        check(graph.vertexCount() == vertexCount && graph.edgeCount() == edgeCount,
                "The generated directed graph must have exactly the requested size.");
        check(graph.isDirected(), "Both generators must produce directed graphs.");
        Set<Long> pairs = new HashSet<>();
        Set<Edge> expectedAdjacency = new HashSet<>();
        for (Edge edge : graph.edges()) {
            check(edge.from() >= 0 && edge.from() < vertexCount && edge.to() >= 0 && edge.to() < vertexCount,
                    "Every generated endpoint must belong to the graph.");
            check(edge.from() != edge.to(), "Generated directed scenarios must not contain self-loops.");
            check(pairs.add((long) edge.from() * vertexCount + edge.to()), "Ordered pairs must not be duplicated.");
            check(edge.weight() >= DirectedGraphGenerator.MIN_WEIGHT && edge.weight() <= DirectedGraphGenerator.MAX_WEIGHT,
                    "Every weight must lie in the declared inclusive range.");
            expectedAdjacency.add(edge);
        }
        long count = 0;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            for (Edge edge : graph.edgesFrom(vertex)) {
                check(edge.from() == vertex, "Adjacency must start at the enumerated vertex.");
                check(expectedAdjacency.remove(edge), "Each logical directed edge must appear in adjacency exactly once.");
                count++;
            }
        }
        check(count == edgeCount && expectedAdjacency.isEmpty(), "Directed adjacency must contain exactly E entries.");
    }

    private static void verifySameGraph(WeightedGraph first, WeightedGraph second) {
        check(first.vertexCount() == second.vertexCount() && first.edgeCount() == second.edgeCount()
                && first.isDirected() == second.isDirected(), "Regeneration must preserve metadata.");
        check(first.edges().equals(second.edges()), "A seed must reproduce weights, orientations, and logical edge order.");
        for (int vertex = 0; vertex < first.vertexCount(); vertex++) {
            check(first.edgesFrom(vertex).equals(second.edgesFrom(vertex)), "A seed must reproduce adjacency order.");
        }
    }

    private static void verifyOrder(WeightedGraph graph, TopologicalSortResult result) {
        check(!result.hasCycle() && result.order().size() == graph.vertexCount(), "A DAG must have a complete order.");
        int[] positions = new int[graph.vertexCount()];
        Arrays.fill(positions, -1);
        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);
            check(vertex >= 0 && vertex < graph.vertexCount() && positions[vertex] == -1,
                    "The order must contain each valid vertex once.");
            positions[vertex] = index;
        }
        for (Edge edge : graph.edges()) {
            check(positions[edge.from()] < positions[edge.to()], "Every DAG edge must point forward in the reported order.");
        }
    }

    private static void verifyCycle(TopologicalSortResult result) {
        check(result.hasCycle() && result.order().isEmpty(), "A cyclic graph must not expose a partial topological order.");
    }

    private static int[] componentLabels(int vertexCount, SccResult result) {
        check(result.componentCount() == result.components().size(), "The SCC count must match the returned groups.");
        int[] labels = new int[vertexCount];
        Arrays.fill(labels, -1);
        for (int group = 0; group < result.components().size(); group++) {
            List<Integer> members = result.components().get(group);
            check(!members.isEmpty(), "SCC groups must not be empty.");
            for (int vertex : members) {
                check(vertex >= 0 && vertex < vertexCount && labels[vertex] == -1,
                        "Every valid vertex must appear in exactly one SCC.");
                labels[vertex] = group;
            }
        }
        for (int label : labels) {
            check(label != -1, "All input vertices must belong to an SCC.");
        }
        return labels;
    }

    private static boolean[][] reachability(WeightedGraph graph) {
        int count = graph.vertexCount();
        boolean[][] reachable = new boolean[count][count];
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

    private static long[][] shortestDistances(WeightedGraph graph) {
        int count = graph.vertexCount();
        long[][] distances = new long[count][count];
        for (int vertex = 0; vertex < count; vertex++) {
            Arrays.fill(distances[vertex], DijkstraResult.UNREACHABLE);
            distances[vertex][vertex] = 0;
        }
        for (Edge edge : graph.edges()) {
            distances[edge.from()][edge.to()] = edge.weight();
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

    private static boolean[] reachableFrom(WeightedGraph graph, int source) {
        boolean[] reached = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        reached[source] = true;
        pending.addLast(source);
        while (!pending.isEmpty()) {
            for (Edge edge : graph.edgesFrom(pending.removeFirst())) {
                if (!reached[edge.to()]) {
                    reached[edge.to()] = true;
                    pending.addLast(edge.to());
                }
            }
        }
        return reached;
    }

    private static void verifyPath(WeightedGraph graph, DijkstraResult result, int target) {
        List<Integer> path = result.pathTo(target);
        if (!result.isReachable(target)) {
            check(path.isEmpty(), "An unreachable generated target must have no route.");
            return;
        }
        check(!path.isEmpty() && path.get(0) == result.source() && path.get(path.size() - 1) == target,
                "A route must join the recorded source to its target.");
        Set<Integer> seen = new HashSet<>();
        long cost = 0;
        for (int index = 0; index < path.size(); index++) {
            int vertex = path.get(index);
            check(vertex >= 0 && vertex < graph.vertexCount() && seen.add(vertex), "A route must use valid unique vertices.");
            if (index > 0) {
                int previous = path.get(index - 1);
                Edge step = graph.edgesFrom(previous).stream().filter(edge -> edge.to() == vertex).findFirst()
                        .orElseThrow(() -> new AssertionError("A reconstructed route must use actual directed edges."));
                cost += step.weight();
            }
        }
        check(cost == result.distanceTo(target), "The actual route cost must match the reported distance.");
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
