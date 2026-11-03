package cityplanner;

import cityplanner.connectivity.Kosaraju;
import cityplanner.connectivity.SccResult;
import cityplanner.connectivity.TopologicalSort;
import cityplanner.connectivity.TopologicalSortResult;
import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import cityplanner.mst.Kruskal;
import cityplanner.mst.MstResult;
import cityplanner.mst.Prim;
import cityplanner.shortestpath.Dijkstra;
import cityplanner.shortestpath.DijkstraResult;
import java.util.ArrayList;
import java.util.List;

public final class Main {
    private static final List<String> DISTRICTS = List.of("Center", "North", "East", "South", "West", "Airport");
    private static final List<String> TASKS = List.of("Survey", "Budget", "Roadwork", "Inspection", "Park");

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Smart City Infrastructure and Route Planning");
        showInfrastructure("Infrastructure: connected districts", infrastructureGraph(5));
        showInfrastructure("Infrastructure: isolated airport", infrastructureGraph(6));
        showDependencies(false);
        showDependencies(true);
        showComponents();
        showRoutes();
    }

    private static WeightedGraph infrastructureGraph(int vertexCount) {
        WeightedGraph graph = new WeightedGraph(vertexCount, false);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 6);
        graph.addEdge(0, 3, 5);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 4);
        graph.addEdge(3, 4, 2);
        return graph;
    }

    private static void showInfrastructure(String title, WeightedGraph graph) {
        printGraph(title, graph, DISTRICTS);
        MstResult kruskal = new Kruskal().findMinimumSpanningForest(graph);
        MstResult prim = new Prim().findMinimumSpanningForest(graph);
        printMst("Kruskal", kruskal);
        printMst("Prim", prim);
        System.out.println("Total weights agree: " + (kruskal.totalWeight() == prim.totalWeight()));
    }

    private static void printMst(String algorithm, MstResult result) {
        String kind = result.isConnected() ? "MST" : "forest";
        System.out.println(algorithm + " " + kind + ": weight=" + result.totalWeight()
                + ", components=" + result.componentCount() + ", connected=" + result.isConnected());
        for (Edge edge : result.edges()) {
            System.out.println("  " + formatEdge(edge, DISTRICTS, false));
        }
    }

    private static void showDependencies(boolean cyclic) {
        WeightedGraph graph = new WeightedGraph(TASKS.size(), true);
        graph.addEdge(0, 2, 0);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 3, 0);
        if (cyclic) {
            graph.addEdge(3, 2, 0);
        }
        printGraph("Dependencies: " + (cyclic ? "cycle" : "DAG"), graph, TASKS);
        TopologicalSortResult result = new TopologicalSort().sort(graph);
        if (result.hasCycle()) {
            System.out.println("No topological order: directed cycle detected.");
        } else {
            System.out.println("Topological order: " + formatVertices(result.order(), TASKS, " -> "));
        }
    }

    private static void showComponents() {
        WeightedGraph graph = new WeightedGraph(DISTRICTS.size(), true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        printGraph("Directed districts: strongly connected components", graph, DISTRICTS);
        SccResult result = new Kosaraju().findStronglyConnectedComponents(graph);
        System.out.println("SCC count: " + result.componentCount());
        for (List<Integer> component : result.components()) {
            System.out.println("  Component: " + formatVertices(component, DISTRICTS, ", "));
        }
    }

    private static void showRoutes() {
        WeightedGraph graph = new WeightedGraph(DISTRICTS.size(), true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 5);
        graph.addEdge(3, 4, 3);
        printGraph("Routes: shortest paths", graph, DISTRICTS);
        DijkstraResult result = new Dijkstra().findShortestPaths(graph, 0);
        System.out.println("Source: " + DISTRICTS.get(result.source()));
        for (int vertex = 0; vertex < result.vertexCount(); vertex++) {
            String district = DISTRICTS.get(vertex);
            if (result.isReachable(vertex)) {
                System.out.println("  " + district + ": distance=" + result.distanceTo(vertex)
                        + ", path=" + formatVertices(result.pathTo(vertex), DISTRICTS, " -> "));
            } else {
                System.out.println("  " + district + ": unreachable");
            }
        }
    }

    private static void printGraph(String title, WeightedGraph graph, List<String> labels) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        List<String> vertices = new ArrayList<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            vertices.add(vertex + "=" + labels.get(vertex));
        }
        System.out.println("Vertices: " + String.join(", ", vertices));
        System.out.println("Edges:");
        for (Edge edge : graph.edges()) {
            System.out.println("  " + formatEdge(edge, labels, graph.isDirected()));
        }
    }

    private static String formatEdge(Edge edge, List<String> labels, boolean directed) {
        String connection = directed ? " -> " : " -- ";
        return labels.get(edge.from()) + connection + labels.get(edge.to()) + " (weight=" + edge.weight() + ")";
    }

    private static String formatVertices(List<Integer> vertices, List<String> labels, String separator) {
        List<String> names = new ArrayList<>();
        for (int vertex : vertices) {
            names.add(labels.get(vertex));
        }
        return String.join(separator, names);
    }
}
