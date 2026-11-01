package cityplanner.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class BenchmarkRunner {
    public static final int WARMUP_RUNS = 3;
    public static final int MEASURED_RUNS = 5;

    private final LongSupplier nanoTime;

    public BenchmarkRunner() {
        this(System::nanoTime);
    }

    BenchmarkRunner(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "Timer must not be null.");
    }

    public interface Task {
        void execute();

        Map<String, Long> counters();

        Map<String, Long> results();
    }

    public record GraphCase(String scenario, int vertices, int edges, long seed, boolean directed) {
        public GraphCase {
            scenario = requireName(scenario);
            if (vertices < 0 || edges < 0) {
                throw new IllegalArgumentException("Graph sizes must not be negative.");
            }
        }
    }

    public record Algorithm(String name, Supplier<Task> factory) {
        public Algorithm {
            name = requireName(name);
            Objects.requireNonNull(factory, "Task factory must not be null.");
        }
    }

    public record Run(GraphCase input, String algorithm, int run, long elapsedNanos,
                      Map<String, Long> counters, Map<String, Long> results) {
        public Run {
            Objects.requireNonNull(input, "Input metadata must not be null.");
            algorithm = requireName(algorithm);
            if (run < 1 || run > MEASURED_RUNS || elapsedNanos < 0) {
                throw new IllegalArgumentException("Invalid measured run number or elapsed time.");
            }
            counters = copyMetrics(counters, true);
            results = copyMetrics(results, false);
        }
    }

    public record Summary(GraphCase input, String algorithm, int measuredRuns, double averageElapsedNanos,
                          Map<String, Double> averageCounters, Map<String, Long> results) {
        public Summary {
            Objects.requireNonNull(input, "Input metadata must not be null.");
            algorithm = requireName(algorithm);
            if (measuredRuns != MEASURED_RUNS || !Double.isFinite(averageElapsedNanos) || averageElapsedNanos < 0) {
                throw new IllegalArgumentException("Invalid summary count or average time.");
            }
            Map<String, Double> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : averageCounters.entrySet()) {
                requireMetricName(entry.getKey());
                double value = Objects.requireNonNull(entry.getValue());
                if (!Double.isFinite(value) || value < 0) {
                    throw new IllegalArgumentException("Average counters must be finite and non-negative.");
                }
                snapshot.put(entry.getKey(), value);
            }
            averageCounters = Collections.unmodifiableMap(snapshot);
            results = copyMetrics(results, false);
        }

        public double averageElapsedMillis() {
            return averageElapsedNanos / 1_000_000.0;
        }
    }

    public List<Run> measure(GraphCase input, List<Algorithm> algorithms) {
        Objects.requireNonNull(input, "Input metadata must not be null.");
        List<Algorithm> entries = List.copyOf(algorithms);
        Set<String> names = new HashSet<>();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("At least one algorithm is required.");
        }
        for (Algorithm algorithm : entries) {
            if (!names.add(algorithm.name())) {
                throw new IllegalArgumentException("Algorithm names must be unique within a case.");
            }
        }

        Map<String, Map<String, Long>> expectedResults = new LinkedHashMap<>();
        for (int warmup = 0; warmup < WARMUP_RUNS; warmup++) {
            for (Algorithm algorithm : entries) {
                Task task = Objects.requireNonNull(algorithm.factory().get());
                task.execute();
                Map<String, Long> results = copyMetrics(task.results(), false);
                Map<String, Long> previous = expectedResults.putIfAbsent(algorithm.name(), results);
                if (previous != null && !previous.equals(results)) {
                    throw new IllegalStateException("Warm-up results changed for " + algorithm.name());
                }
            }
        }

        List<Run> runs = new ArrayList<>();
        Map<String, Set<String>> counterSchemas = new LinkedHashMap<>();
        for (int round = 1; round <= MEASURED_RUNS; round++) {
            for (int position = 0; position < entries.size(); position++) {
                int index = round % 2 == 1 ? position : entries.size() - 1 - position;
                Algorithm algorithm = entries.get(index);
                Task task = Objects.requireNonNull(algorithm.factory().get());
                long started = nanoTime.getAsLong();
                task.execute();
                long elapsed = nanoTime.getAsLong() - started;
                Run run = new Run(input, algorithm.name(), round, elapsed, task.counters(), task.results());
                if (!expectedResults.get(algorithm.name()).equals(run.results())) {
                    throw new IllegalStateException("Measured results changed for " + algorithm.name());
                }
                Set<String> previous = counterSchemas.putIfAbsent(algorithm.name(), run.counters().keySet());
                if (previous != null && !previous.equals(run.counters().keySet())) {
                    throw new IllegalStateException("Counter schema changed for " + algorithm.name());
                }
                runs.add(run);
            }
        }
        return List.copyOf(runs);
    }

    public static List<Summary> summarize(List<Run> runs) {
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("At least one measured run is required.");
        }
        Map<Group, List<Run>> groups = new LinkedHashMap<>();
        for (Run run : runs) {
            Objects.requireNonNull(run);
            groups.computeIfAbsent(new Group(run.input(), run.algorithm()), key -> new ArrayList<>()).add(run);
        }
        List<Summary> summaries = new ArrayList<>();
        for (Map.Entry<Group, List<Run>> entry : groups.entrySet()) {
            List<Run> samples = entry.getValue();
            Run first = samples.get(0);
            Set<Integer> numbers = new HashSet<>();
            if (samples.size() != MEASURED_RUNS) {
                throw new IllegalArgumentException("Every algorithm/input group must have exactly five runs.");
            }
            double elapsedTotal = 0;
            Map<String, Double> totals = new LinkedHashMap<>();
            for (String key : first.counters().keySet()) {
                totals.put(key, 0.0);
            }
            for (Run sample : samples) {
                if (!numbers.add(sample.run()) || !first.results().equals(sample.results())
                        || !first.counters().keySet().equals(sample.counters().keySet())) {
                    throw new IllegalArgumentException("Run numbers, results, or counter schemas are inconsistent.");
                }
                elapsedTotal += sample.elapsedNanos();
                for (String key : totals.keySet()) {
                    totals.put(key, totals.get(key) + sample.counters().get(key));
                }
            }
            Map<String, Double> averages = new LinkedHashMap<>();
            for (Map.Entry<String, Double> total : totals.entrySet()) {
                averages.put(total.getKey(), total.getValue() / MEASURED_RUNS);
            }
            summaries.add(new Summary(entry.getKey().input(), entry.getKey().algorithm(), MEASURED_RUNS,
                    elapsedTotal / MEASURED_RUNS, averages, first.results()));
        }
        return List.copyOf(summaries);
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Names must not be blank.");
        }
        return name;
    }

    private static void requireMetricName(String name) {
        Objects.requireNonNull(name);
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Metric names must use lowercase letters, digits, and underscores.");
        }
    }

    private static Map<String, Long> copyMetrics(Map<String, Long> metrics, boolean nonNegative) {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : metrics.entrySet()) {
            requireMetricName(entry.getKey());
            long value = Objects.requireNonNull(entry.getValue());
            if (nonNegative && value < 0) {
                throw new IllegalArgumentException("Operation counters must not be negative.");
            }
            snapshot.put(entry.getKey(), value);
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private record Group(GraphCase input, String algorithm) {
    }
}
