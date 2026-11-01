package cityplanner.experiments;

import cityplanner.graph.WeightedGraph;
import cityplanner.mst.Kruskal;
import cityplanner.mst.MstResult;
import cityplanner.mst.Prim;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MstExperiments {
    private static final int[][] SIZES = {{10, 30}, {100, 500}, {1000, 5000}};
    private static final long[] SEEDS = {41100L, 41101L, 41102L};
    private static final Set<String> RESULT_KEYS = Set.of("total_weight", "component_count", "selected_edges");

    private MstExperiments() {
    }

    public static List<BenchmarkRunner.Run> run() {
        BenchmarkRunner runner = new BenchmarkRunner();
        List<BenchmarkRunner.Run> runs = new ArrayList<>();
        for (int index = 0; index < SIZES.length; index++) {
            WeightedGraph graph = ConnectedGraphGenerator.generate(SIZES[index][0], SIZES[index][1], SEEDS[index]);
            BenchmarkRunner.GraphCase input = new BenchmarkRunner.GraphCase("connected-undirected",
                    graph.vertexCount(), graph.edgeCount(), SEEDS[index], graph.isDirected());
            runs.addAll(runner.measure(input, algorithms(graph)));
        }
        verifyResults(runs);
        return List.copyOf(runs);
    }

    static List<BenchmarkRunner.Algorithm> algorithms(WeightedGraph graph) {
        Objects.requireNonNull(graph);
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST experiments require undirected input.");
        }
        return List.of(new BenchmarkRunner.Algorithm("Kruskal", () -> new BenchmarkRunner.Task() {
            private final Kruskal algorithm = new Kruskal();
            private MstResult result;

            @Override
            public void execute() {
                result = algorithm.findMinimumSpanningForest(graph);
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("sorting_comparisons", algorithm.sortingComparisons(),
                        "edge_examinations", algorithm.edgeExaminations(), "find_calls", algorithm.findCalls(),
                        "union_calls", algorithm.unionCalls(), "parent_traversals", algorithm.parentTraversals());
            }

            @Override
            public Map<String, Long> results() {
                return mstResults(result);
            }
        }), new BenchmarkRunner.Algorithm("Prim", () -> new BenchmarkRunner.Task() {
            private final Prim algorithm = new Prim();
            private MstResult result;

            @Override
            public void execute() {
                result = algorithm.findMinimumSpanningForest(graph);
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("edge_examinations", algorithm.edgeExaminations(),
                        "queue_offers", algorithm.queueOffers(), "queue_polls", algorithm.queuePolls(),
                        "queue_comparisons", algorithm.queueComparisons());
            }

            @Override
            public Map<String, Long> results() {
                return mstResults(result);
            }
        }));
    }

    private static Map<String, Long> mstResults(MstResult result) {
        return Map.of("total_weight", result.totalWeight(), "component_count", (long) result.componentCount(),
                "selected_edges", (long) result.edges().size());
    }

    public static void verifyResults(List<BenchmarkRunner.Run> runs) {
        List<BenchmarkRunner.Summary> summaries = BenchmarkRunner.summarize(runs);
        Map<BenchmarkRunner.GraphCase, List<BenchmarkRunner.Summary>> groups = new LinkedHashMap<>();
        for (BenchmarkRunner.Summary summary : summaries) {
            BenchmarkRunner.GraphCase input = summary.input();
            Map<String, Long> result = summary.results();
            if (input.directed() || input.vertices() < 1 || !result.keySet().equals(RESULT_KEYS)
                    || result.get("component_count") != 1L || result.get("selected_edges") != input.vertices() - 1L) {
                throw new IllegalArgumentException("Every MST result must span one connected undirected input.");
            }
            groups.computeIfAbsent(input, key -> new ArrayList<>()).add(summary);
        }
        for (List<BenchmarkRunner.Summary> group : groups.values()) {
            Set<String> names = new HashSet<>();
            long weight = group.get(0).results().get("total_weight");
            for (BenchmarkRunner.Summary summary : group) {
                names.add(summary.algorithm());
                if (summary.results().get("total_weight") != weight) {
                    throw new IllegalArgumentException("Kruskal and Prim weights must agree on the same input.");
                }
            }
            if (group.size() != 2 || !names.equals(Set.of("Kruskal", "Prim"))) {
                throw new IllegalArgumentException("Every MST input must have both Kruskal and Prim measurements.");
            }
        }
    }

    public static Map<String, String> environment() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("captured_at_utc", Instant.now().toString());
        for (String property : List.of("java.version", "java.vendor", "java.vm.name", "java.vm.version",
                "os.name", "os.version", "os.arch")) {
            metadata.put(property.replace('.', '_'), System.getProperty(property, "unknown"));
        }
        metadata.put("available_processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
        metadata.put("max_heap_bytes", Long.toString(Runtime.getRuntime().maxMemory()));
        metadata.put("warmup_runs", Integer.toString(BenchmarkRunner.WARMUP_RUNS));
        metadata.put("measured_runs", Integer.toString(BenchmarkRunner.MEASURED_RUNS));
        metadata.put("timer", "System.nanoTime");
        metadata.put("algorithm_order", "alternating_per_round");
        metadata.put("timing_scope", "complete_algorithm_method");
        metadata.put("instrumentation_enabled", "true");
        metadata.put("weight_min", Integer.toString(ConnectedGraphGenerator.MIN_WEIGHT));
        metadata.put("weight_max", Integer.toString(ConnectedGraphGenerator.MAX_WEIGHT));
        return Map.copyOf(metadata);
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: MstExperiments [output-directory]");
        }
        Path directory = args.length == 0 ? Path.of("results", "tables") : Path.of(args[0]);
        List<BenchmarkRunner.Run> runs = run();
        BenchmarkCsv.write(directory, "mst", runs, environment());
        System.out.println("MST experiments saved to " + directory.toAbsolutePath().normalize());
        System.out.println(runs.size() + " measured runs; " + BenchmarkRunner.WARMUP_RUNS
                + " warm-up runs and " + BenchmarkRunner.MEASURED_RUNS + " measured runs per algorithm/input.");
    }
}
