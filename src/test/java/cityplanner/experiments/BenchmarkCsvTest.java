package cityplanner.experiments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BenchmarkCsvTest {
    private static final BenchmarkRunner.GraphCase INPUT =
            new BenchmarkRunner.GraphCase("connected-undirected", 4, 5, 41100L, false);

    private BenchmarkCsvTest() {
    }

    public static void main(String[] args) throws IOException {
        rawRowsPreserveMeasurementsAndSortedSchema();
        summariesAverageTheControlledRuns();
        unavailableCountersHaveEmptyCells();
        csvEscapingPreservesTextAndUnicode();
        environmentRowsAreSortedKeyValuePairs();
        numericOutputDoesNotDependOnLocale();
        outputDirectoryIsCreated();
        repeatedExportPreservesUnrelatedFiles();
        invalidPrefixesDoNotCreateFiles();
        incompleteMeasurementsDoNotCreateFiles();
        outputFileErrorsRemainVisible();
        System.out.println("BenchmarkCsvTest passed.");
    }

    private static void rawRowsPreserveMeasurementsAndSortedSchema() throws IOException {
        withDirectory(directory -> {
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of("java_version", "17"));
            String text = Files.readString(directory.resolve("test-runs.csv"), StandardCharsets.UTF_8);
            List<List<String>> rows = parseCsv(text);

            check(rows.get(0).equals(List.of("scenario", "vertices", "edges", "seed", "directed",
                    "algorithm", "run", "elapsed_ns", "counter_edge_examinations", "counter_queue_offers",
                    "counter_sorting_comparisons", "result_component_count", "result_selected_edges",
                    "result_total_weight")), "Raw columns must retain metadata and sorted named metrics.");
            check(rows.size() == 11, "Only the ten controlled measured runs must be exported.");
            List<String> first = rows.get(1);
            check(first.subList(0, 8).equals(List.of("connected-undirected", "4", "5", "41100", "false",
                    "First", "1", "100")), "Raw metadata and nanoseconds must be preserved exactly.");
            check(first.get(8).equals("2") && first.get(10).equals("4"), "Raw counters must not be averaged.");
            check(first.subList(11, 14).equals(List.of("1", "3", "19")), "Outcome fields must be exported.");
            check(text.endsWith("\n") && !text.contains("\r"), "Record separators must use LF and end the file.");
            for (List<String> row : rows) {
                check(row.size() == rows.get(0).size(), "Every raw row must have the same column count.");
            }
        });
    }

    private static void summariesAverageTheControlledRuns() throws IOException {
        withDirectory(directory -> {
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of());
            List<List<String>> rows = readCsv(directory.resolve("test-summary.csv"));
            List<String> header = rows.get(0);
            check(header.equals(List.of("scenario", "vertices", "edges", "seed", "directed", "algorithm",
                    "measured_runs", "average_elapsed_ns", "average_elapsed_ms", "avg_counter_edge_examinations",
                    "avg_counter_queue_offers", "avg_counter_sorting_comparisons", "result_component_count",
                    "result_selected_edges", "result_total_weight")), "Summary columns must declare units and averages.");
            check(rows.size() == 3, "Each algorithm/input group must produce one summary row.");
            List<String> first = rows.get(1);
            List<String> second = rows.get(2);
            check(first.get(5).equals("First") && second.get(5).equals("Second"),
                    "Summary groups must retain their first encounter order.");
            check(first.get(6).equals("5") && second.get(6).equals("5"), "Each mean must describe five measured runs.");
            check(Double.parseDouble(first.get(7)) == 300.0 && Double.parseDouble(second.get(7)) == 600.0,
                    "Nanosecond means must use the controlled measurements.");
            check(Math.abs(Double.parseDouble(first.get(8)) - 0.0003) < 1e-15,
                    "Millisecond means must convert nanoseconds without truncation.");
            check(Double.parseDouble(first.get(9)) == 6.0 && Double.parseDouble(first.get(11)) == 12.0,
                    "Named counters must be averaged separately.");
            check(Double.parseDouble(second.get(9)) == 9.0 && Double.parseDouble(second.get(10)) == 15.0,
                    "Each algorithm must retain its own counter means.");
        });
    }

    private static void unavailableCountersHaveEmptyCells() throws IOException {
        withDirectory(directory -> {
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of());
            List<List<String>> raw = readCsv(directory.resolve("test-runs.csv"));
            List<List<String>> summary = readCsv(directory.resolve("test-summary.csv"));
            check(raw.get(1).get(9).isEmpty(), "A missing raw queue counter must be empty, not zero.");
            check(raw.get(2).get(10).isEmpty(), "A missing raw sorting counter must be empty, not zero.");
            check(summary.get(1).get(10).isEmpty() && summary.get(2).get(11).isEmpty(),
                    "Summary rows must also leave inapplicable counters empty.");
        });
    }

    private static void csvEscapingPreservesTextAndUnicode() throws IOException {
        withDirectory(directory -> {
            String scenario = "North, \"river\"\ncafé\rzone";
            String algorithm = "Prim,\n\"quoted\"";
            String metadataKey = "runtime, \"name\"";
            String metadataValue = "Java \"17\"\nnaïve\rfinal";
            BenchmarkRunner.GraphCase input = new BenchmarkRunner.GraphCase(scenario, 2, 1, -1, false);
            List<BenchmarkRunner.Run> runs = new ArrayList<>();
            for (int run = 1; run <= 5; run++) {
                runs.add(new BenchmarkRunner.Run(input, algorithm, run, run, Map.of("edges", 1L),
                        Map.of("total_weight", 0L)));
            }
            BenchmarkCsv.write(directory, "test", runs, Map.of(metadataKey, metadataValue));
            List<List<String>> raw = readCsv(directory.resolve("test-runs.csv"));
            List<List<String>> summary = readCsv(directory.resolve("test-summary.csv"));
            List<List<String>> environment = readCsv(directory.resolve("test-environment.csv"));

            check(raw.size() == 6 && raw.get(1).get(0).equals(scenario) && raw.get(1).get(5).equals(algorithm),
                    "Escaped raw text must preserve commas, quotes, CR/LF, and Unicode.");
            check(summary.get(1).get(0).equals(scenario) && summary.get(1).get(5).equals(algorithm),
                    "Summary text must use the same CSV escaping.");
            check(environment.get(1).equals(List.of(metadataKey, metadataValue)),
                    "Metadata keys and values must survive CSV parsing exactly.");
            String rawText = Files.readString(directory.resolve("test-runs.csv"), StandardCharsets.UTF_8);
            check(rawText.contains("\"\"river\"\""), "Embedded quotes must be doubled inside a quoted field.");
            check(!rawText.startsWith("\uFEFF"), "UTF-8 output must not add a byte-order mark.");
        });
    }

    private static void environmentRowsAreSortedKeyValuePairs() throws IOException {
        withDirectory(directory -> {
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of("z_os", "Windows", "a_java", "17"));
            check(readCsv(directory.resolve("test-environment.csv")).equals(
                    List.of(List.of("key", "value"), List.of("a_java", "17"), List.of("z_os", "Windows"))),
                    "Environment metadata must have a stable key/value schema and sorted keys.");
        });
    }

    private static void numericOutputDoesNotDependOnLocale() throws IOException {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            withDirectory(directory -> {
                BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of());
                List<List<String>> summary = readCsv(directory.resolve("test-summary.csv"));
                check(summary.get(1).get(7).equals("300.0"), "Decimal output must use a dot in every locale.");
                check(Double.parseDouble(summary.get(1).get(8)) > 0, "Small time conversions must stay numeric.");
            });
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static void outputDirectoryIsCreated() throws IOException {
        withDirectory(directory -> {
            Path nested = directory.resolve("nested");
            BenchmarkCsv.write(nested, "test", controlledRuns(), Map.of());
            check(Files.isRegularFile(nested.resolve("test-runs.csv"))
                    && Files.isRegularFile(nested.resolve("test-summary.csv"))
                    && Files.isRegularFile(nested.resolve("test-environment.csv")),
                    "An absent output directory must be created with all three CSV files.");
        });
    }

    private static void repeatedExportPreservesUnrelatedFiles() throws IOException {
        withDirectory(directory -> {
            Path unrelated = directory.resolve("keep.txt");
            Files.writeString(unrelated, "student notes", StandardCharsets.UTF_8);
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of("version", "old"));
            BenchmarkCsv.write(directory, "test", controlledRuns(), Map.of("version", "new"));
            check(Files.readString(unrelated, StandardCharsets.UTF_8).equals("student notes"),
                    "Export must preserve unrelated files.");
            check(readCsv(directory.resolve("test-runs.csv")).size() == 11,
                    "Repeated export must replace the CSV instead of appending duplicate runs.");
            check(readCsv(directory.resolve("test-environment.csv")).equals(
                    List.of(List.of("key", "value"), List.of("version", "new"))),
                    "Repeated export must replace old metadata.");
        });
    }

    private static void invalidPrefixesDoNotCreateFiles() throws IOException {
        withDirectory(directory -> {
            Path output = directory.resolve("nested");
            for (String prefix : List.of("", "../escape", "a/b", "a\\b", "Upper", "-bad", "a.b")) {
                expectIllegalArgument(() -> BenchmarkCsv.write(output, prefix, controlledRuns(), Map.of()));
            }
            try (var files = Files.list(directory)) {
                check(files.findAny().isEmpty(), "Rejected prefixes must not create output files.");
            }
        });
    }

    private static void incompleteMeasurementsDoNotCreateFiles() throws IOException {
        withDirectory(directory -> {
            List<BenchmarkRunner.Run> incomplete = controlledRuns().subList(0, 9);
            expectIllegalArgument(() -> BenchmarkCsv.write(directory, "test", incomplete, Map.of()));
            try (var files = Files.list(directory)) {
                check(files.findAny().isEmpty(), "Invalid run groups must be rejected before writing files.");
            }
        });
    }

    private static void outputFileErrorsRemainVisible() throws IOException {
        withDirectory(directory -> {
            Path file = directory.resolve("not-a-directory");
            Files.writeString(file, "existing file", StandardCharsets.UTF_8);
            expectIOException(() -> BenchmarkCsv.write(file, "test", controlledRuns(), Map.of()));
            check(Files.readString(file, StandardCharsets.UTF_8).equals("existing file"),
                    "An output directory error must not overwrite the blocking file.");
        });
    }

    private static List<BenchmarkRunner.Run> controlledRuns() {
        List<BenchmarkRunner.Run> runs = new ArrayList<>();
        Map<String, Long> results = Map.of("total_weight", 19L, "selected_edges", 3L, "component_count", 1L);
        for (int run = 1; run <= 5; run++) {
            runs.add(new BenchmarkRunner.Run(INPUT, "First", run, 100L * run,
                    Map.of("edge_examinations", 2L * run, "sorting_comparisons", 4L * run), results));
            runs.add(new BenchmarkRunner.Run(INPUT, "Second", run, 200L * run,
                    Map.of("edge_examinations", 3L * run, "queue_offers", 5L * run), results));
        }
        return runs;
    }

    private static List<List<String>> readCsv(Path path) throws IOException {
        return parseCsv(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && (character == ',' || character == '\n')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (character == '\n') {
                    rows.add(List.copyOf(row));
                    row.clear();
                }
            } else {
                cell.append(character);
            }
        }
        check(!quoted && row.isEmpty() && cell.length() == 0, "CSV must end with a complete LF-terminated record.");
        return rows;
    }

    private static void withDirectory(DirectoryCheck action) throws IOException {
        Path directory = Files.createTempDirectory("cityplanner-csv-");
        try {
            action.run(directory);
        } finally {
            for (Path output : List.of(directory.resolve("nested"), directory)) {
                for (String suffix : List.of("-runs.csv", "-summary.csv", "-environment.csv")) {
                    Files.deleteIfExists(output.resolve("test" + suffix));
                }
                if (!output.equals(directory)) {
                    Files.deleteIfExists(output);
                }
            }
            Files.deleteIfExists(directory.resolve("keep.txt"));
            Files.deleteIfExists(directory.resolve("not-a-directory"));
            Files.deleteIfExists(directory);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectIllegalArgument(IoAction action) throws IOException {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static void expectIOException(IoAction action) throws IOException {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Expected IOException.");
    }

    @FunctionalInterface
    private interface DirectoryCheck {
        void run(Path directory) throws IOException;
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
