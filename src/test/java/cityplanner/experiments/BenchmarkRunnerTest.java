package cityplanner.experiments;

import cityplanner.experiments.BenchmarkRunner.Algorithm;
import cityplanner.experiments.BenchmarkRunner.GraphCase;
import cityplanner.experiments.BenchmarkRunner.Run;
import cityplanner.experiments.BenchmarkRunner.Summary;
import cityplanner.experiments.BenchmarkRunner.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

public final class BenchmarkRunnerTest {
    private static final GraphCase INPUT = new GraphCase("controlled", 4, 5, 101, false);

    private BenchmarkRunnerTest() {
    }

    public static void main(String[] args) {
        everyInvocationUsesFreshTaskState();
        timingExcludesFactoriesAndMetricExtraction();
        warmupsAreExcludedAndMeasuredOrderAlternates();
        elapsedTimesAreClockDifferences();
        zeroElapsedTimeIsAllowed();
        changingWarmupResultsAreRejected();
        changingMeasuredResultsAreRejected();
        changingMeasuredCounterSchemasAreRejected();
        summariesAverageEachMetricSeparately();
        summariesKeepCasesAndAlgorithmsSeparate();
        largeValuesDoNotOverflowAverages();
        resultCollectionsAreDetachedAndImmutable();
        incompleteAndDuplicateGroupsAreRejected();
        inconsistentSummarySchemasAreRejected();
        invalidRecordValuesAreRejected();
        invalidMeasurementArgumentsAreRejected();
        taskFailuresArePropagated();
        System.out.println("BenchmarkRunnerTest passed.");
    }

    private static void everyInvocationUsesFreshTaskState() {
        Tracking result = trackedMeasurement();
        check(BenchmarkRunner.WARMUP_RUNS == 3 && BenchmarkRunner.MEASURED_RUNS == 5,
                "The experiment must use three warmups and five measured runs.");
        check(result.createdTasks() == 16 && result.distinctTasks() == 16,
                "Two algorithms must each receive eight separately created tasks.");
        for (Run run : result.runs()) {
            check(run.counters().get("executions") == 1,
                    "Every measured task must start with fresh mutable state.");
        }
    }

    private static void timingExcludesFactoriesAndMetricExtraction() {
        Tracking result = trackedMeasurement();
        check(result.clockCalls() == 20, "Only the ten measured invocations must read start and stop timestamps.");
        check(result.runs().stream().allMatch(run -> run.elapsedNanos() == 10),
                "Factory creation and metric extraction must remain outside the timed operation.");
    }

    private static void warmupsAreExcludedAndMeasuredOrderAlternates() {
        Tracking result = trackedMeasurement();
        check(result.runs().size() == 10, "Warmup executions must not appear among exported runs.");
        check(result.executionOrder().subList(0, 6).equals(List.of("Alpha", "Beta", "Alpha", "Beta", "Alpha", "Beta")),
                "Warmups must exercise both algorithms three times.");
        List<String> expected = List.of("Alpha", "Beta", "Beta", "Alpha", "Alpha", "Beta", "Beta", "Alpha", "Alpha", "Beta");
        check(result.runs().stream().map(Run::algorithm).toList().equals(expected),
                "Measured rounds must alternate forward and reverse algorithm order.");
        for (int index = 0; index < result.runs().size(); index++) {
            Run run = result.runs().get(index);
            check(run.input().equals(INPUT) && run.run() == index / 2 + 1,
                    "Measured rows must retain input metadata and one-based measured round numbers.");
        }
    }

    private static void elapsedTimesAreClockDifferences() {
        long[] timestamps = {100, 137, 1000, 1042, 2000, 2000, 3000, 3013, 9000, 9100};
        int[] calls = {0};
        BenchmarkRunner runner = new BenchmarkRunner(() -> {
            check(calls[0] < timestamps.length, "Warmups must not request timestamps.");
            return timestamps[calls[0]++];
        });
        List<Run> runs = runner.measure(INPUT, List.of(new Algorithm("Alpha", () -> simpleTask(7))));
        check(runs.stream().map(Run::elapsedNanos).toList().equals(List.of(37L, 42L, 0L, 13L, 100L)),
                "Durations must use elapsed differences, not absolute timestamp values.");
        check(calls[0] == 10, "Five measured calls require exactly ten timestamps.");
    }

    private static void zeroElapsedTimeIsAllowed() {
        List<Run> runs = new BenchmarkRunner(() -> 55).measure(INPUT,
                List.of(new Algorithm("Alpha", () -> simpleTask(7))));
        check(runs.size() == 5 && runs.stream().allMatch(run -> run.elapsedNanos() == 0),
                "A timer's resolution may legitimately produce a zero duration.");
    }

    private static void changingWarmupResultsAreRejected() {
        int[] tasks = {0};
        Algorithm algorithm = new Algorithm("Changing", () -> simpleTask(++tasks[0]));
        expectIllegalState(() -> new BenchmarkRunner(() -> 0).measure(INPUT, List.of(algorithm)));
    }

    private static void changingMeasuredResultsAreRejected() {
        int[] tasks = {0};
        Algorithm algorithm = new Algorithm("Changing", () -> {
            int invocation = ++tasks[0];
            return simpleTask(invocation <= BenchmarkRunner.WARMUP_RUNS ? 7 : 8);
        });
        expectIllegalState(() -> new BenchmarkRunner(() -> 0).measure(INPUT, List.of(algorithm)));
    }

    private static void summariesAverageEachMetricSeparately() {
        List<Run> runs = new ArrayList<>();
        for (int round = 1; round <= 5; round++) {
            runs.add(new Run(INPUT, "Alpha", round, 10L * round,
                    Map.of("edge_scans", 2L * round, "comparisons", 5L * round), Map.of("answer", 7L)));
            runs.add(new Run(INPUT, "Beta", round, 20L * round,
                    Map.of("queue_offers", 3L * round), Map.of("answer", 7L)));
        }
        List<Summary> summaries = BenchmarkRunner.summarize(runs);
        check(summaries.size() == 2, "Each algorithm must have its own summary.");
        Summary alpha = summaries.get(0);
        check(alpha.measuredRuns() == 5 && alpha.averageElapsedNanos() == 30,
                "The elapsed average must use all five raw runs.");
        check(alpha.averageElapsedMillis() == 30.0 / 1_000_000,
                "Milliseconds must be converted from the nanosecond average.");
        check(alpha.averageCounters().equals(Map.of("edge_scans", 6.0, "comparisons", 15.0)),
                "Different operation counters must be averaged independently.");
        check(summaries.get(1).averageElapsedNanos() == 60
                && summaries.get(1).averageCounters().equals(Map.of("queue_offers", 9.0)),
                "An algorithm's different counter schema must not be mixed with another algorithm.");
        check(alpha.results().equals(Map.of("answer", 7L)), "Stable algorithm outcomes must accompany the averages.");
    }

    private static void changingMeasuredCounterSchemasAreRejected() {
        int[] tasks = {0};
        Algorithm algorithm = new Algorithm("Changing", () -> {
            int invocation = ++tasks[0];
            return new Task() {
                @Override
                public void execute() {
                }

                @Override
                public Map<String, Long> counters() {
                    return Map.of(invocation <= BenchmarkRunner.WARMUP_RUNS + 1 ? "work" : "other", 1L);
                }

                @Override
                public Map<String, Long> results() {
                    return Map.of("answer", 7L);
                }
            };
        });
        expectIllegalState(() -> new BenchmarkRunner(() -> 0).measure(INPUT, List.of(algorithm)));
    }

    private static void summariesKeepCasesAndAlgorithmsSeparate() {
        GraphCase second = new GraphCase("controlled", 4, 5, 102, false);
        List<Run> runs = new ArrayList<>();
        runs.addAll(rawRuns(second, "Beta"));
        runs.addAll(rawRuns(INPUT, "Alpha"));
        runs.addAll(rawRuns(second, "Alpha"));
        List<Summary> summaries = BenchmarkRunner.summarize(runs);
        check(summaries.stream().map(summary -> summary.input().seed() + ":" + summary.algorithm()).toList()
                .equals(List.of("102:Beta", "101:Alpha", "102:Alpha")),
                "Groups must include complete case metadata and preserve first encounter order.");
    }

    private static void largeValuesDoNotOverflowAverages() {
        List<Run> runs = new ArrayList<>();
        for (int round = 1; round <= 5; round++) {
            runs.add(new Run(INPUT, "Alpha", round, Long.MAX_VALUE - 10,
                    Map.of("large_counter", Long.MAX_VALUE), Map.of("answer", 7L)));
        }
        Summary summary = BenchmarkRunner.summarize(runs).get(0);
        checkClose(summary.averageElapsedNanos(), (double) (Long.MAX_VALUE - 10));
        checkClose(summary.averageCounters().get("large_counter"), (double) Long.MAX_VALUE);
    }

    private static void resultCollectionsAreDetachedAndImmutable() {
        Map<String, Long> mutableCounters = new LinkedHashMap<>(Map.of("executions", 1L));
        Map<String, Long> mutableResults = new LinkedHashMap<>(Map.of("answer", 7L));
        Run run = new Run(INPUT, "Alpha", 1, 10, mutableCounters, mutableResults);
        mutableCounters.clear();
        mutableResults.clear();
        check(run.counters().equals(Map.of("executions", 1L)) && run.results().equals(Map.of("answer", 7L)),
                "A raw run must hold snapshots of its metric maps.");
        expectUnsupported(() -> run.counters().put("other", 2L));
        expectUnsupported(() -> run.results().clear());

        Map<String, Double> mutableAverages = new LinkedHashMap<>(Map.of("executions", 1.0));
        Map<String, Long> outcomes = new LinkedHashMap<>(Map.of("answer", 7L));
        Summary summary = new Summary(INPUT, "Alpha", 5, 10, mutableAverages, outcomes);
        mutableAverages.clear();
        outcomes.clear();
        check(summary.averageCounters().equals(Map.of("executions", 1.0))
                && summary.results().equals(Map.of("answer", 7L)), "A summary must also snapshot its maps.");
        expectUnsupported(() -> summary.averageCounters().clear());
        expectUnsupported(() -> summary.results().clear());
        List<Run> measured = new BenchmarkRunner(() -> 0).measure(INPUT,
                List.of(new Algorithm("Alpha", () -> simpleTask(7))));
        expectUnsupported(measured::clear);
        expectUnsupported(() -> BenchmarkRunner.summarize(measured).clear());
    }

    private static void incompleteAndDuplicateGroupsAreRejected() {
        expectIllegalArgument(() -> BenchmarkRunner.summarize(List.of()));
        List<Run> complete = rawRuns(INPUT, "Alpha");
        expectIllegalArgument(() -> BenchmarkRunner.summarize(complete.subList(0, 4)));
        List<Run> duplicates = new ArrayList<>(complete);
        duplicates.set(4, complete.get(0));
        expectIllegalArgument(() -> BenchmarkRunner.summarize(duplicates));
        expectIllegalArgument(() -> {
            List<Run> invalidRound = new ArrayList<>(complete);
            invalidRound.set(4, new Run(INPUT, "Alpha", 6, 50, Map.of("executions", 1L), Map.of("answer", 7L)));
            BenchmarkRunner.summarize(invalidRound);
        });
    }

    private static void inconsistentSummarySchemasAreRejected() {
        List<Run> differentCounter = new ArrayList<>(rawRuns(INPUT, "Alpha"));
        differentCounter.set(4, new Run(INPUT, "Alpha", 5, 50, Map.of("other", 1L), Map.of("answer", 7L)));
        expectIllegalArgument(() -> BenchmarkRunner.summarize(differentCounter));
        List<Run> differentResult = new ArrayList<>(rawRuns(INPUT, "Alpha"));
        differentResult.set(4, new Run(INPUT, "Alpha", 5, 50, Map.of("executions", 1L), Map.of("answer", 8L)));
        expectIllegalArgument(() -> BenchmarkRunner.summarize(differentResult));
    }

    private static void invalidRecordValuesAreRejected() {
        expectIllegalArgument(() -> new GraphCase(" ", 4, 5, 1, false));
        expectIllegalArgument(() -> new GraphCase("invalid", -1, 0, 1, false));
        expectIllegalArgument(() -> new GraphCase("invalid", 1, -1, 1, false));
        expectIllegalArgument(() -> new Algorithm(" ", () -> simpleTask(7)));
        expectIllegalArgument(() -> new Run(INPUT, "Alpha", 0, 10, Map.of(), Map.of()));
        expectIllegalArgument(() -> new Run(INPUT, "Alpha", 1, -1, Map.of(), Map.of()));
        expectIllegalArgument(() -> new Run(INPUT, "Alpha", 1, 10, Map.of("negative", -1L), Map.of()));
        expectRejected(() -> new Algorithm("Alpha", null));
        expectRejected(() -> new Run(null, "Alpha", 1, 10, Map.of(), Map.of()));
        expectRejected(() -> new Run(INPUT, "Alpha", 1, 10, null, Map.of()));
    }

    private static void invalidMeasurementArgumentsAreRejected() {
        BenchmarkRunner runner = new BenchmarkRunner(() -> 0);
        expectIllegalArgument(() -> runner.measure(INPUT, List.of()));
        Algorithm first = new Algorithm("Alpha", () -> simpleTask(7));
        Algorithm duplicate = new Algorithm("Alpha", () -> simpleTask(7));
        expectIllegalArgument(() -> runner.measure(INPUT, List.of(first, duplicate)));
        expectRejected(() -> runner.measure(null, List.of(first)));
        expectRejected(() -> runner.measure(INPUT, null));
    }

    private static void taskFailuresArePropagated() {
        IllegalStateException failure = new IllegalStateException("Controlled failure.");
        Algorithm broken = new Algorithm("Broken", () -> new Task() {
            @Override
            public void execute() {
                throw failure;
            }

            @Override
            public Map<String, Long> counters() {
                throw new AssertionError("A failed task must not be exported.");
            }

            @Override
            public Map<String, Long> results() {
                throw new AssertionError("A failed task must not be exported.");
            }
        });
        try {
            new BenchmarkRunner(() -> 0).measure(INPUT, List.of(broken));
        } catch (IllegalStateException actual) {
            check(actual == failure, "The original algorithm failure must be preserved.");
            return;
        }
        throw new AssertionError("A failed task must stop the benchmark.");
    }

    private static Tracking trackedMeasurement() {
        BoundaryClock clock = new BoundaryClock();
        List<String> order = new ArrayList<>();
        Set<Task> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] creations = {0, 0};
        List<Algorithm> algorithms = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            int algorithmIndex = index;
            String name = index == 0 ? "Alpha" : "Beta";
            algorithms.add(new Algorithm(name, () -> {
                check(!clock.inside, "A task factory must run before the start timestamp.");
                boolean measured = ++creations[algorithmIndex] > BenchmarkRunner.WARMUP_RUNS;
                Task task = new Task() {
                    private int executions;

                    @Override
                    public void execute() {
                        check(clock.inside == measured, "Only measured task execution belongs between clock reads.");
                        check(++executions == 1, "The runner must execute a freshly created task only once.");
                        order.add(name);
                    }

                    @Override
                    public Map<String, Long> counters() {
                        check(!clock.inside && executions == 1, "Counters must be read after execution and timing.");
                        return Map.of("executions", (long) executions);
                    }

                    @Override
                    public Map<String, Long> results() {
                        check(!clock.inside && executions == 1, "Results must be read after execution and timing.");
                        return Map.of("answer", 7L);
                    }
                };
                check(identities.add(task), "Factories must create distinct task identities.");
                return task;
            }));
        }
        List<Run> runs = new BenchmarkRunner(clock).measure(INPUT, algorithms);
        return new Tracking(runs, creations[0] + creations[1], identities.size(), clock.calls, List.copyOf(order));
    }

    private static Task simpleTask(long answer) {
        return new Task() {
            @Override
            public void execute() {
            }

            @Override
            public Map<String, Long> counters() {
                return Map.of("executions", 1L);
            }

            @Override
            public Map<String, Long> results() {
                return Map.of("answer", answer);
            }
        };
    }

    private static List<Run> rawRuns(GraphCase input, String algorithm) {
        List<Run> runs = new ArrayList<>();
        for (int round = 1; round <= 5; round++) {
            runs.add(new Run(input, algorithm, round, round * 10L, Map.of("executions", 1L), Map.of("answer", 7L)));
        }
        return runs;
    }

    private static void checkClose(double actual, double expected) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) <= Math.abs(expected) * 1e-12,
                "Large averages must remain finite and must not overflow a long sum.");
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

    private static void expectIllegalState(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalStateException.");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException expected) {
            return;
        }
        throw new AssertionError("Expected invalid input to be rejected.");
    }

    private static void expectUnsupported(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected an immutable result collection.");
    }

    private static final class BoundaryClock implements LongSupplier {
        private int calls;
        private boolean inside;

        @Override
        public long getAsLong() {
            inside = ++calls % 2 == 1;
            return calls * 10L;
        }
    }

    private record Tracking(List<Run> runs, int createdTasks, int distinctTasks, int clockCalls,
                            List<String> executionOrder) {
    }
}
