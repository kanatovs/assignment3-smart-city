package cityplanner.experiments;

import cityplanner.experiments.BenchmarkRunner.Algorithm;
import cityplanner.experiments.BenchmarkRunner.GraphCase;
import cityplanner.experiments.BenchmarkRunner.Run;
import cityplanner.experiments.BenchmarkRunner.Summary;
import cityplanner.experiments.BenchmarkRunner.Task;
import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class GraphExperimentsTest {
    private static final List<GraphCase> INPUTS = List.of(
            new GraphCase("connected-undirected", 10, 30, 41100L, false),
            new GraphCase("connected-undirected", 100, 500, 41101L, false),
            new GraphCase("connected-undirected", 1000, 5000, 41102L, false),
            new GraphCase("dag-directed", 10, 30, 41200L, true),
            new GraphCase("dag-directed", 100, 500, 41201L, true),
            new GraphCase("dag-directed", 1000, 5000, 41202L, true),
            new GraphCase("cyclic-directed", 10, 30, 41300L, true),
            new GraphCase("cyclic-directed", 100, 500, 41301L, true),
            new GraphCase("cyclic-directed", 1000, 5000, 41302L, true));
    private static List<Run> measurements;

    private GraphExperimentsTest() {
    }

    public static void main(String[] args) throws IOException {
        measurements = GraphExperiments.run();
        everyInputHasExactlyFiveRunsForEachApplicableAlgorithm();
        topologicalResultsDistinguishDagsAndCycles();
        componentOutcomesMatchIndependentReachability();
        distancesMatchIndependentBellmanFord();
        countersRetainAllAlgorithmSpecificPhases();
        summariesAverageOnlyTheirFiveActualIntervals();
        factoriesRunHandCalculatedDirectedGraphs();
        unreachableAndZeroWeightCycleResultsRemainExplicit();
        factoriesCreateIndependentTasksAndAlgorithmState();
        allAdaptersPreserveTheirSharedInput();
        singleVertexFactoriesRemainUsable();
        invalidFactoryInputsAreRejected();
        missingCasesAndAlgorithmsAreRejected();
        unknownCasesAndAlgorithmsAreRejected();
        incompleteOrRepeatedRunNumbersAreRejected();
        incorrectCycleAndOrderingResultsAreRejected();
        incorrectComponentResultsAreRejected();
        incorrectShortestPathResultsAreRejected();
        incorrectMstResultsAreRejected();
        completeCsvExportPreservesCoverageAndMetrics();
        mainWritesTheCompleteSuiteToACustomDirectory();
        environmentDocumentsReproducibilityAndSource();
        System.out.println("GraphExperimentsTest passed.");
    }

    private static void everyInputHasExactlyFiveRunsForEachApplicableAlgorithm() {
        check(measurements.size() == 135, "Nine inputs and three algorithms require 135 measured rows.");
        check(BenchmarkRunner.summarize(measurements).size() == 27, "The suite must have 27 summary groups.");
        for (Run run : measurements) {
            check(INPUTS.contains(run.input()), "Raw rows must retain the exact scenario, size, seed, and direction.");
        }
        for (GraphCase input : INPUTS) {
            Set<String> expected = input.directed() ? Set.of("TopologicalSort", "Kosaraju", "Dijkstra")
                    : Set.of("Kruskal", "Prim", "Dijkstra");
            Set<String> actual = new HashSet<>();
            for (Run run : measurements) {
                if (run.input().equals(input)) {
                    actual.add(run.algorithm());
                }
            }
            check(actual.equals(expected), "Only applicable algorithms may run on an input.");
            for (String algorithm : expected) {
                List<Run> group = group(input, algorithm);
                Set<Integer> numbers = new HashSet<>();
                group.forEach(run -> numbers.add(run.run()));
                check(group.size() == 5 && numbers.equals(Set.of(1, 2, 3, 4, 5)),
                        "Each algorithm/input needs all five measured rounds, without warm-up rows.");
                for (Run run : group) {
                    check(run.results().equals(group.get(0).results()), "Outcomes must remain stable between runs.");
                    check(run.counters().equals(group.get(0).counters()), "Fresh executions must retain stable counters.");
                }
            }
        }
        GraphExperiments.verifyResults(measurements);
    }

    private static void topologicalResultsDistinguishDagsAndCycles() {
        for (Run run : measurements) {
            if (run.algorithm().equals("TopologicalSort")) {
                boolean cyclic = run.input().scenario().equals("cyclic-directed");
                check(run.results().equals(Map.of("has_cycle", cyclic ? 1L : 0L,
                        "ordered_vertices", cyclic ? 0L : (long) run.input().vertices())),
                        "A DAG exposes a complete order; a cyclic input exposes no partial order.");
            }
        }
    }

    private static void componentOutcomesMatchIndependentReachability() {
        for (GraphCase input : INPUTS) {
            if (!input.directed()) {
                continue;
            }
            WeightedGraph graph = regenerate(input);
            boolean[][] reachable = allReachability(graph);
            boolean[] assigned = new boolean[graph.vertexCount()];
            long count = 0;
            long largest = 0;
            for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                if (assigned[vertex]) {
                    continue;
                }
                long size = 0;
                for (int other = 0; other < graph.vertexCount(); other++) {
                    if (reachable[vertex][other] && reachable[other][vertex]) {
                        assigned[other] = true;
                        size++;
                    }
                }
                count++;
                largest = Math.max(largest, size);
            }
            Map<String, Long> expected = Map.of("component_count", count, "largest_component", largest,
                    "covered_vertices", (long) graph.vertexCount());
            for (Run run : group(input, "Kosaraju")) {
                check(run.results().equals(expected), "SCC aggregates must match independent mutual reachability.");
            }
        }
    }

    private static void distancesMatchIndependentBellmanFord() {
        for (GraphCase input : INPUTS) {
            WeightedGraph graph = regenerate(input);
            long[] distances = referenceDistances(graph);
            long reachable = 0;
            long sum = 0;
            long maximum = 0;
            long scannedAdjacency = 0;
            for (int vertex = 0; vertex < distances.length; vertex++) {
                if (distances[vertex] != Long.MAX_VALUE) {
                    reachable++;
                    sum += distances[vertex];
                    maximum = Math.max(maximum, distances[vertex]);
                    scannedAdjacency += graph.edgesFrom(vertex).size();
                }
            }
            Map<String, Long> expected = Map.of("source", 0L, "reachable_vertices", reachable,
                    "unreachable_vertices", graph.vertexCount() - reachable, "distance_sum", sum,
                    "max_distance", maximum);
            for (Run run : group(input, "Dijkstra")) {
                check(run.results().equals(expected), "Distance aggregates must match independent Bellman-Ford.");
                check(run.counters().get("processing_edge_examinations") == scannedAdjacency,
                        "Only adjacency of reachable vertices may be scanned after negative-weight validation.");
            }
        }
    }

    private static void countersRetainAllAlgorithmSpecificPhases() {
        for (Run run : measurements) {
            Map<String, Long> counters = run.counters();
            long edges = run.input().edges();
            switch (run.algorithm()) {
                case "TopologicalSort" -> {
                    check(counters.keySet().equals(Set.of("indegree_edge_examinations", "processing_edge_examinations",
                            "edge_examinations")), "Topological counters must retain both scanning phases.");
                    check(counters.get("indegree_edge_examinations") == edges, "Indegree counting must scan E edges.");
                    check(counters.get("processing_edge_examinations") <= edges,
                            "Processing cannot scan more than E directed edges.");
                    check(counters.get("edge_examinations") == edges + counters.get("processing_edge_examinations"),
                            "The topological total must be the sum of the two documented phases.");
                    if (run.input().scenario().equals("dag-directed")) {
                        check(counters.get("processing_edge_examinations") == edges, "A DAG must process every edge.");
                    }
                }
                case "Kosaraju" -> {
                    check(counters.keySet().equals(Set.of("first_pass_edge_examinations", "reversal_edge_examinations",
                            "second_pass_edge_examinations", "edge_examinations")), "All three SCC phases must be exported.");
                    for (String phase : List.of("first_pass_edge_examinations", "reversal_edge_examinations",
                            "second_pass_edge_examinations")) {
                        check(counters.get(phase) == edges, "Each SCC phase must examine E edges.");
                    }
                    check(counters.get("edge_examinations") == 3 * edges, "The SCC total must equal 3E.");
                }
                case "Dijkstra" -> {
                    check(counters.keySet().equals(Set.of("validation_edge_examinations", "processing_edge_examinations",
                            "edge_examinations", "relaxation_attempts", "successful_relaxations", "queue_offers",
                            "queue_polls", "queue_comparisons")), "Dijkstra must preserve validation, relaxation, and heap counters.");
                    check(counters.get("validation_edge_examinations") == edges, "All logical edges must be validated.");
                    check(counters.get("edge_examinations") == edges + counters.get("processing_edge_examinations"),
                            "The Dijkstra total must retain both kinds of examinations.");
                    check(counters.get("relaxation_attempts").equals(counters.get("processing_edge_examinations")),
                            "Every processed adjacency entry must attempt relaxation.");
                    check(counters.get("queue_offers") == counters.get("successful_relaxations") + 1,
                            "Heap offers include the initial source plus successful relaxations.");
                    check(counters.get("queue_polls").equals(counters.get("queue_offers")), "The lazy heap must be drained.");
                }
                case "Kruskal", "Prim" -> check(counters.containsKey("edge_examinations"), "MST counters must be retained.");
                default -> throw new AssertionError("Unexpected algorithm.");
            }
        }
    }

    private static void summariesAverageOnlyTheirFiveActualIntervals() {
        for (Summary summary : BenchmarkRunner.summarize(measurements)) {
            List<Run> group = group(summary.input(), summary.algorithm());
            double total = 0;
            for (Run run : group) {
                total += run.elapsedNanos();
            }
            check(summary.measuredRuns() == 5 && summary.averageElapsedNanos() == total / 5,
                    "Every summary must average its five actual intervals.");
            check(summary.averageElapsedMillis() == summary.averageElapsedNanos() / 1_000_000,
                    "Summary time units must be consistent.");
            for (String counter : group.get(0).counters().keySet()) {
                double sum = 0;
                for (Run run : group) {
                    sum += run.counters().get(counter);
                }
                check(summary.averageCounters().get(counter) == sum / 5, "Every counter must be averaged independently.");
            }
        }
    }

    private static void factoriesRunHandCalculatedDirectedGraphs() {
        WeightedGraph graph = knownDirectedGraph();
        check(task(graph, "TopologicalSort").results().equals(Map.of("has_cycle", 0L, "ordered_vertices", 6L)),
                "The adapter must run the real ordering algorithm on a known DAG.");
        check(task(graph, "Kosaraju").results().equals(Map.of("component_count", 6L, "largest_component", 1L,
                "covered_vertices", 6L)), "Every vertex in the known DAG is its own SCC.");
        check(task(graph, "Dijkstra").results().equals(Map.of("source", 0L, "reachable_vertices", 5L,
                "unreachable_vertices", 1L, "distance_sum", 15L, "max_distance", 7L)),
                "Distances 0, 3, 1, 4, 7 and an unreachable vertex must be summarized correctly.");
    }

    private static void unreachableAndZeroWeightCycleResultsRemainExplicit() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 0, 0);
        graph.addEdge(1, 2, 5);
        graph.addEdge(0, 2, 7);
        check(task(graph, "TopologicalSort").results().equals(Map.of("has_cycle", 1L, "ordered_vertices", 0L)),
                "Cyclic adapters must not present an acyclic prefix as a complete order.");
        check(task(graph, "Kosaraju").results().equals(Map.of("component_count", 3L, "largest_component", 2L,
                "covered_vertices", 4L)), "The zero-weight cycle and isolated vertex must remain in SCC outcomes.");
        check(task(graph, "Dijkstra").results().equals(Map.of("source", 0L, "reachable_vertices", 3L,
                "unreachable_vertices", 1L, "distance_sum", 5L, "max_distance", 5L)),
                "Unreachable sentinels must be excluded from finite distance aggregates.");
    }

    private static void factoriesCreateIndependentTasksAndAlgorithmState() {
        for (String name : List.of("TopologicalSort", "Kosaraju", "Dijkstra")) {
            WeightedGraph graph = knownDirectedGraph();
            Algorithm factory = GraphExperiments.algorithms(graph).stream()
                    .filter(algorithm -> algorithm.name().equals(name)).findFirst().orElseThrow();
            Task first = factory.factory().get();
            first.execute();
            Map<String, Long> counters = Map.copyOf(first.counters());
            Map<String, Long> result = Map.copyOf(first.results());
            graph.addEdge(4, 0, 0);
            graph.addEdge(4, 5, 0);
            Task second = factory.factory().get();
            check(first != second, "Every factory invocation must create a fresh task.");
            second.execute();
            check(!second.results().equals(result), "Later tasks must use changes made to the same captured graph.");
            check(first.counters().equals(counters) && first.results().equals(result),
                    "A later task must not overwrite earlier results or share mutable algorithm counters.");
        }
    }

    private static void allAdaptersPreserveTheirSharedInput() {
        for (boolean directed : List.of(false, true)) {
            WeightedGraph graph = new WeightedGraph(4, directed);
            graph.addEdge(0, 1, 1);
            graph.addEdge(1, 2, 2);
            graph.addEdge(2, 3, 3);
            List<Edge> edges = List.copyOf(graph.edges());
            List<List<Edge>> adjacency = new ArrayList<>();
            for (int vertex = 0; vertex < 4; vertex++) {
                adjacency.add(List.copyOf(graph.edgesFrom(vertex)));
            }
            for (Algorithm algorithm : GraphExperiments.algorithms(graph)) {
                Task task = algorithm.factory().get();
                task.execute();
                task.results();
                task.counters();
            }
            check(graph.edges().equals(edges), "Every adapter must preserve the shared logical edge list.");
            for (int vertex = 0; vertex < 4; vertex++) {
                check(graph.edgesFrom(vertex).equals(adjacency.get(vertex)), "Every adapter must preserve adjacency.");
            }
        }
    }

    private static void singleVertexFactoriesRemainUsable() {
        for (boolean directed : List.of(false, true)) {
            Task task = task(new WeightedGraph(1, directed), "Dijkstra");
            check(task.results().equals(Map.of("source", 0L, "reachable_vertices", 1L, "unreachable_vertices", 0L,
                    "distance_sum", 0L, "max_distance", 0L)), "A source-only graph has one zero finite distance.");
        }
    }

    private static void invalidFactoryInputsAreRejected() {
        expectIllegalArgument(() -> GraphExperiments.algorithms(new WeightedGraph(0, true)));
        expectIllegalArgument(() -> GraphExperiments.algorithms(new WeightedGraph(0, false)));
        try {
            GraphExperiments.algorithms(null);
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Null graph must be rejected.");
    }

    private static void missingCasesAndAlgorithmsAreRejected() {
        expectIllegalArgument(() -> GraphExperiments.verifyResults(measurements.stream()
                .filter(run -> !run.input().equals(INPUTS.get(3))).toList()));
        expectIllegalArgument(() -> GraphExperiments.verifyResults(measurements.stream()
                .filter(run -> !(run.input().equals(INPUTS.get(3)) && run.algorithm().equals("Dijkstra"))).toList()));
    }

    private static void unknownCasesAndAlgorithmsAreRejected() {
        GraphCase otherSeed = new GraphCase("dag-directed", 10, 30, 999, true);
        expectIllegalArgument(() -> GraphExperiments.verifyResults(changedGroup(INPUTS.get(3), "TopologicalSort",
                run -> new Run(otherSeed, run.algorithm(), run.run(), run.elapsedNanos(), run.counters(), run.results()))));
        expectIllegalArgument(() -> GraphExperiments.verifyResults(changedGroup(INPUTS.get(3), "TopologicalSort",
                run -> new Run(run.input(), "OtherAlgorithm", run.run(), run.elapsedNanos(), run.counters(), run.results()))));
    }

    private static void incompleteOrRepeatedRunNumbersAreRejected() {
        List<Run> incomplete = new ArrayList<>(measurements);
        incomplete.remove(0);
        expectIllegalArgument(() -> GraphExperiments.verifyResults(incomplete));
        expectIllegalArgument(() -> GraphExperiments.verifyResults(changedGroup(INPUTS.get(3), "Kosaraju",
                run -> run.run() == 5 ? new Run(run.input(), run.algorithm(), 1, run.elapsedNanos(),
                        run.counters(), run.results()) : run)));
    }

    private static void incorrectCycleAndOrderingResultsAreRejected() {
        expectInvalidMetric(INPUTS.get(3), "TopologicalSort", "has_cycle", 1);
        expectInvalidMetric(INPUTS.get(6), "TopologicalSort", "has_cycle", 0);
        expectInvalidMetric(INPUTS.get(6), "TopologicalSort", "ordered_vertices", 1);
        expectInvalidMetric(INPUTS.get(3), "TopologicalSort", "ordered_vertices", 9);
    }

    private static void incorrectComponentResultsAreRejected() {
        expectInvalidMetric(INPUTS.get(3), "Kosaraju", "component_count", 9);
        expectInvalidMetric(INPUTS.get(6), "Kosaraju", "largest_component", 1);
        expectInvalidMetric(INPUTS.get(6), "Kosaraju", "covered_vertices", 9);
        expectInvalidMetric(INPUTS.get(6), "Kosaraju", "component_count", 0);
    }

    private static void incorrectShortestPathResultsAreRejected() {
        expectInvalidMetric(INPUTS.get(0), "Dijkstra", "source", 1);
        expectInvalidMetric(INPUTS.get(0), "Dijkstra", "reachable_vertices", 9);
        expectInvalidMetric(INPUTS.get(3), "Dijkstra", "unreachable_vertices", 11);
        expectInvalidMetric(INPUTS.get(3), "Dijkstra", "distance_sum", -1);
        expectInvalidMetric(INPUTS.get(3), "Dijkstra", "max_distance", -1);
    }

    private static void incorrectMstResultsAreRejected() {
        expectInvalidMetric(INPUTS.get(0), "Prim", "total_weight", 102);
        expectInvalidMetric(INPUTS.get(0), "Kruskal", "component_count", 2);
        expectInvalidMetric(INPUTS.get(0), "Kruskal", "selected_edges", 8);
    }

    private static void completeCsvExportPreservesCoverageAndMetrics() throws IOException {
        Path directory = Files.createTempDirectory("cityplanner-all-csv-");
        try {
            BenchmarkCsv.write(directory, "all", measurements, GraphExperiments.environment());
            List<String> raw = Files.readAllLines(directory.resolve("all-runs.csv"), StandardCharsets.UTF_8);
            List<String> summary = Files.readAllLines(directory.resolve("all-summary.csv"), StandardCharsets.UTF_8);
            check(raw.size() == 136 && summary.size() == 28, "Export must preserve all 135 runs and 27 means.");
            List<String> header = Arrays.asList(raw.get(0).split(",", -1));
            for (String column : List.of("result_source", "result_has_cycle", "result_reachable_vertices",
                    "result_component_count", "counter_reversal_edge_examinations", "counter_validation_edge_examinations")) {
                check(header.contains(column), "Shared CSV must retain every algorithm's result and counter fields.");
            }
            for (String line : raw.subList(1, raw.size())) {
                String[] row = line.split(",", -1);
                check(row.length == header.size(), "Every exported row must match the shared schema.");
                if (row[5].equals("Dijkstra")) {
                    check(row[header.indexOf("result_source")].equals("0"), "Dijkstra source must be explicit in raw output.");
                    check(row[header.indexOf("result_has_cycle")].isEmpty(), "Inapplicable cycle metrics must remain empty.");
                }
            }
        } finally {
            deleteOutput(directory);
        }
    }

    private static void mainWritesTheCompleteSuiteToACustomDirectory() throws IOException {
        Path directory = Files.createTempDirectory("cityplanner-all-main-");
        try {
            GraphExperiments.main(new String[] {directory.toString()});
            check(Files.readAllLines(directory.resolve("all-runs.csv"), StandardCharsets.UTF_8).size() == 136,
                    "The public entry point must execute and export the complete suite.");
            check(Files.readAllLines(directory.resolve("all-summary.csv"), StandardCharsets.UTF_8).size() == 28,
                    "The public entry point must export all 27 means.");
            check(Files.readString(directory.resolve("all-environment.csv"), StandardCharsets.UTF_8)
                    .contains("dijkstra_source,0\n"), "Public export must document the shortest-path source.");
        } finally {
            deleteOutput(directory);
        }
    }

    private static void environmentDocumentsReproducibilityAndSource() {
        Map<String, String> environment = GraphExperiments.environment();
        check(environment.get("dijkstra_source").equals("0"), "Metadata must document the chosen source.");
        check(environment.get("warmup_runs").equals("3") && environment.get("measured_runs").equals("5"),
                "Metadata must retain the shared measurement settings.");
        check(environment.get("timer").equals("System.nanoTime"), "Metadata must identify the actual timer.");
        for (String key : List.of("captured_at_utc", "java_version", "java_vm_version", "os_name", "os_arch",
                "algorithm_order", "timing_scope", "instrumentation_enabled", "weight_min", "weight_max")) {
            check(environment.containsKey(key) && !environment.get(key).isBlank(), "Reproduction metadata must remain complete.");
        }
    }

    private static List<Run> group(GraphCase input, String algorithm) {
        return measurements.stream().filter(run -> run.input().equals(input) && run.algorithm().equals(algorithm)).toList();
    }

    private static List<Run> changedGroup(GraphCase input, String algorithm, UnaryOperator<Run> change) {
        List<Run> changed = new ArrayList<>();
        for (Run run : measurements) {
            changed.add(run.input().equals(input) && run.algorithm().equals(algorithm) ? change.apply(run) : run);
        }
        return changed;
    }

    private static void expectInvalidMetric(GraphCase input, String algorithm, String metric, long value) {
        List<Run> changed = changedGroup(input, algorithm, run -> {
            Map<String, Long> results = new LinkedHashMap<>(run.results());
            results.put(metric, value);
            return new Run(run.input(), run.algorithm(), run.run(), run.elapsedNanos(), run.counters(), results);
        });
        expectIllegalArgument(() -> GraphExperiments.verifyResults(changed));
    }

    private static Task task(WeightedGraph graph, String name) {
        Task task = GraphExperiments.algorithms(graph).stream().filter(algorithm -> algorithm.name().equals(name))
                .findFirst().orElseThrow().factory().get();
        task.execute();
        return task;
    }

    private static WeightedGraph knownDirectedGraph() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 5);
        graph.addEdge(3, 4, 3);
        return graph;
    }

    private static WeightedGraph regenerate(GraphCase input) {
        return switch (input.scenario()) {
            case "connected-undirected" -> ConnectedGraphGenerator.generate(input.vertices(), input.edges(), input.seed());
            case "dag-directed" -> DirectedGraphGenerator.generateDag(input.vertices(), input.edges(), input.seed());
            case "cyclic-directed" -> DirectedGraphGenerator.generateCyclic(input.vertices(), input.edges(), input.seed());
            default -> throw new AssertionError("Unexpected scenario.");
        };
    }

    private static boolean[][] allReachability(WeightedGraph graph) {
        boolean[][] reachable = new boolean[graph.vertexCount()][graph.vertexCount()];
        for (int source = 0; source < graph.vertexCount(); source++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            reachable[source][source] = true;
            queue.add(source);
            while (!queue.isEmpty()) {
                int vertex = queue.removeFirst();
                for (Edge edge : graph.edgesFrom(vertex)) {
                    if (!reachable[source][edge.to()]) {
                        reachable[source][edge.to()] = true;
                        queue.addLast(edge.to());
                    }
                }
            }
        }
        return reachable;
    }

    private static long[] referenceDistances(WeightedGraph graph) {
        long[] distances = new long[graph.vertexCount()];
        Arrays.fill(distances, Long.MAX_VALUE);
        distances[0] = 0;
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                if (distances[vertex] == Long.MAX_VALUE) {
                    continue;
                }
                for (Edge edge : graph.edgesFrom(vertex)) {
                    long candidate = distances[vertex] + edge.weight();
                    if (candidate < distances[edge.to()]) {
                        distances[edge.to()] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return distances;
    }

    private static void deleteOutput(Path directory) throws IOException {
        for (String suffix : List.of("-runs.csv", "-summary.csv", "-environment.csv")) {
            Files.deleteIfExists(directory.resolve("all" + suffix));
        }
        Files.deleteIfExists(directory);
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
