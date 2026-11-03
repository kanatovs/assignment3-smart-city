package cityplanner.experiments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class BenchmarkCsv {
    private BenchmarkCsv() {
    }

    public static void write(Path directory, String prefix, List<BenchmarkRunner.Run> runs,
                             Map<String, String> environment) throws IOException {
        Objects.requireNonNull(directory, "Output directory must not be null.");
        Objects.requireNonNull(prefix, "File prefix must not be null.");
        if (!prefix.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("File prefix must use lowercase letters, digits, and hyphens.");
        }
        List<BenchmarkRunner.Run> rows = List.copyOf(runs);
        List<BenchmarkRunner.Summary> summaries = BenchmarkRunner.summarize(rows);
        Set<String> counterNames = new TreeSet<>();
        Set<String> resultNames = new TreeSet<>();
        for (BenchmarkRunner.Run row : rows) {
            counterNames.addAll(row.counters().keySet());
            resultNames.addAll(row.results().keySet());
        }

        String rawCsv = rawCsv(rows, counterNames, resultNames);
        String summaryCsv = summaryCsv(summaries, counterNames, resultNames);
        String environmentCsv = environmentCsv(environment);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(prefix + "-runs.csv"), rawCsv, StandardCharsets.UTF_8);
        Files.writeString(directory.resolve(prefix + "-summary.csv"), summaryCsv, StandardCharsets.UTF_8);
        Files.writeString(directory.resolve(prefix + "-environment.csv"), environmentCsv, StandardCharsets.UTF_8);
    }

    private static String rawCsv(List<BenchmarkRunner.Run> runs, Set<String> counterNames, Set<String> resultNames) {
        StringBuilder csv = new StringBuilder();
        List<String> header = new ArrayList<>(List.of("scenario", "vertices", "edges", "seed", "directed",
                "algorithm", "run", "elapsed_ns"));
        counterNames.forEach(name -> header.add("counter_" + name));
        resultNames.forEach(name -> header.add("result_" + name));
        appendRow(csv, header);
        for (BenchmarkRunner.Run run : runs) {
            List<String> cells = caseCells(run.input(), run.algorithm());
            cells.add(Integer.toString(run.run()));
            cells.add(Long.toString(run.elapsedNanos()));
            counterNames.forEach(name -> cells.add(metric(run.counters(), name)));
            resultNames.forEach(name -> cells.add(metric(run.results(), name)));
            appendRow(csv, cells);
        }
        return csv.toString();
    }

    private static String summaryCsv(List<BenchmarkRunner.Summary> summaries,
                                     Set<String> counterNames, Set<String> resultNames) {
        StringBuilder csv = new StringBuilder();
        List<String> header = new ArrayList<>(List.of("scenario", "vertices", "edges", "seed", "directed",
                "algorithm", "measured_runs", "average_elapsed_ns", "average_elapsed_ms"));
        counterNames.forEach(name -> header.add("avg_counter_" + name));
        resultNames.forEach(name -> header.add("result_" + name));
        appendRow(csv, header);
        for (BenchmarkRunner.Summary summary : summaries) {
            List<String> cells = caseCells(summary.input(), summary.algorithm());
            cells.add(Integer.toString(summary.measuredRuns()));
            cells.add(Double.toString(summary.averageElapsedNanos()));
            cells.add(Double.toString(summary.averageElapsedMillis()));
            counterNames.forEach(name -> cells.add(metric(summary.averageCounters(), name)));
            resultNames.forEach(name -> cells.add(metric(summary.results(), name)));
            appendRow(csv, cells);
        }
        return csv.toString();
    }

    private static List<String> caseCells(BenchmarkRunner.GraphCase input, String algorithm) {
        return new ArrayList<>(List.of(input.scenario(), Integer.toString(input.vertices()),
                Integer.toString(input.edges()), Long.toString(input.seed()), Boolean.toString(input.directed()), algorithm));
    }

    private static String metric(Map<String, ? extends Number> metrics, String name) {
        Number value = metrics.get(name);
        return value == null ? "" : value.toString();
    }

    private static String environmentCsv(Map<String, String> environment) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of("key", "value"));
        new TreeMap<>(environment).forEach((key, value) -> appendRow(csv, List.of(key, value)));
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            String cell = Objects.requireNonNull(cells.get(index), "CSV cells must not be null.");
            if (cell.indexOf(',') >= 0 || cell.indexOf('"') >= 0 || cell.indexOf('\n') >= 0 || cell.indexOf('\r') >= 0) {
                csv.append('"').append(cell.replace("\"", "\"\"")).append('"');
            } else {
                csv.append(cell);
            }
        }
        csv.append('\n');
    }
}
