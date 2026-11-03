package cityplanner.experiments;

import cityplanner.connectivity.Kosaraju;
import cityplanner.connectivity.SccResult;
import cityplanner.connectivity.TopologicalSort;
import cityplanner.connectivity.TopologicalSortResult;
import cityplanner.graph.WeightedGraph;
import cityplanner.shortestpath.Dijkstra;
import cityplanner.shortestpath.DijkstraResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GraphExperiments {
    private static final int[][] SIZES = {{10, 30}, {100, 500}, {1000, 5000}};
    private static final String CONNECTED = "connected-undirected";
    private static final String DAG = "dag-directed";
    private static final String CYCLIC = "cyclic-directed";
    private static final List<BenchmarkRunner.GraphCase> INPUTS = requiredInputs();
    private static final Set<String> UNDIRECTED_ALGORITHMS = Set.of("Kruskal", "Prim", "Dijkstra");
    private static final Set<String> DIRECTED_ALGORITHMS = Set.of("TopologicalSort", "Kosaraju", "Dijkstra");
    private static final int SOURCE = 0;

    private GraphExperiments() {
    }

    private static List<BenchmarkRunner.GraphCase> requiredInputs() {
        List<BenchmarkRunner.GraphCase> inputs = new ArrayList<>();
        for (int index = 0; index < SIZES.length; index++) {
            int vertices = SIZES[index][0];
            int edges = SIZES[index][1];
            inputs.add(new BenchmarkRunner.GraphCase(CONNECTED, vertices, edges, 41100L + index, false));
            inputs.add(new BenchmarkRunner.GraphCase(DAG, vertices, edges, 41200L + index, true));
            inputs.add(new BenchmarkRunner.GraphCase(CYCLIC, vertices, edges, 41300L + index, true));
        }
        return List.copyOf(inputs);
    }

    public static List<BenchmarkRunner.Run> run() {
        BenchmarkRunner runner = new BenchmarkRunner();
        List<BenchmarkRunner.Run> runs = new ArrayList<>();
        for (BenchmarkRunner.GraphCase input : INPUTS) {
            WeightedGraph graph = switch (input.scenario()) {
                case CONNECTED -> ConnectedGraphGenerator.generate(input.vertices(), input.edges(), input.seed());
                case DAG -> DirectedGraphGenerator.generateDag(input.vertices(), input.edges(), input.seed());
                case CYCLIC -> DirectedGraphGenerator.generateCyclic(input.vertices(), input.edges(), input.seed());
                default -> throw new IllegalStateException("Unknown experiment scenario.");
            };
            runs.addAll(runner.measure(input, algorithms(graph)));
        }
        verifyResults(runs);
        return List.copyOf(runs);
    }

    static List<BenchmarkRunner.Algorithm> algorithms(WeightedGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (graph.vertexCount() == 0) {
            throw new IllegalArgumentException("Experiments require source vertex zero.");
        }
        List<BenchmarkRunner.Algorithm> entries = new ArrayList<>();
        if (graph.isDirected()) {
            entries.add(topologicalSort(graph));
            entries.add(kosaraju(graph));
        } else {
            entries.addAll(MstExperiments.algorithms(graph));
        }
        entries.add(dijkstra(graph));
        return List.copyOf(entries);
    }

    private static BenchmarkRunner.Algorithm topologicalSort(WeightedGraph graph) {
        return new BenchmarkRunner.Algorithm("TopologicalSort", () -> new BenchmarkRunner.Task() {
            private final TopologicalSort algorithm = new TopologicalSort();
            private TopologicalSortResult result;

            @Override
            public void execute() {
                result = algorithm.sort(graph);
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("indegree_edge_examinations", algorithm.indegreeEdgeExaminations(),
                        "processing_edge_examinations", algorithm.processingEdgeExaminations(),
                        "edge_examinations", algorithm.edgeExaminations());
            }

            @Override
            public Map<String, Long> results() {
                return Map.of("has_cycle", result.hasCycle() ? 1L : 0L,
                        "ordered_vertices", (long) result.order().size());
            }
        });
    }

    private static BenchmarkRunner.Algorithm kosaraju(WeightedGraph graph) {
        return new BenchmarkRunner.Algorithm("Kosaraju", () -> new BenchmarkRunner.Task() {
            private final Kosaraju algorithm = new Kosaraju();
            private SccResult result;

            @Override
            public void execute() {
                result = algorithm.findStronglyConnectedComponents(graph);
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("first_pass_edge_examinations", algorithm.firstPassEdgeExaminations(),
                        "reversal_edge_examinations", algorithm.reversalEdgeExaminations(),
                        "second_pass_edge_examinations", algorithm.secondPassEdgeExaminations(),
                        "edge_examinations", algorithm.edgeExaminations());
            }

            @Override
            public Map<String, Long> results() {
                long covered = 0;
                long largest = 0;
                for (List<Integer> component : result.components()) {
                    covered += component.size();
                    largest = Math.max(largest, component.size());
                }
                return Map.of("component_count", (long) result.componentCount(),
                        "covered_vertices", covered, "largest_component", largest);
            }
        });
    }

    private static BenchmarkRunner.Algorithm dijkstra(WeightedGraph graph) {
        return new BenchmarkRunner.Algorithm("Dijkstra", () -> new BenchmarkRunner.Task() {
            private final Dijkstra algorithm = new Dijkstra();
            private DijkstraResult result;

            @Override
            public void execute() {
                result = algorithm.findShortestPaths(graph, SOURCE);
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("validation_edge_examinations", algorithm.validationEdgeExaminations(),
                        "processing_edge_examinations", algorithm.processingEdgeExaminations(),
                        "edge_examinations", algorithm.edgeExaminations(),
                        "relaxation_attempts", algorithm.relaxationAttempts(),
                        "successful_relaxations", algorithm.successfulRelaxations(),
                        "queue_offers", algorithm.queueOffers(), "queue_polls", algorithm.queuePolls(),
                        "queue_comparisons", algorithm.queueComparisons());
            }

            @Override
            public Map<String, Long> results() {
                long reachable = 0;
                long sum = 0;
                long maximum = 0;
                for (int vertex = 0; vertex < result.vertexCount(); vertex++) {
                    if (result.isReachable(vertex)) {
                        reachable++;
                        long distance = result.distanceTo(vertex);
                        sum = Math.addExact(sum, distance);
                        maximum = Math.max(maximum, distance);
                    }
                }
                return Map.of("source", (long) result.source(), "reachable_vertices", reachable,
                        "unreachable_vertices", result.vertexCount() - reachable,
                        "distance_sum", sum, "max_distance", maximum);
            }
        });
    }

    public static void verifyResults(List<BenchmarkRunner.Run> runs) {
        List<BenchmarkRunner.Summary> summaries = BenchmarkRunner.summarize(runs);
        Map<BenchmarkRunner.GraphCase, Set<String>> coverage = new LinkedHashMap<>();
        for (BenchmarkRunner.Summary summary : summaries) {
            BenchmarkRunner.GraphCase input = summary.input();
            Set<String> expectedAlgorithms = input.directed() ? DIRECTED_ALGORITHMS : UNDIRECTED_ALGORITHMS;
            if (!INPUTS.contains(input) || !expectedAlgorithms.contains(summary.algorithm())) {
                throw new IllegalArgumentException("Measurements must use the prescribed inputs and algorithms.");
            }
            coverage.computeIfAbsent(input, key -> new HashSet<>()).add(summary.algorithm());
            switch (summary.algorithm()) {
                case "TopologicalSort" -> verifyOrdering(summary);
                case "Kosaraju" -> verifyComponents(summary);
                case "Dijkstra" -> verifyDistances(summary);
                default -> { }
            }
        }
        if (!coverage.keySet().equals(Set.copyOf(INPUTS))) {
            throw new IllegalArgumentException("Measurements must cover all nine required inputs.");
        }
        for (Map.Entry<BenchmarkRunner.GraphCase, Set<String>> entry : coverage.entrySet()) {
            Set<String> expected = entry.getKey().directed() ? DIRECTED_ALGORITHMS : UNDIRECTED_ALGORITHMS;
            if (!entry.getValue().equals(expected)) {
                throw new IllegalArgumentException("Every input must include all applicable algorithms.");
            }
        }
        List<BenchmarkRunner.Run> mstRuns = runs.stream()
                .filter(run -> run.algorithm().equals("Kruskal") || run.algorithm().equals("Prim")).toList();
        MstExperiments.verifyResults(mstRuns);
    }

    private static void verifyOrdering(BenchmarkRunner.Summary summary) {
        Map<String, Long> result = summary.results();
        boolean cyclic = summary.input().scenario().equals(CYCLIC);
        if (!result.keySet().equals(Set.of("has_cycle", "ordered_vertices"))
                || result.get("has_cycle") != (cyclic ? 1L : 0L)
                || result.get("ordered_vertices") != (cyclic ? 0L : summary.input().vertices())) {
            throw new IllegalArgumentException("Ordering results must distinguish a complete DAG order from a cycle.");
        }
    }

    private static void verifyComponents(BenchmarkRunner.Summary summary) {
        Map<String, Long> result = summary.results();
        if (!result.keySet().equals(Set.of("component_count", "covered_vertices", "largest_component"))) {
            throw new IllegalArgumentException("SCC results must include counts and coverage.");
        }
        long vertices = summary.input().vertices();
        long count = result.get("component_count");
        long largest = result.get("largest_component");
        boolean dag = summary.input().scenario().equals(DAG);
        if (result.get("covered_vertices") != vertices || count < 1 || count > vertices
                || largest < 1 || largest > vertices - count + 1 || largest * count < vertices
                || (dag && (count != vertices || largest != 1))
                || (!dag && (count == vertices || largest < 2))) {
            throw new IllegalArgumentException("SCC results must cover every vertex and match the scenario.");
        }
    }

    private static void verifyDistances(BenchmarkRunner.Summary summary) {
        Map<String, Long> result = summary.results();
        if (!result.keySet().equals(Set.of("source", "reachable_vertices", "unreachable_vertices",
                "distance_sum", "max_distance"))) {
            throw new IllegalArgumentException("Shortest-path results must include the source and finite distances.");
        }
        long vertices = summary.input().vertices();
        long reachable = result.get("reachable_vertices");
        long maximum = result.get("max_distance");
        long sum = result.get("distance_sum");
        if (result.get("source") != SOURCE || reachable < 1 || reachable > vertices
                || result.get("unreachable_vertices") != vertices - reachable
                || maximum < 0 || maximum > (vertices - 1) * ConnectedGraphGenerator.MAX_WEIGHT
                || sum < maximum || sum > maximum * (reachable - 1)
                || (!summary.input().directed() && reachable != vertices)) {
            throw new IllegalArgumentException("Shortest-path results must preserve source zero and reachability.");
        }
    }

    public static Map<String, String> environment() {
        Map<String, String> metadata = new LinkedHashMap<>(MstExperiments.environment());
        metadata.put("experiment_suite", "all");
        metadata.put("dijkstra_source", Integer.toString(SOURCE));
        metadata.put("scenarios", CONNECTED + "," + DAG + "," + CYCLIC);
        metadata.put("undirected_algorithm_order", "Kruskal,Prim,Dijkstra");
        metadata.put("directed_algorithm_order", "TopologicalSort,Kosaraju,Dijkstra");
        metadata.put("connected_seeds", "41100,41101,41102");
        metadata.put("dag_seeds", "41200,41201,41202");
        metadata.put("cyclic_seeds", "41300,41301,41302");
        return Map.copyOf(metadata);
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: GraphExperiments [output-directory]");
        }
        Path directory = args.length == 0 ? Path.of("results", "tables") : Path.of(args[0]);
        List<BenchmarkRunner.Run> runs = run();
        BenchmarkCsv.write(directory, "all", runs, environment());
        System.out.println("Complete experiments saved to " + directory.toAbsolutePath().normalize());
        System.out.println(runs.size() + " measured runs across " + BenchmarkRunner.summarize(runs).size()
                + " algorithm/input groups; " + BenchmarkRunner.WARMUP_RUNS + " warm-up runs and "
                + BenchmarkRunner.MEASURED_RUNS + " measured runs per group.");
    }
}
