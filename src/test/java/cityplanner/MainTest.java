package cityplanner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainTest {
    private static final List<String> DISTRICTS = List.of("Center", "North", "East", "South", "West", "Airport");
    private static final List<String> HEADERS = List.of(
            "=== Infrastructure: connected districts ===",
            "=== Infrastructure: isolated airport ===",
            "=== Dependencies: DAG ===",
            "=== Dependencies: cycle ===",
            "=== Directed districts: strongly connected components ===",
            "=== Routes: shortest paths ===");

    private MainTest() {
    }

    public static void main(String[] args) {
        titleAndExamplesArePresentInOrder();
        connectedInfrastructurePrintsValidTrees();
        isolatedAirportPrintsForests();
        dagOrderRespectsEveryDependency();
        cyclicDependenciesDoNotPrintPartialOrder();
        strongComponentsHaveExpectedMembership();
        shortestPathsHaveCorrectDistancesAndRoutes();
        repeatedRunsHaveTheSameOutput();
        System.out.println("MainTest passed.");
    }

    private static void titleAndExamplesArePresentInOrder() {
        List<String> output = runMain().lines().toList();
        check(!output.isEmpty() && output.get(0).equals("Smart City Infrastructure and Route Planning"),
                "The application must start with its title.");
        List<String> headers = output.stream().filter(line -> line.startsWith("=== ")).toList();
        check(headers.equals(HEADERS), "Every example must appear exactly once in the documented order.");
    }

    private static void connectedInfrastructurePrintsValidTrees() {
        List<String> example = section(runMain(), HEADERS.get(0));
        verifyInfrastructure(example, 5, 1, "MST");
        check(example.stream().noneMatch(line -> line.contains("Airport")),
                "The connected example must include only its five districts.");
    }

    private static void isolatedAirportPrintsForests() {
        List<String> example = section(runMain(), HEADERS.get(1));
        verifyInfrastructure(example, 6, 2, "forest");
        check(example.stream().noneMatch(line -> line.contains(" MST:")),
                "A disconnected result must be labeled as a forest.");
        check(example.stream().anyMatch(line -> line.startsWith("Vertices:") && line.contains("5=Airport")),
                "The isolated airport must be included among the input vertices.");
    }

    private static void dagOrderRespectsEveryDependency() {
        List<String> example = section(runMain(), HEADERS.get(2));
        List<PrintedEdge> edges = inputEdges(example, true);
        verifyInputEdges(edges, dependencyEdges());
        String line = singleLineStarting(example, "Topological order: ");
        List<String> order = List.of(line.substring("Topological order: ".length()).split(" -> "));
        Set<String> tasks = Set.of("Survey", "Budget", "Roadwork", "Inspection", "Park");
        check(order.size() == tasks.size() && new HashSet<>(order).equals(tasks),
                "The topological order must contain every task once, including the isolated park.");
        for (PrintedEdge edge : edges) {
            check(order.indexOf(edge.from()) < order.indexOf(edge.to()),
                    "Every printed dependency must point forward in the order.");
        }
        check(example.stream().noneMatch(value -> value.contains("cycle detected")),
                "The DAG must not be reported as cyclic.");
    }

    private static void cyclicDependenciesDoNotPrintPartialOrder() {
        List<String> example = section(runMain(), HEADERS.get(3));
        List<PrintedEdge> expected = new ArrayList<>(dependencyEdges());
        expected.add(new PrintedEdge("Inspection", "Roadwork", 0));
        verifyInputEdges(inputEdges(example, true), expected);
        check(example.contains("No topological order: directed cycle detected."),
                "The directed cycle must be reported explicitly.");
        check(example.stream().noneMatch(line -> line.startsWith("Topological order:")),
                "An acyclic prefix must not be printed as a complete order.");
    }

    private static void strongComponentsHaveExpectedMembership() {
        List<String> example = section(runMain(), HEADERS.get(4));
        verifyInputEdges(inputEdges(example, true), List.of(
                new PrintedEdge("Center", "North", 1), new PrintedEdge("North", "East", 1),
                new PrintedEdge("East", "Center", 1), new PrintedEdge("East", "South", 1),
                new PrintedEdge("South", "West", 1), new PrintedEdge("West", "South", 1)));
        check(example.contains("SCC count: 3"), "The example must contain three strongly connected components.");
        Set<Set<String>> actual = new HashSet<>();
        Set<String> covered = new HashSet<>();
        for (String line : example) {
            if (!line.startsWith("  Component: ")) {
                continue;
            }
            List<String> members = List.of(line.substring("  Component: ".length()).split(", "));
            Set<String> group = new HashSet<>(members);
            check(group.size() == members.size(), "A component must not repeat a district.");
            check(actual.add(group), "Component groups must not be duplicated.");
            for (String district : group) {
                check(covered.add(district), "A district must belong to exactly one component.");
            }
        }
        check(actual.equals(Set.of(Set.of("Center", "North", "East"), Set.of("South", "West"), Set.of("Airport"))),
                "Component membership must match the graph regardless of traversal order.");
        check(covered.equals(new HashSet<>(DISTRICTS)), "Every district must appear in a component.");
    }

    private static void shortestPathsHaveCorrectDistancesAndRoutes() {
        List<String> example = section(runMain(), HEADERS.get(5));
        List<PrintedEdge> edges = inputEdges(example, true);
        verifyInputEdges(edges, List.of(
                new PrintedEdge("Center", "North", 4), new PrintedEdge("Center", "East", 1),
                new PrintedEdge("East", "North", 2), new PrintedEdge("North", "South", 1),
                new PrintedEdge("East", "South", 5), new PrintedEdge("South", "West", 3)));
        check(example.contains("Source: Center"), "The route source must be named.");
        Map<String, Long> expected = Map.of("Center", 0L, "North", 3L, "East", 1L, "South", 4L, "West", 7L);
        Pattern routePattern = Pattern.compile("^  (\\w+): distance=(\\d+), path=(.+)$");
        Set<String> reported = new HashSet<>();
        for (String line : example) {
            Matcher match = routePattern.matcher(line);
            if (!match.matches()) {
                continue;
            }
            String target = match.group(1);
            check(expected.containsKey(target) && reported.add(target), "Reachable targets must be valid and unique.");
            long distance = Long.parseLong(match.group(2));
            check(distance == expected.get(target), "The printed shortest distance is incorrect for " + target + ".");
            List<String> path = List.of(match.group(3).split(" -> "));
            check(path.get(0).equals("Center") && path.get(path.size() - 1).equals(target),
                    "A route must start at the source and end at the reported district.");
            check(new HashSet<>(path).size() == path.size(), "A route must not contain a predecessor cycle.");
            long cost = 0;
            for (int index = 1; index < path.size(); index++) {
                String from = path.get(index - 1);
                String to = path.get(index);
                PrintedEdge step = edges.stream().filter(edge -> edge.from().equals(from) && edge.to().equals(to))
                        .findFirst().orElseThrow(() -> new AssertionError("A route uses a missing directed edge."));
                cost += step.weight();
            }
            check(cost == distance, "The actual route cost must equal the printed shortest distance.");
        }
        check(reported.equals(expected.keySet()), "Every reachable target must have a distance and a route.");
        check(example.stream().filter(line -> line.equals("  Airport: unreachable")).count() == 1,
                "The isolated airport must be labeled unreachable exactly once.");
        check(example.stream().noneMatch(line -> line.contains(Long.toString(Long.MAX_VALUE))),
                "Unreachable output must not expose the internal distance sentinel.");
    }

    private static void repeatedRunsHaveTheSameOutput() {
        check(runMain().equals(runMain()), "Repeated runs must produce the same examples without retaining mutable state.");
    }

    private static void verifyInfrastructure(List<String> example, int vertexCount, int components, String kind) {
        List<PrintedEdge> input = inputEdges(example, false);
        verifyInputEdges(input, List.of(
                new PrintedEdge("Center", "North", 10), new PrintedEdge("Center", "East", 6),
                new PrintedEdge("Center", "South", 5), new PrintedEdge("North", "South", 15),
                new PrintedEdge("East", "South", 4), new PrintedEdge("South", "West", 2)));
        for (String algorithm : List.of("Kruskal", "Prim")) {
            String summary = algorithm + " " + kind + ": weight=21, components=" + components
                    + ", connected=" + (components == 1);
            int start = example.indexOf(summary);
            check(start >= 0, "The " + algorithm + " summary must report the known weight and connectivity.");
            List<PrintedEdge> available = new ArrayList<>(input);
            int[] groups = new int[vertexCount];
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                groups[vertex] = vertex;
            }
            int selected = 0;
            long weight = 0;
            for (int index = start + 1; index < example.size() && example.get(index).startsWith("  "); index++) {
                PrintedEdge edge = parseEdge(example.get(index), false);
                boolean present = available.remove(edge);
                if (!present) {
                    present = available.remove(new PrintedEdge(edge.to(), edge.from(), edge.weight()));
                }
                check(present, "Selected edges must come from the printed undirected input.");
                int from = DISTRICTS.indexOf(edge.from());
                int to = DISTRICTS.indexOf(edge.to());
                check(from >= 0 && from < vertexCount && to >= 0 && to < vertexCount,
                        "Selected districts must belong to the input graph.");
                int oldGroup = groups[to];
                int newGroup = groups[from];
                check(oldGroup != newGroup, "Selected edges must not form a cycle.");
                for (int vertex = 0; vertex < vertexCount; vertex++) {
                    if (groups[vertex] == oldGroup) {
                        groups[vertex] = newGroup;
                    }
                }
                selected++;
                weight += edge.weight();
            }
            check(selected == 4 && weight == 21, "The selected edges must form the hand-calculated minimum forest.");
            for (int vertex = 1; vertex < 5; vertex++) {
                check(groups[vertex] == groups[0], "The five connected districts must be spanned.");
            }
            if (vertexCount == 6) {
                check(groups[5] != groups[0], "The airport must remain a separate component.");
            }
        }
        check(example.contains("Total weights agree: true"), "The two algorithms must report equal total weights.");
    }

    private static List<PrintedEdge> dependencyEdges() {
        return List.of(new PrintedEdge("Survey", "Roadwork", 0), new PrintedEdge("Budget", "Roadwork", 0),
                new PrintedEdge("Roadwork", "Inspection", 0));
    }

    private static List<PrintedEdge> inputEdges(List<String> example, boolean directed) {
        int start = example.indexOf("Edges:");
        check(start >= 0, "Each example must show its input edges.");
        List<PrintedEdge> edges = new ArrayList<>();
        for (int index = start + 1; index < example.size() && example.get(index).startsWith("  "); index++) {
            edges.add(parseEdge(example.get(index), directed));
        }
        return edges;
    }

    private static PrintedEdge parseEdge(String line, boolean directed) {
        String connector = Pattern.quote(directed ? " -> " : " -- ");
        Matcher match = Pattern.compile("^  (\\w+)" + connector + "(\\w+) \\(weight=(\\d+)\\)$").matcher(line);
        check(match.matches(), "An input or selected edge has an unexpected format: " + line);
        return new PrintedEdge(match.group(1), match.group(2), Integer.parseInt(match.group(3)));
    }

    private static void verifyInputEdges(List<PrintedEdge> actual, List<PrintedEdge> expected) {
        check(actual.size() == expected.size() && new HashSet<>(actual).equals(new HashSet<>(expected)),
                "The printed input graph must match the hand-checkable example.");
    }

    private static String singleLineStarting(List<String> example, String prefix) {
        List<String> matching = example.stream().filter(line -> line.startsWith(prefix)).toList();
        check(matching.size() == 1, "The example must contain exactly one line starting with " + prefix);
        return matching.get(0);
    }

    private static List<String> section(String output, String header) {
        List<String> lines = output.lines().toList();
        int start = lines.indexOf(header);
        check(start >= 0, "A console example is missing: " + header);
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).startsWith("=== ")) {
            end++;
        }
        return lines.subList(start, end);
    }

    private static String runMain() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            try {
                System.setOut(captured);
                Main.main(new String[0]);
            } finally {
                System.setOut(original);
            }
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record PrintedEdge(String from, String to, int weight) {
    }
}
