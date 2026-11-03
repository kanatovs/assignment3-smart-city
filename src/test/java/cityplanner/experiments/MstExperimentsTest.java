package cityplanner.experiments;

import cityplanner.experiments.BenchmarkRunner.Algorithm;
import cityplanner.experiments.BenchmarkRunner.GraphCase;
import cityplanner.experiments.BenchmarkRunner.Run;
import cityplanner.experiments.BenchmarkRunner.Summary;
import cityplanner.experiments.BenchmarkRunner.Task;
import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MstExperimentsTest {
    private static final List<GraphCase> INPUTS = List.of(
            new GraphCase("connected-undirected", 10, 30, 41100L, false),
            new GraphCase("connected-undirected", 100, 500, 41101L, false),
            new GraphCase("connected-undirected", 1000, 5000, 41102L, false));
    private static final Set<String> RESULT_KEYS = Set.of("total_weight", "component_count", "selected_edges");
    private static List<Run> measurements;

    private MstExperimentsTest() {
    }

    public static void main(String[] args) {
        measurements = MstExperiments.run();
        everyRequiredInputHasFiveRunsPerAlgorithm();
        measuredOutcomesAgreeAndSpanEveryVertex();
        countersKeepTheirAlgorithmSpecificDefinitions();
        summariesDescribeAllMeasuredGroups();
        factoriesCreateFreshTasksForTheSameInput();
        factoryTasksKeepIndependentAlgorithmState();
        algorithmTasksPreserveTheInputGraph();
        differentMinimumWeightsAreRejected();
        disconnectedOrIncompleteResultsAreRejected();
        missingAndUnknownAlgorithmsAreRejected();
        incompleteAndRepeatedRunNumbersAreRejected();
        System.out.println("MstExperimentsTest passed.");
    }

    private static void everyRequiredInputHasFiveRunsPerAlgorithm() {
        check(measurements.size() == 30, "Three inputs and two algorithms must produce thirty measured rows.");
        for (Run run : measurements) {
            check(INPUTS.contains(run.input()), "Every row must retain the prescribed size, seed, and scenario.");
            check(Set.of("Kruskal", "Prim").contains(run.algorithm()), "Only the two MST algorithms belong in this suite.");
        }
        for (GraphCase input : INPUTS) {
            for (String algorithm : List.of("Kruskal", "Prim")) {
                List<Run> group = group(input, algorithm);
                check(group.size() == 5, "Every input and algorithm needs exactly five measured runs.");
                Set<Integer> numbers = new HashSet<>();
                for (Run run : group) {
                    numbers.add(run.run());
                }
                check(numbers.equals(Set.of(1, 2, 3, 4, 5)), "Measured run numbers must exclude all warm-up calls.");
            }
        }
        MstExperiments.verifyResults(measurements);
    }

    private static void measuredOutcomesAgreeAndSpanEveryVertex() {
        for (GraphCase input : INPUTS) {
            Set<Long> weights = new HashSet<>();
            for (Run run : measurements) {
                if (!run.input().equals(input)) {
                    continue;
                }
                check(run.results().keySet().equals(RESULT_KEYS), "MST outcomes must expose the documented fields.");
                check(run.results().get("component_count") == 1, "Every generated MST input must be connected.");
                check(run.results().get("selected_edges") == input.vertices() - 1L,
                        "Every measured result must span all vertices with V - 1 edges.");
                weights.add(run.results().get("total_weight"));
            }
            check(weights.size() == 1, "All runs of both methods must agree on the minimum total weight.");
        }
    }

    private static void countersKeepTheirAlgorithmSpecificDefinitions() {
        Set<String> kruskalKeys = Set.of("sorting_comparisons", "edge_examinations", "find_calls", "union_calls",
                "parent_traversals");
        Set<String> primKeys = Set.of("edge_examinations", "queue_offers", "queue_polls", "queue_comparisons");
        for (Run run : measurements) {
            Map<String, Long> counters = run.counters();
            if (run.algorithm().equals("Kruskal")) {
                check(counters.keySet().equals(kruskalKeys), "Kruskal must export sorting and DSU counters separately.");
                check(counters.get("edge_examinations").equals(counters.get("union_calls")),
                        "Every Kruskal edge examination must attempt one union.");
                check(counters.get("find_calls") == 2 * counters.get("union_calls"),
                        "Each valid union must perform two root searches.");
                check(counters.get("sorting_comparisons") > 0, "Sorting comparisons must be recorded for these inputs.");
            } else {
                check(counters.keySet().equals(primKeys), "Prim must export heap counters separately.");
                check(counters.get("edge_examinations") == 2L * run.input().edges(),
                        "Prim must scan both orientations of every generated edge.");
                check(counters.get("queue_offers") == run.input().edges()
                        && counters.get("queue_polls") == run.input().edges(),
                        "Prim must offer and drain every non-loop logical edge exactly once.");
                check(counters.get("queue_comparisons") > 0, "Heap comparisons must be recorded for these inputs.");
            }
        }
    }

    private static void summariesDescribeAllMeasuredGroups() {
        List<Summary> summaries = BenchmarkRunner.summarize(measurements);
        check(summaries.size() == 6, "The suite must have one summary for each input and algorithm.");
        for (Summary summary : summaries) {
            List<Run> group = group(summary.input(), summary.algorithm());
            check(group.size() == 5 && summary.measuredRuns() == 5, "A summary must average exactly five measured runs.");
            double totalNanos = 0;
            for (Run run : group) {
                totalNanos += run.elapsedNanos();
            }
            checkClose(summary.averageElapsedNanos(), totalNanos / 5, "The summary must average its actual intervals.");
            checkClose(summary.averageElapsedMillis(), summary.averageElapsedNanos() / 1_000_000,
                    "Milliseconds must be a unit conversion of the nanosecond mean.");
            check(summary.averageCounters().keySet().equals(group.get(0).counters().keySet()),
                    "A summary must retain each algorithm's counter schema.");
            check(summary.results().equals(group.get(0).results()), "The summary must retain the verified MST outcome.");
        }
    }

    private static void factoriesCreateFreshTasksForTheSameInput() {
        WeightedGraph graph = knownGraph();
        List<Algorithm> algorithms = MstExperiments.algorithms(graph);
        check(algorithms.size() == 2, "The MST input must have exactly two algorithm factories.");
        Set<String> names = new HashSet<>();
        for (Algorithm algorithm : algorithms) {
            names.add(algorithm.name());
            Task first = algorithm.factory().get();
            Task second = algorithm.factory().get();
            check(first != second, "Every factory call must create a fresh task.");
            first.execute();
            second.execute();
            check(first.results().equals(Map.of("total_weight", 19L, "component_count", 1L, "selected_edges", 3L)),
                    "Both adapters must run the real algorithm on the hand-calculated graph.");
            check(second.results().equals(first.results()) && second.counters().equals(first.counters()),
                    "A fresh task must not accumulate the previous task's counters.");
        }
        check(names.equals(Set.of("Kruskal", "Prim")), "Factories must identify both MST algorithms.");
    }

    private static void factoryTasksKeepIndependentAlgorithmState() {
        for (String name : List.of("Kruskal", "Prim")) {
            WeightedGraph graph = knownGraph();
            Algorithm factory = MstExperiments.algorithms(graph).stream()
                    .filter(candidate -> candidate.name().equals(name)).findFirst().orElseThrow();
            Task first = factory.factory().get();
            first.execute();
            Map<String, Long> firstCounters = Map.copyOf(first.counters());
            graph.addEdge(0, 1, 1);
            Task second = factory.factory().get();
            second.execute();
            check(second.results().get("total_weight") == 10,
                    "Factories must use the same input graph, including changes made before a later invocation.");
            check(first.results().get("total_weight") == 19 && first.counters().equals(firstCounters),
                    "Later tasks must not replace an earlier task's result or algorithm counter state.");
        }
    }

    private static void algorithmTasksPreserveTheInputGraph() {
        WeightedGraph graph = knownGraph();
        List<Edge> edges = List.copyOf(graph.edges());
        List<List<Edge>> adjacency = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            adjacency.add(List.copyOf(graph.edgesFrom(vertex)));
        }
        for (Algorithm algorithm : MstExperiments.algorithms(graph)) {
            Task task = algorithm.factory().get();
            task.execute();
            task.results();
            task.counters();
        }
        check(graph.edges().equals(edges), "Measurements must preserve the input edge list and its order.");
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(graph.edgesFrom(vertex).equals(adjacency.get(vertex)), "Measurements must preserve adjacency.");
        }
    }

    private static void differentMinimumWeightsAreRejected() {
        List<Run> changed = new ArrayList<>();
        for (Run run : measurements) {
            if (run.input().equals(INPUTS.get(0)) && run.algorithm().equals("Prim")) {
                changed.add(withResult(run, "total_weight", run.results().get("total_weight") + 1));
            } else {
                changed.add(run);
            }
        }
        expectIllegalArgument(() -> MstExperiments.verifyResults(changed));
    }

    private static void disconnectedOrIncompleteResultsAreRejected() {
        List<Run> disconnected = changedFirstCaseResults("component_count", 2);
        expectIllegalArgument(() -> MstExperiments.verifyResults(disconnected));
        List<Run> incomplete = changedFirstCaseResults("selected_edges", INPUTS.get(0).vertices() - 2L);
        expectIllegalArgument(() -> MstExperiments.verifyResults(incomplete));
    }

    private static void missingAndUnknownAlgorithmsAreRejected() {
        List<Run> missing = measurements.stream()
                .filter(run -> !(run.input().equals(INPUTS.get(0)) && run.algorithm().equals("Prim"))).toList();
        expectIllegalArgument(() -> MstExperiments.verifyResults(missing));
        List<Run> unknown = new ArrayList<>();
        for (Run run : measurements) {
            unknown.add(run.input().equals(INPUTS.get(0)) && run.algorithm().equals("Kruskal")
                    ? new Run(run.input(), "OtherMst", run.run(), run.elapsedNanos(), run.counters(), run.results()) : run);
        }
        expectIllegalArgument(() -> MstExperiments.verifyResults(unknown));
    }

    private static void incompleteAndRepeatedRunNumbersAreRejected() {
        List<Run> incomplete = new ArrayList<>(measurements);
        incomplete.remove(0);
        expectIllegalArgument(() -> MstExperiments.verifyResults(incomplete));
        List<Run> repeated = new ArrayList<>(measurements);
        for (int index = 0; index < repeated.size(); index++) {
            Run run = repeated.get(index);
            if (run.input().equals(INPUTS.get(0)) && run.algorithm().equals("Kruskal") && run.run() == 5) {
                repeated.set(index, new Run(run.input(), run.algorithm(), 1, run.elapsedNanos(), run.counters(), run.results()));
                break;
            }
        }
        expectIllegalArgument(() -> MstExperiments.verifyResults(repeated));
    }

    private static List<Run> group(GraphCase input, String algorithm) {
        return measurements.stream().filter(run -> run.input().equals(input) && run.algorithm().equals(algorithm)).toList();
    }

    private static List<Run> changedFirstCaseResults(String metric, long value) {
        List<Run> changed = new ArrayList<>();
        for (Run run : measurements) {
            changed.add(run.input().equals(INPUTS.get(0)) ? withResult(run, metric, value) : run);
        }
        return changed;
    }

    private static Run withResult(Run run, String metric, long value) {
        Map<String, Long> results = new LinkedHashMap<>(run.results());
        results.put(metric, value);
        return new Run(run.input(), run.algorithm(), run.run(), run.elapsedNanos(), run.counters(), results);
    }

    private static WeightedGraph knownGraph() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 6);
        graph.addEdge(0, 3, 5);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 4);
        return graph;
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) <= Math.max(1, Math.abs(expected)) * 1e-12, message);
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
